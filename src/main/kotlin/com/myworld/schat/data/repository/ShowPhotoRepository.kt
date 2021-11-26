package com.myworld.schat.data.repository

import com.myworld.schat.data.entity.ShowPhoto
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface ShowPhotoRepository : MongoRepository<ShowPhoto, String> {
    fun countByBlogIdDetail(blogIdDetail: String): Int
    fun deleteAllByBlogIdDetail(blogIdDetail: String)
}
