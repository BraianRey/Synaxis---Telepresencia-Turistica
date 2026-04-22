package com.sismptm.client.data.remote

import android.app.ActivityManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.nio.ByteBuffer
import org.webrtc.*

/** Manager for WebRTC PeerConnection on the Client (viewer) side. */
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

    // Outgoing ICE candidates buffering
    @Volatile private var canSendOutgoingCandidates = false
    private val pendingOutgoingCandidates = mutableListOf<IceCandidate>()

    private var onIceCandidateCallback: ((IceCandidate) -> Unit)? = null
    private var onConnectionStateChangeListener: ((PeerConnection.PeerConnectionState) -> Unit)? =
        null

    private val deviceTier: Int by lazy { detectDeviceTier() }

    init {
        ensureInitialized(context)

        val encoderFactory = DefaultVideoEncoderFactory(eglBase.eglBaseContext, false, true)
        val decoderFactory = DefaultVideoDecoderFactory(eglBase.eglBaseContext)

        peerConnectionFactory =
            PeerConnectionFactory.builder()
                .setVideoEncoderFactory(encoderFactory)
                .setVideoDecoderFactory(decoderFactory)
                .setOptions(
                    PeerConnectionFactory.Options().apply {
                        disableEncryption = false
                        disableNetworkMonitor = false
                    }
                )
                .createPeerConnectionFactory()

        Log.d(TAG, "PeerConnectionFactory built [Tier: $deviceTier]")
    }

    private fun detectDeviceTier(): Int {
        val rt = Runtime.getRuntime()
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        val totalRamGb = memInfo.totalMem / (1024 * 1024 * 1024.0)
        return when {
            rt.availableProcessors() <= 4 || totalRamGb <= 3.0 -> 1
            rt.availableProcessors() <= 6 || totalRamGb <= 6.0 -> 2
            else -> 3
        }
    }

    fun createPeerConnection() {
        if (isDisposed) return

        // Clean up previous connection if it exists
        peerConnection?.dispose()
        remoteDescriptionSet = false
        synchronized(pendingCandidates) { pendingCandidates.clear() }

        val iceServers =
            listOf(
                PeerConnection.IceServer.builder("stun:stun.l.google.com:19302")
                    .createIceServer(),
                PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302")
                    .createIceServer()
            )

        val rtcConfig =
            PeerConnection.RTCConfiguration(iceServers).apply {
                sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
                continualGatheringPolicy =
                    PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
                keyType = PeerConnection.KeyType.ECDSA
            }

        peerConnection =
            peerConnectionFactory?.createPeerConnection(
                rtcConfig,
                object : PeerConnection.Observer {
                    override fun onIceCandidate(candidate: IceCandidate) {
                        if (isDisposed) return

                        if (canSendOutgoingCandidates) {
                            mainHandler.post { onIceCandidateCallback?.invoke(candidate) }
                        } else {
                            synchronized(pendingOutgoingCandidates) {
                                pendingOutgoingCandidates.add(candidate)
                            }
                        }
                    }

                    override fun onConnectionChange(
                        state: PeerConnection.PeerConnectionState
                    ) {
                        Log.d(TAG, "PeerConnection state change: $state")
                        mainHandler.post { onConnectionStateChangeListener?.invoke(state) }
                    }

                    override fun onIceConnectionChange(
                        state: PeerConnection.IceConnectionState
                    ) {
                        Log.d(TAG, "ICE connection state: $state")
                    }

                    override fun onTrack(transceiver: RtpTransceiver) {
                        if (isDisposed) return
                        val track = transceiver.receiver.track()
                        if (track is VideoTrack) {
                            mainHandler.post {
                                Log.i(TAG, ">>> REMOTE VIDEO TRACK RECEIVED <<<")
                                track.setEnabled(true)
                                onRemoteTrack(track)
                            }
                        }
                    }

                    override fun onDataChannel(dc: DataChannel) {
                        if (dc.label() == "control") dataChannel = dc
                    }

                    override fun onRenegotiationNeeded() {}
                    override fun onSignalingChange(p0: PeerConnection.SignalingState?) {}
                    override fun onIceConnectionReceivingChange(p0: Boolean) {}
                    override fun onIceGatheringChange(
                        p0: PeerConnection.IceGatheringState?
                    ) {}
                    override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) {}
                    override fun onAddTrack(
                        p0: RtpReceiver?,
                        p1: Array<out MediaStream>?
                    ) {}
                    override fun onRemoveTrack(p0: RtpReceiver?) {}
                    @Deprecated("Deprecated") override fun onAddStream(p0: MediaStream?) {}
                    @Deprecated("Deprecated")
                    override fun onRemoveStream(p0: MediaStream?) {}
                }
            )
    }

    fun handleOfferAndAnswer(sdpOffer: String, onAnswerReady: (String) -> Unit) {
        if (isDisposed || peerConnection == null) return

        // Ensure we are ready for a new session
        remoteDescriptionSet = false
        canSendOutgoingCandidates = false
        synchronized(pendingCandidates) { pendingCandidates.clear() }
        synchronized(pendingOutgoingCandidates) { pendingOutgoingCandidates.clear() }

        val remoteDesc = SessionDescription(SessionDescription.Type.OFFER, sdpOffer)
        peerConnection?.setRemoteDescription(
            object : SdpObserver {
                override fun onSetSuccess() {
                    remoteDescriptionSet = true
                    Log.d(TAG, "Remote Offer set. Creating Answer...")

                    peerConnection?.setLocalDescription(
                        object : SdpObserver {
                            override fun onSetSuccess() {
                                val localDesc = peerConnection?.localDescription
                                if (localDesc != null) {
                                    Log.d(TAG, "Local Answer set. Sending to Partner.")
                                    canSendOutgoingCandidates = true
                                    onAnswerReady(localDesc.description)
                                }
                                processPendingCandidates()
                                flushOutgoingCandidates()
                            }
                            override fun onCreateSuccess(p0: SessionDescription?) {}
                            override fun onCreateFailure(p0: String?) {
                                Log.e(TAG, "Auto-Answer fail: $p0")
                            }
                            override fun onSetFailure(p0: String?) {
                                Log.e(TAG, "SetLocal fail: $p0")
                            }
                        }
                    )
                }
                override fun onCreateSuccess(p0: SessionDescription?) {}
                override fun onSetFailure(p0: String?) {
                    Log.e(TAG, "SetRemote fail: $p0")
                }
                override fun onCreateFailure(p0: String?) {}
            },
            remoteDesc
        )
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
        peerConnection?.dispose()
        peerConnectionFactory?.dispose()
    }
}