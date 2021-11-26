package com.myworld.schat.data.entity

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field
import java.io.Serializable

@Document(collection = "chat_file")
class ChatFile : Serializable {
    /**
     * PHOTO-fromName-sendTime
     */
    @Id
    @Field("_id")
    var id: String? = null
    @Field("file_name")
    var fileName: String? = null
    @Field("extension_type")
    var extensionType: String? = null
    @Field("file_byte")
    var fileByte: ByteArray? = null

    @Field("from_name")
    var fromName: String? = null
    /**
     * 收图片的，可以是1个人，也可以是多人，用逗号分隔
     */
    @Field("to_names")
    var toNames: String? = null
    @Field("send_time")
    var sendTime: Long? = null
}
