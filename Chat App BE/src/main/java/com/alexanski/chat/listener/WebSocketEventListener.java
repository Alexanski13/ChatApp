package com.alexanski.chat.listener;

import com.alexanski.chat.dto.ChatMessage;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class WebSocketEventListener {

    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketEventListener(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @EventListener
    public void handleConnect(SessionConnectedEvent event) {

    }

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());

        String username = accessor.getUser() != null ? accessor.getUser().getName() : null;

        if (username != null) {
            ChatMessage leaveMessage = new ChatMessage(
                    "LEAVE",
                    username,
                    "left the chat",
                    LocalDateTime.now().format((DateTimeFormatter.ofPattern("HH:mm:ss"))
                    ));

            messagingTemplate.convertAndSend("/topic/chat", leaveMessage);
        }
    }
}
