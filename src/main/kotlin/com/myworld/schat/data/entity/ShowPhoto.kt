package com.myworld.schat.data.entity

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field
import java.io.Serializable
import java.util.*

@Document(collection = "show_photo")
class ShowPhoto(
    @Id
    @Field("_id")
    var id: String,
    @Field("file_name")
    var fileName: String,
    @Field("extension_type")
    var extensionType: String,

    @Field("user_wid")
    var userWid: String,
    @Field("blog_id_detail")
    var blogIdDetail: String,

    @Field("file_byte")
    var fileByte: ByteArray,

    @Field("create_time")
    var createTime: Long = Date().time
) : Serializable
