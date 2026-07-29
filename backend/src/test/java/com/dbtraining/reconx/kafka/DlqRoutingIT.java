package com.dbtraining.reconx.kafka;

import com.dbtraining.reconx.dto.TradeEvent;
import com.dbtraining.reconx.service.ReconciliationEngine;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.apache.kafka.clients.consumer.ConsumerConfig.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/** TICKET-ADV135 / ADV144 — runtime failures retry then route to trade-events-dlq. */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@EmbeddedKafka(
        partitions = 3,
        topics = {
                KafkaTopicsConfig.TRADE_EVENTS,
                KafkaTopicsConfig.TRADE_EVENTS_DLQ,
                KafkaTopicsConfig.SYSTEM_ALERTS,
                KafkaTopicsConfig.RECON_RESULTS
        })
@ActiveProfiles("dev")
class DlqRoutingIT {

    private static final int RECON_PARTITION = 0;

    @Autowired KafkaTemplate<String, TradeEvent> kafkaTemplate;
    @Autowired org.springframework.kafka.test.EmbeddedKafkaBroker embeddedKafka;
    @MockBean ReconciliationEngine reconEngine;

    @Test
    void failingConsumerRetriesThenRoutesToDlq() throws Exception {
        String tradeRef = "TRD-DLQ-" + System.currentTimeMillis();
        AtomicInteger attempts = new AtomicInteger();
        Mockito.doAnswer(inv -> {
            attempts.incrementAndGet();
            throw new RuntimeException("boom");
        }).when(reconEngine).scheduleRecon(Mockito.anyString());

        Instant start = Instant.now();
        TradeEvent event = TradeEvent.created(
                tradeRef,
                JsonNodeFactory.instance.objectNode().put("status", "PENDING"));
        kafkaTemplate.send(KafkaTopicsConfig.TRADE_EVENTS, RECON_PARTITION, tradeRef, event)
                .get(5, TimeUnit.SECONDS);

        await().atMost(Duration.ofSeconds(25))
                .untilAsserted(() -> {
                    assertThat(attempts.get()).isGreaterThanOrEqualTo(3);
                    assertThat(dlqContains(tradeRef)).isTrue();
                });

        assertThat(Duration.between(start, Instant.now()).toMillis()).isGreaterThanOrEqualTo(6_000L);
    }

    private boolean dlqContains(String tradeRef) {
        Properties p = new Properties();
        p.put(BOOTSTRAP_SERVERS_CONFIG, embeddedKafka.getBrokersAsString());
        p.put(GROUP_ID_CONFIG, "dlq-assert-" + System.nanoTime());
        p.put(AUTO_OFFSET_RESET_CONFIG, "earliest");
        p.put(KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        p.put(VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        TopicPartition dlqPartition =
                new TopicPartition(KafkaTopicsConfig.TRADE_EVENTS_DLQ, RECON_PARTITION);

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(p)) {
            consumer.assign(List.of(dlqPartition));
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(3));
            for (ConsumerRecord<String, String> record : records) {
                if (record.value() != null && record.value().contains(tradeRef)) {
                    return true;
                }
            }
        }
        return false;
    }
}
