package com.dbtraining.reconx.service;

import com.dbtraining.reconx.exception.InvalidTradeException;
import com.dbtraining.reconx.repository.InstrumentRepository;
import com.dbtraining.reconx.repository.entity.Instrument;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class InstrumentService {

    private final InstrumentRepository repo;
    private static final Logger log = LoggerFactory.getLogger(InstrumentService.class);

    public InstrumentService(InstrumentRepository repo) { 
        this.repo = repo; 
    }

    @Cacheable("instruments")
    public Instrument findBySymbol(String symbol) {

        log.info("DB hit for {}", symbol);
        return repo.findBySymbol(symbol)
                .orElseThrow(() -> new InvalidTradeException("Unknown instrument symbol: " + symbol));
    }
}
