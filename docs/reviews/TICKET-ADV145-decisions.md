# TICKET-ADV145 — Kafka consumer config review decisions

Team review of Claude findings for production readiness.  
Context: trade reconciliation service, ~500 events/sec peak, strict audit trail requirements.

## Findings and team decisions

| # | Area | Finding | Recommendation | Decision | Rationale |
|---|------|---------|----------------|----------|-----------|
| 1 | Backpressure | Default `max.poll.records` (500) risks exceeding `max.poll.interval.ms` when recon/audit DB I/O is slow | `spring.kafka.consumer.properties.max.poll.records: 100` | **Accept** | Applied — smaller batches keep poll loop within interval at our per-record DB latency |
| 2 | Backpressure | `spring.kafka.listener.concurrency` not set; single-threaded container won't sustain 500 evt/s | Set explicitly to 3 (match partition count) | **Defer** | Current load is demo-scale; tune after load test on Day 10 with partition metrics |
| 3 | Error handling | `ExponentialBackOff` max elapsed time is 8s — too short for broker leader election or DB blip | Increase to 60s or make configurable | **Defer** | ADV144 IT proves current budget works for DLQ path; prod tuning tracked separately |
| 4 | Error handling | `ExponentialBackOff` has no jitter — retry stampede after outage | Add jitter via custom `BackOff` | **Defer** | Known gap; backlog item for Day 10 hardening |
| 5 | Error handling | `systemAlertListenerContainerFactory` has no `DefaultErrorHandler` | Wire same error handler as trade listeners | **Accept** | Valid gap — alert listener should not infinite-retry poison pills without DLQ |
| 6 | Error handling | DLQ `TopicPartition(rec.topic()+"-dlq", rec.partition())` assumes equal partition counts | Use `-1` (RoundRobin) or guarantee DLQ ≥ source partitions | **Reject** | ADV128 pre-declares DLQ with same 3 partitions as `trade-events`; documented in `KafkaTopicsConfig` |
| 7 | Idempotence | Producer `enable.idempotence` not explicit | `spring.kafka.producer.properties.enable.idempotence: true` | **Accept** | Applied — cheap guard against duplicate trade events on producer retry |
| 8 | Idempotence | `transaction-id-prefix` + `read_committed` needed for full EOS | Add transactional producer + `KafkaTransactionManager` | **Reject** | At-least-once + audit dedup by `eventId` is sufficient for Day 9; EOS is Day 10 scope |
| 9 | Observability | Kafka Micrometer series need `application` tag | `management.metrics.tags.application: ${spring.application.name}` | **Accept** | Already wired in ADV139; confirmed in Grafana scrape |
| 10 | Observability | No distributed tracing (`management.tracing.*`) | Add Micrometer Tracing + OTel exporter | **Defer** | Grafana lag/DLQ panels cover Day 9; tracing added with prod observability stack |
| 11 | Security | `bootstrap-servers` uses PLAINTEXT | `security.protocol: SASL_SSL` + truststore in prod | **Reject** | Known local-dev gap; prod overrides in `application-prod.yml` on Day 10 |

## Accepted changes applied in this PR

- `application.yml`: `max.poll.records: 100`, `enable.idempotence: true`
- `KafkaConsumerConfig`: `DefaultErrorHandler` wired on `systemAlertListenerContainerFactory` (#5)

## Deferred / rejected — follow-up

- **Defer #2, #3, #4, #10**: Day 10 prod hardening backlog
- **Reject #6, #8, #11**: Documented architectural choices; not appropriate for dev/default profile

## Claude review prompt

See [`TICKET-ADV145-prompt.md`](TICKET-ADV145-prompt.md).

## Test plan

- [ ] `./mvnw -Dtest=KafkaPipelineIT test` — happy path still passes
- [ ] `./mvnw -Dtest=DlqRoutingIT test` — DLQ path still passes
- [ ] `curl -s http://localhost:8081/api/actuator/prometheus | grep kafka_consumer` — metrics tagged `application="reconx"`
