package com.dbtraining.reconx.kafka;

import com.dbtraining.reconx.dto.SystemAlert;
import com.dbtraining.reconx.dto.TradeEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.autoconfigure.kafka.DefaultKafkaConsumerFactoryCustomizer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

/** TICKET-ADV133 / ADV134 — Kafka listener container factories. */
@Configuration
public class KafkaConsumerConfig {

    @Bean
    public ConsumerFactory<String, TradeEvent> tradeEventConsumerFactory(
            KafkaProperties props, DefaultKafkaConsumerFactoryCustomizer customizer) {
        Map<String, Object> config = new HashMap<>(props.buildConsumerProperties(null));
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        config.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class);
        config.put(JsonDeserializer.TRUSTED_PACKAGES, "com.dbtraining.reconx.dto");
        config.put(JsonDeserializer.VALUE_DEFAULT_TYPE, TradeEvent.class.getName());
        config.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        DefaultKafkaConsumerFactory<String, TradeEvent> factory = new DefaultKafkaConsumerFactory<>(config);
        customizer.customize(factory);
        return factory;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, TradeEvent> tradeEventListenerContainerFactory(
            ConsumerFactory<String, TradeEvent> tradeEventConsumerFactory,
            DefaultErrorHandler errorHandler) {
        ConcurrentKafkaListenerContainerFactory<String, TradeEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(tradeEventConsumerFactory);
        factory.setCommonErrorHandler(errorHandler);
        return factory;
    }

    @Bean
    public ConsumerFactory<String, SystemAlert> systemAlertConsumerFactory(
            KafkaProperties props, DefaultKafkaConsumerFactoryCustomizer customizer) {
        Map<String, Object> config = new HashMap<>(props.buildConsumerProperties(null));
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        config.put(JsonDeserializer.TRUSTED_PACKAGES, "com.dbtraining.reconx.dto");
        config.put(JsonDeserializer.VALUE_DEFAULT_TYPE, SystemAlert.class.getName());
        config.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        DefaultKafkaConsumerFactory<String, SystemAlert> factory = new DefaultKafkaConsumerFactory<>(config);
        customizer.customize(factory);
        return factory;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, SystemAlert> systemAlertListenerContainerFactory(
            ConsumerFactory<String, SystemAlert> systemAlertConsumerFactory,
            DefaultErrorHandler errorHandler) {
        ConcurrentKafkaListenerContainerFactory<String, SystemAlert> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(systemAlertConsumerFactory);
        factory.setCommonErrorHandler(errorHandler);
        return factory;
    }
}
