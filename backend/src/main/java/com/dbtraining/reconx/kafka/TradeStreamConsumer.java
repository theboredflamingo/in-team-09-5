package com.dbtraining.reconx.kafka;

import com.dbtraining.reconx.dto.TradeEvent;
import com.dbtraining.reconx.service.TradeStreamService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * TICKET-ADV106 — Forwards Kafka trade-events to connected SSE dashboard clients.
 */
@Component
public class TradeStreamConsumer {

    private final TradeStreamService streamService;

    public TradeStreamConsumer(TradeStreamService streamService) {
        this.streamService = streamService;
    }

    @KafkaListener(
            topics = KafkaTopicsConfig.TRADE_EVENTS,
            groupId = "sse-dashboard",
            containerFactory = "tradeEventListenerContainerFactory")
    public void onTradeEvent(TradeEvent event) {
        streamService.broadcast(event);
    }
}
