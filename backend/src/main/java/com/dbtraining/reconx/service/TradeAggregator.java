package com.dbtraining.reconx.service;

import com.dbtraining.reconx.dto.TradeEvent;
import com.dbtraining.reconx.repository.AuditLogRepository;
import com.dbtraining.reconx.repository.entity.AuditLogEntry;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * TICKET-ADV137 — Event-sourcing rebuild from audit_log.
 */
@Service
public class TradeAggregator {

    private static final Logger log = LoggerFactory.getLogger(TradeAggregator.class);

    private final AuditLogRepository auditRepo;
    private final ObjectMapper objectMapper;

    public TradeAggregator(AuditLogRepository auditRepo, ObjectMapper objectMapper) {
        this.auditRepo = auditRepo;
        this.objectMapper = objectMapper;
    }

    public Optional<JsonNode> rebuild(String tradeRef) {
        List<AuditLogEntry> events = auditRepo.findByTradeRefOrderByEventTimestampAsc(tradeRef);
        if (events.isEmpty()) {
            return Optional.empty();
        }

        JsonNode state = null;
        for (AuditLogEntry e : events) {
            log.debug("Folding tradeRef={} event={} at {}", tradeRef, e.getEventType(), e.getEventTimestamp());
            switch (TradeEvent.EventType.valueOf(e.getEventType())) {
                case TRADE_CREATED, TRADE_UPDATED -> state = parseAfterState(e.getAfterState());
                case TRADE_CANCELLED              -> state = null;
                default -> { /* ignore unknown audit event types */ }
            }
        }
        log.info("Rebuilt tradeRef={} from {} events -> {}",
                tradeRef, events.size(), state == null ? "absent (cancelled)" : "present");
        return Optional.ofNullable(state);
    }

    private JsonNode parseAfterState(String afterState) {
        if (afterState == null) {
            return null;
        }
        try {
            return objectMapper.readTree(afterState);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Invalid after_state JSON in audit_log", ex);
        }
    }
}
