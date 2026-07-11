package com.xigeandwillian.parkingsystem.common.config;

import com.xigeandwillian.parkingsystem.client.websocket.NotificationWebSocketHandler;
import com.xigeandwillian.parkingsystem.common.interceptor.NotificationHandshakeInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Autowired
    private NotificationWebSocketHandler notificationWebSocketHandler;

    @Autowired
    private NotificationHandshakeInterceptor notificationHandshakeInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(notificationWebSocketHandler, "/ws/*/messages")
                .addInterceptors(notificationHandshakeInterceptor)
                .setAllowedOrigins("*");
    }
}