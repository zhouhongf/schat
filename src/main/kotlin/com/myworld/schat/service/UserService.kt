package com.myworld.schat.service

import com.myworld.schat.common.ApiResult
import com.myworld.schat.common.SimpleUser
import java.io.IOException
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

interface UserService {
    fun saveNickname(nickname: String): ApiResult<*>
    fun getNickname(): ApiResult<*>
    fun createContactTag(simpleUser: SimpleUser)

    fun getIpAddress(request: HttpServletRequest): String?
}
