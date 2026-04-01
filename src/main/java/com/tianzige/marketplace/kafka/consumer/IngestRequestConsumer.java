package com.tianzige.marketplace.kafka.consumer;

import com.tianzige.marketplace.ingest.async.AsyncIngestService;
import com.tianzige.marketplace.kafka.event.IngestRequestEvent;
import com.tianzige.marketplace.kafka.producer.IngestEventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class IngestRequestConsumer {

    private final AsyncIngestService asyncIngestService;
    private final IngestEventProducer eventProducer;

    @KafkaListener(
            topics = "${marketplace.ingest.kafka.topics.requests}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(ConsumerRecord<String, IngestRequestEvent> record) {
        IngestRequestEvent event = record.value();
        log.info("Received ingest request: jobId={}, quarter={}", event.getJobId(), event.getQuarter());

        try {
            asyncIngestService.processIngestRequest(event);
        } catch (Exception e) {
            log.error("Failed to process ingest request for job: {}", event.getJobId(), e);
            eventProducer.publishToDlq(event, "Processing failed: " + e.getMessage());
            throw e;
        }
    }
}
