package com.tianzige.marketplace.kafka.event;

import com.tianzige.marketplace.ingest.IngestSummary;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IngestCompletedEvent {

    private UUID jobId;
    private int totalFilesProcessed;
    private int totalDocumentsIndexed;
    private IngestSummary summary;
    private Instant completedAt;
}
