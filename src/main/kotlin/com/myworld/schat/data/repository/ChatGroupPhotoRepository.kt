package com.myworld.schat.data.repository

import com.myworld.schat.data.entity.ChatGroupPhoto
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface ChatGroupPhotoRepository : MongoRepository<ChatGroupPhoto, String>
