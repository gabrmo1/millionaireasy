package br.com.bot_mexc.services;

import br.com.bot_mexc.models.entities.Analise;
import br.com.bot_mexc.repositories.AnaliseRepository;
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