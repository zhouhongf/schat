package com.myworld.schat.service.impl

import com.myworld.schat.service.UserRedisService
import org.apache.logging.log4j.LogManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.redis.core.BoundHashOperations
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service

@Service
class UserRedisServiceImpl : UserRedisService {
    private val log = LogManager.getRootLogger()

    private val WSUSER = "wsuser"

    @Autowired
    @Qualifier("myRedisTemplate")
    private lateinit var redisTemplate: RedisTemplate<String, Any>

    override fun addUser(userWid: String, sessionId: String?, subscribeId: String?) {
        if (sessionId != null) {
            val boundHashOperations: BoundHashOperations<String, String, MutableSet<String>> = redisTemplate.boundHashOps("$WSUSER:$userWid")
            var subscribeIds : MutableSet<String>? = boundHashOperations.get(sessionId)
            // 根据key为sessionId查找value为subscribeIds的记录, 如果value部分为null, 则新建一个空的HashSet集合
            // 群聊，格式：/user/group/name/MYUSER15342193174008888GROUP1580352212190；
            // 单聊，格式：/user/queue/name/MYUSER15547704317619000
            // value的格式就是Set集合{MYUSER15342193174008888GROUP1580352212190，MYUSER15547704317619000}
            if (subscribeIds == null) {
                subscribeIds = HashSet()
            }
            // 如果传入进来的subscribeId不是“”空的，则添加进去后，再保存
            if (subscribeId != null) {
                subscribeIds.add(subscribeId)
            }

            boundHashOperations.put(sessionId, subscribeIds)
        }
    }

    override fun deleteUser(userWid: String) {
        redisTemplate.delete("$WSUSER:$userWid")
    }


    override fun getReceiverSessionId(userWid: String): String? {
        val boundHashOperations: BoundHashOperations<String, String, MutableSet<String>> = redisTemplate.boundHashOps("$WSUSER:$userWid")
        val keys = boundHashOperations.keys() ?: return null
        if (keys.size > 1) {
            return null
        }
        return keys.toTypedArray()[0]
    }

    override fun checkReceiverSessionIdAndSubscribeIds(userWid: String): MutableSet<String>? {
        val boundHashOperations: BoundHashOperations<String, String, MutableSet<String>> = redisTemplate.boundHashOps("$WSUSER:$userWid")
        val keys = boundHashOperations.keys()
        if (keys.isNullOrEmpty()) {
            return null
        }
        // 一般来说，一个用户wid为mainKey的记录中，keys中只有一条key的记录，对应多个subscribeUri，譬如前端在建立一次连接后，有多个subscribe的动作；
        // 一个用户keys中存在多条key的记录的前提是：该用户在不同的IP地址多处登录，并同时打开聊天会话；
        // 为了保证聊天记录的一致性和完整性，此处只限定为只有一个key的记录，才符合要求。
        if (keys.size > 1) {
            return null
        }

        val key = keys.toTypedArray()[0]
        return boundHashOperations.get(key)
    }

}
