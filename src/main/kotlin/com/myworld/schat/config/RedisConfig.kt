package com.myworld.schat.config

import com.alibaba.fastjson.parser.ParserConfig
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.serializer.StringRedisSerializer
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.data.redis.core.RedisOperations
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.data.redis.RedisProperties


@Configuration
@ConditionalOnClass(RedisOperations::class)
@EnableConfigurationProperties(RedisProperties::class)
open class RedisConfig {

    @Bean(name = ["myRedisTemplate"])
    open fun redisTemplate(redisConnectionFactory: RedisConnectionFactory): RedisTemplate<String, Any> {

        // ParserConfig.getGlobalInstance().isAutoTypeSupport = true  // 允许全部类的实体Json转换
        ParserConfig.getGlobalInstance().addAccept("com.myworld.schat.data.entity.")  // 允许指定包名下属的类的实体Json转换

        val template = RedisTemplate<String, Any>()
        val fastJsonRedisSerializer = FastJsonRedisSerializer(Any::class.java)

        template.keySerializer = StringRedisSerializer()
        template.hashKeySerializer = StringRedisSerializer()
        template.valueSerializer = fastJsonRedisSerializer
        template.hashValueSerializer = fastJsonRedisSerializer

        template.connectionFactory = redisConnectionFactory
        return template
    }
}
