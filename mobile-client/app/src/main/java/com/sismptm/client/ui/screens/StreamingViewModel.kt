package com.sismptm.client.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.sismptm.client.data.remote.*
import org.webrtc.*

/**
 * ViewModel for the Client streaming screen (viewer / answerer role).
 *
 * The EglBase is created here (in the ViewModel) so it survives Compose recompositions.
 * The same EglBase is shared with WebRTCManager to ensure consistent EGL context
 * between the PeerConnectionFactory and the SurfaceViewRenderer.
 */
class StreamingViewModel(application: Application) : AndroidViewModel(application), SignalingClientListener {

    private var signalingClient: SignalingClient = SignalingClient(this)
    private var webRTCManager: WebRTCManager? = null
    private var partnerId: String = ""

    /** Exposed so StreamingScreen can init the SurfaceViewRenderer with the same EGL context */
    val eglBase: EglBase = EglBase.create()

    fun startConnection(targetPartnerId: String, onRemoteTrack: (VideoTrack) -> Unit) {
        this.partnerId = targetPartnerId

        webRTCManager = WebRTCManager(
            context = getApplication(),
            eglBase = eglBase,
            onRemoteTrack = onRemoteTrack,
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

    fun sendCommand(command: String) {
        webRTCManager?.sendCommand(command)
    }

    // ─── SignalingClientListener ─────────────────────────────────────────────

    override fun onConnectionOpened() {
        println("SignalingClient: Connected to signaling server as CLIENT_01")
        // Client waits for the Partner's offer — nothing to send on connect
    }

    override fun onOfferReceived(sdp: String, senderId: String) {
        println("SignalingClient: Offer received from '$senderId'")
        this.partnerId = senderId.ifBlank { partnerId }

        webRTCManager?.handleOffer(sdp, object : SimpleSdpObserver() {
            override fun onSetSuccess() {
                webRTCManager?.createAnswer(object : SimpleSdpObserver() {
                    override fun onCreateSuccess(desc: SessionDescription?) {
                        desc?.let { answer ->
                            webRTCManager?.setLocalDescription(answer, object : SimpleSdpObserver() {
                                override fun onSetSuccess() {
                                    println("SignalingClient: Sending answer to '$partnerId'")
                                    signalingClient.sendSdp("answer", answer.description, partnerId)
                                }
                            })
                        }
                    }
                })
            }
        })
    }

    override fun onAnswerReceived(sdp: String) {
        // Client is the answerer — it does not receive answers
        println("SignalingClient: Unexpected answer received on Client side — ignoring")
    }

    override fun onIceCandidateReceived(sdp: String, sdpMid: String, sdpMLineIndex: Int) {
        val candidate = IceCandidate(sdpMid, sdpMLineIndex, sdp)
        webRTCManager?.addIceCandidate(candidate)
    }

    override fun onCleared() {
        super.onCleared()
        webRTCManager?.close()
        signalingClient.close()
        eglBase.release()
    }
}

open class SimpleSdpObserver : SdpObserver {
    override fun onCreateSuccess(desc: SessionDescription?) {}
    override fun onSetSuccess() {}
    override fun onCreateFailure(error: String?) { println("SDP Create Failure: $error") }
    override fun onSetFailure(error: String?) { println("SDP Set Failure: $error") }
}
