package com.myworld.schat.security.kafka

import com.myworld.schat.data.modal.Constants
import org.apache.kafka.clients.producer.Partitioner
import org.apache.kafka.common.Cluster
import org.apache.logging.log4j.LogManager
import java.util.*

class MyKafkaPartitioner : Partitioner {
    private val log = LogManager.getRootLogger()
    private lateinit var random: Random

    override fun configure(configs: Map<String?, *>?) {
        random = Random()
    }

    override fun partition(topic: String, keyObj: Any, keyBytes: ByteArray, value: Any, valueBytes: ByteArray, cluster: Cluster): Int {
        // key格式，如：chatpair-ABC123456789-ABC123456
        // topic格式，如：myworld.kafka.chatpair

        val key = keyObj as String
        val partitionInfoList = cluster.availablePartitionsForTopic(topic)
        val partitionCount = partitionInfoList.size

        if (partitionCount > 4) {
            val lastTopicWord = topic.split(".")[2]
            log.info("【lastTopicWord是：{}】", lastTopicWord)
            val topicKeyWord = lastTopicWord.replace("chat", "").toUpperCase()
            val partitionNumber = when(topicKeyWord) {
                Constants.MESSAGE_PAIR_PREFIX -> partitionCount - 1
                Constants.MESSAGE_GROUP_PREFIX -> partitionCount - 2
                Constants.MESSAGE_ALL_PREFIX -> partitionCount - 3
                else -> partitionCount - 4
            }
            log.info("【partitionNumber是：{}】", partitionNumber)
            return partitionNumber
        } else {
            return random.nextInt(partitionCount)
        }
        // 旧的配置
        // val auditPartition = partitionCount - 1
        // 如果key不是空的，并且key不包含audit，则将该消息保存至kafka除最后一个partition之外的任意其他4个partition当中
        // return if (key.isEmpty() || !key.contains("audit")) random.nextInt(partitionCount - 1) else auditPartition
    }

    override fun close() {
        log.info("【AuditPartitioner的close方法实现必要资源的清理工作】")
    }
}
