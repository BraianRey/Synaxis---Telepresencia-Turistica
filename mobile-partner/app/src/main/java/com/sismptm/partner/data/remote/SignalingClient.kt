package com.sismptm.partner.data.remote

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.gson.Gson
import okhttp3.*
import okio.ByteString
import java.util.concurrent.TimeUnit

/**
 * WebSocket client for WebRTC signaling with Auto-Reconnection and Ping/Pong.
 */
class SignalingClient(private val serverUrl: String, private val listener: SignalingListener) {
    private val TAG = "SignalingClient"

    // Requirement 1: Ping/Pong - 15 seconds interval to keep connection alive
    private val client = OkHttpClient.Builder()
        .pingInterval(15, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private val gson = Gson()
    private val mainHandler = Handler(Looper.getMainLooper())

    private var isManualDisconnect = false
    private var reconnectInterval = 2000L // 2 seconds base

    interface SignalingListener {
        fun onOfferReceived(sdp: String)
        fun onAnswerReceived(sdp: String)
        fun onIceCandidateReceived(candidate: IceCandidateModel)
        fun onJoinReceived(senderId: String)
        fun onCommandReceived(command: String)
        fun onConnected()
        fun onDisconnected()
        fun onError(error: String)
    }

    fun connect() {
        isManualDisconnect = false
        Log.d(TAG, "Connecting to: $serverUrl")
        val request = Request.Builder().url(serverUrl).build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.i(TAG, "WebSocket connected successfully")
                reconnectInterval = 2000L // Reset interval on success
                mainHandler.post { listener.onConnected() }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleMessage(text)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket failure: ${t.message}")
                mainHandler.post { listener.onError(t.message ?: "Connection error") }
                attemptReconnect()
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.w(TAG, "WebSocket closed: $reason")
                mainHandler.post { listener.onDisconnected() }
                attemptReconnect()
            }
        })
    }

    private fun attemptReconnect() {
        if (isManualDisconnect) return

        // Requirement 1: Auto-reconnect every few seconds
        mainHandler.postDelayed({
            Log.i(TAG, "Attempting automatic reconnection...")
            connect()
        }, reconnectInterval)

        // Exponential backoff up to 10 seconds
        reconnectInterval = (reconnectInterval * 1.5).toLong().coerceAtMost(10000L)
    }

    fun sendMessage(message: SignalingMessage) {
        val json = gson.toJson(message)
        val sent = webSocket?.send(json) ?: false
        if (!sent) Log.e(TAG, "Failed to send message: WebSocket closed")
    }

    fun disconnect() {
        isManualDisconnect = true
        webSocket?.close(1000, "Normal closure")
    }

    private fun handleMessage(text: String) {
        try {
            val message = gson.fromJson(text, SignalingMessage::class.java)
            mainHandler.post {
                when (message.type) {
                    "offer" -> message.sdp?.let { listener.onOfferReceived(it) }
                    "answer" -> message.sdp?.let { listener.onAnswerReceived(it) }
                    "candidate" -> message.candidate?.let { listener.onIceCandidateReceived(it) }
                    "join" -> message.senderId?.let { listener.onJoinReceived(it) }
                    "command" -> message.text?.let { listener.onCommandReceived(it) }
                    else -> Log.w(TAG, "Unknown message type: ${message.type}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing message: ${e.message}")
        }
    }
}