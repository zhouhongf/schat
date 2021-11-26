package com.myworld.schat.data.entity

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field
import java.io.Serializable

@Document(collection = "friend_apply")
class FriendApply : Serializable {
    @Id
    @Field("_id")
    var id: String? = null

    /**
     * 申请状态：等待验证，验证通过，拒绝通过
     */
    var status: String? = null
    @Field("user_wid")
    var userWid: String? = null
    @Field("user_display_name")
    var userDisplayName: String? = null
    @Field("user_phone_number")
    var userPhoneNumber: String? = null
    @Field("apply_content")
    var applyContent: String? = null
    @Field("apply_time")
    var applyTime: Long? = null


    @Field("target_wid")
    var targetWid: String? = null
    @Field("target_display_name")
    var targetDisplayName: String? = null
    @Field("target_phone_number")
    var targetPhoneNumber: String? = null
    @Field("target_remark_name")
    var targetRemarkName: String? = null
    @Field("target_tag_name")
    var targetTagName: String? = null
    @Field("target_content")
    var targetContent: String? = null
    @Field("confirm_time")
    var confirmTime: Long? = null

}
