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
public class IngestStartedEvent {

    private UUID jobId;
    private int totalFiles;
    private Instant startedAt;
}
