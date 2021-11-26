package com.myworld.schat.data.entity

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field
import java.io.Serializable
import java.util.*


@Document(collection = "chat_message")
class ChatMessage(
    /**
     * 单聊 id的格式如：未分类-ABC123456789-PAIR-TEXT-1580184911951
     * 群聊 id的格式如：groupId-ABC123456789-GROUP-TEXT-1580184911951
     * groupId的格式为：群创建人wid+GROUP+createTime，无“+”号
     */
    @Id
    @Field("_id")
    var id: String = "",
    var type: String = "",
    @Field("from_name")
    var fromName: String = "",
    @Field("to_name")
    var toName: String = "",
    var content: String? = null,
    var link: String? = null,
    @Field("send_time")
    var sendTime: Long = Date().time,

    @Field("ack_time")
    var ackTime: Long? = null
) : Serializable, Comparable<ChatMessage> {

    /**
     * 设置为升序排列，即小的在前，大的在后
     */
    override fun compareTo(other: ChatMessage): Int {
        return sendTime.compareTo(other.sendTime)
    }

}
