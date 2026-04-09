package com.sismptm.partner.ui.screens

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import com.sismptm.partner.data.remote.IceCandidateModel
import com.sismptm.partner.data.remote.SignalingClient
import com.sismptm.partner.data.remote.SignalingMessage
import com.sismptm.partner.webrtc.WebRTCManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.webrtc.*

/**
 * ViewModel to orchestrate WebRTC streaming and signaling.
 */
class StreamingViewModel(application: Application) : AndroidViewModel(application), SignalingClient.SignalingListener, WebRTCManager.WebRTCListener {
    private val TAG = "StreamingViewModel"
    private var partnerId = "PARTNER_01"
    
    // Replace with your actual server IP
    private val signalingUrl = "ws://10.0.2.2:8087/ws/webrtc"
    
    private val signalingClient = SignalingClient(signalingUrl, this)
    private val webRTCManager = WebRTCManager(application, this)

    private val _connectionState = MutableStateFlow(PeerConnection.PeerConnectionState.NEW)
    val connectionState: StateFlow<PeerConnection.PeerConnectionState> = _connectionState

    private val _commands = MutableStateFlow<List<String>>(emptyList())
    val commands: StateFlow<List<String>> = _commands

    private val _lastCommand = MutableStateFlow<String?>(null)
    val lastCommand: StateFlow<String?> = _lastCommand

    fun initStreaming(surfaceViewRenderer: SurfaceViewRenderer, customId: String) {
        this.partnerId = customId
        webRTCManager.startLocalCapture(surfaceViewRenderer)
        signalingClient.connect()
    }

    // --- SignalingListener ---
    override fun onConnected() {
        Log.d(TAG, "Signaling Connected as $partnerId. Creating Offer...")
        webRTCManager.createOffer()
    }

    override fun onOfferReceived(sdp: String) {
        webRTCManager.setRemoteDescription(sdp, true)
    }

    override fun onAnswerReceived(sdp: String) {
        Log.d(TAG, "Answer Received. Setting remote description...")
        webRTCManager.setRemoteDescription(sdp, false)
    }

    override fun onIceCandidateReceived(candidate: IceCandidateModel) {
        val iceCandidate = IceCandidate(candidate.sdpMid, candidate.sdpMLineIndex, candidate.candidate)
        webRTCManager.addIceCandidate(iceCandidate)
    }

    override fun onDisconnected() {
        Log.d(TAG, "Signaling Disconnected")
    }

    override fun onError(error: String) {
        Log.e(TAG, "Signaling Error: $error")
    }

    // --- WebRTCListener ---
    override fun onIceCandidate(candidate: IceCandidate) {
        signalingClient.sendMessage(SignalingMessage(
            type = "candidate",
            candidate = IceCandidateModel(
                sdpMid = candidate.sdpMid, 
                sdpMLineIndex = candidate.sdpMLineIndex, 
                candidate = candidate.sdp,
                targetId = partnerId
            ),
            targetId = partnerId
        ))
    }

    override fun onLocalSdpCreated(sdp: SessionDescription) {
        signalingClient.sendMessage(SignalingMessage(
            type = if (sdp.type == SessionDescription.Type.OFFER) "offer" else "answer",
            sdp = sdp.description,
            senderId = partnerId
        ))
    }

    override fun onCommandReceived(command: String) {
        _lastCommand.value = command
        val currentList = _commands.value.toMutableList()
        currentList.add(command)
        _commands.value = currentList.takeLast(3)
    }

    override fun onConnectionStateChange(state: PeerConnection.PeerConnectionState) {
        _connectionState.value = state
    }

    override fun onCleared() {
        super.onCleared()
        webRTCManager.dispose()
        signalingClient.disconnect()
    }
}
