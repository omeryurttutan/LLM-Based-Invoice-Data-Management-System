package com.faturaocr.infrastructure.config;

import com.faturaocr.infrastructure.security.JwtHandshakeInterceptor;
import com.faturaocr.infrastructure.security.WebSocketAuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketAuthInterceptor authInterceptor;

    @Value("${websocket.allowed-origins:http://localhost:3001}")
    private String allowedOrigins;

    @Override
    @SuppressWarnings("null")
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enable simple broker for /topic (broadcast) and /queue (private)
        config.enableSimpleBroker("/topic", "/queue");
        // Application prefixes for client sends
        config.setApplicationDestinationPrefixes("/app");
        // Prefix for user-specific destinations
        config.setUserDestinationPrefix("/user");
    }

    @Override
    @SuppressWarnings("null")
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Register /ws endpoint with SockJS support
        registry.addEndpoint("/ws")
                .setAllowedOrigins(allowedOrigins.split(","))
                .addInterceptors(new JwtHandshakeInterceptor()) // Add handshake interceptor
                .withSockJS();
    }

    @Override
    @SuppressWarnings("null")
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // Register interceptor for authentication
        registration.interceptors(authInterceptor);
    }
}
