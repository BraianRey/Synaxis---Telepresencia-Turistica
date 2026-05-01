package com.sismptm.client.manager.webrtc

import android.app.ActivityManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.nio.ByteBuffer
import org.webrtc.*

/**
 * Manager for WebRTC PeerConnection on the Client (viewer) side.
 * Optimized for low-latency P2P streaming with device-aware constraints and surgical SDP injection.
 */
class WebRTCManager(
    private val context: Context,
    private val eglBase: EglBase,
    private val onRemoteTrack: (VideoTrack) -> Unit
) {
    companion object {
        private const val TAG = "WebRTCManager"
        private var isFactoryInitialized = false

        /**
         * Initializes the PeerConnectionFactory if it has not been initialized yet.
         */
        fun ensureInitialized(context: Context) {
            if (!isFactoryInitialized) {
                val options = PeerConnectionFactory.InitializationOptions.builder(context.applicationContext)
                    .setEnableInternalTracer(false)
                    .createInitializationOptions()
                PeerConnectionFactory.initialize(options)
                isFactoryInitialized = true
                Log.d(TAG, "PeerConnectionFactory initialized")
            }
        }
    }

    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null
    private var dataChannel: DataChannel? = null
    private var isDisposed = false

    private val mainHandler = Handler(Looper.getMainLooper())
    private val pendingCandidates = mutableListOf<IceCandidate>()
    private var remoteDescriptionSet = false

    @Volatile private var canSendOutgoingCandidates = false
    private val pendingOutgoingCandidates = mutableListOf<IceCandidate>()

    private var onIceCandidateCallback: ((IceCandidate) -> Unit)? = null
    private var onConnectionStateChangeListener: ((PeerConnection.PeerConnectionState) -> Unit)? = null

    val deviceTier: Int by lazy { detectDeviceTier() }

    init {
        ensureInitialized(context)

        val encoderFactory = DefaultVideoEncoderFactory(eglBase.eglBaseContext, true, true)
        val decoderFactory = DefaultVideoDecoderFactory(eglBase.eglBaseContext)

        peerConnectionFactory = PeerConnectionFactory.builder()
            .setVideoEncoderFactory(encoderFactory)
            .setVideoDecoderFactory(decoderFactory)
            .setOptions(PeerConnectionFactory.Options().apply {
                disableEncryption = false
                disableNetworkMonitor = false
            })
            .createPeerConnectionFactory()

        Log.d(TAG, "PeerConnectionFactory built [Tier: $deviceTier]")
    }

    private fun detectDeviceTier(): Int {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        val totalRamGb = memInfo.totalMem / (1024 * 1024 * 1024.0)
        val processors = Runtime.getRuntime().availableProcessors()
        
        return when {
            processors <= 4 || totalRamGb <= 3.0 -> 1 // Low-end
            processors <= 6 || totalRamGb <= 6.0 -> 2 // Mid-range
            else -> 3 // High-end
        }
    }

    fun createPeerConnection() {
        if (isDisposed) return

        peerConnection?.dispose()
        remoteDescriptionSet = false
        synchronized(pendingCandidates) { pendingCandidates.clear() }

        val iceServers = listOf(
            PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer()
        )

        val rtcConfig = PeerConnection.RTCConfiguration(iceServers).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
            keyType = PeerConnection.KeyType.ECDSA
            tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.ENABLED
            bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE
            rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE
        }

        peerConnection = peerConnectionFactory?.createPeerConnection(rtcConfig, object : PeerConnection.Observer {
            override fun onIceCandidate(candidate: IceCandidate) {
                if (isDisposed) return
                if (canSendOutgoingCandidates) {
                    mainHandler.post { onIceCandidateCallback?.invoke(candidate) }
                } else {
                    synchronized(pendingOutgoingCandidates) { pendingOutgoingCandidates.add(candidate) }
                }
            }

            override fun onConnectionChange(state: PeerConnection.PeerConnectionState) {
                Log.d(TAG, "PeerConnection state change: $state")
                mainHandler.post { onConnectionStateChangeListener?.invoke(state) }
            }

            override fun onIceConnectionChange(state: PeerConnection.IceConnectionState) {
                Log.d(TAG, "ICE connection state: $state")
            }

            override fun onTrack(transceiver: RtpTransceiver) {
                if (isDisposed) return
                val track = transceiver.receiver.track()
                mainHandler.post {
                    when (track) {
                        is VideoTrack -> {
                            Log.i(TAG, "Remote video track received")
                            track.setEnabled(true)
                            onRemoteTrack(track)
                        }
                        is AudioTrack -> {
                            Log.i(TAG, "Remote audio track received")
                            track.setEnabled(true)
                        }
                    }
                }
            }

            override fun onDataChannel(dc: DataChannel) {
                if (dc.label() == "control") {
                    dataChannel = dc
                    dc.registerObserver(object : DataChannel.Observer {
                        override fun onMessage(buffer: DataChannel.Buffer) {
                            val bytes = ByteArray(buffer.data.remaining())
                            buffer.data.get(bytes)
                            Log.d(TAG, "Data message: ${String(bytes)}")
                        }
                        override fun onBufferedAmountChange(p0: Long) {}
                        override fun onStateChange() {}
                    })
                }
            }

            override fun onRenegotiationNeeded() {}
            override fun onSignalingChange(p0: PeerConnection.SignalingState?) {}
            override fun onIceConnectionReceivingChange(p0: Boolean) {}
            override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {}
            override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) {}
            override fun onAddTrack(p0: RtpReceiver?, p1: Array<out MediaStream>?) {}
            override fun onRemoveTrack(p0: RtpReceiver?) {}
            @Deprecated("Deprecated") override fun onAddStream(p0: MediaStream?) {}
            @Deprecated("Deprecated") override fun onRemoveStream(p0: MediaStream?) {}
        })
    }

    fun handleOfferAndAnswer(sdpOffer: String, onAnswerReady: (String) -> Unit) {
        if (isDisposed || peerConnection == null) return

        remoteDescriptionSet = false
        canSendOutgoingCandidates = false
        synchronized(pendingCandidates) { pendingCandidates.clear() }
        synchronized(pendingOutgoingCandidates) { pendingOutgoingCandidates.clear() }

        val remoteDesc = SessionDescription(SessionDescription.Type.OFFER, sdpOffer)

        peerConnection?.setRemoteDescription(object : SdpObserver {
            override fun onSetSuccess() {
                remoteDescriptionSet = true
                Log.d(TAG, "Remote Offer set. Creating Answer for Tier $deviceTier...")

                val audioConstraints = MediaConstraints().apply {
                    mandatory.add(MediaConstraints.KeyValuePair("googEchoCancellation", "true"))
                    mandatory.add(MediaConstraints.KeyValuePair("googAutoGainControl", "true"))
                    mandatory.add(MediaConstraints.KeyValuePair("googNoiseSuppression", "true"))
                    mandatory.add(MediaConstraints.KeyValuePair("googHighpassFilter", "true"))
                }

                peerConnection?.createAnswer(object : SdpObserver {
                    override fun onCreateSuccess(desc: SessionDescription) {
                        val bitrate = when (deviceTier) {
                            3 -> 5000 
                            2 -> 2500 
                            else -> 800 
                        }
                        val optimizedSdp = sdpWithBitrate(desc.description, bitrate)
                        val optimizedDesc = SessionDescription(desc.type, optimizedSdp)

                        peerConnection?.setLocalDescription(object : SdpObserver {
                            override fun onSetSuccess() {
                                canSendOutgoingCandidates = true
                                onAnswerReady(optimizedDesc.description)
                                processPendingCandidates()
                                flushOutgoingCandidates()
                            }
                            override fun onCreateSuccess(p1: SessionDescription?) {}
                            override fun onCreateFailure(p1: String?) {}
                            override fun onSetFailure(p1: String?) {}
                        }, optimizedDesc)
                    }
                    override fun onSetSuccess() {}
                    override fun onCreateFailure(p0: String?) {}
                    override fun onSetFailure(p0: String?) {}
                }, audioConstraints)
            }
            override fun onCreateSuccess(p0: SessionDescription?) {}
            override fun onSetFailure(p0: String?) { Log.e(TAG, "SetRemote fail: $p0") }
            override fun onCreateFailure(p0: String?) {}
        }, remoteDesc)
    }

    /**
     * Injects bitrate constraints and google-specific flags into the SDP.
     * Uses a precise approach to only target the video section and prioritize H264.
     */
    private fun sdpWithBitrate(sdp: String, bitrateKbps: Int): String {
        val lines = sdp.split("\r\n").toMutableList()
        var videoLineIndex = -1
        for (i in lines.indices) {
            if (lines[i].startsWith("m=video")) {
                videoLineIndex = i
                break
            }
        }

        if (videoLineIndex != -1) {
            var h264Payload: String? = null
            for (line in lines) {
                if (line.startsWith("a=rtpmap:") && line.contains("H264/90000")) {
                    h264Payload = line.substringBefore(" ").substringAfter(":")
                    break
                }
            }

            if (h264Payload != null) {
                val parts = lines[videoLineIndex].split(" ").toMutableList()
                if (parts.size > 3) {
                    val currentPos = parts.indexOf(h264Payload)
                    if (currentPos > 3) {
                        parts.removeAt(currentPos)
                        parts.add(3, h264Payload)
                        lines[videoLineIndex] = parts.joinToString(" ")
                    }
                }
            }

            val minBitrate = 300
            val startBitrate = (bitrateKbps * 0.75).toInt()
            val targetPayload = h264Payload ?: "96"

            val resultLines = mutableListOf<String>()
            var inVideoSection = false
            for (line in lines) {
                if (line.startsWith("m=video")) inVideoSection = true
                else if (line.startsWith("m=")) inVideoSection = false
                
                if (inVideoSection && line.startsWith("b=AS:")) continue
                
                resultLines.add(line)
                
                if (line.startsWith("a=mid:video")) {
                    resultLines.add("b=AS:$bitrateKbps")
                }
            }

            for (i in resultLines.indices) {
                if (resultLines[i].startsWith("a=fmtp:$targetPayload")) {
                    if (!resultLines[i].contains("x-google-max-bitrate")) {
                        resultLines[i] = "${resultLines[i]};x-google-min-bitrate=$minBitrate;x-google-max-bitrate=$bitrateKbps;x-google-start-bitrate=$startBitrate"
                    }
                }
            }
            return resultLines.joinToString("\r\n")
        }
        return sdp
    }

    fun getStats(callback: (RTCStatsReport) -> Unit) {
        peerConnection?.getStats { callback(it) }
    }

    fun setOnIceCandidateCallback(callback: (IceCandidate) -> Unit) {
        this.onIceCandidateCallback = callback
    }

    fun setOnConnectionStateChangeListener(listener: (PeerConnection.PeerConnectionState) -> Unit) {
        this.onConnectionStateChangeListener = listener
    }

    fun sendCommand(command: String) {
        val dc = dataChannel ?: return
        if (dc.state() == DataChannel.State.OPEN) {
            dc.send(DataChannel.Buffer(ByteBuffer.wrap(command.toByteArray()), false))
        }
    }

    fun addIceCandidate(candidate: IceCandidate) {
        if (isDisposed || peerConnection == null) return
        if (!remoteDescriptionSet) {
            synchronized(pendingCandidates) { pendingCandidates.add(candidate) }
        } else {
            peerConnection?.addIceCandidate(candidate)
        }
    }

    private fun processPendingCandidates() {
        synchronized(pendingCandidates) {
            pendingCandidates.forEach { peerConnection?.addIceCandidate(it) }
            pendingCandidates.clear()
        }
    }

    private fun flushOutgoingCandidates() {
        synchronized(pendingOutgoingCandidates) {
            pendingOutgoingCandidates.forEach {
                mainHandler.post { onIceCandidateCallback?.invoke(it) }
            }
            pendingOutgoingCandidates.clear()
        }
    }

    fun close() {
        isDisposed = true
        canSendOutgoingCandidates = false
        synchronized(pendingOutgoingCandidates) { pendingOutgoingCandidates.clear() }
        dataChannel?.dispose()
        peerConnection?.dispose()
        peerConnectionFactory?.dispose()
        peerConnection = null
        peerConnectionFactory = null
        dataChannel = null
    }
}
