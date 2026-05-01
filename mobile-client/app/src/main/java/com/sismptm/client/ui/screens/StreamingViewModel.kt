package com.sismptm.client.ui.screens

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sismptm.client.data.remote.signaling.SignalingClient
import com.sismptm.client.data.remote.signaling.SignalingClientListener
import com.sismptm.client.manager.webrtc.WebRTCManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.webrtc.*
import kotlin.random.Random

/**
 * ViewModel for the Client streaming screen, managing the WebRTC session and signaling.
 */
class StreamingViewModel(application: Application) :
    AndroidViewModel(application), SignalingClientListener {

    private val TAG = "ClientStreamingViewModel"
    private var signalingClient: SignalingClient? = null
    private var webRTCManager: WebRTCManager? = null
    private var partnerId: String = ""
    private var connectionTimeout: Job? = null

    private var reconnectionAttempts = 0
    private val maxReconnectionAttempts = 3
    private val reconnectionDelays = listOf(4000L, 8000L, 16000L)
    private var reconnectionJob: Job? = null
    private var lastState: PeerConnection.PeerConnectionState = PeerConnection.PeerConnectionState.NEW

    val eglBase: EglBase = EglBase.create()

    /**
     * Initializes the WebRTC connection using serviceId to identify the peers.
     */
    fun startConnection(serviceId: String, onRemoteTrack: (VideoTrack) -> Unit) {
        this.partnerId = serviceId // Partner uses serviceId as their peerId
        val myClientId = "CLIENT_$serviceId"

        signalingClient = SignalingClient(this, myClientId)
        
        webRTCManager = WebRTCManager(
            context = getApplication(),
            eglBase = eglBase,
            onRemoteTrack = onRemoteTrack
        ).apply {
            createPeerConnection()
            setOnIceCandidateCallback { candidate ->
                signalingClient?.sendIceCandidate(
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

        connectionTimeout?.cancel()
        connectionTimeout = viewModelScope.launch {
            delay(20000)
            if (lastState != PeerConnection.PeerConnectionState.CONNECTED) {
                Log.e(TAG, "Initial Connection Timeout")
            }
        }

        signalingClient?.connect()
    }

    fun onConnectionStateChanged(state: PeerConnection.PeerConnectionState) {
        Log.d(TAG, "PeerConnection State: $state")
        lastState = state
        if (state == PeerConnection.PeerConnectionState.CONNECTED) {
            connectionTimeout?.cancel()
            reconnectionJob?.cancel()
            reconnectionAttempts = 0
        } else if (state == PeerConnection.PeerConnectionState.DISCONNECTED || 
                   state == PeerConnection.PeerConnectionState.FAILED) {
            startReconnectionTimer()
        }
    }

    private fun startReconnectionTimer() {
        if (reconnectionJob?.isActive == true) return
        if (reconnectionAttempts >= maxReconnectionAttempts) return

        val delayMs = reconnectionDelays.getOrElse(reconnectionAttempts) { 16000L } + Random.nextLong(0, 1000)
        reconnectionJob = viewModelScope.launch {
            delay(delayMs)
            if (lastState != PeerConnection.PeerConnectionState.CONNECTED) {
                attemptRejoin()
            }
        }
    }

    private fun attemptRejoin() {
        reconnectionAttempts++
        webRTCManager?.createPeerConnection()
        signalingClient?.sendJoin(partnerId)
    }

    /**
     * Sends a directional command to the partner via signaling channel as requested.
     */
    fun sendCommand(command: String) {
        signalingClient?.sendCommand(command, partnerId)
    }

    override fun onConnectionOpened() {
        Log.d(TAG, "Signaling opened, joining target: $partnerId")
        signalingClient?.sendJoin(partnerId)
    }

    override fun onOfferReceived(sdp: String, senderId: String) {
        webRTCManager?.handleOfferAndAnswer(sdp) { answerSdp ->
            signalingClient?.sendSdp("answer", answerSdp, partnerId)
        }
    }

    override fun onAnswerReceived(sdp: String) {}

    override fun onIceCandidateReceived(sdp: String, sdpMid: String, sdpMLineIndex: Int) {
        webRTCManager?.addIceCandidate(IceCandidate(sdpMid, sdpMLineIndex, sdp))
    }

    override fun onCleared() {
        super.onCleared()
        reconnectionJob?.cancel()
        connectionTimeout?.cancel()
        webRTCManager?.close()
        signalingClient?.close()
        eglBase.release()
    }
}