package com.myworld.schat.service.impl

import com.myworld.schat.common.ApiResult
import com.myworld.schat.common.ResultUtil
import com.myworld.schat.common.UserContextHolder
import com.myworld.schat.common.UtilService
import com.myworld.schat.data.entity.ChatFile
import com.myworld.schat.data.entity.ChatGroup
import com.myworld.schat.data.entity.ChatGroupPhoto
import com.myworld.schat.data.entity.ChatPhoto
import com.myworld.schat.data.modal.Constants
import com.myworld.schat.data.modal.GroupList
import com.myworld.schat.data.repository.*
import com.myworld.schat.service.ChatMessageService
import com.myworld.schat.service.ChatRedisService
import net.coobird.thumbnailator.Thumbnails
import org.apache.commons.io.IOUtils
import org.apache.logging.log4j.LogManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.*
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import kotlin.collections.ArrayList
import kotlin.collections.HashSet

@Service
class ChatMessageServiceImpl : ChatMessageService {

    private val log = LogManager.getRootLogger()

    @Autowired
    private lateinit var phoneContactRepository: PhoneContactRepository
    @Autowired
    private lateinit var chatPhotoRepository: ChatPhotoRepository
    @Autowired
    private lateinit var chatFileRepository: ChatFileRepository
    @Autowired
    private lateinit var chatRedisService: ChatRedisService
    @Autowired
    private lateinit var mongoTemplate: MongoTemplate
    @Autowired
    private lateinit var chatGroupPhotoRepository: ChatGroupPhotoRepository
    @Autowired
    private lateinit var chatGroupRepository: ChatGroupRepository


    override fun getListPair(tagName: String, request: HttpServletRequest): ApiResult<*>? {
        val simpleUser = UserContextHolder.getUserContext()
        val userWid = simpleUser.wid

        val mainKey = "offline-$userWid"
        val messageIds = chatRedisService.listMessageKeys(mainKey)
        if (messageIds!!.isEmpty()) {
            return ResultUtil.success()
        }
        // 创建一个离线消息的map，key是消息发送者的wid，value是消息数量
        val map: MutableMap<String, Int> = HashMap()
        // 查找进入的tag下面的所有的phoneContacts
        val phoneContacts = phoneContactRepository.findByUserWidAndTag(userWid, tagName)
        for (pc in phoneContacts!!) {
            val fromWidPc = pc.targetWid!!
            var num = 0
            // 通过遍历messageId, 使用fromWidPc匹配messageId的第二个关键字
            // 离线消息保存的格式是：第一个key是：offline-收消息人wid, 第二个key格式如：未分类-ABC123456789-PAIR-TEXT-1580184911951
            for (messageId in messageIds) {
                val words = messageId.split("-").toTypedArray()
                val tag = words[0]
                val fromWid = words[1]     // 即fromUserWid
                val chatType = words[2]    // 判断是PAIR还是GROUP
                if (chatType == Constants.MESSAGE_PAIR_PREFIX && tagName == tag && fromWidPc == fromWid) {
                    num += 1
                }
            }
            if (num > 0) {
                map[fromWidPc] = num
            }
        }
        return ResultUtil.success(data=map)
    }

    override fun getListGroup(): ApiResult<*> {
        val simpleUser = UserContextHolder.getUserContext()
        val userWid = simpleUser.wid

        // 通过mongoTemplate查询wids集合中，含有userWid的所有的chatGroup
        val query = Query()
        query.addCriteria(Criteria.where("wids").all(userWid))
        val chatGroups: List<ChatGroup> = mongoTemplate.find(query.with(Sort(Sort.Direction.DESC, "update_time")), ChatGroup::class.java)
        if (chatGroups.isEmpty()) {
            return ResultUtil.success()
        }

        // 将chatGroup简化为groupList
        val groupLists: MutableList<GroupList> = ArrayList()
        for (chatGroup in chatGroups) {
            val groupList = GroupList(id = chatGroup.id, nickname = chatGroup.nickname, wids = chatGroup.wids, memberNames = chatGroup.memberNames)
            for (one in chatGroup.displayNames) {
                val words = one.split("=")
                if (userWid == words[0]) {
                    groupList.displayName = words[1]
                    break
                }
            }

            for (one in chatGroup.showMemberNicknames) {
                val words = one.split("=")
                if (userWid == words[0]) {
                    groupList.showMemberNickname = (words[1] != "false")
                    break
                }
            }
            groupLists.add(groupList)
        }

        // 检查redis数据库中是否有该用户的offline消息
        // 单聊 离线消息保存的格式是：第一个key是：offline-收消息人wid, 第二个key格式如：未分类-ABC123456789-PAIR-TEXT-1580184911951
        // 群聊 离线消息保存的格式是：第一个key是：offline-收消息人wid, 第二个key格式如: groupId-ABC123456789-GROUP-TEXT-1580184911951
        // groupId的格式为：群创建人wid+GROUP+createTime，无“+”号
        val mainKey = "offline-$userWid"
        val messageIds = chatRedisService.listMessageKeys(mainKey)
        if (messageIds!!.isEmpty()) {
            return ResultUtil.success(data=groupLists)
        }
        // 从redis当中取出全部offline消息，检查key当中的groupId，是否有与上面找到的chatGroup中相同的id的
        for (one in groupLists) {
            var num = 0
            // 检查离线消息key中，分离出来的第一个word是否等于groupId
            for (messageId in messageIds) {
                val words = messageId.split("-").toTypedArray()
                val groupId = words[0]
                val chatType = words[2]    // 判断是PAIR还是GROUP
                if (chatType == Constants.MESSAGE_GROUP_PREFIX && groupId == one.id) {
                    num += 1
                }
            }
            one.unreadMessageNum = num
        }
        return ResultUtil.success(data = groupLists)
    }


    override fun editChatGroup(nickname: String, memberName: String, showMemberNickname: Boolean, groupId: String?, widNameList: MutableList<String>): ApiResult<*>? {
        val simpleUser = UserContextHolder.getUserContext()
        val userWid = simpleUser.wid

        val memberNames: MutableSet<String> = widNameList.toMutableSet()
        log.info("widNameList的内容是：{}", memberNames)

        val chatGroup: ChatGroup
        if (groupId == null) {
            chatGroup = ChatGroup(id = userWid + "GROUP"+ Date().time, creator = userWid, nickname = nickname, updater = userWid)
            val wids : MutableSet<String> = HashSet()
            val displayNames: MutableSet<String> = HashSet()
            val showMemberNicknames: MutableSet<String> = HashSet()
            memberNames.forEach {
                val wid = it.split("=")[0]
                wids.add(wid)
                displayNames.add("$wid=$nickname")
                showMemberNicknames.add("$wid=$showMemberNickname")
            }
            chatGroup.wids = wids
            chatGroup.displayNames = displayNames
            chatGroup.showMemberNicknames = showMemberNicknames
            chatGroup.memberNames = memberNames

        } else {
            val chatGroupOptional = chatGroupRepository.findById(groupId)
            if (chatGroupOptional.isPresent) {
                chatGroup = chatGroupOptional.get()
                // 只有群组成员才能编辑该群组
                val widsOld = chatGroup.wids
                if (userWid !in widsOld) {
                    return ResultUtil.failure(-2, "您不在该群组中，无法编辑该群组")
                }
                val wids : MutableSet<String> = HashSet()
                memberNames.forEach {
                    val wid = it.split("=")[0]
                    wids.add(wid)
                }
                if (userWid !in wids) {
                    return ResultUtil.failure(-2, "不能把自己移出群组")
                }

                if (userWid == chatGroup.creator) {
                    // 如果编辑人是群创建人，则同时更新群nickname和群的displayName
                    chatGroup.nickname = nickname
                    // 群管理员，可以新增或减少群成员, widsOld减wids取差集，得出减少了的wids
                    val widsDel = widsOld.subtract(wids)
                    if (widsDel.isNotEmpty()) {
                        chatGroup.wids.removeIf{ t ->  t.split("=")[0] in widsDel }
                        chatGroup.displayNames.removeIf { t ->  t.split("=")[0] in widsDel }
                        chatGroup.showMemberNicknames.removeIf { t -> t.split("=")[0] in widsDel }
                        chatGroup.memberNames.removeIf { t -> t.split("=")[0] in widsDel }
                    }
                }
                // 不是群管理员，则只能新增群成员，即新增wids
                val widsAdd = wids.subtract(widsOld)
                // 通过跟原来的widsOld比较，取差集，即可找到新增的wids, 则按照新增的wids, 分别新增displayNames、showMemberNicknames、memberNames中的内容
                if (widsAdd.isNotEmpty()) {
                    widsAdd.forEach{
                        chatGroup.wids.add(it)                                          // 添加新增成员的wid
                        chatGroup.displayNames.add("$it=$nickname")                     // 添加新增成员的自己的群的群名称显示
                        chatGroup.showMemberNicknames.add("$it=$showMemberNickname")    // 添加新增成员的自己的群的是否显示其他成员的备注名称
                    }
                    // 如果提交上来的widNameList中通过widsAdd判断出来有新增的成员，则通过匹配wid, 将新增成员添加进chatGroup.memberNames
                    for (one in memberNames) {
                        val wid = one.split("=")[0]
                        if (wid in widsAdd) {
                            chatGroup.memberNames.add(one)
                        }
                    }
                }

                // 操作员更新关于自己在群里的信息
                // 操作员更新自己的群的群名片
                chatGroup.displayNames.removeIf { t ->  t.split("=")[0] == userWid}
                chatGroup.displayNames.add("$userWid=$nickname")
                // 操作员更新自己的名称显示控制
                chatGroup.showMemberNicknames.removeIf { t ->  t.split("=")[0] == userWid}
                chatGroup.showMemberNicknames.add("$userWid=$showMemberNickname")
                // 操作员更新自己的名称
                chatGroup.memberNames.removeIf { t ->  t.split("=")[0] == userWid}
                chatGroup.memberNames.add("$userWid=$memberName")

                chatGroup.updater = userWid
                chatGroup.updateTime = Date().time
            } else {
                return ResultUtil.failure(-2, "错误，无该群组")
            }
        }
        chatGroupRepository.save(chatGroup)
        return ResultUtil.success(data = chatGroup.id);
    }

    override fun delChatGroup(id: String): ApiResult<*> {
        val simpleUser = UserContextHolder.getUserContext()
        val userWid = simpleUser.wid
        val optional = chatGroupRepository.findById(id)
        if (optional.isPresent) {
            val chatGroup = optional.get()
            val creator = chatGroup.creator
            // 如果是群主，删除群，则是直接将群和群头像删除
            if (userWid == creator) {
                chatGroupRepository.deleteById(id)
                chatGroupPhotoRepository.deleteById(id)
            } else {
                // 如果不是群主，是群成员，删除群，则是将自己移出该群
                chatGroup.wids.remove(userWid)
                chatGroup.displayNames.removeIf { t ->  t.split("=")[0] == userWid}
                chatGroup.showMemberNicknames.removeIf { t ->  t.split("=")[0] == userWid}
                chatGroup.memberNames.removeIf { t ->  t.split("=")[0] == userWid}
                chatGroupRepository.save(chatGroup)
            }
        }

        return ResultUtil.success()
    }



    @Throws(IOException::class)
    override fun uploadChatFile(toNames: String, sendTime: String, file: MultipartFile, request: HttpServletRequest, response: HttpServletResponse): ApiResult<*>? {
        val simpleUser = UserContextHolder.getUserContext()
        val userWid = simpleUser.wid

        val idDetail = Constants.MESSAGE_TYPE_FILE + "-" + userWid + "-" + sendTime
        val chatFile = ChatFile()
        chatFile.id = idDetail
        chatFile.fileByte = file.bytes
        chatFile.extensionType = file.contentType
        chatFile.fileName = file.originalFilename
        chatFile.fromName = userWid
        chatFile.toNames = toNames
        chatFile.sendTime = sendTime.toLong()
        chatFileRepository.save(chatFile)
        return ResultUtil.success()
    }

    @Throws(IOException::class)
    override fun uploadChatPhoto(toNames: String, sendTime: String, file: MultipartFile, request: HttpServletRequest, response: HttpServletResponse): ApiResult<*>? {
        val simpleUser = UserContextHolder.getUserContext()
        val userWid = simpleUser.wid

        val base64Str = saveChatPhoto(userWid, toNames, sendTime, file.bytes, file.contentType!!, file.originalFilename!!)
        return ResultUtil.success(data = base64Str)
    }

    @Throws(IOException::class)
    override fun uploadChatPhotoBase64(toNames: String, sendTime: String, base64: String, request: HttpServletRequest, response: HttpServletResponse): ApiResult<*>? {
        val simpleUser = UserContextHolder.getUserContext()
        val userWid = simpleUser.wid

        val base64Bytes = UtilService.base64ToBytes(base64)
        val fileName = "BASE64-$userWid.jpg"
        val base64Str = saveChatPhoto(userWid, toNames, sendTime, base64Bytes, "image/jpeg", fileName)
        return ResultUtil.success(data = base64Str)
    }

    @Throws(IOException::class)
    override fun saveChatPhoto(fromName: String, toNames: String, sendTime: String, fileBytes: ByteArray, extensionType: String, fileName: String): String {
        val idDetail = Constants.MESSAGE_TYPE_PHOTO + "-" + fromName + "-" + sendTime
        // 按照比例压缩图片，制作成base64返回
        val baos = ByteArrayOutputStream()
        Thumbnails.of(ByteArrayInputStream(fileBytes)).size(80, 60).toOutputStream(baos)
        val theBytes = baos.toByteArray()
        baos.close()
        val base64Str = "data:image/jpeg;base64," + UtilService.bytesToBase64(theBytes)
        val photo = ChatPhoto()
        photo.id = idDetail
        photo.fileByte = fileBytes
        photo.extensionType = extensionType
        photo.fileName = fileName
        photo.fromName = fromName
        photo.toNames = toNames
        photo.sendTime = sendTime.toLong()
        chatPhotoRepository.save(photo)
        return base64Str
    }

    @Throws(IOException::class)
    override fun getChatFileLocation(idDetail: String, response: HttpServletResponse) {
        val words = idDetail.split("-").toTypedArray()
        val type = words[0]
        if (type == Constants.MESSAGE_TYPE_PHOTO) {
            val photo = chatPhotoRepository.findById(idDetail)
            if (photo.isPresent) {
                IOUtils.copy(ByteArrayInputStream(photo.get().fileByte), response.outputStream)
                response.contentType = photo.get().extensionType
            }
        } else if (type == Constants.MESSAGE_TYPE_FILE) {
            val file = chatFileRepository.findById(idDetail)
            if (file.isPresent) {
                IOUtils.copy(ByteArrayInputStream(file.get().fileByte), response.outputStream)
                response.contentType = file.get().extensionType
            }
        }
    }

    @Throws(IOException::class)
    override fun uploadGroupAvatarBase64(groupId: String, base64: String): ApiResult<*>? {
        val simpleUser = UserContextHolder.getUserContext()
        val userWid = simpleUser.wid

        val base64Bytes = UtilService.base64ToBytes(base64)
        val chatGroupPhoto = ChatGroupPhoto(id = groupId, updater = userWid, fileName = "$groupId.jpg", extensionType = "image/jpeg")
        chatGroupPhoto.fileByte = base64Bytes
        chatGroupPhotoRepository.save(chatGroupPhoto)
        return ResultUtil.success()
    }

    @Throws(IOException::class)
    override fun getAvatarGroup(id: String, response: HttpServletResponse) {
        val photo = chatGroupPhotoRepository.findById(id)
        if (photo.isPresent) {
            IOUtils.copy(ByteArrayInputStream(photo.get().fileByte), response.outputStream)
            response.contentType = photo.get().extensionType
        }
    }

}
