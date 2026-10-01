package br.com.bot_mexc.modules.oms.services;

import br.com.bot_mexc.modules.strategy.services.OperacaoCacheService;
import br.com.bot_mexc.modules.market.services.mexc.MexcSubscriptionService;
import br.com.bot_mexc.modules.oms.dtos.KillSwitchResponseDTO;
import br.com.bot_mexc.modules.strategy.entities.Operacao;
import br.com.bot_mexc.modules.strategy.repositories.OperacaoRepository;
import br.com.bot_mexc.shared.enums.StatusOperacoes;
import br.com.bot_mexc.shared.utils.DateUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class KillSwitchService {

    private final OperacaoRepository operacaoRepository;
    private final OperacaoCacheService operacaoCacheService;
    private final MexcSubscriptionService mexcSubscriptionService;
    private final MexcOrderDispatchService orderDispatchService;
    private final MexcPrivateUserDataStreamService privateUserDataStreamService;

    @Transactional
    public KillSwitchResponseDTO acionarKillSwitchGlobal() {
        log.warn("🚨 [KILL SWITCH ATIVADO] Iniciando desligamento de emergência global de todos os robôs e ordens!");

        // 1. Buscar todas as operações em andamento
        List<Operacao> ativas = operacaoRepository.findAll().stream()
                .filter(op -> op.getStatus() == StatusOperacoes.EM_ANDAMENTO)
                .toList();

        int totalParadas = 0;

        for (Operacao op : ativas) {
            try {
                // Parar no banco
                op.setStatus(StatusOperacoes.PARADO);
                op.setDataFim(DateUtils.agora());
                operacaoRepository.save(op);

                // Remover do cache Redis
                operacaoCacheService.removerOperacao(op);

                // Desassinar WebSocket público
                mexcSubscriptionService.removeSubscription(op.getPar(), op.getIntervalo());

                // Se conta real, cancelar ordens abertas na exchange
                if (!Boolean.TRUE.equals(op.getModoTeste()) && op.getOperador() != null) {
                    orderDispatchService.cancelarTodasOrdensAbertas(op.getPar(), op.getOperador());
                }

                totalParadas++;
                log.info("[KILL SWITCH] Operação {} ({}) parada com sucesso.", op.getId(), op.getPar());

            } catch (Exception e) {
                log.error("[KILL SWITCH ERRO] Falha ao parar operação {}: {}", op.getId(), e.getMessage(), e);
            }
        }

        // 2. Encerrar todas as conexões WebSocket privadas
        privateUserDataStreamService.desconectarTodos();

        log.warn("🚨 [KILL SWITCH CONCLUÍDO] Total de operações paradas com sucesso: {}", totalParadas);

        return new KillSwitchResponseDTO(
                totalParadas,
                "KILL_SWITCH_EXECUTADO",
                String.format("Desligamento de emergência concluído. %d operações foram desativadas e ordens canceladas.", totalParadas),
                Instant.now()
        );
    }
}
