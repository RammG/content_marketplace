package com.tianzige.marketplace.ingest.job;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface IngestJobRepository extends JpaRepository<IngestJob, UUID> {

    Page<IngestJob> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<IngestJob> findByStatus(IngestJobStatus status);

    List<IngestJob> findByQuarter(String quarter);
}
