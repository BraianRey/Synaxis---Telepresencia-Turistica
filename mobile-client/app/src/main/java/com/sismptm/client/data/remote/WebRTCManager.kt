package com.sismptm.client.data.remote

import android.app.ActivityManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.nio.ByteBuffer
import org.webrtc.*

/**
 * Manager for WebRTC PeerConnection on the Client (viewer) side.
 * - Device-Aware: Detects hardware tier (Low/Mid/High) to cap incoming quality.
 * - SDP Munging: Limits max-bitrate in the Answer if the device is low-end.
 * - VR-Optimized: Prioritizes FrameRate over Resolution (MAINTAIN_FRAMERATE).
 * - Robustness: Strict lifecycle guards and international connectivity.
 */
class WebRTCManager(
        private val context: Context,
        private val eglBase: EglBase,
        private val onRemoteTrack: (VideoTrack) -> Unit
) {
    companion object {
        private const val TAG = "ClientWebRTCManager"
        private var isFactoryInitialized = false

        fun ensureInitialized(context: Context) {
            if (!isFactoryInitialized) {
                val options =
                        PeerConnectionFactory.InitializationOptions.builder(
                                        context.applicationContext
                                )
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
    private var onIceCandidateCallback: ((IceCandidate) -> Unit)? = null

    // Detected device tier: 1 = Low, 2 = Mid, 3 = High
    private val deviceTier: Int by lazy { detectDeviceTier() }

    init {
        ensureInitialized(context)

        // Optimization: Hardware decoding is mandatory for 2026 standards
        val encoderFactory = DefaultVideoEncoderFactory(eglBase.eglBaseContext, false, true)
        val decoderFactory = DefaultVideoDecoderFactory(eglBase.eglBaseContext)

        peerConnectionFactory =
                PeerConnectionFactory.builder()
                        .setVideoEncoderFactory(encoderFactory)
                        .setVideoDecoderFactory(decoderFactory)
                        .setOptions(PeerConnectionFactory.Options().apply {
                            disableEncryption = false
                            disableNetworkMonitor = false
                        })
                        .createPeerConnectionFactory()

        Log.d(TAG, "PeerConnectionFactory built [Device Tier: $deviceTier]")
    }

    private fun detectDeviceTier(): Int {
        val rt = Runtime.getRuntime()
        val cpuCores = rt.availableProcessors()
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        val totalRamGb = memInfo.totalMem / (1024 * 1024 * 1024.0)

        return when {
            cpuCores <= 4 || totalRamGb <= 3.0 -> 1 // Low-end
            cpuCores <= 6 || totalRamGb <= 6.0 -> 2 // Mid-end
            else -> 3 // High-end
        }
    }

    fun createPeerConnection() {
        if (isDisposed) return

        val iceServers =
                listOf(
                        PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
                        PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer(),
                        PeerConnection.IceServer.builder("stun:stun2.l.google.com:19302").createIceServer()
                )
        val rtcConfig =
                PeerConnection.RTCConfiguration(iceServers).apply {
                    sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
                    continualGatheringPolicy =
                            PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
                    keyType = PeerConnection.KeyType.ECDSA
                    iceBackupCandidatePairPingInterval = 2000
                    iceConnectionReceivingTimeout = 5000
                }

        peerConnection =
                peerConnectionFactory?.createPeerConnection(
                        rtcConfig,
                        object : PeerConnection.Observer {
                            override fun onIceCandidate(candidate: IceCandidate) {
                                if (!isDisposed) {
                                    mainHandler.post { addIceCandidate(candidate) }
                                }
                            }

                            override fun onConnectionChange(state: PeerConnection.PeerConnectionState) {
                                Log.d(TAG, "Connection state → $state")
                            }

                            override fun onIceConnectionChange(state: PeerConnection.IceConnectionState) {
                                Log.d(TAG, "ICE connection → $state")
                            }

                            override fun onTrack(transceiver: RtpTransceiver) {
                                if (isDisposed) return
                                mainHandler.post {
                                    val track = transceiver.receiver.track()
                                    if (track is VideoTrack) {
                                        Log.d(TAG, "Remote video track received")
                                        track.setEnabled(true)
                                        onRemoteTrack(track)
                                        applyReceiverPreferences()
                                    }
                                }
                            }

                            override fun onDataChannel(dc: DataChannel) {
                                if (isDisposed) return
                                Log.d(TAG, "DataChannel 'control' received")
                                if (dc.label() == "control") {
                                    dataChannel = dc
                                    dc.registerObserver(
                                            object : DataChannel.Observer {
                                                override fun onBufferedAmountChange(p0: Long) {}
                                                override fun onStateChange() {
                                                    Log.d(TAG, "DataChannel state → ${dc.state()}")
                                                }
                                                override fun onMessage(buffer: DataChannel.Buffer) {}
                                            }
                                    )
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
                        }
                )

        Log.d(TAG, "PeerConnection created (Unified Plan)")
    }

    /**
     * VR-Ready: Prioritizes FPS to avoid motion sickness.
     */
    private fun applyReceiverPreferences() {
        mainHandler.post {
            val pc = peerConnection ?: return@post
            val transceivers = pc.transceivers ?: return@post
            for (transceiver in transceivers) {
                if (transceiver.receiver.track()?.kind() == "video") {
                    val parameters = transceiver.receiver.parameters
                    parameters.degradationPreference = RtpParameters.DegradationPreference.MAINTAIN_FRAMERATE
                    transceiver.receiver.parameters = parameters
                }
            }
        }
    }

    fun setOnIceCandidateCallback(callback: (IceCandidate) -> Unit) {
        this.onIceCandidateCallback = callback
    }

    fun sendCommand(command: String) {
        val dc = dataChannel ?: return
        if (dc.state() != DataChannel.State.OPEN || isDisposed) return
        val buffer = DataChannel.Buffer(ByteBuffer.wrap(command.toByteArray(Charsets.UTF_8)), false)
        dc.send(buffer)
        Log.d(TAG, "Command sent: $command")
    }

    fun handleOffer(sdp: String, observer: SdpObserver) {
        if (isDisposed || peerConnection == null) return
        val desc = SessionDescription(SessionDescription.Type.OFFER, sdp)
        peerConnection?.setRemoteDescription(
                object : SdpObserver {
                    override fun onSetSuccess() {
                        remoteDescriptionSet = true
                        observer.onSetSuccess()
                        processPendingCandidates()
                    }
                    override fun onSetFailure(error: String?) {
                        observer.onSetFailure(error)
                    }
                    override fun onCreateSuccess(p0: SessionDescription?) {}
                    override fun onCreateFailure(p0: String?) {}
                },
                desc
        )
    }

    private fun processPendingCandidates() {
        synchronized(pendingCandidates) {
            for (candidate in pendingCandidates) {
                peerConnection?.addIceCandidate(candidate)
                onIceCandidateCallback?.invoke(candidate)
            }
            pendingCandidates.clear()
        }
    }

    fun createAnswer(observer: SdpObserver) {
        if (isDisposed || peerConnection == null) return
        peerConnection?.createAnswer(
                object : SdpObserver {
                    override fun onCreateSuccess(desc: SessionDescription) {
                        // Dynamically cap bitrate based on device tier
                        val adaptiveSdp = capBitrateInSdp(desc.description)
                        val adaptiveDesc = SessionDescription(desc.type, adaptiveSdp)
                        observer.onCreateSuccess(adaptiveDesc)
                    }
                    override fun onCreateFailure(p0: String?) { observer.onCreateFailure(p0) }
                    override fun onSetSuccess() {}
                    override fun onSetFailure(p0: String?) {}
                },
                MediaConstraints()
        )
    }

    /**
     * Injects bitrate limits into the SDP Answer.
     * Tells the Partner (sender) to reduce quality if this device is low-end.
     */
    private fun capBitrateInSdp(sdp: String): String {
        val maxBitrate = when (deviceTier) {
            1 -> 1500 // 1.5 Mbps for low-end (Safe for 720p/30)
            2 -> 3000 // 3.0 Mbps for mid-end (Ideal for 720p/60 or 1080p/30)
            else -> 5000 // 5.0 Mbps for high-end (Full 1080p/60)
        }
        
        return if (!sdp.contains("x-google-max-bitrate")) {
            sdp.replace("a=fmtp:111", "a=fmtp:111;x-google-max-bitrate=$maxBitrate")
        } else {
            sdp
        }
    }

    fun setLocalDescription(sdp: SessionDescription, observer: SdpObserver) {
        if (isDisposed || peerConnection == null) return
        peerConnection?.setLocalDescription(observer, sdp)
    }

    fun addIceCandidate(candidate: IceCandidate) {
        if (isDisposed || peerConnection == null) return
        if (!remoteDescriptionSet) {
            synchronized(pendingCandidates) { pendingCandidates.add(candidate) }
            return
        }
        peerConnection?.addIceCandidate(candidate)
        onIceCandidateCallback?.invoke(candidate)
    }

    fun close() {
        if (isDisposed) return
        isDisposed = true
        Log.d(TAG, "Closing WebRTCManager [Final Adaptive Edition]...")
        dataChannel?.close()
        dataChannel?.dispose()
        dataChannel = null
        peerConnection?.dispose()
        peerConnection = null
        peerConnectionFactory?.dispose()
        peerConnectionFactory = null
        onIceCandidateCallback = null
    }
}
