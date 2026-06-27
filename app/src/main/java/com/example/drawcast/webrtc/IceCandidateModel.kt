package com.amishsxt.drawcast.webrtc

data class IceCandidateModel(
    val candidate: String = "",
    val sdpMid: String = "",
    val sdpMLineIndex: Int = 0
)
