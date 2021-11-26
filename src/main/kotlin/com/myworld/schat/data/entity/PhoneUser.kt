package com.myworld.schat.data.entity

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field
import java.io.Serializable

@Document(collection = "phone_user")
data class PhoneUser(
    /**
     * wid即idDetail, 放在token中的
     */
    @Id
    @Field("_id")
    var wid: String,
    /**
     * 即手机号，昵称，头像
     */
    var username: String? = null,
    var nickname: String? = null,

    /**
     * 用户角色简单分类CLIENT, BANKER
     */
    @Field("player_type")
    var playerType: Set<*>? = null,
    /**
     * 如果是银行人员，则其提供的服务
     * WEALTH, LOANPERSON, LOANCOMPANY
     */
    var offer: String? = null
) : Serializable
