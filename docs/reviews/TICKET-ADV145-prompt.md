# TICKET-ADV145 — Claude review prompt (Kafka consumer config)

Paste the blocks below into Claude with this prompt:

---

Review the following Spring Kafka consumer configuration for production
readiness. Flag any missing or risky settings in these areas:
  (1) backpressure & poll tuning,
  (2) error handling, retry & DLQ,
  (3) idempotence and exactly-once semantics,
  (4) observability — metrics, logging, traces,
  (5) security — TLS, SASL, ACLs.

For each finding, give the concrete config key, the recommended value, and a
one-line justification. Do NOT rewrite the whole file — just list findings.

Application context: trade reconciliation service, ~500 events/sec, strict
audit requirements.

=== application.yml (Kafka section) ===

```yaml
spring:
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP:localhost:9092}
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
      properties:
        spring.json.add.type.headers: false
    consumer:
      group-id: reconx-service
      auto-offset-reset: earliest
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring.json.trusted.packages: com.dbtraining.reconx.dto
        spring.json.use.type.headers: false
        spring.json.value.default.type: com.dbtraining.reconx.dto.TradeEvent

management:
  metrics:
    binders:
      kafka:
        enabled: true
    tags:
      application: ${spring.application.name}
```

=== KafkaErrorHandlerConfig.java ===

```java
@Configuration
public class KafkaErrorHandlerConfig {

    @Bean
    public DefaultErrorHandler errorHandler(KafkaOperations<Object, Object> template) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                template,
                (ConsumerRecord<?, ?> rec, Exception ex) ->
                        new TopicPartition(rec.topic() + "-dlq", rec.partition()));

        ExponentialBackOff backoff = new ExponentialBackOff(1000L, 2.0);
        backoff.setMaxElapsedTime(8_000L);

        DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, backoff);
        handler.addNotRetryableExceptions(DeserializationException.class, IllegalArgumentException.class);
        return handler;
    }
}
```

=== KafkaConsumerConfig.java (listener factories) ===

Custom `ConsumerFactory` beans apply `DefaultKafkaConsumerFactoryCustomizer` for Micrometer.
`tradeEventListenerContainerFactory` wires `DefaultErrorHandler`; `systemAlertListenerContainerFactory` does not.
