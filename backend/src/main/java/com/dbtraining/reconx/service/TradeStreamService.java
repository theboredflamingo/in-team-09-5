package com.dbtraining.reconx.service;

import com.dbtraining.reconx.dto.TradeEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * TICKET-ADV106 — In-memory SSE hub broadcasting trade-events to dashboard clients.
 */
@Service
public class TradeStreamService {

    private static final Logger log = LoggerFactory.getLogger(TradeStreamService.class);
    private static final long EMITTER_TIMEOUT_MS = 3_600_000L;

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();
    private final ObjectMapper objectMapper;

    public TradeStreamService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT_MS);
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(ex -> emitters.remove(emitter));
        try {
            emitter.send(SseEmitter.event().name("connected").data("ok"));
        } catch (IOException ex) {
            emitters.remove(emitter);
            log.debug("SSE client disconnected during handshake: {}", ex.getMessage());
        }
        return emitter;
    }

    public void broadcast(TradeEvent event) {
        JsonNode after = event.after();
        if (after == null || after.isNull()) {
            return;
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("tradeRef", text(after, "tradeRef"));
        payload.put("symbol", text(after, "instrumentSymbol"));
        payload.put("quantity", after.path("quantity").isMissingNode() ? null : after.get("quantity"));
        payload.put("price", after.path("price").isMissingNode() ? null : after.get("price"));
        payload.put("status", text(after, "status"));
        payload.put("eventType", event.eventType().name());

        String json;
        try {
            json = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            log.warn("Failed to serialise trade stream payload for ref={}: {}", event.tradeRef(), ex.getMessage());
            return;
        }

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().data(json));
            } catch (IOException ex) {
                emitters.remove(emitter);
            }
        }
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }
}
