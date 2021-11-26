package com.myworld.schat.service

interface UserRedisService {
    fun addUser(userWid: String, sessionId: String?, subscribeId: String?)
    fun deleteUser(userWid: String)
    fun checkReceiverSessionIdAndSubscribeIds(userWid: String): MutableSet<String>?
    fun getReceiverSessionId(userWid: String): String?
}
