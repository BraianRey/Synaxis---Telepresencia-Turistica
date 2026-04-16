package com.sismptm.client.data.remote

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.gson.Gson
import com.sismptm.client.utils.NetworkConfig
import okhttp3.*
import okio.ByteString
import java.util.concurrent.TimeUnit

/**
 * Listener interface for handling signaling events from the remote peer.
 */
interface SignalingClientListener {
    /**
     * Called when the signaling connection is successfully established.
     */
    fun onConnectionOpened()

    /**
     * Called when an SDP offer is received.
     * @param sdp The session description protocol string.
     * @param senderId The identifier of the remote peer.
     */
    fun onOfferReceived(sdp: String, senderId: String)

    /**
     * Called when an SDP answer is received.
     * @param sdp The session description protocol string.
     */
    fun onAnswerReceived(sdp: String)

    /**
     * Called when a remote ICE candidate is received.
     * @param sdp The candidate description.
     * @param sdpMid The media stream identifier.
     * @param sdpMLineIndex The media line index.
     */
    fun onIceCandidateReceived(sdp: String, sdpMid: String, sdpMLineIndex: Int)
}

/**
 * WebSocket client for WebRTC signaling that manages connection lifecycle and message routing.
 */
class SignalingClient(private val listener: SignalingClientListener) {
    private val TAG = "SignalingClient"
    private val gson = Gson()
    
    /**
     * HTTP client configured with a ping interval to maintain the WebSocket connection.
     */
    private val httpClient = OkHttpClient.Builder()
        .pingInterval(15, TimeUnit.SECONDS) 
        .retryOnConnectionFailure(true)
        .build()

    private var webSocket: WebSocket? = null
    private var isClosing = false

    companion object {
        /**
         * Unique identifier for this client in the signaling server.
         */
        const val CLIENT_PEER_ID = "CLIENT_01"
    }

    /**
     * Establishes a WebSocket connection to the signaling server and handles reconnection logic.
     */
    fun connect() {
        if (isClosing) return
        val baseUrl = NetworkConfig.WS_SIGNALING_URL
        val url = if (baseUrl.contains("?")) "$baseUrl&peerId=$CLIENT_PEER_ID" else "$baseUrl?peerId=$CLIENT_PEER_ID"

        Log.d(TAG, "Connecting to $url")
        val request = Request.Builder().url(url).build()
        
        webSocket = httpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.i(TAG, "Signaling connected as $CLIENT_PEER_ID")
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

    /**
     * Returns true if the WebSocket connection is active.
     */
    fun isConnected(): Boolean = webSocket != null

    /**
     * Sends a request to join a session with a target peer.
     */
    fun sendJoin(targetId: String) {
        val message = mapOf("type" to "join", "senderId" to CLIENT_PEER_ID, "targetId" to targetId)
        sendRaw(gson.toJson(message))
    }

    /**
     * Sends an SDP message (offer or answer) to the target peer.
     */
    fun sendSdp(type: String, sdp: String, targetId: String) {
        val message = mapOf("type" to type, "sdp" to sdp, "senderId" to CLIENT_PEER_ID, "targetId" to targetId)
        sendRaw(gson.toJson(message))
    }

    /**
     * Sends a local ICE candidate to the target peer.
     */
    fun sendIceCandidate(sdp: String, sdpMid: String, sdpMLineIndex: Int, targetId: String) {
        val candidateMap = mapOf("candidate" to sdp, "sdpMid" to sdpMid, "sdpMLineIndex" to sdpMLineIndex)
        val message = mapOf("type" to "candidate", "candidate" to candidateMap, "senderId" to CLIENT_PEER_ID, "targetId" to targetId)
        sendRaw(gson.toJson(message))
    }

    /**
     * Sends a raw JSON string through the WebSocket connection.
     */
    private fun sendRaw(json: String) {
        val sent = webSocket?.send(json) ?: false
        if (!sent) Log.w(TAG, "Failed to send message (socket disconnected)")
    }

    /**
     * Parses incoming WebSocket messages and dispatches them to the listener.
     */
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

    /**
     * Closes the signaling connection and stops reconnection attempts.
     */
    fun close() {
        isClosing = true
        webSocket?.close(1000, "Closed by user")
    }
}
