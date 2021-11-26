package com.myworld.schat.data.repository

import com.myworld.schat.data.entity.ShowPanelPhoto
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface ShowPanelPhotoRepository : MongoRepository<ShowPanelPhoto, String>
