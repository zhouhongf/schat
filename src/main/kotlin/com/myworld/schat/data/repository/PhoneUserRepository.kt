package com.myworld.schat.data.repository

import com.myworld.schat.data.entity.PhoneUser
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface PhoneUserRepository : MongoRepository<PhoneUser, String> {

    fun findByUsername(username: String): PhoneUser?
    fun findByWid(wid: String): PhoneUser?
    fun findByNickname(nickname: String): List<PhoneUser>?
}
