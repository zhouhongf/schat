package com.myworld.schat.data.repository

import com.myworld.schat.data.entity.ContactTag
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface ContactTagRepository : MongoRepository<ContactTag, String> {
    fun findByUserWid(userWid: String): List<ContactTag>?
    fun findByUserWid(userWid: String, sort: Sort): List<ContactTag>?
    fun findByUserWidAndName(userWid: String, name: String): ContactTag?
    fun findByUserWidAndId(userWid: String, id: String): ContactTag?
    fun deleteByUserWidAndId(userWid: String, id: String)
    fun countByUserWid(userWid: String): Int
}
