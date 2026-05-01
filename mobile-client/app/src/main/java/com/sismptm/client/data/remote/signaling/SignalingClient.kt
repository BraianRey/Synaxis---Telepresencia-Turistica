package com.sismptm.client.data.remote.signaling

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.gson.Gson
import com.sismptm.client.core.network.NetworkConfig
import okhttp3.*
import java.util.concurrent.TimeUnit

/**
 * Listener interface for handling signaling events from the remote peer.
 */
interface SignalingClientListener {
    fun onConnectionOpened()
    fun onOfferReceived(sdp: String, senderId: String)
    fun onAnswerReceived(sdp: String)
    fun onIceCandidateReceived(sdp: String, sdpMid: String, sdpMLineIndex: Int)
}

/**
 * WebSocket client for WebRTC signaling that manages connection lifecycle and message routing.
 */
class SignalingClient(
    private val listener: SignalingClientListener,
    private val clientPeerId: String = "CLIENT_UNKNOWN"
) {
    private val TAG = "SignalingClient"
    private val gson = Gson()

    private val httpClient = OkHttpClient.Builder()
        .pingInterval(15, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private var webSocket: WebSocket? = null
    private var isClosing = false

    /**
     * Establishes a WebSocket connection to the signaling server.
     */
    fun connect() {
        if (isClosing) return
        val baseUrl = NetworkConfig.WS_SIGNALING_URL
        val url = if (baseUrl.contains("?")) "$baseUrl&peerId=$clientPeerId" else "$baseUrl?peerId=$clientPeerId"

        Log.d(TAG, "Connecting to $url")
        val request = Request.Builder().url(url).build()

        webSocket = httpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.i(TAG, "Signaling connected as $clientPeerId")
                listener.onConnectionOpened()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleMessage(text)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                if (isClosing) return
                Log.e(TAG, "Signaling failure: ${t.message}. Reconnecting in 3s...")
                Handler(Looper.getMainLooper()).postDelayed({ connect() }, 3000)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "Signaling closing: $reason")
            }
        })
    }

    fun isConnected(): Boolean = webSocket != null

    fun sendJoin(targetId: String) {
        val message = mapOf("type" to "join", "senderId" to clientPeerId, "targetId" to targetId)
        sendRaw(gson.toJson(message))
    }

    fun sendSdp(type: String, sdp: String, targetId: String) {
        val message = mapOf("type" to type, "sdp" to sdp, "senderId" to clientPeerId, "targetId" to targetId)
        sendRaw(gson.toJson(message))
    }

    fun sendIceCandidate(sdp: String, sdpMid: String, sdpMLineIndex: Int, targetId: String) {
        val candidateMap = mapOf("candidate" to sdp, "sdpMid" to sdpMid, "sdpMLineIndex" to sdpMLineIndex)
        val message = mapOf("type" to "candidate", "candidate" to candidateMap, "senderId" to clientPeerId, "targetId" to targetId)
        sendRaw(gson.toJson(message))
    }

    /**
     * Sends a command message (UP, DOWN, etc) to the target peer.
     */
    fun sendCommand(command: String, targetId: String) {
        val message = mapOf(
            "type" to "command",
            "text" to command,
            "senderId" to clientPeerId,
            "targetId" to targetId
        )
        sendRaw(gson.toJson(message))
    }

    private fun sendRaw(json: String) {
        val sent = webSocket?.send(json) ?: false
        if (!sent) Log.w(TAG, "Failed to send message (socket disconnected)")
    }

    private fun handleMessage(text: String) {
        try {
            val map = gson.fromJson(text, Map::class.java)
            val msgType = map["type"] as? String ?: return

            when (msgType) {
                "offer" -> {
                    val sdp = map["sdp"] as? String ?: return
                    val senderId = map["senderId"] as? String ?: ""
                    listener.onOfferReceived(sdp, senderId)
                }
                "answer" -> {
                    val sdp = map["sdp"] as? String ?: return
                    listener.onAnswerReceived(sdp)
                }
                "candidate" -> {
                    val candidateObj = map["candidate"] as? Map<*, *> ?: return
                    val candidateSdp = candidateObj["candidate"] as? String ?: return
                    val sdpMid = candidateObj["sdpMid"] as? String ?: return
                    val sdpMLineIndex = (candidateObj["sdpMLineIndex"] as? Double)?.toInt() ?: return
                    listener.onIceCandidateReceived(candidateSdp, sdpMid, sdpMLineIndex)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling message: ${e.message}")
        }
    }

    fun close() {
        isClosing = true
        webSocket?.close(1000, "Closed by user")
    }
}
