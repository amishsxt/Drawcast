package com.example.drawcast.webrtc

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class WebRTCManager(private val context: Context) {

    private val _connectionState = MutableStateFlow(ConnectionState.IDLE)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _incomingMessages = MutableStateFlow<String?>(null)
    val incomingMessages: StateFlow<String?> = _incomingMessages.asStateFlow()

    val iceServers = listOf("stun:stun.l.google.com:19302")

    fun initPeerConnection() {
        // TODO: initialize PeerConnectionFactory and PeerConnection
    }

    fun createOffer(onSuccess: (sdp: String) -> Unit) {
        // TODO: create SDP offer and invoke callback
    }

    fun createAnswer(remoteSdp: String, onSuccess: (sdp: String) -> Unit) {
        // TODO: set remote description, create SDP answer
    }

    fun setRemoteDescription(sdp: String) {
        // TODO: set remote SDP
    }

    fun addIceCandidate(candidate: String) {
        // TODO: parse and add ICE candidate
    }

    fun sendMessage(message: String) {
        // TODO: send JSON over data channel
    }

    fun startCamera() {
        // TODO: capture camera and attach to local video track
    }

    fun release() {
        // TODO: close peer connection and release resources
    }

    enum class ConnectionState { IDLE, CONNECTING, CONNECTED, DISCONNECTED, FAILED }
}
