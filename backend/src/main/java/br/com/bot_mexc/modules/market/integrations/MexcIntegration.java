package br.com.bot_mexc.modules.market.integrations;
import br.com.bot_mexc.shared.enums.*;
import br.com.bot_mexc.shared.configs.RabbitMQConfig;
import br.com.bot_mexc.modules.oms.services.*;
import br.com.bot_mexc.modules.oms.dtos.*;
import br.com.bot_mexc.modules.oms.integrations.*;
import br.com.bot_mexc.modules.strategy.services.OperacaoCacheService;
import br.com.bot_mexc.modules.strategy.dtos.OperacaoCacheDTO;
import br.com.bot_mexc.modules.strategy.services.SimulacaoService;

import br.com.bot_mexc.modules.market.dtos.ValorMoedaDTO;
import br.com.bot_mexc.modules.market.dtos.mexc.ExchangeInfoDTO;
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
                        @RequestParam("limit") String limit,
                        @RequestParam(value = "startTime", required = false) Long startTime,
                        @RequestParam(value = "endTime", required = false) Long endTime);

    @GetMapping("/api/v3/ticker/price")
    ValorMoedaDTO obterValorAtualMoeda(@RequestParam("symbol") String symbol);

    @GetMapping("/api/v3/exchangeInfo")
    ExchangeInfoDTO getExchangeInfo();
}