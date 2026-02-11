package com.alexanski.chat.interceptor;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.security.Principal;

@Component
public class UsernameHandshakeInterceptor implements ChannelInterceptor {

    private static final String USERNAME_HEADER = "username";

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {

        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(
                        message,
                        StompHeaderAccessor.class
                );

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {

            String username =
                    accessor.getFirstNativeHeader(USERNAME_HEADER);

            accessor.setUser(new Principal() {
                @Override
                public String getName() {
                    return username;
                }
            });
        }

        return message;
    }
}
