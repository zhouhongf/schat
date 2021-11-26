package com.myworld.schat.service

import com.myworld.schat.common.ApiResult
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import javax.servlet.http.HttpServletRequest
import javax.websocket.server.PathParam

@Component
@FeignClient(name = "cgate-server", url = "\${feign.cgate.url}")
interface GateFeignService {

    @RequestMapping(value = ["/cauth/getUsername"], method = [RequestMethod.GET])
    fun getUsername(@RequestParam wid: String): String?



    @RequestMapping(value = ["/tokenToSimpleUser"], method = [RequestMethod.POST])
    fun tokenToSimpleUser(@RequestBody token: String?): String?

    @RequestMapping(value = ["/getRegionOnIpAddress"], method = [RequestMethod.GET])
    fun getRegionOnIpAddress(@RequestParam ip: String): String?

    @RequestMapping(value = ["/getCityOnIpAddress"], method = [RequestMethod.GET])
    fun getCityOnIpAddress(request: HttpServletRequest): ApiResult<*>?
}
