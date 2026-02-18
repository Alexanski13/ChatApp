package com.alexanski.chat.controller;

import com.alexanski.chat.dto.ChatMessage;
import com.alexanski.chat.service.ChatRoomService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Controller
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);
    private final SimpMessagingTemplate messagingTemplate;
    private final ChatRoomService chatRoomService;

    public ChatController(SimpMessagingTemplate messagingTemplate, ChatRoomService chatRoomService) {
        this.messagingTemplate = messagingTemplate;
        this.chatRoomService = chatRoomService;
    }

    @MessageMapping("/chat/join")
    public void joinRoom(ChatMessage msg, Principal principal) {
        log.info("User {} joined room: {}", principal.getName(), msg.getRoom());

        String username = principal.getName();
        String room = msg.getRoom();

        chatRoomService.addUserToRoom(room, username);

        // Send history to user
        List<ChatMessage> history = chatRoomService.getMessages(room);
        messagingTemplate.convertAndSendToUser(
                username,
                "/queue/history",
                history
        );

        // Broadcast join message
        ChatMessage joinMsg = new ChatMessage();
        joinMsg.setType("JOIN");
        joinMsg.setFrom(username);
        joinMsg.setRoom(room);
        joinMsg.setContent("joined the room");
        joinMsg.setTimestamp(timestampNow());

        messagingTemplate.convertAndSend("/topic/room/" + room, joinMsg);

        broadcastUsers(room);
        broadcastRooms();
    }

    @MessageMapping("/chat/leave")
    public void leaveRoom(ChatMessage msg, Principal principal) {
        log.info("User {} left room: {}", principal.getName(), msg.getRoom());

        String username = principal.getName();
        String room = msg.getRoom();

        chatRoomService.removeUserFromRoom(room, username);

        ChatMessage leaveMsg = new ChatMessage();
        leaveMsg.setType("LEAVE");
        leaveMsg.setFrom(username);
        leaveMsg.setRoom(room);
        leaveMsg.setContent("left the room");
        leaveMsg.setTimestamp(timestampNow());

        messagingTemplate.convertAndSend("/topic/room/" + room, leaveMsg);

        broadcastUsers(room);
    }

    @MessageMapping("/chat/send")
    public void sendMessage(ChatMessage msg, Principal principal) {
        log.info("Received general msg: {}", msg);

        String username = principal.getName();
        String room = (msg.getRoom() == null || msg.getRoom().isEmpty())
                ? "general"
                : msg.getRoom();

        msg.setFrom(username);
        msg.setRoom(room);
        msg.setTimestamp(timestampNow());
        msg.setType("CHAT");

        chatRoomService.addMessage(room, msg);
        chatRoomService.addUserToRoom(room, username);

        messagingTemplate.convertAndSend("/topic/room/" + room, msg);

        broadcastRooms();
        broadcastUsers(room);
    }

    @MessageMapping("/chat/send/private")
    public void sendPrivateMessage(ChatMessage msg, Principal principal) {

        String from = principal.getName();
        String to = msg.getTo();

        msg.setFrom(from);
        msg.setTimestamp(timestampNow());
        msg.setType("PRIVATE");

        chatRoomService.addPrivateMessage(from, to, msg);

        messagingTemplate.convertAndSendToUser(to, "/queue/private", msg);
        messagingTemplate.convertAndSendToUser(from, "/queue/private", msg);
    }

    @MessageMapping("/chat/private/history")
    public void getPrivateChatHistory(ChatMessage msg, Principal principal) {
        String from = principal.getName();
        String to = msg.getTo();

        List<ChatMessage> history = chatRoomService.getPrivateMessages(from, to);

        messagingTemplate.convertAndSendToUser(
                from,
                "/queue/private-history",
                history
        );
    }

    private void broadcastUsers(String room) {
        messagingTemplate.convertAndSend(
                "/topic/users/" + room,
                chatRoomService.getUsersInRoom(room)
        );
    }

    private void broadcastRooms() {
        messagingTemplate.convertAndSend(
                "/topic/rooms",
                chatRoomService.getAllRooms()
        );
    }


    private String timestampNow() {
        return LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }
}
