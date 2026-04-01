package com.tianzige.marketplace.ingest.job;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "ingest_jobs")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IngestJob {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IngestJobStatus status;

    @Column(name = "file_reference", nullable = false)
    private String fileReference;

    @Column(nullable = false)
    private String quarter;

    @Column(name = "max_rows_per_file")
    private int maxRowsPerFile;

    @Column(name = "current_file")
    private String currentFile;

    @Column(name = "files_processed")
    private int filesProcessed;

    @Column(name = "total_files")
    private int totalFiles;

    @Column(name = "documents_indexed")
    private int documentsIndexed;

    @Column(name = "percent_complete")
    private double percentComplete;

    @Type(JsonType.class)
    @Column(name = "result_summary", columnDefinition = "jsonb")
    private Map<String, Object> resultSummary;

    @Column(name = "error_message", length = 2000)
    private String errorMessage;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;
}
