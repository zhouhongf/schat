package com.myworld.schat.data.repository

import com.myworld.schat.data.entity.ChatFile
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface ChatFileRepository : MongoRepository<ChatFile, String>
