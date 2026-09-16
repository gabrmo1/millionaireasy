package br.com.bot_mexc.services.strategy.impl;

import br.com.bot_mexc.models.enums.TipoIndicador;
import br.com.bot_mexc.utils.constants.IndicadorKeys;
import org.springframework.stereotype.Component;

@Component
class RsiMedioStrategy extends AbstractRsiStrategy {

    @Override
    public TipoIndicador getTipoIndicador() {
        return TipoIndicador.RSI_MEDIO;
    }

    @Override
    protected String getPeriodoParamKey() {
        return IndicadorKeys.PARAM_PERIODO_RSI_MEDIO;
    }
}