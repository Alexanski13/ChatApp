package com.alexanski.chat.service;

import com.alexanski.chat.dto.ChatMessage;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ChatRoomService {

    private final Map<String, List<ChatMessage>> roomMessages = new HashMap<>();
    private final Map<String, Set<String>> roomUsers = new HashMap<>();
    private final Map<String, List<ChatMessage>> privateMessages = new HashMap<>();

    public void addMessage(String room, ChatMessage message) {
        roomMessages
                .computeIfAbsent(room, r -> new ArrayList<>())
                .add(message);
    }

    public List<ChatMessage> getMessages(String room) {
        return roomMessages.getOrDefault(room, new ArrayList<>());
    }

    public void addUserToRoom(String room, String username) {
        roomUsers
                .computeIfAbsent(room, r -> new HashSet<>())
                .add(username);
    }

    public void removeUserFromRoom(String room, String username) {
        if (roomUsers.containsKey(room)) {
            roomUsers.get(room).remove(username);
        }
    }

    public Set<String> getUsersInRoom(String room) {
        return roomUsers.getOrDefault(room, new HashSet<>());
    }

    public Set<String> getAllRooms() {
        return roomUsers.keySet();
    }

    public void addPrivateMessage(String user1, String user2, ChatMessage msg) {
        String key = generatePrivateKey(user1, user2);
        privateMessages
                .computeIfAbsent(key, k -> new ArrayList<>())
                .add(msg);
    }

    public List<ChatMessage> getPrivateMessages(String user1, String user2) {
        String key = generatePrivateKey(user1, user2);
        return privateMessages.getOrDefault(key, new ArrayList<>());
    }

    private String generatePrivateKey(String u1, String u2) {
        return (u1.compareTo(u2) < 0) ? u1 + "_" + u2 : u2 + "_" + u1;
    }
}
