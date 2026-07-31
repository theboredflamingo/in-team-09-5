package com.dbtraining.reconx.dto;

import java.time.Instant;

/** TICKET-ADV133 — wire format for the system-alerts Kafka topic. */
public record SystemAlert(
        String severity,
        String code,
        String message,
        Instant timestamp
) {}
