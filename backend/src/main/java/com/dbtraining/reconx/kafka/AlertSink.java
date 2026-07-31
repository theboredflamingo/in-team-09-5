package com.dbtraining.reconx.kafka;

import com.dbtraining.reconx.dto.SystemAlert;

/** TICKET-ADV133 — pluggable alert notification channel (Slack, PagerDuty, etc.). */
public interface AlertSink {
    void notify(SystemAlert alert);
}
