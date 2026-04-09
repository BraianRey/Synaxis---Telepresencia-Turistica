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
 *  - Created once when entering StreamingScreen
 *  - Cleared when leaving (disposes WebRTC + closes WebSocket)
 *
 * Signaling flow:
 *  1. initStreaming(surface, partnerId) → camera preview starts + WS connects
 *  2. onConnected() → createOffer()
 *  3. onLocalSdpCreated(offer) → send to CLIENT_01 via server
 *  4. onAnswerReceived(sdp) → setRemoteDescription (partner receives answer)
 *  5. ICE candidates exchanged → P2P established
 */
class StreamingViewModel(application: Application) : AndroidViewModel(application),
    SignalingClient.SignalingListener, WebRTCManager.WebRTCListener {

    private val TAG = "StreamingViewModel"

    /** Static target: the Client peer always registers as CLIENT_01 */
    private val targetClientId = "CLIENT_01"

    private var partnerId = "PARTNER_01"
    private var signalingClient: SignalingClient? = null

    // EglBase is created HERE (in the ViewModel) so it survives recompositions.
    // It is shared with WebRTCManager to ensure the same EGL context is used
    // for both the PeerConnectionFactory and the SurfaceViewRenderer.
    val eglBase: EglBase = EglBase.create()

    private val webRTCManager = WebRTCManager(
        context = application,
        listener = this,
        eglBase = eglBase
    )

    private val _connectionState = MutableStateFlow(PeerConnection.PeerConnectionState.NEW)
    val connectionState: StateFlow<PeerConnection.PeerConnectionState> = _connectionState

    private val _commands = MutableStateFlow<List<String>>(emptyList())
    val commands: StateFlow<List<String>> = _commands

    private val _lastCommand = MutableStateFlow<String?>(null)
    val lastCommand: StateFlow<String?> = _lastCommand

    /**
     * Call once when the streaming screen is ready.
     * Starts the camera preview and connects to the signaling server.
     *
     * @param surfaceViewRenderer The surface to show the local camera.
     * @param customId The Partner ID entered by the user.
     */
    fun initStreaming(surfaceViewRenderer: SurfaceViewRenderer, customId: String) {
        this.partnerId = customId
        Log.d(TAG, "initStreaming — partnerId='$partnerId', targeting '$targetClientId'")

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
        return if (base.contains("?")) "$base&peerId=$peerId"
        else "$base?peerId=$peerId"
    }

    // ─── SignalingClient.SignalingListener ───────────────────────────────────

    override fun onConnected() {
        Log.d(TAG, "Signaling connected — creating offer for '$targetClientId'")
        webRTCManager.createOffer()
    }

    override fun onOfferReceived(sdp: String) {
        // Partner is the CALLER, it should not receive offers
        Log.w(TAG, "Unexpected offer received on Partner — ignoring")
    }

    override fun onAnswerReceived(sdp: String) {
        Log.d(TAG, "Answer received — setting remote description")
        webRTCManager.setRemoteDescription(sdp, isOffer = false)
    }

    override fun onIceCandidateReceived(candidate: IceCandidateModel) {
        val iceCandidate = IceCandidate(
            candidate.sdpMid,
            candidate.sdpMLineIndex,
            candidate.candidate
        )
        webRTCManager.addIceCandidate(iceCandidate)
    }

    override fun onDisconnected() {
        Log.d(TAG, "Signaling disconnected")
    }

    override fun onError(error: String) {
        Log.e(TAG, "Signaling error: $error")
    }

    // ─── WebRTCManager.WebRTCListener ───────────────────────────────────────

    override fun onIceCandidate(candidate: IceCandidate) {
        signalingClient?.sendMessage(
            SignalingMessage(
                type = "candidate",
                candidate = IceCandidateModel(
                    sdpMid = candidate.sdpMid,
                    sdpMLineIndex = candidate.sdpMLineIndex,
                    candidate = candidate.sdp,
                    targetId = targetClientId
                ),
                senderId = partnerId,
                targetId = targetClientId
            )
        )
    }

    override fun onLocalSdpCreated(sdp: SessionDescription) {
        val type = if (sdp.type == SessionDescription.Type.OFFER) "offer" else "answer"
        Log.d(TAG, "Local SDP ready (type=$type) → sending to '$targetClientId'")
        signalingClient?.sendMessage(
            SignalingMessage(
                type = type,
                sdp = sdp.description,
                senderId = partnerId,
                targetId = targetClientId
            )
        )
    }

    override fun onCommandReceived(command: String) {
        _lastCommand.value = command
        _commands.value = (_commands.value + command).takeLast(3)
    }

    override fun onConnectionStateChange(state: PeerConnection.PeerConnectionState) {
        Log.d(TAG, "PeerConnection → $state")
        _connectionState.value = state
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "ViewModel cleared — disposing resources")
        webRTCManager.dispose()   // also releases eglBase
        signalingClient?.disconnect()
    }
}
