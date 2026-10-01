package br.com.bot_mexc.modules.gateway.controllers;

import br.com.bot_mexc.modules.gateway.services.FrontendStreamingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

@RestController
@RequestMapping("/v1/gateway")
@RequiredArgsConstructor
public class GatewayStreamController {

    private final FrontendStreamingService streamingService;

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter connectSse() {
        return streamingService.registrarSseEmitter();
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getGatewayStatus() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "websocketEndpoint", "/ws-bot",
                "brokerPrefix", "/topic",
                "timestamp", System.currentTimeMillis()
        ));
    }
}
