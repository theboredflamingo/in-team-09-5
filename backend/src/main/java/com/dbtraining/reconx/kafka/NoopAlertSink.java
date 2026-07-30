package com.dbtraining.reconx.kafka;

import com.dbtraining.reconx.dto.SystemAlert;
import org.springframework.stereotype.Component;

/** TICKET-ADV133 — default sink; replace with Slack/webhook implementation in prod. */
@Component
public class NoopAlertSink implements AlertSink {

    @Override
    public void notify(SystemAlert alert) {
        // intentionally empty — training default
    }
}
