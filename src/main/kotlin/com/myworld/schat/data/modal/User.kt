package com.myworld.schat.data.modal

import java.security.Principal


class User(private val name: String) : Principal {

    // 该name设定为userIdDetail
    override fun getName(): String {
        return name
    }

}
