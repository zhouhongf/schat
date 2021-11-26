package com.myworld.schat.common

import java.io.Serializable


data class ApiResult<T>(
    val code: Int,
    val msg: String,
    val num: Long? = null,
    val data: T? = null
)

enum class ApiResultEnum(val code: Int, val msg: String) {
    // 枚举成员的构造方法，同枚举类的构造方法
    FAILURE(-2, "失败"),
    ERROR(-1, "未知错误"),
    SUCCESS(0, "成功"),
    UPDATE(9, "更新令牌");
}

data class SimpleUser(
    var wid: String = "",
    var token: String? = null,
    var username: String? = null,
    var nickname: String? = null,
    var playerType: Set<*>? = null,
    var offer: String? = null
) : Serializable

