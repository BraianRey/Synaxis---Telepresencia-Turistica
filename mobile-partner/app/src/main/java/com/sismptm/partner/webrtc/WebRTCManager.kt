package com.sismptm.partner.webrtc

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.nio.ByteBuffer
import org.webrtc.*

/**
 * Manager for WebRTC operations on the Partner (broadcaster) side.
 *
 * Architecture:
 * - Uses Unified Plan (required by webrtc-sdk:android 144+)
 * - Camera capture via Camera2Enumerator (back camera preferred)
 * - Local video rendered on the provided SurfaceViewRenderer
 * - DataChannel created BEFORE offer (so it is included in the SDP)
 * - All SDP operations happen via callbacks — never blocking the UI thread
 *
 * Crash fixes applied vs previous version:
 * 1. PeerConnectionFactory.initialize() is now in a companion object (called once per process)
 * 2. EglBase is passed in from outside so lifecycle is controlled by the caller
 * 3. DataChannel is created right after PeerConnection (before offer)
 * 4. onRenegotiationNeeded handled explicitly to avoid duplicate offers
 * 5. videoCapturer is stopped safely before dispose()
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
                        /* enableIntelVp8Encoder = */ true,
                        /* enableH264HighProfile = */ false // H264 High can cause codec negotiation
                        // failures
                        )
        val decoderFactory = DefaultVideoDecoderFactory(eglBase.eglBaseContext)

        peerConnectionFactory =
                PeerConnectionFactory.builder()
                        .setVideoEncoderFactory(encoderFactory)
                        .setVideoDecoderFactory(decoderFactory)
                        .createPeerConnectionFactory()

        Log.d(TAG, "PeerConnectionFactory built")
    }

    /**
     * Initializes camera capture and binds video to the provided renderer. Call this BEFORE
     * [createOffer].
     *
     * @param surfaceViewRenderer Surface where the local camera preview will be shown.
     */
    fun startLocalCapture(surfaceViewRenderer: SurfaceViewRenderer) {
        // Init renderer with our EglBase context
        surfaceViewRenderer.init(eglBase.eglBaseContext, null)
        surfaceViewRenderer.setEnableHardwareScaler(true)
        surfaceViewRenderer.setMirror(true)

        // Build PeerConnection with standard STUN servers
        peerConnection = buildPeerConnection()

        // Create DataChannel BEFORE offer so it is included in the SDP negotiation
        // (In Unified Plan, DataChannel must exist before createOffer)
        val dcInit = DataChannel.Init().apply { ordered = true }
        dataChannel = peerConnection?.createDataChannel("control", dcInit)
        dataChannel?.registerObserver(dataChannelObserver)
        Log.d(TAG, "DataChannel 'control' created, state=${dataChannel?.state()}")

        // Setup video capture
        val capturer = createVideoCapturer()
        if (capturer == null) {
            Log.e(TAG, "No camera available on this device!")
            return
        }
        this.videoCapturer = capturer

        val videoSource = peerConnectionFactory!!.createVideoSource(capturer.isScreencast)
        surfaceTextureHelper =
                SurfaceTextureHelper.create(
                        "CaptureThread-${System.currentTimeMillis()}",
                        eglBase.eglBaseContext
                )
        capturer.initialize(surfaceTextureHelper, context, videoSource.capturerObserver)
        capturer.startCapture(1280, 720, 30)
        isCapturing = true
        Log.d(TAG, "Camera capture started at 1280x720@30fps")

        localVideoTrack = peerConnectionFactory!!.createVideoTrack("video0", videoSource)
        localVideoTrack?.addSink(surfaceViewRenderer)
        localVideoTrack?.setEnabled(true)

        // Setup audio
        val audioConstraints =
                MediaConstraints().apply {
                    mandatory.add(MediaConstraints.KeyValuePair("googEchoCancellation", "true"))
                    mandatory.add(MediaConstraints.KeyValuePair("googNoiseSuppression", "true"))
                }
        val audioSource = peerConnectionFactory!!.createAudioSource(audioConstraints)
        localAudioTrack = peerConnectionFactory!!.createAudioTrack("audio0", audioSource)
        localAudioTrack?.setEnabled(true)

        // Add tracks to PeerConnection (Unified Plan: each track in its own transceiver)
        peerConnection?.addTrack(localVideoTrack, listOf("stream0"))
        peerConnection?.addTrack(localAudioTrack, listOf("stream0"))

        Log.d(TAG, "Local tracks added to PeerConnection")
    }

    private fun buildPeerConnection(): PeerConnection? {
        val iceServers =
                listOf(
                        PeerConnection.IceServer.builder("stun:stun.l.google.com:19302")
                                .createIceServer(),
                        PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302")
                                .createIceServer()
                )
        val rtcConfig =
                PeerConnection.RTCConfiguration(iceServers).apply {
                    sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
                    continualGatheringPolicy =
                            PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
                    keyType = PeerConnection.KeyType.ECDSA
                }
        return peerConnectionFactory?.createPeerConnection(rtcConfig, peerConnectionObserver)
    }

    private val peerConnectionObserver =
            object : PeerConnection.Observer {
                override fun onIceCandidate(candidate: IceCandidate) {
                    // CRITICAL: wrap in Main thread - WebRTC calls this from signaling_thread
                    mainHandler.post {
                        Log.d(
                                TAG,
                                "ICE candidate: ${candidate.sdpMid} / ${candidate.sdp.take(60)}..."
                        )
                        listener.onIceCandidate(candidate)
                    }
                }

                override fun onConnectionChange(newState: PeerConnection.PeerConnectionState) {
                    // Wrap in Main thread
                    mainHandler.post {
                        Log.d(TAG, "Connection state → $newState")
                        listener.onConnectionStateChange(newState)
                    }
                }

                override fun onIceConnectionChange(state: PeerConnection.IceConnectionState) {
                    Log.d(TAG, "ICE connection → $state")
                }

                override fun onIceGatheringChange(state: PeerConnection.IceGatheringState) {
                    Log.d(TAG, "ICE gathering → $state")
                }

                override fun onDataChannel(dc: DataChannel) {
                    Log.d(TAG, "Remote DataChannel received: ${dc.label()}")
                    if (dc.label() == "control") {
                        dc.registerObserver(dataChannelObserver)
                    }
                }

                // onRenegotiationNeeded MUST be a no-op here: we control offer creation explicitly
                // Letting WebRTC fire renegotiation automatically causes duplicate offers & crashes
                override fun onRenegotiationNeeded() {
                    Log.d(TAG, "onRenegotiationNeeded — ignored (offer driven manually)")
                }

                override fun onSignalingChange(state: PeerConnection.SignalingState) {
                    Log.d(TAG, "Signaling state → $state")
                }

                override fun onIceConnectionReceivingChange(receiving: Boolean) {}
                override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>) {}

                @Deprecated("Deprecated in Unified Plan — use onTrack instead")
                override fun onAddStream(stream: MediaStream) {}

                @Deprecated("Deprecated in Unified Plan")
                override fun onRemoveStream(stream: MediaStream) {}

                override fun onAddTrack(
                        receiver: RtpReceiver,
                        mediaStreams: Array<out MediaStream>
                ) {
                    Log.d(TAG, "Remote track added: ${receiver.track()?.kind()}")
                }
            }

    private val dataChannelObserver =
            object : DataChannel.Observer {
                override fun onBufferedAmountChange(previousAmount: Long) {}
                override fun onStateChange() {
                    Log.d(TAG, "DataChannel state → ${dataChannel?.state()}")
                }
                override fun onMessage(buffer: DataChannel.Buffer) {
                    val bytes = ByteArray(buffer.data.remaining())
                    buffer.data.get(bytes)
                    val message = String(bytes, Charsets.UTF_8)
                    Log.d(TAG, "DataChannel command received: $message")
                    listener.onCommandReceived(message)
                }
            }

    /**
     * Creates an SDP offer to initiate P2P connection with the Client.
     *
     * After successful creation, [WebRTCListener.onLocalSdpCreated] is called with the offer. Safe
     * to call only once — subsequent calls will be ignored.
     */
    fun createOffer() {
        if (offerCreated) {
            Log.w(TAG, "createOffer() called again — skipping (offer already created)")
            return
        }

        if (peerConnection == null) {
            Log.e(TAG, "[ERROR] Cannot create offer - PeerConnection is null")
            return
        }

        offerCreated = true

        val constraints =
                MediaConstraints().apply {
                    mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "false"))
                    mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "false"))
                }

        peerConnection?.createOffer(
                object : SdpObserver {
                    override fun onCreateSuccess(desc: SessionDescription) {
                        Log.d(TAG, "[OK] Offer created - setting as local description")
                        peerConnection?.setLocalDescription(
                                object : SdpObserver {
                                    override fun onSetSuccess() {
                                        Log.d(
                                                TAG,
                                                "[OK] Local description (offer) set successfully"
                                        )
                                        listener.onLocalSdpCreated(desc)
                                    }
                                    override fun onCreateSuccess(p0: SessionDescription?) {}
                                    override fun onCreateFailure(error: String?) {
                                        Log.e(
                                                TAG,
                                                "[ERROR] setLocalDescription create failure: $error"
                                        )
                                    }
                                    override fun onSetFailure(error: String?) {
                                        Log.e(
                                                TAG,
                                                "[ERROR] setLocalDescription set failure: $error"
                                        )
                                        offerCreated = false // Allow retry
                                    }
                                },
                                desc
                        )
                    }
                    override fun onSetSuccess() {}
                    override fun onCreateFailure(error: String?) {
                        Log.e(TAG, "[ERROR] createOffer failure: $error")
                        offerCreated = false // Allow retry
                    }
                    override fun onSetFailure(error: String?) {
                        Log.e(TAG, "[ERROR] createOffer set failure")
                    }
                },
                constraints
        )
    }

    /**
     * Sets the remote SDP (answer from the Client).
     *
     * @param sdp The SDP string received from the remote peer.
     * @param isOffer true if the remote SDP is an offer, false if it is an answer.
     */
    fun setRemoteDescription(sdp: String, isOffer: Boolean) {
        if (peerConnection == null) {
            Log.e(TAG, "[ERROR] Cannot set remote description - PeerConnection is null")
            return
        }

        try {
            val type =
                    if (isOffer) SessionDescription.Type.OFFER else SessionDescription.Type.ANSWER
            val sessionDescription = SessionDescription(type, sdp)
            Log.d(TAG, "[OK] Setting remote description (type=${type.name})")

            peerConnection?.setRemoteDescription(
                    object : SdpObserver {
                        override fun onSetSuccess() {
                            Log.d(TAG, "[OK] Remote description (${type.name}) set successfully")
                        }
                        override fun onCreateSuccess(p0: SessionDescription?) {}
                        override fun onCreateFailure(error: String?) {
                            Log.e(TAG, "[ERROR] setRemoteDescription create failure: $error")
                        }
                        override fun onSetFailure(error: String?) {
                            Log.e(TAG, "[ERROR] setRemoteDescription set failure: $error")
                        }
                    },
                    sessionDescription
            )
        } catch (e: Exception) {
            Log.e(TAG, "[ERROR] Error setting remote description: ${e.message}", e)
        }
    }

    /**
     * Adds an ICE candidate from the remote peer.
     *
     * Validates candidate fields before adding to PeerConnection.
     *
     * @param candidate The ICE candidate to add
     */
    fun addIceCandidate(candidate: IceCandidate) {
        if (peerConnection == null) {
            Log.w(TAG, "[ERROR] Cannot add ICE candidate - PeerConnection is null")
            return
        }

        try {
            // Validate candidate before adding
            if (candidate.sdpMid == null || candidate.sdpMid.isEmpty()) {
                Log.w(TAG, "[WARN] Skipping ICE candidate with null/empty sdpMid")
                return
            }

            if (candidate.sdpMLineIndex < 0) {
                Log.w(
                        TAG,
                        "[WARN] Skipping ICE candidate with negative sdpMLineIndex: ${candidate.sdpMLineIndex}"
                )
                return
            }

            val sdpCandidate = candidate.sdp
            if (sdpCandidate == null || !sdpCandidate.startsWith("candidate:")) {
                Log.w(TAG, "[WARN] Skipping malformed ICE candidate SDP")
                return
            }

            Log.d(
                    TAG,
                    "Adding valid ICE candidate: ${candidate.sdpMid}[${candidate.sdpMLineIndex}]"
            )
            peerConnection?.addIceCandidate(candidate)
        } catch (e: Exception) {
            Log.e(TAG, "[ERROR] Error adding ICE candidate: ${e.message}", e)
        }
    }

    /**
     * Sends a text command to the remote peer via DataChannel.
     *
     * Command examples: "UP", "DOWN", "LEFT", "RIGHT" for drone/camera control. Only works when
     * DataChannel is open (after connection established).
     *
     * @param command The command string to send
     */
    fun sendCommand(command: String) {
        val dc = dataChannel ?: return
        if (dc.state() != DataChannel.State.OPEN) {
            Log.w(TAG, "DataChannel not open (state=${dc.state()}) — cannot send '$command'")
            return
        }
        val buffer = DataChannel.Buffer(ByteBuffer.wrap(command.toByteArray(Charsets.UTF_8)), false)
        dc.send(buffer)
        Log.d(TAG, "Command sent via DataChannel: $command")
    }

    private fun createVideoCapturer(): VideoCapturer? {
        val enumerator = Camera2Enumerator(context)
        // Prefer back camera (the "robot/partner" camera)
        for (deviceName in enumerator.deviceNames) {
            if (enumerator.isBackFacing(deviceName)) {
                val capturer = enumerator.createCapturer(deviceName, null)
                if (capturer != null) {
                    Log.d(TAG, "Using back camera: $deviceName")
                    return capturer
                }
            }
        }
        // Fallback to any available camera
        for (deviceName in enumerator.deviceNames) {
            val capturer = enumerator.createCapturer(deviceName, null)
            if (capturer != null) {
                Log.d(TAG, "Fallback camera: $deviceName")
                return capturer
            }
        }
        return null
    }

    /**
     * Disposes all WebRTC resources and releases native objects.
     *
     * Call this when done streaming to properly clean up:
     * - Camera capture
     * - Video and audio tracks
     * - PeerConnection
     * - DataChannel
     * - EGL context
     *
     * Should be called from the ViewModel's onCleared() method.
     */
    fun dispose() {
        Log.d(TAG, "Disposing WebRTCManager...")
        try {
            if (isCapturing) {
                videoCapturer?.stopCapture()
                isCapturing = false
            }
        } catch (e: InterruptedException) {
            Log.e(TAG, "Error stopping capture: ${e.message}")
        }
        videoCapturer?.dispose()
        surfaceTextureHelper?.dispose()
        dataChannel?.dispose()
        localVideoTrack?.dispose()
        localAudioTrack?.dispose()
        peerConnection?.dispose()
        peerConnectionFactory?.dispose()
        eglBase.release()
        Log.d(TAG, "WebRTCManager disposed [OK]")
    }
}
