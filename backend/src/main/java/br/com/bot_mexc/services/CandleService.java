package br.com.bot_mexc.services;

import br.com.bot_mexc.models.dtos.CandleDTO;
import br.com.bot_mexc.models.entities.Candle;
import br.com.bot_mexc.repositories.CandleRepository;
import br.com.bot_mexc.utils.CandleUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CandleService {

    private final CandleRepository candleRepository;

    public CandleService(CandleRepository candleRepository) {
        this.candleRepository = candleRepository;
    }

    @Async("asyncExecutor")
    public void salvarCandlesAsync(List<CandleDTO> candlesDto, String par, String intervalo) {
        final var optionalUltimoCandle = candleRepository.findTopByParAndIntervaloOrderByDataFechamentoDesc(par, intervalo);
        List<Candle> candlesEntity;

        if (optionalUltimoCandle.isPresent()) {
            final var ultimoCandle = optionalUltimoCandle.get();
            final var penultimoCandleDTO = candlesDto.get(candlesDto.size() - 2);
            final var penultimoCandleOptional = candleRepository.findByParAndIntervaloAndDataFechamento(par, intervalo, penultimoCandleDTO.closeTime());

            //Atualiza o volume do candle em aberto
            candlesDto.stream()
                    .filter(c -> c.closeTime().equals(ultimoCandle.getDataFechamento()))
                    .findFirst()
                    .ifPresent(candleAberto -> {
                        ultimoCandle.setVolume(candleAberto.volume());
                        candleRepository.save(ultimoCandle);
                    });

            if (penultimoCandleOptional.isPresent()) {
                final var penultimoCandle = penultimoCandleOptional.get();

                if (!penultimoCandle.getVolume().equals(penultimoCandleDTO.volume())) {
                    penultimoCandle.setVolume(penultimoCandleDTO.volume());
                    candleRepository.save(penultimoCandle);
                }
            }

            //Adiciona apenas candles mais recentes que o último salvo
            candlesEntity = candlesDto.stream()
                    .filter(c -> c.closeTime().isAfter(ultimoCandle.getDataFechamento()))
                    .map(c -> CandleUtils.converterDtoParaEntidade(c, par, intervalo))
                    .toList();

        } else {
            //Não existe candle ainda -> salvar todos
            candlesEntity = candlesDto.stream()
                    .map(c -> CandleUtils.converterDtoParaEntidade(c, par, intervalo))
                    .toList();
        }

        if (!candlesEntity.isEmpty())
            candleRepository.saveAll(candlesEntity);
    }

}
