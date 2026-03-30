package com.tianzige.marketplace.ingest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.UUID;

@Value
@AllArgsConstructor
public class IngestSummary {

    String quarter;
    List<FileResult> fileResults;

    public int totalDocumentsIndexed() {
        return fileResults.stream().mapToInt(FileResult::getDocumentsIndexed).sum();
    }

    public int totalFilesProcessed() {
        return fileResults.size();
    }

    @Value
    @Builder
    public static class FileResult {
        String filename;
        UUID dataItemId;
        int documentsIndexed;
        long totalRowsInFile;
        int fieldCount;
    }
}
