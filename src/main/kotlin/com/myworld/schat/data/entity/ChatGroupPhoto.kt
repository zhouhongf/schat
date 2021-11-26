package com.myworld.schat.data.entity

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field
import java.io.Serializable

// id就是chatGroup的Id, 因为一个chatGroup只能有一个头像

@Document(collection = "chat_group_photo")
data class ChatGroupPhoto(
    @Id
    @Field("_id")
    var id: String,
    var updater: String,
    @Field("file_name")
    var fileName: String?,
    @Field("extension_type")
    var extensionType: String?
) : Serializable {
    @Field("file_byte")
    var fileByte: ByteArray? = null
}
