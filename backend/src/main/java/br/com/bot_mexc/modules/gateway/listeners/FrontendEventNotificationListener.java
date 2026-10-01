package br.com.bot_mexc.modules.gateway.listeners;

import br.com.bot_mexc.modules.gateway.services.FrontendStreamingService;
import br.com.bot_mexc.shared.events.MarketDataUpdateEvent;
import br.com.bot_mexc.shared.events.TradeSignalEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FrontendEventNotificationListener {

    private final FrontendStreamingService streamingService;

    @EventListener
    public void onTradeSignal(TradeSignalEvent event) {
        log.info("[GATEWAY BFF] Evento de trade capturado. Reencaminhando para o Frontend...");
        streamingService.broadcastTradeSignal(event);
    }

    @EventListener
    public void onMarketData(MarketDataUpdateEvent event) {
        streamingService.broadcastMarketData(event);
    }
}
