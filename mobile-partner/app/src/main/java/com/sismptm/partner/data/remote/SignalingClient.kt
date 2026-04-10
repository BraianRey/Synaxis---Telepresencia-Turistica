package com.sismptm.partner.data.remote

import android.util.Log
import com.google.gson.Gson
import okhttp3.*
import okio.ByteString

/**
 * WebSocket client for WebRTC signaling.
 *
 * Connects to the RAW WebSocket server (not STOMP). The full URL including ?peerId=XXX must be
 * passed when calling connect().
 *
 * Protocol:
 * - Send: JSON with "type", "sdp"/"candidate", "senderId", "targetId"
 * - Receive: JSON with same structure, routed by server based on targetId
 */
class SignalingClient(private val serverUrl: String, private val listener: SignalingListener) {
    private val TAG = "SignalingClient"
    private val client = OkHttpClient()
    private var webSocket: WebSocket? = null
    private val gson = Gson()

    /**
     * WebSocket listener interface for signaling events.
     *
     * Implementers receive notifications for offer/answer SDP, ICE candidates, and other signaling
     * events.
     */
    interface SignalingListener {
        /**
         * Called when an SDP offer is received.
         *
         * @param sdp The offer SDP string
         */
        fun onOfferReceived(sdp: String)

        /**
         * Called when an SDP answer is received.
         *
         * @param sdp The answer SDP string
         */
        fun onAnswerReceived(sdp: String)

        /**
         * Called when an ICE candidate is received.
         *
         * @param candidate The ICE candidate with SDP, sdpMid, and sdpMLineIndex
         */
        fun onIceCandidateReceived(candidate: IceCandidateModel)

        /**
         * Called when a peer sends a join message.
         *
         * @param senderId The ID of the peer requesting to join
         */
        fun onJoinReceived(senderId: String)

        /** Called when the WebSocket connection is successfully established. */
        fun onConnected()

        /** Called when the WebSocket connection is closed or lost. */
        fun onDisconnected()

        /**
         * Called when a signaling error occurs.
         *
         * @param error Error message describing the failure
         */
        fun onError(error: String)
    }

    /**
     * Opens the WebSocket connection.
     *
     * The serverUrl must already contain the peerId query parameter. Example:
     * ws://192.168.18.9:8081/ws-signaling?peerId=PARTNER_01
     */
    fun connect() {
        Log.d(TAG, "Connecting to: $serverUrl")
        val request = Request.Builder().url(serverUrl).build()
        webSocket =
                client.newWebSocket(
                        request,
                        object : WebSocketListener() {
                            override fun onOpen(webSocket: WebSocket, response: Response) {
                                Log.d(TAG, "WebSocket connected successfully")
                                listener.onConnected()
                            }

                            override fun onMessage(webSocket: WebSocket, text: String) {
                                Log.d(TAG, "Message received: $text")
                                handleMessage(text)
                            }

                            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                                handleMessage(bytes.utf8())
                            }

                            override fun onClosing(
                                    webSocket: WebSocket,
                                    code: Int,
                                    reason: String
                            ) {
                                Log.d(TAG, "WebSocket closing: code=$code reason=$reason")
                                webSocket.close(1000, null)
                                listener.onDisconnected()
                            }

                            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                                Log.d(TAG, "WebSocket closed: code=$code reason=$reason")
                                listener.onDisconnected()
                            }

                            override fun onFailure(
                                    webSocket: WebSocket,
                                    t: Throwable,
                                    response: Response?
                            ) {
                                Log.e(
                                        TAG,
                                        "WebSocket failure: ${t.message} | Response: ${response?.code}"
                                )
                                listener.onError(t.message ?: "Connection error")
                            }
                        }
                )
    }

    /**
     * Sends a signaling message (offer, answer, or ICE candidate) as JSON.
     *
     * @param message The signaling message to send
     */
    fun sendMessage(message: SignalingMessage) {
        val json = gson.toJson(message)
        Log.d(TAG, "Sending: $json")
        val sent = webSocket?.send(json) ?: false
        if (!sent) {
            Log.e(TAG, "Failed to send message — WebSocket may be closed")
        }
    }

    /**
     * Closes the WebSocket connection and shuts down the HTTP dispatcher.
     *
     * Call this when done with signaling to release resources.
     */
    fun disconnect() {
        webSocket?.close(1000, "Normal closure")
        client.dispatcher.executorService.shutdown()
    }

    private fun handleMessage(text: String) {
        try {
            val message = gson.fromJson(text, SignalingMessage::class.java)
            when (message.type) {
                "offer" -> {
                    if (message.sdp != null) {
                        Log.d(TAG, "[OK] Offer received")
                        listener.onOfferReceived(message.sdp!!)
                    } else {
                        Log.w(TAG, "[WARN] Offer message without SDP")
                    }
                }
                "answer" -> {
                    if (message.sdp != null) {
                        Log.d(TAG, "[OK] Answer received")
                        listener.onAnswerReceived(message.sdp!!)
                    } else {
                        Log.w(TAG, "[WARN] Answer message without SDP")
                    }
                }
                "candidate" -> {
                    if (message.candidate != null) {
                        Log.d(TAG, "[OK] ICE candidate received")
                        listener.onIceCandidateReceived(message.candidate!!)
                    } else {
                        Log.w(TAG, "[WARN] Candidate message without candidate object")
                    }
                }
                "join" -> {
                    if (message.senderId != null) {
                        Log.d(TAG, "[OK] Join received from: ${message.senderId}")
                        listener.onJoinReceived(message.senderId!!)
                    } else {
                        Log.w(TAG, "[WARN] Join message without senderId")
                    }
                }
                "error" -> Log.e(TAG, "[ERROR] Server error: ${message.type}")
                else -> Log.w(TAG, "[WARN] Unknown message type: ${message.type}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "[ERROR] Error parsing message: ${e.message} | Raw: $text", e)
        }
    }
}
