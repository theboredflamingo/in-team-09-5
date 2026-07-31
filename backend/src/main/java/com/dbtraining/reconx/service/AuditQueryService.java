package com.dbtraining.reconx.service;

import com.dbtraining.reconx.dto.TradeEvent;
import com.dbtraining.reconx.repository.AuditLogRepository;
import com.dbtraining.reconx.repository.entity.AuditLogEntry;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/** TICKET-ADV138 — Read-side mapping from audit_log rows to TradeEvent DTOs. */
@Service
public class AuditQueryService {

    private final AuditLogRepository auditRepo;
    private final ObjectMapper objectMapper;

    public AuditQueryService(AuditLogRepository auditRepo, ObjectMapper objectMapper) {
        this.auditRepo = auditRepo;
        this.objectMapper = objectMapper;
    }

    public List<TradeEvent> eventsForTrade(String tradeRef) {
        return auditRepo.findByTradeRefOrderByEventTimestampAsc(tradeRef).stream()
                .map(this::toTradeEvent)
                .toList();
    }

    private TradeEvent toTradeEvent(AuditLogEntry entry) {
        return new TradeEvent(
                UUID.fromString(entry.getEventId()),
                entry.getTradeRef(),
                TradeEvent.EventType.valueOf(entry.getEventType()),
                entry.getEventTimestamp(),
                parseJson(entry.getBeforeState()),
                parseJson(entry.getAfterState()));
    }

    private JsonNode parseJson(String json) {
        if (json == null) {
            return null;
        }
        try {
            return objectMapper.readTree(json);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Invalid JSON in audit_log", ex);
        }
    }
}
