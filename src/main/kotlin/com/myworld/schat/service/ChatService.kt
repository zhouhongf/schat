package com.myworld.schat.service

import com.myworld.schat.common.ApiResult
import com.myworld.schat.data.entity.ChatMessage
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.support.Acknowledgment
import org.springframework.messaging.MessageHeaders
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.web.bind.annotation.RequestParam

interface ChatService {
    fun createHeaders(sessionId: String): MessageHeaders?
    fun makeMessageId(fromName: String, toName: String, time: Long): String?
    fun sendChatpair(chatMessage: ChatMessage, headerAccessor: StompHeaderAccessor)
    fun receiveChatpair(records: List<ConsumerRecord<*, *>>, ack: Acknowledgment)

    fun sendChatgroup(chatMessage: ChatMessage, headerAccessor: StompHeaderAccessor)
    fun receiveChatgroup(chatMessages: List<ChatMessage>, ack: Acknowledgment)
    fun saveChatMessageGroup(chatMessage: ChatMessage, wids: MutableSet<String>)

    fun getChatMessagesPair(pageSize: Int, startNumber: Long, friendWid: String): ApiResult<*>?
    fun getChatMessagesGroup(pageSize: Int, startNumber: Long, groupId: String): ApiResult<*>?
}
