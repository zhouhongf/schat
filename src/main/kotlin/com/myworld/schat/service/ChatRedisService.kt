package com.myworld.schat.service

import com.myworld.schat.data.entity.ChatMessage
import com.myworld.schat.data.modal.MessageAck

interface ChatRedisService {
    fun deleteByMainKey(mainKey: String)

    fun addMessage(mainKey: String, chatMessage: ChatMessage)
    fun deleteMessage(mainKey: String, messageId: String)
    fun updateMessageAck(mainKey: String, messageAck: MessageAck)
    fun getMessage(mainKey: String, messageId: String): ChatMessage?

    fun listMessages(mainKey: String): List<ChatMessage>?
    fun listMessageKeys(mainKey: String): Set<String>?

    fun addMessageGroupAckTime(mainKey: String, messageId: String, userWidAckTime: String)
    fun deleteMessageGroupAckTime(mainKey: String, messageId: String)
    fun listMessagesGroupAckTime(mainKey: String): MutableMap<String, String>?
}
