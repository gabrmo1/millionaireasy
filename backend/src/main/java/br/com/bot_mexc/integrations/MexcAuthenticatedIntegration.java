package br.com.bot_mexc.integrations;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Component
@FeignClient(
        name = "MexcAuthenticatedIntegration",
        url = "https://api.mexc.com"
//configuration = ) //TODO: Criar classe para gerar autenticação
)
public interface MexcAuthenticatedIntegration {

    @GetMapping("/api/v3/klines")
    String obterCandles(@RequestParam("symbol") String symbol,
                        @RequestParam("interval") String interval,
                        @RequestParam("limit") String limit);
}
