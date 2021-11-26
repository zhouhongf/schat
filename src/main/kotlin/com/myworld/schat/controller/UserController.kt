package com.myworld.schat.controller

import com.myworld.schat.common.ApiResult
import com.myworld.schat.service.UserService
import org.apache.logging.log4j.LogManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import java.io.IOException
import javax.servlet.http.HttpServletResponse

@RestController
class UserController {
    private val log = LogManager.getRootLogger()

    @Autowired
    private lateinit var userService: UserService

    // 如传递SimpleUser类，可以使用JSONObject将其转换为string
    // JSONObject jsonObject =JSONObject.parseObject(simpleUserStr);
    // SimpleUser simpleUser = JSONObject.toJavaObject(jsonObject, SimpleUser.class);

    @GetMapping("/nickname")
    fun saveNickname(@RequestParam nickname: String): ApiResult<*> {
        return userService.saveNickname(nickname)
    }

    @GetMapping("/getNickname")
    fun getNickname(): ApiResult<*> {
        return userService.getNickname()
    }

}
