package com.dbtraining.reconx.kafka;

import com.dbtraining.reconx.dto.TradeEvent;
import com.dbtraining.reconx.model.DlqMessage;
import com.dbtraining.reconx.repository.DlqMessageRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * TICKET-ADV136 — Persists each message routed to trade-events-dlq for operator review.
 */
@Component
public class DlqConsumer {

    private static final Logger log = LoggerFactory.getLogger(DlqConsumer.class);

    private final DlqMessageRepository repo;

    public DlqConsumer(DlqMessageRepository repo) {
        this.repo = repo;
    }

    @KafkaListener(
            topics = "trade-events-dlq",
            groupId = "dlq-monitor",
            containerFactory = "tradeEventListenerContainerFactory"
    )
    public void onDlqMessage(ConsumerRecord<String, TradeEvent> record,
                             @Header(KafkaHeaders.EXCEPTION_MESSAGE) String exMsg) {
        TradeEvent event = record.value();
        log.error("DLQ: trade={} eventId={} topic={} partition={} offset={} reason={}",
                event.tradeRef(), event.eventId(), record.topic(),
                record.partition(), record.offset(), exMsg);

        repo.save(DlqMessage.builder()
                .eventId(event.eventId().toString())
                .tradeRef(event.tradeRef())
                .originalTopic(record.topic().replace("-dlq", ""))
                .partition(record.partition())
                .offset(record.offset())
                .payload(event)
                .reason(exMsg)
                .firstSeen(Instant.now())
                .build());
    }
}
