package com.dbtraining.reconx.service;

import com.dbtraining.reconx.exception.TradeNotFoundException;
import com.dbtraining.reconx.repository.AuditLogRepository;
import com.dbtraining.reconx.repository.CounterpartyRepository;
import com.dbtraining.reconx.repository.InstrumentRepository;
import com.dbtraining.reconx.repository.TradeRepository;
import com.dbtraining.reconx.repository.entity.Trade;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * TICKET-ADV137 — Rebuild the trades table from audit_log event streams.
 */
@Service
public class TradeRebuildService {

    private static final Logger log = LoggerFactory.getLogger(TradeRebuildService.class);

    private final AuditLogRepository auditRepo;
    private final TradeAggregator aggregator;
    private final TradeRepository tradeRepo;
    private final InstrumentRepository instRepo;
    private final CounterpartyRepository cpRepo;

    public TradeRebuildService(AuditLogRepository auditRepo,
                               TradeAggregator aggregator,
                               TradeRepository tradeRepo,
                               InstrumentRepository instRepo,
                               CounterpartyRepository cpRepo) {
        this.auditRepo = auditRepo;
        this.aggregator = aggregator;
        this.tradeRepo = tradeRepo;
        this.instRepo = instRepo;
        this.cpRepo = cpRepo;
    }

    @Transactional
    public Map<String, Object> rebuildAll() {
        long beforeCount = tradeRepo.count();
        log.info("Starting event-sourced rebuild — {} trades currently in table", beforeCount);

        tradeRepo.deleteAllHard();

        List<String> tradeRefs = auditRepo.findDistinctTradeRefs();
        int restored = 0;
        int skipped = 0;

        for (String tradeRef : tradeRefs) {
            Optional<JsonNode> state = aggregator.rebuild(tradeRef);
            if (state.isEmpty()) {
                skipped++;
                continue;
            }
            tradeRepo.save(fromSnapshot(state.get()));
            restored++;
        }

        long afterCount = tradeRepo.count();
        log.info("Rebuild complete — restored={}, skipped(cancelled)={}, trades now={}",
                restored, skipped, afterCount);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("beforeCount", beforeCount);
        result.put("afterCount", afterCount);
        result.put("restored", restored);
        result.put("skipped", skipped);
        result.put("distinctTradeRefsInAuditLog", tradeRefs.size());
        return result;
    }

    private Trade fromSnapshot(JsonNode node) {
        long instrumentId = node.get("instrumentId").asLong();
        long counterpartyId = node.get("counterpartyId").asLong();

        var instrument = instRepo.findById(instrumentId)
                .orElseThrow(() -> new TradeNotFoundException("instrument " + instrumentId));
        var counterparty = cpRepo.findById(counterpartyId)
                .orElseThrow(() -> new TradeNotFoundException("counterparty " + counterpartyId));

        var t = new Trade();
        t.setTradeRef(node.get("tradeRef").asText());
        t.setInstrument(instrument);
        t.setCounterparty(counterparty);
        t.setAssetClass(node.get("assetClass").asText());
        t.setSide(node.get("side").asText());
        t.setQuantity(new BigDecimal(node.get("quantity").asText()));
        t.setPrice(new BigDecimal(node.get("price").asText()));
        t.setTradeDate(LocalDate.parse(node.get("tradeDate").asText()));
        t.setStatus(node.get("status").asText());
        return t;
    }
}
