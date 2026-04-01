package com.tianzige.marketplace.kafka.producer;

import com.tianzige.marketplace.kafka.event.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class IngestEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${marketplace.ingest.kafka.topics.requests}")
    private String requestsTopic;

    @Value("${marketplace.ingest.kafka.topics.events}")
    private String eventsTopic;

    @Value("${marketplace.ingest.kafka.topics.dlq}")
    private String dlqTopic;

    public CompletableFuture<SendResult<String, Object>> publishRequest(IngestRequestEvent event) {
        log.info("Publishing ingest request for job: {}", event.getJobId());
        return kafkaTemplate.send(requestsTopic, event.getJobId().toString(), event);
    }

    public CompletableFuture<SendResult<String, Object>> publishStarted(IngestStartedEvent event) {
        log.debug("Publishing ingest started for job: {}", event.getJobId());
        return kafkaTemplate.send(eventsTopic, event.getJobId().toString(), event);
    }

    public CompletableFuture<SendResult<String, Object>> publishProgress(IngestProgressEvent event) {
        log.debug("Publishing progress for job {}: {}%", event.getJobId(), event.getPercentComplete());
        return kafkaTemplate.send(eventsTopic, event.getJobId().toString(), event);
    }

    public CompletableFuture<SendResult<String, Object>> publishCompleted(IngestCompletedEvent event) {
        log.info("Publishing ingest completed for job: {}, docs indexed: {}",
                event.getJobId(), event.getTotalDocumentsIndexed());
        return kafkaTemplate.send(eventsTopic, event.getJobId().toString(), event);
    }

    public CompletableFuture<SendResult<String, Object>> publishFailed(IngestFailedEvent event) {
        log.error("Publishing ingest failed for job: {}, error: {}",
                event.getJobId(), event.getErrorMessage());
        return kafkaTemplate.send(eventsTopic, event.getJobId().toString(), event);
    }

    public CompletableFuture<SendResult<String, Object>> publishToDlq(Object event, String reason) {
        log.warn("Publishing to DLQ, reason: {}", reason);
        return kafkaTemplate.send(dlqTopic, reason, event);
    }
}
