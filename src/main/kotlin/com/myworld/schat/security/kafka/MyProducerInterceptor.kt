package com.myworld.schat.security.kafka

import org.apache.kafka.clients.producer.ProducerInterceptor
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.logging.log4j.LogManager

class MyProducerInterceptor : ProducerInterceptor<String, Any> {
    private val log = LogManager.getRootLogger()
    private var errorCounter = 0
    private var successCounter = 0
    override fun configure(configs: Map<String?, *>?) {}
    override fun onSend(record: ProducerRecord<String, Any>): ProducerRecord<String, Any> {
        return record
    }

    override fun onAcknowledgement(metadata: RecordMetadata, exception: Exception) {
        if (exception == null) {
            successCounter++
            log.info("统计成功发送：$successCounter")
        } else {
            errorCounter++
            log.info("统计失败发送：$errorCounter")
        }
    }

    override fun close() { // 关闭producer后，才会调用interceptor中的close方法
        log.info("统计成功发送累计：$successCounter")
        log.info("统计失败发送累计：$errorCounter")
    }
}
