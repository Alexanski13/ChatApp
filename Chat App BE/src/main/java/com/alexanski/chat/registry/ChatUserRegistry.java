package com.alexanski.chat.registry;

import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChatUserRegistry {

    private final Set<String> users = ConcurrentHashMap.newKeySet();

    public void add(String username) {
        if (username != null) {
            users.add(username);
        }
    }

    public void remove(String username) {
        if (username != null) {
            users.remove(username);
        }
    }

    public Set<String> getUsers() {
        return users;
    }
}
