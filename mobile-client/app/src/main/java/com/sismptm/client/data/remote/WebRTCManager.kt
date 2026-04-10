package com.sismptm.client.data.remote

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.nio.ByteBuffer
import org.webrtc.*

/**
 * Manager for WebRTC PeerConnection on the Client (viewer) side.
 *
 * Role: receiver / answerer.
 * - Does NOT create an offer
 * - Receives offer from Partner → creates answer → sends back
 * - Receives remote video track and delivers via [onRemoteTrack]
 * - Sends directional commands to Partner via DataChannel
 *
 * Crash fixes vs previous version:
 * 1. PeerConnectionFactory.initialize() called only once per process (companion object guard)
 * 2. Unified Plan explicitly enabled (required by webrtc-sdk:android 144+)
 * 3. Remote video received via onTrack (not deprecated onAddStream)
 * 4. DataChannel is NOT created by the client — it's received via onDataChannel()
 * ```
 *     (Partner creates it; Client just registers an observer on receipt)
 * ```
 * 5. dispose() properly closes all resources in order
 */
class WebRTCManager(
        private val context: Context,
        private val eglBase: EglBase,
        private val onRemoteTrack: (VideoTrack) -> Unit
) {
    companion object {
        private const val TAG = "ClientWebRTCManager"
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
                Log.d(TAG, "PeerConnectionFactory initialized")
            }
        }
    }

    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null
    /// DataChannel received from Partner via onDataChannel callback
    private var dataChannel: DataChannel? = null

    /// Handler for thread marshalling - all WebRTC operations must run on Main thread
    /// to ensure thread safety with Android UI framework
    private val mainHandler = Handler(Looper.getMainLooper())

    /// Buffer for ICE candidates that arrive before remote SDP description is set
    /// This prevents race conditions during connection establishment
    private val pendingCandidates = mutableListOf<IceCandidate>()
    private var remoteDescriptionSet = false

    /// Callback to notify about newly discovered ICE candidates
    /// Invoked after remote description is set to avoid synchronization issues
    private var onIceCandidateCallback: ((IceCandidate) -> Unit)? = null

    init {
        ensureInitialized(context)

        val encoderFactory = DefaultVideoEncoderFactory(eglBase.eglBaseContext, true, false)
        val decoderFactory = DefaultVideoDecoderFactory(eglBase.eglBaseContext)

        peerConnectionFactory =
                PeerConnectionFactory.builder()
                        .setVideoEncoderFactory(encoderFactory)
                        .setVideoDecoderFactory(decoderFactory)
                        .createPeerConnectionFactory()

        Log.d(TAG, "PeerConnectionFactory built")
    }

    fun createPeerConnection() {
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

        peerConnection =
                peerConnectionFactory?.createPeerConnection(
                        rtcConfig,
                        object : PeerConnection.Observer {
                            override fun onIceCandidate(candidate: IceCandidate) {
                                /// CRITICAL: Wrap in Main thread - WebRTC calls this from
                                // signaling_thread
                                /// Main thread required for thread-safe UI access
                                mainHandler.post { addIceCandidate(candidate) }
                            }

                            override fun onConnectionChange(
                                    state: PeerConnection.PeerConnectionState
                            ) {
                                Log.d(TAG, "Connection state → $state")
                            }

                            override fun onIceConnectionChange(
                                    state: PeerConnection.IceConnectionState
                            ) {
                                Log.d(TAG, "ICE connection → $state")
                            }

                            /// KEY FIX: Use onTrack (Unified Plan) instead of deprecated
                            // onAddStream
                            override fun onTrack(transceiver: RtpTransceiver) {
                                /// Wrap in Main thread for safe UI access
                                mainHandler.post {
                                    val track = transceiver.receiver.track()
                                    Log.d(
                                            TAG,
                                            "Remote track received: kind=${track?.kind()}, enabled=${track?.enabled()}"
                                    )
                                    if (track is VideoTrack) {
                                        track.setEnabled(true)
                                        onRemoteTrack(track)
                                    }
                                }
                            }

                            /// DataChannel created by Partner is received here
                            override fun onDataChannel(dc: DataChannel) {
                                Log.d(TAG, "DataChannel received from Partner: label=${dc.label()}")
                                if (dc.label() == "control") {
                                    dataChannel = dc
                                    dc.registerObserver(
                                            object : DataChannel.Observer {
                                                override fun onBufferedAmountChange(
                                                        previousAmount: Long
                                                ) {}
                                                override fun onStateChange() {
                                                    Log.d(TAG, "DataChannel state → ${dc.state()}")
                                                }
                                                override fun onMessage(buffer: DataChannel.Buffer) {
                                                    /// Client receives instructions from Partner
                                                    /// Future use for bidirectional control
                                                }
                                            }
                                    )
                                }
                            }

                            override fun onRenegotiationNeeded() {
                                Log.d(TAG, "onRenegotiationNeeded — ignored (answer-only role)")
                            }

                            override fun onSignalingChange(state: PeerConnection.SignalingState) {
                                Log.d(TAG, "Signaling → $state")
                            }

                            override fun onIceConnectionReceivingChange(receiving: Boolean) {}
                            override fun onIceGatheringChange(
                                    state: PeerConnection.IceGatheringState
                            ) {}
                            override fun onIceCandidatesRemoved(
                                    candidates: Array<out IceCandidate>
                            ) {}

                            @Deprecated("Deprecated in Unified Plan")
                            override fun onAddStream(stream: MediaStream) {}

                            @Deprecated("Deprecated in Unified Plan")
                            override fun onRemoveStream(stream: MediaStream) {}

                            override fun onAddTrack(
                                    receiver: RtpReceiver,
                                    mediaStreams: Array<out MediaStream>
                            ) {}

                            // Missing methods in modern WebRTC Observer
                            override fun onRemoveTrack(receiver: RtpReceiver) {}
                        }
                )

        Log.d(TAG, "PeerConnection created (Unified Plan)")
    }

    /**
     * Registers a callback to be invoked when a new ICE candidate is discovered.
     *
     * @param callback Function to invoke with each discovered ICE candidate
     */
    fun setOnIceCandidateCallback(callback: (IceCandidate) -> Unit) {
        this.onIceCandidateCallback = callback
        Log.d(TAG, "ICE candidate callback registered")
    }

    /**
     * Sends a directional command to the Partner via DataChannel.
     *
     * Command examples: "UP", "DOWN", "LEFT", "RIGHT" for drone control. Only works after
     * DataChannel is opened by the Partner.
     *
     * @param command Command string to send
     */
    fun sendCommand(command: String) {
        val dc = dataChannel
        if (dc == null || dc.state() != DataChannel.State.OPEN) {
            Log.w(TAG, "DataChannel not open — cannot send '$command'")
            return
        }
        val buffer = DataChannel.Buffer(ByteBuffer.wrap(command.toByteArray(Charsets.UTF_8)), false)
        dc.send(buffer)
        Log.d(TAG, "Command sent: $command")
    }

    /**
     * Handles an incoming SDP offer from the remote peer.
     *
     * Sets the remote description and automatically processes any pending ICE candidates that
     * arrived before this point.
     *
     * @param sdp Session Description Protocol string from remote peer
     * @param observer Callback to handle success/failure of SDP setting
     */
    fun handleOffer(sdp: String, observer: SdpObserver) {
        try {
            if (peerConnection == null) {
                Log.e(TAG, "Cannot set offer — PeerConnection is null")
                return
            }
            Log.d(TAG, "Setting remote description (offer)")
            val desc = SessionDescription(SessionDescription.Type.OFFER, sdp)
            peerConnection?.setRemoteDescription(
                    object : SdpObserver {
                        override fun onSetSuccess() {
                            remoteDescriptionSet = true
                            Log.d(TAG, "Remote description (OFFER) set successfully")
                            observer.onSetSuccess()

                            /// Process any pending ICE candidates that arrived before
                            /// remote description was set
                            processPendingCandidates()
                        }
                        override fun onSetFailure(error: String?) {
                            Log.e(TAG, "Failed to set remote description: $error")
                            observer.onSetFailure(error)
                        }
                        override fun onCreateSuccess(p0: SessionDescription?) {}
                        override fun onCreateFailure(p0: String?) {}
                    },
                    desc
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error handling offer: ${e.message}", e)
        }
    }

    /**
     * Processes all pending ICE candidates after remote description is set.
     *
     * This prevents the race condition where ICE candidates arrive before the remote SDP is
     * available for processing.
     */
    private fun processPendingCandidates() {
        synchronized(pendingCandidates) {
            Log.d(TAG, "Processing ${pendingCandidates.size} pending ICE candidates")
            for (candidate in pendingCandidates) {
                try {
                    peerConnection?.addIceCandidate(candidate)
                    Log.d(TAG, "Added pending candidate: ${candidate.sdpMid}")
                    /// Notify callback that this candidate should be sent to partner
                    onIceCandidateCallback?.invoke(candidate)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to add pending candidate: ${e.message}")
                }
            }
            pendingCandidates.clear()
        }
    }

    /**
     * Creates an SDP answer in response to a received offer.
     *
     * This is the answerer role in the WebRTC connection flow.
     *
     * @param observer Callback to handle success/failure of answer creation
     */
    fun createAnswer(observer: SdpObserver) {
        try {
            if (peerConnection == null) {
                Log.e(TAG, "Cannot create answer — PeerConnection is null")
                return
            }
            Log.d(TAG, "Creating answer")
            peerConnection?.createAnswer(observer, MediaConstraints())
        } catch (e: Exception) {
            Log.e(TAG, "Error creating answer: ${e.message}", e)
        }
    }

    /**
     * Sets the local SDP description (the answer) after creation.
     *
     * @param sdp Session Description Protocol object to set as local description
     * @param observer Callback to handle success/failure of setting description
     */
    fun setLocalDescription(sdp: SessionDescription, observer: SdpObserver) {
        try {
            if (peerConnection == null) {
                Log.e(TAG, "Cannot set local description — PeerConnection is null")
                return
            }
            Log.d(TAG, "Setting local description (answer)")
            peerConnection?.setLocalDescription(observer, sdp)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting local description: ${e.message}", e)
        }
    }

    /**
     * Adds an ICE candidate for connection establishment.
     *
     * If remote description is not yet set, the candidate is buffered and processed later when
     * setRemoteDescription completes. This prevents adding candidates before the PeerConnection is
     * ready.
     *
     * @param candidate The ICE candidate to add
     */
    fun addIceCandidate(candidate: IceCandidate) {
        if (peerConnection == null) {
            Log.w(TAG, "Cannot add ICE candidate — PeerConnection is null")
            return
        }

        try {
            /// Validate candidate before adding
            if (candidate.sdpMid == null || candidate.sdpMid.isEmpty()) {
                Log.w(TAG, "Skipping ICE candidate with null/empty sdpMid")
                return
            }

            if (candidate.sdpMLineIndex < 0) {
                Log.w(
                        TAG,
                        "Skipping ICE candidate with negative sdpMLineIndex: ${candidate.sdpMLineIndex}"
                )
                return
            }

            val sdpCandidate = candidate.sdp
            if (sdpCandidate == null || !sdpCandidate.startsWith("candidate:")) {
                Log.w(TAG, "Skipping malformed ICE candidate SDP")
                return
            }

            /// If remote description is not set yet, buffer the candidate
            if (!remoteDescriptionSet) {
                Log.d(
                        TAG,
                        "Remote description not set yet — buffering candidate ${candidate.sdpMid}[${candidate.sdpMLineIndex}]"
                )
                synchronized(pendingCandidates) { pendingCandidates.add(candidate) }
                return
            }

            Log.d(
                    TAG,
                    "Adding valid ICE candidate: ${candidate.sdpMid}[${candidate.sdpMLineIndex}]"
            )
            /// Add to PeerConnection and notify callback to send to partner
            peerConnection?.addIceCandidate(candidate)
            onIceCandidateCallback?.invoke(candidate)
        } catch (e: Exception) {
            Log.e(TAG, "Error adding ICE candidate: ${e.message}", e)
        }
    }

    /**
     * Closes and disposes of all WebRTC resources.
     *
     * Call this when disconnecting from the remote peer to ensure proper cleanup and release of
     * native resources.
     */
    fun close() {
        Log.d(TAG, "Closing WebRTCManager...")
        dataChannel?.close()
        dataChannel?.dispose()
        peerConnection?.dispose()
        peerConnectionFactory?.dispose()
        Log.d(TAG, "WebRTCManager closed")
    }
}
