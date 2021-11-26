package com.myworld.schat.service.impl

import com.myworld.schat.common.ApiResult
import com.myworld.schat.common.ResultUtil
import com.myworld.schat.common.UserContextHolder
import com.myworld.schat.data.entity.ChatMessage
import com.myworld.schat.data.entity.ChatMessageGroup
import com.myworld.schat.data.modal.Constants
import com.myworld.schat.data.repository.ChatGroupRepository
import com.myworld.schat.data.repository.ChatMessageGroupRepository
import com.myworld.schat.data.repository.ChatMessageRepository
import com.myworld.schat.data.repository.PhoneContactRepository
import com.myworld.schat.service.ChatRedisService
import com.myworld.schat.service.ChatService
import com.myworld.schat.service.UserRedisService
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.logging.log4j.LogManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.Acknowledgment
import org.springframework.kafka.support.serializer.JsonDeserializer
import org.springframework.messaging.MessageHeaders
import org.springframework.messaging.simp.SimpMessageHeaderAccessor
import org.springframework.messaging.simp.SimpMessageType
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.stereotype.Service
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashSet

@Service
class ChatServiceImpl : ChatService {
    private val log = LogManager.getRootLogger()
    @Autowired
    private lateinit var kafkaTemplate: KafkaTemplate<String, Any>
    @Autowired
    private lateinit var simpMessagingTemplate: SimpMessagingTemplate
    @Autowired
    private lateinit var userRedisService: UserRedisService
    @Autowired
    private lateinit var chatRedisService: ChatRedisService
    @Autowired
    private lateinit var phoneContactRepository: PhoneContactRepository
    @Autowired
    private lateinit var mongoTemplate: MongoTemplate
    @Autowired
    private lateinit var chatMessageRepository: ChatMessageRepository
    @Autowired
    private lateinit var chatGroupRepository: ChatGroupRepository
    @Autowired
    private lateinit var chatMessageGroupRepository: ChatMessageGroupRepository


    @Value("\${kafka.bootstrapserver}")
    private var bootstrapserver = "localhost:9092"
    @Value("\${kafka.groupid}")
    private var groupid = "group-kafka"
    @Value("\${kafka.chatpair}")
    private var chatpair = "myworld.kafka.chatpair"
    @Value("\${kafka.chatgroup}")
    private var chatgroup = "myworld.kafka.chatgroup"
    @Value("\${kafka.chatall}")
    private var chatall = "myworld.kafka.chatall"
    @Value("\${kafka.durable}")
    private var durable = "myworld.kafka.durable"


    override fun createHeaders(sessionId: String): MessageHeaders? {
        val headerAccessor = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE)
        headerAccessor.sessionId = sessionId
        headerAccessor.setLeaveMutable(true)
        return headerAccessor.messageHeaders
    }

    /**
     * 如果消息的接收方offline，则将消息重新编写messageId后，储存在redis中
     * fromName即fromUserIdDetail, toName即toUserIdDetail
     * 格式如：未分类-MYUSER15342193174008888-PAIR-TEXT-1580394051378
     */
    override fun makeMessageId(fromName: String, toName: String, time: Long): String {
        // PhoneContact为消息接收方 对 消息发送方 储存的实例，toTag为消息接收方 对 消息发送方 的标签分类
        val phoneContact = phoneContactRepository.findByUserWidAndTargetWid(toName, fromName)
        val toTag = phoneContact!!.tag
        return toTag + "-" + fromName + "-" + Constants.MESSAGE_PAIR_PREFIX + "-" + Constants.MESSAGE_TYPE_TEXT + "-" + time
    }

    /**
     * 操作chatMessage, redis数据库中存储的key有3类：offline-toName, online-toName, ack-toName
     * （1）webSocket消息到达服务器后，如果收消息人offline, 则将消息id通过makeMessageId()方法重新编辑，放在key为offline-toName的redis数据库中储存
     * （2）如果收消息人online, 则消息通过kafkaTemplate发送
     * 收消息人通过订阅“/queue/name/发消息人wid”这个地址，来接收消息
     * 消息收到后，将该消息保存在key为online-toName的redis数据库中
     * （3）通过MyChannelInterceptor类的postSend()方法，监听StompCommand.ACK响应，
     * ack后的消息，通过查询key为online-toName的redis数据，重新保存在key为ack-toName的redis数据中，并删除原来的数据
     * （4）用户下线，连接取消后，通过MyWebSocketHandlerDecoratorFactory类的afterConnectionClosed()方法，进行最后的清理工作
     * 根据ack-toName, 即“ack-该用户的wid”, 来在redis数据库中查询该用户累计ack的消息
     * 因为只有ack后的消息，才是完整的ChatMessage
     * （5）将消息通过kafkaTemplate发送到kafka的durable主题中去，kafka收到消息后，根据messageId，删除redis数据库中key为ack-toName的消息
     * （6）如果发出去的消息，一直都是offline-toName，并且收消息人一直都没有ack，那么发消息人即使下线，该消息在redis数据库中也不会删除
     */
    override fun sendChatpair(chatMessage: ChatMessage, headerAccessor: StompHeaderAccessor) {
        val sendTime = chatMessage.sendTime
        val fromName = chatMessage.fromName
        val toName = chatMessage.toName

        // 检查 收件人是否在线，以及收件人订阅的消息监听频道中，是否有订阅我，即fromName，如果有，则可以发送消息
        val subscribeIds = userRedisService.checkReceiverSessionIdAndSubscribeIds(toName)
        if (subscribeIds != null && fromName in subscribeIds) {
            val topicKey = "chatpair-$fromName-$toName"
            kafkaTemplate.send(chatpair, topicKey, chatMessage)
        } else {
            log.info("用户还没有上线，重新制作messageId保存在redis中")
            val messageId = this.makeMessageId(fromName, toName, sendTime)
            chatMessage.id = messageId
            val key = "offline-$toName"
            chatRedisService.addMessage(key, chatMessage)
        }
    }

    override fun sendChatgroup(chatMessage: ChatMessage, headerAccessor: StompHeaderAccessor) {
        log.info("收到chatMessage群聊消息: {}", chatMessage)
        val fromName = chatMessage.fromName
        val groupId = chatMessage.toName

        val chatGroup = chatGroupRepository.findById(groupId)
        if (chatGroup.isPresent) {
            val wids: MutableSet<String> = chatGroup.get().wids
            saveChatMessageGroup(chatMessage, wids)
            // 按照chatGroup中取出的群成员wids, 将message发送出去
            for (wid in wids) {
                if (wid != fromName) {
                    chatMessage.toName = wid                                    // 将chatMessage的toName由groupId转换为每个群成员的wid

                    val subscribeIds = userRedisService.checkReceiverSessionIdAndSubscribeIds(wid)
                    if (subscribeIds != null && groupId in subscribeIds) {
                        val topicKey = "chatgroup-$fromName-$wid"               // topicKey最后一个，是写wid还是写groupId? 使用wid可以在kafka的Partition中Key这列，有效的区分是谁给谁发的消息
                        kafkaTemplate.send(chatgroup, topicKey, chatMessage)
                    } else {
                        val key = "offline-$wid"
                        chatRedisService.addMessage(key, chatMessage)
                    }
                }
            }
        }
    }

    override fun saveChatMessageGroup(chatMessage: ChatMessage, wids: MutableSet<String>) {
        val fromName = chatMessage.fromName
        val groupId = chatMessage.toName
        val sendTime = chatMessage.sendTime

        // 将chatMessage转换成chatMessageGroup保存进数据库,
        // messageId不变，fromName由发件人wid变为groupId，toName由groupId变为群成员wids
        val ackTimes : MutableSet<String> = HashSet()
        ackTimes.add("$fromName=$sendTime")

        val chatMessageGroup = ChatMessageGroup(
            id = chatMessage.id,
            fromName = groupId,
            toNames = wids,
            sendTime = chatMessage.sendTime,
            ackTimes = ackTimes,
            type = chatMessage.type,
            content = chatMessage.content,
            link = chatMessage.link
        )
        chatMessageGroupRepository.save(chatMessageGroup)
    }


    /**
     * 该方法中也可以使用List<ChatMessage>作为参数
     * Kafka是通过最新保存偏移量进行消息消费的，而且确认消费的消息并不会立刻删除，
     * 所以我们可以重复的消费未被删除的数据，
     * 当第一条消息未被确认，而第二条消息被确认的时候，Kafka会保存第二条消息的偏移量，
     * 也就是说第一条消息再也不会被监听器所获取，除非是根据第一条消息的偏移量手动获取。
     * 使用Kafka的Ack机制只需简单的三步：1、设置ENABLE_AUTO_COMMIT_CONFIG=false， 2、设置AckMode=MANUAL_IMMEDIATE， 3、监听方法加入Acknowledgment ack 参数
     * 怎么拒绝消息呢，只要在监听方法中不调用ack.acknowledge()即可
     * 没办法重复消费未被Ack的消息，解决办法有2个：
     * （1）重新将消息发送到队列中，这种方式比较简单而且可以使用Headers实现第几次消费的功能，用以下次判断
     * （2）使用Consumer.seek方法，重新回到该未ack消息偏移量的位置重新消费，这种可能会导致死循环，原因出现于业务一直没办法处理这条数据，但还是不停的重新定位到该数据的偏移量上。
     */
    @Throws(Exception::class)
    @KafkaListener(id = "chatpair", clientIdPrefix = "chatpair", topics = ["\${kafka.chatpair}"], containerFactory = "kafkaListenerContainerFactory", errorHandler = "consumerAwareErrorHandler")
    override fun receiveChatpair(records: List<ConsumerRecord<*, *>>, ack: Acknowledgment) {
        log.info("收到kafka单聊消息并准备发送: {}", records)
        for (record in records) {
            val chatMessage = record.value() as ChatMessage
            val sessionId = userRedisService.getReceiverSessionId(chatMessage.toName)
            if (sessionId != null) {
                val destinationUrl = "/queue/name/" + chatMessage.fromName                      // 作为前端用户是收消息，故fromName即前端的friendWid
                simpMessagingTemplate.convertAndSendToUser(sessionId, destinationUrl, chatMessage, createHeaders(sessionId))
                ack.acknowledge()
                val key = "online-" + chatMessage.toName
                chatRedisService.addMessage(key, chatMessage)
            } else {
                log.info("==================================== 错误!!, 单聊，未能找到收件人sessionId，消息作废：{}", chatMessage.content)
            }

        }
    }

    @Throws(Exception::class)
    @KafkaListener(id = "chatgroup", clientIdPrefix = "chatgroup", topics = ["\${kafka.chatgroup}"], containerFactory = "kafkaListenerContainerFactory", errorHandler = "consumerAwareErrorHandler")
    override fun receiveChatgroup(chatMessages: List<ChatMessage>, ack: Acknowledgment) {
        log.info("收到kafka群聊消息并准备发送: {}", chatMessages)
        for (chatMessage in chatMessages) {
            val sessionId = userRedisService.getReceiverSessionId(chatMessage.toName)
            if (sessionId != null) {
                val words = chatMessage.id.split("-")
                val groupId = words[0]
                val destinationUrl = "/group/name/$groupId"
                simpMessagingTemplate.convertAndSendToUser(sessionId, destinationUrl, chatMessage, createHeaders(sessionId))
                ack.acknowledge()
                val key = "online-" + chatMessage.toName
                chatRedisService.addMessage(key, chatMessage)
            } else {
                log.info("==================================== 错误!!, 群聊，未能找到收件人sessionId，消息作废：{}", chatMessage.content)
            }

        }
    }



    @KafkaListener(id = "durable", clientIdPrefix = "durable", topics = ["\${kafka.durable}"], containerFactory = "kafkaListenerContainerFactory", errorHandler = "consumerAwareErrorHandler")
    fun receiveDurable(chatMessages: List<ChatMessage>, ack: Acknowledgment) {
        log.info("ChatServiceImpl收到需要持久化的消息： {}", chatMessages)
        for (chatMessage in chatMessages) {
            ack.acknowledge()
            val mainKey = "ack-" + chatMessage.toName
            chatRedisService.deleteMessage(mainKey, chatMessage.id)
        }
        this.chatMessageRepository.saveAll(chatMessages)
    }

    /**
     * 定时进行数据持久化操作
     */
    // @Scheduled(cron = "0 35 22 * * ?")
    fun saveToDB() {
        log.info("【开始执行saveToDB方法】")
        val properties = Properties()
        properties[ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG] = bootstrapserver
        properties[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
        properties[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = JsonDeserializer::class.java
        properties[ConsumerConfig.GROUP_ID_CONFIG] = groupid
        properties[ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG] = "false"
        properties[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "earliest"
        val deserializer = JsonDeserializer<Any>()
        deserializer.addTrustedPackages("com.myworld.mygate.kafka.data.entity")
        val consumer = KafkaConsumer(properties, StringDeserializer(), deserializer)
        consumer.subscribe(listOf(durable))

        val minBatchSize = 500
        val buffer: MutableList<ConsumerRecord<String, Any>> = ArrayList()
        try {
            val records = consumer.poll(1000)
            for (record in records) {
                log.info("定时任务中获取的内容是:{}", record)
                buffer.add(record)
            }
            if (buffer.size >= minBatchSize) {
                log.info("已累计满500条记录，可以存入数据库了")
                consumer.commitSync()
                buffer.clear()
            }
        } catch (e: Exception) {
            log.info(e.toString());
        } finally {
            consumer.close()
        }
    }

    override fun getChatMessagesPair(pageSize: Int, startNumber: Long, friendWid: String): ApiResult<*>? {
        val simpleUser = UserContextHolder.getUserContext()
        val userWid = simpleUser.wid

        val query = Query()
        query.addCriteria(
            Criteria().orOperator(
                Criteria().andOperator(Criteria.where("from_name").`is`(userWid), Criteria.where("to_name").`is`(friendWid)),
                Criteria().andOperator(Criteria.where("from_name").`is`(friendWid), Criteria.where("to_name").`is`(userWid))
            )
        )

        val chatMessages: MutableList<ChatMessage> = mongoTemplate.find(query.with(Sort(Sort.Direction.DESC, "send_time")).skip(startNumber).limit(pageSize), ChatMessage::class.java)
        if (chatMessages.isEmpty()) {
            return ResultUtil.failure(-2, "没有历史聊天数据")
        }
        return ResultUtil.success(data = chatMessages)
    }


    override fun getChatMessagesGroup(pageSize: Int, startNumber: Long, groupId: String): ApiResult<*>? {
        val simpleUser = UserContextHolder.getUserContext()
        val userWid = simpleUser.wid

        // 在MongoDB中的查询条件是：(1)发件人是groupId, (2)群成员收件人中含有该用户的userWid
        // 因为有可能该用户是后来才加入的，因此群里面前面的消息，他是不能看到的。
        val query = Query()
        query.addCriteria(Criteria.where("from_name").`is`(groupId))
        query.addCriteria(Criteria.where("to_names").all(userWid))

        val chatMessageGroups: MutableList<ChatMessageGroup> = mongoTemplate.find(query.with(Sort(Sort.Direction.DESC, "send_time")).skip(startNumber).limit(pageSize), ChatMessageGroup::class.java)
        if (chatMessageGroups.isEmpty()) {
            return ResultUtil.failure(-2, "没有历史聊天数据")
        }

        // 然后再将chatGroupMessage转换为chatMessage返回给前端
        // 非常重要，注意返回的chatMessage的格式！！
        // 本人发的消息， fromName是userWid, toName是groupId,
        // 其他人发的消息，fromName是其他人的userWid, toName是本人的userWid
        val chatMessages : MutableList<ChatMessage> = ArrayList()
        for (chatMessageGroup in chatMessageGroups) {
            val messageId = chatMessageGroup.id
            val sender = messageId.split("-")[1]
            val chatMessage = ChatMessage(fromName = sender, sendTime = chatMessageGroup.sendTime, content = chatMessageGroup.content, type = chatMessageGroup.type, link = chatMessageGroup.link)
            if (sender == userWid) {
                chatMessage.toName = groupId
            } else {
                chatMessage.toName = userWid
            }
            chatMessages.add(chatMessage)
        }

        return ResultUtil.success(data = chatMessages)

    }
}
