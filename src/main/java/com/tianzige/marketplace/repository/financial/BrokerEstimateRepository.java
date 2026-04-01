package com.tianzige.marketplace.repository.financial;

import com.tianzige.marketplace.model.financial.BrokerEstimate;
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
public interface BrokerEstimateRepository extends JpaRepository<BrokerEstimate, UUID> {

    @EntityGraph(attributePaths = {"company"})
    Optional<BrokerEstimate> findWithCompanyById(UUID id);

    Page<BrokerEstimate> findByCompanyId(UUID companyId, Pageable pageable);

    Page<BrokerEstimate> findByCompanyIdAndMetric(UUID companyId, EstimateMetric metric, Pageable pageable);

    Page<BrokerEstimate> findByBrokerName(String brokerName, Pageable pageable);

    @Query("SELECT be FROM BrokerEstimate be WHERE be.company.id = :companyId " +
            "AND (:metric IS NULL OR be.metric = :metric) " +
            "AND (:fiscalYear IS NULL OR be.fiscalYear = :fiscalYear) " +
            "AND (:fiscalQuarter IS NULL OR be.fiscalQuarter = :fiscalQuarter)")
    Page<BrokerEstimate> findByCompanyIdAndFilters(
            @Param("companyId") UUID companyId,
            @Param("metric") EstimateMetric metric,
            @Param("fiscalYear") Integer fiscalYear,
            @Param("fiscalQuarter") Integer fiscalQuarter,
            Pageable pageable
    );
}
