package com.sismptm.client.data.remote

import android.content.Context
import org.webrtc.*
import java.util.*

/**
 * Manager to handle WebRTC PeerConnection, DataChannel, and MediaStreams.
 * Handles the P2P connection logic and command transmission.
 */
class WebRTCManager(
    private val context: Context,
    private val eglBase: EglBase,
    private val onRemoteStream: (MediaStream) -> Unit,
    private val onIceCandidate: (IceCandidate) -> Unit
) {
    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null
    private var dataChannel: DataChannel? = null

    init {
        initPeerConnectionFactory()
    }

    private fun initPeerConnectionFactory() {
        val options = PeerConnectionFactory.InitializationOptions.builder(context)
            .setEnableInternalTracer(true)
            .createInitializationOptions()
        PeerConnectionFactory.initialize(options)

        val videoEncoderFactory = DefaultVideoEncoderFactory(eglBase.eglBaseContext, true, true)
        val videoDecoderFactory = DefaultVideoDecoderFactory(eglBase.eglBaseContext)

        peerConnectionFactory = PeerConnectionFactory.builder()
            .setVideoEncoderFactory(videoEncoderFactory)
            .setVideoDecoderFactory(videoDecoderFactory)
            .createPeerConnectionFactory()
    }

    /**
     * Initializes the PeerConnection and sets up the DataChannel for instructions.
     */
    fun createPeerConnection() {
        val iceServers = listOf(
            PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer()
        )

        val rtcConfig = PeerConnection.RTCConfiguration(iceServers)
        peerConnection = peerConnectionFactory?.createPeerConnection(rtcConfig, object : PeerConnection.Observer {
            override fun onIceCandidate(candidate: IceCandidate?) {
                candidate?.let { onIceCandidate(it) }
            }

            override fun onAddStream(stream: MediaStream?) {
                stream?.let { onRemoteStream(it) }
            }

            override fun onDataChannel(dc: DataChannel?) {
                // If the Partner creates the channel, we assign it here
                if (dc?.label() == "control") {
                    dataChannel = dc
                }
            }

            override fun onIceConnectionChange(newState: PeerConnection.IceConnectionState?) {}
            override fun onIceConnectionReceivingChange(receiving: Boolean) {}
            override fun onIceGatheringChange(newState: PeerConnection.IceGatheringState?) {}
            override fun onSignalingChange(newState: PeerConnection.SignalingState?) {}
            override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {}
            override fun onRemoveStream(stream: MediaStream?) {}
            override fun onRenegotiationNeeded() {}
            override fun onTrack(transceiver: RtpTransceiver?) {}
        })
        
        // Setup DataChannel locally just in case, but using the correct label "control"
        val dcInit = DataChannel.Init()
        dataChannel = peerConnection?.createDataChannel("control", dcInit)
    }

    /**
     * Sends a command string directly to the partner via P2P DataChannel.
     */
    fun sendCommand(command: String) {
        val dc = dataChannel
        if (dc != null && dc.state() == DataChannel.State.OPEN) {
            val buffer = DataChannel.Buffer(
                java.nio.ByteBuffer.wrap(command.toByteArray()),
                false
            )
            dc.send(buffer)
        }
    }

    fun handleOffer(sdp: String, observer: SdpObserver) {
        val sessionDescription = SessionDescription(SessionDescription.Type.OFFER, sdp)
        peerConnection?.setRemoteDescription(observer, sessionDescription)
    }

    fun createAnswer(observer: SdpObserver) {
        val mediaConstraints = MediaConstraints()
        peerConnection?.createAnswer(observer, mediaConstraints)
    }

    fun addIceCandidate(candidate: IceCandidate) {
        peerConnection?.addIceCandidate(candidate)
    }

    fun setLocalDescription(sdp: SessionDescription, observer: SdpObserver) {
        peerConnection?.setLocalDescription(observer, sdp)
    }

    fun close() {
        dataChannel?.close()
        peerConnection?.close()
        peerConnectionFactory?.dispose()
    }
}
