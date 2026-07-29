package com.dbtraining.reconx.kafka;

import com.dbtraining.reconx.dto.SystemAlert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * ============================================================================
 * TICKET-ADV133 — AlertConsumer
 *
 * WHAT:    Subscribes to `system-alerts` and logs + forwards each alert.
 * HOW:     @KafkaListener on system-alerts, groupId alert-service. Delegates
 *          to pluggable AlertSink (NoopAlertSink in training).
 * WHY:     Single-partition topic preserves global alert ordering.
 * OBSERVE: Publish a SystemAlert -> WARN log line + alert-service group in Kafdrop.
 * ============================================================================
 */
@Component
public class AlertConsumer {

    private static final Logger log = LoggerFactory.getLogger(AlertConsumer.class);
    private final AlertSink sink;

    public AlertConsumer(AlertSink sink) {
        this.sink = sink;
    }

    @KafkaListener(
            topics = KafkaTopicsConfig.SYSTEM_ALERTS,
            groupId = "alert-service",
            containerFactory = "systemAlertListenerContainerFactory")
    public void onAlert(SystemAlert alert) {
        log.warn("WARN ALERT severity={} code={} message={}",
                alert.severity(), alert.code(), alert.message());
        sink.notify(alert);
    }
}
