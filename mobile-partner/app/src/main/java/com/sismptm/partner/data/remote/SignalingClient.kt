package com.sismptm.partner.data.remote

import android.util.Log
import com.google.gson.Gson
import okhttp3.*
import okio.ByteString

/**
 * WebSocket client for WebRTC signaling.
 *
 * Connects to the RAW WebSocket server (not STOMP).
 * The full URL including ?peerId=XXX must be passed when calling connect().
 *
 * Protocol:
 *  - Send: JSON with "type", "sdp"/"candidate", "senderId", "targetId"
 *  - Receive: JSON with same structure, routed by server based on targetId
 */
class SignalingClient(
    private val serverUrl: String,
    private val listener: SignalingListener
) {
    private val TAG = "SignalingClient"
    private val client = OkHttpClient()
    private var webSocket: WebSocket? = null
    private val gson = Gson()

    interface SignalingListener {
        fun onOfferReceived(sdp: String)
        fun onAnswerReceived(sdp: String)
        fun onIceCandidateReceived(candidate: IceCandidateModel)
        fun onConnected()
        fun onDisconnected()
        fun onError(error: String)
    }

    /**
     * Opens the WebSocket connection. The serverUrl must already contain the peerId
     * query param (e.g., ws://192.168.18.9:8081/ws-signaling?peerId=PARTNER_01).
     */
    fun connect() {
        Log.d(TAG, "Connecting to: $serverUrl")
        val request = Request.Builder().url(serverUrl).build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
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

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closing: code=$code reason=$reason")
                webSocket.close(1000, null)
                listener.onDisconnected()
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closed: code=$code reason=$reason")
                listener.onDisconnected()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket failure: ${t.message} | Response: ${response?.code}")
                listener.onError(t.message ?: "Connection error")
            }
        })
    }

    /**
     * Sends a signaling message (offer, answer, or ICE candidate) as JSON.
     */
    fun sendMessage(message: SignalingMessage) {
        val json = gson.toJson(message)
        Log.d(TAG, "Sending: $json")
        val sent = webSocket?.send(json) ?: false
        if (!sent) {
            Log.e(TAG, "Failed to send message — WebSocket may be closed")
        }
    }

    private fun handleMessage(text: String) {
        try {
            val message = gson.fromJson(text, SignalingMessage::class.java)
            when (message.type) {
                "offer"     -> message.sdp?.let { listener.onOfferReceived(it) }
                "answer"    -> message.sdp?.let { listener.onAnswerReceived(it) }
                "candidate" -> message.candidate?.let { listener.onIceCandidateReceived(it) }
                "error"     -> Log.e(TAG, "Server error: ${message.type}")
                else        -> Log.w(TAG, "Unknown message type: ${message.type}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing message: ${e.message} | Raw: $text")
        }
    }

    fun disconnect() {
        webSocket?.close(1000, "Normal closure")
        client.dispatcher.executorService.shutdown()
    }
}
