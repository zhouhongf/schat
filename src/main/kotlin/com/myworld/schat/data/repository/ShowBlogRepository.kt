package com.myworld.schat.data.repository

import com.myworld.schat.data.entity.ShowBlog
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface ShowBlogRepository : MongoRepository<ShowBlog, String> {
    fun findByUserWid(userWid: String, pageable: Pageable): Page<ShowBlog>?
    fun findByOpenToWidsContaining(userWid: String, pageable: Pageable): Page<ShowBlog>?
}
