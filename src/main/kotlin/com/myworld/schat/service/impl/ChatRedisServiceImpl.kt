package com.myworld.schat.service.impl

import com.myworld.schat.data.entity.ChatMessage
import com.myworld.schat.data.modal.Constants
import com.myworld.schat.data.modal.MessageAck
import com.myworld.schat.service.ChatRedisService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.redis.core.BoundHashOperations
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service


@Service
class ChatRedisServiceImpl : ChatRedisService {

    @Autowired
    @Qualifier("myRedisTemplate")
    private lateinit var redisTemplate: RedisTemplate<String, Any>

    override fun deleteByMainKey(mainKey: String) {
        redisTemplate.delete(mainKey)
    }


    override fun addMessage(mainKey: String, chatMessage: ChatMessage) {
        val boundHashOperations: BoundHashOperations<String, String, ChatMessage> = redisTemplate.boundHashOps(mainKey)
        val messageId = chatMessage.id
        boundHashOperations.put(messageId, chatMessage)
    }

    override fun updateMessageAck(mainKey: String, messageAck: MessageAck) {
        val messageId = messageAck.id
        // messageId的格式为：群聊groupId-ABC123456789-GROUP-TEXT-1580184911951，单聊userTag-ABC123456789-PAIR-TEXT-1580184911951
        // 如果是PAIR单聊类型的，则将储存message的mainKey从online-userWid改为ack-userWid的形式继续保存
        // 如果是GROUP群聊类型的，则将储存message的mainKey从online-userWid改为ackgroup-userWid的形式继续保存, 但仅保存key为messageId, value为userWid=ackTime
        // 重新保存的数据，等到收件人下线后，再统一保存到数据库中
        // messageAck中的内容为：MessageAck(messageId, sender, receiver, ackTimeStr.toLong())
        val chatMessageType = messageId.split("-")[2]
        if (chatMessageType == Constants.MESSAGE_PAIR_PREFIX) {
            val chatMessage = getMessage(mainKey, messageId)
            chatMessage!!.ackTime = messageAck.ackTime
            val newKey = mainKey.replace("online", "ack")
            addMessage(newKey, chatMessage)
        } else if (chatMessageType == Constants.MESSAGE_GROUP_PREFIX) {
            val newKey = mainKey.replace("online", "ackgroup")
            val userWidAckTime = messageAck.receiver + "=" + messageAck.ackTime
            addMessageGroupAckTime(newKey, messageId, userWidAckTime)
        }
        deleteMessage(mainKey, messageId)
    }

    override fun deleteMessage(mainKey: String, messageId: String) {
        val boundHashOperations: BoundHashOperations<String, String, ChatMessage> = redisTemplate.boundHashOps(mainKey)
        boundHashOperations.delete(messageId)
    }

    override fun getMessage(mainKey: String, messageId: String): ChatMessage? {
        val boundHashOperations: BoundHashOperations<String, String, ChatMessage> = redisTemplate.boundHashOps(mainKey)
        return boundHashOperations[messageId]
    }



    override fun listMessages(mainKey: String): MutableList<ChatMessage>? {
        val boundHashOperations: BoundHashOperations<String, String, ChatMessage> = redisTemplate.boundHashOps(mainKey)
        val values = boundHashOperations.values()
        values?.sort()    // sort如果是null, 就不执行sort; 这一步很关键，这样收到offline消息时，会按照时间先后顺序
        return values
    }

    override fun listMessageKeys(mainKey: String): MutableSet<String>? {
        val boundHashOperations: BoundHashOperations<String, String, ChatMessage> = redisTemplate.boundHashOps(mainKey)
        return boundHashOperations.keys()
    }





    override fun addMessageGroupAckTime(mainKey: String, messageId: String, userWidAckTime: String) {
        val boundHashOperations: BoundHashOperations<String, String, String> = redisTemplate.boundHashOps(mainKey)
        boundHashOperations.put(messageId, userWidAckTime)
    }

    override fun deleteMessageGroupAckTime(mainKey: String, messageId: String) {
        val boundHashOperations: BoundHashOperations<String, String, String> = redisTemplate.boundHashOps(mainKey)
        boundHashOperations.delete(messageId)
    }

    override fun listMessagesGroupAckTime(mainKey: String): MutableMap<String, String>? {
        val boundHashOperations: BoundHashOperations<String, String, String> = redisTemplate.boundHashOps(mainKey)
        return boundHashOperations.entries()
    }


}
