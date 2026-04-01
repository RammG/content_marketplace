package com.tianzige.marketplace.ingest.async;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tianzige.marketplace.ingest.IngestSummary;
import com.tianzige.marketplace.ingest.ParsedFile;
import com.tianzige.marketplace.ingest.SecZipParser;
import com.tianzige.marketplace.ingest.job.IngestJob;
import com.tianzige.marketplace.ingest.job.IngestJobRepository;
import com.tianzige.marketplace.ingest.job.IngestJobStatus;
import com.tianzige.marketplace.ingest.storage.FileStorageService;
import com.tianzige.marketplace.kafka.event.IngestCompletedEvent;
import com.tianzige.marketplace.kafka.event.IngestFailedEvent;
import com.tianzige.marketplace.kafka.event.IngestRequestEvent;
import com.tianzige.marketplace.kafka.event.IngestStartedEvent;
import com.tianzige.marketplace.kafka.producer.IngestEventProducer;
import com.tianzige.marketplace.model.content.ContentDocument;
import com.tianzige.marketplace.model.dir.DataItem;
import com.tianzige.marketplace.repository.ContentRepository;
import com.tianzige.marketplace.repository.DataItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AsyncIngestService {

    private static final int BATCH_SIZE = 500;
    private static final String PROVIDER = "SEC";
    private static final String ES_INDEX = "content_documents";

    private final SecZipParser zipParser;
    private final DataItemRepository dataItemRepository;
    private final ContentRepository contentRepository;
    private final ObjectMapper objectMapper;
    private final FileStorageService fileStorageService;
    private final IngestJobRepository jobRepository;
    private final IngestProgressReporter progressReporter;
    private final IngestEventProducer eventProducer;

    public void processIngestRequest(IngestRequestEvent request) {
        UUID jobId = request.getJobId();
        log.info("Processing ingest request for job: {}", jobId);

        try {
            IngestJob job = progressReporter.getJob(jobId);
            if (job == null) {
                log.error("Job not found: {}", jobId);
                return;
            }

            if (job.getStatus() == IngestJobStatus.CANCELLED) {
                log.info("Job {} was cancelled, skipping", jobId);
                return;
            }

            InputStream zipStream = fileStorageService.retrieve(request.getFileReference());
            List<ParsedFile> parsedFiles = zipParser.parse(zipStream, request.getMaxRowsPerFile());

            publishStarted(jobId, parsedFiles.size());
            progressReporter.reportStarted(jobId, parsedFiles.size());

            List<IngestSummary.FileResult> results = new ArrayList<>();
            int filesProcessed = 0;

            for (ParsedFile parsedFile : parsedFiles) {
                IngestJob currentJob = progressReporter.getJob(jobId);
                if (currentJob != null && currentJob.getStatus() == IngestJobStatus.CANCELLED) {
                    log.info("Job {} cancelled during processing", jobId);
                    return;
                }

                progressReporter.reportProgress(jobId, parsedFile.getFilename(), filesProcessed,
                        parsedFiles.size(), 0);

                IngestSummary.FileResult result = ingestFile(parsedFile, request.getQuarter());
                results.add(result);
                filesProcessed++;

                progressReporter.reportFileCompleted(jobId, parsedFile.getFilename(),
                        filesProcessed, parsedFiles.size(), result.getDocumentsIndexed());

                log.info("Job {} - Ingested {}: {} DIR entry, {} documents indexed",
                        jobId, parsedFile.getFilename(), result.getDataItemId(), result.getDocumentsIndexed());
            }

            IngestSummary summary = new IngestSummary(request.getQuarter(), results);
            completeJob(jobId, summary);

            fileStorageService.delete(request.getFileReference());
            log.info("Job {} completed successfully", jobId);

        } catch (Exception e) {
            log.error("Job {} failed", jobId, e);
            failJob(jobId, e);
        }
    }

    private void publishStarted(UUID jobId, int totalFiles) {
        IngestStartedEvent event = IngestStartedEvent.builder()
                .jobId(jobId)
                .totalFiles(totalFiles)
                .startedAt(Instant.now())
                .build();
        eventProducer.publishStarted(event);
    }

    @Transactional
    public void completeJob(UUID jobId, IngestSummary summary) {
        jobRepository.findById(jobId).ifPresent(job -> {
            job.setStatus(IngestJobStatus.COMPLETED);
            job.setPercentComplete(100.0);
            job.setCompletedAt(LocalDateTime.now());
            job.setResultSummary(summaryToMap(summary));
            jobRepository.save(job);

            IngestCompletedEvent event = IngestCompletedEvent.builder()
                    .jobId(jobId)
                    .totalFilesProcessed(summary.totalFilesProcessed())
                    .totalDocumentsIndexed(summary.totalDocumentsIndexed())
                    .summary(summary)
                    .completedAt(Instant.now())
                    .build();
            eventProducer.publishCompleted(event);
        });
    }

    @Transactional
    public void failJob(UUID jobId, Exception e) {
        jobRepository.findById(jobId).ifPresent(job -> {
            job.setStatus(IngestJobStatus.FAILED);
            job.setErrorMessage(e.getMessage());
            job.setCompletedAt(LocalDateTime.now());
            jobRepository.save(job);

            IngestFailedEvent event = IngestFailedEvent.builder()
                    .jobId(jobId)
                    .errorMessage(e.getMessage())
                    .errorType(e.getClass().getSimpleName())
                    .filesProcessedBeforeFailure(job.getFilesProcessed())
                    .documentsIndexedBeforeFailure(job.getDocumentsIndexed())
                    .failedAt(Instant.now())
                    .build();
            eventProducer.publishFailed(event);
        });
    }

    @Transactional
    protected IngestSummary.FileResult ingestFile(ParsedFile parsedFile, String quarter) {
        Map<String, Object> metadata = buildDirMetadata(parsedFile, quarter);

        DataItem dataItem = DataItem.builder()
                .name("SEC " + quarter + " - " + parsedFile.getFilename())
                .sourceType(DataItem.SourceType.OTHER)
                .sourceFormat("text/tab-separated-values")
                .contentType("financial/sec-edgar")
                .provider(PROVIDER)
                .version(quarter)
                .description("SEC EDGAR Financial Statements Data Set - " + parsedFile.getFilename())
                .elasticsearchId(ES_INDEX)
                .metadata(metadata)
                .build();

        DataItem savedDataItem = dataItemRepository.save(dataItem);
        log.debug("Registered DataItem {} for {}", savedDataItem.getId(), parsedFile.getFilename());

        int totalIndexed = indexRowsBatched(parsedFile, savedDataItem.getId().toString());

        return IngestSummary.FileResult.builder()
                .filename(parsedFile.getFilename())
                .dataItemId(savedDataItem.getId())
                .documentsIndexed(totalIndexed)
                .totalRowsInFile(parsedFile.getTotalRows())
                .fieldCount(parsedFile.getFields().size())
                .build();
    }

    private int indexRowsBatched(ParsedFile parsedFile, String dataItemId) {
        List<Map<String, Object>> rows = parsedFile.getRows();
        int indexed = 0;

        for (int i = 0; i < rows.size(); i += BATCH_SIZE) {
            List<Map<String, Object>> batch = rows.subList(i, Math.min(i + BATCH_SIZE, rows.size()));

            List<ContentDocument> docs = batch.stream()
                    .map(row -> toContentDocument(row, dataItemId, parsedFile))
                    .collect(Collectors.toList());

            contentRepository.saveAll(docs);
            indexed += docs.size();
            log.debug("Indexed batch {}/{} for {}", indexed, rows.size(), parsedFile.getFilename());
        }

        return indexed;
    }

    private ContentDocument toContentDocument(Map<String, Object> row, String dataItemId, ParsedFile parsedFile) {
        String rawJson = toJson(row);
        String title = extractTitle(row, parsedFile.getFilename());

        return ContentDocument.builder()
                .id(UUID.randomUUID().toString())
                .dataItemId(dataItemId)
                .rawContent(rawJson)
                .contentType("financial/sec-edgar")
                .sourceType("TSV")
                .provider(PROVIDER)
                .title(title)
                .extractedFields(new LinkedHashMap<>(row))
                .indexedAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private String extractTitle(Map<String, Object> row, String filename) {
        return switch (filename) {
            case "sub.txt" -> {
                String adsh = str(row, "adsh");
                String name = str(row, "name");
                yield name.isEmpty() ? adsh : name + " (" + adsh + ")";
            }
            case "tag.txt" -> {
                String tag = str(row, "tag");
                String label = str(row, "tlabel");
                yield label.isEmpty() ? tag : label + " [" + tag + "]";
            }
            case "num.txt" -> str(row, "adsh") + " / " + str(row, "tag");
            case "pre.txt" -> str(row, "adsh") + " / " + str(row, "tag") + " / " + str(row, "stmt");
            default -> filename;
        };
    }

    private Map<String, Object> buildDirMetadata(ParsedFile parsedFile, String quarter) {
        List<Map<String, Object>> fieldMeta = parsedFile.getFields().stream()
                .map(f -> {
                    Map<String, Object> fm = new LinkedHashMap<>();
                    fm.put("name", f.getName());
                    fm.put("type", f.getType());
                    fm.put("nullable", f.isNullable());
                    return fm;
                })
                .collect(Collectors.toList());

        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("filename", parsedFile.getFilename());
        meta.put("quarter", quarter);
        meta.put("fields", fieldMeta);
        meta.put("fieldCount", parsedFile.getFields().size());
        meta.put("totalRows", parsedFile.getTotalRows());
        meta.put("indexedRows", parsedFile.getRows().size());
        return meta;
    }

    private Map<String, Object> summaryToMap(IngestSummary summary) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("quarter", summary.getQuarter());
        map.put("totalFilesProcessed", summary.totalFilesProcessed());
        map.put("totalDocumentsIndexed", summary.totalDocumentsIndexed());

        List<Map<String, Object>> fileResults = summary.getFileResults().stream()
                .map(fr -> {
                    Map<String, Object> frMap = new LinkedHashMap<>();
                    frMap.put("filename", fr.getFilename());
                    frMap.put("dataItemId", fr.getDataItemId().toString());
                    frMap.put("documentsIndexed", fr.getDocumentsIndexed());
                    frMap.put("totalRowsInFile", fr.getTotalRowsInFile());
                    frMap.put("fieldCount", fr.getFieldCount());
                    return frMap;
                })
                .collect(Collectors.toList());
        map.put("fileResults", fileResults);

        return map;
    }

    private String toJson(Map<String, Object> row) {
        try {
            return objectMapper.writeValueAsString(row);
        } catch (Exception e) {
            return row.toString();
        }
    }

    private String str(Map<String, Object> row, String key) {
        Object v = row.get(key);
        return v == null ? "" : v.toString();
    }
}
