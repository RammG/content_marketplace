package com.tianzige.marketplace.repository.financial;

import com.tianzige.marketplace.model.financial.FinancialPeriod;
import com.tianzige.marketplace.model.financial.PeriodType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FinancialPeriodRepository extends JpaRepository<FinancialPeriod, UUID> {

    @EntityGraph(attributePaths = {"company"})
    Optional<FinancialPeriod> findWithCompanyById(UUID id);

    Page<FinancialPeriod> findByCompanyId(UUID companyId, Pageable pageable);

    Page<FinancialPeriod> findByCompanyIdAndPeriodType(
            UUID companyId,
            PeriodType periodType,
            Pageable pageable
    );

    @Query("SELECT fp FROM FinancialPeriod fp WHERE fp.company.id = :companyId " +
            "AND (:periodType IS NULL OR fp.periodType = :periodType) " +
            "AND (:fiscalYear IS NULL OR fp.fiscalYear = :fiscalYear)")
    Page<FinancialPeriod> findByCompanyIdAndFilters(
            @Param("companyId") UUID companyId,
            @Param("periodType") PeriodType periodType,
            @Param("fiscalYear") Integer fiscalYear,
            Pageable pageable
    );

    @Query("SELECT fp FROM FinancialPeriod fp WHERE fp.company.id = :companyId " +
            "AND fp.periodType = :periodType ORDER BY fp.fiscalYear DESC, fp.fiscalQuarter DESC NULLS LAST")
    List<FinancialPeriod> findLatestByCompanyIdAndPeriodType(
            @Param("companyId") UUID companyId,
            @Param("periodType") PeriodType periodType,
            Pageable pageable
    );
}
