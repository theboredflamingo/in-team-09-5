package com.dbtraining.reconx.kafka;

import org.junit.jupiter.api.Test;
import org.springframework.util.backoff.BackOffExecution;
import org.springframework.util.backoff.ExponentialBackOff;

import static org.assertj.core.api.Assertions.assertThat;

/** TICKET-ADV135 — backoff yields ~3 retries before the 8s budget is exhausted. */
class ExponentialBackOffTest {

    @Test
    void backoffScheduleMatchesOneTwoFourSeconds() {
        ExponentialBackOff backoff = new ExponentialBackOff(1000L, 2.0);
        backoff.setMaxElapsedTime(8_000L);

        BackOffExecution execution = backoff.start();
        assertThat(execution.nextBackOff()).isEqualTo(1000L);
        assertThat(execution.nextBackOff()).isEqualTo(2000L);
        assertThat(execution.nextBackOff()).isEqualTo(4000L);
        // Budget (~8s) allows three widening gaps before DLQ; further calls stop or cap out.
        long fourth = execution.nextBackOff();
        assertThat(fourth == BackOffExecution.STOP || fourth == 8000L).isTrue();
    }
}
