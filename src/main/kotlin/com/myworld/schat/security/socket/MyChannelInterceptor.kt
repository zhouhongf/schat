package com.myworld.schat.security.socket

import com.myworld.schat.data.modal.Constants
import com.myworld.schat.data.modal.MessageAck
import com.myworld.schat.data.modal.User
import com.myworld.schat.service.ChatRedisService
import com.myworld.schat.service.UserRedisService
import org.apache.logging.log4j.LogManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.messaging.support.ChannelInterceptor
import org.springframework.messaging.support.MessageHeaderAccessor
import org.springframework.stereotype.Component


/**
 * 可以在Message对象在发送到MessageChannel前后查看修改此值，也可以在MessageChannel接收MessageChannel对象前后修改此值
 * 在此拦截器中使用StompHeaderAccessor 或 SimpMessageHeaderAccessor访问消息
 */
@Component
class MyChannelInterceptor : ChannelInterceptor {
    private val log = LogManager.getRootLogger()

    @Value("\${kafka.chatpair}")
    private var chatpair: String = "myworld.kafka.chatpair"
    @Value("\${kafka.chatgroup}")
    private var chatgroup: String = "myworld.kafka.chatgroup"

    @Autowired
    private lateinit var userRedisService: UserRedisService
    @Autowired
    private lateinit var chatRedisService: ChatRedisService
    @Autowired
    private lateinit var kafkaTemplate: KafkaTemplate<String, Any>

    /**
     * offline消息，操作：我已上线，打开了和一个朋友的会话，我要查看该朋友之前有没有在我不在线时，发消息给我
     * 单聊，格式：未分类-MYUSER15342193174008888-PAIR-TEXT-1580394051378
     * 群聊，格式：groupId-MYUSER15342193174008888-GROUP-TEXT-1580394051378
     * groupId，格式：creatorWid+GROUP+1580394051378
     * fromWid是发件人，userWid是收件人
     */
    private fun checkOfflineMessage(fromWid: String, userWid: String) {
        val mainKey = "offline-$userWid"
        val chatMessages = chatRedisService.listMessages(mainKey)           // 列出我不在线时，所有朋友发给我的离线消息
        if (chatMessages != null && chatMessages.isNotEmpty()) {
            log.info("【有offline消息：{}】", chatMessages)
            for (chatMessage in chatMessages) {
                val messageId = chatMessage.id
                val keys = messageId.split("-")
                val tagOrGroup = keys[0]                // Tag名称，或者groupId
                val fromWidInMessageId = keys[1]
                val chatType = keys[2]
                val messageType = keys[3]
                val sendTime = keys[4]

                if (chatType == Constants.MESSAGE_PAIR_PREFIX && fromWidInMessageId == fromWid) {           // 如果是单聊消息，并且消息的发件人，就是当前聊天会话的发件人，则发送这部分offline消息给userWid
                    // 根据用户分类的好友标签，取出offline消息后，重新修改为原来的messageId，然后发送; 前面添加标签tag后保存为offline消息，是为了方便查询所在tag有多少条未读消息
                    val messageIdPair = fromWid + "-" + Constants.MESSAGE_PAIR_PREFIX + "-" + messageType + "-" + sendTime
                    chatMessage.id = messageIdPair
                    val topicKey = "chatpair-" + chatMessage.fromName + "-" + chatMessage.toName
                    // 发送消息，并在redis中删除该条消息
                    kafkaTemplate.send(chatpair, topicKey, chatMessage)
                    chatRedisService.deleteMessage(mainKey, messageId)
                } else if (chatType == Constants.MESSAGE_GROUP_PREFIX && tagOrGroup == fromWid) {          // 如果是群聊消息，并且消息id的开头为当前群聊的groupId的，则将消息发送给userWid
                    // 此处的chatMessage.toName, 已经是修改过后的每个群成员的wid了，不再是groupId了
                    val topicKey = "chatgroup-" + chatMessage.fromName + "-" + chatMessage.toName
                    kafkaTemplate.send(chatgroup, topicKey, chatMessage)
                    // 发送消息，并在redis中删除该条未发消息的记录，群聊消息在发送时，已经保存在MongoDB数据库中了，此处redis保存的，只是记录消息发给哪些群成员
                    chatRedisService.deleteMessage(mainKey, messageId)
                }

            }
        }
    }

    /**
     * 用户上线后
     * 群聊：
     * interceptors-> CONNECT-StompHeaderAccessor [headers={simpMessageType=CONNECT, stompCommand=CONNECT, nativeHeaders={login=[MYUSER15342193174008888], passcode=[PROTECTED], accept-version=[1.0,1.1,1.2], heart-beat=[20000,0]}, simpSessionAttributes={wid=MYUSER15342193174008888}, simpHeartbeat=[J@7d1070c0, stompCredentials=[PROTECTED], simpUser=com.myworld.schat.data.modal.User@9415a0f, simpSessionId=3c9b8dc1-45fb-e90a-817d-e7a6ce2c3a93}]
     * interceptors-> SUBSCRIBE-StompHeaderAccessor [headers={simpMessageType=SUBSCRIBE, stompCommand=SUBSCRIBE, nativeHeaders={ack=[auto], id=[sub-0], destination=[/user/group/name/MYUSER15342193174008888GROUP1580352212190]}, simpSessionAttributes={wid=MYUSER15342193174008888}, simpHeartbeat=[J@598f09d7, simpSubscriptionId=sub-0, simpUser=com.myworld.schat.data.modal.User@50d7183e, simpSessionId=3c9b8dc1-45fb-e90a-817d-e7a6ce2c3a93, simpDestination=/user/group/name/MYUSER15342193174008888GROUP1580352212190}]
     * interceptors-> SEND-StompHeaderAccessor [headers={simpMessageType=MESSAGE, stompCommand=SEND, nativeHeaders={destination=[/app/sendChatgroup], content-length=[263]}, simpSessionAttributes={wid=MYUSER15342193174008888}, simpHeartbeat=[J@31dc31cb, simpUser=com.myworld.schat.data.modal.User@50d7183e, simpSessionId=3c9b8dc1-45fb-e90a-817d-e7a6ce2c3a93, simpDestination=/app/sendChatgroup}]
     *
     * 单聊：
     * interceptors-> CONNECT-StompHeaderAccessor [headers={simpMessageType=CONNECT, stompCommand=CONNECT, nativeHeaders={login=[MYUSER15342193174008888], passcode=[PROTECTED], accept-version=[1.0,1.1,1.2], heart-beat=[20000,0]}, simpSessionAttributes={wid=MYUSER15342193174008888}, simpHeartbeat=[J@68bd5141, stompCredentials=[PROTECTED], simpUser=com.myworld.schat.data.modal.User@7a715edb, simpSessionId=3e90aa2b-39b1-e78e-8d24-330e0859f103}]
     * interceptors-> SUBSCRIBE-StompHeaderAccessor [headers={simpMessageType=SUBSCRIBE, stompCommand=SUBSCRIBE, nativeHeaders={ack=[auto], id=[sub-0], destination=[/user/queue/name/MYUSER15547704317619000]}, simpSessionAttributes={wid=MYUSER15342193174008888}, simpHeartbeat=[J@159003b0, simpSubscriptionId=sub-0, simpUser=com.myworld.schat.data.modal.User@6b028b9a, simpSessionId=3e90aa2b-39b1-e78e-8d24-330e0859f103, simpDestination=/user/queue/name/MYUSER15547704317619000}]
     * interceptors-> SEND-StompHeaderAccessor [headers={simpMessageType=MESSAGE, stompCommand=SEND, nativeHeaders={destination=[/app/sendChatpair], content-length=[194]}, simpSessionAttributes={wid=MYUSER15342193174008888}, simpHeartbeat=[J@3792caf2, simpUser=com.myworld.schat.data.modal.User@6b028b9a, simpSessionId=3e90aa2b-39b1-e78e-8d24-330e0859f103, simpDestination=/app/sendChatpair}]
     *
     * 用户下线
     * interceptors-> DISCONNECT-StompHeaderAccessor [headers={simpMessageType=DISCONNECT, stompCommand=DISCONNECT, nativeHeaders={receipt=[close-1]}, simpSessionAttributes={wid=MYUSER15342193174008888}, simpHeartbeat=[J@7ffa08cd, simpUser=com.myworld.schat.data.modal.User@6b028b9a, simpSessionId=3e90aa2b-39b1-e78e-8d24-330e0859f103}]
     * interceptors-> DISCONNECT-StompHeaderAccessor [headers={simpMessageType=DISCONNECT, stompCommand=DISCONNECT, simpSessionAttributes={wid=MYUSER15342193174008888}, simpUser=com.myworld.schat.data.modal.User@6b028b9a, simpSessionId=3e90aa2b-39b1-e78e-8d24-330e0859f103}]
     *
     *
     * 以下是没有将userWid放入httpSession在MyHandshakeInterceptor和MyHandshakeHandler中制作principle情况下的链接状态
     * MyChannelInterceptor-> CONNECT-StompHeaderAccessor [headers={simpMessageType=CONNECT, stompCommand=CONNECT, nativeHeaders={login=[MYUSER15342193174008888], passcode=[PROTECTED], accept-version=[1.0,1.1,1.2], heart-beat=[20000,0]}, simpSessionAttributes={}, simpHeartbeat=[J@53eb7d7f, stompCredentials=[PROTECTED], simpSessionId=ed266946-8cfa-e349-172f-fc17b45add30}]
     * MyChannelInterceptor-> SUBSCRIBE-StompHeaderAccessor [headers={simpMessageType=SUBSCRIBE, stompCommand=SUBSCRIBE, nativeHeaders={ack=[auto], id=[sub-0], destination=[/user/group/name/MYUSER15342193174008888GROUP1580352212190]}, simpSessionAttributes={}, simpHeartbeat=[J@628d41d0, simpSubscriptionId=sub-0, simpSessionId=ed266946-8cfa-e349-172f-fc17b45add30, simpDestination=/user/group/name/MYUSER15342193174008888GROUP1580352212190}]
     *
     */
    override fun preSend(message: Message<*>, channel: MessageChannel): Message<*> {
        val accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor::class.java)
        when(accessor!!.command) {
            StompCommand.CONNECT -> {
                val userWid = accessor.login
                val sessionId = accessor.sessionId
                val user = accessor.user as User
                if (userWid == user.name) {
                    log.info("userWid验证一致, 为：{}， sessionId为：{}, 添加进redis储存", userWid, sessionId)
                    userRedisService.addUser(userWid, sessionId, null)
                }
            }

            StompCommand.SUBSCRIBE -> {
                val user = accessor.user as User
                val userWid = user.name
                val sessionId = accessor.sessionId

                // 群聊，格式：/user/group/name/MYUSER15342193174008888GROUP1580352212190；
                // 单聊，格式：/user/queue/name/MYUSER15547704317619000
                val destinationUrl = accessor.destination
                val words = destinationUrl!!.split("/").toTypedArray()
                val theIndex = words.size - 1
                val friendWid = words[theIndex]         // 如果是groupMessage, 则对应的是groupId

                // 可以处理同一个sessionId, 用户即订阅群聊，也可以订阅单聊，或者订阅多个聊天
                // 只要把订阅的friendId或groupId放入该sessionId为key, value为set集合的集合中即可
                log.info("userWid：{}, sessionId：{}, 订阅的subscribeId: {}，更新redis储存", userWid, sessionId, friendWid)
                userRedisService.addUser(userWid, sessionId, friendWid)

                // friendWid是发件人的userWid，name是当前用户、收件人; 如果是群聊，则friendWid是groupId
                checkOfflineMessage(friendWid, userWid)
            }
            else -> println("MyChannelInterceptor的preSend方法-> $accessor")
        }
        return message
    }

    override fun postSend(message: Message<*>, channel: MessageChannel, sent: Boolean) {
        val accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor::class.java)
        if (accessor!!.command == StompCommand.ACK) {
            log.info("用户确认收到消息，并从前端获取ack的内容，accessor为：{}", accessor)
            val receiver = accessor.sessionAttributes[Constants.CHAT_ID] as String?
            val sender = accessor.getNativeHeader("sender")[0]
            val messageId = accessor.getNativeHeader("messageId")[0]
            val ackTimeStr = accessor.getNativeHeader("ackTime")[0]
            if (receiver != null && messageId != null && ackTimeStr != null) {
                val messageAck = MessageAck(messageId, sender, receiver, ackTimeStr.toLong())
                val key = "online-$receiver"
                chatRedisService.updateMessageAck(key, messageAck)
            }
        }
    }

}
