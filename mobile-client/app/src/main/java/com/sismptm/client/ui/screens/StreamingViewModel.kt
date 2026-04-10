package com.sismptm.client.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.sismptm.client.data.remote.*
import org.webrtc.*

/**
 * ViewModel for the Client streaming screen (viewer / answerer role).
 *
 * The EglBase is created here (in the ViewModel) so it survives Compose recompositions. The same
 * EglBase is shared with WebRTCManager to ensure consistent EGL context between the
 * PeerConnectionFactory and the SurfaceViewRenderer.
 *
 * Manages the connection lifecycle using SignalingClient to relay messages with the server and
 * WebRTCManager to handle the P2P WebRTC connection with the Partner.
 *
 * @see WebRTCManager
 * @see SignalingClient
 */
class StreamingViewModel(application: Application) :
        AndroidViewModel(application), SignalingClientListener {

    private var signalingClient: SignalingClient = SignalingClient(this)
    private var webRTCManager: WebRTCManager? = null
    private var partnerId: String = ""
    private var connectionTimeout: Thread? = null

    /** Shared EGL context for SurfaceViewRenderer initialization and WebRTC operations */
    val eglBase: EglBase = EglBase.create()

    /**
     * Initiates a connection to a Partner.
     *
     * Starts a 15-second timeout while waiting for the Partner's SDP offer. If the offer arrives
     * within the timeout, the timeout is cancelled and the connection proceeds normally. If no
     * offer arrives within 15 seconds, an error message is printed but the stream remains listening
     * for late arrivals.
     *
     * @param targetPartnerId The Partner's identifier to connect to
     * @param onRemoteTrack Callback invoked when the remote video track is received
     */
    fun startConnection(targetPartnerId: String, onRemoteTrack: (VideoTrack) -> Unit) {
        this.partnerId = targetPartnerId

        webRTCManager =
                WebRTCManager(
                                context = getApplication(),
                                eglBase = eglBase,
                                onRemoteTrack = onRemoteTrack
                        )
                        .apply {
                            createPeerConnection()
                            // Register ICE candidate callback
                            setOnIceCandidateCallback { candidate ->
                                signalingClient.sendIceCandidate(
                                        sdp = candidate.sdp,
                                        sdpMid = candidate.sdpMid,
                                        sdpMLineIndex = candidate.sdpMLineIndex,
                                        targetId = partnerId
                                )
                            }
                        }

        // Start connection timeout (15 seconds) — prevents infinite hang if offer never arrives
        connectionTimeout =
                Thread {
                    try {
                        Thread.sleep(15000) // 15 second timeout
                        println(
                                "[ERROR] StreamingViewModel: CONNECTION TIMEOUT — no offer received within 15s"
                        )
                        System.err.println(
                                "[ERROR] StreamingViewModel: CONNECTION TIMEOUT — no offer received within 15s"
                        )
                    } catch (e: InterruptedException) {
                        // Offer received before timeout — this is expected and good!
                        println(
                                "[OK] StreamingViewModel: Offer received before timeout — connection successful"
                        )
                    }
                }
                        .apply { start() }

        signalingClient.connect()
    }

    /**
     * Sends a directional command to the Partner.
     *
     * Forward to [WebRTCManager.sendCommand] via DataChannel.
     *
     * @param command Command string (e.g., "UP", "DOWN", "LEFT", "RIGHT")
     */
    fun sendCommand(command: String) {
        webRTCManager?.sendCommand(command)
    }

    // ─── SignalingClientListener Implementation ──────────────────────────────────

    /**
     * Called when the WebSocket connection to the signaling server is established.
     *
     * Sends a join request to the configured Partner after a small delay to ensure the connection
     * is fully ready.
     */
    override fun onConnectionOpened() {
        println("SignalingClient: Connected to signaling server as CLIENT_01")
        // Small delay to ensure connection is fully established
        Thread {
                    Thread.sleep(100)
                    if (partnerId.isNotBlank()) {
                        println("SignalingClient: Sending join request to '$partnerId'")
                        signalingClient.sendJoin(partnerId)
                    }
                }
                .start()
    }

    /**
     * Called when the Partner's SDP offer is received.
     *
     * Cancels the connection timeout (since offer arrived successfully), then:
     * 1. Sets the remote description (the Partner's offer)
     * 2. Creates an SDP answer
     * 3. Sets the local description (the answer)
     * 4. Sends the answer back to the Partner via signaling
     *
     * @param sdp The SDP offer string from the Partner
     * @param senderId The ID of the Partner sending the offer
     */
    override fun onOfferReceived(sdp: String, senderId: String) {
        // Cancel timeout — we received the offer!
        connectionTimeout?.interrupt()
        connectionTimeout = null

        println("SignalingClient: Offer received from '$senderId'")
        this.partnerId = senderId.ifBlank { partnerId }

        webRTCManager?.handleOffer(
                sdp,
                object : SimpleSdpObserver() {
                    override fun onSetSuccess() {
                        webRTCManager?.createAnswer(
                                object : SimpleSdpObserver() {
                                    override fun onCreateSuccess(desc: SessionDescription?) {
                                        desc?.let { answer ->
                                            webRTCManager?.setLocalDescription(
                                                    answer,
                                                    object : SimpleSdpObserver() {
                                                        override fun onSetSuccess() {
                                                            println(
                                                                    "SignalingClient: Sending answer to '$partnerId'"
                                                            )
                                                            signalingClient.sendSdp(
                                                                    "answer",
                                                                    answer.description,
                                                                    partnerId
                                                            )
                                                        }
                                                    }
                                            )
                                        }
                                    }
                                }
                        )
                    }
                }
        )
    }

    /**
     * Called when an SDP answer is received.
     *
     * This should NOT occur on the Client side (Client is the answerer in Unified Plan). This is a
     * safety check to catch unexpected behavior.
     *
     * @param sdp The unexpected answer SDP
     */
    override fun onAnswerReceived(sdp: String) {
        // Client is the answerer — it does not receive answers
        println("SignalingClient: Unexpected answer received on Client side — ignoring")
    }

    /**
     * Called when an ICE candidate is received from the Partner.
     *
     * Converts the candidate parameters to an [IceCandidate] object and passes it to WebRTCManager
     * for adding to the PeerConnection.
     *
     * @param sdp The candidate SDP
     * @param sdpMid The media stream ID
     * @param sdpMLineIndex The media line index
     */
    override fun onIceCandidateReceived(sdp: String, sdpMid: String, sdpMLineIndex: Int) {
        val candidate = IceCandidate(sdpMid, sdpMLineIndex, sdp)
        webRTCManager?.addIceCandidate(candidate)
    }

    /**
     * Lifecycle cleanup when the ViewModel is destroyed.
     *
     * Cancels timeout thread, closes WebRTC connection, closes signaling client, and releases the
     * shared EGL context.
     */
    override fun onCleared() {
        super.onCleared()
        connectionTimeout?.interrupt() // Clean up timeout thread
        webRTCManager?.close()
        signalingClient.close()
        eglBase.release()
    }
}

/**
 * Simple implementation of [SdpObserver] that logs success and failure events.
 *
 * Used for SDP callbacks where error handling is minimal (logging only). Subclass this and override
 * specific methods for custom behavior.
 */
open class SimpleSdpObserver : SdpObserver {
    /**
     * Called when an SDP description is successfully created.
     *
     * @param desc The created SessionDescription (contains SDP string and type)
     */
    override fun onCreateSuccess(desc: SessionDescription?) {
        println("[OK] SDP Created Successfully — Type: ${desc?.type}")
    }

    /** Called when an SDP description is successfully set. */
    override fun onSetSuccess() {
        println("[OK] SDP Set Successfully")
    }

    /**
     * Called when SDP description creation fails.
     *
     * @param error Error message describing the failure
     */
    override fun onCreateFailure(error: String?) {
        println("[ERROR] SDP Create FAILED: $error")
    }

    /**
     * Called when setting an SDP description fails.
     *
     * @param error Error message describing the failure
     */
    override fun onSetFailure(error: String?) {
        println("[ERROR] SDP Set FAILED: $error")
    }
}
