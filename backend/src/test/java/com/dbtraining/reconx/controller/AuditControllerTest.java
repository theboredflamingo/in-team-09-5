package com.dbtraining.reconx.controller;

import com.dbtraining.reconx.repository.AuditLogRepository;
import com.dbtraining.reconx.repository.entity.AuditLogEntry;
import com.dbtraining.reconx.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** TICKET-ADV138 — role-gated audit events endpoint. */
@SpringBootTest
@AutoConfigureMockMvc
class AuditControllerTest {

    private static final String TRADE_REF = "TRD-ADV138";
    private static final String EVENT_ID = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee";

    @Autowired MockMvc mvc;
    @Autowired JwtTokenProvider jwt;
    @Autowired AuditLogRepository auditRepo;

    @BeforeEach
    void seedAuditRow() {
        auditRepo.deleteAll();
        auditRepo.save(new AuditLogEntry(
                EVENT_ID,
                TRADE_REF,
                "TRADE_CREATED",
                Instant.parse("2026-07-29T09:00:00Z"),
                null,
                null,
                "{\"tradeRef\":\"TRD-ADV138\",\"status\":\"NEW\"}"));
    }

    @Test
    void adminGetsOrderedEvents() throws Exception {
        String token = jwt.generate("admin@db.com", "ADMIN");
        mvc.perform(get("/v1/audit/trades/{tradeRef}/events", TRADE_REF)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].eventId").value(EVENT_ID))
                .andExpect(jsonPath("$[0].eventType").value("TRADE_CREATED"));
    }

    @Test
    void reconAnalystGetsEvents() throws Exception {
        String token = jwt.generate("recon@db.com", "RECON_ANALYST");
        mvc.perform(get("/v1/audit/trades/{tradeRef}/events", TRADE_REF)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void unauthenticatedIsDenied() throws Exception {
        mvc.perform(get("/v1/audit/trades/{tradeRef}/events", TRADE_REF))
                .andExpect(status().isForbidden());
    }

    @Test
    void traderGetsForbidden() throws Exception {
        String token = jwt.generate("trader@db.com", "TRADER");
        mvc.perform(get("/v1/audit/trades/{tradeRef}/events", TRADE_REF)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }
}
