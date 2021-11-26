package com.myworld.schat.config

import com.myworld.schat.common.SimpleUser
import com.myworld.schat.common.UserContextHolder
import com.myworld.schat.data.modal.Constants
import com.myworld.schat.security.jwt.JwtUtil
import com.myworld.schat.security.jwt.TokenPublicKeyGenerator
import org.apache.logging.log4j.LogManager
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor
import org.springframework.web.servlet.ModelAndView
import java.util.*
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import kotlin.collections.ArrayList

@Component
class MyMvcInterceptor : HandlerInterceptor {
    private val log = LogManager.getRootLogger()
    private val uriWhiteList = arrayListOf("getChatFileLocation", "getBlogPanelPhotoLocation", "getPhotoShowLocation", "avatarGroup")

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        // 用于检查httpHeaders中有哪些内容
        // val enu: Enumeration<*> = request.headerNames
        // while (enu.hasMoreElements()) {
        //     val paraName = enu.nextElement() as String
        //     log.info("【request中的Header参数】" + paraName + "值为:" + request.getHeader(paraName))
        // }
        // val params: Enumeration<*> = request.parameterNames
        // while (params.hasMoreElements()) {
        //     val one = params.nextElement() as String
        //     log.info("【request中的Parameter参数】" + one + "值为:" + request.getParameter(one))
        // }

        val requestUri = request.requestURI
        val requestUriList = requestUri.split("/")
        log.info("uriList是：{}", requestUriList)
        if (requestUriList[1] in uriWhiteList) {
            log.info("uri是：{}, 在白名单内", requestUri)
            return true
        }

        val simpleUser = checkTokenInfo(request) ?: return false
        UserContextHolder.setUserContext(simpleUser)
        return true
    }

    override fun postHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any?, modelAndView: ModelAndView?) {}

    override fun afterCompletion(request: HttpServletRequest, response: HttpServletResponse, handler: Any?, ex: Exception?) {
        UserContextHolder.shutdown()
    }


    private fun checkTokenInfo(request: HttpServletRequest): SimpleUser? {
        val token = request.getHeader(Constants.HEADER_AUTH)
        if (token.isNullOrEmpty()) {
            return null
        }

        // 使用公钥解析token后并进行token验证
        val publicKey = TokenPublicKeyGenerator.publicKey
        val map = JwtUtil.parseToken(token, publicKey)

        val userWid = map[JwtUtil.HEADER_WID] as String
        val role = map[JwtUtil.HEADER_ROLE] as ArrayList<*>
        val groupName = map[JwtUtil.GROUP] as String
        val serviceNames = map[JwtUtil.HEADER_SERVICE] as ArrayList<*>
        val expireTime = map[JwtUtil.TIME_EXPIRE] as Long
        val tokenType = map[JwtUtil.TOKEN_TYPE] as String

        // 1、如果token中的group标记不是本机构的，则返回false
        if (groupName != JwtUtil.GROUP_NAME) {
            return null
        }
        //2、验证 将要访问的路由名称 是否与授权时TOKEN中的路由名称 相同
        if ("SCHAT" !in serviceNames) {
            return null
        }
        //3、如果token已过期，需要重新登录，因为redis中保存的原token的黑名单时间即为diffTime，所以无须查找redis中是否有该token的key
        if (expireTime - Date().time < 0) {
            log.info("token已过期，需要重新登录")
            return null
        }
        return SimpleUser(wid = userWid, token = token, playerType = setOf(role))
    }
}
