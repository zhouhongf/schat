package com.myworld.schat.config

import com.myworld.schat.security.socket.MyChannelInterceptor
import com.myworld.schat.security.socket.MyHandshakeHandler
import com.myworld.schat.security.socket.MyHandshakeInterceptor
import com.myworld.schat.security.socket.MyWebSocketHandlerDecoratorFactory
import org.apache.logging.log4j.LogManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Configuration
import org.springframework.messaging.simp.config.ChannelRegistration
import org.springframework.messaging.simp.config.MessageBrokerRegistry
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker
import org.springframework.web.socket.config.annotation.StompEndpointRegistry
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration

@Configuration
@EnableScheduling // 开启使用STOMP协议来传输基于代理(message broker)的消息,这时控制器支持使用@MessageMapping,就像使用@RequestMapping一样。
@EnableWebSocketMessageBroker
open class WebSocketConfig : WebSocketMessageBrokerConfigurer {
    private val log = LogManager.getRootLogger()
    @Autowired
    private lateinit var myHandshakeHandler: MyHandshakeHandler
    @Autowired
    private lateinit var myHandshakeInterceptor: MyHandshakeInterceptor
    @Autowired
    private lateinit var myWebSocketHandlerDecoratorFactory: MyWebSocketHandlerDecoratorFactory
    @Autowired
    private lateinit var myChannelInterceptor: MyChannelInterceptor

    /**
     * 注册端点，发布或者订阅消息的时候需要连接此端点
     * addEndpoint websocket的端点，客户端需要注册这个端点进行链接
     * setAllowedOrigins 非必须，*表示允许其他域进行连接，跨域
     * withSockJS 允许客户端利用sockjs进行浏览器兼容性处理
     */
    override fun registerStompEndpoints(registry: StompEndpointRegistry) {
        // 添加一个访问端点“/socket”,客户端打开双通道时需要的url, 允许所有的域名跨域访问，指定使用SockJS协议。
        registry
            .addEndpoint("/socket")
            .setAllowedOrigins("*")
            .addInterceptors(myHandshakeInterceptor)
            .setHandshakeHandler(myHandshakeHandler)
        registry
            .addEndpoint("/sockjs")
            .setAllowedOrigins("*")
            .addInterceptors(myHandshakeInterceptor)
            .setHandshakeHandler(myHandshakeHandler)
            .withSockJS() // 添加后，注册地址使用http://开头，而不再是ws://开头了
    }

    /**
     * enableSimpleBroker方法中： P2P should conf a /user; Broadcast should conf a /topic
     * 前端用户接收消息 单聊：'/user/queue/name/' + friendWid
     * 前端用户接收消息 群聊：'/user/queue/group/' + groupId
     * 前端用户发送消息：/app/sendChatpair; /app/sendChatgroup; /app/sendBinary;
     */
    override fun configureMessageBroker(registry: MessageBrokerRegistry) {
        // registry.enableSimpleBroker("/topic", "/user", "/queue")
        registry.enableSimpleBroker("/queue", "/group")
        registry.setApplicationDestinationPrefixes("/app")      // Client to Server
        registry.setUserDestinationPrefix("/user")              // Server to Client
    }

    /**
     * 这是实际spring websocket集群的新增的配置，用于获取建立websocket时获取对应的sessionid值
     */
    override fun configureWebSocketTransport(registration: WebSocketTransportRegistration) {
        registration.setDecoratorFactories(myWebSocketHandlerDecoratorFactory)
    }

    override fun configureClientInboundChannel(registration: ChannelRegistration) {
        registration.interceptors(myChannelInterceptor)
    }
}
