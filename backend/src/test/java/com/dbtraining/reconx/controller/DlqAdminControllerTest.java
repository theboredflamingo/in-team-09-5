package com.dbtraining.reconx.controller;

import com.dbtraining.reconx.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DlqAdminControllerTest {

    @Autowired MockMvc mvc;
    @Autowired JwtTokenProvider jwt;

    @Test
    void adminCanCallReplayEndpoint() throws Exception {
        String token = jwt.generate("admin@db.com", "ADMIN");
        mvc.perform(post("/v1/admin/dlq/replay")
                        .param("eventId", UUID.randomUUID().toString())
                        .param("dryRun", "true")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound()); // no DLQ row yet — auth passed
    }

    @Test
    void traderGetsForbidden() throws Exception {
        String token = jwt.generate("trader@db.com", "TRADER");
        mvc.perform(post("/v1/admin/dlq/replay")
                        .param("eventId", UUID.randomUUID().toString())
                        .param("dryRun", "true")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }
}
