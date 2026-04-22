package com.sismptm.partner.ui.screens

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sismptm.partner.BuildConfig
import com.sismptm.partner.data.remote.IceCandidateModel
import com.sismptm.partner.data.remote.SignalingClient
import com.sismptm.partner.data.remote.SignalingMessage
import com.sismptm.partner.webrtc.WebRTCManager
import java.util.UUID
import kotlin.random.Random
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.webrtc.*

/**
 * Represents a command with a unique ID to ensure UI reactivity even if the command text repeats.
 */
data class CommandEvent(val text: String, val id: String = UUID.randomUUID().toString())

/**
 * ViewModel for the Partner streaming screen managing WebRTC sessions and signaling.
 */
class StreamingViewModel(application: Application) :
    AndroidViewModel(application),
    SignalingClient.SignalingListener,
    WebRTCManager.WebRTCListener {

    private val TAG = "StreamingViewModel"

    private var targetClientId: String? = null
    private var partnerId = "PARTNER_01"
    private var signalingClient: SignalingClient? = null
    private var connectionTimeout: Job? = null

    private var reconnectionAttempts = 0
    private val maxReconnectionAttempts = 3
    private val reconnectionDelays =
        listOf(
            4000L, // Attempt 1: 4 seconds
            8000L, // Attempt 2: 8 seconds
            16000L // Attempt 3: 16 seconds
        )
    private var reconnectionJob: Job? = null
    private var lastState: PeerConnection.PeerConnectionState =
        PeerConnection.PeerConnectionState.NEW

    val eglBase: EglBase = EglBase.create()

    private val webRTCManager =
        WebRTCManager(context = application, listener = this, eglBase = eglBase)

    private val _connectionState = MutableStateFlow(PeerConnection.PeerConnectionState.NEW)
    val connectionState: StateFlow<PeerConnection.PeerConnectionState> = _connectionState

    private val _lastCommandEvent = MutableStateFlow<CommandEvent?>(null)
    val lastCommandEvent: StateFlow<CommandEvent?> = _lastCommandEvent

    private val _commands = MutableStateFlow<List<String>>(emptyList())
    val commands: StateFlow<List<String>> = _commands

    /**
     * Initializes the local camera capture and connects to the signaling server.
     */
    fun initStreaming(surfaceViewRenderer: SurfaceViewRenderer, customId: String) {
        this.partnerId = customId
        webRTCManager.startLocalCapture(surfaceViewRenderer)

        val baseUrl = BuildConfig.BASE_WEBRTC
        val signalingUrl = buildUrl(baseUrl, partnerId)
        signalingClient = SignalingClient(signalingUrl, this)
        signalingClient?.connect()
    }

    private fun buildUrl(base: String, peerId: String): String {
        return if (base.contains("?")) "$base&peerId=$peerId" else "$base?peerId=$peerId"
    }

    override fun onConnected() {
        Log.i(TAG, "Signaling connected (Partner: $partnerId)")
    }

    override fun onJoinReceived(senderId: String) {
        if (senderId.isBlank()) return
        Log.i(TAG, "Join request from client: $senderId.")
        this.targetClientId = senderId

        connectionTimeout?.cancel()
        connectionTimeout = viewModelScope.launch {
            delay(25000)
            if (lastState != PeerConnection.PeerConnectionState.CONNECTED) {
                Log.e(TAG, "Connection setup timeout")
            }
        }

        webRTCManager.setupNewPeerConnection()
        webRTCManager.createOffer()
    }

    override fun onOfferReceived(sdp: String) {}

    override fun onAnswerReceived(sdp: String) {
        if (targetClientId == null) return
        connectionTimeout?.cancel()
        webRTCManager.setRemoteDescription(sdp, isOffer = false)
    }

    override fun onIceCandidateReceived(candidate: IceCandidateModel) {
        if (targetClientId == null) return
        val iceCandidate = IceCandidate(candidate.sdpMid, candidate.sdpMLineIndex, candidate.candidate)
        webRTCManager.addIceCandidate(iceCandidate)
    }

    override fun onDisconnected() {
        Log.w(TAG, "Signaling WebSocket disconnected")
    }

    override fun onError(error: String) {
        Log.e(TAG, "Signaling Error: $error")
    }

    override fun onIceCandidate(candidate: IceCandidate) {
        val target = targetClientId ?: return
        signalingClient?.sendMessage(
            SignalingMessage(
                type = "candidate",
                candidate = IceCandidateModel(candidate.sdpMid, candidate.sdpMLineIndex, candidate.sdp, target),
                senderId = partnerId,
                targetId = target
            )
        )
    }

    override fun onLocalSdpCreated(sdp: SessionDescription) {
        val target = targetClientId ?: return
        signalingClient?.sendMessage(
            SignalingMessage(
                type = "offer",
                sdp = sdp.description,
                senderId = partnerId,
                targetId = target
            )
        )
    }

    override fun onCommandReceived(command: String) {
        // Updates state for UI reactivity; audio feedback is handled in the UI layer
        _lastCommandEvent.value = CommandEvent(command)
        _commands.value = (_commands.value + command).takeLast(10)
    }

    override fun onConnectionStateChange(state: PeerConnection.PeerConnectionState) {
        lastState = state
        _connectionState.value = state
        if (state == PeerConnection.PeerConnectionState.CONNECTED) {
            connectionTimeout?.cancel()
            reconnectionJob?.cancel()
            reconnectionAttempts = 0
        } else if (state == PeerConnection.PeerConnectionState.DISCONNECTED || state == PeerConnection.PeerConnectionState.FAILED) {
            startReconnectionTimer()
        }
    }

    /**
     * Schedules an automatic reconnection attempt if the connection is lost.
     */
    private fun startReconnectionTimer() {
        if (reconnectionJob?.isActive == true || reconnectionAttempts >= maxReconnectionAttempts) return
        val baseDelay = reconnectionDelays.getOrElse(reconnectionAttempts) { 16000L }
        reconnectionJob = viewModelScope.launch {
            delay(baseDelay + Random.nextLong(0, 1000))
            if (lastState != PeerConnection.PeerConnectionState.CONNECTED) {
                attemptRecovery()
            }
        }
    }

    /**
     * Attempts to recover the WebRTC session by re-initiating the negotiation process.
     */
    private fun attemptRecovery() {
        if (reconnectionAttempts >= maxReconnectionAttempts || targetClientId == null) return
        reconnectionAttempts++
        webRTCManager.setupNewPeerConnection()
        webRTCManager.createOffer()
    }

    override fun onCleared() {
        super.onCleared()
        connectionTimeout?.cancel()
        reconnectionJob?.cancel()
        webRTCManager.dispose()
        signalingClient?.disconnect()
    }
}