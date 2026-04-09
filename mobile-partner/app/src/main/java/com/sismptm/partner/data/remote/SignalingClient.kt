package com.sismptm.partner.data.remote

import android.util.Log
import com.google.gson.Gson
import okhttp3.*
import okio.ByteString

/**
 * Signaling client to handle WebSocket communication for WebRTC handshake.
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

    fun connect() {
        val request = Request.Builder().url(serverUrl).build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "WebSocket Connected")
                listener.onConnected()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d(TAG, "Message received: $text")
                try {
                    val message = gson.fromJson(text, SignalingMessage::class.java)
                    when (message.type) {
                        "offer" -> message.sdp?.let { listener.onOfferReceived(it) }
                        "answer" -> message.sdp?.let { listener.onAnswerReceived(it) }
                        "candidate" -> message.candidate?.let { listener.onIceCandidateReceived(it) }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing message", e)
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket Closing: $reason")
                listener.onDisconnected()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket Failure: ${t.message}")
                listener.onError(t.message ?: "Unknown error")
            }
        })
    }

    fun sendMessage(message: SignalingMessage) {
        val json = gson.toJson(message)
        Log.d(TAG, "Sending message: $json")
        webSocket?.send(json)
    }

    fun disconnect() {
        webSocket?.close(1000, "Normal closure")
    }
}
