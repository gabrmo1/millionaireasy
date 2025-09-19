// backend/src/main/java/br/com/bot_mexc/utils/EstrategiaUtils.java
package br.com.bot_mexc.utils;

import br.com.bot_mexc.models.dtos.EstrategiaDTO;
import br.com.bot_mexc.models.entities.Estrategia;
import lombok.experimental.UtilityClass;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.stream.Collectors;

@UtilityClass
public class EstrategiaUtils {

    public static Estrategia converterDtoParaEntidade(EstrategiaDTO dto) {
        Estrategia entidade = new Estrategia();
        atualizarEntidadeComDto(entidade, dto);
        return entidade;
    }

    public static void atualizarEntidadeComDto(Estrategia entidade, EstrategiaDTO dto) {
        entidade.setNome(dto.nome());
        entidade.setUtilizarRsiCurto(dto.utilizarRsiCurto());
        entidade.setPeriodoRsiCurto(dto.periodoRsiCurto());
        entidade.setUtilizarRsiMedio(dto.utilizarRsiMedio());
        entidade.setPeriodoRsiMedio(dto.periodoRsiMedio());
        entidade.setUtilizarRsiLongo(dto.utilizarRsiLongo());
        entidade.setPeriodoRsiLongo(dto.periodoRsiLongo());
        entidade.setUtilizarRsiEstocastico(dto.utilizarRsiEstocastico());
        entidade.setPeriodoRsiEstocastico(dto.periodoRsiEstocastico());
        entidade.setSuavizacaoRsiEstocasticoD(dto.suavizacaoRsiEstocasticoD());
        entidade.setSuavizacaoRsiEstocasticoK(dto.suavizacaoRsiEstocasticoK());
        entidade.setUtilizarEma(dto.utilizarEma());
        entidade.setPeriodoEma(dto.periodoEma());
        entidade.setUtilizarSma(dto.utilizarSma());
        entidade.setPeriodoSma(dto.periodoSma());
        entidade.setRealizarLeituraVolume(dto.realizarLeituraVolume());
        entidade.setValorOperacaoFixo(dto.valorOperacaoFixo());
        entidade.setTipoMoedaValorOperacao(dto.tipoMoedaValorOperacao());
        entidade.setPercentualValorOperacao(dto.percentualValorOperacao());
        entidade.setVendaApenasPorLucro(dto.vendaApenasPorLucro());
        entidade.setPercentualLucro(dto.percentualLucro());
    }

    public static EstrategiaDTO converterEntidadeParaDto(Estrategia entidade) {
        if (entidade == null) {
            return null;
        }

        return EstrategiaDTO.builder()
                .id(entidade.getId())
                .nome(entidade.getNome())
                .utilizarRsiCurto(entidade.getUtilizarRsiCurto())
                .periodoRsiCurto(entidade.getPeriodoRsiCurto())
                .utilizarRsiMedio(entidade.getUtilizarRsiMedio())
                .periodoRsiMedio(entidade.getPeriodoRsiMedio())
                .utilizarRsiLongo(entidade.getUtilizarRsiLongo())
                .periodoRsiLongo(entidade.getPeriodoRsiLongo())
                .utilizarRsiEstocastico(entidade.getUtilizarRsiEstocastico())
                .periodoRsiEstocastico(entidade.getPeriodoRsiEstocastico())
                .suavizacaoRsiEstocasticoD(entidade.getSuavizacaoRsiEstocasticoD())
                .suavizacaoRsiEstocasticoK(entidade.getSuavizacaoRsiEstocasticoK())
                .utilizarEma(entidade.getUtilizarEma())
                .periodoEma(entidade.getPeriodoEma())
                .utilizarSma(entidade.getUtilizarSma())
                .periodoSma(entidade.getPeriodoSma())
                .realizarLeituraVolume(entidade.getRealizarLeituraVolume())
                .valorOperacaoFixo(entidade.getValorOperacaoFixo())
                .tipoMoedaValorOperacao(entidade.getTipoMoedaValorOperacao())
                .percentualValorOperacao(entidade.getPercentualValorOperacao())
                .vendaApenasPorLucro(entidade.getVendaApenasPorLucro())
                .percentualLucro(entidade.getPercentualLucro())
                .condicoesCompra(
                        !CollectionUtils.isEmpty(entidade.getCondicoesCompra()) ?
                                entidade.getCondicoesCompra().stream().map(CondicaoCompraUtils::converterEntidadeParaDto).collect(Collectors.toList()) :
                                Collections.emptyList()
                )
                .condicoesVenda(
                        !CollectionUtils.isEmpty(entidade.getCondicoesVenda()) ?
                                entidade.getCondicoesVenda().stream().map(CondicaoVendaUtils::converterEntidadeParaDto).collect(Collectors.toList()) :
                                Collections.emptyList()
                )
                .build();
    }
}