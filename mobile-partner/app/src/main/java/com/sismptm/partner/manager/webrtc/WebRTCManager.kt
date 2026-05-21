package com.sismptm.partner.manager.webrtc

import android.app.ActivityManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import org.webrtc.*
import java.util.regex.Pattern

/**
 * Orchestrates WebRTC operations for broadcasting. Manages the peer connection lifecycle, hardware
 * media capture, and stream quality. Handles self-recovery on hardware freezes and adapts
 * connection bitrates organically.
 */
class WebRTCManager(
        private val context: Context,
        private val listener: WebRTCListener,
        private val eglBase: EglBase
) {
    companion object {
        private const val TAG = "WebRTCManager"
        private var isFactoryInitialized = false

        fun ensureInitialized(context: Context) {
            if (!isFactoryInitialized) {
                val options =
                        PeerConnectionFactory.InitializationOptions.builder(
                                        context.applicationContext
                                )
                                .setEnableInternalTracer(false)
                                .createInitializationOptions()
                PeerConnectionFactory.initialize(options)
                isFactoryInitialized = true
            }
        }
    }

    interface WebRTCListener {
        fun onIceCandidate(candidate: IceCandidate)
        fun onLocalSdpCreated(sdp: SessionDescription)
        fun onCommandReceived(command: String)
        fun onConnectionStateChange(state: PeerConnection.PeerConnectionState)
    }

    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null
    private var localVideoTrack: VideoTrack? = null
    private var localAudioTrack: AudioTrack? = null
    private var videoSource: VideoSource? = null
    private var audioSource: AudioSource? = null
    private var videoCapturer: VideoCapturer? = null
    private var surfaceTextureHelper: SurfaceTextureHelper? = null
    private var dataChannel: DataChannel? = null
    private var isCapturing = false
    private var isDisposed = false
    private var lastRenderer: SurfaceViewRenderer? = null

    @Volatile private var isLocalDescriptionSet = false
    @Volatile private var isRemoteDescriptionSet = false
    private val pendingLocalIceCandidates = mutableListOf<IceCandidate>()
    private val pendingRemoteIceCandidates = mutableListOf<IceCandidate>()
    private val mainHandler = Handler(Looper.getMainLooper())

    private val deviceTier: Int by lazy { detectDeviceTier() }

    init {
        ensureInitialized(context)
        buildPeerConnectionFactory()
    }

    private fun detectDeviceTier(): Int {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        val totalRamGb = memInfo.totalMem / (1024 * 1024 * 1024.0)
        val processors = Runtime.getRuntime().availableProcessors()

        // Conservative tiering: heavily penalize low RAM to prevent OOM and encoder crashes
        return when {
            totalRamGb <= 3.0 -> 1 // Low tier: 480p max
            processors <= 4 || totalRamGb <= 4.0 -> 1 
            processors <= 6 || totalRamGb <= 6.0 -> 2 // Mid tier: 720p max
            else -> 3 // High tier: 1080p max
        }
    }

    private fun buildPeerConnectionFactory() {
        val encoderFactory = DefaultVideoEncoderFactory(eglBase.eglBaseContext, true, true)
        val decoderFactory = DefaultVideoDecoderFactory(eglBase.eglBaseContext)

        peerConnectionFactory =
                PeerConnectionFactory.builder()
                        .setVideoEncoderFactory(encoderFactory)
                        .setVideoDecoderFactory(decoderFactory)
                        .createPeerConnectionFactory()
    }

    fun startLocalCapture(surfaceViewRenderer: SurfaceViewRenderer) {
        if (isCapturing || isDisposed) return
        
        // Ensure renderer is initialized only once to avoid "Already initialized" crash
        if (this.lastRenderer !== surfaceViewRenderer) {
            this.lastRenderer = surfaceViewRenderer
            try {
                surfaceViewRenderer.init(eglBase.eglBaseContext, null)
                surfaceViewRenderer.setEnableHardwareScaler(true)
                surfaceViewRenderer.setMirror(false) // Disable mirroring for back-facing camera capture
                surfaceViewRenderer.setFpsReduction(15f) // Reduce GPU overhead for local preview
            } catch (e: IllegalStateException) {
                Log.w(TAG, "SurfaceViewRenderer already initialized, skipping init")
            }
        }

        val enumerator = Camera2Enumerator(context)
        val deviceNames = enumerator.deviceNames
        val deviceName =
                deviceNames.find { enumerator.isBackFacing(it) }
                        ?: deviceNames.firstOrNull() ?: return

        // Implement self-healing hardware event handler
        val eventsHandler = object : CameraVideoCapturer.CameraEventsHandler {
            override fun onCameraError(errorDescription: String?) {
                Log.e(TAG, "Hardware Camera Error: $errorDescription")
                mainHandler.post { restartCapture() }
            }
            override fun onCameraDisconnected() {
                Log.w(TAG, "Hardware Camera Disconnected (OS reclaimed access)")
                mainHandler.post { restartCapture() }
            }
            override fun onCameraFreezed(errorDescription: String?) {
                Log.e(TAG, "Hardware Camera Freezed: $errorDescription")
                mainHandler.post { restartCapture() }
            }
            override fun onCameraOpening(cameraName: String?) {}
            override fun onFirstFrameAvailable() {}
            override fun onCameraClosed() {}
        }

        val capturer = enumerator.createCapturer(deviceName, eventsHandler)
        this.videoCapturer = capturer

        videoSource = peerConnectionFactory!!.createVideoSource(capturer.isScreencast)
        surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", eglBase.eglBaseContext)
        capturer.initialize(surfaceTextureHelper, context, videoSource!!.capturerObserver)

        when (deviceTier) {
            3 -> capturer.startCapture(1280, 720, 30) // Capped at 720p to prevent thermal throttling on high-end devices
            2 -> capturer.startCapture(1280, 720, 30)
            else -> capturer.startCapture(640, 480, 30)
        }

        localVideoTrack = peerConnectionFactory!!.createVideoTrack("video0", videoSource)
        localVideoTrack?.addSink(surfaceViewRenderer)

        // Audio constraints for low-latency and noise reduction enhancement
        val audioConstraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("googEchoCancellation", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("googAutoGainControl", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("googNoiseSuppression", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("googHighpassFilter", "true"))
        }
        audioSource = peerConnectionFactory!!.createAudioSource(audioConstraints)
        localAudioTrack = peerConnectionFactory!!.createAudioTrack("audio0", audioSource)

        isCapturing = true
        setupNewPeerConnection()
    }

    /**
     * Detaches the local video track from the SurfaceViewRenderer.
     * Use this when the app goes to the background to save GPU memory and prevent OOM crashes,
     * while keeping the camera and stream running for the remote viewer.
     */
    fun detachLocalPreview() {
        val renderer = lastRenderer ?: return
        localVideoTrack?.removeSink(renderer)
    }

    /**
     * Re-attaches the local video track to the SurfaceViewRenderer when the app returns to the foreground.
     */
    fun attachLocalPreview(renderer: SurfaceViewRenderer) {
        lastRenderer = renderer
        localVideoTrack?.addSink(renderer)
    }

    /**
     * Re-initializes media capture using the previously provided renderer.
     */
    fun restartCapture() {
        val renderer = lastRenderer ?: return
        stopLocalCapture()
        startLocalCapture(renderer)
    }

    /**
     * Stops current media capture and releases hardware resources without disposing the factory.
     */
    fun stopLocalCapture() {
        isCapturing = false
        
        try { videoCapturer?.stopCapture() } catch (e: Exception) {}
        videoCapturer?.dispose()
        videoCapturer = null
        
        surfaceTextureHelper?.dispose()
        surfaceTextureHelper = null
        
        localVideoTrack?.dispose()
        localVideoTrack = null
        
        videoSource?.dispose()
        videoSource = null
        
        localAudioTrack?.dispose()
        localAudioTrack = null
        
        audioSource?.dispose()
        audioSource = null
        
        peerConnection?.close()
        peerConnection?.dispose()
        peerConnection = null
    }

    fun setupNewPeerConnection() {
        peerConnection?.close()
        peerConnection?.dispose()
        peerConnection = null

        isLocalDescriptionSet = false
        isRemoteDescriptionSet = false

        synchronized(pendingLocalIceCandidates) { pendingLocalIceCandidates.clear() }
        synchronized(pendingRemoteIceCandidates) { pendingRemoteIceCandidates.clear() }

        peerConnection = buildPeerConnection()

        val dcInit = DataChannel.Init().apply { ordered = true }
        dataChannel = peerConnection?.createDataChannel("control", dcInit)
        dataChannel?.registerObserver(dataChannelObserver)

        localVideoTrack?.let { track ->
            val sender = peerConnection?.addTrack(track, listOf("stream0"))
            configureSender(sender)
        }
        localAudioTrack?.let { peerConnection?.addTrack(it, listOf("stream0")) }
        
        // Notify listener that a new connection has been established in NEW state
        mainHandler.post { listener.onConnectionStateChange(PeerConnection.PeerConnectionState.NEW) }
    }

    private fun configureSender(sender: RtpSender?) {
        if (sender == null) return
        val parameters = sender.parameters

        // Prioritize frame rate to ensure fluid movement during telepresence guidance
        parameters.degradationPreference = RtpParameters.DegradationPreference.MAINTAIN_FRAMERATE

        if (parameters.encodings.isNotEmpty()) {
            for (encoding in parameters.encodings) {
                when (deviceTier) {
                    3 -> {
                        encoding.minBitrateBps = 1500 * 1000
                        encoding.maxBitrateBps = 5000 * 1000
                    }
                    2 -> {
                        encoding.minBitrateBps = 800 * 1000
                        encoding.maxBitrateBps = 2500 * 1000
                    }
                    else -> {
                        encoding.minBitrateBps = 300 * 1000
                        encoding.maxBitrateBps = 800 * 1000
                    }
                }
                encoding.active = true
            }
        }
        sender.parameters = parameters
    }

    private fun buildPeerConnection(): PeerConnection? {
        val iceServers =
                listOf(
                        PeerConnection.IceServer.builder("stun:stun.l.google.com:19302")
                                .createIceServer()
                )
        val rtcConfig =
                PeerConnection.RTCConfiguration(iceServers).apply {
                    sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
                    continualGatheringPolicy =
                            PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
                    // Optimization for lower latency and efficient stream bundling
                    bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE
                    rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE
                }
        return peerConnectionFactory?.createPeerConnection(rtcConfig, peerConnectionObserver)
    }

    fun createOffer() {
        if (peerConnection == null || isDisposed) return

        val constraints =
                MediaConstraints().apply {
                    mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
                    mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
                }

        peerConnection?.createOffer(
                object : SdpObserver {
                    override fun onCreateSuccess(desc: SessionDescription) {
                        val bitrate =
                                when (deviceTier) {
                                    3 -> 5000
                                    2 -> 2500
                                    else -> 1000
                                }
                        val optimizedSdp = sdpWithBitrate(desc.description, bitrate)
                        val optimizedDesc = SessionDescription(desc.type, optimizedSdp)

                        peerConnection?.setLocalDescription(
                                object : SdpObserver {
                                    override fun onSetSuccess() {
                                        isLocalDescriptionSet = true
                                        listener.onLocalSdpCreated(optimizedDesc)
                                        drainLocalIceCandidates()
                                    }
                                    override fun onCreateSuccess(p0: SessionDescription?) {}
                                    override fun onCreateFailure(p0: String?) {}
                                    override fun onSetFailure(p0: String?) {}
                                },
                                optimizedDesc
                        )
                    }
                    override fun onSetSuccess() {}
                    override fun onCreateFailure(p0: String?) {}
                    override fun onSetFailure(p0: String?) {}
                },
                constraints
        )
    }

    /**
     * Injects bitrate limits into the SDP using robust regex parsing.
     * Starts with a low organic bitrate to prevent buffer overflow and eliminates initial latency.
     */
    private fun sdpWithBitrate(sdp: String, bitrateKbps: Int): String {
        var modifiedSdp = sdp
        
        // Find H264 payload type (usually 96, 102, or 125)
        val h264Pattern = Pattern.compile("a=rtpmap:(\\d+) H264/90000\r\n")
        val h264Matcher = h264Pattern.matcher(modifiedSdp)
        var h264Payload: String? = null
        
        if (h264Matcher.find()) {
            h264Payload = h264Matcher.group(1)
        }
        
        // Inject b=AS (Application Specific) max bitrate limit into video mid
        val videoMidPattern = Pattern.compile("(a=mid:video\r\n)")
        val videoMidMatcher = videoMidPattern.matcher(modifiedSdp)
        if (videoMidMatcher.find()) {
            modifiedSdp = videoMidMatcher.replaceFirst("$1b=AS:$bitrateKbps\r\n")
        }
        
        // Inject Google-specific limits to the H264 fmtp line
        if (h264Payload != null) {
            val fmtpPattern = Pattern.compile("(a=fmtp:$h264Payload[^\r\n]+)")
            val fmtpMatcher = fmtpPattern.matcher(modifiedSdp)
            if (fmtpMatcher.find()) {
                val originalFmtp = fmtpMatcher.group(1)
                // Clean any existing x-google flags to prevent duplicates
                val cleanedFmtp = originalFmtp!!.split(";").filterNot { it.contains("x-google-") }.joinToString(";")
                
                // Allow WebRTC to start at a low bitrate (300kbps) and scale up organically.
                // This eliminates the 3-second startup latency caused by forced high bitrates.
                val minBitrate = 100
                val startBitrate = 300 
                
                val newFmtp = "$cleanedFmtp;x-google-min-bitrate=$minBitrate;x-google-max-bitrate=$bitrateKbps;x-google-start-bitrate=$startBitrate"
                modifiedSdp = modifiedSdp.replace(originalFmtp, newFmtp)
            }
        }
        
        return modifiedSdp
    }

    fun setRemoteDescription(sdp: String, isOffer: Boolean) {
        val type = if (isOffer) SessionDescription.Type.OFFER else SessionDescription.Type.ANSWER
        peerConnection?.setRemoteDescription(
                object : SdpObserver {
                    override fun onSetSuccess() {
                        isRemoteDescriptionSet = true
                        drainRemoteIceCandidates()
                    }
                    override fun onCreateSuccess(p0: SessionDescription?) {}
                    override fun onCreateFailure(p0: String?) {}
                    override fun onSetFailure(p0: String?) {}
                },
                SessionDescription(type, sdp)
        )
    }

    fun addIceCandidate(candidate: IceCandidate) {
        if (isRemoteDescriptionSet) {
            peerConnection?.addIceCandidate(candidate)
        } else {
            synchronized(pendingRemoteIceCandidates) { pendingRemoteIceCandidates.add(candidate) }
        }
    }

    private fun drainLocalIceCandidates() {
        synchronized(pendingLocalIceCandidates) {
            pendingLocalIceCandidates.forEach { listener.onIceCandidate(it) }
            pendingLocalIceCandidates.clear()
        }
    }

    private fun drainRemoteIceCandidates() {
        synchronized(pendingRemoteIceCandidates) {
            pendingRemoteIceCandidates.forEach { peerConnection?.addIceCandidate(it) }
            pendingRemoteIceCandidates.clear()
        }
    }

    private val peerConnectionObserver =
            object : PeerConnection.Observer {
                override fun onIceCandidate(candidate: IceCandidate) {
                    mainHandler.post {
                        if (isLocalDescriptionSet) {
                            listener.onIceCandidate(candidate)
                        } else {
                            synchronized(pendingLocalIceCandidates) {
                                pendingLocalIceCandidates.add(candidate)
                            }
                        }
                    }
                }

                override fun onConnectionChange(newState: PeerConnection.PeerConnectionState) {
                    mainHandler.post { listener.onConnectionStateChange(newState) }
                }

                override fun onDataChannel(dc: DataChannel) {
                    if (dc.label() == "control") dc.registerObserver(dataChannelObserver)
                }

                override fun onIceConnectionChange(p0: PeerConnection.IceConnectionState?) {}
                override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {}
                override fun onRenegotiationNeeded() {}
                override fun onSignalingChange(p0: PeerConnection.SignalingState?) {}
                override fun onIceConnectionReceivingChange(p0: Boolean) {}
                override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) {}
                override fun onAddTrack(p0: RtpReceiver?, p1: Array<out MediaStream>?) {}
                override fun onAddStream(p0: MediaStream?) {}
                override fun onRemoveStream(p0: MediaStream?) {}
            }

    private val dataChannelObserver =
            object : DataChannel.Observer {
                override fun onMessage(buffer: DataChannel.Buffer) {
                    val bytes = ByteArray(buffer.data.remaining())
                    buffer.data.get(bytes)
                    listener.onCommandReceived(String(bytes))
                }
                override fun onBufferedAmountChange(p0: Long) {}
                override fun onStateChange() {}
            }

    fun dispose() {
        isDisposed = true
        stopLocalCapture()
        peerConnectionFactory?.dispose()
        lastRenderer = null
    }
}
