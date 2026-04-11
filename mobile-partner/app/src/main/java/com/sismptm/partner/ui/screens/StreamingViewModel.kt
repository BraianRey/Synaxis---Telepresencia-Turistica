package com.sismptm.partner.ui.screens

import android.app.Application
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sismptm.partner.BuildConfig
import com.sismptm.partner.R
import com.sismptm.partner.data.remote.IceCandidateModel
import com.sismptm.partner.data.remote.SignalingClient
import com.sismptm.partner.data.remote.SignalingMessage
import com.sismptm.partner.webrtc.WebRTCManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.webrtc.*

/**
 * ViewModel for the Partner streaming screen.
 */
class StreamingViewModel(application: Application) :
        AndroidViewModel(application),
        SignalingClient.SignalingListener,
        WebRTCManager.WebRTCListener {

    private val TAG = "StreamingViewModel"

    private var targetClientId: String? = null
    private var partnerId = "PARTNER_01"
    private var signalingClient: SignalingClient? = null
    private var mediaPlayer: MediaPlayer? = null
    
    // Reconexion Logic
    private var reconnectionAttempts = 0
    private val maxReconnectionAttempts = 5
    private var reconnectionJob: Job? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    val eglBase: EglBase = EglBase.create()

    private val webRTCManager =
            WebRTCManager(context = application, listener = this, eglBase = eglBase)

    private val _connectionState = MutableStateFlow(PeerConnection.PeerConnectionState.NEW)
    val connectionState: StateFlow<PeerConnection.PeerConnectionState> = _connectionState

    private val _commands = MutableStateFlow<List<String>>(emptyList())
    val commands: StateFlow<List<String>> = _commands

    private val _lastCommand = MutableStateFlow<String?>(null)
    val lastCommand: StateFlow<String?> = _lastCommand

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

    // ─── SignalingClient.SignalingListener ───────────────────────────────────

    override fun onConnected() {
        Log.d(TAG, "Signaling connected")
    }

    override fun onJoinReceived(senderId: String) {
        Log.d(TAG, "Client '$senderId' joined")
        this.targetClientId = senderId
        reconnectionAttempts = 0 // Reset on successful join
        webRTCManager.createOffer()
    }

    override fun onOfferReceived(sdp: String) {}

    override fun onAnswerReceived(sdp: String) {
        webRTCManager.setRemoteDescription(sdp, isOffer = false)
    }

    override fun onIceCandidateReceived(candidate: IceCandidateModel) {
        val iceCandidate = IceCandidate(candidate.sdpMid, candidate.sdpMLineIndex, candidate.candidate)
        webRTCManager.addIceCandidate(iceCandidate)
    }

    override fun onDisconnected() {
        Log.w(TAG, "Signaling disconnected - monitoring WebRTC state")
    }

    override fun onError(error: String) {
        Log.e(TAG, "Signaling error: $error")
    }

    // ─── WebRTCManager.WebRTCListener Implementation ───────────────────────────────────

    override fun onIceCandidate(candidate: IceCandidate) {
        val target = targetClientId ?: return
        signalingClient?.sendMessage(
                SignalingMessage(
                        type = "candidate",
                        candidate = IceCandidateModel(
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

    override fun onLocalSdpCreated(sdp: SessionDescription) {
        val target = targetClientId ?: return
        val type = if (sdp.type == SessionDescription.Type.OFFER) "offer" else "answer"
        signalingClient?.sendMessage(
                SignalingMessage(
                        type = type,
                        sdp = sdp.description,
                        senderId = partnerId,
                        targetId = target
                )
        )
    }

    override fun onCommandReceived(command: String) {
        _lastCommand.value = command
        _commands.value = (_commands.value + command).takeLast(3)
        playInstructionAudio(command.lowercase())
    }

    private fun playInstructionAudio(command: String) {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) it.stop()
                it.release()
            }
            mediaPlayer = null

            val resId = when (command) {
                "up" -> R.raw.up
                "down" -> R.raw.down
                "left" -> R.raw.left
                "right" -> R.raw.right
                else -> null
            }

            resId?.let {
                mediaPlayer = MediaPlayer.create(getApplication(), it).apply {
                    setOnCompletionListener { mp -> 
                        mp.release()
                        if (mediaPlayer == mp) mediaPlayer = null
                    }
                    start()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error playing audio: ${e.message}")
        }
    }

    /**
     * WebRTC Connection State Monitor.
     * Logic: If disconnected, wait 3s. If still disconnected, trigger ICE Restart.
     */
    override fun onConnectionStateChange(state: PeerConnection.PeerConnectionState) {
        Log.d(TAG, "PeerConnection → $state")
        _connectionState.value = state

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
            Log.d(TAG, "Connection lost. Waiting 3s for auto-recovery...")
            delay(3000)
            
            if (_connectionState.value != PeerConnection.PeerConnectionState.CONNECTED) {
                attemptRenegotiation()
            }
        }
    }

    private fun attemptRenegotiation() {
        if (reconnectionAttempts >= maxReconnectionAttempts) {
            Log.e(TAG, "Max reconnection attempts reached ($maxReconnectionAttempts)")
            return
        }

        reconnectionAttempts++
        Log.i(TAG, "Attempting Renegotiation (ICE Restart) #$reconnectionAttempts...")
        
        // Trigger a new offer from the Partner
        webRTCManager.createOffer() 
    }

    override fun onCleared() {
        super.onCleared()
        reconnectionJob?.cancel()
        mediaPlayer?.let {
            if (it.isPlaying) it.stop()
            it.release()
        }
        mediaPlayer = null
        webRTCManager.dispose()
        signalingClient?.disconnect()
    }
}
