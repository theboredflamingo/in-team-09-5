package com.dbtraining.reconx.controller;

import com.dbtraining.reconx.exception.TradeNotFoundException;
import com.dbtraining.reconx.kafka.TradeEventProducer;
import com.dbtraining.reconx.model.DlqMessage;
import com.dbtraining.reconx.repository.DlqMessageRepository;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * TICKET-ADV136 — One-at-a-time DLQ replay for operators.
 */
@RestController
@RequestMapping("/v1/admin/dlq")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "admin-dlq")
@SecurityRequirement(name = "bearerAuth")
public class DlqAdminController {

    private final DlqMessageRepository repo;
    private final TradeEventProducer producer;

    public DlqAdminController(DlqMessageRepository repo, TradeEventProducer producer) {
        this.repo = repo;
        this.producer = producer;
    }

    @PostMapping("/replay")
    public ResponseEntity<Map<String, Object>> replay(
            @RequestParam UUID eventId,
            @RequestParam(defaultValue = "false") boolean dryRun) {

        DlqMessage msg = repo.findByEventId(eventId.toString())
                .orElseThrow(() -> new TradeNotFoundException("No DLQ message: " + eventId));

        if (dryRun) {
            return ResponseEntity.ok(Map.of(
                    "dryRun", true,
                    "wouldReplayTo", msg.getOriginalTopic(),
                    "tradeRef", msg.getTradeRef(),
                    "payload", msg.getPayload()
            ));
        }

        producer.publish(msg.getPayload());
        repo.delete(msg);

        return ResponseEntity.ok(Map.of(
                "replayed", true,
                "eventId", eventId,
                "topic", msg.getOriginalTopic()
        ));
    }
}
