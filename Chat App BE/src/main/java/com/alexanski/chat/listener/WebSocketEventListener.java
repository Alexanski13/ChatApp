package com.alexanski.chat.listener;

import com.alexanski.chat.dto.ChatMessage;
import com.alexanski.chat.registry.ChatUserRegistry;
import com.alexanski.chat.service.ChatRoomService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(WebSocketEventListener.class);
    private final SimpMessagingTemplate messagingTemplate;
    private final ChatUserRegistry chatUserRegistry;
    private final ChatRoomService chatRoomService;

    public WebSocketEventListener(SimpMessagingTemplate messagingTemplate, ChatUserRegistry chatUserRegistry, ChatRoomService chatRoomService) {
        this.messagingTemplate = messagingTemplate;
        this.chatUserRegistry = chatUserRegistry;
        this.chatRoomService = chatRoomService;
    }

    @EventListener
    public void handleConnect(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String username = accessor.getUser() != null ? accessor.getUser().getName() : null;
        if (username != null) {
            chatUserRegistry.add(username);
            log.info("User connected: {}", username);
            broadcastUserList();
        }
    }

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String username = accessor.getUser() != null ? accessor.getUser().getName() : null;

        if (username != null) {
            log.info("User disconnected: {}", username);
            chatUserRegistry.remove(username);

            chatRoomService.getAllRooms().forEach(room -> {
                if (chatRoomService.getUsersInRoom(room).contains(username)) {
                    chatRoomService.removeUserFromRoom(room, username);

                    ChatMessage leaveMsg = new ChatMessage();
                    leaveMsg.setType("LEAVE");
                    leaveMsg.setFrom(username);
                    leaveMsg.setRoom(room);
                    leaveMsg.setContent("disconnected unexpectedly");
                    leaveMsg.setTimestamp(LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));

                    messagingTemplate.convertAndSend("/topic/room/" + room, leaveMsg);
                    messagingTemplate.convertAndSend("/topic/users/" + room, chatRoomService.getUsersInRoom(room));
                }
            });

            broadcastUserList();
        }
    }

    private void broadcastUserList() {
        messagingTemplate.convertAndSend(
                "/topic/users",
                chatUserRegistry.getUsers()
        );
    }
}
