package com.amishsxt.drawcast.webrtc

import android.content.Context
import com.amishsxt.drawcast.core.AppLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.webrtc.Camera2Capturer
import org.webrtc.Camera2Enumerator
import org.webrtc.CameraVideoCapturer
import org.webrtc.DataChannel
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RtpReceiver
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import org.webrtc.SurfaceTextureHelper
import org.webrtc.VideoSource
import org.webrtc.VideoTrack
import java.nio.ByteBuffer

class WebRTCManager(private val context: Context) {

    companion object {
        private const val TAG = "WebRTCManager"
    }

    private val _connectionState = MutableStateFlow(ConnectionState.IDLE)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _incomingMessages = MutableStateFlow<String?>(null)
    val incomingMessages: StateFlow<String?> = _incomingMessages.asStateFlow()

    // Callbacks wired up by CallViewModel
    var onIceCandidate: ((IceCandidateModel) -> Unit)? = null
    var onDataChannelMessage: ((String) -> Unit)? = null

    val eglBase: EglBase = EglBase.create()
    private lateinit var peerConnectionFactory: PeerConnectionFactory
    private var peerConnection: PeerConnection? = null
    private var dataChannel: DataChannel? = null

    private val _localVideoTrack = MutableStateFlow<VideoTrack?>(null)
    val localVideoTrack: StateFlow<VideoTrack?> = _localVideoTrack.asStateFlow()

    private val _remoteVideoTrack = MutableStateFlow<VideoTrack?>(null)
    val remoteVideoTrack: StateFlow<VideoTrack?> = _remoteVideoTrack.asStateFlow()

    private var videoSource: VideoSource? = null
    private var surfaceTextureHelper: SurfaceTextureHelper? = null
    private var capturer: CameraVideoCapturer? = null

    private val iceServers = listOf(
        PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer()
    )

    fun initPeerConnection() {
        AppLogger.d(TAG, "initPeerConnection")
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions
                .builder(context.applicationContext)
                .createInitializationOptions()
        )

        peerConnectionFactory = PeerConnectionFactory.builder()
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(eglBase.eglBaseContext))
            .setVideoEncoderFactory(DefaultVideoEncoderFactory(eglBase.eglBaseContext, true, true))
            .createPeerConnectionFactory()

        val rtcConfig = PeerConnection.RTCConfiguration(iceServers).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
        }

        peerConnection = peerConnectionFactory.createPeerConnection(rtcConfig, peerConnectionObserver)

        // Expert creates the data channel; Field User receives it via onDataChannel callback
        val dcInit = DataChannel.Init()
        dataChannel = peerConnection?.createDataChannel("annotations", dcInit)
        dataChannel?.let { observeDataChannel(it) }
    }

    private val peerConnectionObserver = object : PeerConnection.Observer {
        override fun onSignalingChange(state: PeerConnection.SignalingState?) {}

        override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {
            AppLogger.d(TAG, "onIceConnectionChange: $state")
            _connectionState.value = when (state) {
                PeerConnection.IceConnectionState.CONNECTED,
                PeerConnection.IceConnectionState.COMPLETED -> ConnectionState.CONNECTED
                PeerConnection.IceConnectionState.DISCONNECTED -> {
                    _remoteVideoTrack.value = null
                    ConnectionState.DISCONNECTED
                }
                PeerConnection.IceConnectionState.FAILED -> {
                    _remoteVideoTrack.value = null
                    ConnectionState.FAILED
                }
                PeerConnection.IceConnectionState.CHECKING -> ConnectionState.CONNECTING
                else -> _connectionState.value
            }
        }

        override fun onIceConnectionReceivingChange(receiving: Boolean) {}
        override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {}

        override fun onIceCandidate(candidate: IceCandidate?) {
            AppLogger.d(TAG, "onIceCandidate: ${candidate?.sdpMid}")
            candidate?.let {
                onIceCandidate?.invoke(
                    IceCandidateModel(it.sdp, it.sdpMid, it.sdpMLineIndex)
                )
            }
        }

        override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {}
        override fun onAddStream(stream: MediaStream?) {}
        override fun onRemoveStream(stream: MediaStream?) {}

        override fun onDataChannel(channel: DataChannel?) {
            AppLogger.d(TAG, "onDataChannel received: ${channel?.label()}")
            channel?.let {
                dataChannel = it
                observeDataChannel(it)
            }
        }

        override fun onRenegotiationNeeded() {}
        override fun onAddTrack(receiver: RtpReceiver?, streams: Array<out MediaStream>?) {
            AppLogger.d(TAG, "onAddTrack: ${receiver?.track()?.kind()}")
            val track = receiver?.track()
            if (track is VideoTrack) {
                _remoteVideoTrack.value = track
            }
        }
    }

    private fun observeDataChannel(channel: DataChannel) {
        channel.registerObserver(object : DataChannel.Observer {
            override fun onBufferedAmountChange(amount: Long) {}
            override fun onStateChange() {}
            override fun onMessage(buffer: DataChannel.Buffer?) {
                AppLogger.v(TAG, "dataChannel onMessage received")
                buffer?.let {
                    val bytes = ByteArray(it.data.remaining())
                    it.data.get(bytes)
                    val message = String(bytes, Charsets.UTF_8)
                    _incomingMessages.value = message
                    onDataChannelMessage?.invoke(message)
                }
            }
        })
    }

    fun createOffer(onSuccess: (sdp: String) -> Unit) {
        AppLogger.i(TAG, "createOffer")
        _connectionState.value = ConnectionState.CONNECTING
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
        }
        peerConnection?.createOffer(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription?) {
                sdp ?: return
                AppLogger.d(TAG, "offer created")
                peerConnection?.setLocalDescription(simpleSdpObserver { onSuccess(sdp.description) }, sdp)
            }
            override fun onSetSuccess() {}
            override fun onCreateFailure(error: String?) { AppLogger.e(TAG, "createOffer failed: $error") }
            override fun onSetFailure(error: String?) { AppLogger.e(TAG, "setLocalDescription failed: $error") }
        }, constraints)
    }

    fun createAnswer(remoteSdp: String, onSuccess: (sdp: String) -> Unit) {
        AppLogger.i(TAG, "createAnswer")
        val offer = SessionDescription(SessionDescription.Type.OFFER, remoteSdp)
        peerConnection?.setRemoteDescription(simpleSdpObserver {
            val constraints = MediaConstraints()
            peerConnection?.createAnswer(object : SdpObserver {
                override fun onCreateSuccess(sdp: SessionDescription?) {
                    sdp ?: return
                    AppLogger.d(TAG, "answer created")
                    peerConnection?.setLocalDescription(simpleSdpObserver { onSuccess(sdp.description) }, sdp)
                }
                override fun onSetSuccess() {}
                override fun onCreateFailure(error: String?) { AppLogger.e(TAG, "createAnswer failed: $error") }
                override fun onSetFailure(error: String?) { AppLogger.e(TAG, "setLocalDescription failed: $error") }
            }, constraints)
        }, offer)
    }

    fun setRemoteDescription(sdp: String) {
        val answer = SessionDescription(SessionDescription.Type.ANSWER, sdp)
        peerConnection?.setRemoteDescription(simpleSdpObserver {}, answer)
    }

    fun addIceCandidate(model: IceCandidateModel) {
        peerConnection?.addIceCandidate(
            IceCandidate(model.sdpMid, model.sdpMLineIndex, model.candidate)
        )
    }

    fun sendMessage(message: String) {
        AppLogger.d(TAG, "sendMessage: $message")
        val buffer = DataChannel.Buffer(
            ByteBuffer.wrap(message.toByteArray(Charsets.UTF_8)),
            false
        )
        dataChannel?.send(buffer)
    }

    fun startCamera() {
        AppLogger.i(TAG, "startCamera")
        val enumerator = Camera2Enumerator(context)
        val cameraName = enumerator.deviceNames.firstOrNull { enumerator.isFrontFacing(it) }
            ?: enumerator.deviceNames.firstOrNull()
            ?: run { AppLogger.e(TAG, "No camera found"); return }

        capturer = Camera2Capturer(context, cameraName, null)
        surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", eglBase.eglBaseContext)
        videoSource = peerConnectionFactory.createVideoSource(false)
        capturer!!.initialize(surfaceTextureHelper, context, videoSource!!.capturerObserver)
        capturer!!.startCapture(1280, 720, 30)

        val localTrack = peerConnectionFactory.createVideoTrack("local_video_track", videoSource)
        _localVideoTrack.value = localTrack
        peerConnection?.addTrack(localTrack, listOf("local_stream"))
        AppLogger.d(TAG, "camera started, local track added to peer connection")
    }

    fun release() {
        AppLogger.i(TAG, "release")
        capturer?.stopCapture()
        capturer?.dispose()
        surfaceTextureHelper?.dispose()
        videoSource?.dispose()
        _localVideoTrack.value?.dispose()
        _localVideoTrack.value = null
        _remoteVideoTrack.value = null
        dataChannel?.close()
        peerConnection?.close()
        if (::peerConnectionFactory.isInitialized) peerConnectionFactory.dispose()
        eglBase.release()
    }

    // Reduces SdpObserver boilerplate — only onSetSuccess is needed in most cases
    private fun simpleSdpObserver(onSetSuccess: () -> Unit = {}) = object : SdpObserver {
        override fun onCreateSuccess(p0: SessionDescription?) {}
        override fun onSetSuccess() { onSetSuccess() }
        override fun onCreateFailure(p0: String?) {}
        override fun onSetFailure(p0: String?) {}
    }

    enum class ConnectionState { IDLE, CONNECTING, CONNECTED, DISCONNECTED, FAILED }
}
