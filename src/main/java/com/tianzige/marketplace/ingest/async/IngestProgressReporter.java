package com.tianzige.marketplace.ingest.async;

import com.tianzige.marketplace.ingest.job.IngestJob;
import com.tianzige.marketplace.ingest.job.IngestJobRepository;
import com.tianzige.marketplace.ingest.job.IngestJobStatus;
import com.tianzige.marketplace.kafka.event.IngestProgressEvent;
import com.tianzige.marketplace.kafka.producer.IngestEventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class IngestProgressReporter {

    private final IngestJobRepository jobRepository;
    private final IngestEventProducer eventProducer;

    @Transactional
    public void reportStarted(UUID jobId, int totalFiles) {
        jobRepository.findById(jobId).ifPresent(job -> {
            job.setStatus(IngestJobStatus.IN_PROGRESS);
            job.setTotalFiles(totalFiles);
            job.setStartedAt(LocalDateTime.now());
            jobRepository.save(job);
            log.info("Job {} started with {} files", jobId, totalFiles);
        });
    }

    @Transactional
    public void reportProgress(UUID jobId, String currentFile, int filesProcessed,
                               int totalFiles, int documentsIndexed) {
        jobRepository.findById(jobId).ifPresent(job -> {
            double percentComplete = totalFiles > 0 ? (filesProcessed * 100.0 / totalFiles) : 0;

            job.setCurrentFile(currentFile);
            job.setFilesProcessed(filesProcessed);
            job.setDocumentsIndexed(documentsIndexed);
            job.setPercentComplete(percentComplete);
            jobRepository.save(job);

            IngestProgressEvent event = IngestProgressEvent.builder()
                    .jobId(jobId)
                    .currentFile(currentFile)
                    .filesProcessed(filesProcessed)
                    .totalFiles(totalFiles)
                    .documentsIndexed(documentsIndexed)
                    .percentComplete(percentComplete)
                    .timestamp(Instant.now())
                    .build();

            eventProducer.publishProgress(event);
        });
    }

    @Transactional
    public void reportFileCompleted(UUID jobId, String filename, int filesProcessed,
                                    int totalFiles, int newDocsIndexed) {
        jobRepository.findById(jobId).ifPresent(job -> {
            int totalDocsIndexed = job.getDocumentsIndexed() + newDocsIndexed;
            double percentComplete = totalFiles > 0 ? (filesProcessed * 100.0 / totalFiles) : 0;

            job.setCurrentFile(null);
            job.setFilesProcessed(filesProcessed);
            job.setDocumentsIndexed(totalDocsIndexed);
            job.setPercentComplete(percentComplete);
            jobRepository.save(job);

            log.debug("Job {} file {} completed: {}/{} files, {} docs total",
                    jobId, filename, filesProcessed, totalFiles, totalDocsIndexed);

            IngestProgressEvent event = IngestProgressEvent.builder()
                    .jobId(jobId)
                    .currentFile(filename + " (completed)")
                    .filesProcessed(filesProcessed)
                    .totalFiles(totalFiles)
                    .documentsIndexed(totalDocsIndexed)
                    .percentComplete(percentComplete)
                    .timestamp(Instant.now())
                    .build();

            eventProducer.publishProgress(event);
        });
    }

    public IngestJob getJob(UUID jobId) {
        return jobRepository.findById(jobId).orElse(null);
    }
}
