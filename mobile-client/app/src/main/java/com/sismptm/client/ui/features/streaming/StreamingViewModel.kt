package com.sismptm.client.ui.features.streaming

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sismptm.client.core.network.RetrofitClient
import com.sismptm.client.data.remote.signaling.SignalingClient
import com.sismptm.client.data.remote.signaling.SignalingClientListener
import com.sismptm.client.manager.webrtc.WebRTCManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    private var serviceId: Long = 0L
    private var connectionTimeout: Job? = null

    private var reconnectionAttempts = 0
    private val maxReconnectionAttempts = 10 // Increased for better field resilience
    private val reconnectionDelays = listOf(2000L, 4000L, 8000L, 10000L)
    private var reconnectionJob: Job? = null
    private var qualityMonitorJob: Job? = null
    private var connectivityManager: ConnectivityManager? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    private val _connectionState = MutableStateFlow(PeerConnection.PeerConnectionState.NEW)
    val connectionState: StateFlow<PeerConnection.PeerConnectionState> = _connectionState

    private val _sessionEnded = MutableStateFlow(false)
    val sessionEnded: StateFlow<Boolean> = _sessionEnded.asStateFlow()

    // Quality metrics for degradation detection
    private var lastFramesDecoded = 0L
    private var lastTimestampMs = 0L
    private var lowFpsCount = 0

    val eglBase: EglBase = EglBase.create()

    private val cachedIceServers = mutableListOf<PeerConnection.IceServer>()

    private fun mapIceServers(infoList: List<com.sismptm.client.data.remote.api.dto.IceServerInfo>): List<PeerConnection.IceServer> {
        return infoList.map { info ->
            val builder = PeerConnection.IceServer.builder(info.urls)
            if (info.username != null) {
                builder.setUsername(info.username)
            }
            if (info.credential != null) {
                builder.setPassword(info.credential)
            }
            builder.createIceServer()
        }
    }

    /**
     * Initializes the WebRTC connection.
     */
    fun startConnection(serviceId: String, onRemoteTrack: (VideoTrack) -> Unit) {
        webRTCManager?.close()
        signalingClient?.close()
        
        this.partnerId = serviceId
        this.serviceId = serviceId.toLongOrNull() ?: 0L
        val myClientId = "CLIENT_$serviceId"

        signalingClient = SignalingClient(this, myClientId)
        
        viewModelScope.launch {
            val servers = try {
                val response = RetrofitClient.apiService.getIceServers()
                if (response.isSuccessful) {
                    response.body()?.iceServers?.let { mapIceServers(it) } ?: emptyList()
                } else {
                    Log.e(TAG, "Failed to load ICE servers from backend: ${response.code()}")
                    emptyList()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching ICE servers: ${e.message}")
                emptyList()
            }

            synchronized(cachedIceServers) {
                cachedIceServers.clear()
                cachedIceServers.addAll(servers)
            }

            webRTCManager = WebRTCManager(
                context = getApplication(),
                eglBase = eglBase,
                onRemoteTrack = onRemoteTrack
            ).apply {
                createPeerConnection(servers)
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
            registerNetworkCallback()
        }
    }

    /**
     * Pauses decoding/rendering of the remote view when the app goes to the background.
     */
    fun pauseDecoding() {
        webRTCManager?.detachRemoteView()
    }

    /**
     * Resumes rendering of the remote view when the app returns to the foreground.
     */
    fun resumeDecoding(surfaceViewRenderer: SurfaceViewRenderer) {
        webRTCManager?.attachRemoteView(surfaceViewRenderer)
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
                        Log.w(TAG, "Persistent Low FPS ($fps), triggering network ICE restart/renegotiation...")
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

    /**
     * Sends a join message. Since the Partner no longer reboots the camera on join, 
     * this effectively serves as a seamless network ICE restart / SDP renegotiation request.
     */
    private fun refreshConnection() {
        signalingClient?.sendJoin(partnerId)
    }

    private fun startReconnectionTimer() {
        if (reconnectionJob?.isActive == true) return
        if (reconnectionAttempts >= maxReconnectionAttempts) {
            Log.e(TAG, "Max reconnection attempts reached")
            checkServiceStatus()
            return
        }

        val delayMs = reconnectionDelays.getOrElse(reconnectionAttempts) { 10000L }
        Log.i(TAG, "Scheduling reconnection attempt ${reconnectionAttempts + 1} in ${delayMs}ms")

        reconnectionJob = viewModelScope.launch {
            delay(delayMs)
            if (_connectionState.value != PeerConnection.PeerConnectionState.CONNECTED) {
                if (isServiceCompleted()) {
                    Log.i(TAG, "Service completed detected before reconnect attempt. Navigating to summary.")
                    _sessionEnded.value = true
                    return@launch
                }
                attemptRejoin()
            }
        }
    }

    private fun checkServiceStatus() {
        if (serviceId == 0L) return
        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.getServiceById(serviceId)
                if (response.isSuccessful) {
                    val status = response.body()?.status
                    Log.i(TAG, "Service $serviceId status after give-up: $status")
                    if (status == "COMPLETED") {
                        _sessionEnded.value = true
                    }
                }
            } catch (e: Exception) {
                // Generic exception kept: Network error checking service status
                Log.e(TAG, "Error checking service status", e)
            }
        }
    }

    private suspend fun isServiceCompleted(): Boolean {
        if (serviceId == 0L) return false
        return try {
            val response = RetrofitClient.apiService.getServiceById(serviceId)
            if (response.isSuccessful) {
                val status = response.body()?.status
                Log.i(TAG, "Pre-reconnect status check: service $serviceId is $status")
                status == "COMPLETED"
            } else false
        } catch (e: Exception) {
            // Generic exception kept: Network error checking service status
            Log.e(TAG, "Error in pre-reconnect status check", e)
            false
        }
    }

    fun endSessionAndNavigate() {
        // Cancel all reconnection jobs
        reconnectionJob?.cancel()
        connectionTimeout?.cancel()

        // Stop WebRTC and signaling AFTER backend call
        viewModelScope.launch {
            try {
                // Notify backend that client is ending the session
                // This triggers payment calculation and sets status = COMPLETED
                val completeResponse = RetrofitClient.apiService
                    .completeServiceByClient(serviceId)
                if (completeResponse.isSuccessful) {
                    Log.i(TAG, "Service $serviceId completed by client successfully")
                } else {
                    Log.e(TAG, "Failed to complete service by client: " +
                        "${completeResponse.code()}")
                }
            } catch (e: Exception) {
                // Generic exception kept: Network error calling client-complete
                Log.e(TAG, "Error calling client-complete", e)
            } finally {
                // Close WebRTC and signaling after backend call attempt
                signalingClient?.close()
                webRTCManager?.close()
                // Navigate regardless — ServiceSummaryScreen handles missing payment
                _sessionEnded.value = true
            }
        }
    }

    private fun attemptRejoin() {
        reconnectionAttempts++
        // Reset PeerConnection for a clean start on failure
        val servers = synchronized(cachedIceServers) { ArrayList(cachedIceServers) }
        webRTCManager?.createPeerConnection(servers)
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

    private fun registerNetworkCallback() {
        val context = getApplication<Application>()
        connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                Log.i(TAG, "Network became available. Resetting reconnection counters.")
                reconnectionAttempts = 0
                signalingClient?.connect()
                viewModelScope.launch {
                    if (_connectionState.value != PeerConnection.PeerConnectionState.CONNECTED) {
                        Log.i(TAG, "Reconnection attempt triggered by network recovery.")
                        attemptRejoin()
                    }
                }
            }
        }
        try {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            connectivityManager?.registerNetworkCallback(request, networkCallback!!)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register network callback: ${e.message}")
        }
    }

    private fun unregisterNetworkCallback() {
        try {
            networkCallback?.let { connectivityManager?.unregisterNetworkCallback(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to unregister network callback: ${e.message}")
        }
    }

    override fun onCleared() {
        super.onCleared()
        unregisterNetworkCallback()
        reconnectionJob?.cancel()
        qualityMonitorJob?.cancel()
        connectionTimeout?.cancel()
        webRTCManager?.close()
        signalingClient?.close()
        eglBase.release()
    }
}
