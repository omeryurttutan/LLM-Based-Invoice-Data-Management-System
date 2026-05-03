package com.faturaocr.infrastructure.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import java.util.Map;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    @SuppressWarnings("null")
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String token = extractToken(accessor);

            if (token != null && jwtTokenProvider.validateToken(token)) {
                UUID userId = jwtTokenProvider.getUserIdFromToken(token);

                // Create a principal for the user
                Principal principal = new Principal() {
                    @Override
                    public String getName() {
                        return userId.toString();
                    }
                };

                accessor.setUser(principal);

                // Also set authentication in SecurityContext for current thread if needed
                // though WebSocket sessions are persistent and Context is thread-bound
                Authentication auth = new UsernamePasswordAuthenticationToken(
                        userId, null, Collections.emptyList());
                SecurityContextHolder.getContext().setAuthentication(auth);

                log.info("WebSocket connection authenticated for user: {}", userId);
            } else {
                log.warn("WebSocket connection rejected: Invalid or missing token");
                // Returning null prevents the message from being sent, effectively rejecting
                // the connection
                return null;
            }
        }

        return message;
    }

    private String extractToken(StompHeaderAccessor accessor) {
        // 1. Try "Authorization" header
        List<String> authHeader = accessor.getNativeHeader("Authorization");
        if (authHeader != null && !authHeader.isEmpty()) {
            String token = authHeader.get(0);
            if (token.startsWith("Bearer ")) {
                return token.substring(7);
            }
            return token;
        }

        // 2. Try session attributes (populated by JwtHandshakeInterceptor)
        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
        if (sessionAttributes != null && sessionAttributes.containsKey("token")) {
            return (String) sessionAttributes.get("token");
        }

        // 3. Try native header "token" (fallback)
        List<String> tokenHeader = accessor.getNativeHeader("token");
        if (tokenHeader != null && !tokenHeader.isEmpty()) {
            return tokenHeader.get(0);
        }

        return null;
    }
}
