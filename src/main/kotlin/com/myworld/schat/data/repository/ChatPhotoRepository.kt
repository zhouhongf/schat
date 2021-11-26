package com.myworld.schat.data.repository

import com.myworld.schat.data.entity.ChatPhoto
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface ChatPhotoRepository : MongoRepository<ChatPhoto, String>
