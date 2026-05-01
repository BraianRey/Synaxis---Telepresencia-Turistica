package com.sismptm.client.ui.features.streaming

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sismptm.client.data.remote.signaling.SignalingClient
import com.sismptm.client.data.remote.signaling.SignalingClientListener
import com.sismptm.client.manager.webrtc.WebRTCManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.webrtc.*
import kotlin.random.Random

/**
 * ViewModel for the Client streaming screen.
 * Manages the WebRTC session, signaling, and connection state monitoring.
 */
class StreamingViewModel(application: Application) :
    AndroidViewModel(application), SignalingClientListener {

    private val TAG = "ClientStreamingViewModel"
    private var signalingClient: SignalingClient? = null
    private var webRTCManager: WebRTCManager? = null
    private var partnerId: String = ""
    private var connectionTimeout: Job? = null

    private var reconnectionAttempts = 0
    private val maxReconnectionAttempts = 10 // Increased for better field resilience
    private val reconnectionDelays = listOf(2000L, 4000L, 8000L, 10000L)
    private var reconnectionJob: Job? = null
    private var qualityMonitorJob: Job? = null

    private val _connectionState = MutableStateFlow(PeerConnection.PeerConnectionState.NEW)
    val connectionState: StateFlow<PeerConnection.PeerConnectionState> = _connectionState

    // Quality metrics for degradation detection
    private var lastFramesDecoded = 0L
    private var lastTimestampMs = 0L
    private var lowFpsCount = 0

    val eglBase: EglBase = EglBase.create()

    /**
     * Initializes the WebRTC connection.
     */
    fun startConnection(serviceId: String, onRemoteTrack: (VideoTrack) -> Unit) {
        webRTCManager?.close()
        signalingClient?.close()
        
        this.partnerId = serviceId
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
            delay(30000) // 30s timeout for initial connection
            if (_connectionState.value != PeerConnection.PeerConnectionState.CONNECTED) {
                Log.e(TAG, "Initial Connection Timeout - Retrying...")
                attemptRejoin()
            }
        }

        signalingClient?.connect()
    }

    fun onConnectionStateChanged(state: PeerConnection.PeerConnectionState) {
        Log.d(TAG, "PeerConnection State Changed: $state")
        _connectionState.value = state
        
        when (state) {
            PeerConnection.PeerConnectionState.CONNECTED -> {
                connectionTimeout?.cancel()
                reconnectionJob?.cancel()
                reconnectionAttempts = 0
                startQualityMonitoring()
            }
            PeerConnection.PeerConnectionState.DISCONNECTED, 
            PeerConnection.PeerConnectionState.FAILED,
            PeerConnection.PeerConnectionState.CLOSED -> {
                stopQualityMonitoring()
                startReconnectionTimer()
            }
            else -> {}
        }
    }

    private fun startQualityMonitoring() {
        qualityMonitorJob?.cancel()
        lowFpsCount = 0
        qualityMonitorJob = viewModelScope.launch {
            delay(5000)
            while (true) {
                delay(3000)
                webRTCManager?.getStats { report -> analyzeStats(report) }
            }
        }
    }

    private fun stopQualityMonitoring() {
        qualityMonitorJob?.cancel()
        qualityMonitorJob = null
    }

    private fun analyzeStats(report: RTCStatsReport) {
        report.statsMap.values.find { it.type == "inbound-rtp" && it.members["kind"] == "video" }?.let { videoStats ->
            val framesDecoded = (videoStats.members["framesDecoded"] as? Number)?.toLong() ?: 0L
            val currentTimestampMs = (videoStats.timestampUs / 1000.0).toLong()

            if (lastTimestampMs != 0L) {
                val timeDiffSec = (currentTimestampMs - lastTimestampMs) / 1000.0
                val framesDiff = framesDecoded - lastFramesDecoded
                val fps = if (timeDiffSec > 0) framesDiff / timeDiffSec else 0.0

                if (fps < 12.0 && framesDiff > 0) {
                    lowFpsCount++
                    if (lowFpsCount >= 4) {
                        Log.w(TAG, "Persistent Low FPS ($fps), refreshing...")
                        lowFpsCount = 0
                        refreshConnection()
                    }
                } else {
                    lowFpsCount = 0
                }
            }
            lastFramesDecoded = framesDecoded
            lastTimestampMs = currentTimestampMs
        }
    }

    private fun refreshConnection() {
        signalingClient?.sendJoin(partnerId)
    }

    private fun startReconnectionTimer() {
        if (reconnectionJob?.isActive == true) return
        if (reconnectionAttempts >= maxReconnectionAttempts) {
            Log.e(TAG, "Max reconnection attempts reached")
            return
        }

        val delayMs = reconnectionDelays.getOrElse(reconnectionAttempts) { 10000L }
        Log.i(TAG, "Scheduling reconnection attempt ${reconnectionAttempts + 1} in ${delayMs}ms")

        reconnectionJob = viewModelScope.launch {
            delay(delayMs)
            if (_connectionState.value != PeerConnection.PeerConnectionState.CONNECTED) {
                attemptRejoin()
            }
        }
    }

    private fun attemptRejoin() {
        reconnectionAttempts++
        // Reset PeerConnection for a clean start on failure
        webRTCManager?.createPeerConnection()
        signalingClient?.sendJoin(partnerId)
    }

    fun sendCommand(command: String) {
        signalingClient?.sendCommand(command, partnerId)
    }

    override fun onConnectionOpened() {
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
        qualityMonitorJob?.cancel()
        connectionTimeout?.cancel()
        webRTCManager?.close()
        signalingClient?.close()
        eglBase.release()
    }
}
