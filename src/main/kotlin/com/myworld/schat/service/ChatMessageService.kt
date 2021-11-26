package com.myworld.schat.service

import com.myworld.schat.common.ApiResult
import org.springframework.web.multipart.MultipartFile
import java.io.IOException
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

interface ChatMessageService {

    fun getListPair(tagName: String, request: HttpServletRequest): ApiResult<*>?
    fun getListGroup(): ApiResult<*>

    @Throws(IOException::class)
    fun uploadChatPhoto(toNames: String, sendTime: String, file: MultipartFile, request: HttpServletRequest, response: HttpServletResponse): ApiResult<*>?
    @Throws(IOException::class)
    fun uploadChatPhotoBase64(toNames: String, sendTime: String, base64: String, request: HttpServletRequest, response: HttpServletResponse): ApiResult<*>?
    @Throws(IOException::class)
    fun saveChatPhoto(fromName: String, toNames: String, sendTime: String, fileBytes: ByteArray, extensionType: String, fileName: String): String?
    @Throws(IOException::class)
    fun getChatFileLocation(idDetail: String, response: HttpServletResponse)
    @Throws(IOException::class)
    fun uploadChatFile(toNames: String, sendTime: String, file: MultipartFile, request: HttpServletRequest, response: HttpServletResponse): ApiResult<*>?


    @Throws(IOException::class)
    fun uploadGroupAvatarBase64(groupId: String, base64: String): ApiResult<*>?
    @Throws(IOException::class)
    fun getAvatarGroup(id: String, response: HttpServletResponse)

    fun editChatGroup(nickname: String, memberName: String, showMemberNickname: Boolean, groupId: String?, widNameList: MutableList<String>): ApiResult<*>?
    fun delChatGroup(id: String): ApiResult<*>
}
