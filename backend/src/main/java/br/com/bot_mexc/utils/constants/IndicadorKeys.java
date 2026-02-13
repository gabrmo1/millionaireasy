package br.com.bot_mexc.utils.constants;

import lombok.experimental.UtilityClass;

@UtilityClass
public class IndicadorKeys {

    // --- Parâmetros de Configuração (JSON/Frontend) ---
    public static final String PARAM_PERIODO_EMA = "periodoEma";
    public static final String PARAM_PERIODO_SMA = "periodoSma";
    public static final String PARAM_PERIODO_RSI_CURTO = "periodoRsiCurto";
    public static final String PARAM_PERIODO_RSI_MEDIO = "periodoRsiMedio";
    public static final String PARAM_PERIODO_RSI_LONGO = "periodoRsiLongo";
    public static final String PARAM_PERIODO_RSI_ESTOCASTICO = "periodoRsiEstocastico";
    public static final String PARAM_SUAVIZACAO_K = "suavizacaoRsiEstocasticoK";
    public static final String PARAM_SUAVIZACAO_D = "suavizacaoRsiEstocasticoD";

    // --- Chaves de Contexto/Resultados (Cálculo em Tempo Real) ---
    public static final String RESULT_PRECO_FECHAMENTO = "PRECO_FECHAMENTO";
    public static final String RESULT_PREVIOUS_PREFIX = "PREVIOUS_";

    // --- Nomes de Exibição dos Indicadores (Enviados ao Frontend no Monitoramento) ---
    public static final String NAME_EMA = "EMA";
    public static final String NAME_SMA = "SMA";
    public static final String NAME_RSI_CURTO = "RSI_Curto";
    public static final String NAME_RSI_MEDIO = "RSI_Medio";
    public static final String NAME_RSI_LONGO = "RSI_Longo";
    public static final String NAME_RSI_STOCH_K = "RSI_Stoch_K";
    public static final String NAME_RSI_STOCH_D = "RSI_Stoch_D";

    // --- Chaves Internas de Busca (Contexto de Monitoramento/Repository) ---
    public static final String KEY_RSI_CURTO = "RSI_CURTO";
    public static final String KEY_RSI_MEDIO = "RSI_MEDIO";
    public static final String KEY_RSI_LONGO = "RSI_LONGO";
    public static final String KEY_RSI_STOCH = "RSI_STOCH";
    public static final String KEY_STOCH_K = "STOCH_K";
    public static final String KEY_STOCH_D = "STOCH_D";
    public static final String KEY_EMA = "EMA";
    public static final String KEY_SMA = "SMA";
}