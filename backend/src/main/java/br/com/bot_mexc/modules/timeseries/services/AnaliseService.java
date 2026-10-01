package br.com.bot_mexc.modules.timeseries.services;
import br.com.bot_mexc.shared.enums.*;
import br.com.bot_mexc.shared.utils.DateUtils;
import br.com.bot_mexc.shared.entities.BaseEntity;
import br.com.bot_mexc.shared.configs.RabbitMQConfig;
import br.com.bot_mexc.modules.timeseries.entities.*;
import br.com.bot_mexc.modules.timeseries.dtos.*;
import br.com.bot_mexc.modules.timeseries.repositories.*;
import br.com.bot_mexc.modules.timeseries.services.*;
import br.com.bot_mexc.modules.timeseries.utils.*;
import br.com.bot_mexc.modules.timeseries.builders.*;
import br.com.bot_mexc.modules.strategy.entities.IndicadorConfig;

import br.com.bot_mexc.modules.timeseries.entities.Analise;
import br.com.bot_mexc.modules.timeseries.repositories.AnaliseRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class AnaliseService {

    private final AnaliseRepository analiseRepository;

    public AnaliseService(AnaliseRepository analiseRepository) {
        this.analiseRepository = analiseRepository;
    }

    @Async("asyncExecutor")
    public void salvarAnaliseAsync(Analise analise) {
        analiseRepository.save(analise);
    }

}