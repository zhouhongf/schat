package com.myworld.schat.common

object UserContextHolder {
    var userContext = ThreadLocal<SimpleUser>()

    @JvmStatic
    fun getUserContext(): SimpleUser {
        return userContext.get()
    }

    @JvmStatic
    fun setUserContext(simpleUser: SimpleUser) {
        userContext.set(simpleUser)
    }

    @JvmStatic
    fun shutdown() {
        userContext.remove()
    }
}
