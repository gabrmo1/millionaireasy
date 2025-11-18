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

    private final CandleRepository repository;

    public CandleService(CandleRepository repository) {
        this.repository = repository;
    }

    @Async("asyncExecutor")
    public void salvarCandlesAsync(List<CandleDTO> candlesDto, String par, String intervalo) {
        final var optionalUltimoCandle = repository.findTopByParAndIntervaloOrderByDataFechamentoDesc(par, intervalo);
        List<Candle> candlesEntity;

        if (optionalUltimoCandle.isPresent()) {
            final var ultimoCandle = optionalUltimoCandle.get();
            final var penultimoCandleDTO = candlesDto.get(candlesDto.size() - 2);
            final var penultimoCandleOptional = repository.findByParAndIntervaloAndDataFechamento(par, intervalo, penultimoCandleDTO.closeTime());

            //Atualiza o volume do candle em aberto
            candlesDto.stream()
                    .filter(c -> c.closeTime().equals(ultimoCandle.getDataFechamento()))
                    .findFirst()
                    .ifPresent(candleAberto -> {
                        ultimoCandle.setVolume(candleAberto.volume());
                        repository.save(ultimoCandle);
                    });

            if (penultimoCandleOptional.isPresent()) {
                final var penultimoCandle = penultimoCandleOptional.get();

                if (!penultimoCandle.getVolume().equals(penultimoCandleDTO.volume())) {
                    penultimoCandle.setVolume(penultimoCandleDTO.volume());
                    repository.save(penultimoCandle);
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
            repository.saveAll(candlesEntity);
    }

    @Async("asyncExecutor")
    public void salvarCandleWebsocket(CandleDTO candleDto, String par, String intervalo) {
        final var candleEntity = CandleUtils.converterDtoParaEntidade(candleDto, par, intervalo);
        repository.save(candleEntity);
    }

}
