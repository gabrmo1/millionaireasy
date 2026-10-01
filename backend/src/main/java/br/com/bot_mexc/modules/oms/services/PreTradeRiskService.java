package br.com.bot_mexc.modules.oms.services;

import br.com.bot_mexc.modules.strategy.dtos.OperacaoCacheDTO;
import br.com.bot_mexc.shared.events.TradeSignalEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
public class PreTradeRiskService {

    private static final BigDecimal VALOR_MINIMO_ORDEM_USDT = new BigDecimal("5.00");

    public boolean validarRiscoPreTrade(OperacaoCacheDTO operacao, TradeSignalEvent sinal) {
        if (operacao == null) {
            log.warn("[RISK-CHECK] REJEITADO: Operação não encontrada em cache.");
            return false;
        }

        // Validação de tipo de sinal vs estado da posição
        if (sinal.tipo() == TradeSignalEvent.TipoSinal.COMPRA) {
            if (Boolean.TRUE.equals(operacao.posicionado())) {
                log.warn("[RISK-CHECK] REJEITADO: Operação {} já está posicionada. Ignorando novo sinal de compra.", operacao.id());
                return false;
            }

            // Checagem de valor mínimo configurado (em USDT)
            BigDecimal valorOrdem = operacao.valorOperacaoFixo();
            if (valorOrdem != null && valorOrdem.compareTo(VALOR_MINIMO_ORDEM_USDT) < 0) {
                log.warn("[RISK-CHECK] REJEITADO: Valor configurado ({}) abaixo do mínimo da MEXC ({} USDT).",
                        valorOrdem, VALOR_MINIMO_ORDEM_USDT);
                return false;
            }

        } else if (sinal.tipo() == TradeSignalEvent.TipoSinal.VENDA) {
            if (!Boolean.TRUE.equals(operacao.posicionado())) {
                log.warn("[RISK-CHECK] REJEITADO: Operação {} NÃO está posicionada. Impossível vender sem ativos em custódia.", operacao.id());
                return false;
            }

            // Checagem de venda apenas por lucro
            if (Boolean.TRUE.equals(operacao.vendaApenasPorLucro()) && operacao.precoMedioEntrada() != null && operacao.precoMedioEntrada().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal precoAtual = sinal.preco();
                if (precoAtual.compareTo(operacao.precoMedioEntrada()) <= 0) {
                    log.info("[RISK-CHECK] VENDA SUSPENSA: Configuração de venda apenas por lucro ativa. Preço atual ({}) <= Preço Entrada ({}).",
                            precoAtual, operacao.precoMedioEntrada());
                    return false;
                }
            }
        }

        log.info("[RISK-CHECK] APROVADO: Sinal de {} para Operação ID: {} ({}) passou em todas as travas pré-trade.",
                sinal.tipo(), operacao.id(), operacao.par());
        return true;
    }
}
