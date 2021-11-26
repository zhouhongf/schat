package com.myworld.schat.data.repository

import com.myworld.schat.data.entity.ChatMessageGroup
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface ChatMessageGroupRepository : MongoRepository<ChatMessageGroup, String> {
}
