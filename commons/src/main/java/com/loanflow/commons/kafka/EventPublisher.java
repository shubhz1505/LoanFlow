package com.loanflow.commons.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Single publisher class used by ALL services.
 *
 * Why wrap KafkaTemplate?
 * 1. Centralized logging — every publish is logged with correlationId
 * 2. Centralized error handling — callback failure handled once
 * 3. Testability — mock this class in unit tests instead of KafkaTemplate
 * 4. Metrics — one place to add publish latency tracking
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Publish an event to a Kafka topic.
     *
     * @param topic         The Kafka topic name
     * @param key           The message key (use entityId for ordering)
     * @param event         The event payload (any BaseEvent subclass)
     * @param correlationId For distributed tracing in logs
     */
    public void publish(String topic, String key,
                        Object event, String correlationId) {

        log.info("[{}] Publishing event to topic={} key={}",
                correlationId, topic, key);

        /**
         * kafkaTemplate.send() is ASYNCHRONOUS.
         * It returns CompletableFuture immediately.
         * Actual network send happens in background thread.
         *
         * We attach a callback to know if it succeeded or failed.
         * We do NOT block (no .get()) — that would kill throughput.
         */
        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(topic, key, event);

        future.whenComplete((result, exception) -> {

            if (exception != null) {
                /**
                 * CRITICAL: Publish failed even after producer retries.
                 * Options:
                 * 1. Log and accept data loss (wrong for fintech)
                 * 2. Save to DB outbox table and retry (Transactional Outbox Pattern)
                 * 3. Alert ops immediately
                 *
                 * For now: log ERROR so Prometheus alert fires.
                 * Production: implement Transactional Outbox Pattern.
                 * We discuss that pattern when building Payment Service.
                 */
                log.error(
                        "[{}] FAILED to publish event to topic={} key={} error={}",
                        correlationId, topic, key, exception.getMessage(), exception
                );

            } else {
                /**
                 * RecordMetadata tells us exactly where in Kafka
                 * this message landed. Invaluable for debugging:
                 * "Find the message at partition=1 offset=4829"
                 */
                var metadata = result.getRecordMetadata();
                log.info(
                        "[{}] Event published successfully topic={} partition={} offset={}",
                        correlationId, topic,
                        metadata.partition(),
                        metadata.offset()
                );
            }
        });
    }

    /**
     * Publish with explicit partition routing.
     * Use when you need a specific partition (rare — usually let key handle it).
     */
    public void publishToPartition(String topic, Integer partition,
                                   String key, Object event,
                                   String correlationId) {
        log.info("[{}] Publishing to topic={} partition={} key={}",
                correlationId, topic, partition, key);

        kafkaTemplate.send(topic, partition, key, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("[{}] Partition publish failed: {}", correlationId, ex.getMessage());
                    }
                });
    }
}