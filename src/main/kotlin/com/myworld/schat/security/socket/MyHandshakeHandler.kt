package com.myworld.schat.security.socket

import com.myworld.schat.data.modal.Constants
import com.myworld.schat.data.modal.User
import org.apache.logging.log4j.LogManager
import org.springframework.http.server.ServerHttpRequest
import org.springframework.stereotype.Component
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.server.support.DefaultHandshakeHandler
import java.security.Principal

/**
 * 处理WebSocket的握手请求，定义自己的Principal
 */
@Component
class MyHandshakeHandler : DefaultHandshakeHandler() {

    private val log = LogManager.getRootLogger()
    override fun determineUser(request: ServerHttpRequest, wsHandler: WebSocketHandler, attributes: Map<String, Any>): Principal {
         log.info("【MyHandshakeHandler开始执行determineUser方法attributes是：{}】", attributes.toString())
         val userWid = attributes[Constants.CHAT_ID] as String
         return User(userWid)
    }
}
