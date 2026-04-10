package com.sismptm.client.data.remote

import com.google.gson.Gson
import com.sismptm.client.utils.NetworkConfig
import okhttp3.*
import okio.ByteString

/**
 * Listener interface for WebRTC signaling events.
 *
 * Implementers receive notifications when:
 * - WebSocket connection opens
 * - SDP offers/answers are received
 * - ICE candidates are discovered
 */
interface SignalingClientListener {
    /** Called when the WebSocket connection is successfully established. */
    fun onConnectionOpened()

    /**
     * Called when an SDP offer is received from the remote peer.
     *
     * @param sdp The SDP offer string
     * @param senderId The ID of the peer sending the offer
     */
    fun onOfferReceived(sdp: String, senderId: String)

    /**
     * Called when an SDP answer is received from the remote peer.
     *
     * @param sdp The SDP answer string
     */
    fun onAnswerReceived(sdp: String)

    /**
     * Called when an ICE candidate is received from the remote peer.
     *
     * @param sdp The candidate SDP
     * @param sdpMid Media stream ID
     * @param sdpMLineIndex Media line index
     */
    fun onIceCandidateReceived(sdp: String, sdpMid: String, sdpMLineIndex: Int)
}

/**
 * WebSocket client for WebRTC signaling (Client/viewer side).
 *
 * Connects to a RAW WebSocket signaling server and registers a peer ID to route messages
 * appropriately. All messages are forwarded to the configured [SignalingClientListener].
 *
 * ## Message Flow
 *
 * - **join**: Sends a join request to trigger an offer from the Partner
 * - **offer**: Receives Partner's SDP offer
 * - **answer**: Not expected on Client side but handled for robustness
 * - **candidate**: Receives ICE candidates from Partner
 *
 * ## Important
 *
 * The CLIENT_ID ("CLIENT_01") must match what the Partner app uses as targetId when requesting an
 * offer. Keep them synchronized between both apps for testing.
 *
 * @param listener Callback interface for signaling events
 * @see SignalingClientListener
 */
class SignalingClient(private val listener: SignalingClientListener) {
    private val gson = Gson()
    private val httpClient = OkHttpClient()
    private var webSocket: WebSocket? = null

    companion object {
        /**
         * Client peer ID used to register with the signaling server. The Partner must use this same
         * value as targetId when sending its offer.
         */
        const val CLIENT_PEER_ID = "CLIENT_01"
    }

    /**
     * Connects to the signaling server, registering this session as CLIENT_PEER_ID.
     *
     * The URL is built from NetworkConfig.WS_SIGNALING_URL (local.properties → BuildConfig) with
     * the peerId appended as a query param.
     */
    fun connect() {
        val baseUrl = NetworkConfig.WS_SIGNALING_URL
        val url =
                if (baseUrl.contains("?")) {
                    "$baseUrl&peerId=$CLIENT_PEER_ID"
                } else {
                    "$baseUrl?peerId=$CLIENT_PEER_ID"
                }

        println("[START] SignalingClient: connecting to $url")
        System.err.println("[START] SignalingClient: connecting to $url")

        val request = Request.Builder().url(url).build()
        webSocket =
                httpClient.newWebSocket(
                        request,
                        object : WebSocketListener() {
                            override fun onOpen(webSocket: WebSocket, response: Response) {
                                println(
                                        "[CONNECTED] SignalingClient: OK! connected as $CLIENT_PEER_ID"
                                )
                                System.err.println(
                                        "[CONNECTED] SignalingClient: OK! connected as $CLIENT_PEER_ID"
                                )
                                listener.onConnectionOpened()
                            }

                            override fun onMessage(webSocket: WebSocket, text: String) {
                                println(
                                        "[MSG] SignalingClient: Message received: ${text.take(100)}..."
                                )
                                System.err.println(
                                        "[MSG] SignalingClient: Message received: ${text.take(100)}..."
                                )
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
                                webSocket.close(1000, null)
                            }

                            override fun onFailure(
                                    webSocket: WebSocket,
                                    t: Throwable,
                                    response: Response?
                            ) {
                                println(
                                        "[ERROR] SignalingClient: FAILURE: ${t.message} | response: ${response?.code}"
                                )
                                System.err.println(
                                        "[ERROR] SignalingClient: FAILURE: ${t.message} | response: ${response?.code}"
                                )
                                t.printStackTrace()
                            }
                        }
                )
    }

    /**
     * Sends a 'join' message to request an SDP offer from the target Partner.
     *
     * @param targetId The Partner's ID
     */
    fun sendJoin(targetId: String) {
        val message = mapOf("type" to "join", "senderId" to CLIENT_PEER_ID, "targetId" to targetId)
        sendRaw(gson.toJson(message))
    }

    /**
     * Sends an SDP offer or answer to the target peer.
     *
     * @param type "offer" or "answer"
     * @param sdp The SDP string
     * @param targetId The Partner's ID
     */
    fun sendSdp(type: String, sdp: String, targetId: String) {
        val message =
                mapOf(
                        "type" to type,
                        "sdp" to sdp,
                        "senderId" to CLIENT_PEER_ID,
                        "targetId" to targetId
                )
        sendRaw(gson.toJson(message))
    }

    /**
     * Sends an ICE candidate to the target peer.
     *
     * Wraps the candidate in a nested object structure expected by the receiving peer.
     *
     * @param sdp The candidate SDP
     * @param sdpMid Media stream ID
     * @param sdpMLineIndex Media line index
     * @param targetId The Partner's ID
     */
    fun sendIceCandidate(sdp: String, sdpMid: String, sdpMLineIndex: Int, targetId: String) {
        val candidateMap =
                mapOf("candidate" to sdp, "sdpMid" to sdpMid, "sdpMLineIndex" to sdpMLineIndex)
        val message =
                mapOf(
                        "type" to "candidate",
                        "candidate" to candidateMap,
                        "senderId" to CLIENT_PEER_ID,
                        "targetId" to targetId
                )
        sendRaw(gson.toJson(message))
    }

    private fun sendRaw(json: String) {
        println("SignalingClient: Sending: $json")
        webSocket?.send(json)
    }

    private fun handleMessage(text: String) {
        try {
            println("[PROC] SignalingClient: handleMessage() START with: ${text.take(100)}")
            System.err.println(
                    "[PROC] SignalingClient: handleMessage() START with: ${text.take(100)}"
            )

            val map = gson.fromJson(text, Map::class.java)
            val msgType = map["type"]
            println("[PARSE] SignalingClient: Parsed type = '$msgType' from map keys: ${map.keys}")
            System.err.println(
                    "[PARSE] SignalingClient: Parsed type = '$msgType' from map keys: ${map.keys}"
            )

            when (msgType) {
                "offer" -> {
                    println("[OFFER] SignalingClient: processing offer")
                    System.err.println("[OFFER] SignalingClient: processing offer")
                    val sdp = map["sdp"] as? String
                    if (sdp != null) {
                        val senderId = map["senderId"] as? String ?: ""
                        println(
                                "[OFFER] SignalingClient: onOfferReceived called with SDP length=${sdp.length}"
                        )
                        System.err.println(
                                "[OFFER] SignalingClient: onOfferReceived called with SDP length=${sdp.length}"
                        )
                        listener.onOfferReceived(sdp, senderId)
                    } else {
                        println("[ERROR] SignalingClient: Offer missing SDP")
                    }
                }
                "answer" -> {
                    println("[ANSWER] SignalingClient: processing answer")
                    System.err.println("[ANSWER] SignalingClient: processing answer")
                    val sdp = map["sdp"] as? String
                    if (sdp != null) {
                        println(
                                "[ANSWER] SignalingClient: onAnswerReceived called with SDP length=${sdp.length}"
                        )
                        System.err.println(
                                "[ANSWER] SignalingClient: onAnswerReceived called with SDP length=${sdp.length}"
                        )
                        listener.onAnswerReceived(sdp)
                    } else {
                        println("[ERROR] SignalingClient: Answer missing SDP")
                    }
                }
                "candidate" -> {
                    println("[ICE] SignalingClient: processing candidate")
                    System.err.println("[ICE] SignalingClient: processing candidate")
                    val candidateObj = map["candidate"] as? Map<*, *>
                    if (candidateObj != null) {
                        val candidateSdp = candidateObj["candidate"] as? String
                        val sdpMid = candidateObj["sdpMid"] as? String
                        val sdpMLineIndexObj = candidateObj["sdpMLineIndex"]

                        println(
                                "[ICE] SignalingClient: candidate fields: sdp=${candidateSdp?.take(30)}, sdpMid=$sdpMid, mLineIndex=$sdpMLineIndexObj"
                        )
                        System.err.println(
                                "[ICE] SignalingClient: candidate fields: sdp=${candidateSdp?.take(30)}, sdpMid=$sdpMid, mLineIndex=$sdpMLineIndexObj"
                        )

                        if (candidateSdp != null && sdpMid != null && sdpMLineIndexObj != null) {
                            try {
                                val sdpMLineIndex =
                                        when (sdpMLineIndexObj) {
                                            is Double -> sdpMLineIndexObj.toInt()
                                            is Int -> sdpMLineIndexObj
                                            is Long -> sdpMLineIndexObj.toInt()
                                            else -> {
                                                println(
                                                        "[ERROR] SignalingClient: Invalid sdpMLineIndex type: ${sdpMLineIndexObj.javaClass.simpleName}"
                                                )
                                                null
                                            }
                                        }
                                                ?: return

                                if (sdpMLineIndex < 0) {
                                    println(
                                            "[WARN] SignalingClient: Negative sdpMLineIndex: $sdpMLineIndex — skipping"
                                    )
                                    return
                                }

                                println("[ICE] SignalingClient: accepting ICE candidate")
                                System.err.println("[ICE] SignalingClient: accepting ICE candidate")
                                listener.onIceCandidateReceived(candidateSdp, sdpMid, sdpMLineIndex)
                            } catch (e: NumberFormatException) {
                                println(
                                        "[ERROR] SignalingClient: Failed to parse sdpMLineIndex: $e"
                                )
                            }
                        } else {
                            println(
                                    "[WARN] SignalingClient: ICE candidate missing fields — " +
                                            "candidate=$candidateSdp, sdpMid=$sdpMid, sdpMLineIndex=$sdpMLineIndexObj"
                            )
                        }
                    } else {
                        println(
                                "[ERROR] SignalingClient: Candidate message missing 'candidate' object"
                        )
                    }
                }
                "error" -> {
                    println("[WARN] SignalingClient: ERROR branch entered: ${map["message"]}")
                    System.err.println(
                            "[WARN] SignalingClient: ERROR branch entered: ${map["message"]}"
                    )
                }
                else -> {
                    println(
                            "[WARN] SignalingClient: UNKNOWN TYPE branch: msgType='${map["type"]}' | Full map: $map"
                    )
                    System.err.println(
                            "[WARN] SignalingClient: UNKNOWN TYPE branch: msgType='${map["type"]}' | Full map: $map"
                    )
                }
            }
        } catch (e: Exception) {
            println("[ERROR] SignalingClient: Error handling message — ${e.message}")
            System.err.println("[ERROR] SignalingClient: Error handling message — ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * Closes the WebSocket connection and shuts down the HTTP dispatcher.
     *
     * Call this when cleaning up to release resources.
     */
    fun close() {
        webSocket?.close(1000, "Closed by user")
        httpClient.dispatcher.executorService.shutdown()
    }
}
