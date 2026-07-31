package com.dbtraining.reconx.repository;

import com.dbtraining.reconx.repository.entity.AuditLogEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLogEntry, Long> {
    List<AuditLogEntry> findByTradeRefOrderByEventTimestampAsc(String tradeRef);

    @Query("SELECT DISTINCT a.tradeRef FROM AuditLogEntry a ORDER BY a.tradeRef")
    List<String> findDistinctTradeRefs();
}
