package com.spkt.libraSys.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    private final WebSocketAuthInterceptor webSocketAuthInterceptor;

    public WebSocketConfig(WebSocketAuthInterceptor webSocketAuthInterceptor) {
        this.webSocketAuthInterceptor = webSocketAuthInterceptor;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Định cấu hình Message Broker
        config.enableSimpleBroker("/topic","/queue"); // Đường dẫn cho các thông điệp gửi đến client
        config.setApplicationDestinationPrefixes("/app"); // Đường dẫn cho các thông điệp từ client đến server
        config.setUserDestinationPrefix("/user"); // Định nghĩa prefix cho các tin nhắn gửi đến người dùng cụ thể
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Khai báo endpoint WebSocket và cho phép CORS
//        registry.addEndpoint("/ws")
//                .setAllowedOrigins("http://localhost:3000")
//                .addInterceptors(webSocketAuthInterceptor) // Dùng Interceptor để xác thực JWT
//                .setHandshakeHandler(new DefaultHandshakeHandler())
//                .withSockJS();

        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")  // ✅ Cho phép mọi origin — hoặc chỉ định rõ cụ thể
                .addInterceptors(webSocketAuthInterceptor)
                .setHandshakeHandler(new DefaultHandshakeHandler())
                .withSockJS();
    }

}