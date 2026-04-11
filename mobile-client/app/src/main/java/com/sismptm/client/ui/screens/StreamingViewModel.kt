package com.sismptm.client.ui.screens

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sismptm.client.data.remote.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.webrtc.*

/**
 * ViewModel for the Client streaming screen (viewer / answerer role).
 * 
 * Adaptive Reconnection Logic:
 * - Monitors PeerConnection states.
 * - Wait 6s on disconnect (giving Partner priority to restart).
 * - Max 5 renegotiation attempts via signaling.
 */
class StreamingViewModel(application: Application) :
        AndroidViewModel(application), SignalingClientListener {

    private val TAG = "ClientStreamingViewModel"
    private var signalingClient: SignalingClient = SignalingClient(this)
    private var webRTCManager: WebRTCManager? = null
    private var partnerId: String = ""
    private var connectionTimeout: Thread? = null

    // Reconnection Logic
    private var reconnectionAttempts = 0
    private val maxReconnectionAttempts = 5
    private var reconnectionJob: Job? = null
    private var lastState: PeerConnection.PeerConnectionState = PeerConnection.PeerConnectionState.NEW

    val eglBase: EglBase = EglBase.create()

    fun startConnection(targetPartnerId: String, onRemoteTrack: (VideoTrack) -> Unit) {
        this.partnerId = targetPartnerId

        webRTCManager = WebRTCManager(
            context = getApplication(),
            eglBase = eglBase,
            onRemoteTrack = onRemoteTrack
        ).apply {
            createPeerConnection()
            setOnIceCandidateCallback { candidate ->
                signalingClient.sendIceCandidate(
                    sdp = candidate.sdp,
                    sdpMid = candidate.sdpMid,
                    sdpMLineIndex = candidate.sdpMLineIndex,
                    targetId = partnerId
                )
            }
        }

        // Monitoring state for reconnection
        // Note: In a real app, WebRTCManager should expose a Flow or Listener for this.
        // For this prototype, we'll monitor via onIceCandidateReceived or other callbacks if needed,
        // but the best way is adding a listener to WebRTCManager.
        
        connectionTimeout = Thread {
            try {
                Thread.sleep(15000)
                Log.e(TAG, "Initial Connection Timeout")
            } catch (e: InterruptedException) {}
        }.apply { start() }

        signalingClient.connect()
    }

    /**
     * Call this when PeerConnection state changes (Should be called from WebRTCManager observer)
     */
    fun onConnectionStateChanged(state: PeerConnection.PeerConnectionState) {
        Log.d(TAG, "PeerConnection State: $state")
        lastState = state
        
        when (state) {
            PeerConnection.PeerConnectionState.CONNECTED -> {
                reconnectionJob?.cancel()
                reconnectionAttempts = 0
            }
            PeerConnection.PeerConnectionState.DISCONNECTED,
            PeerConnection.PeerConnectionState.FAILED -> {
                startReconnectionTimer()
            }
            else -> {}
        }
    }

    private fun startReconnectionTimer() {
        if (reconnectionJob?.isActive == true) return
        
        reconnectionJob = viewModelScope.launch {
            Log.d(TAG, "Connection lost. Waiting 6s for Partner to restart...")
            delay(6000) // Client waits longer than Partner (3s) to avoid race conditions
            
            if (lastState != PeerConnection.PeerConnectionState.CONNECTED) {
                attemptRejoin()
            }
        }
    }

    private fun attemptRejoin() {
        if (reconnectionAttempts >= maxReconnectionAttempts) {
            Log.e(TAG, "Max rejoin attempts reached")
            return
        }
        
        reconnectionAttempts++
        Log.i(TAG, "Attempting Re-join #$reconnectionAttempts...")
        if (partnerId.isNotBlank()) {
            signalingClient.sendJoin(partnerId)
        }
    }

    fun sendCommand(command: String) {
        webRTCManager?.sendCommand(command)
    }

    // ─── SignalingClientListener Implementation ──────────────────────────────────

    override fun onConnectionOpened() {
        if (partnerId.isNotBlank()) {
            signalingClient.sendJoin(partnerId)
        }
    }

    override fun onOfferReceived(sdp: String, senderId: String) {
        connectionTimeout?.interrupt()
        connectionTimeout = null
        
        // If we receive an offer while disconnected, it's likely a renegotiation
        reconnectionJob?.cancel()
        
        this.partnerId = senderId.ifBlank { partnerId }

        webRTCManager?.handleOffer(sdp, object : SimpleSdpObserver() {
            override fun onSetSuccess() {
                webRTCManager?.createAnswer(object : SimpleSdpObserver() {
                    override fun onCreateSuccess(desc: SessionDescription?) {
                        desc?.let { answer ->
                            webRTCManager?.setLocalDescription(answer, object : SimpleSdpObserver() {
                                override fun onSetSuccess() {
                                    signalingClient.sendSdp("answer", answer.description, partnerId)
                                }
                            })
                        }
                    }
                })
            }
        })
    }

    override fun onAnswerReceived(sdp: String) {}

    override fun onIceCandidateReceived(sdp: String, sdpMid: String, sdpMLineIndex: Int) {
        val candidate = IceCandidate(sdpMid, sdpMLineIndex, sdp)
        webRTCManager?.addIceCandidate(candidate)
    }

    override fun onCleared() {
        super.onCleared()
        reconnectionJob?.cancel()
        connectionTimeout?.interrupt()
        webRTCManager?.close()
        signalingClient.close()
        eglBase.release()
    }
}

open class SimpleSdpObserver : SdpObserver {
    override fun onCreateSuccess(desc: SessionDescription?) {}
    override fun onSetSuccess() {}
    override fun onCreateFailure(error: String?) { Log.e("SDP", "Create FAILED: $error") }
    override fun onSetFailure(error: String?) { Log.e("SDP", "Set FAILED: $error") }
}
