package com.myworld.schat.data.repository

import com.myworld.schat.data.entity.ChatMessage
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface ChatMessageRepository : MongoRepository<ChatMessage, String>{
    fun findByFromNameAndToNameOrToNameAndFromName(fromName0: String, toName0: String, toName1: String, fromName1: String): List<ChatMessage>?
}
