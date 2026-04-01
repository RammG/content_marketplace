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
public class IngestRequestEvent {

    private UUID jobId;
    private String fileReference;
    private String quarter;
    private int maxRowsPerFile;
    private Instant requestedAt;
}
