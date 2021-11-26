package com.myworld.schat.data.repository

import com.myworld.schat.data.entity.PhoneContact
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface PhoneContactRepository : MongoRepository<PhoneContact, String> {
    fun findByUserWid(userWid: String): List<PhoneContact>?
    fun findByUserWidAndTargetWid(userWid: String, targetWid: String): PhoneContact?
    fun findByUserWidAndTag(userWid: String, tag: String): List<PhoneContact>?
    fun countByUserWidAndTag(userWid: String, tag: String): Int
}
