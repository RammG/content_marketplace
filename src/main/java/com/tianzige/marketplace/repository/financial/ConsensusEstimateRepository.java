package com.tianzige.marketplace.repository.financial;

import com.tianzige.marketplace.model.financial.ConsensusEstimate;
import com.tianzige.marketplace.model.financial.EstimateMetric;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConsensusEstimateRepository extends JpaRepository<ConsensusEstimate, UUID> {

    @EntityGraph(attributePaths = {"company"})
    Optional<ConsensusEstimate> findWithCompanyById(UUID id);

    Page<ConsensusEstimate> findByCompanyId(UUID companyId, Pageable pageable);

    Page<ConsensusEstimate> findByCompanyIdAndMetric(UUID companyId, EstimateMetric metric, Pageable pageable);

    @Query("SELECT ce FROM ConsensusEstimate ce WHERE ce.company.id = :companyId " +
            "AND (:metric IS NULL OR ce.metric = :metric) " +
            "AND (:fiscalYear IS NULL OR ce.fiscalYear = :fiscalYear) " +
            "AND (:fiscalQuarter IS NULL OR ce.fiscalQuarter = :fiscalQuarter)")
    Page<ConsensusEstimate> findByCompanyIdAndFilters(
            @Param("companyId") UUID companyId,
            @Param("metric") EstimateMetric metric,
            @Param("fiscalYear") Integer fiscalYear,
            @Param("fiscalQuarter") Integer fiscalQuarter,
            Pageable pageable
    );

    @Query("SELECT ce FROM ConsensusEstimate ce WHERE ce.company.id = :companyId " +
            "AND ce.metric = :metric " +
            "ORDER BY ce.estimateDate DESC")
    Page<ConsensusEstimate> findLatestByCompanyIdAndMetric(
            @Param("companyId") UUID companyId,
            @Param("metric") EstimateMetric metric,
            Pageable pageable
    );
}
