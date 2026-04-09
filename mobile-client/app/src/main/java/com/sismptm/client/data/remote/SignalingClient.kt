package com.sismptm.client.data.remote

import com.google.gson.Gson
import com.sismptm.client.utils.NetworkConfig
import okhttp3.*
import okio.ByteString

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
 * WebSocket client for WebRTC signaling (Client/viewer side).
 *
 * Connects to the RAW WebSocket signaling server with a peerId so that the
 * server can route messages from the Partner to this specific session.
 *
 * The CLIENT_ID must match what the Partner uses as its targetId when sending
 * its offer. Keep them synchronized between both apps for testing.
 */
class SignalingClient(
    private val listener: SignalingClientListener
) {
    private val gson = Gson()
    private val httpClient = OkHttpClient()
    private var webSocket: WebSocket? = null

    companion object {
        /**
         * Client peer ID used to register with the signaling server.
         * The Partner must use this same value as targetId when sending its offer.
         */
        const val CLIENT_PEER_ID = "CLIENT_01"
    }

    /**
     * Connects to the signaling server, registering this session as CLIENT_PEER_ID.
     *
     * The URL is built from NetworkConfig.WS_SIGNALING_URL (local.properties → BuildConfig)
     * with the peerId appended as a query param.
     */
    fun connect() {
        val baseUrl = NetworkConfig.WS_SIGNALING_URL
        val url = if (baseUrl.contains("?")) {
            "$baseUrl&peerId=$CLIENT_PEER_ID"
        } else {
            "$baseUrl?peerId=$CLIENT_PEER_ID"
        }

        println("SignalingClient: Connecting to $url")

        val request = Request.Builder().url(url).build()
        webSocket = httpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                println("SignalingClient: Connected as $CLIENT_PEER_ID")
                listener.onConnectionOpened()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                println("SignalingClient: Message received: $text")
                handleMessage(text)
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                handleMessage(bytes.utf8())
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000, null)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                println("SignalingClient: Failure: ${t.message} | response: ${response?.code}")
            }
        })
    }

    /**
     * Sends an SDP Offer or Answer to the target peer.
     */
    fun sendSdp(type: String, sdp: String, targetId: String) {
        val message = mapOf(
            "type"     to type,
            "sdp"      to sdp,
            "senderId" to CLIENT_PEER_ID,
            "targetId" to targetId
        )
        sendRaw(gson.toJson(message))
    }

    /**
     * Sends an ICE Candidate to the target peer.
     * Uses the nested candidate object structure expected by the Partner's SignalingClient.
     */
    fun sendIceCandidate(sdp: String, sdpMid: String, sdpMLineIndex: Int, targetId: String) {
        val candidateMap = mapOf(
            "candidate"     to sdp,
            "sdpMid"        to sdpMid,
            "sdpMLineIndex" to sdpMLineIndex
        )
        val message = mapOf(
            "type"      to "candidate",
            "candidate" to candidateMap,
            "senderId"  to CLIENT_PEER_ID,
            "targetId"  to targetId
        )
        sendRaw(gson.toJson(message))
    }

    private fun sendRaw(json: String) {
        println("SignalingClient: Sending: $json")
        webSocket?.send(json)
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
                    val candidateObj = map["candidate"] as? Map<*, *>
                    if (candidateObj != null) {
                        listener.onIceCandidateReceived(
                            candidateObj["candidate"] as String,
                            candidateObj["sdpMid"] as String,
                            (candidateObj["sdpMLineIndex"] as Double).toInt()
                        )
                    }
                }
                "error" -> println("SignalingClient: Server error: ${map["message"]}")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun close() {
        webSocket?.close(1000, "Closed by user")
        httpClient.dispatcher.executorService.shutdown()
    }
}
