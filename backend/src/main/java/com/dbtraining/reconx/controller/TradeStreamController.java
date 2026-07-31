package com.dbtraining.reconx.controller;

import com.dbtraining.reconx.service.TradeStreamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * TICKET-ADV106 — GET /api/v1/trades/stream (SSE live trade feed).
 */
@RestController
@RequestMapping("/v1/trades")
@Tag(name = "trades", description = "Trade CRUD and search")
public class TradeStreamController {

    private final TradeStreamService streamService;

    public TradeStreamController(TradeStreamService streamService) {
        this.streamService = streamService;
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Live trade event stream (Server-Sent Events)")
    public SseEmitter stream() {
        return streamService.subscribe();
    }
}
