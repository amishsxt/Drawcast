package com.amishsxt.drawcast.webrtc

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

class SignalingRepository {

    fun createRoom(roomId: String) {
        // TODO: create room node in Firebase Realtime DB
    }

    fun sendOffer(roomId: String, sdp: String) {
        // TODO: write offer SDP to Firebase
    }

    fun sendAnswer(roomId: String, sdp: String) {
        // TODO: write answer SDP to Firebase
    }

    fun sendIceCandidate(roomId: String, candidate: String) {
        // TODO: push ICE candidate to Firebase
    }

    fun observeOffer(roomId: String): Flow<String> {
        // TODO: listen to offer node in Firebase
        return emptyFlow()
    }

    fun observeAnswer(roomId: String): Flow<String> {
        // TODO: listen to answer node in Firebase
        return emptyFlow()
    }

    fun observeIceCandidates(roomId: String): Flow<String> {
        // TODO: listen to ICE candidates list in Firebase
        return emptyFlow()
    }
}
