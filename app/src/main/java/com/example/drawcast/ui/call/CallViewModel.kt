package com.amishsxt.drawcast.ui.call

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amishsxt.drawcast.annotation.models.Annotation
import com.amishsxt.drawcast.webrtc.SignalingRepository
import com.amishsxt.drawcast.webrtc.WebRTCManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CallViewModel : ViewModel() {

    private lateinit var webRTCManager: WebRTCManager
    private val signalingRepository = SignalingRepository()

    private val _annotations = MutableStateFlow<List<Annotation>>(emptyList())
    val annotations: StateFlow<List<Annotation>> = _annotations.asStateFlow()

    val connectionState get() = webRTCManager.connectionState

    fun init(context: Context, roomId: String, isExpert: Boolean) {
        webRTCManager = WebRTCManager(context)
        webRTCManager.initPeerConnection()

        if (isExpert) {
            startAsExpert(roomId)
        } else {
            startAsFieldUser(roomId)
        }
    }

    private fun startAsExpert(roomId: String) {
        webRTCManager.createOffer { sdp ->
            signalingRepository.sendOffer(roomId, sdp)
        }
        viewModelScope.launch {
            signalingRepository.observeAnswer(roomId).collect { sdp ->
                webRTCManager.setRemoteDescription(sdp)
            }
        }
        observeIceCandidates(roomId)
    }

    private fun startAsFieldUser(roomId: String) {
        viewModelScope.launch {
            signalingRepository.observeOffer(roomId).collect { sdp ->
                webRTCManager.createAnswer(sdp) { answer ->
                    signalingRepository.sendAnswer(roomId, answer)
                }
            }
        }
        observeIceCandidates(roomId)
        webRTCManager.startCamera()
    }

    private fun observeIceCandidates(roomId: String) {
        viewModelScope.launch {
            signalingRepository.observeIceCandidates(roomId).collect { candidate ->
                webRTCManager.addIceCandidate(candidate)
            }
        }
    }

    fun sendAnnotation(annotation: Annotation) {
        // TODO: serialize annotation to JSON and send via data channel
        _annotations.value = _annotations.value + annotation
    }

    fun undoLast() {
        if (_annotations.value.isNotEmpty()) {
            _annotations.value = _annotations.value.dropLast(1)
        }
    }

    fun clearAll() {
        _annotations.value = emptyList()
        webRTCManager.sendMessage("{\"type\":\"clear\"}")
    }

    override fun onCleared() {
        super.onCleared()
        webRTCManager.release()
    }
}
