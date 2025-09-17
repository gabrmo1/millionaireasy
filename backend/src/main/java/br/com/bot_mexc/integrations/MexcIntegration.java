package br.com.bot_mexc.integrations;

import br.com.bot_mexc.models.dtos.ValorMoedaDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Component
@FeignClient(
        name = "MexcIntegration",
        url = "https://api.mexc.com")
public interface MexcIntegration {

    @GetMapping("/api/v3/klines")
    String obterCandles(@RequestParam("symbol") String symbol,
                        @RequestParam("interval") String interval,
                        @RequestParam("limit") String limit);

    @GetMapping("/api/v3/ticker/price")
    ValorMoedaDTO obterValorAtualMoeda(@RequestParam("symbol") String symbol);
}
