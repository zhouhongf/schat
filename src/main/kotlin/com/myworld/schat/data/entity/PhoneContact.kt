package com.myworld.schat.data.entity

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field
import java.io.Serializable

@Document(collection = "phone_contact")
class PhoneContact : Serializable {
    @Id
    @Field("_id")
    var id: String? = null

    // 所有者信息
    @Field("user_wid")
    var userWid: String? = null
    var tag: String? = null
    // 联系人信息，只记录联系人不会变的信息，联系人的phoneNumber, nickname, avatar都会有变化
    @Field("target_wid")
    var targetWid: String? = null
    // 联系人 displayName 由 所有者修改并保存
    @Field("display_name")
    var displayName: String? = null

}
