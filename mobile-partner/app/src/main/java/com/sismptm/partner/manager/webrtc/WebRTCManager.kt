package com.sismptm.partner.manager.webrtc

import android.app.ActivityManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import org.webrtc.*

/**
 * Orchestrates WebRTC operations for broadcasting. Manages the peer connection lifecycle, hardware
 * media capture, and stream quality.
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

        return when {
            processors <= 4 || totalRamGb <= 3.0 -> 1
            processors <= 6 || totalRamGb <= 6.0 -> 2
            else -> 3
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
                surfaceViewRenderer.setFpsReduction(30f) // Reduce GPU overhead for local preview
            } catch (e: IllegalStateException) {
                Log.w(TAG, "SurfaceViewRenderer already initialized, skipping init")
            }
        }

        val enumerator = Camera2Enumerator(context)
        val deviceNames = enumerator.deviceNames
        val deviceName =
                deviceNames.find { enumerator.isBackFacing(it) }
                        ?: deviceNames.firstOrNull() ?: return

        val capturer = enumerator.createCapturer(deviceName, null)
        this.videoCapturer = capturer

        videoSource = peerConnectionFactory!!.createVideoSource(capturer.isScreencast)
        surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", eglBase.eglBaseContext)
        capturer.initialize(surfaceTextureHelper, context, videoSource!!.capturerObserver)

        when (deviceTier) {
            3 -> capturer.startCapture(1920, 1080, 30)
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

    private fun sdpWithBitrate(sdp: String, bitrateKbps: Int): String {
        val lines = sdp.split("\r\n").toMutableList()
        var videoLineIndex = -1
        for (i in lines.indices) {
            if (lines[i].startsWith("m=video")) {
                videoLineIndex = i
                break
            }
        }

        if (videoLineIndex == -1) return sdp

        // 1. Identify H264 payload to prioritize it in the media line
        var h264Payload: String? = null
        for (line in lines) {
            if (line.startsWith("a=rtpmap:") && line.contains("H264/90000")) {
                h264Payload = line.substringBefore(" ").substringAfter(":")
                break
            }
        }

        if (h264Payload != null) {
            val mVideoLine = lines[videoLineIndex]
            val parts = mVideoLine.split(" ").toMutableList()
            if (parts.size > 3) {
                parts.remove(h264Payload)
                parts.add(3, h264Payload) // Move H264 payload to the first preference position
                lines[videoLineIndex] = parts.joinToString(" ")
            }
        }

        // 2. Rebuild SDP cleaning previous bitrate and proprietary flags
        val minBitrate = 300 // Safe minimum for real-time guidance as expected by Client
        val startBitrate = (bitrateKbps * 0.8).toInt()
        val resultLines = mutableListOf<String>()
        var inVideoSection = false

        for (line in lines) {
            if (line.startsWith("m=video")) inVideoSection = true
            else if (line.startsWith("m=audio")) inVideoSection = false

            // Filter out existing bitrate lines in the video section
            if (inVideoSection && line.startsWith("b=AS:")) continue

            // Clean previous Google-specific latency flags from fmtp line
            if (h264Payload != null && line.startsWith("a=fmtp:$h264Payload")) {
                val cleanedFmtp = line.split(";").filterNot { it.contains("x-google-") }.joinToString(";")
                resultLines.add("$cleanedFmtp;x-google-min-bitrate=$minBitrate;x-google-max-bitrate=$bitrateKbps;x-google-start-bitrate=$startBitrate")
                continue
            }

            resultLines.add(line)

            // Inject optimized bitrate settings for the video track
            if (line.startsWith("a=mid:video")) {
                resultLines.add("b=AS:$bitrateKbps")
            }
        }

        return resultLines.joinToString("\r\n")
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
