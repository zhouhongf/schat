package com.myworld.schat.controller

import com.myworld.schat.common.ApiResult
import com.myworld.schat.service.ChatMessageService
import org.apache.logging.log4j.LogManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.io.IOException
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@RestController
class ChatMessageController {
    private val log = LogManager.getRootLogger()
    @Autowired
    private lateinit var chatMessageService: ChatMessageService

    @GetMapping("/getListPair")
    fun getListPair(@RequestParam tagName: String, request: HttpServletRequest): ApiResult<*>? {
        return chatMessageService.getListPair(tagName, request)
    }

    @GetMapping("/getListGroup")
    fun getListGroup(): ApiResult<*> {
        return chatMessageService.getListGroup()
    }

    @PostMapping("/editChatGroup")
    fun editChatGroup(
        @RequestParam("nickname") nickname: String,
        @RequestParam("memberName") memberName: String,
        @RequestParam("showMemberNickname") showMemberNickname: Boolean,
        @RequestParam("groupId") groupId: String?,
        @RequestBody widNameList: MutableList<String>
    ): ApiResult<*>? {
        return chatMessageService.editChatGroup(nickname, memberName, showMemberNickname, groupId, widNameList)
    }

    @DeleteMapping("/delChatGroup")
    fun delChatGroup(@RequestParam id: String): ApiResult<*> {
        return chatMessageService.delChatGroup(id)
    }


    @PostMapping(value = ["/uploadChatPhoto"], consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @Throws(IOException::class)
    fun uploadChatPhoto(@RequestParam toNames: String, @RequestParam sendTime: String, @RequestBody file: MultipartFile, request: HttpServletRequest, response: HttpServletResponse): ApiResult<*>? {
        return chatMessageService.uploadChatPhoto(toNames, sendTime, file, request, response)
    }

    @PostMapping(value = ["/uploadChatFile"], consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @Throws(IOException::class)
    fun uploadChatFile(@RequestParam toNames: String, @RequestParam sendTime: String, @RequestBody file: MultipartFile, request: HttpServletRequest, response: HttpServletResponse): ApiResult<*>? {
        return chatMessageService.uploadChatFile(toNames, sendTime, file, request, response)
    }

    @PostMapping("/uploadChatPhotoBase64")
    @Throws(IOException::class)
    fun uploadChatPhotoBase64(@RequestParam toNames: String, @RequestParam sendTime: String, @RequestBody base64: String, request: HttpServletRequest, response: HttpServletResponse): ApiResult<*>? {
        return chatMessageService.uploadChatPhotoBase64(toNames, sendTime, base64, request, response)
    }

    @GetMapping(value = ["/getChatFileLocation/{idDetail}"], produces = [MediaType.APPLICATION_OCTET_STREAM_VALUE])
    @Throws(IOException::class)
    fun getChatFileLocation(@PathVariable idDetail: String, response: HttpServletResponse) {
        chatMessageService.getChatFileLocation(idDetail, response)
    }


    @PostMapping("/uploadGroupAvatarBase64")
    @Throws(IOException::class)
    fun uploadGroupAvatarBase64(@RequestParam groupId: String, @RequestBody base64: String): ApiResult<*>? {
        return chatMessageService.uploadGroupAvatarBase64(groupId, base64)
    }

    @GetMapping(value = ["/avatarGroup/{id}"], produces = [MediaType.APPLICATION_OCTET_STREAM_VALUE])
    @Throws(IOException::class)
    fun getGroupPhotoLocation(@PathVariable id: String, response: HttpServletResponse) {
        chatMessageService.getAvatarGroup(id, response)
    }
}
