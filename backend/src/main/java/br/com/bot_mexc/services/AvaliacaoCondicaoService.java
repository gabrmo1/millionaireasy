package br.com.bot_mexc.services;

import br.com.bot_mexc.models.entities.CondicaoCompra;
import br.com.bot_mexc.models.entities.CondicaoVenda;
import br.com.bot_mexc.models.enums.OperadorComparacao;
import br.com.bot_mexc.models.enums.OperadorLogico;
import br.com.bot_mexc.models.enums.TipoOperando;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class AvaliacaoCondicaoService {

    public boolean avaliarCondicoesCompra(List<CondicaoCompra> condicoes, Map<String, BigDecimal> indicadores) {
        if (CollectionUtils.isEmpty(condicoes) || indicadores == null)
            return false;

        condicoes.sort(Comparator.comparing(CondicaoCompra::getOrdem));

        boolean resultadoGrupo = true;
        boolean resultadoFinal = false;

        for (int i = 0; i < condicoes.size(); i++) {
            CondicaoCompra c = condicoes.get(i);
            boolean resultadoCondicao = comparar(
                    c.getOperandoATipo(), c.getOperandoAReferencia(), c.getOperandoAValor(),
                    c.getOperandoBTipo(), c.getOperandoBReferencia(), c.getOperandoBValor(),
                    c.getOperador(), indicadores
            );

            if (i == 0 || c.getOperadorParaProxima() == null || c.getOperadorParaProxima() == OperadorLogico.AND) {
                resultadoGrupo = resultadoGrupo && resultadoCondicao;
            }

            if (c.getOperadorParaProxima() == OperadorLogico.OR || i == condicoes.size() - 1) {
                resultadoFinal = resultadoFinal || resultadoGrupo;
                resultadoGrupo = true;
            }
        }
        return resultadoFinal;
    }

    public boolean avaliarCondicoesVenda(List<CondicaoVenda> condicoes, Map<String, BigDecimal> indicadores) {
        if (CollectionUtils.isEmpty(condicoes) || indicadores == null)
            return false;

        condicoes.sort(Comparator.comparing(CondicaoVenda::getOrdem));

        boolean resultadoGrupo = true;
        boolean resultadoFinal = false;

        for (int i = 0; i < condicoes.size(); i++) {
            CondicaoVenda c = condicoes.get(i);
            boolean resultadoCondicao = comparar(
                    c.getOperandoATipo(), c.getOperandoAReferencia(), c.getOperandoAValor(),
                    c.getOperandoBTipo(), c.getOperandoBReferencia(), c.getOperandoBValor(),
                    c.getOperador(), indicadores
            );

            if (i == 0 || c.getOperadorParaProxima() == null || c.getOperadorParaProxima() == OperadorLogico.AND) {
                resultadoGrupo = resultadoGrupo && resultadoCondicao;
            }

            if (c.getOperadorParaProxima() == OperadorLogico.OR || i == condicoes.size() - 1) {
                resultadoFinal = resultadoFinal || resultadoGrupo;
                resultadoGrupo = true;
            }
        }
        return resultadoFinal;
    }

    private boolean comparar(TipoOperando tipoA, String refA, BigDecimal valA,
                             TipoOperando tipoB, String refB, BigDecimal valB,
                             OperadorComparacao operador,
                             Map<String, BigDecimal> indicadores) {

        BigDecimal valorA = getValor(tipoA, refA, valA, indicadores);
        BigDecimal valorB = getValor(tipoB, refB, valB, indicadores);

        if (valorA == null || valorB == null) {
            // Se não temos os valores atuais, impossível avaliar
            return false;
        }

        int comparacaoAtual = valorA.compareTo(valorB);

        switch (operador) {
            case MAIOR_QUE:
                return comparacaoAtual > 0;
            case MENOR_QUE:
                return comparacaoAtual < 0;
            case CRUZOU_PARA_CIMA: {
                BigDecimal prevA = getValorAnterior(tipoA, refA, valA, indicadores);
                BigDecimal prevB = getValorAnterior(tipoB, refB, valB, indicadores);

                if (prevA == null || prevB == null) {
                    log.debug("Dados anteriores insuficientes para avaliar 'Cruzou para Cima'. RefA: {}, RefB: {}", refA, refB);
                    return false;
                }

                return prevA.compareTo(prevB) < 0 && comparacaoAtual >= 0;
            }
            case CRUZOU_PARA_BAIXO: {
                // Lógica: AnteriorA > AnteriorB  E  AtualA <= AtualB
                BigDecimal prevA = getValorAnterior(tipoA, refA, valA, indicadores);
                BigDecimal prevB = getValorAnterior(tipoB, refB, valB, indicadores);

                if (prevA == null || prevB == null) {
                    log.debug("Dados anteriores insuficientes para avaliar 'Cruzou para Baixo'. RefA: {}, RefB: {}", refA, refB);
                    return false;
                }

                return prevA.compareTo(prevB) > 0 && comparacaoAtual <= 0;
            }
            default:
                return false;
        }
    }

    private BigDecimal getValor(TipoOperando tipo, String referencia, BigDecimal valorFixo, Map<String, BigDecimal> indicadores) {
        return switch (tipo) {
            case VALOR_FIXO -> valorFixo;
            case INDICADOR -> indicadores.get(referencia);
            case PRECO_FECHAMENTO -> indicadores.get("PRECO_FECHAMENTO");
            default -> null;
        };
    }

    private BigDecimal getValorAnterior(TipoOperando tipo, String referencia, BigDecimal valorFixo, Map<String, BigDecimal> indicadores) {
        return switch (tipo) {
            case VALOR_FIXO -> valorFixo; // Valor fixo mantém-se o mesmo no passado
            case INDICADOR -> indicadores.get("PREVIOUS_" + referencia);
            case PRECO_FECHAMENTO -> indicadores.get("PREVIOUS_PRECO_FECHAMENTO");
            default -> null;
        };
    }
}