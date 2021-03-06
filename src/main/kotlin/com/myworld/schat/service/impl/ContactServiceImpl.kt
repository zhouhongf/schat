package com.myworld.schat.service.impl

import com.myworld.schat.common.ApiResult
import com.myworld.schat.common.ResultUtil
import com.myworld.schat.common.UserContextHolder
import com.myworld.schat.common.UtilService
import com.myworld.schat.data.entity.ChatGroup
import com.myworld.schat.data.entity.ContactTag
import com.myworld.schat.data.entity.FriendApply
import com.myworld.schat.data.entity.PhoneContact
import com.myworld.schat.data.modal.Constants
import com.myworld.schat.data.modal.ContactTagMessageNum
import com.myworld.schat.data.modal.PhoneContactReply
import com.myworld.schat.data.repository.*
import com.myworld.schat.service.ChatRedisService
import com.myworld.schat.service.ContactService
import org.apache.logging.log4j.LogManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Service
import java.sql.Timestamp
import java.util.*
import kotlin.collections.ArrayList

@Service
open class ContactServiceImpl : ContactService {

    private val log = LogManager.getRootLogger()

    @Autowired
    private lateinit var phoneUserRepository: PhoneUserRepository
    @Autowired
    private lateinit var contactTagRepository: ContactTagRepository
    @Autowired
    private lateinit var phoneContactRepository: PhoneContactRepository
    @Autowired
    private lateinit var friendApplyRepository: FriendApplyRepository
    @Autowired
    private lateinit var chatRedisService: ChatRedisService
    @Autowired
    private lateinit var mongoTemplate: MongoTemplate


    override fun getContactTagsRemote(): ApiResult<*>? {
        val simpleUser = UserContextHolder.getUserContext()
        val userWid = simpleUser.wid

        val sort = Sort(Sort.Direction.ASC, "orderClass")
        val contactTags = contactTagRepository.findByUserWid(userWid, sort) ?: return ResultUtil.failure(-2, "????????????????????????contactTag??????")

        val contactTagMessageNums: MutableList<ContactTagMessageNum> = ArrayList()
        for (contactTag in contactTags) {
            val ctmn = ContactTagMessageNum()
            ctmn.idDetail = contactTag.id
            ctmn.name = contactTag.name
            ctmn.nums = contactTag.nums
            ctmn.order = contactTag.orderClass
            ctmn.messages = 0
            contactTagMessageNums.add(ctmn)
        }

        // ?????????????????????
        val query = Query()
        query.addCriteria(Criteria.where("wids").all(userWid))
        val groupCount = mongoTemplate.count(query, ChatGroup::class.java)

        // ???????????????????????????
        val mainKey = "offline-$userWid"
        val messageKeys = chatRedisService.listMessageKeys(mainKey) ?: return ResultUtil.success(num = groupCount, data = contactTagMessageNums)

        for (ctmn in contactTagMessageNums) {
            val tagName = ctmn.name
            for (mkey in messageKeys) {
                val tag = mkey.split("-").toTypedArray()[0]
                if (tagName == tag) {
                    val unreadMesNum = ctmn.messages!! + 1
                    ctmn.messages = unreadMesNum
                }
            }
        }
        return ResultUtil.success(num = groupCount, data = contactTagMessageNums)
    }

    override fun updateContactTagsOrder(contactTagMessageNums: List<ContactTagMessageNum>): ApiResult<*>? {
        val simpleUser = UserContextHolder.getUserContext()
        val userWid = simpleUser.wid
        val contactTags = contactTagRepository.findByUserWid(userWid) ?: return ResultUtil.failure(-2, "????????????????????????contactTags")

        for ((_, name, _, _, order) in contactTagMessageNums) {
            for (ct in contactTags) {
                val theName = ct.name
                if (theName == name) {
                    ct.orderClass = order
                    ct.updateTime = Date().time
                    contactTagRepository.save(ct)
                }
            }
        }
        return ResultUtil.success()
    }

    override fun updateContactTag(wid: String, tagName: String) {
        val contactTag = contactTagRepository.findByUserWidAndName(wid, tagName)
        if (contactTag != null) {
            val num = phoneContactRepository.countByUserWidAndTag(wid, tagName)
            contactTag.nums = num
            contactTag.updateTime = Date().time
            contactTagRepository.save(contactTag)
        }
    }

    override fun getAllContacts(): ApiResult<*>? {
        val simpleUser = UserContextHolder.getUserContext()
        val userWid = simpleUser.wid
        val phoneContacts = phoneContactRepository.findByUserWid(userWid) ?: return ResultUtil.failure(-2, "??????????????????????????????")

        val phoneContactReplies: MutableList<PhoneContactReply> = ArrayList()
        for (phoneContact in phoneContacts) {
            val targetWid = phoneContact.targetWid
            if (targetWid != null) {
                val user = phoneUserRepository.findByWid(targetWid)
                val phoneContactReply = PhoneContactReply()
                phoneContactReply.wid = targetWid
                phoneContactReply.nickname = user!!.nickname
                phoneContactReply.phoneNumber = user.username
                phoneContactReply.displayName = phoneContact.displayName
                phoneContactReply.tag = phoneContact.tag
                phoneContactReplies.add(phoneContactReply)
            }
        }
        return ResultUtil.success(data = phoneContactReplies)
    }

    /**
     * ????????????????????????wid
     */
    override fun searchContactRemote(value: String): ApiResult<*>? {
        val simpleUser = UserContextHolder.getUserContext()
        val theWid = simpleUser.wid
        if (theWid == value) {
            return ResultUtil.failure(-2, "?????????????????????????????????")
        }

        // ???????????????????????????
        if (UtilService.isZhCN(value)) {
            val phoneUsers = phoneUserRepository.findByNickname(value)
            if (phoneUsers!!.size == 1) {
                val phoneUser = phoneUsers[0]
                val wid = phoneUser.wid
                val username = phoneUser.username
                if (theWid != wid) {
                    val back = "$wid,$value,$username"
                    return ResultUtil.success(data = back)
                }
            }
        }
        // ????????????????????????
        if (UtilService.isInteger(value)) {
            val phoneUser = phoneUserRepository.findByUsername(value)
            if (phoneUser != null) {
                val wid = phoneUser.wid
                val nickname = phoneUser.nickname
                if (theWid != wid) {
                    val back = "$wid,$nickname,$value"
                    return ResultUtil.success(data = back)
                }
            }
        }
        // ???????????????6-18?????????????????????????????????????????????????????????????????????
        if (UtilService.isWid(value)) {
            val phoneUser = phoneUserRepository.findByWid(value)
            if (phoneUser != null) {
                val nickname = phoneUser.nickname
                val username = phoneUser.username
                val back = "$value,$nickname,$username"
                return ResultUtil.success(data = back)
            }
        }
        return ResultUtil.failure(-2, "???????????????????????????")
    }

    override fun checkPhoneContacts(values: List<String>): ApiResult<*>? {
        log.info("???????????????checkPhoneContacts?????????")
        val simpleUser = UserContextHolder.getUserContext()
        val userWid = simpleUser.wid
        val phoneContactReplies: MutableList<PhoneContactReply> = ArrayList()
        for (value in values) {
            val valueList = value.split("--").toTypedArray()
            val displayName = valueList[0]
            val phoneNumber = valueList[1]
            // ???????????????????????? ?????????????????????????????????????????????APP???phoneUser??????
            val targetUser = phoneUserRepository.findByUsername(phoneNumber)
            if (targetUser != null) {
                val targetWid = targetUser.wid
                val phoneContact = phoneContactRepository.findByUserWidAndTargetWid(userWid, targetWid)
                // ????????????userWid???targetWid???????????????????????????phoneContact??????????????????????????????"???????????????"?????????
                if (phoneContact == null) {
                    val phoneContactReply = PhoneContactReply()
                    phoneContactReply.wid = targetWid
                    phoneContactReply.nickname = targetUser.nickname
                    phoneContactReply.displayName = displayName
                    phoneContactReply.phoneNumber = phoneNumber
                    phoneContactReply.tag = "?????????"
                    phoneContactReplies.add(phoneContactReply)
                }
            }
        }
        return if (phoneContactReplies.size > 0) {
            ResultUtil.success(data = phoneContactReplies)
        } else ResultUtil.failure(-2, "???????????????????????????")
    }

    override fun applyFriend(wid: String, remarkName: String, tagName: String, applyContent: String): ApiResult<*>? {
        val simpleUser = UserContextHolder.getUserContext()
        val userWid = simpleUser.wid


        val phoneContact = phoneContactRepository.findByUserWidAndTargetWid(userWid, wid)
        if (phoneContact != null) {
            return ResultUtil.failure(-2, "????????????????????????????????????")
        }
        val friendApply = friendApplyRepository.findByUserWidAndTargetWidAndStatus(userWid, wid, "????????????")
        if (friendApply != null) {
            return ResultUtil.failure(-2, "????????????????????????????????????????????????")
        }
        val friendAccept = friendApplyRepository.findByUserWidAndTargetWidAndStatus(wid, userWid, "????????????")
        if (friendAccept != null) {
            return ResultUtil.failure(-2, "???????????????????????????????????????????????????")
        }

        val user = phoneUserRepository.findByWid(userWid)
        val targetUser = phoneUserRepository.findByWid(wid)
        val friend = FriendApply()
        val currentTime = Date().time
        val idDetail = Constants.FRIEND_APPLY_PREFIX + currentTime + ((Math.random() * 9 + 1) * 1000).toInt()
        friend.id = idDetail
        friend.status = "????????????"
        friend.userWid = userWid
        friend.userDisplayName = user!!.nickname
        friend.userPhoneNumber = user.username
        friend.applyContent = applyContent
        friend.applyTime = currentTime
        friend.targetWid = wid
        friend.targetDisplayName = targetUser!!.nickname
        friend.targetPhoneNumber = targetUser.username
        friend.targetRemarkName = remarkName
        friend.targetTagName = tagName
        friendApplyRepository.save(friend)
        return ResultUtil.success()
    }

    override fun checkPhoneApplies(): ApiResult<*>? {
        val simpleUser = UserContextHolder.getUserContext()
        val userWid = simpleUser.wid

        val sort = Sort(Sort.Direction.DESC, "applyTime")
        val friendApplies = friendApplyRepository.findByStatusAndUserWidOrStatusAndTargetWid("????????????", userWid, "????????????", userWid, sort) ?: return ResultUtil.failure(-2, "???????????????")
        return ResultUtil.success(data = friendApplies)
    }

    override fun confirmFriend(wid: String, remarkName: String, tagName: String, result: String, reject: String): ApiResult<*>? {
        val simpleUser = UserContextHolder.getUserContext()
        val userWid = simpleUser.wid

        val phoneContact = phoneContactRepository.findByUserWidAndTargetWid(userWid, wid)
        if (phoneContact != null) {
            return ResultUtil.failure(-2, "??????????????????????????????????????????????????????")
        }
        val friendApply = friendApplyRepository.findByUserWidAndTargetWidAndStatus(wid, userWid, "????????????")
        friendApply!!.status = result
        friendApply.targetContent = reject
        friendApply.confirmTime = Date().time
        friendApplyRepository.save(friendApply)
        if (result == "????????????") { // ???????????????
            val idDetailAccept = Constants.FRIEND_PREFIX + '-' + userWid + '-' + wid
            val contactForAcceptor = PhoneContact()
            contactForAcceptor.id = idDetailAccept
            contactForAcceptor.userWid = userWid
            contactForAcceptor.targetWid = wid
            contactForAcceptor.displayName = remarkName
            contactForAcceptor.tag = tagName
            phoneContactRepository.save(contactForAcceptor)
            updateContactTag(userWid, tagName)
            // ???????????????
            val idDetailApply = Constants.FRIEND_PREFIX + '-' + wid + '-' + userWid
            val tagNameApply = friendApply.targetTagName
            val contactForApplier = PhoneContact()
            contactForApplier.id = idDetailApply
            contactForApplier.userWid = wid
            contactForApplier.targetWid = userWid
            contactForApplier.displayName = friendApply.targetRemarkName
            contactForApplier.tag = tagNameApply
            phoneContactRepository.save(contactForApplier)
            updateContactTag(wid, tagNameApply!!)
        }
        return ResultUtil.success()
    }

    override fun updateFriendInfo(wid: String, remarkName: String, tagName: String): ApiResult<*>? {
        val simpleUser = UserContextHolder.getUserContext()
        val userWid = simpleUser.wid

        val phoneContact = phoneContactRepository.findByUserWidAndTargetWid(userWid, wid)
        val tagNameOld = phoneContact!!.tag
        phoneContact.displayName = remarkName
        phoneContact.tag = tagName
        phoneContactRepository.save(phoneContact)
        if (tagName != tagNameOld) {
            val tagOld = contactTagRepository.findByUserWidAndName(userWid, tagNameOld!!)
            if (tagOld != null) {
                val numOld = tagOld.nums
                tagOld.nums = numOld!! - 1
                tagOld.updateTime = Date().time
                contactTagRepository.save(tagOld)
            }
            val tagNew = contactTagRepository.findByUserWidAndName(userWid, tagName)
            if (tagNew != null) {
                val numNew = tagNew.nums
                tagNew.nums = numNew!! + 1
                tagNew.updateTime = Date().time
                contactTagRepository.save(tagNew)
            }
        }
        return ResultUtil.success()
    }


    override fun editContactTag(idDetail: String, name: String, wids: List<String>): ApiResult<*>? {
        val simpleUser = UserContextHolder.getUserContext()
        val userWid = simpleUser.wid

        val phoneUser = phoneUserRepository.findByWid(userWid)

        val theTime = Date().time
        var contactTagEdit = contactTagRepository.findByUserWidAndId(userWid, idDetail)
        if (contactTagEdit == null) { // ??????idDetail???NEW, ?????????name?????? ????????????????????????????????????????????????????????????????????????????????????
            if (idDetail == "NEW") {
                if (name == "?????????") {
                    return ResultUtil.failure(-2, "????????????????????????")
                }
                val ctag = contactTagRepository.findByUserWidAndName(userWid, name)
                if (ctag != null) {
                    return ResultUtil.failure(-2, "????????????????????????")
                }
                for (targetWid in wids) {
                    val phoneContact = phoneContactRepository.findByUserWidAndTargetWid(userWid, targetWid)
                    if (phoneContact != null) {
                        phoneContact.tag = name
                        phoneContactRepository.save(phoneContact)
                    }
                }
                val tagIdDetail = Constants.CONTACT_TAG_PREFIX + theTime
                contactTagEdit = ContactTag(id = tagIdDetail)
                // ???????????????contactTag???orderClass????????????
                val totalTagNum = contactTagRepository.countByUserWid(userWid)
                contactTagEdit.orderClass = totalTagNum
                contactTagEdit.id = tagIdDetail
                contactTagEdit.userWid = userWid
                contactTagEdit.username = phoneUser!!.username
                contactTagEdit.createTime = theTime
            } else {
                return ResultUtil.failure(-2, "??????????????????????????????")
            }
        } else { // ??????contactTag??????????????????????????????
            val oldTagName = contactTagEdit.name
            if (oldTagName == name) { // ????????????????????????????????????????????????
                // ????????????wid???????????????????????????????????????????????????????????????phoneContact??????
                val phoneContacts = phoneContactRepository.findByUserWidAndTag(userWid, oldTagName)
                // ??????????????????????????????????????????????????????list???
                val widsSet: Set<String> = HashSet(wids)
                for (pc in phoneContacts!!) {
                    val oldWid = pc.targetWid
                    // ??????wid???set????????????????????????????????????phoneContact???wid????????????phoneContact?????? ????????? ?????????
                    if (!widsSet.contains(oldWid)) {
                        pc.tag = "?????????"
                        phoneContactRepository.save(pc)
                    }
                }
                // ????????????list??????phoneContact????????????????????????tagName????????????????????????phoneContact?????????????????????
                for (targetWid in wids) {
                    val phoneContact = phoneContactRepository.findByUserWidAndTargetWid(userWid, targetWid)
                    if (phoneContact != null) {
                        phoneContact.tag = name
                        phoneContactRepository.save(phoneContact)
                    }
                }
            } else { // ?????????????????????
                // ?????????????????????????????????list??????phoneContact
                for (targetWid in wids) {
                    val phoneContact = phoneContactRepository.findByUserWidAndTargetWid(userWid, targetWid)
                    if (phoneContact != null) {
                        phoneContact.tag = name
                        phoneContactRepository.save(phoneContact)
                    }
                }
                // ????????????wid????????????????????????????????????????????????????????????????????????????????????????????????phoneContact??????
                val phoneContacts = phoneContactRepository.findByUserWidAndTag(userWid, oldTagName!!)
                if (phoneContacts!!.size > 0) {
                    for (pc in phoneContacts) {
                        pc.tag = "?????????"
                        phoneContactRepository.save(pc)
                    }
                }
            }
        }
        // ?????? ????????? ????????????
        val contactTagUntag = contactTagRepository.findByUserWidAndName(userWid, "?????????")
        // ???????????? ????????? ?????????????????????
        val untagNum = phoneContactRepository.countByUserWidAndTag(userWid, "?????????")
        contactTagUntag!!.nums = untagNum
        contactTagUntag.updateTime = theTime
        contactTagRepository.save(contactTagUntag)
        // ??????????????????????????????????????????contactTag???????????????
        val tagEditNum = phoneContactRepository.countByUserWidAndTag(userWid, name)
        contactTagEdit.name = name
        contactTagEdit.nums = tagEditNum
        contactTagEdit.updateTime = theTime
        contactTagRepository.save<ContactTag>(contactTagEdit)
        return ResultUtil.success()
    }

    override fun delContactTag(idDetail: String): ApiResult<*>? {
        val simpleUser = UserContextHolder.getUserContext()
        val userWid = simpleUser.wid

        val contactTag = contactTagRepository.findByUserWidAndId(userWid, idDetail) ?: return ResultUtil.failure(-2, "??????????????????????????????")
        val tagName = contactTag.name
        val tagOrder = contactTag.orderClass
        // ?????????????????????????????????PhoneContact???????????????????????????????????????????????? ????????? ?????????
        val phoneContacts = phoneContactRepository.findByUserWidAndTag(userWid, tagName!!)
        val numChange = phoneContacts!!.size
        if (numChange > 0) {
            for (pc in phoneContacts) {
                pc.tag = "?????????"
                phoneContactRepository.save(pc)
            }
        }
        // ???????????????
        contactTagRepository.deleteByUserWidAndId(userWid, idDetail)
        // ?????????????????????????????????????????????????????????
        val contactTags = contactTagRepository.findByUserWid(userWid)
        for (ct in contactTags!!) {
            val theOrder = ct.orderClass
            if (theOrder!! > tagOrder!!) {
                val newOrder = theOrder - 1
                ct.orderClass = newOrder
                ct.updateTime = Date().time
                contactTagRepository.save(ct)
            }
            // ?????????????????????????????? ????????? ??????
            val theTagName = ct.name
            if (theTagName == "?????????") {
                val theNum = ct.nums
                ct.nums = theNum!! + numChange
                contactTagRepository.save(ct)
            }
        }
        return ResultUtil.success()
    }
}
