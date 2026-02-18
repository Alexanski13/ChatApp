package com.alexanski.chat.integration;

import com.alexanski.chat.dto.ChatMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.JacksonJsonMessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;
import tools.jackson.databind.json.JsonMapper;

import java.lang.reflect.Type;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ChatWebSocketIntegrationTest {

    @LocalServerPort
    private int port;

    private WebSocketStompClient stompClient;

    @BeforeEach
    void setup() {
        var transport = new WebSocketTransport(new StandardWebSocketClient());
        var sockJs = new SockJsClient(List.of(transport));

        stompClient = new WebSocketStompClient(sockJs);

        JacksonJsonMessageConverter converter = new JacksonJsonMessageConverter(JsonMapper.builder().build());

        stompClient.setMessageConverter(converter);
    }

    @Test
    void shouldJoinRoomAndReceiveOwnJoinMessage() throws Exception {
        String username = "alice";
        String room = "test-room";

        BlockingQueue<ChatMessage> received = new ArrayBlockingQueue<>(10);

        StompSessionHandler handler = new StompSessionHandlerAdapter() {
            @Override
            public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
                session.subscribe("/topic/room/" + room, new StompFrameHandler() {
                    @Override
                    public Type getPayloadType(StompHeaders headers) {
                        return ChatMessage.class;
                    }

                    @Override
                    public void handleFrame(StompHeaders headers, Object payload) {
                        ChatMessage msg = (ChatMessage) payload;
                        System.out.println("Received: " + msg);
                        received.add(msg);
                    }
                });

                ChatMessage joinMsg = new ChatMessage();
                joinMsg.setType("JOIN");
                joinMsg.setRoom(room);
                session.send("/app/chat/join", joinMsg);
            }

            @Override
            public void handleException(StompSession session, StompCommand command, StompHeaders headers,
                                        byte[] payload, Throwable exception) {
                exception.printStackTrace();
            }
        };

        WebSocketHttpHeaders handshakeHeaders = new WebSocketHttpHeaders();
        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.add("username", username);

        StompSession session = stompClient
                .connectAsync(
                        "ws://localhost:" + port + "/ws",
                        handshakeHeaders,
                        connectHeaders,
                        handler
                )
                .get(5, TimeUnit.SECONDS);

        ChatMessage msg = received.poll(8, TimeUnit.SECONDS);

        assertThat(msg)
                .as("No JOIN message received after 8 seconds")
                .isNotNull();

        assertThat(msg.getType()).isEqualTo("JOIN");
        assertThat(msg.getFrom()).isEqualTo(username);
    }
}