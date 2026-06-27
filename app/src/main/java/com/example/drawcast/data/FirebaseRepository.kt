package com.example.drawcast.data

class FirebaseRepository {

    fun generateRoomId(): String {
        // TODO: return a random 6-digit room code
        return (100000..999999).random().toString()
    }

    fun roomExists(roomId: String, onResult: (Boolean) -> Unit) {
        // TODO: check if room node exists in Firebase
    }

    fun deleteRoom(roomId: String) {
        // TODO: remove room from Firebase when session ends
    }
}
