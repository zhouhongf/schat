package com.myworld.schat.config

import com.myworld.schat.security.kafka.MyKafkaPartitioner
import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.admin.AdminClientConfig
import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.apache.logging.log4j.LogManager
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.kafka.annotation.TopicPartition
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.*
import org.springframework.kafka.listener.ConsumerAwareListenerErrorHandler
import org.springframework.kafka.listener.ContainerProperties
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.kafka.support.serializer.JsonDeserializer
import org.springframework.kafka.support.serializer.JsonSerializer
import java.util.*


@EnableKafka
@Configuration
open class KafkaConfig {
    private val log = LogManager.getRootLogger()

    @Value("\${kafka.bootstrapserver}")
    private val bootstrapserver: String? = null
    @Value("\${kafka.groupid}")
    private val groupid: String? = null
    @Value("\${kafka.chatpair}")
    private val chatpair: String? = null
    @Value("\${kafka.chatgroup}")
    private val chatgroup: String? = null
    @Value("\${kafka.chatall}")
    private val chatall: String? = null
    @Value("\${kafka.durable}")
    private val durable: String? = null

    //kafkaTemplate实现了Kafka发送接收等功能
    @Bean
    open fun kafkaTemplate(): KafkaTemplate<String, Any> {
        return KafkaTemplate(producerFactory())
    }

    // 配置Kafka实例的连接地址, 必须配置，否则启动报错
    @Bean
    open fun kafkaAdmin(): KafkaAdmin {
        val props: MutableMap<String, Any> = HashMap()
        props[AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG] = bootstrapserver!!
        return KafkaAdmin(props)
    }
    // 必须配置，否则启动报错
    @Bean
    open fun adminClient(): AdminClient {
        return AdminClient.create(kafkaAdmin().config)
    }


    /**
     * Kafka消息监听器的工厂类，这里只配置了消费者
     * Primary注解的意思是在拥有多个同类型的Bean时优先使用该Bean，到时候方便我们使用@Autowired注解自动注入。
     */
    @Primary
    @Bean("kafkaListenerContainerFactory")
    open fun kafkaListenerContainerFactory(): ConcurrentKafkaListenerContainerFactory<String, Any> {
        val factory = ConcurrentKafkaListenerContainerFactory<String, Any>()
        factory.consumerFactory = consumerFactory()
        factory.setConcurrency(5)
        factory.isBatchListener = true
        val containerProperties = factory.containerProperties
        // 当Acknowledgment.acknowledge()方法被调用即提交offset
        containerProperties.ackMode = ContainerProperties.AckMode.MANUAL_IMMEDIATE
        containerProperties.pollTimeout = 3000
        // containerProperties.setMissingTopicsFatal(false);
        // 调用commitAsync()异步提交
        // containerProperties.setSyncCommits(false);
        return factory
    }

    /**
     * 根据consumerProps填写的参数创建消费者工厂
     * 定义序列化class
     */
    @Bean
    open fun consumerFactory(): ConsumerFactory<String, Any> {
        val consumerProps: MutableMap<String, Any?> = HashMap()
        consumerProps[ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG] = bootstrapserver
        consumerProps[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
        consumerProps[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = JsonDeserializer::class.java
        consumerProps[ConsumerConfig.GROUP_ID_CONFIG] = groupid
        consumerProps[ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG] = false
        consumerProps[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "earliest"
        consumerProps[ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG] = "15000"
        consumerProps[ConsumerConfig.MAX_POLL_RECORDS_CONFIG] = "50"
        val deserializer = JsonDeserializer<Any>()
        deserializer.addTrustedPackages("com.myworld.schat.data.entity")
        return DefaultKafkaConsumerFactory(consumerProps, StringDeserializer(), deserializer)
    }

    /**
     * 根据producerProps填写的参数创建生产者工厂
     * 同时开启了事务管理
     * 然后在kafkaTemplate.send()所在的方法上添加@Transactional注释，开启事务
     */
    @Bean
    open fun producerFactory(): ProducerFactory<String, Any> {
        val producerProps: MutableMap<String, Any?> = HashMap()
        producerProps[ProducerConfig.BOOTSTRAP_SERVERS_CONFIG] = bootstrapserver
        producerProps[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java
        producerProps[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = JsonSerializer::class.java
        producerProps[ProducerConfig.ACKS_CONFIG] = "all"
        producerProps[ProducerConfig.RETRIES_CONFIG] = 3
        producerProps[ProducerConfig.BATCH_SIZE_CONFIG] = 1048576
        producerProps[ProducerConfig.LINGER_MS_CONFIG] = 10
        producerProps[ProducerConfig.BUFFER_MEMORY_CONFIG] = 33554432
        producerProps[ProducerConfig.MAX_BLOCK_MS_CONFIG] = 3000
        producerProps[ProducerConfig.MAX_REQUEST_SIZE_CONFIG] = 10485760
        producerProps[ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG] = 6000
        producerProps[ProducerConfig.PARTITIONER_CLASS_CONFIG] = MyKafkaPartitioner::class.java
        // producerProps[ProducerConfig.INTERCEPTOR_CLASSES_CONFIG] = arrayListOf(MyProducerInterceptor::class)
        return DefaultKafkaProducerFactory(producerProps)
    }

    //第一个是参数是topic名字，第二个参数是分区个数，第三个是topic的复制因子个数
    //当broker个数为1个时会创建topic失败，
    //提示：replication factor: 2 larger than available brokers: 1
    //只有在集群中才能使用kafka的备份功能
    @Bean
    open fun initialChatPair(): NewTopic {
        return NewTopic(chatpair, 5, 1.toShort())
    }

    @Bean
    open fun initialChatGroup(): NewTopic {
        return NewTopic(chatgroup, 5, 1.toShort())
    }

    @Bean
    open fun initialChatAll(): NewTopic {
        return NewTopic(chatall, 5, 1.toShort())
    }

    @Bean
    open fun initialDurable(): NewTopic {
        return NewTopic(durable, 5, 1.toShort())
    }

    @Bean("consumerAwareErrorHandler")
    open fun consumerAwareErrorHandler(): ConsumerAwareListenerErrorHandler {
        return ConsumerAwareListenerErrorHandler { message, e, consumer ->
            log.info("consumerAwareErrorHandler receive : " + message.payload.toString())
            val headers = message.headers
            val topics = headers.get(KafkaHeaders.RECEIVED_TOPIC, MutableList::class.java)
            val partitions = headers.get(KafkaHeaders.RECEIVED_PARTITION_ID, MutableList::class.java)
            val offsets = headers.get(KafkaHeaders.OFFSET, MutableList::class.java)
            val offsetsToReset: Map<TopicPartition, Long> = HashMap()
            null
        }
    }
}
