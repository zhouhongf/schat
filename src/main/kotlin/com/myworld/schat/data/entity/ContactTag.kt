package com.myworld.schat.data.entity

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field
import java.io.Serializable
import java.util.*

@Document(collection = "contact_tag")
class ContactTag(
    @Id
    @Field("_id")
    var id: String,

    @Field("user_wid")
    var userWid: String? = null,
    var username: String? = null,
    var name: String? = null,
    var nums: Int? = null,
    @Field("order_class")
    var orderClass: Int? = null,


    @Field("create_time")
    var createTime: Long = Date().time,
    @Field("update_time")
    var updateTime: Long = Date().time
) : Serializable
