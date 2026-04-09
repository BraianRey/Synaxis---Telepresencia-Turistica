package com.sismptm.client.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.sismptm.client.data.remote.*
import org.webrtc.*

/**
 * ViewModel to manage WebRTC state and Signaling events.
 * Orchestrates the connection between the UI and the P2P logic.
 */
class StreamingViewModel(application: Application) : AndroidViewModel(application), SignalingClientListener {

    private var signalingClient: SignalingClient = SignalingClient(this)
    private var webRTCManager: WebRTCManager? = null
    private var partnerId: String = ""

    /**
     * Starts the connection process by connecting to the Signaling server.
     */
    fun startConnection(eglBase: EglBase, targetPartnerId: String, onRemoteStream: (MediaStream) -> Unit) {
        this.partnerId = targetPartnerId
        webRTCManager = WebRTCManager(
            context = getApplication(),
            eglBase = eglBase,
            onRemoteStream = onRemoteStream,
            onIceCandidate = { candidate ->
                signalingClient.sendIceCandidate(
                    sdp = candidate.sdp,
                    sdpMid = candidate.sdpMid,
                    sdpMLineIndex = candidate.sdpMLineIndex,
                    targetId = partnerId
                )
            }
        ).apply {
            createPeerConnection()
        }
        signalingClient.connect()
    }

    /**
     * Sends a directional command via P2P DataChannel.
     */
    fun sendCommand(command: String) {
        webRTCManager?.sendCommand(command)
    }

    // --- SignalingClientListener Implementation ---

    override fun onConnectionOpened() {
        // Ready to receive Offers from Partner
        println("Signaling: Connected to server")
    }

    override fun onOfferReceived(sdp: String, senderId: String) {
        // 1. Partner sends an Offer. We set it as Remote Description.
        webRTCManager?.handleOffer(sdp, object : SimpleSdpObserver() {
            override fun onSetSuccess() {
                // 2. Once set, we create our Answer.
                webRTCManager?.createAnswer(object : SimpleSdpObserver() {
                    override fun onCreateSuccess(desc: SessionDescription?) {
                        desc?.let { sdp ->
                            // 3. Set our Answer as Local Description.
                            webRTCManager?.setLocalDescription(sdp, object : SimpleSdpObserver() {
                                override fun onSetSuccess() {
                                    // 4. Send our Answer back to the Partner via Signaling server.
                                    signalingClient.sendSdp("answer", sdp.description, partnerId)
                                }
                            })
                        }
                    }
                })
            }
        })
    }

    override fun onAnswerReceived(sdp: String) {
        // Not expected for the Client in this flow (usually Client answers Partner's offer)
    }

    override fun onIceCandidateReceived(sdp: String, sdpMid: String, sdpMLineIndex: Int) {
        val candidate = IceCandidate(sdpMid, sdpMLineIndex, sdp)
        webRTCManager?.addIceCandidate(candidate)
    }

    override fun onCleared() {
        super.onCleared()
        webRTCManager?.close()
        signalingClient.close()
    }
}

/**
 * Base SdpObserver to reduce boilerplate code in the ViewModel.
 */
open class SimpleSdpObserver : SdpObserver {
    override fun onCreateSuccess(desc: SessionDescription?) {}
    override fun onSetSuccess() {}
    override fun onCreateFailure(error: String?) { println("SDP Create Failure: $error") }
    override fun onSetFailure(error: String?) { println("SDP Set Failure: $error") }
}
