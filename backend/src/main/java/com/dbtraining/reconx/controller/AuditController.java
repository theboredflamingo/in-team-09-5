package com.dbtraining.reconx.controller;

import com.dbtraining.reconx.dto.TradeEvent;
import com.dbtraining.reconx.repository.AuditLogRepository;
import com.dbtraining.reconx.repository.entity.AuditLogEntry;
import com.dbtraining.reconx.service.AuditQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

/**
 * TICKET-ADV071 — GET /api/v1/audit/trades/{tradeRef}
 * TICKET-ADV138 — GET /api/v1/audit/trades/{tradeRef}/events
 */
@RestController
@RequestMapping("/v1/audit/trades")
@PreAuthorize("hasAnyRole('ADMIN','RECON_ANALYST')")
@Tag(name = "audit")
@SecurityRequirement(name = "bearerAuth")
public class AuditController {

    private final AuditLogRepository auditRepo;
    private final AuditQueryService queryService;

    public AuditController(AuditLogRepository auditRepo, AuditQueryService queryService) {
        this.auditRepo = auditRepo;
        this.queryService = queryService;
    }

    @GetMapping("/{tradeRef}")
    @Operation(summary = "Get audit history for a trade (by tradeRef)")
    public List<AuditLogEntry> history(@PathVariable String tradeRef) {
        // TODO(TICKET-ADV071): return auditRepo.findByTradeRefOrderByEventTimestampAsc(tradeRef).
        //   Day-0 returns an empty list so the React audit-trail panel renders
        //   "no history yet" instead of erroring.
        return Collections.emptyList();
    }

    @GetMapping("/{tradeRef}/events")
    @Operation(summary = "Stream of all Kafka-sourced events for a trade")
    public List<TradeEvent> getEvents(@PathVariable String tradeRef) {
        return queryService.eventsForTrade(tradeRef);
    }
}
