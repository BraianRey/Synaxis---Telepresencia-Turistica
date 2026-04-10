package com.sismptm.partner.ui.screens

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import com.sismptm.partner.BuildConfig
import com.sismptm.partner.data.remote.IceCandidateModel
import com.sismptm.partner.data.remote.SignalingClient
import com.sismptm.partner.data.remote.SignalingMessage
import com.sismptm.partner.webrtc.WebRTCManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.webrtc.*

/**
 * ViewModel for the Partner streaming screen.
 *
 * Lifecycle:
 * - Created once when entering StreamingScreen
 * - Cleared when leaving (disposes WebRTC + closes WebSocket)
 *
 * Signaling flow:
 * 1. initStreaming(surface, partnerId) → camera preview starts + WS connects
 * 2. onJoinReceived(clientId) → Partner receives join request from client
 * 3. Partner calls createOffer() targeting the specific clientId
 * 4. onLocalSdpCreated(offer) → send to clientId via server
 * 5. onAnswerReceived(sdp) → setRemoteDescription (partner receives answer)
 * 6. ICE candidates exchanged → P2P established
 */
class StreamingViewModel(application: Application) :
        AndroidViewModel(application),
        SignalingClient.SignalingListener,
        WebRTCManager.WebRTCListener {

    private val TAG = "StreamingViewModel"

    /** Dynamic target: determined when a client sends a 'join' message */
    private var targetClientId: String? = null

    private var partnerId = "PARTNER_01"
    private var signalingClient: SignalingClient? = null

    // EglBase is created HERE (in the ViewModel) so it survives recompositions.
    // It is shared with WebRTCManager to ensure the same EGL context is used
    // for both the PeerConnectionFactory and the SurfaceViewRenderer.
    val eglBase: EglBase = EglBase.create()

    private val webRTCManager =
            WebRTCManager(context = application, listener = this, eglBase = eglBase)

    private val _connectionState = MutableStateFlow(PeerConnection.PeerConnectionState.NEW)
    val connectionState: StateFlow<PeerConnection.PeerConnectionState> = _connectionState

    private val _commands = MutableStateFlow<List<String>>(emptyList())
    val commands: StateFlow<List<String>> = _commands

    private val _lastCommand = MutableStateFlow<String?>(null)
    val lastCommand: StateFlow<String?> = _lastCommand

    /**
     * Call once when the streaming screen is ready. Starts the camera preview and connects to the
     * signaling server.
     *
     * @param surfaceViewRenderer The surface to show the local camera.
     * @param customId The Partner ID entered by the user.
     */
    fun initStreaming(surfaceViewRenderer: SurfaceViewRenderer, customId: String) {
        this.partnerId = customId
        Log.d(TAG, "initStreaming — partnerId='$partnerId'")

        // Start camera (immediate preview + sets up PeerConnection + DataChannel)
        webRTCManager.startLocalCapture(surfaceViewRenderer)

        // Connect to signaling server with peerId query param
        val baseUrl = BuildConfig.BASE_WEBRTC
        val signalingUrl = buildUrl(baseUrl, partnerId)
        Log.d(TAG, "Signaling URL: $signalingUrl")

        signalingClient = SignalingClient(signalingUrl, this)
        signalingClient?.connect()
    }

    private fun buildUrl(base: String, peerId: String): String {
        return if (base.contains("?")) "$base&peerId=$peerId" else "$base?peerId=$peerId"
    }

    // ─── SignalingClient.SignalingListener ───────────────────────────────────

    override fun onConnected() {
        Log.d(TAG, "Signaling connected — waiting for client to join...")
    }

    override fun onJoinReceived(senderId: String) {
        Log.d(TAG, "Client '$senderId' joined — creating offer")
        this.targetClientId = senderId
        webRTCManager.createOffer()
    }

    /**
     * Called when an SDP offer is received (unexpected for Partner).
     *
     * Partner is the caller role and should NOT receive offers. This is a safety check.
     *
     * @param sdp The received offer SDP
     */
    override fun onOfferReceived(sdp: String) {
        // Partner is the CALLER, it should not receive offers
        Log.w(TAG, "Unexpected offer received on Partner — ignoring")
    }

    /**
     * Called when an SDP answer is received from the Client.
     *
     * Sets the remote description to complete the SDP negotiation.
     *
     * @param sdp The answer SDP from the Client
     */
    override fun onAnswerReceived(sdp: String) {
        Log.d(TAG, "Answer received — setting remote description")
        webRTCManager.setRemoteDescription(sdp, isOffer = false)
    }

    /**
     * Called when an ICE candidate is received from the Client.
     *
     * Converts the model to IceCandidate and passes to WebRTCManager.
     *
     * @param candidate The received ICE candidate
     */
    override fun onIceCandidateReceived(candidate: IceCandidateModel) {
        val iceCandidate =
                IceCandidate(candidate.sdpMid, candidate.sdpMLineIndex, candidate.candidate)
        webRTCManager.addIceCandidate(iceCandidate)
    }

    /** Called when the signaling WebSocket is disconnected. */
    override fun onDisconnected() {
        Log.d(TAG, "Signaling disconnected")
    }

    /**
     * Called when a signaling error occurs.
     *
     * @param error Error message describing the failure
     */
    override fun onError(error: String) {
        Log.e(TAG, "Signaling error: $error")
    }

    // ─── WebRTCManager.WebRTCListener Implementation ───────────────────────────────────

    /**
     * Called when a local ICE candidate is discovered.
     *
     * Sends the candidate to the target Client via signaling.
     *
     * @param candidate The discovered ICE candidate
     */
    override fun onIceCandidate(candidate: IceCandidate) {
        val target = targetClientId ?: return
        signalingClient?.sendMessage(
                SignalingMessage(
                        type = "candidate",
                        candidate =
                                IceCandidateModel(
                                        sdpMid = candidate.sdpMid,
                                        sdpMLineIndex = candidate.sdpMLineIndex,
                                        candidate = candidate.sdp,
                                        targetId = target
                                ),
                        senderId = partnerId,
                        targetId = target
                )
        )
    }

    /**
     * Called when the local SDP (offer) is successfully created.
     *
     * Sends the SDP to the target Client via signaling.
     *
     * @param sdp The created SDP offer
     */
    override fun onLocalSdpCreated(sdp: SessionDescription) {
        val target = targetClientId ?: return
        val type = if (sdp.type == SessionDescription.Type.OFFER) "offer" else "answer"
        Log.d(TAG, "Local SDP ready (type=$type) → sending to '$target'")
        signalingClient?.sendMessage(
                SignalingMessage(
                        type = type,
                        sdp = sdp.description,
                        senderId = partnerId,
                        targetId = target
                )
        )
    }

    /**
     * Called when a command is received from the Client via DataChannel.
     *
     * Updates the last command and maintains a history.
     *
     * @param command The received command string
     */
    override fun onCommandReceived(command: String) {
        _lastCommand.value = command
        _commands.value = (_commands.value + command).takeLast(3)
    }

    /**
     * Called when the PeerConnection state changes.
     *
     * @param state The new PeerConnection state
     */
    override fun onConnectionStateChange(state: PeerConnection.PeerConnectionState) {
        Log.d(TAG, "PeerConnection → $state")
        _connectionState.value = state
    }

    /**
     * Lifecycle cleanup when the ViewModel is destroyed.
     *
     * Disposes WebRTC resources and closes signaling connection.
     */
    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "ViewModel cleared — disposing resources")
        webRTCManager.dispose() // also releases eglBase
        signalingClient?.disconnect()
    }
}
