package com.myworld.schat.data.modal

interface Constants {
    companion object {
        const val APP_CHAT = "schat"
        const val QUEUE_PRE = "jingrong-user"
        const val HEADER_AUTH = "Authorization"
        const val CHAT_ID = "wid"
        const val HOST_AUTH = "http://122.114.50.172:9999"
        // String HOST_AUTH = "http://localhost:9999";
        // String HOST_AUTH = "http://192.168.1.102:9999";
        const val MESSAGE_PAIR_PREFIX = "PAIR"
        const val MESSAGE_GROUP_PREFIX = "GROUP"
        const val MESSAGE_ALL_PREFIX = "ALL"

        const val MESSAGE_TYPE_TEXT = "TEXT"
        const val MESSAGE_TYPE_PHOTO = "PHOTO"
        const val MESSAGE_TYPE_FILE = "FILE"
        const val CONTACT_TAG_PREFIX = "TAG"
        const val FRIEND_APPLY_PREFIX = "FDAPPLY"
        const val FRIEND_PREFIX = "FRIEND"

        const val BLOG_SHOW_PREFIX = "BLOGSHOW"
        const val BLOG_SHOW_PHOTO = "PHOTOSHOW"
        const val BLOG_SHOW_COMMENT = "BLOGCOMMENT"
    }
}
