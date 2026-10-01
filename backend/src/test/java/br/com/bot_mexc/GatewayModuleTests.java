package br.com.bot_mexc;

import br.com.bot_mexc.modules.gateway.controllers.GatewayStreamController;
import br.com.bot_mexc.modules.gateway.filters.GatewayRateLimitingFilter;
import br.com.bot_mexc.modules.gateway.services.FrontendStreamingService;
import br.com.bot_mexc.shared.events.MarketDataUpdateEvent;
import br.com.bot_mexc.shared.events.TradeSignalEvent;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class GatewayModuleTests {

    private SimpMessagingTemplate messagingTemplate;
    private FrontendStreamingService streamingService;
    private GatewayRateLimitingFilter rateLimitingFilter;
    private GatewayStreamController streamController;

    @BeforeEach
    void setUp() {
        messagingTemplate = Mockito.mock(SimpMessagingTemplate.class);
        streamingService = new FrontendStreamingService(messagingTemplate);
        rateLimitingFilter = new GatewayRateLimitingFilter();
        streamController = new GatewayStreamController(streamingService);
    }

    @Test
    @DisplayName("FrontendStreamingService deve transmitir sinal de trade via SimpMessagingTemplate")
    void deveTransmitirSinalDeTrade() {
        TradeSignalEvent signal = new TradeSignalEvent(
                "op-99", "BTCUSDT", "15m", TradeSignalEvent.TipoSinal.COMPRA,
                new BigDecimal("65000"), Map.of(), System.currentTimeMillis(), Instant.now()
        );

        streamingService.broadcastTradeSignal(signal);

        verify(messagingTemplate, times(1)).convertAndSend("/topic/signals", signal);
        verify(messagingTemplate, times(1)).convertAndSend("/topic/signals/op-99", signal);
    }

    @Test
    @DisplayName("FrontendStreamingService deve transmitir MarketDataUpdateEvent")
    void deveTransmitirMarketData() {
        MarketDataUpdateEvent marketData = new MarketDataUpdateEvent(
                "ETHUSDT", "1h",
                new BigDecimal("3500"), new BigDecimal("3520"),
                new BigDecimal("3490"), new BigDecimal("3530"),
                new BigDecimal("1200"), 1000L, 2000L, true
        );

        streamingService.broadcastMarketData(marketData);

        verify(messagingTemplate, times(1)).convertAndSend("/topic/market/ETHUSDT/1h", marketData);
    }

    @Test
    @DisplayName("FrontendStreamingService deve registrar SseEmitter com sucesso")
    void deveRegistrarSseEmitter() {
        SseEmitter emitter = streamingService.registrarSseEmitter();
        assertNotNull(emitter);
    }

    @Test
    @DisplayName("GatewayRateLimitingFilter deve permitir bypass de rotas WebSocket e SSE")
    void devePermitirBypassEmRotasWebSocketESse() throws ServletException, IOException {
        MockHttpServletRequest requestWs = new MockHttpServletRequest("GET", "/ws-bot/info");
        MockHttpServletResponse responseWs = new MockHttpServletResponse();
        FilterChain filterChain = Mockito.mock(FilterChain.class);

        rateLimitingFilter.doFilter(requestWs, responseWs, filterChain);
        verify(filterChain, times(1)).doFilter(requestWs, responseWs);

        MockHttpServletRequest requestSse = new MockHttpServletRequest("GET", "/v1/gateway/stream");
        MockHttpServletResponse responseSse = new MockHttpServletResponse();
        rateLimitingFilter.doFilter(requestSse, responseSse, filterChain);
        verify(filterChain, times(1)).doFilter(requestSse, responseSse);
    }

    @Test
    @DisplayName("GatewayRateLimitingFilter deve limitar rota sensível de auth após exceder capacidade")
    void deveBloquearRotaSensivelAoExcederLimite() throws ServletException, IOException {
        FilterChain filterChain = Mockito.mock(FilterChain.class);
        String clientIp = "192.168.1.100";

        // Consome os 20 tokens permitidos por minuto
        for (int i = 0; i < 20; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/v1/auth/login");
            req.setRemoteAddr(clientIp);
            MockHttpServletResponse res = new MockHttpServletResponse();
            rateLimitingFilter.doFilter(req, res, filterChain);
            assertEquals(200, res.getStatus());
        }

        // 21ª requisição deve retornar HTTP 429
        MockHttpServletRequest reqExcedente = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        reqExcedente.setRemoteAddr(clientIp);
        MockHttpServletResponse resExcedente = new MockHttpServletResponse();
        rateLimitingFilter.doFilter(reqExcedente, resExcedente, filterChain);

        assertEquals(HttpStatus.TOO_MANY_REQUESTS.value(), resExcedente.getStatus());
        assertTrue(resExcedente.getContentAsString().contains("Too Many Requests"));
    }

    @Test
    @DisplayName("GatewayStreamController deve retornar status UP")
    void deveRetornarStatusDoGateway() {
        ResponseEntity<Map<String, Object>> response = streamController.getGatewayStatus();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("UP", response.getBody().get("status"));
        assertEquals("/ws-bot", response.getBody().get("websocketEndpoint"));
    }
}
