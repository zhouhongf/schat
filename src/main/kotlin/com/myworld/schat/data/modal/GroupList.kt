package com.myworld.schat.data.modal


data class GroupList(
    var id: String = "",
    var nickname: String = "",

    var wids: MutableSet<String> = HashSet(),
    var memberNames: MutableSet<String> = HashSet(),

    var displayName: String = "",
    var showMemberNickname: Boolean = false,
    var unreadMessageNum: Int = 0
)
