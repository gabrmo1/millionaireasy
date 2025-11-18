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
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class AvaliacaoCondicaoService {

    private interface Condicao {
        Integer getOrdem();
        OperadorLogico getOperadorParaProxima();
        TipoOperando getOperandoATipo();
        String getOperandoAReferencia();
        BigDecimal getOperandoAValor();
        OperadorComparacao getOperador();
        TipoOperando getOperandoBTipo();
        String getOperandoBReferencia();
        BigDecimal getOperandoBValor();
    }

    public boolean avaliarCondicoesCompra(Set<CondicaoCompra> condicoes, Map<String, BigDecimal> indicadores) {
        if (CollectionUtils.isEmpty(condicoes)) {
            return false;
        }
        List<Condicao> listaCondicoes = condicoes.stream()
                .map(c -> (Condicao) (new Condicao() {
                    public Integer getOrdem() { return c.getOrdem(); }
                    public OperadorLogico getOperadorParaProxima() { return c.getOperadorParaProxima(); }
                    public TipoOperando getOperandoATipo() { return c.getOperandoATipo(); }
                    public String getOperandoAReferencia() { return c.getOperandoAReferencia(); }
                    public BigDecimal getOperandoAValor() { return c.getOperandoAValor(); }
                    public OperadorComparacao getOperador() { return c.getOperador(); }
                    public TipoOperando getOperandoBTipo() { return c.getOperandoBTipo(); }
                    public String getOperandoBReferencia() { return c.getOperandoBReferencia(); }
                    public BigDecimal getOperandoBValor() { return c.getOperandoBValor(); }
                }))
                .sorted(Comparator.comparing(Condicao::getOrdem))
                .toList();

        return avaliar(listaCondicoes, indicadores);
    }

    public boolean avaliarCondicoesVenda(Set<CondicaoVenda> condicoes, Map<String, BigDecimal> indicadores) {
        if (CollectionUtils.isEmpty(condicoes)) {
            return false;
        }
        List<Condicao> listaCondicoes = condicoes.stream()
                .map(c -> (Condicao) (new Condicao() {
                    public Integer getOrdem() { return c.getOrdem(); }
                    public OperadorLogico getOperadorParaProxima() { return c.getOperadorParaProxima(); }
                    public TipoOperando getOperandoATipo() { return c.getOperandoATipo(); }
                    public String getOperandoAReferencia() { return c.getOperandoAReferencia(); }
                    public BigDecimal getOperandoAValor() { return c.getOperandoAValor(); }
                    public OperadorComparacao getOperador() { return c.getOperador(); }
                    public TipoOperando getOperandoBTipo() { return c.getOperandoBTipo(); }
                    public String getOperandoBReferencia() { return c.getOperandoBReferencia(); }
                    public BigDecimal getOperandoBValor() { return c.getOperandoBValor(); }
                }))
                .sorted(Comparator.comparing(Condicao::getOrdem))
                .toList();

        return avaliar(listaCondicoes, indicadores);
    }

    private boolean avaliar(List<Condicao> condicoes, Map<String, BigDecimal> indicadores) {
        boolean resultadoGrupo = true;
        boolean resultadoFinal = false;

        for (int i = 0; i < condicoes.size(); i++) {
            Condicao c = condicoes.get(i);
            boolean resultadoCondicao = comparar(c, indicadores);

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

    private boolean comparar(Condicao c, Map<String, BigDecimal> indicadores) {
        BigDecimal valA = getValor(c.getOperandoATipo(), c.getOperandoAReferencia(), c.getOperandoAValor(), indicadores);
        BigDecimal valB = getValor(c.getOperandoBTipo(), c.getOperandoBReferencia(), c.getOperandoBValor(), indicadores);

        if (valA == null || valB == null) {
            log.warn("Comparação pulada: Valor Nulo (A: {}, B: {})", valA, valB);
            return false;
        }

        int comparacao = valA.compareTo(valB);

        switch (c.getOperador()) {
            case MAIOR_QUE:
                return comparacao > 0;
            case MENOR_QUE:
                return comparacao < 0;
            // TODO: CRUZOU_PARA_CIMA e CRUZOU_PARA_BAIXO exigem estado anterior,
            // o que é muito mais complexo e precisa ser implementado no CalculoIndicadorService.
            // Por enquanto, trataremos como MAIOR/MENOR.
            case CRUZOU_PARA_CIMA:
                log.warn("Tratando CRUZOU_PARA_CIMA como MAIOR_QUE (Implementação pendente)");
                return comparacao > 0;
            case CRUZOU_PARA_BAIXO:
                log.warn("Tratando CRUZOU_PARA_BAIXO como MENOR_QUE (Implementação pendente)");
                return comparacao < 0;
            default:
                return false;
        }
    }

    private BigDecimal getValor(TipoOperando tipo, String referencia, BigDecimal valorFixo, Map<String, BigDecimal> indicadores) {
        switch (tipo) {
            case VALOR_FIXO:
                return valorFixo;
            case INDICADOR:
                return indicadores.get(referencia);
            case PRECO_FECHAMENTO:
                return indicadores.get("PRECO_FECHAMENTO");
            default:
                return null;
        }
    }
}