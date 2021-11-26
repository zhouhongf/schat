package com.myworld.schat.data.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field
import java.io.Serializable
import java.util.*

@Document(collection = "show_blog")
data class ShowBlog(
    @Id
    @Field("_id")
    var id: String,

    @Field("user_wid")
    var userWid: String,
    var content: String = "",
    @Field("create_time")
    var createTime: Long = Date().time
) : Serializable, Comparable<ShowBlog> {

    @JsonIgnore
    @Field("open_to_wids")
    var openToWids: MutableSet<String>? = null

    @Field("show_photos")
    var showPhotos: MutableSet<String>? = null

    @Field("show_blog_comments")
    var showBlogComments: MutableSet<ShowBlogComment>? = null

    /**
     * 设置为升序排列，即小的在前，大的在后
     */
    override fun compareTo(other: ShowBlog): Int {
        return createTime.compareTo(other.createTime)
    }

}
