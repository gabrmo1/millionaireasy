package br.com.bot_mexc.modules.gateway.services;

import br.com.bot_mexc.modules.timeseries.dtos.CandleDTO;
import br.com.bot_mexc.shared.events.MarketDataUpdateEvent;
import br.com.bot_mexc.shared.events.TradeSignalEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Service
@RequiredArgsConstructor
public class FrontendStreamingService {

    private final SimpMessagingTemplate messagingTemplate;
    private final List<SseEmitter> sseEmitters = new CopyOnWriteArrayList<>();

    public void broadcastCandle(String par, String intervalo, CandleDTO candle) {
        final String destination = String.format("/topic/market/%s/%s", par, intervalo);
        messagingTemplate.convertAndSend(destination, candle);
        sendSseEvent("candle", candle);
    }

    public void broadcastMarketData(MarketDataUpdateEvent marketData) {
        final String destination = String.format("/topic/market/%s/%s", marketData.par(), marketData.intervalo());
        messagingTemplate.convertAndSend(destination, marketData);
        sendSseEvent("market-data", marketData);
    }

    public void broadcastTradeSignal(TradeSignalEvent signal) {
        log.info("[BFF STREAM] Transmitindo sinal de trade para o Frontend via WebSocket/SSE: {} {}", signal.tipo(), signal.par());
        messagingTemplate.convertAndSend("/topic/signals", signal);
        messagingTemplate.convertAndSend("/topic/signals/" + signal.idOperacao(), signal);
        sendSseEvent("trade-signal", signal);
    }

    public void broadcastNotification(String titulo, String mensagem, String tipo) {
        NotificationPayload payload = new NotificationPayload(titulo, mensagem, tipo, System.currentTimeMillis());
        messagingTemplate.convertAndSend("/topic/notifications", payload);
        sendSseEvent("notification", payload);
    }

    public SseEmitter registrarSseEmitter() {
        SseEmitter emitter = new SseEmitter(180_000L); // 3 minutos de timeout
        sseEmitters.add(emitter);

        emitter.onCompletion(() -> sseEmitters.remove(emitter));
        emitter.onTimeout(() -> sseEmitters.remove(emitter));
        emitter.onError(e -> sseEmitters.remove(emitter));

        try {
            emitter.send(SseEmitter.event().name("INIT").data("Conexão SSE estabelecida com o BFF"));
        } catch (IOException e) {
            sseEmitters.remove(emitter);
        }

        return emitter;
    }

    private void sendSseEvent(String eventName, Object data) {
        for (SseEmitter emitter : sseEmitters) {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(data));
            } catch (Exception e) {
                sseEmitters.remove(emitter);
            }
        }
    }

    public record NotificationPayload(String titulo, String mensagem, String tipo, long timestamp) {}
}
