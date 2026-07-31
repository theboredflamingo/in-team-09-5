package com.dbtraining.reconx.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** TICKET-ADV130 — Jackson round-trip for TradeEvent JsonNode snapshots. */
class TradeEventTest {

    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    void roundTripsUpdatedEventWithBeforeAndAfterSnapshots() throws Exception {
        JsonNode before = JsonNodeFactory.instance.objectNode()
                .put("status", "PENDING")
                .put("quantity", 100);
        JsonNode after = JsonNodeFactory.instance.objectNode()
                .put("status", "PENDING")
                .put("quantity", 200);

        TradeEvent original = TradeEvent.updated("ADV-20260729-0001", before, after);

        String json = mapper.writeValueAsString(original);
        TradeEvent restored = mapper.readValue(json, TradeEvent.class);

        assertThat(restored.eventId()).isEqualTo(original.eventId());
        assertThat(restored.tradeRef()).isEqualTo("ADV-20260729-0001");
        assertThat(restored.eventType()).isEqualTo(TradeEvent.EventType.TRADE_UPDATED);
        assertThat(restored.before()).isEqualTo(before);
        assertThat(restored.after()).isEqualTo(after);
    }

    @Test
    void createdEventHasNullBefore() throws Exception {
        JsonNode after = JsonNodeFactory.instance.objectNode().put("status", "PENDING");
        TradeEvent event = TradeEvent.created("ADV-20260729-0002", after);

        TradeEvent restored = mapper.readValue(mapper.writeValueAsString(event), TradeEvent.class);

        assertThat(restored.before() == null || restored.before().isNull()).isTrue();
        assertThat(restored.after()).isEqualTo(after);
        assertThat(restored.eventType()).isEqualTo(TradeEvent.EventType.TRADE_CREATED);
    }
}
