package com.myworld.schat.data.entity

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field
import java.io.Serializable
import java.util.*
import kotlin.collections.HashSet

// id格式：创建人wid+GROUP+createTime
// creator为：创建人wid，拥有群主的管理权限

// nickname为创建人创建群时设置，作为id的辅助，供后续申请加入群的用户查询添加使用
// 创建人编辑群信息，群昵称，显示的是nickname, 修改nickname, 即同时修改GroupList的displayName

// wids为群聊时，供服务器发送消息使用，格式为：{wid1,wid2,wid3,}

// ========================================================================================
//                      群成员要素：wid, 头像, 昵称，在本群昵称
// ========================================================================================

// displayNames为群成员后续设置的群名称，供群成员自己分别查询使用
// displayNames格式为：{wid1=groupDisplayName1, wid2=groupDisplayName2,}
// 群成员修改群昵称，即只修改displayNames集合中该成员wid所对应的displayName


// 群成员，显示其他群成员昵称，或本群昵称的控制开关，showMemberNicknames的格式为：{wid1=false, wid2=false,}

// 默认不显示成员的全局名称，默认显示成员修改过后的在本群显示的名称，用户未设置自己在本群的名称，则默认显示最初添加用户时的该用户的nickname
// memberNames为群成员修改的显示名称，格式为：{wid1=displayName1, wid2=displayName2,}
// 在数据库中储存的是以上格式，每次群成员，修改自己在本群的昵称，即通过群ID和memberNames中的用户wid，来定位修改


// 群成员信息，不关联用户本地储存的手机通讯录
// 添加群成员的时候，上传至服务器的拟添加的群成员信息格式为：{wid1=displayName1, wid2=displayName2,}，即成员在操作人手机上显示的名称
// 所以操作人在手机上修改某个成员的备注名称，需要同时同步群聊中的该成员的displayName

// 群成员修改在本群显示的名称，即修改该displayName, 修改过后，如果其他群成员，不使用showMemberNickname=true的话，就显示该名称
// 显不显示 成员真实备注名称，取决于操作人，在将群成员数据返回给操作人前端后，
// 如果是showMemberNickname=true，则在操作人前端本地储存的通讯录中，将displayName全部替换为操作人备注的成员名称


// 单独设置一个wids: MutableSet<String>的目的是为了方便每次获取groupList, 在MongoDB中，只要包含用户的wid, 即可提取出该chatGroup



@Document(collection = "chat_group")
data class ChatGroup(
    @Id
    @Field("_id")
    var id: String = "",
    var creator: String = "",
    var nickname: String = "",

    var updater: String = "",
    @Field("update_time")
    var updateTime: Long = Date().time,

    var wids: MutableSet<String> = HashSet(),

    @Field("display_names")
    var displayNames: MutableSet<String> = HashSet(),


    @Field("show_member_nicknames")
    var showMemberNicknames: MutableSet<String> = HashSet(),
    @Field("member_names")
    var memberNames: MutableSet<String> = HashSet()
) : Serializable

