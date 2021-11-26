package com.myworld.schat.service

import com.myworld.schat.common.ApiResult
import com.myworld.schat.data.modal.ContactTagMessageNum


interface ContactService {

    fun getContactTagsRemote(): ApiResult<*>?
    fun updateContactTagsOrder(contactTagMessageNums: List<ContactTagMessageNum>): ApiResult<*>?
    fun updateContactTag(wid: String, tagName: String)
    fun editContactTag(idDetail: String, name: String, wids: List<String>): ApiResult<*>?
    fun delContactTag(idDetail: String): ApiResult<*>?

    fun getAllContacts(): ApiResult<*>?
    fun searchContactRemote(value: String): ApiResult<*>?
    fun checkPhoneContacts(values: List<String>): ApiResult<*>?

    fun applyFriend(wid: String, remarkName: String, tagName: String, applyContent: String): ApiResult<*>?
    fun checkPhoneApplies(): ApiResult<*>?
    fun confirmFriend(wid: String, remarkName: String, tagName: String, result: String, reject: String): ApiResult<*>?
    fun updateFriendInfo(wid: String, remarkName: String, tagName: String): ApiResult<*>?
}
