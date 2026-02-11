package com.alexanski.chat.config;

import com.alexanski.chat.interceptor.UsernameHandshakeInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private static final String STOMP_ENDPOINT = "/ws";
    private static final String ALLOWED_ORIGINS = "*";
    private static final String MESSAGE_BROKER_DESTINATION = "/topic";
    private static final String MESSAGE_BROKER_DESTINATION_PREFIX = "/app";

    private final UsernameHandshakeInterceptor interceptor;

    public WebSocketConfig(UsernameHandshakeInterceptor interceptor) {
        this.interceptor = interceptor;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint(STOMP_ENDPOINT)
                .setAllowedOriginPatterns(ALLOWED_ORIGINS)
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker(MESSAGE_BROKER_DESTINATION);
        registry.setApplicationDestinationPrefixes(MESSAGE_BROKER_DESTINATION_PREFIX);
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(interceptor);
    }
}
