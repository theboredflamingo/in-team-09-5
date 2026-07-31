package com.dbtraining.reconx.service;

import com.dbtraining.reconx.repository.AuditLogRepository;
import com.dbtraining.reconx.repository.entity.AuditLogEntry;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/** TICKET-ADV137 — fold audit_log events into current trade state. */
@ExtendWith(MockitoExtension.class)
class TradeAggregatorTest {

    @Mock
    private AuditLogRepository auditRepo;

    private TradeAggregator aggregator;

    @BeforeEach
    void setUp() {
        aggregator = new TradeAggregator(auditRepo, new ObjectMapper());
    }

    @Test
    void rebuild_noEvents_returnsEmpty() {
        when(auditRepo.findByTradeRefOrderByEventTimestampAsc("TRD-001")).thenReturn(List.of());

        assertThat(aggregator.rebuild("TRD-001")).isEmpty();
    }

    @Test
    void rebuild_createdThenUpdated_returnsLastAfterSnapshot() {
        when(auditRepo.findByTradeRefOrderByEventTimestampAsc("TRD-001")).thenReturn(List.of(
                entry("TRADE_CREATED", null, "{\"tradeRef\":\"TRD-001\",\"status\":\"NEW\"}"),
                entry("TRADE_UPDATED", "{\"status\":\"NEW\"}", "{\"tradeRef\":\"TRD-001\",\"status\":\"CONFIRMED\"}")
        ));

        Optional<com.fasterxml.jackson.databind.JsonNode> result = aggregator.rebuild("TRD-001");

        assertThat(result).isPresent();
        assertThat(result.get().get("status").asText()).isEqualTo("CONFIRMED");
    }

    @Test
    void rebuild_createdUpdatedCancelled_returnsEmpty() {
        when(auditRepo.findByTradeRefOrderByEventTimestampAsc("TRD-001")).thenReturn(List.of(
                entry("TRADE_CREATED", null, "{\"tradeRef\":\"TRD-001\",\"status\":\"NEW\"}"),
                entry("TRADE_UPDATED", "{\"status\":\"NEW\"}", "{\"tradeRef\":\"TRD-001\",\"status\":\"CONFIRMED\"}"),
                entry("TRADE_CANCELLED", "{\"tradeRef\":\"TRD-001\",\"status\":\"CONFIRMED\"}", null)
        ));

        assertThat(aggregator.rebuild("TRD-001")).isEmpty();
    }

    private static AuditLogEntry entry(String eventType, String before, String after) {
        return new AuditLogEntry(
                "evt-" + eventType,
                "TRD-001",
                eventType,
                Instant.parse("2026-07-29T10:00:00Z"),
                null,
                before,
                after);
    }
}
