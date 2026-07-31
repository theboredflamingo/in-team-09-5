package com.dbtraining.reconx.controller;

import com.dbtraining.reconx.service.TradeRebuildService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * TICKET-ADV137 — POST /api/v1/admin/rebuild replays audit_log into trades.
 */
@RestController
@RequestMapping("/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "admin-rebuild")
@SecurityRequirement(name = "bearerAuth")
public class RebuildAdminController {

    private final TradeRebuildService rebuildService;

    public RebuildAdminController(TradeRebuildService rebuildService) {
        this.rebuildService = rebuildService;
    }

    @PostMapping("/rebuild")
    @Operation(summary = "Rebuild trades table from audit_log event streams")
    public ResponseEntity<Map<String, Object>> rebuild() {
        return ResponseEntity.ok(rebuildService.rebuildAll());
    }
}
