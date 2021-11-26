package com.myworld.schat.data.entity

import java.io.Serializable
import java.util.*


class ShowBlogComment(
    val id: String,
    val idAt: String,
    val comment: String,
    val commenterWid: String,
    val commentAtWid: String,
    val createTime: Long = Date().time
) : Serializable
