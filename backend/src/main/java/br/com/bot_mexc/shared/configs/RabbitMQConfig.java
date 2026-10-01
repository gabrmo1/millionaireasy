package br.com.bot_mexc.shared.configs;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class RabbitMQConfig {

    // --- Live Data Exchange & Queues ---
    public static final String MEXC_DATA_TOPIC = "mexc.data.topic";
    public static final String KLINE_ROUTING_KEY_PREFIX = "kline.data";
    public static final String KLINE_ROUTING_KEY_PATTERN = "kline.data.#";

    public static final String KLINE_ANALYSIS_QUEUE = "analysis.kline.queue";
    public static final String KLINE_ANALYSIS_DLX = "analysis.kline.dlx";
    public static final String KLINE_ANALYSIS_DLQ = "analysis.kline.dlq";
    private static final String KLINE_ANALYSIS_DLQ_ROUTING_KEY = "failed.analysis.kline";

    // --- Live Orders Exchange & Queues ---
    public static final String ORDERS_ACTIONS_TOPIC = "orders.actions.topic";
    public static final String ORDER_EXECUTE_ROUTING_KEY_PATTERN = "order.execute.#";

    public static final String ORDERS_SIMULATE_QUEUE = "orders.simulate.queue";
    public static final String ORDERS_SIMULATE_DLX = "orders.simulate.dlx";
    public static final String ORDERS_SIMULATE_DLQ = "orders.simulate.dlq";
    private static final String ORDERS_SIMULATE_DLQ_ROUTING_KEY = "failed.simulate.order";

    // --- Backtest & Simulation Queues (Novas Filas) ---
    public static final String SIMULATIONS_PROCESS_QUEUE = "simulations.process.queue";
    public static final String SIMULATIONS_PROCESS_DLX = "simulations.process.dlx";
    public static final String SIMULATIONS_PROCESS_DLQ = "simulations.process.dlq";
    private static final String SIMULATIONS_PROCESS_DLQ_ROUTING_KEY = "failed.process.simulation";

    public static final String CANDLES_PERSIST_QUEUE = "candles.persist.queue";
    public static final String CANDLES_PERSIST_DLX = "candles.persist.dlx";
    public static final String CANDLES_PERSIST_DLQ = "candles.persist.dlq";
    private static final String CANDLES_PERSIST_DLQ_ROUTING_KEY = "failed.persist.candle";

    // --- Beans de Configuração Base ---
    @Bean
    public MessageConverter messageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter);
        return rabbitTemplate;
    }

    // --- Beans: Live Data ---
    @Bean
    public TopicExchange mexcDataTopic() {
        return new TopicExchange(MEXC_DATA_TOPIC);
    }

    @Bean
    public FanoutExchange klineAnalysisDlx() {
        return new FanoutExchange(KLINE_ANALYSIS_DLX);
    }

    @Bean
    public Queue klineAnalysisDlq() {
        return new Queue(KLINE_ANALYSIS_DLQ, true);
    }

    @Bean
    public Binding klineAnalysisDlqBinding() {
        return BindingBuilder.bind(klineAnalysisDlq()).to(klineAnalysisDlx());
    }

    @Bean
    public Queue klineAnalysisQueue() {
        return QueueBuilder.durable(KLINE_ANALYSIS_QUEUE)
                .withArguments(Map.of(
                        "x-dead-letter-exchange", KLINE_ANALYSIS_DLX,
                        "x-dead-letter-routing-key", KLINE_ANALYSIS_DLQ_ROUTING_KEY
                ))
                .build();
    }

    @Bean
    public Binding klineAnalysisBinding() {
        return BindingBuilder
                .bind(klineAnalysisQueue())
                .to(mexcDataTopic())
                .with(KLINE_ROUTING_KEY_PATTERN);
    }

    // --- Beans: Live Orders ---
    @Bean
    public TopicExchange ordersActionsTopic() {
        return new TopicExchange(ORDERS_ACTIONS_TOPIC);
    }

    @Bean
    public FanoutExchange ordersSimulateDlx() {
        return new FanoutExchange(ORDERS_SIMULATE_DLX);
    }

    @Bean
    public Queue ordersSimulateDlq() {
        return new Queue(ORDERS_SIMULATE_DLQ, true);
    }

    @Bean
    public Binding ordersSimulateDlqBinding() {
        return BindingBuilder.bind(ordersSimulateDlq()).to(ordersSimulateDlx());
    }

    @Bean
    public Queue ordersSimulateQueue() {
        return QueueBuilder.durable(ORDERS_SIMULATE_QUEUE)
                .withArguments(Map.of(
                        "x-dead-letter-exchange", ORDERS_SIMULATE_DLX,
                        "x-dead-letter-routing-key", ORDERS_SIMULATE_DLQ_ROUTING_KEY
                ))
                .build();
    }

    @Bean
    public Binding ordersSimulateBinding() {
        return BindingBuilder
                .bind(ordersSimulateQueue())
                .to(ordersActionsTopic())
                .with(ORDER_EXECUTE_ROUTING_KEY_PATTERN);
    }

    // --- Beans: Backtest & Simulation ---
    @Bean
    public FanoutExchange simulationsProcessDlx() {
        return new FanoutExchange(SIMULATIONS_PROCESS_DLX);
    }

    @Bean
    public Queue simulationsProcessDlq() {
        return new Queue(SIMULATIONS_PROCESS_DLQ, true);
    }

    @Bean
    public Binding simulationsProcessDlqBinding() {
        return BindingBuilder.bind(simulationsProcessDlq()).to(simulationsProcessDlx());
    }

    @Bean
    public Queue simulationsProcessQueue() {
        return QueueBuilder.durable(SIMULATIONS_PROCESS_QUEUE)
                .withArguments(Map.of(
                        "x-dead-letter-exchange", SIMULATIONS_PROCESS_DLX,
                        "x-dead-letter-routing-key", SIMULATIONS_PROCESS_DLQ_ROUTING_KEY
                ))
                .build();
    }

    // --- Beans: Candles Persist ---
    @Bean
    public FanoutExchange candlesPersistDlx() {
        return new FanoutExchange(CANDLES_PERSIST_DLX);
    }

    @Bean
    public Queue candlesPersistDlq() {
        return new Queue(CANDLES_PERSIST_DLQ, true);
    }

    @Bean
    public Binding candlesPersistDlqBinding() {
        return BindingBuilder.bind(candlesPersistDlq()).to(candlesPersistDlx());
    }

    @Bean
    public Queue candlesPersistQueue() {
        return QueueBuilder.durable(CANDLES_PERSIST_QUEUE)
                .withArguments(Map.of(
                        "x-dead-letter-exchange", CANDLES_PERSIST_DLX,
                        "x-dead-letter-routing-key", CANDLES_PERSIST_DLQ_ROUTING_KEY
                ))
                .build();
    }
}