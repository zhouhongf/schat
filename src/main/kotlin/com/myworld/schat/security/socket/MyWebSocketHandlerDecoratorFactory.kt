package com.myworld.schat.security.socket

import com.myworld.schat.data.repository.ChatMessageGroupRepository
import com.myworld.schat.data.repository.ChatMessageRepository
import com.myworld.schat.service.ChatRedisService
import com.myworld.schat.service.UserRedisService
import org.apache.logging.log4j.LogManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.web.socket.*
import org.springframework.web.socket.handler.WebSocketHandlerDecorator
import org.springframework.web.socket.handler.WebSocketHandlerDecoratorFactory


/**
 * 装饰 WebSocketHandler
 * 装饰WebSocketHandlerDecorator对象，在连接建立时，保存websocket的session id；在连接断开时，从缓存中删除用户的sesionId值
 */
@Component
class MyWebSocketHandlerDecoratorFactory: WebSocketHandlerDecoratorFactory {
    private val log = LogManager.getRootLogger()

    // @Value("\${kafka.durable}")
    // private var durable: String = "myworld.kafka.durable"

    @Autowired
    private lateinit var userRedisService: UserRedisService
    @Autowired
    private lateinit var chatRedisService: ChatRedisService
    @Autowired
    private lateinit var chatMessageRepository: ChatMessageRepository
    @Autowired
    private lateinit var chatMessageGroupRepository: ChatMessageGroupRepository


    override fun decorate(handler: WebSocketHandler): WebSocketHandler {
        return object : WebSocketHandlerDecorator(handler) {
            @Throws(Exception::class)
            override fun afterConnectionEstablished(session: WebSocketSession) {
                session.binaryMessageSizeLimit = 819200
                session.textMessageSizeLimit = 81920
                val sessionId = session.id
                val principal = session.principal
                // 如果没有制作principal, 则此处显示为null
                log.info("afterConnectionEstablished方法: websocket用户上线: {}, sessionId是: {}", principal?.name, sessionId)
                // session.handshakeHeaders可以用来查验类似HttpHeader里面的内容
                super.afterConnectionEstablished(session)
            }

            @Throws(Exception::class)
            override fun afterConnectionClosed(session: WebSocketSession, closeStatus: CloseStatus) {
                val principal = session.principal
                if (principal != null) {
                    val name = principal.name
                    val sessionId = session.id
                    log.info("afterConnectionClosed方法，用户下线: {}, 其sessionId是: {}，删除redis中的用户sessionId数据", name, sessionId)
                    userRedisService.deleteUser(name)

                    // 处理单聊消息：redis中保存的chatMessage的mainKey格式都是根据chatMessage收件人userWid来命名的
                    val mainKey = "ack-$name"
                    val chatMessages = chatRedisService.listMessages(mainKey)
                    if (chatMessages != null) {
                        chatMessageRepository.saveAll(chatMessages)
                        chatRedisService.deleteByMainKey(mainKey)
                    }

                    // 处理群聊消息
                    val mainKeyGroup = "ackgroup-$name"
                    val messagesGroupAckTime = chatRedisService.listMessagesGroupAckTime(mainKeyGroup)
                    if (messagesGroupAckTime != null) {
                        messagesGroupAckTime.forEach {
                            val messageId = it.key
                            val userWidAckTime = it.value

                            val optional = chatMessageGroupRepository.findById(messageId)
                            if (optional.isPresent) {
                                val chatMessageGroup = optional.get()
                                val ackTimes = chatMessageGroup.ackTimes
                                ackTimes.add(userWidAckTime)
                                chatMessageGroup.ackTimes = ackTimes
                                chatMessageGroupRepository.save(chatMessageGroup)
                            }
                        }
                        chatRedisService.deleteByMainKey(mainKeyGroup)
                    }

                }
                super.afterConnectionClosed(session, closeStatus)
            }
        }
    }

}
