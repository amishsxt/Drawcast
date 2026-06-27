package com.amishsxt.drawcast.webrtc

import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class SignalingRepository {

    private val db = FirebaseDatabase.getInstance().reference.child("rooms")

    fun createRoom(roomId: String) {
        db.child(roomId).child("created").setValue(true)
    }

    fun sendOffer(roomId: String, sdp: String) {
        db.child(roomId).child("offer").setValue(mapOf("sdp" to sdp))
    }

    fun sendAnswer(roomId: String, sdp: String) {
        db.child(roomId).child("answer").setValue(mapOf("sdp" to sdp))
    }

    fun sendIceCandidate(roomId: String, isOffer: Boolean, model: IceCandidateModel) {
        val key = if (isOffer) "offerCandidates" else "answerCandidates"
        db.child(roomId).child(key).push().setValue(model)
    }

    // Emits the offer SDP once it appears in Firebase
    fun observeOffer(roomId: String): Flow<String> = callbackFlow {
        val ref = db.child(roomId).child("offer")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val sdp = snapshot.child("sdp").getValue(String::class.java)
                if (sdp != null) trySend(sdp)
            }
            override fun onCancelled(error: DatabaseError) { close(error.toException()) }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    // Emits the answer SDP once it appears in Firebase
    fun observeAnswer(roomId: String): Flow<String> = callbackFlow {
        val ref = db.child(roomId).child("answer")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val sdp = snapshot.child("sdp").getValue(String::class.java)
                if (sdp != null) trySend(sdp)
            }
            override fun onCancelled(error: DatabaseError) { close(error.toException()) }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    // Emits each ICE candidate as it's added (existing + new via ChildEventListener)
    fun observeIceCandidates(roomId: String, fromOffer: Boolean): Flow<IceCandidateModel> = callbackFlow {
        val key = if (fromOffer) "offerCandidates" else "answerCandidates"
        val ref = db.child(roomId).child(key)
        val listener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val model = snapshot.getValue(IceCandidateModel::class.java)
                if (model != null) trySend(model)
            }
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) { close(error.toException()) }
        }
        ref.addChildEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    fun deleteRoom(roomId: String) {
        db.child(roomId).removeValue()
    }
}
