package com.tianzige.marketplace.kafka.event;

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
public class IngestProgressEvent {

    private UUID jobId;
    private String currentFile;
    private int filesProcessed;
    private int totalFiles;
    private int documentsIndexed;
    private double percentComplete;
    private Instant timestamp;
}
