package com.myworld.schat.service.impl

import com.myworld.schat.common.*
import com.myworld.schat.data.entity.ContactTag
import com.myworld.schat.data.entity.PhoneUser
import com.myworld.schat.data.modal.Constants
import com.myworld.schat.data.repository.ContactTagRepository
import com.myworld.schat.data.repository.PhoneUserRepository
import com.myworld.schat.service.GateFeignService
import com.myworld.schat.service.UserService
import org.apache.logging.log4j.LogManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.*
import javax.servlet.http.HttpServletRequest



@Service
open class UserServiceImpl : UserService {

    private val log = LogManager.getRootLogger()

    @Autowired
    private lateinit var phoneUserRepository: PhoneUserRepository
    @Autowired
    private lateinit var contactTagRepository: ContactTagRepository
    @Autowired
    private lateinit var gateFeignService: GateFeignService


    override fun saveNickname(nickname: String): ApiResult<*> {
        val simpleUser = UserContextHolder.getUserContext()
        val wid = simpleUser.wid

        val optional = phoneUserRepository.findById(wid)
        val phoneUser: PhoneUser
        if (optional.isPresent) {
            phoneUser = optional.get()
        } else {
            val username = gateFeignService.getUsername(wid)
            simpleUser.username = username
            phoneUser = PhoneUser(wid = wid, username = username, playerType = simpleUser.playerType, offer = simpleUser.offer)
            createContactTag(simpleUser)
        }
        phoneUser.nickname = nickname
        phoneUserRepository.save(phoneUser)
        return ResultUtil.success()
    }

    override fun getNickname(): ApiResult<*> {
        val simpleUser = UserContextHolder.getUserContext()
        val wid = simpleUser.wid
        val optional = phoneUserRepository.findById(wid)
        return if (optional.isPresent) {
            ResultUtil.success(data = optional.get().nickname)
        } else {
            ResultUtil.failure(msg = "此用户无昵称")
        }
    }

    override fun createContactTag(simpleUser: SimpleUser) {
        val tagIdDetail = Constants.CONTACT_TAG_PREFIX + Date().time
        val contactTag = ContactTag(id = tagIdDetail, userWid = simpleUser.wid, username = simpleUser.username, name = "未分类", nums = 0, orderClass = 0)
        contactTagRepository.save(contactTag)
    }

    override fun getIpAddress(request: HttpServletRequest): String? {
        return UtilService.getIpAddress(request)
    }

}
