package com.loanflow.commons.kafka;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

/**
 * Central Kafka configuration for all LoanFlow services.
 *
 * Every service imports this via commons dependency.
 * Each service provides its own group.id via application.yml
 * — that's the ONLY thing that differs per service.
 *
 * Why centralize this?
 * Retry logic, DLQ routing, serialization config —
 * these must be IDENTICAL across all services.
 * One place to update = consistency guaranteed.
 */
@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    // ══════════════════════════════════════════════════════════
    // PRODUCER CONFIGURATION
    // ══════════════════════════════════════════════════════════

    /**
     * ProducerFactory creates Kafka producers.
     * We configure it once — Spring reuses the same producer
     * instance (thread-safe) across all publish calls.
     */
    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> config = new HashMap<>();

        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

        // Key is always a String (our loanApplicationId / userId)
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

        // Value is any Java object → serialized to JSON by Jackson
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        /**
         * IDEMPOTENT PRODUCER — critical for financial systems.
         *
         * Without idempotence:
         *   Producer sends message → network timeout → producer retries
         *   Kafka received the first one but ACK got lost
         *   Kafka now has the message TWICE
         *   Payment processed twice → money debited twice → disaster
         *
         * With idempotence:
         *   Each message gets a sequence number
         *   Kafka deduplicates on the broker side
         *   Exactly-once delivery to the partition
         */
        config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);

        /**
         * ACKS=all means:
         *   Leader partition receives message AND
         *   All in-sync replicas acknowledge it
         *   Before producer gets success confirmation
         *
         * In single-broker Minikube: functionally same as acks=1
         * In 3-broker production: survives broker failure mid-write
         */
        config.put(ProducerConfig.ACKS_CONFIG, "all");

        /**
         * MAX_IN_FLIGHT = 1 with idempotence ensures ordering.
         * Without this: producer can have 5 messages "in flight"
         * simultaneously. If message 3 fails and retries, it might
         * arrive AFTER message 4. Ordering broken.
         * With 1 in-flight: strict ordering guaranteed.
         *
         * Trade-off: slightly lower throughput.
         * For financial events: correctness > throughput always.
         */
        config.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 1);

        config.put(ProducerConfig.RETRIES_CONFIG, 3);

        /**
         * Compression reduces network bandwidth.
         * SNAPPY chosen over GZIP: faster compression/decompression
         * at slightly worse compression ratio.
         * For high-volume event streams, CPU cost of GZIP isn't worth it.
         */
        config.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");

        return new DefaultKafkaProducerFactory<>(config);
    }

    /**
     * KafkaTemplate is the Spring abstraction over raw Kafka producer.
     * Injected into services via @Autowired.
     * Provides send(), sendDefault(), executeInTransaction() methods.
     */
    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        KafkaTemplate<String, Object> template =
                new KafkaTemplate<>(producerFactory());

        /**
         * Observe producer metrics automatically.
         * Prometheus will scrape: kafka_producer_record_send_total,
         * kafka_producer_record_error_total etc.
         * Shows up in Grafana dashboard without extra code.
         */
        template.setObservationEnabled(true);

        return template;
    }

    // ══════════════════════════════════════════════════════════
    // CONSUMER CONFIGURATION
    // ══════════════════════════════════════════════════════════

    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        Map<String, Object> config = new HashMap<>();

        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);

        /**
         * TRUSTED PACKAGES — security critical.
         *
         * JsonDeserializer needs to know which Java packages
         * are safe to instantiate from incoming JSON.
         *
         * Without this: a malicious actor could publish a Kafka
         * message with __TypeId__ header pointing to any class
         * on your classpath. Deserialization attack vector.
         *
         * We explicitly trust only our commons events package.
         */
        config.put(JsonDeserializer.TRUSTED_PACKAGES,
                "com.loanflow.commons.events");

        config.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, true);

        /**
         * AUTO_OFFSET_RESET = "earliest"
         *
         * Scenario: Credit Service is down for 2 hours.
         * 500 loan applications submitted during that time.
         * Credit Service restarts.
         *
         * "earliest": reads from where it LEFT OFF → processes all 500 ✅
         * "latest":   reads only NEW messages → 500 loans never scored ❌
         *
         * Always use "earliest" for event-driven microservices.
         * Use "latest" only for real-time dashboards where
         * historical data is irrelevant.
         */
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        /**
         * ENABLE_AUTO_COMMIT = false
         *
         * Auto-commit: Kafka commits offset every 5 seconds automatically.
         * Problem: Consumer reads message, starts processing,
         * auto-commit fires at 5s mark, then consumer crashes at 6s.
         * Message marked as processed but wasn't. LOST.
         *
         * Manual commit (AFTER_PROCESSING): offset committed only
         * AFTER @KafkaListener method returns successfully.
         * If method throws → no commit → message retried. SAFE.
         */
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        /**
         * Fetch 500KB max per poll for performance tuning.
         * Reduce if consumers are slow (avoid timeout warnings).
         * Increase if events are large and throughput matters.
         */
        config.put(ConsumerConfig.MAX_PARTITION_FETCH_BYTES_CONFIG, 512 * 1024);

        return new DefaultKafkaConsumerFactory<>(config);
    }

    /**
     * The container factory is what @KafkaListener uses internally.
     * This is where we attach: retry logic, DLQ routing, commit mode.
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object>
    kafkaListenerContainerFactory() {

        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(consumerFactory());

        /**
         * CONCURRENCY = 3
         * Runs 3 consumer threads per @KafkaListener.
         * Each thread handles one partition.
         * Matches our partition count of 3 per topic.
         *
         * If concurrency > partitions: extra threads idle (waste)
         * If concurrency < partitions: some partitions wait (slow)
         * Match them for optimal throughput.
         */
        factory.setConcurrency(3);

        /**
         * MANUAL_IMMEDIATE: we control exactly when offset is committed.
         * The @KafkaListener method receives Acknowledgment parameter
         * and calls ack.acknowledge() explicitly after processing.
         *
         * This gives us: process → verify → commit
         * Not: commit → process (auto-commit disaster)
         */
        factory.getContainerProperties()
                .setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);

        // Attach retry + DLQ error handler
        factory.setCommonErrorHandler(errorHandler());

        factory.getContainerProperties().setObservationEnabled(true);

        return factory;
    }

    // ══════════════════════════════════════════════════════════
    // RETRY + DEAD LETTER QUEUE
    // ══════════════════════════════════════════════════════════

    /**
     * DefaultErrorHandler replaces the old SeekToCurrentErrorHandler.
     * It handles: retry scheduling + DLQ routing in one place.
     */
    @Bean
    public DefaultErrorHandler errorHandler() {

        /**
         * DeadLetterPublishingRecoverer:
         * After all retries exhausted, publishes the failed message
         * to a DLT (Dead Letter Topic).
         *
         * DLT naming convention (Spring default):
         *   original topic: loan.application.submitted
         *   DLT:            loan.application.submitted.DLT
         *
         * The DLT message includes special headers:
         *   kafka_dlt-exception-cause-fqcn   → exception class name
         *   kafka_dlt-exception-message      → exception message
         *   kafka_dlt-exception-stacktrace   → full stack trace
         *   kafka_dlt-original-topic         → where it came from
         *   kafka_dlt-original-partition     → which partition
         *   kafka_dlt-original-offset        → original offset
         *
         * This lets you: find the bad message, understand WHY it failed,
         * fix the issue, and replay it manually.
         */
        DeadLetterPublishingRecoverer recoverer =
                new DeadLetterPublishingRecoverer(kafkaTemplate());

        /**
         * FixedBackOff(interval, maxAttempts):
         *   interval = 1000ms between retries
         *   maxAttempts = 3 total retry attempts
         *
         * Total time before DLQ: 3 * 1000ms = 3 seconds
         *
         * For transient failures (DB timeout, network blip):
         *   3 retries over 3 seconds catches most of them.
         *
         * For permanent failures (bad data, null pointer):
         *   Retries won't help — go to DLQ immediately.
         *   We handle this below with non-retryable exceptions.
         *
         * Alternative: ExponentialBackOff(1000, 2.0) with maxElapsed(30s)
         *   Retry at: 1s, 2s, 4s, 8s, 16s → total 31 seconds
         *   Better for downstream services that need more recovery time.
         *   We use Fixed here for simplicity and predictability.
         */
        FixedBackOff backOff = new FixedBackOff(1000L, 3L);

        DefaultErrorHandler handler =
                new DefaultErrorHandler(recoverer, backOff);

        /**
         * NON-RETRYABLE EXCEPTIONS
         *
         * For these exceptions, retrying is pointless.
         * Skip retries entirely → go straight to DLQ.
         *
         * IllegalArgumentException: bad data — retrying won't fix bad data
         * NullPointerException: code bug — retrying won't fix a bug
         * ClassCastException: wrong type — retrying won't change the type
         *
         * Why this matters:
         * Without this, a message with a null required field would:
         *   retry 3 times × 1 second = waste 3 seconds
         *   Then go to DLQ anyway
         * With this: goes to DLQ instantly. Consumer unblocked faster.
         */
        handler.addNotRetryableExceptions(
                IllegalArgumentException.class,
                NullPointerException.class,
                ClassCastException.class
        );

        return handler;
    }
}