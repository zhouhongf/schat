package com.myworld.schat.security.socket

import com.myworld.schat.data.modal.Constants
import org.apache.logging.log4j.LogManager
import org.springframework.http.server.ServerHttpRequest
import org.springframework.http.server.ServerHttpResponse
import org.springframework.http.server.ServletServerHttpRequest
import org.springframework.lang.Nullable
import org.springframework.stereotype.Component
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.server.HandshakeInterceptor
import javax.servlet.http.HttpSession

/**
 * 参考 HttpSessionHandshakeInterceptor自定义握手拦截
 * 这个拦截器用来管理握手和握手后的事情，结合WebSocketcontroller中的makeHttpSession()方法，可以先通过解析token, 来判用户是否可以连接
 * 并将解析出来的userWid放入httpSession中
 */
@Component
class MyHandshakeInterceptor : HandshakeInterceptor {
    private val log = LogManager.getRootLogger()

    @Throws(Exception::class)
    override fun beforeHandshake(request: ServerHttpRequest, response: ServerHttpResponse, wsHandler: WebSocketHandler, attributes: MutableMap<String, Any>): Boolean {
        log.info("【开始执行beforeHandshake方法】")
        val session = getSession(request)
        if (session == null) {
            log.info("【没有HttpSession】")
            return false
        }
        // val names = session.attributeNames
        // while (names.hasMoreElements()) {
        //     val name = names.nextElement()
        //     log.info("【迭代并取出session中的内容{}：{}】", name, session.getAttribute(name))
        // }
        val wid = session.getAttribute(Constants.CHAT_ID)
        if (wid == null) {
            log.info("【HttpSession中没有wid】")
            return false
        }
        // 将userWid放入attribute中
        attributes[Constants.CHAT_ID] = wid
        return true
    }

    override fun afterHandshake(request: ServerHttpRequest, response: ServerHttpResponse, wsHandler: WebSocketHandler, @Nullable exception: Exception) {
        log.info("【开始执行afterHandshake方法】")
    }

    // 获取HttpSession
    @Nullable
    private fun getSession(request: ServerHttpRequest): HttpSession? {
        if (request is ServletServerHttpRequest) {
            return request.servletRequest.getSession(false)
        }
        return null
    }
}
