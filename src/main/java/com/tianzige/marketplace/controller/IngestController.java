package com.tianzige.marketplace.controller;

import com.tianzige.marketplace.ingest.job.IngestJob;
import com.tianzige.marketplace.ingest.job.IngestJobRepository;
import com.tianzige.marketplace.ingest.job.IngestJobStatus;
import com.tianzige.marketplace.ingest.storage.FileStorageService;
import com.tianzige.marketplace.kafka.event.IngestRequestEvent;
import com.tianzige.marketplace.kafka.producer.IngestEventProducer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ingest")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Ingest", description = "Async data feed ingestion API")
public class IngestController {

    private final FileStorageService fileStorageService;
    private final IngestJobRepository jobRepository;
    private final IngestEventProducer eventProducer;

    @PostMapping("/sec")
    @Operation(summary = "Submit SEC dataset for async ingestion",
               description = "Uploads a SEC quarterly ZIP file and starts async ingestion. Returns immediately with job ID.")
    public ResponseEntity<Map<String, Object>> submitSecIngest(
            @Parameter(description = "SEC quarterly dataset ZIP file")
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "Quarter identifier, e.g., 2025Q4")
            @RequestParam("quarter") String quarter,
            @Parameter(description = "Max rows per file (0 = unlimited)")
            @RequestParam(value = "maxRowsPerFile", defaultValue = "0") int maxRowsPerFile) throws IOException {

        log.info("Received SEC ingest request: quarter={}, filename={}, size={}",
                quarter, file.getOriginalFilename(), file.getSize());

        String fileReference = fileStorageService.store(file.getInputStream(), file.getOriginalFilename());

        IngestJob job = IngestJob.builder()
                .status(IngestJobStatus.PENDING)
                .fileReference(fileReference)
                .quarter(quarter)
                .maxRowsPerFile(maxRowsPerFile)
                .filesProcessed(0)
                .documentsIndexed(0)
                .percentComplete(0.0)
                .build();

        IngestJob savedJob = jobRepository.save(job);
        UUID jobId = savedJob.getId();

        IngestRequestEvent event = IngestRequestEvent.builder()
                .jobId(jobId)
                .fileReference(fileReference)
                .quarter(quarter)
                .maxRowsPerFile(maxRowsPerFile)
                .requestedAt(Instant.now())
                .build();

        eventProducer.publishRequest(event);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("jobId", jobId.toString());
        response.put("status", "PENDING");
        response.put("statusUrl", "/api/v1/ingest/jobs/" + jobId);

        return ResponseEntity.accepted().body(response);
    }

    @GetMapping("/jobs/{jobId}")
    @Operation(summary = "Get job status", description = "Returns the current status and progress of an ingestion job")
    public ResponseEntity<Map<String, Object>> getJobStatus(
            @Parameter(description = "Job ID returned from submit endpoint")
            @PathVariable UUID jobId) {

        return jobRepository.findById(jobId)
                .map(job -> ResponseEntity.ok(jobToResponse(job)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/jobs")
    @Operation(summary = "List jobs", description = "Returns paginated list of all ingestion jobs")
    public ResponseEntity<Map<String, Object>> listJobs(
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<IngestJob> jobPage = jobRepository.findAllByOrderByCreatedAtDesc(pageable);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("jobs", jobPage.getContent().stream()
                .map(this::jobToResponse)
                .toList());
        response.put("page", page);
        response.put("size", size);
        response.put("totalElements", jobPage.getTotalElements());
        response.put("totalPages", jobPage.getTotalPages());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/jobs/{jobId}/cancel")
    @Operation(summary = "Cancel job", description = "Cancels a pending or in-progress job")
    public ResponseEntity<Map<String, Object>> cancelJob(
            @Parameter(description = "Job ID to cancel")
            @PathVariable UUID jobId) {

        return jobRepository.findById(jobId)
                .map(job -> {
                    if (job.getStatus() == IngestJobStatus.COMPLETED ||
                        job.getStatus() == IngestJobStatus.FAILED ||
                        job.getStatus() == IngestJobStatus.CANCELLED) {
                        Map<String, Object> response = new LinkedHashMap<>();
                        response.put("success", false);
                        response.put("message", "Job cannot be cancelled, status: " + job.getStatus());
                        return ResponseEntity.badRequest().body(response);
                    }

                    job.setStatus(IngestJobStatus.CANCELLED);
                    jobRepository.save(job);

                    try {
                        fileStorageService.delete(job.getFileReference());
                    } catch (IOException e) {
                        log.warn("Failed to delete file for cancelled job: {}", jobId, e);
                    }

                    Map<String, Object> response = new LinkedHashMap<>();
                    response.put("success", true);
                    response.put("jobId", jobId.toString());
                    response.put("status", "CANCELLED");

                    return ResponseEntity.ok(response);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    private Map<String, Object> jobToResponse(IngestJob job) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("jobId", job.getId().toString());
        response.put("status", job.getStatus().name());
        response.put("quarter", job.getQuarter());
        response.put("currentFile", job.getCurrentFile());
        response.put("filesProcessed", job.getFilesProcessed());
        response.put("totalFiles", job.getTotalFiles());
        response.put("documentsIndexed", job.getDocumentsIndexed());
        response.put("percentComplete", job.getPercentComplete());
        response.put("createdAt", job.getCreatedAt());
        response.put("startedAt", job.getStartedAt());
        response.put("completedAt", job.getCompletedAt());

        if (job.getStatus() == IngestJobStatus.COMPLETED && job.getResultSummary() != null) {
            response.put("resultSummary", job.getResultSummary());
        }

        if (job.getStatus() == IngestJobStatus.FAILED && job.getErrorMessage() != null) {
            response.put("errorMessage", job.getErrorMessage());
        }

        return response;
    }
}
