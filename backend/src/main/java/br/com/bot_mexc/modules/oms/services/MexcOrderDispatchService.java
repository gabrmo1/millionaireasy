package br.com.bot_mexc.modules.oms.services;

import br.com.bot_mexc.modules.strategy.dtos.OperacaoCacheDTO;
import br.com.bot_mexc.modules.oms.utils.MexcSignatureHelper;
import br.com.bot_mexc.modules.strategy.entities.Operador;
import br.com.bot_mexc.modules.strategy.repositories.OperadorRepository;
import br.com.bot_mexc.shared.events.TradeSignalEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MexcOrderDispatchService {

    private final MexcRateLimiter rateLimiter;
    private final OperadorRepository operadorRepository;
    private final ObjectMapper objectMapper;

    private static final String BASE_URL = "https://api.mexc.com";
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public String despacharOrdemMercado(OperacaoCacheDTO operacao, TradeSignalEvent sinal) {
        rateLimiter.acquireToken();

        Operador operador = operadorRepository.findById(operacao.operadorId())
                .orElseThrow(() -> new IllegalStateException("Operador não encontrado para conta real: " + operacao.operadorId()));

        final String accessKey = operador.getAccessKey();
        final String secretKey = operador.getSecretKey();
        final String symbol = operacao.par();
        final String side = (sinal.tipo() == TradeSignalEvent.TipoSinal.COMPRA) ? "BUY" : "SELL";
        final String clientOrderId = "MO" + UUID.randomUUID().toString().replace("-", "").substring(0, 18);
        final long timestamp = System.currentTimeMillis();

        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("symbol=").append(symbol)
                .append("&side=").append(side)
                .append("&type=").append("MARKET")
                .append("&newClientOrderId=").append(clientOrderId)
                .append("&recvWindow=5000")
                .append("&timestamp=").append(timestamp);

        if ("BUY".equals(side)) {
            // Em ordens de compra a mercado na MEXC, informa-se o valor em USDT (quoteOrderQty)
            BigDecimal valorUSDT = operacao.valorOperacaoFixo();
            queryBuilder.append("&quoteOrderQty=").append(valorUSDT);
        } else {
            // Em ordens de venda a mercado, informa-se a quantidade de tokens em custódia
            BigDecimal volumeVenda = operacao.volumeEmMao();
            queryBuilder.append("&quantity=").append(volumeVenda);
        }

        final String queryString = queryBuilder.toString();
        final String signature = MexcSignatureHelper.signHmacSha256(queryString, secretKey);
        final String fullUrl = BASE_URL + "/api/v3/order?" + queryString + "&signature=" + signature;

        log.info("[DESPACHO MEXC] Enviando ordem {} MARKET para {} via REST. clientOrderId: {}", side, symbol, clientOrderId);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(fullUrl))
                    .header("X-MEXC-APIKEY", accessKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .timeout(Duration.ofSeconds(10))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode json = objectMapper.readTree(response.body());
                String orderId = json.has("orderId") ? json.get("orderId").asText() : "N/A";
                log.info("[MEXC SUCESSO] Ordem executada com sucesso na MEXC! OrderID: {}, Status: {}", orderId, json.path("status").asText());
                return orderId;
            } else {
                log.error("[MEXC ERRO {}] Falha ao despachar ordem: {}", response.statusCode(), response.body());
                throw new RuntimeException("Erro da MEXC ao executar ordem: " + response.body());
            }

        } catch (Exception e) {
            log.error("[DESPACHO FALHA] Erro de rede ou comunicação com a MEXC: {}", e.getMessage(), e);
            throw new RuntimeException("Falha na chamada REST à MEXC: " + e.getMessage(), e);
        }
    }

    public void cancelarTodasOrdensAbertas(String par, Operador operador) {
        rateLimiter.acquireToken();

        final String symbol = par;
        final long timestamp = System.currentTimeMillis();
        final String queryString = "symbol=" + symbol + "&recvWindow=5000&timestamp=" + timestamp;
        final String signature = MexcSignatureHelper.signHmacSha256(queryString, operador.getSecretKey());
        final String fullUrl = BASE_URL + "/api/v3/openOrders?" + queryString + "&signature=" + signature;

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(fullUrl))
                    .header("X-MEXC-APIKEY", operador.getAccessKey())
                    .DELETE()
                    .timeout(Duration.ofSeconds(10))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            log.info("[CANCELAMENTO MEXC] Cancelamento de ordens abertas para {} retornado com HTTP {}: {}", symbol, response.statusCode(), response.body());
        } catch (Exception e) {
            log.error("[CANCELAMENTO FALHA] Erro ao cancelar ordens na MEXC para {}: {}", symbol, e.getMessage(), e);
        }
    }
}
