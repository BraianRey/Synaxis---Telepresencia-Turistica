package com.sismptm.client.ui.screens

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sismptm.client.data.remote.*
import kotlin.random.Random
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.webrtc.*

/**
 * ViewModel for the Client streaming screen, managing the WebRTC session and signaling.
 */
class StreamingViewModel(application: Application) :
        AndroidViewModel(application), SignalingClientListener {

    private val TAG = "ClientStreamingViewModel"
    private var signalingClient: SignalingClient = SignalingClient(this)
    private var webRTCManager: WebRTCManager? = null
    private var partnerId: String = ""
    private var connectionTimeout: Job? = null

    /**
     * Reconnection state management using exponential backoff.
     */
    private var reconnectionAttempts = 0
    private val maxReconnectionAttempts = 3
    private val reconnectionDelays = listOf(4000L, 8000L, 16000L)
    private var reconnectionJob: Job? = null
    private var lastState: PeerConnection.PeerConnectionState = PeerConnection.PeerConnectionState.NEW

    /**
     * Shared EglBase for WebRTC rendering and factory initialization.
     */
    val eglBase: EglBase = EglBase.create()

    /**
     * Initializes the WebRTC connection and connects to the signaling server.
     * @param targetPartnerId The ID of the peer to connect to.
     * @param onRemoteTrack Callback invoked when the remote video track is received.
     */
    fun startConnection(targetPartnerId: String, onRemoteTrack: (VideoTrack) -> Unit) {
        this.partnerId = targetPartnerId

        webRTCManager = WebRTCManager(
            context = getApplication(),
            eglBase = eglBase,
            onRemoteTrack = onRemoteTrack
        ).apply {
            createPeerConnection()
            setOnIceCandidateCallback { candidate ->
                if (partnerId.isBlank()) {
                    Log.w(TAG, "ICE candidate dropped: partnerId is empty")
                    return@setOnIceCandidateCallback
                }

                if (!signalingClient.isConnected()) {
                    Log.w(TAG, "ICE candidate dropped: signaling not connected")
                    return@setOnIceCandidateCallback
                }

                signalingClient.sendIceCandidate(
                    sdp = candidate.sdp,
                    sdpMid = candidate.sdpMid,
                    sdpMLineIndex = candidate.sdpMLineIndex,
                    targetId = partnerId
                )
            }
            setOnConnectionStateChangeListener { state ->
                onConnectionStateChanged(state)
            }
        }

        /**
         * Monitor for initial connection success within a 20s window.
         */
        connectionTimeout?.cancel()
        connectionTimeout = viewModelScope.launch {
            delay(20000)
            if (lastState != PeerConnection.PeerConnectionState.CONNECTED) {
                Log.e(TAG, "Initial Connection Timeout - No activity detected")
            }
        }

        signalingClient.connect()
    }

    /**
     * Handles changes in the PeerConnection state to trigger recovery logic if needed.
     */
    fun onConnectionStateChanged(state: PeerConnection.PeerConnectionState) {
        Log.d(TAG, "PeerConnection State: $state")
        lastState = state
        
        when (state) {
            PeerConnection.PeerConnectionState.CONNECTED -> {
                Log.i(TAG, "!!! WEBRTC CONNECTED !!!")
                connectionTimeout?.cancel()
                reconnectionJob?.cancel()
                reconnectionAttempts = 0
            }
            PeerConnection.PeerConnectionState.DISCONNECTED,
            PeerConnection.PeerConnectionState.FAILED -> {
                Log.w(TAG, "Connection lost/failed. Starting recovery...")
                startReconnectionTimer()
            }
            else -> {}
        }
    }

    /**
     * Waits with jitter and backoff before attempting to rejoin the session.
     */
    private fun startReconnectionTimer() {
        if (reconnectionJob?.isActive == true) return
        
        if (reconnectionAttempts >= maxReconnectionAttempts) {
            Log.e(TAG, "Max recovery attempts reached")
            return
        }

        val baseDelay = reconnectionDelays.getOrElse(reconnectionAttempts) { 16000L }
        val jitter = Random.nextLong(0, 1000)
        val totalDelay = baseDelay + jitter

        reconnectionJob = viewModelScope.launch {
            Log.d(TAG, "Scheduling reconnection in ${totalDelay}ms (attempt ${reconnectionAttempts + 1}/$maxReconnectionAttempts)")
            delay(totalDelay)
            if (lastState != PeerConnection.PeerConnectionState.CONNECTED) {
                attemptRejoin()
            }
        }
    }

    /**
     * Sends a join message to the signaling server to request a new session from the partner.
     */
    private fun attemptRejoin() {
        if (reconnectionAttempts >= maxReconnectionAttempts) {
            Log.e(TAG, "Max rejoin attempts reached")
            return
        }
        
        reconnectionAttempts++
        Log.i(TAG, "Recovery Attempt #$reconnectionAttempts...")
        if (partnerId.isNotBlank()) {
            // Re-create the peer connection to ensure a clean state before joining
            webRTCManager?.createPeerConnection()
            signalingClient.sendJoin(partnerId)
        }
    }

    /**
     * Sends a control command to the partner via the DataChannel.
     */
    fun sendCommand(command: String) {
        webRTCManager?.sendCommand(command)
    }

    /**
     * Signaling listener implementation.
     */
    override fun onConnectionOpened() {
        Log.d(TAG, "Signaling opened, sending 'join' to $partnerId")
        if (partnerId.isNotBlank()) {
            signalingClient.sendJoin(partnerId)
        }
    }

    /**
     * Processes a remote SDP offer and sends back an answer.
     */
    override fun onOfferReceived(sdp: String, senderId: String) {
        Log.i(TAG, "Offer received from $senderId")
        connectionTimeout?.cancel()
        reconnectionJob?.cancel()
        
        if (senderId.isNotBlank()) {
            this.partnerId = senderId
        }

        webRTCManager?.handleOfferAndAnswer(sdp) { answerSdp ->
            Log.d(TAG, "Sending Answer to $partnerId")
            signalingClient.sendSdp("answer", answerSdp, partnerId)
        }
    }

    override fun onAnswerReceived(sdp: String) {
        Log.d(TAG, "Answer received (unexpected in Client role)")
    }

    /**
     * Adds a remote ICE candidate to the PeerConnection.
     */
    override fun onIceCandidateReceived(sdp: String, sdpMid: String, sdpMLineIndex: Int) {
        val candidate = IceCandidate(sdpMid, sdpMLineIndex, sdp)
        webRTCManager?.addIceCandidate(candidate)
    }

    /**
     * Cleans up all WebRTC and signaling resources.
     */
    override fun onCleared() {
        super.onCleared()
        reconnectionJob?.cancel()
        connectionTimeout?.cancel()
        webRTCManager?.close()
        signalingClient.close()
        eglBase.release()
    }
}
