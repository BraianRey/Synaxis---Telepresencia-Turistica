package com.sismptm.client.data.remote

import com.google.gson.Gson
import com.sismptm.client.utils.NetworkConfig
import okhttp3.*

/**
 * Interface to handle incoming signaling messages from the WebSocket.
 */
interface SignalingClientListener {
    fun onConnectionOpened()
    fun onOfferReceived(sdp: String, senderId: String)
    fun onAnswerReceived(sdp: String)
    fun onIceCandidateReceived(sdp: String, sdpMid: String, sdpMLineIndex: Int)
}

/**
 * WebSocket client to handle WebRTC signaling (SDP and ICE Candidates exchange).
 */
class SignalingClient(
    private val listener: SignalingClientListener
) {
    private val gson = Gson()
    private val client = OkHttpClient()
    private var webSocket: WebSocket? = null

    /**
     * Connects to the signaling server via WebSocket.
     */
    fun connect() {
        val request = Request.Builder()
            .url(NetworkConfig.WS_SIGNALING_URL)
            .build()
        
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                listener.onConnectionOpened()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleMessage(text)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000, null)
            }
        })
    }

    /**
     * Sends an SDP Offer/Answer to the peer.
     */
    fun sendSdp(type: String, sdp: String, targetId: String) {
        val message = mapOf(
            "type" to type,
            "sdp" to sdp,
            "targetId" to targetId
        )
        webSocket?.send(gson.toJson(message))
    }

    /**
     * Sends an ICE Candidate to the peer matching the Partner's expected nested structure.
     */
    fun sendIceCandidate(sdp: String, sdpMid: String, sdpMLineIndex: Int, targetId: String) {
        val candidateMap = mapOf(
            "candidate" to sdp,
            "sdpMid" to sdpMid,
            "sdpMLineIndex" to sdpMLineIndex
        )
        val message = mapOf(
            "type" to "candidate",
            "candidate" to candidateMap,
            "targetId" to targetId
        )
        webSocket?.send(gson.toJson(message))
    }

    private fun handleMessage(text: String) {
        try {
            val map = gson.fromJson(text, Map::class.java)
            when (map["type"]) {
                "offer" -> {
                    val sdp = map["sdp"] as String
                    val senderId = map["senderId"] as? String ?: ""
                    listener.onOfferReceived(sdp, senderId)
                }
                "answer" -> listener.onAnswerReceived(map["sdp"] as String)
                "candidate" -> {
                    // Handle nested candidate object from Partner
                    val candidateObj = map["candidate"] as? Map<*, *>
                    if (candidateObj != null) {
                        listener.onIceCandidateReceived(
                            candidateObj["candidate"] as String,
                            candidateObj["sdpMid"] as String,
                            (candidateObj["sdpMLineIndex"] as Double).toInt()
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun close() {
        webSocket?.close(1000, "Closed by user")
    }
}
