package com.myworld.schat.controller

import com.myworld.schat.common.ApiResult
import com.myworld.schat.data.modal.ContactTagMessageNum
import com.myworld.schat.service.ContactService
import org.apache.logging.log4j.LogManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*


@RestController
class ContactController {
    private val log = LogManager.getRootLogger()

    @Autowired
    private lateinit var contactService: ContactService

    @GetMapping("/getContactTagsRemote")
    fun getContactTagsRemote(): ApiResult<*>? {
        return contactService.getContactTagsRemote()
    }

    @PostMapping("/updateContactTagsOrder")
    fun updateContactTagsOrder(@RequestBody contactTagMessageNums: List<ContactTagMessageNum>): ApiResult<*>? {
        return contactService.updateContactTagsOrder(contactTagMessageNums)
    }

    @GetMapping("/getAllContacts")
    fun getAllContacts(): ApiResult<*>? {
        return contactService.getAllContacts()
    }

    @GetMapping("/searchContactRemote")
    fun searchContactRemote(@RequestParam value: String): ApiResult<*>? {
        return contactService.searchContactRemote(value)
    }

    @PostMapping("/checkPhoneContacts")
    fun checkPhoneContacts(@RequestBody values: List<String>): ApiResult<*>? {
        return contactService.checkPhoneContacts(values)
    }

    @PostMapping("/applyFriend")
    fun applyFriend(@RequestParam wid: String, @RequestParam remarkName: String, @RequestParam tagName: String, @RequestBody applyContent: String): ApiResult<*>? {
        return contactService.applyFriend(wid, remarkName, tagName, applyContent)
    }

    @GetMapping("/checkPhoneApplies")
    fun checkPhoneApplies(): ApiResult<*>? {
        return contactService.checkPhoneApplies()
    }

    @PostMapping("/confirmFriend")
    fun confirmFriend(@RequestParam wid: String, @RequestParam remarkName: String, @RequestParam tagName: String, @RequestParam result: String, @RequestBody reject: String): ApiResult<*>? {
        return contactService.confirmFriend(wid, remarkName, tagName, result, reject)
    }

    @GetMapping("/updateFriendInfo")
    fun updateFriendInfo(@RequestParam wid: String, @RequestParam remarkName: String, @RequestParam tagName: String): ApiResult<*>? {
        return contactService.updateFriendInfo(wid, remarkName, tagName)
    }

    @PostMapping("/editContactTag")
    fun editContactTag(@RequestParam idDetail: String, @RequestParam name: String, @RequestBody wids: List<String>): ApiResult<*>? {
        return contactService.editContactTag(idDetail, name, wids)
    }

    @DeleteMapping("/delContactTag")
    fun delContactTag(@RequestParam idDetail: String): ApiResult<*>? {
        return contactService.delContactTag(idDetail)
    }

}
