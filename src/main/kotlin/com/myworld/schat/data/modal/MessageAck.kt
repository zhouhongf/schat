package com.myworld.schat.data.modal

data class MessageAck(
    var id: String = "",
    var sender: String = "",
    var receiver: String = "",
    var ackTime: Long = 0L
)
