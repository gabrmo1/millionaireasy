package br.com.bot_mexc.models.dtos;

import br.com.bot_mexc.models.entities.CondicaoCompra;
import br.com.bot_mexc.models.entities.CondicaoVenda;
import br.com.bot_mexc.models.entities.IndicadorConfig;
import br.com.bot_mexc.models.entities.Operacao;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OperacaoCacheDTO(
        String id,
        String par,
        String intervalo,
        String operadorId,
        String estrategiaId,
        Boolean modoTeste,
        BigDecimal saldo,

        boolean posicionado,
        BigDecimal precoMedioEntrada,
        BigDecimal volumeEmMao,

        BigDecimal valorOperacaoFixo,
        BigDecimal percentualValorOperacao,
        String stablecoin,
        Boolean vendaApenasPorLucro,
        BigDecimal percentualLucro,

        List<IndicadorConfig> indicadores,
        List<CondicaoCompra> condicoesCompra,
        List<CondicaoVenda> condicoesVenda
) {
    public static OperacaoCacheDTO fromEntity(Operacao operacao, BigDecimal saldo, boolean posicionado, BigDecimal precoEntrada, BigDecimal volume) {
        return new OperacaoCacheDTO(
                operacao.getId(),
                operacao.getPar(),
                operacao.getIntervalo(),
                operacao.getOperador() != null ? operacao.getOperador().getId() : null,
                operacao.getEstrategia().getId(),
                operacao.getModoTeste(),
                saldo,
                posicionado,
                precoEntrada,
                volume,
                operacao.getEstrategia().getValorOperacaoFixo(),
                operacao.getEstrategia().getPercentualValorOperacao(),
                operacao.getEstrategia().getStablecoin(),
                operacao.getEstrategia().getVendaApenasPorLucro(),
                operacao.getEstrategia().getPercentualLucro(),
                new ArrayList<>(operacao.getEstrategia().getIndicadoresConfig()),
                new ArrayList<>(operacao.getEstrategia().getCondicoesCompra()),
                new ArrayList<>(operacao.getEstrategia().getCondicoesVenda())
        );
    }
}