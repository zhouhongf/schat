package com.myworld.schat.config

import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.serializer.SerializerFeature
import org.springframework.data.redis.serializer.RedisSerializer
import org.springframework.data.redis.serializer.SerializationException
import java.nio.charset.Charset


class FastJsonRedisSerializer<T>(private val clazz: Class<T>) : RedisSerializer<T> {

    companion object {
        private val DEFAULT_CHARSET = Charset.forName("UTF-8")
    }

    @Throws(SerializationException::class)
    override fun serialize(t: T?) = if (null == t) {
        ByteArray(0)
    } else JSON.toJSONString(t, SerializerFeature.WriteClassName).toByteArray(DEFAULT_CHARSET)

    @Throws(SerializationException::class)
    override fun deserialize(bytes: ByteArray?): T? {

        if (null == bytes || bytes.size <= 0) {
            return null
        }
        val str = String(bytes, DEFAULT_CHARSET)
        return JSON.parseObject(str, clazz) as T
    }
}
