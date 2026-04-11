package com.sismptm.partner.webrtc

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.nio.ByteBuffer
import org.webrtc.*

/**
 * Manager for WebRTC operations on the Partner (broadcaster) side.
 * - VR-Optimized: Prioritizes FrameRate over Resolution (MAINTAIN_FRAMERATE) to prevent motion sickness.
 * - International-Ready: Redundant STUN servers and optimized ICE pings for long routes (e.g., Colombia-Japan).
 * - High-Fidelity: 5 Mbps bitrate ceiling for clear 1080p/60fps detail.
 * - Audio-Priority: Enhanced constraints for clear voice instructions.
 * - Industrial Safety: Strict lifecycle guards (isDisposed) to prevent crashes and memory leaks.
 */
class WebRTCManager(
        private val context: Context,
        private val listener: WebRTCListener,
        private val eglBase: EglBase = EglBase.create()
) {
    companion object {
        private const val TAG = "WebRTCManager"
        private var isFactoryInitialized = false

        /** Must be called once per process. Safe to call multiple times. */
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
                Log.d(TAG, "PeerConnectionFactory initialized")
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
    private var videoCapturer: VideoCapturer? = null
    private var surfaceTextureHelper: SurfaceTextureHelper? = null
    private var dataChannel: DataChannel? = null
    private var isCapturing = false
    private var isDisposed = false

    /** Guard to avoid creating multiple offers if onRenegotiationNeeded fires repeatedly */
    @Volatile private var offerCreated = false

    // Handler for thread marshalling - all WebRTC operations must run on Main thread
    private val mainHandler = Handler(Looper.getMainLooper())

    init {
        ensureInitialized(context)
        buildPeerConnectionFactory()
    }

    private fun buildPeerConnectionFactory() {
        val encoderFactory =
                DefaultVideoEncoderFactory(
                        eglBase.eglBaseContext,
                        /* enableIntelVp8Encoder = */ false, // Optimization: Avoid legacy software path
                        /* enableH264HighProfile = */ true   // Better efficiency for 1080p
                )
        val decoderFactory = DefaultVideoDecoderFactory(eglBase.eglBaseContext)

        peerConnectionFactory =
                PeerConnectionFactory.builder()
                        .setVideoEncoderFactory(encoderFactory)
                        .setVideoDecoderFactory(decoderFactory)
                        .setOptions(PeerConnectionFactory.Options().apply {
                            disableEncryption = false
                            disableNetworkMonitor = false
                        })
                        .createPeerConnectionFactory()

        Log.d(TAG, "PeerConnectionFactory built [Final Edition]")
    }

    /**
     * Initializes camera capture with dynamic resolution and binds video to the renderer.
     * 
     * Optimizations:
     * - Protection against double initialization.
     * - Dynamic FPS (chooses 60fps if available for VR fluidity).
     * - Enhanced audio constraints for clear voice.
     */
    fun startLocalCapture(surfaceViewRenderer: SurfaceViewRenderer) {
        if (isCapturing || isDisposed) {
            Log.w(TAG, "Capture already running or manager disposed. Ignoring.")
            return
        }

        // Init renderer with our EglBase context
        surfaceViewRenderer.init(eglBase.eglBaseContext, null)
        surfaceViewRenderer.setEnableHardwareScaler(true)
        surfaceViewRenderer.setMirror(true)

        // Build PeerConnection with international-optimized ICE config
        peerConnection = buildPeerConnection()

        // Create DataChannel BEFORE offer so it is included in the SDP negotiation
        val dcInit = DataChannel.Init().apply { ordered = true }
        dataChannel = peerConnection?.createDataChannel("control", dcInit)
        dataChannel?.registerObserver(dataChannelObserver)
        Log.d(TAG, "DataChannel 'control' created")

        // Setup video capture
        val enumerator = Camera2Enumerator(context)
        val deviceName = enumerator.deviceNames.find { enumerator.isBackFacing(it) } 
                         ?: enumerator.deviceNames.firstOrNull() ?: return

        val capturer = enumerator.createCapturer(deviceName, null)
        this.videoCapturer = capturer

        // 2026 Logic: Pick highest resolution up to 1080p, prioritizing high framerate
        val formats = enumerator.getSupportedFormats(deviceName)
        val bestFormat = formats?.filter { it.width <= 1920 && it.height <= 1080 }
            ?.sortedWith(compareByDescending<CameraEnumerationAndroid.CaptureFormat> { it.width * it.height }
                .thenByDescending { it.framerate.max })
            ?.firstOrNull() ?: formats?.maxByOrNull { it.width * it.height }

        val width = bestFormat?.width ?: 1280
        val height = bestFormat?.height ?: 720
        val fps = (bestFormat?.framerate?.max ?: 30000) / 1000

        val videoSource = peerConnectionFactory!!.createVideoSource(capturer.isScreencast)
        surfaceTextureHelper =
                SurfaceTextureHelper.create(
                        "CaptureThread-${System.currentTimeMillis()}",
                        eglBase.eglBaseContext
                )
        capturer.initialize(surfaceTextureHelper, context, videoSource.capturerObserver)
        capturer.startCapture(width, height, fps)
        isCapturing = true
        Log.d(TAG, "Capture started: ${width}x${height} @${fps}fps")

        localVideoTrack = peerConnectionFactory!!.createVideoTrack("video0", videoSource)
        localVideoTrack?.addSink(surfaceViewRenderer)

        // Setup audio with enhanced constraints for telepresence
        val audioConstraints =
                MediaConstraints().apply {
                    mandatory.add(MediaConstraints.KeyValuePair("googEchoCancellation", "true"))
                    mandatory.add(MediaConstraints.KeyValuePair("googNoiseSuppression", "true"))
                    mandatory.add(MediaConstraints.KeyValuePair("googHighpassFilter", "true"))
                    mandatory.add(MediaConstraints.KeyValuePair("googAudioMirroring", "false"))
                }
        val audioSource = peerConnectionFactory!!.createAudioSource(audioConstraints)
        localAudioTrack = peerConnectionFactory!!.createAudioTrack("audio0", audioSource)

        // Add tracks to PeerConnection
        peerConnection?.addTrack(localVideoTrack, listOf("stream0"))
        peerConnection?.addTrack(localAudioTrack, listOf("stream0"))
    }

    private fun buildPeerConnection(): PeerConnection? {
        // Redundant STUN for global reliability (Colombia - Japan)
        val iceServers =
                listOf(
                        PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
                        PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer(),
                        PeerConnection.IceServer.builder("stun:stun2.l.google.com:19302").createIceServer()
                )
        val rtcConfig =
                PeerConnection.RTCConfiguration(iceServers).apply {
                    sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
                    continualGatheringPolicy =
                            PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
                    keyType = PeerConnection.KeyType.ECDSA
                    // Detect network route changes faster for long-distance stability
                    iceBackupCandidatePairPingInterval = 2000
                    iceConnectionReceivingTimeout = 5000
                }
        return peerConnectionFactory?.createPeerConnection(rtcConfig, peerConnectionObserver)
    }

    /**
     * VR-Ready Strategy: MAINTAIN_FRAMERATE is crucial to prevent nausea.
     * High bitrate floor (500kbps) and ceiling (5Mbps) for 1080p clarity.
     */
    private fun applyVideoBitrateLimits() {
        if (isDisposed) return
        mainHandler.post {
            val pc = peerConnection ?: return@post
            val senders = pc.senders ?: return@post
            for (sender in senders) {
                val track = sender.track() ?: continue
                if (track.kind() == "video") {
                    val parameters = sender.parameters
                    
                    // PREVENCION DE MAREO: Sacrificar resolución antes que FPS.
                    parameters.degradationPreference = RtpParameters.DegradationPreference.MAINTAIN_FRAMERATE
                    
                    for (encoding in parameters.encodings) {
                        encoding.maxBitrateBps = 5_000_000 // 5 Mbps
                        encoding.minBitrateBps = 500_000   // 500 kbps stable floor
                    }
                    sender.parameters = parameters
                    Log.d(TAG, "Adaptive VR-Ready bitrate applied: 5Mbps Max")
                }
            }
        }
    }

    private val peerConnectionObserver =
            object : PeerConnection.Observer {
                override fun onIceCandidate(candidate: IceCandidate) {
                    if (!isDisposed) {
                        mainHandler.post {
                            listener.onIceCandidate(candidate)
                        }
                    }
                }

                override fun onConnectionChange(newState: PeerConnection.PeerConnectionState) {
                    if (!isDisposed) {
                        mainHandler.post {
                            Log.d(TAG, "Connection state → $newState")
                            listener.onConnectionStateChange(newState)
                        }
                    }
                }

                override fun onIceConnectionChange(state: PeerConnection.IceConnectionState) {}
                override fun onIceGatheringChange(state: PeerConnection.IceGatheringState) {}
                override fun onDataChannel(dc: DataChannel) {
                    if (!isDisposed && dc.label() == "control") {
                        dc.registerObserver(dataChannelObserver)
                    }
                }
                override fun onRenegotiationNeeded() {}
                override fun onSignalingChange(state: PeerConnection.SignalingState) {}
                override fun onIceConnectionReceivingChange(receiving: Boolean) {}
                override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>) {}
                override fun onAddTrack(receiver: RtpReceiver, mediaStreams: Array<out MediaStream>) {}
                override fun onAddStream(stream: MediaStream) {}
                override fun onRemoveStream(stream: MediaStream) {}
            }

    private val dataChannelObserver =
            object : DataChannel.Observer {
                override fun onBufferedAmountChange(previousAmount: Long) {}
                override fun onStateChange() {}
                override fun onMessage(buffer: DataChannel.Buffer) {
                    if (!isDisposed) {
                        val bytes = ByteArray(buffer.data.remaining())
                        buffer.data.get(bytes)
                        val message = String(bytes, Charsets.UTF_8)
                        // This app is the streamer, so it RECEIVES instructions here
                        listener.onCommandReceived(message)
                    }
                }
            }

    fun createOffer() {
        if (offerCreated || peerConnection == null || isDisposed) return
        offerCreated = true

        peerConnection?.createOffer(
                object : SdpObserver {
                    override fun onCreateSuccess(desc: SessionDescription) {
                        if (isDisposed) return
                        peerConnection?.setLocalDescription(
                                object : SdpObserver {
                                    override fun onSetSuccess() {
                                        listener.onLocalSdpCreated(desc)
                                    }
                                    override fun onCreateSuccess(p0: SessionDescription?) {}
                                    override fun onCreateFailure(error: String?) {}
                                    override fun onSetFailure(error: String?) {
                                        offerCreated = false
                                    }
                                },
                                desc
                        )
                    }
                    override fun onSetSuccess() {}
                    override fun onCreateFailure(error: String?) {
                        offerCreated = false
                    }
                    override fun onSetFailure(error: String?) {}
                },
                MediaConstraints()
        )
    }

    fun setRemoteDescription(sdp: String, isOffer: Boolean) {
        val pc = peerConnection ?: return
        val type = if (isOffer) SessionDescription.Type.OFFER else SessionDescription.Type.ANSWER
        pc.setRemoteDescription(
                object : SdpObserver {
                    override fun onSetSuccess() {
                        if (!isOffer && !isDisposed) {
                            applyVideoBitrateLimits()
                        }
                    }
                    override fun onCreateSuccess(p0: SessionDescription?) {}
                    override fun onCreateFailure(error: String?) {}
                    override fun onSetFailure(error: String?) {}
                },
                SessionDescription(type, sdp)
        )
    }

    fun addIceCandidate(candidate: IceCandidate) {
        if (!isDisposed) {
            peerConnection?.addIceCandidate(candidate)
        }
    }

    /**
     * Sends a message via DataChannel.
     */
    fun sendCommand(command: String) {
        val dc = dataChannel ?: return
        if (dc.state() != DataChannel.State.OPEN || isDisposed) return
        val buffer = DataChannel.Buffer(ByteBuffer.wrap(command.toByteArray(Charsets.UTF_8)), false)
        dc.send(buffer)
    }

    /**
     * Comprehensive disposal to prevent memory leaks and hardware locks.
     */
    fun dispose() {
        if (isDisposed) return
        isDisposed = true
        isCapturing = false
        Log.d(TAG, "Disposing WebRTCManager [Final Edition]...")

        try { videoCapturer?.stopCapture() } catch (e: Exception) {}
        
        videoCapturer?.dispose()
        videoCapturer = null
        
        surfaceTextureHelper?.dispose()
        surfaceTextureHelper = null
        
        dataChannel?.dispose()
        dataChannel = null
        
        localVideoTrack?.dispose()
        localVideoTrack = null
        
        localAudioTrack?.dispose()
        localAudioTrack = null
        
        peerConnection?.dispose()
        peerConnection = null
        
        peerConnectionFactory?.dispose()
        peerConnectionFactory = null

        eglBase.release()
    }
}
