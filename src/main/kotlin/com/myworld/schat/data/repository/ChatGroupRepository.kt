package com.myworld.schat.data.repository

import com.myworld.schat.data.entity.ChatGroup
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface ChatGroupRepository : MongoRepository<ChatGroup, String> {
    fun countByWidsContains(userWid: String): Int?
    fun findByIdAndCreator(id: String, creator: String): ChatGroup?
}
