package com.amishsxt.drawcast.ui.home

import androidx.lifecycle.ViewModel
import com.amishsxt.drawcast.core.AppLogger
import com.amishsxt.drawcast.data.FirebaseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class HomeViewModel : ViewModel() {

    companion object {
        private const val TAG = "HomeViewModel"
    }

    private val firebaseRepository = FirebaseRepository()

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Idle)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun createRoom() {
        val roomId = firebaseRepository.generateRoomId()
        AppLogger.i(TAG, "createRoom roomId=$roomId")
        _uiState.value = HomeUiState.RoomCreated(roomId)
        // TODO: write room to Firebase, then navigate to call screen as Expert
    }

    fun joinRoom(roomId: String) {
        AppLogger.i(TAG, "joinRoom roomId=$roomId")
        firebaseRepository.roomExists(roomId) { exists ->
            if (exists) {
                _uiState.value = HomeUiState.RoomJoined(roomId)
                // TODO: navigate to call screen as Field User
            } else {
                _uiState.value = HomeUiState.Error("Room not found")
            }
        }
    }
}

sealed class HomeUiState {
    object Idle : HomeUiState()
    data class RoomCreated(val roomId: String) : HomeUiState()
    data class RoomJoined(val roomId: String) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}
