package com.loanflow.commons.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * DLQ Monitor — listens to ALL dead letter topics.
 *
 * In production this would:
 * 1. Parse the failed message
 * 2. Store it in a "failed_events" database table
 * 3. Send PagerDuty/Slack alert to on-call engineer
 * 4. Expose a REST endpoint for ops to inspect + replay
 *
 * For LoanFlow: we log everything with full context.
 * Kibana alert fires when ERROR log contains "DLQ_RECEIVED".
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DlqMonitor {

    /**
     * Wildcard topic pattern — matches ANY topic ending in .DLT
     * One listener handles DLQ for all services.
     *
     * Topics matched:
     *   loan.application.submitted.DLT
     *   loan.approved.DLT
     *   payment.received.DLT
     *   ... etc
     */
    @KafkaListener(
            topicPattern = ".*\\.DLT",
            groupId = "loanflow-dlq-monitor"
    )
    public void onDeadLetterMessage(ConsumerRecord<String, Object> record) {

        // Extract failure context from DLT headers
        String exceptionMessage = getHeader(record, "kafka_dlt-exception-message");
        String originalTopic    = getHeader(record, "kafka_dlt-original-topic");
        String originalPartition = getHeader(record, "kafka_dlt-original-partition");
        String originalOffset   = getHeader(record, "kafka_dlt-original-offset");
        String exceptionClass   = getHeader(record, "kafka_dlt-exception-cause-fqcn");

        /**
         * DLQ_RECEIVED in log message is our Kibana alert trigger.
         * Kibana alert rule: "if ERROR log contains DLQ_RECEIVED → notify Slack"
         *
         * Log contains everything ops needs:
         * - Which topic failed (originalTopic)
         * - Where to find it (partition + offset)
         * - Why it failed (exceptionClass + exceptionMessage)
         * - What the message was (record.value())
         */
        log.error(
                "DLQ_RECEIVED | originalTopic={} partition={} offset={} " +
                        "key={} exceptionClass={} exceptionMessage={} payload={}",
                originalTopic, originalPartition, originalOffset,
                record.key(), exceptionClass, exceptionMessage, record.value()
        );
    }

    private String getHeader(ConsumerRecord<?, ?> record, String headerName) {
        Header header = record.headers().lastHeader(headerName);
        if (header == null) return "unknown";
        return new String(header.value(), StandardCharsets.UTF_8);
    }
}