package br.com.bot_mexc.modules.oms.integrations;
import br.com.bot_mexc.shared.enums.*;
import br.com.bot_mexc.shared.configs.RabbitMQConfig;
import br.com.bot_mexc.modules.oms.services.*;
import br.com.bot_mexc.modules.oms.dtos.*;
import br.com.bot_mexc.modules.oms.integrations.*;
import br.com.bot_mexc.modules.strategy.services.OperacaoCacheService;
import br.com.bot_mexc.modules.strategy.dtos.OperacaoCacheDTO;
import br.com.bot_mexc.modules.strategy.services.SimulacaoService;

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
