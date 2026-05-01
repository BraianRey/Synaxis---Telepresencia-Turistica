package com.sismptm.partner.ui.features.streaming

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sismptm.partner.BuildConfig
import com.sismptm.partner.data.remote.api.dto.IceCandidateModel
import com.sismptm.partner.data.remote.signaling.SignalingClient
import com.sismptm.partner.data.remote.signaling.SignalingMessage
import com.sismptm.partner.manager.webrtc.WebRTCManager
import java.util.UUID
import kotlin.random.Random
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.webrtc.*

data class CommandEvent(val text: String, val id: String = UUID.randomUUID().toString())

/**
 * ViewModel for managing the streaming session, signaling, and WebRTC lifecycle.
 * Coordinates between the UI, Signaling server, and WebRTC hardware manager.
 */
class StreamingViewModel(application: Application) :
    AndroidViewModel(application),
    SignalingClient.SignalingListener,
    WebRTCManager.WebRTCListener {

    private val TAG = "StreamingViewModel"

    private var targetClientId: String? = null
    private var partnerId = ""
    private var signalingClient: SignalingClient? = null
    private var connectionTimeout: Job? = null

    private var reconnectionAttempts = 0
    private val maxReconnectionAttempts = 5
    private val reconnectionDelays = listOf(3000L, 6000L, 12000L, 20000L, 30000L)
    private var reconnectionJob: Job? = null
    private var lastState: PeerConnection.PeerConnectionState = PeerConnection.PeerConnectionState.NEW

    val eglBase: EglBase = EglBase.create()
    private val webRTCManager = WebRTCManager(context = application, listener = this, eglBase = eglBase)

    private val _connectionState = MutableStateFlow(PeerConnection.PeerConnectionState.NEW)
    val connectionState: StateFlow<PeerConnection.PeerConnectionState> = _connectionState

    private val _lastCommandEvent = MutableStateFlow<CommandEvent?>(null)
    val lastCommandEvent: StateFlow<CommandEvent?> = _lastCommandEvent

    private val _commands = MutableStateFlow<List<String>>(emptyList())
    val commands: StateFlow<List<String>> = _commands

    /**
     * Initializes hardware capture and connects to the signaling server using the defined peer ID.
     * Ensures previous sessions are cleaned up before starting.
     */
    fun initStreaming(surfaceViewRenderer: SurfaceViewRenderer, customId: String) {
        signalingClient?.disconnect()
        webRTCManager.stopLocalCapture()
        
        this.partnerId = customId
        webRTCManager.startLocalCapture(surfaceViewRenderer)

        val baseUrl = BuildConfig.BASE_WEBRTC
        val signalingUrl = if (baseUrl.contains("?")) "$baseUrl&peerId=$partnerId" else "$baseUrl?peerId=$partnerId"

        signalingClient = SignalingClient(signalingUrl, this)
        signalingClient?.connect()
    }

    override fun onConnected() {
        Log.i(TAG, "Signaling connected. Resetting reconnection attempts.")
        reconnectionAttempts = 0
    }

    override fun onJoinReceived(senderId: String) {
        if (senderId.isBlank()) return
        Log.i(TAG, "Join request from client: $senderId. Performing proactive hardware reset.")
        this.targetClientId = senderId

        connectionTimeout?.cancel()
        connectionTimeout = viewModelScope.launch {
            delay(25000)
            if (lastState != PeerConnection.PeerConnectionState.CONNECTED) {
                Log.e(TAG, "Connection recovery timeout")
            }
        }

        // Proactive reset: Clear all hardware and network buffers to ensure clean recovery
        webRTCManager.restartCapture()
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
        Log.w(TAG, "Signaling WebSocket lost. Reconnection managed by SignalingClient.")
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
        _lastCommandEvent.value = CommandEvent(command)
        _commands.value = (_commands.value + command).takeLast(10)
    }

    override fun onConnectionStateChange(state: PeerConnection.PeerConnectionState) {
        lastState = state
        _connectionState.value = state
        
        when (state) {
            PeerConnection.PeerConnectionState.CONNECTED -> {
                connectionTimeout?.cancel()
                reconnectionJob?.cancel()
                reconnectionAttempts = 0
            }
            PeerConnection.PeerConnectionState.DISCONNECTED,
            PeerConnection.PeerConnectionState.FAILED,
            PeerConnection.PeerConnectionState.CLOSED -> {
                Log.w(TAG, "Connection state: $state. Triggering reconnection timer.")
                startReconnectionTimer()
            }
            else -> {}
        }
    }

    private fun startReconnectionTimer() {
        if (reconnectionJob?.isActive == true || reconnectionAttempts >= maxReconnectionAttempts) return
        
        val baseDelay = reconnectionDelays.getOrElse(reconnectionAttempts) { 30000L }
        reconnectionJob = viewModelScope.launch {
            delay(baseDelay + Random.nextLong(0, 1000))
            if (lastState != PeerConnection.PeerConnectionState.CONNECTED) {
                attemptRecovery()
            }
        }
    }

    private fun attemptRecovery() {
        if (reconnectionAttempts >= maxReconnectionAttempts || targetClientId == null) return
        reconnectionAttempts++
        Log.i(TAG, "Attempting connection recovery (Attempt $reconnectionAttempts)")
        
        // Rebuild PeerConnection to escape potentially corrupted stack states
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
