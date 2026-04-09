package com.sismptm.partner.webrtc

import android.content.Context
import android.util.Log
import org.webrtc.*
import java.util.*

/**
 * Manager for WebRTC operations on the Partner side.
 * Handles peer connection, media capture, and DataChannel for commands.
 * Configured with standard STUN servers to support international P2P connections.
 */
class WebRTCManager(
    private val context: Context,
    private val listener: WebRTCListener
) {
    private val TAG = "WebRTCManager"
    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null
    private var localVideoTrack: VideoTrack? = null
    private var localAudioTrack: AudioTrack? = null
    private var videoCapturer: VideoCapturer? = null
    private var dataChannel: DataChannel? = null
    private val eglBase = EglBase.create()

    interface WebRTCListener {
        fun onIceCandidate(candidate: IceCandidate)
        fun onLocalSdpCreated(sdp: SessionDescription)
        fun onCommandReceived(command: String)
        fun onConnectionStateChange(state: PeerConnection.PeerConnectionState)
    }

    init {
        initPeerConnectionFactory()
    }

    private fun initPeerConnectionFactory() {
        val options = PeerConnectionFactory.InitializationOptions.builder(context)
            .setEnableInternalTracer(true)
            .createInitializationOptions()
        PeerConnectionFactory.initialize(options)

        val factoryOptions = PeerConnectionFactory.Options()
        peerConnectionFactory = PeerConnectionFactory.builder()
            .setOptions(factoryOptions)
            .setVideoEncoderFactory(DefaultVideoEncoderFactory(eglBase.eglBaseContext, true, true))
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(eglBase.eglBaseContext))
            .createPeerConnectionFactory()
    }

    fun startLocalCapture(surfaceViewRenderer: SurfaceViewRenderer) {
        // Essential for displaying local video
        surfaceViewRenderer.init(eglBase.eglBaseContext, null)
        surfaceViewRenderer.setEnableHardwareScaler(true)
        surfaceViewRenderer.setMirror(true)

        // Standard STUN servers to allow P2P discovery through NAT/Firewalls
        val iceServers = listOf(
            PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer()
        )

        val rtcConfig = PeerConnection.RTCConfiguration(iceServers)
        rtcConfig.sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN

        peerConnection = peerConnectionFactory?.createPeerConnection(rtcConfig, object : PeerConnection.Observer {
            override fun onIceCandidate(candidate: IceCandidate) {
                Log.d(TAG, "New ICE Candidate: ${candidate.sdp}")
                listener.onIceCandidate(candidate)
            }

            override fun onConnectionChange(newState: PeerConnection.PeerConnectionState) {
                Log.d(TAG, "Connection State Change: $newState")
                listener.onConnectionStateChange(newState)
            }

            override fun onIceConnectionChange(newState: PeerConnection.IceConnectionState) {
                Log.d(TAG, "ICE Connection Change: $newState")
            }

            override fun onDataChannel(dc: DataChannel) {
                Log.d(TAG, "DataChannel received from remote")
                setupDataChannel(dc)
            }

            override fun onSignalingChange(newState: PeerConnection.SignalingState) {}
            override fun onIceConnectionReceivingChange(receiving: Boolean) {}
            override fun onIceGatheringChange(newState: PeerConnection.IceGatheringState) {}
            override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>) {}
            override fun onAddStream(stream: MediaStream) {}
            override fun onRemoveStream(stream: MediaStream) {}
            override fun onAddTrack(receiver: RtpReceiver, mediaStreams: Array<out MediaStream>) {}
            override fun onRenegotiationNeeded() {}
        })

        // Setup Video Capture
        val videoSource = peerConnectionFactory?.createVideoSource(false)
        videoCapturer = createVideoCapturer()
        
        val surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", eglBase.eglBaseContext)
        videoCapturer?.initialize(surfaceTextureHelper, context, videoSource?.capturerObserver)
        videoCapturer?.startCapture(1280, 720, 30)

        localVideoTrack = peerConnectionFactory?.createVideoTrack("100", videoSource)
        localVideoTrack?.addSink(surfaceViewRenderer)

        // Setup Audio Capture
        val audioSource = peerConnectionFactory?.createAudioSource(MediaConstraints())
        localAudioTrack = peerConnectionFactory?.createAudioTrack("101", audioSource)

        peerConnection?.addTrack(localVideoTrack, listOf("stream1"))
        peerConnection?.addTrack(localAudioTrack, listOf("stream1"))

        // Setup Data Channel for P2P Commands
        val dcInit = DataChannel.Init()
        dcInit.ordered = true
        // Changed from "commands" to "control" for client interoperability
        dataChannel = peerConnection?.createDataChannel("control", dcInit)
        dataChannel?.let { setupDataChannel(it) }
    }

    private fun setupDataChannel(dc: DataChannel) {
        dc.registerObserver(object : DataChannel.Observer {
            override fun onBufferedAmountChange(previousAmount: Long) {}
            override fun onStateChange() {
                Log.d(TAG, "DataChannel State: ${dc.state()}")
            }

            override fun onMessage(buffer: DataChannel.Buffer) {
                val data = buffer.data
                val bytes = ByteArray(data.remaining())
                data.get(bytes)
                val message = String(bytes)
                Log.d(TAG, "Command Received via DataChannel: $message")
                listener.onCommandReceived(message)
            }
        })
    }

    fun createOffer() {
        val constraints = MediaConstraints()
        peerConnection?.createOffer(object : SdpObserver {
            override fun onCreateSuccess(desc: SessionDescription) {
                peerConnection?.setLocalDescription(object : SdpObserver {
                    override fun onCreateSuccess(p0: SessionDescription?) {}
                    override fun onSetSuccess() {
                        Log.d(TAG, "Local Description (Offer) set successfully")
                        listener.onLocalSdpCreated(desc)
                    }
                    override fun onCreateFailure(error: String?) {
                        Log.e(TAG, "Failed to set local description: $error")
                    }
                    override fun onSetFailure(error: String?) {
                        Log.e(TAG, "Failed to set local description: $error")
                    }
                }, desc)
            }
            override fun onSetSuccess() {}
            override fun onCreateFailure(error: String?) {
                Log.e(TAG, "Failed to create offer: $error")
            }
            override fun onSetFailure(error: String?) {}
        }, constraints)
    }

    fun setRemoteDescription(sdp: String, isOffer: Boolean) {
        val type = if (isOffer) SessionDescription.Type.OFFER else SessionDescription.Type.ANSWER
        val desc = SessionDescription(type, sdp)
        peerConnection?.setRemoteDescription(object : SdpObserver {
            override fun onCreateSuccess(p0: SessionDescription?) {}
            override fun onSetSuccess() {
                Log.d(TAG, "Remote description set successfully")
            }
            override fun onCreateFailure(error: String?) {
                Log.e(TAG, "Failed to set remote description: $error")
            }
            override fun onSetFailure(error: String?) {
                Log.e(TAG, "Failed to set remote description: $error")
            }
        }, desc)
    }

    fun addIceCandidate(candidate: IceCandidate) {
        peerConnection?.addIceCandidate(candidate)
    }

    private fun createVideoCapturer(): VideoCapturer? {
        val enumerator = Camera2Enumerator(context)
        val deviceNames = enumerator.deviceNames
        for (deviceName in deviceNames) {
            if (enumerator.isBackFacing(deviceName)) {
                return enumerator.createCapturer(deviceName, null)
            }
        }
        for (deviceName in deviceNames) {
            return enumerator.createCapturer(deviceName, null)
        }
        return null
    }

    fun dispose() {
        videoCapturer?.stopCapture()
        videoCapturer?.dispose()
        dataChannel?.dispose()
        peerConnection?.dispose()
        peerConnectionFactory?.dispose()
        eglBase.release()
    }
}
