package com.dbtraining.reconx.service;

import com.dbtraining.reconx.dto.TradeRequest;
import com.dbtraining.reconx.exception.DuplicateTradeRefException;
import com.dbtraining.reconx.exception.TradeNotFoundException;
import com.dbtraining.reconx.kafka.TradeEventProducer;
import com.dbtraining.reconx.observability.TradeMetrics;
import com.dbtraining.reconx.repository.CounterpartyRepository;
import com.dbtraining.reconx.repository.InstrumentRepository;
import com.dbtraining.reconx.repository.TradeRepository;
import com.dbtraining.reconx.repository.entity.Trade;
import com.dbtraining.reconx.dto.TradeEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static com.dbtraining.reconx.repository.TradeSpecifications.*;

/**
 * ============================================================================
 * TICKET-ADV064 — TradeService.create (POST endpoint backing)
 * TICKET-ADV065 — update
 * TICKET-ADV066 — updateStatus (PATCH)
 * TICKET-ADV067 — softDelete
 * TICKET-ADV083 — increments trade_created_total Counter on create
 * TICKET-ADV129 — publishes TradeEvent on every state change
 * TICKET-ADV055/ADV056 — list() uses Specifications + filter query
 * ============================================================================
 */
@Service
@Transactional
public class TradeService {

    private final TradeRepository tradeRepo;
    private final CounterpartyRepository cpRepo;
    private final InstrumentRepository instRepo;
    private final TradeEventProducer events;
    private final TradeMetrics metrics;
    private final ObjectMapper objectMapper;

    public TradeService(TradeRepository tradeRepo,
                        CounterpartyRepository cpRepo,
                        InstrumentRepository instRepo,
                        TradeEventProducer events,
                        TradeMetrics metrics,
                        ObjectMapper objectMapper) {
        this.tradeRepo = tradeRepo;
        this.cpRepo = cpRepo;
        this.instRepo = instRepo;
        this.events = events;
        this.metrics = metrics;
        this.objectMapper = objectMapper;
    }

    public Trade create(TradeRequest req, String actor) {
        tradeRepo.findByTradeRef(req.tradeRef()).ifPresent(t -> {
            throw new DuplicateTradeRefException(req.tradeRef());
        });
        var instrument = instRepo.findById(req.instrumentId())
                .orElseThrow(() -> new TradeNotFoundException("instrument " + req.instrumentId()));
        var counterparty = cpRepo.findById(req.counterpartyId())
                .orElseThrow(() -> new TradeNotFoundException("counterparty " + req.counterpartyId()));

        var t = new Trade();
        t.setTradeRef(req.tradeRef());
        t.setInstrument(instrument);
        t.setCounterparty(counterparty);
        t.setAssetClass(req.assetClass());
        t.setSide(req.side());
        t.setQuantity(req.quantity());
        t.setPrice(req.price());
        t.setTradeDate(req.tradeDate());
        t.setStatus("PENDING");
        Trade saved = tradeRepo.save(t);

        metrics.incrementTradeCreated();
        metrics.recordTradeValue(req.quantity().multiply(req.price()).doubleValue());
        events.publish(TradeEvent.created(saved.getTradeRef(), snapshot(saved)));
        return saved;
    }

    public Trade update(Long id, TradeRequest req, String actor) {
        var t = tradeRepo.findById(id)
                .orElseThrow(() -> new TradeNotFoundException("id " + id));
        JsonNode before = snapshot(t);

        t.setAssetClass(req.assetClass());
        t.setSide(req.side());
        t.setQuantity(req.quantity());
        t.setPrice(req.price());
        t.setTradeDate(req.tradeDate());
        Trade saved = tradeRepo.save(t);

        events.publish(TradeEvent.updated(saved.getTradeRef(), before, snapshot(saved)));
        return saved;
    }

    public Trade updateStatus(Long id, String status, String actor) {
        var t = tradeRepo.findById(id)
                .orElseThrow(() -> new TradeNotFoundException("id " + id));
        JsonNode before = snapshot(t);
        t.setStatus(status);
        Trade saved = tradeRepo.save(t);

        events.publish(TradeEvent.updated(saved.getTradeRef(), before, snapshot(saved)));
        return saved;
    }

    public void softDelete(Long id, String actor) {
        var t = tradeRepo.findById(id)
                .orElseThrow(() -> new TradeNotFoundException("id " + id));
        JsonNode before = snapshot(t);
        t.softDelete();
        tradeRepo.save(t);

        events.publish(TradeEvent.cancelled(t.getTradeRef(), before));
    }

    private JsonNode snapshot(Trade trade) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("tradeRef", trade.getTradeRef());
        node.put("status", trade.getStatus());
        node.put("side", trade.getSide());
        node.put("assetClass", trade.getAssetClass());
        node.put("quantity", trade.getQuantity());
        node.put("price", trade.getPrice());
        node.put("tradeDate", trade.getTradeDate().toString());
        if (trade.getInstrument() != null) {
            node.put("instrumentId", trade.getInstrument().getId());
            node.put("instrumentSymbol", trade.getInstrument().getSymbol());
        }
        if (trade.getCounterparty() != null) {
            node.put("counterpartyId", trade.getCounterparty().getId());
            node.put("counterpartyName", trade.getCounterparty().getName());
        }
        if (trade.getDeletedAt() != null) {
            node.put("deletedAt", trade.getDeletedAt().toString());
        }
        return node;
    }

    @Transactional(readOnly = true)
    public Page<Trade> list(LocalDate from, LocalDate to, String status, Long counterpartyId, Pageable pageable) {
        Specification<Trade> spec = Specification
                .where(tradeDateBetween(from, to))
                .and(hasStatus(status))
                .and(hasCounterparty(counterpartyId));
        return tradeRepo.findAll(spec, pageable);
    }
}
