package com.sismptm.client.data.remote

import android.content.Context
import android.util.Log
import org.webrtc.*
import java.nio.ByteBuffer

/**
 * Manager for WebRTC PeerConnection on the Client (viewer) side.
 *
 * Role: receiver / answerer.
 *  - Does NOT create an offer
 *  - Receives offer from Partner → creates answer → sends back
 *  - Receives remote video track and delivers via [onRemoteTrack]
 *  - Sends directional commands to Partner via DataChannel
 *
 * Crash fixes vs previous version:
 *  1. PeerConnectionFactory.initialize() called only once per process (companion object guard)
 *  2. Unified Plan explicitly enabled (required by webrtc-sdk:android 144+)
 *  3. Remote video received via onTrack (not deprecated onAddStream)
 *  4. DataChannel is NOT created by the client — it's received via onDataChannel()
 *     (Partner creates it; Client just registers an observer on receipt)
 *  5. dispose() properly closes all resources in order
 */
class WebRTCManager(
    private val context: Context,
    private val eglBase: EglBase,
    private val onRemoteTrack: (VideoTrack) -> Unit,
    private val onIceCandidate: (IceCandidate) -> Unit
) {
    companion object {
        private const val TAG = "ClientWebRTCManager"
        private var isFactoryInitialized = false

        fun ensureInitialized(context: Context) {
            if (!isFactoryInitialized) {
                val options = PeerConnectionFactory.InitializationOptions
                    .builder(context.applicationContext)
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
    private var dataChannel: DataChannel? = null  // Received from Partner via onDataChannel

    init {
        ensureInitialized(context)

        val encoderFactory = DefaultVideoEncoderFactory(eglBase.eglBaseContext, true, false)
        val decoderFactory = DefaultVideoDecoderFactory(eglBase.eglBaseContext)

        peerConnectionFactory = PeerConnectionFactory.builder()
            .setVideoEncoderFactory(encoderFactory)
            .setVideoDecoderFactory(decoderFactory)
            .createPeerConnectionFactory()

        Log.d(TAG, "PeerConnectionFactory built")
    }

    fun createPeerConnection() {
        val iceServers = listOf(
            PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer()
        )
        val rtcConfig = PeerConnection.RTCConfiguration(iceServers).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
            keyType = PeerConnection.KeyType.ECDSA
        }

        peerConnection = peerConnectionFactory?.createPeerConnection(rtcConfig, object : PeerConnection.Observer {
            override fun onIceCandidate(candidate: IceCandidate) {
                Log.d(TAG, "ICE candidate: ${candidate.sdpMid}")
                onIceCandidate(candidate)
            }

            override fun onConnectionChange(state: PeerConnection.PeerConnectionState) {
                Log.d(TAG, "Connection state → $state")
            }

            override fun onIceConnectionChange(state: PeerConnection.IceConnectionState) {
                Log.d(TAG, "ICE connection → $state")
            }

            // KEY FIX: use onTrack (Unified Plan) instead of deprecated onAddStream
            override fun onTrack(transceiver: RtpTransceiver) {
                val track = transceiver.receiver.track()
                Log.d(TAG, "Remote track received: kind=${track?.kind()}, enabled=${track?.enabled()}")
                if (track is VideoTrack) {
                    track.setEnabled(true)
                    onRemoteTrack(track)
                }
            }

            // DataChannel created by Partner is received here
            override fun onDataChannel(dc: DataChannel) {
                Log.d(TAG, "DataChannel received from Partner: label=${dc.label()}")
                if (dc.label() == "control") {
                    dataChannel = dc
                    dc.registerObserver(object : DataChannel.Observer {
                        override fun onBufferedAmountChange(previousAmount: Long) {}
                        override fun onStateChange() {
                            Log.d(TAG, "DataChannel state → ${dc.state()}")
                        }
                        override fun onMessage(buffer: DataChannel.Buffer) {
                            // Client receives instructions from Partner (future use)
                        }
                    })
                }
            }

            override fun onRenegotiationNeeded() {
                Log.d(TAG, "onRenegotiationNeeded — ignored (answer-only role)")
            }

            override fun onSignalingChange(state: PeerConnection.SignalingState) {
                Log.d(TAG, "Signaling → $state")
            }

            override fun onIceConnectionReceivingChange(receiving: Boolean) {}
            override fun onIceGatheringChange(state: PeerConnection.IceGatheringState) {}
            override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>) {}

            @Deprecated("Deprecated in Unified Plan")
            override fun onAddStream(stream: MediaStream) {}

            @Deprecated("Deprecated in Unified Plan")
            override fun onRemoveStream(stream: MediaStream) {}

            override fun onAddTrack(receiver: RtpReceiver, mediaStreams: Array<out MediaStream>) {}
        })

        Log.d(TAG, "PeerConnection created (Unified Plan)")
    }

    /**
     * Sends a directional command to the Partner via the DataChannel.
     * Only works after the DataChannel is opened by the Partner.
     */
    fun sendCommand(command: String) {
        val dc = dataChannel
        if (dc == null || dc.state() != DataChannel.State.OPEN) {
            Log.w(TAG, "DataChannel not open — cannot send '$command'")
            return
        }
        val buffer = DataChannel.Buffer(
            ByteBuffer.wrap(command.toByteArray(Charsets.UTF_8)),
            false
        )
        dc.send(buffer)
        Log.d(TAG, "Command sent: $command")
    }

    fun handleOffer(sdp: String, observer: SdpObserver) {
        Log.d(TAG, "Setting remote description (offer)")
        val desc = SessionDescription(SessionDescription.Type.OFFER, sdp)
        peerConnection?.setRemoteDescription(observer, desc)
    }

    fun createAnswer(observer: SdpObserver) {
        Log.d(TAG, "Creating answer")
        peerConnection?.createAnswer(observer, MediaConstraints())
    }

    fun setLocalDescription(sdp: SessionDescription, observer: SdpObserver) {
        Log.d(TAG, "Setting local description (answer)")
        peerConnection?.setLocalDescription(observer, sdp)
    }

    fun addIceCandidate(candidate: IceCandidate) {
        Log.d(TAG, "Adding ICE candidate: ${candidate.sdpMid}")
        peerConnection?.addIceCandidate(candidate)
    }

    fun close() {
        Log.d(TAG, "Closing WebRTCManager...")
        dataChannel?.close()
        dataChannel?.dispose()
        peerConnection?.dispose()
        peerConnectionFactory?.dispose()
        Log.d(TAG, "WebRTCManager closed ✅")
    }
}
