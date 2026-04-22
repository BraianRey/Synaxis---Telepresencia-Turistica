package com.sismptm.partner.webrtc

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import org.webrtc.*

/**
 * Manager for WebRTC operations on the Partner (broadcaster) side.
 * Implements high resilience: Clean start on "join" and dual ICE candidate queuing.
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
                val options = PeerConnectionFactory.InitializationOptions.builder(context.applicationContext)
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
    private var videoCapturer: VideoCapturer? = null
    private var surfaceTextureHelper: SurfaceTextureHelper? = null
    private var dataChannel: DataChannel? = null
    private var isCapturing = false
    private var isDisposed = false

    @Volatile private var isLocalDescriptionSet = false
    @Volatile private var isRemoteDescriptionSet = false
    private val pendingLocalIceCandidates = mutableListOf<IceCandidate>()
    private val pendingRemoteIceCandidates = mutableListOf<IceCandidate>()
    private val mainHandler = Handler(Looper.getMainLooper())

    init {
        ensureInitialized(context)
        buildPeerConnectionFactory()
    }

    private fun buildPeerConnectionFactory() {
        val encoderFactory = DefaultVideoEncoderFactory(eglBase.eglBaseContext, false, true)
        val decoderFactory = DefaultVideoDecoderFactory(eglBase.eglBaseContext)

        peerConnectionFactory = PeerConnectionFactory.builder()
            .setVideoEncoderFactory(encoderFactory)
            .setVideoDecoderFactory(decoderFactory)
            .createPeerConnectionFactory()
    }

    fun startLocalCapture(surfaceViewRenderer: SurfaceViewRenderer) {
        if (isCapturing || isDisposed) return

        surfaceViewRenderer.init(eglBase.eglBaseContext, null)
        surfaceViewRenderer.setEnableHardwareScaler(true)

        val enumerator = Camera2Enumerator(context)
        val deviceName = enumerator.deviceNames.find { enumerator.isBackFacing(it) } ?: enumerator.deviceNames.first()
        val capturer = enumerator.createCapturer(deviceName, null)
        this.videoCapturer = capturer

        val videoSource = peerConnectionFactory!!.createVideoSource(capturer.isScreencast)
        surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", eglBase.eglBaseContext)
        capturer.initialize(surfaceTextureHelper, context, videoSource.capturerObserver)
        capturer.startCapture(1280, 720, 30)

        localVideoTrack = peerConnectionFactory!!.createVideoTrack("video0", videoSource)
        localVideoTrack?.addSink(surfaceViewRenderer)

        val audioSource = peerConnectionFactory!!.createAudioSource(MediaConstraints())
        localAudioTrack = peerConnectionFactory!!.createAudioTrack("audio0", audioSource)

        isCapturing = true
        setupNewPeerConnection()
    }

    /**
     * Requirement 2: Clean start for Reactive Negotiation.
     */
    fun setupNewPeerConnection() {
        Log.i(TAG, "Reactive Reset: Building a fresh PeerConnection session.")

        peerConnection?.close()
        peerConnection?.dispose()
        peerConnection = null

        isLocalDescriptionSet = false
        isRemoteDescriptionSet = false

        synchronized(pendingLocalIceCandidates) { pendingLocalIceCandidates.clear() }
        synchronized(pendingRemoteIceCandidates) { pendingRemoteIceCandidates.clear() }

        peerConnection = buildPeerConnection()

        // Create Control DataChannel
        val dcInit = DataChannel.Init().apply { ordered = true }
        dataChannel = peerConnection?.createDataChannel("control", dcInit)
        dataChannel?.registerObserver(dataChannelObserver)

        // Re-attach tracks
        localVideoTrack?.let { peerConnection?.addTrack(it, listOf("stream0")) }
        localAudioTrack?.let { peerConnection?.addTrack(it, listOf("stream0")) }
    }

    private fun buildPeerConnection(): PeerConnection? {
        val iceServers = listOf(
            PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer()
        )
        val rtcConfig = PeerConnection.RTCConfiguration(iceServers).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
        }
        return peerConnectionFactory?.createPeerConnection(rtcConfig, peerConnectionObserver)
    }

    fun createOffer() {
        if (peerConnection == null || isDisposed) return
        peerConnection?.createOffer(object : SdpObserver {
            override fun onCreateSuccess(desc: SessionDescription) {
                peerConnection?.setLocalDescription(object : SdpObserver {
                    override fun onSetSuccess() {
                        Log.i(TAG, "Local Description (Offer) set. Draining local candidates.")
                        isLocalDescriptionSet = true
                        listener.onLocalSdpCreated(desc)
                        drainLocalIceCandidates()
                    }
                    override fun onCreateSuccess(p0: SessionDescription?) {}
                    override fun onCreateFailure(p0: String?) {}
                    override fun onSetFailure(p0: String?) { Log.e(TAG, "Local SDP Error: $p0") }
                }, desc)
            }
            override fun onSetSuccess() {}
            override fun onCreateFailure(p0: String?) { Log.e(TAG, "Offer Creation Error: $p0") }
            override fun onSetFailure(p0: String?) {}
        }, MediaConstraints())
    }

    fun setRemoteDescription(sdp: String, isOffer: Boolean) {
        val type = if (isOffer) SessionDescription.Type.OFFER else SessionDescription.Type.ANSWER
        peerConnection?.setRemoteDescription(object : SdpObserver {
            override fun onSetSuccess() {
                Log.i(TAG, "Remote Description set. Draining remote candidates.")
                isRemoteDescriptionSet = true
                drainRemoteIceCandidates()
            }
            override fun onCreateSuccess(p0: SessionDescription?) {}
            override fun onCreateFailure(p0: String?) {}
            override fun onSetFailure(p0: String?) {}
        }, SessionDescription(type, sdp))
    }

    /**
     * Requirement 3: Handle incoming candidates reactively.
     */
    fun addIceCandidate(candidate: IceCandidate) {
        if (isRemoteDescriptionSet) {
            peerConnection?.addIceCandidate(candidate)
        } else {
            synchronized(pendingRemoteIceCandidates) {
                pendingRemoteIceCandidates.add(candidate)
            }
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

    private val peerConnectionObserver = object : PeerConnection.Observer {
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

    private val dataChannelObserver = object : DataChannel.Observer {
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
        isCapturing = false
        try { videoCapturer?.stopCapture() } catch (e: Exception) {}
        videoCapturer?.dispose()
        surfaceTextureHelper?.dispose()
        dataChannel?.dispose()
        localVideoTrack?.dispose()
        localAudioTrack?.dispose()
        peerConnection?.dispose()
        peerConnectionFactory?.dispose()
    }
}