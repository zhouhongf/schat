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
        val contactTags = contactTagRepository.findByUserWid(userWid, sort) ?: return ResultUtil.failure(-2, "未能找到该用户的contactTag记录")

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

        // 检查群聊的数量
        val query = Query()
        query.addCriteria(Criteria.where("wids").all(userWid))
        val groupCount = mongoTemplate.count(query, ChatGroup::class.java)

        // 检查是否有离线消息
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
        val contactTags = contactTagRepository.findByUserWid(userWid) ?: return ResultUtil.failure(-2, "未能找到该用户的contactTags")

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
        val phoneContacts = phoneContactRepository.findByUserWid(userWid) ?: return ResultUtil.failure(-2, "数据库中没有您的伙伴")

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
     * 返回通讯录用户的wid
     */
    override fun searchContactRemote(value: String): ApiResult<*>? {
        val simpleUser = UserContextHolder.getUserContext()
        val theWid = simpleUser.wid
        if (theWid == value) {
            return ResultUtil.failure(-2, "您搜索的是您本人的账号")
        }

        // 检查是否是中文字符
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
        // 检查是否是纯数字
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
        // 检查是否是6-18位，字母开头，字母或数字组合（有可能是纯字母）
        if (UtilService.isWid(value)) {
            val phoneUser = phoneUserRepository.findByWid(value)
            if (phoneUser != null) {
                val nickname = phoneUser.nickname
                val username = phoneUser.username
                val back = "$value,$nickname,$username"
                return ResultUtil.success(data = back)
            }
        }
        return ResultUtil.failure(-2, "未能搜索到相关用户")
    }

    override fun checkPhoneContacts(values: List<String>): ApiResult<*>? {
        log.info("【开始执行checkPhoneContacts方法】")
        val simpleUser = UserContextHolder.getUserContext()
        val userWid = simpleUser.wid
        val phoneContactReplies: MutableList<PhoneContactReply> = ArrayList()
        for (value in values) {
            val valueList = value.split("--").toTypedArray()
            val displayName = valueList[0]
            val phoneNumber = valueList[1]
            // 根据电话号码查询 该电话号码用户有没有注册为竞融APP的phoneUser用户
            val targetUser = phoneUserRepository.findByUsername(phoneNumber)
            if (targetUser != null) {
                val targetWid = targetUser.wid
                val phoneContact = phoneContactRepository.findByUserWidAndTargetWid(userWid, targetWid)
                // 如果根据userWid和targetWid在数据库中没有找到phoneContact，则将该电话号码放入"未分类标签"中返回
                if (phoneContact == null) {
                    val phoneContactReply = PhoneContactReply()
                    phoneContactReply.wid = targetWid
                    phoneContactReply.nickname = targetUser.nickname
                    phoneContactReply.displayName = displayName
                    phoneContactReply.phoneNumber = phoneNumber
                    phoneContactReply.tag = "未分类"
                    phoneContactReplies.add(phoneContactReply)
                }
            }
        }
        return if (phoneContactReplies.size > 0) {
            ResultUtil.success(data = phoneContactReplies)
        } else ResultUtil.failure(-2, "没有新的通讯录伙伴")
    }

    override fun applyFriend(wid: String, remarkName: String, tagName: String, applyContent: String): ApiResult<*>? {
        val simpleUser = UserContextHolder.getUserContext()
        val userWid = simpleUser.wid


        val phoneContact = phoneContactRepository.findByUserWidAndTargetWid(userWid, wid)
        if (phoneContact != null) {
            return ResultUtil.failure(-2, "该朋友已存在，不用再添加")
        }
        val friendApply = friendApplyRepository.findByUserWidAndTargetWidAndStatus(userWid, wid, "等待验证")
        if (friendApply != null) {
            return ResultUtil.failure(-2, "该朋友已申请验证中，请勿重复申请")
        }
        val friendAccept = friendApplyRepository.findByUserWidAndTargetWidAndStatus(wid, userWid, "等待验证")
        if (friendAccept != null) {
            return ResultUtil.failure(-2, "该朋友已申请您添加他为伙伴，请查看")
        }

        val user = phoneUserRepository.findByWid(userWid)
        val targetUser = phoneUserRepository.findByWid(wid)
        val friend = FriendApply()
        val currentTime = Date().time
        val idDetail = Constants.FRIEND_APPLY_PREFIX + currentTime + ((Math.random() * 9 + 1) * 1000).toInt()
        friend.id = idDetail
        friend.status = "等待验证"
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
        val friendApplies = friendApplyRepository.findByStatusAndUserWidOrStatusAndTargetWid("等待验证", userWid, "等待验证", userWid, sort) ?: return ResultUtil.failure(-2, "没有新朋友")
        return ResultUtil.success(data = friendApplies)
    }

    override fun confirmFriend(wid: String, remarkName: String, tagName: String, result: String, reject: String): ApiResult<*>? {
        val simpleUser = UserContextHolder.getUserContext()
        val userWid = simpleUser.wid

        val phoneContact = phoneContactRepository.findByUserWidAndTargetWid(userWid, wid)
        if (phoneContact != null) {
            return ResultUtil.failure(-2, "该用户已经是您的伙伴了，无需再次添加")
        }
        val friendApply = friendApplyRepository.findByUserWidAndTargetWidAndStatus(wid, userWid, "等待验证")
        friendApply!!.status = result
        friendApply.targetContent = reject
        friendApply.confirmTime = Date().time
        friendApplyRepository.save(friendApply)
        if (result == "验证通过") { // 验证审核者
            val idDetailAccept = Constants.FRIEND_PREFIX + '-' + userWid + '-' + wid
            val contactForAcceptor = PhoneContact()
            contactForAcceptor.id = idDetailAccept
            contactForAcceptor.userWid = userWid
            contactForAcceptor.targetWid = wid
            contactForAcceptor.displayName = remarkName
            contactForAcceptor.tag = tagName
            phoneContactRepository.save(contactForAcceptor)
            updateContactTag(userWid, tagName)
            // 验证申请者
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
        if (contactTagEdit == null) { // 如果idDetail为NEW, 且标签name不是 未分类，并且拟新建的标签名称还不存在，则新建一个标签实例
            if (idDetail == "NEW") {
                if (name == "未分类") {
                    return ResultUtil.failure(-2, "已存在未分类标签")
                }
                val ctag = contactTagRepository.findByUserWidAndName(userWid, name)
                if (ctag != null) {
                    return ResultUtil.failure(-2, "该标签名称已存在")
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
                // 将新增加的contactTag的orderClass排在最后
                val totalTagNum = contactTagRepository.countByUserWid(userWid)
                contactTagEdit.orderClass = totalTagNum
                contactTagEdit.id = tagIdDetail
                contactTagEdit.userWid = userWid
                contactTagEdit.username = phoneUser!!.username
                contactTagEdit.createTime = theTime
            } else {
                return ResultUtil.failure(-2, "非法操作，请重新登陆")
            }
        } else { // 存在contactTag，本次操作为修改内容
            val oldTagName = contactTagEdit.name
            if (oldTagName == name) { // 如果标签名没有改变，只是人员改变
                // 根据用户wid和标签原来的名称，查询出原来标签下，所有的phoneContact实例
                val phoneContacts = phoneContactRepository.findByUserWidAndTag(userWid, oldTagName)
                // 检查原标签名称中的伙伴是否在传上来的list中
                val widsSet: Set<String> = HashSet(wids)
                for (pc in phoneContacts!!) {
                    val oldWid = pc.targetWid
                    // 如果wid的set集合中不包含原来标签下的phoneContact的wid，则将该phoneContact移至 未分类 标签下
                    if (!widsSet.contains(oldWid)) {
                        pc.tag = "未分类"
                        phoneContactRepository.save(pc)
                    }
                }
                // 所有的在list中的phoneContact都将重新更新一遍tagName，以防止新增加的phoneContact未能处理的情况
                for (targetWid in wids) {
                    val phoneContact = phoneContactRepository.findByUserWidAndTargetWid(userWid, targetWid)
                    if (phoneContact != null) {
                        phoneContact.tag = name
                        phoneContactRepository.save(phoneContact)
                    }
                }
            } else { // 标签名称改变了
                // 按照新的标签名称，更新list中的phoneContact
                for (targetWid in wids) {
                    val phoneContact = phoneContactRepository.findByUserWidAndTargetWid(userWid, targetWid)
                    if (phoneContact != null) {
                        phoneContact.tag = name
                        phoneContactRepository.save(phoneContact)
                    }
                }
                // 根据用户wid和标签原来的名称，查询出，经过前面更新后，在原来标签下，还剩下的phoneContact实例
                val phoneContacts = phoneContactRepository.findByUserWidAndTag(userWid, oldTagName!!)
                if (phoneContacts!!.size > 0) {
                    for (pc in phoneContacts) {
                        pc.tag = "未分类"
                        phoneContactRepository.save(pc)
                    }
                }
            }
        }
        // 更新 未分类 伙伴数量
        val contactTagUntag = contactTagRepository.findByUserWidAndName(userWid, "未分类")
        // 重新计算 未分类 标签的伙伴数量
        val untagNum = phoneContactRepository.countByUserWidAndTag(userWid, "未分类")
        contactTagUntag!!.nums = untagNum
        contactTagUntag.updateTime = theTime
        contactTagRepository.save(contactTagUntag)
        // 统计该标签的伙伴数量，并设置contactTag的标签人数
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

        val contactTag = contactTagRepository.findByUserWidAndId(userWid, idDetail) ?: return ResultUtil.failure(-2, "用户名和该标签不匹配")
        val tagName = contactTag.name
        val tagOrder = contactTag.orderClass
        // 删除该标签后，该用户的PhoneContact伙伴，原来是该标签下的，都要转回 未分类 标签下
        val phoneContacts = phoneContactRepository.findByUserWidAndTag(userWid, tagName!!)
        val numChange = phoneContacts!!.size
        if (numChange > 0) {
            for (pc in phoneContacts) {
                pc.tag = "未分类"
                phoneContactRepository.save(pc)
            }
        }
        // 删除该标签
        contactTagRepository.deleteByUserWidAndId(userWid, idDetail)
        // 删除该标签后，剩下的标签排序要重新排序
        val contactTags = contactTagRepository.findByUserWid(userWid)
        for (ct in contactTags!!) {
            val theOrder = ct.orderClass
            if (theOrder!! > tagOrder!!) {
                val newOrder = theOrder - 1
                ct.orderClass = newOrder
                ct.updateTime = Date().time
                contactTagRepository.save(ct)
            }
            // 将标签的伙伴人数加回 未分类 标签
            val theTagName = ct.name
            if (theTagName == "未分类") {
                val theNum = ct.nums
                ct.nums = theNum!! + numChange
                contactTagRepository.save(ct)
            }
        }
        return ResultUtil.success()
    }
}
