package br.com.bot_mexc.modules.gateway.configs;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Prefixo para as mensagens que vão para o cliente (Ex: /topic/indicadores/123)
        config.enableSimpleBroker("/topic");
        // Prefixo para mensagens que vem do cliente (se necessário futuramente)
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Endpoint de conexão que o SockJS vai usar
        registry.addEndpoint("/ws-bot")
                .setAllowedOriginPatterns("*") // Permite conexão do Frontend (CORS)
                .withSockJS();
    }
}