package com.amishsxt.drawcast.ui.call

import android.content.Context
import androidx.lifecycle.ViewModel
import com.amishsxt.drawcast.core.AppLogger
import androidx.lifecycle.viewModelScope
import com.amishsxt.drawcast.annotation.models.Annotation
import com.amishsxt.drawcast.webrtc.IceCandidateModel
import com.amishsxt.drawcast.webrtc.SignalingRepository
import com.amishsxt.drawcast.webrtc.WebRTCManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CallViewModel : ViewModel() {

    companion object {
        private const val TAG = "CallViewModel"
    }

    private lateinit var webRTCManager: WebRTCManager
    private val signalingRepository = SignalingRepository()

    private val _annotations = MutableStateFlow<List<Annotation>>(emptyList())
    val annotations: StateFlow<List<Annotation>> = _annotations.asStateFlow()

    private val _uiState = MutableStateFlow<CallUiState>(CallUiState.Idle)
    val uiState: StateFlow<CallUiState> = _uiState.asStateFlow()

    val connectionState get() = webRTCManager.connectionState

    fun init(context: Context, roomId: String, isExpert: Boolean) {
        AppLogger.i(TAG, "init roomId=$roomId isExpert=$isExpert")
        webRTCManager = WebRTCManager(context)
        webRTCManager.initPeerConnection()

        // Forward ICE candidates gathered locally to Firebase
        webRTCManager.onIceCandidate = { candidate ->
            signalingRepository.sendIceCandidate(roomId, isOffer = isExpert, candidate)
        }

        // Forward incoming data channel messages to annotation state
        webRTCManager.onDataChannelMessage = { json ->
            // TODO: Week 3 — deserialize JSON → Annotation and add to list
        }

        if (isExpert) startAsExpert(roomId) else startAsFieldUser(roomId)
    }

    // ── Expert (Phone A) ──────────────────────────────────────────────────
    // Creates room → sends offer → waits for answer → streams remote ICE
    private fun startAsExpert(roomId: String) {
        AppLogger.i(TAG, "startAsExpert roomId=$roomId")
        signalingRepository.createRoom(roomId)

        webRTCManager.createOffer { sdp ->
            signalingRepository.sendOffer(roomId, sdp)
        }

        viewModelScope.launch {
            signalingRepository.observeAnswer(roomId).collect { sdp ->
                webRTCManager.setRemoteDescription(sdp)
                _uiState.value = CallUiState.Connected(roomId)
            }
        }

        observeRemoteIceCandidates(roomId, fromOffer = false)
    }

    // ── Field User (Phone B) ──────────────────────────────────────────────
    // Waits for offer → sends answer → starts camera → streams remote ICE
    private fun startAsFieldUser(roomId: String) {
        AppLogger.i(TAG, "startAsFieldUser roomId=$roomId")
        viewModelScope.launch {
            signalingRepository.observeOffer(roomId).collect { sdp ->
                webRTCManager.createAnswer(sdp) { answer ->
                    signalingRepository.sendAnswer(roomId, answer)
                    _uiState.value = CallUiState.Connected(roomId)
                }
            }
        }

        webRTCManager.startCamera()
        observeRemoteIceCandidates(roomId, fromOffer = true)
    }

    // ── Shared ────────────────────────────────────────────────────────────
    private fun observeRemoteIceCandidates(roomId: String, fromOffer: Boolean) {
        viewModelScope.launch {
            signalingRepository.observeIceCandidates(roomId, fromOffer).collect { model ->
                webRTCManager.addIceCandidate(model)
            }
        }
    }

    fun sendAnnotation(annotation: Annotation) {
        _annotations.value = _annotations.value + annotation
        // TODO: Week 3 — serialize to JSON and send via webRTCManager.sendMessage()
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
        if (::webRTCManager.isInitialized) webRTCManager.release()
    }
}

sealed class CallUiState {
    object Idle : CallUiState()
    data class Connected(val roomId: String) : CallUiState()
    data class Error(val message: String) : CallUiState()
}
