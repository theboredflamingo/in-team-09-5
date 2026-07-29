# TICKET-ADV145 — Kafka consumer config review decisions

Team review of Claude findings for production readiness. Context: trade reconciliation
service, ~500 events/sec peak, strict audit trail requirements.

## Findings and team decisions

| # | Area | Finding | Recommendation | Decision | Rationale |
|---|------|---------|----------------|----------|-----------|
| 1 | Backpressure | Default `max.poll.records` (500) can exceed `max.poll.interval.ms` when reconciliation or audit persistence is slow, causing spurious rebalance | `spring.kafka.consumer.properties.max.poll.records: 100` | **Accept** | Recon + audit listeners do DB I/O per record; smaller batches keep the poll loop within interval bounds at our target throughput |
| 2 | Error handling | `ExponentialBackOff` in `KafkaErrorHandlerConfig` has no jitter — retries from multiple partitions can stampede the broker after an outage | Subclass `ExponentialBackOff` or switch to `FixedBackOff` with random delay | **Defer** | Current 1s/2s/4s budget is adequate for Day 9; jitter is a Day 10 hardening item tracked in backlog |
| 3 | Idempotence | Producer `enable.idempotence` is not explicitly set; relies on Kafka/Java defaults which may differ by broker version | `spring.kafka.producer.properties.enable.idempotence: true` | **Accept** | Cheap guard against duplicate trade events on producer retry; required for audit correctness |
| 4 | Observability | Kafka Micrometer series need an `application` tag to avoid collisions when multiple services share one Prometheus | `management.metrics.tags.application: ${spring.application.name}` | **Accept** | Already wired in ADV139; confirmed during Grafana scrape — no further change needed |
| 5 | Security | `bootstrap-servers` uses PLAINTEXT (`localhost:9092`); no TLS or SASL | `security.protocol: SASL_SSL` + broker certs in prod profile | **Reject** | Known local-dev gap; prod overrides live in `application-prod.yml` on Day 10 — not appropriate for default/dev YAML |

## Accepted changes applied

- `application.yml`: `max.poll.records: 100`, `enable.idempotence: true`
- Observability tag already present (ADV139); no diff required

## Deferred / rejected — follow-up

- **Defer #2**: Add backoff jitter when implementing prod retry tuning (Day 10)
- **Reject #5**: SASL_SSL documented for prod profile; do not break local docker-compose stack

## Test plan

- [ ] `./mvnw -Dtest=KafkaPipelineIT test` — happy path still passes
- [ ] `./mvnw -Dtest=DlqRoutingIT test` — DLQ path still passes after retry/DLQ config unchanged
- [ ] `curl -s http://localhost:8081/api/actuator/prometheus | grep kafka_consumer` — metrics still tagged `application="reconx"`
