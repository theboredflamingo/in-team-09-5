package com.dbtraining.reconx.model;

import com.dbtraining.reconx.dto.TradeEvent;
import jakarta.persistence.*;

import java.time.Instant;

/**
 * TICKET-ADV136 — Persisted DLQ record written by {@link com.dbtraining.reconx.kafka.DlqConsumer}.
 */
@Entity
@Table(name = "dlq_messages")
public class DlqMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, unique = true, length = 36)
    private String eventId;

    @Column(name = "trade_ref", nullable = false, length = 30)
    private String tradeRef;

    @Column(name = "original_topic", nullable = false, length = 100)
    private String originalTopic;

    @Column(name = "topic_partition", nullable = false)
    private int partition;

    @Column(name = "record_offset", nullable = false)
    private long offset;

    @Convert(converter = TradeEventConverter.class)
    @Column(name = "payload_json", nullable = false, columnDefinition = "TEXT")
    private TradeEvent payload;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "first_seen", nullable = false)
    private Instant firstSeen;

    protected DlqMessage() {}

    private DlqMessage(Builder b) {
        this.eventId = b.eventId;
        this.tradeRef = b.tradeRef;
        this.originalTopic = b.originalTopic;
        this.partition = b.partition;
        this.offset = b.offset;
        this.payload = b.payload;
        this.reason = b.reason;
        this.firstSeen = b.firstSeen;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Long getId()              { return id; }
    public String getEventId()         { return eventId; }
    public String getTradeRef()      { return tradeRef; }
    public String getOriginalTopic() { return originalTopic; }
    public int getPartition()        { return partition; }
    public long getOffset()          { return offset; }
    public TradeEvent getPayload()   { return payload; }
    public String getReason()        { return reason; }
    public Instant getFirstSeen()    { return firstSeen; }

    public static final class Builder {
        private String eventId;
        private String tradeRef;
        private String originalTopic;
        private int partition;
        private long offset;
        private TradeEvent payload;
        private String reason;
        private Instant firstSeen;

        public Builder eventId(String eventId)           { this.eventId = eventId; return this; }
        public Builder tradeRef(String tradeRef)       { this.tradeRef = tradeRef; return this; }
        public Builder originalTopic(String topic)     { this.originalTopic = topic; return this; }
        public Builder partition(int partition)        { this.partition = partition; return this; }
        public Builder offset(long offset)             { this.offset = offset; return this; }
        public Builder payload(TradeEvent payload)     { this.payload = payload; return this; }
        public Builder reason(String reason)           { this.reason = reason; return this; }
        public Builder firstSeen(Instant firstSeen)    { this.firstSeen = firstSeen; return this; }

        public DlqMessage build() {
            return new DlqMessage(this);
        }
    }
}
