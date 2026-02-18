package com.alexanski.chat.dto;

public class ChatMessage {
    private String type;
    private String from;
    private String to;
    private String room;
    private String content;
    private String timestamp;

    public ChatMessage() {
    }

    public ChatMessage(String type, String from, String to, String room, String content, String timestamp) {
        this.type = type;
        this.from = from;
        this.to = to;
        this.room = room;
        this.content = content;
        this.timestamp = timestamp;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getRoom() {
        return room;
    }

    public void setRoom(String room) {
        this.room = room;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "ChatMessage{" +
                "type='" + type + '\'' +
                ", from='" + from + '\'' +
                ", content='" + content + '\'' +
                '}';
    }
}
