package com.myworld.schat.controller

import com.myworld.schat.common.ApiResult
import com.myworld.schat.common.ResultUtil
import com.myworld.schat.common.UserContextHolder
import com.myworld.schat.data.entity.ChatMessage
import com.myworld.schat.data.modal.Constants
import com.myworld.schat.service.impl.ChatServiceImpl
import org.apache.logging.log4j.LogManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.simp.annotation.SubscribeMapping
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.*
import javax.servlet.http.HttpServletRequest


@RestController
class WebSocketController {
    private val log = LogManager.getRootLogger()
    @Autowired
    private lateinit var chatService: ChatServiceImpl

    @GetMapping("/makeHttpSession")
    fun makeHttpSession(request: HttpServletRequest): ApiResult<*> {
        val simpleUser = UserContextHolder.getUserContext()
        val wid = simpleUser.wid
        if (wid == "") {
            return ResultUtil.failure(-2, "没有wid")
        }
        val session = request.session
        session.setAttribute(Constants.CHAT_ID, wid)
        return ResultUtil.success()
    }

    @GetMapping("/getChatMessagesPair")
    fun getChatMessagesPair(@RequestParam pageSize: Int, @RequestParam startNumber: Long, @RequestParam friendWid: String): ApiResult<*>? {
        return chatService.getChatMessagesPair(pageSize, startNumber, friendWid)
    }

    @GetMapping("/getChatMessagesGroup")
    fun getChatMessagesGroup(@RequestParam pageSize: Int, @RequestParam startNumber: Long, @RequestParam groupId: String): ApiResult<*>? {
        return chatService.getChatMessagesGroup(pageSize, startNumber, groupId)
    }


    @SubscribeMapping("/chatall")
    fun chatall(): Long {
        return Date().time
    }

    @MessageMapping("/sendChatpair")
    fun sendChatpair(@RequestBody chatMessage: ChatMessage, headerAccessor: StompHeaderAccessor) {
        // log.info("在controller中headerAccessor的内容是：{}", headerAccessor.toString());
        // String fromName = headerAccessor.getUser().getName();
        // chatMessage.setFromName(fromName);
        chatService.sendChatpair(chatMessage, headerAccessor)
    }

    @MessageMapping("/sendChatgroup")
    fun sendChatgroup(@RequestBody chatMessage: ChatMessage, headerAccessor: StompHeaderAccessor) {
        // log.info("在controller中headerAccessor的内容是：{}", headerAccessor.toString());
        // String fromName = headerAccessor.getUser().getName();
        // chatMessage.setFromName(fromName);
        chatService.sendChatgroup(chatMessage, headerAccessor)
    }

}
