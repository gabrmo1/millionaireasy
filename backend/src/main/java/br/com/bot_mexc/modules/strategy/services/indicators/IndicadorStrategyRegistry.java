package br.com.bot_mexc.modules.strategy.services.indicators;

import br.com.bot_mexc.shared.enums.TipoIndicador;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Registry Pattern Otimizado.
 * O Fail-Fast no construtor garante que a aplicação não inicie
 * caso exista algum TipoIndicador sem a respectiva Strategy implementada.
 */
@Component
public class IndicadorStrategyRegistry {

    private final Map<TipoIndicador, IndicadorStrategy> registry = new EnumMap<>(TipoIndicador.class);

    public IndicadorStrategyRegistry(List<IndicadorStrategy> strategies) {
        for (IndicadorStrategy strategy : strategies) {
            registry.put(strategy.getTipoIndicador(), strategy);
            if (strategy.getTipoIndicador() == TipoIndicador.RSI_ESTOCASTICO_K) {
                registry.put(TipoIndicador.RSI_ESTOCASTICO_D, strategy);
            }
        }

        // Fail-Fast: Validação arquitetural no Boot (Garante a ausência de NPE nos consumers)
        for (TipoIndicador tipo : TipoIndicador.values()) {
            if (!registry.containsKey(tipo)) {
                throw new IllegalStateException("Falha Crítica de Arquitetura: A estratégia para o indicador " + tipo + " não foi implementada ou injetada no Registry.");
            }
        }
    }

    public IndicadorStrategy get(TipoIndicador tipo) {
        return registry.get(tipo); // O(1) puro no EnumMap. Totalmente seguro graças ao Fail-Fast.
    }
}