package com.myworld.schat.data.repository

import com.myworld.schat.data.entity.FriendApply
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface FriendApplyRepository : MongoRepository<FriendApply, String> {
    fun findByUserWidAndTargetWidAndStatus(userWid: String, targetWid: String, status: String): FriendApply?
    fun findByUserWidAndStatus(userWid: String, status: String): List<FriendApply>?
    fun findByTargetWidAndStatus(targetWid: String, status: String): List<FriendApply>?
    fun findByStatusAndUserWidOrStatusAndTargetWid(status0: String, userWid: String, status1: String, targetWid: String, sort: Sort): List<FriendApply>?
}
