package com.tianzige.marketplace.kafka.consumer;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class IngestDlqHandler {

    @KafkaListener(
            topics = "${marketplace.ingest.kafka.topics.dlq}",
            groupId = "marketplace-dlq-group"
    )
    public void handleDlqMessage(ConsumerRecord<String, Object> record) {
        log.error("DLQ message received - key: {}, value: {}, partition: {}, offset: {}",
                record.key(),
                record.value(),
                record.partition(),
                record.offset());
    }
}
