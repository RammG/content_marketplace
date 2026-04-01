package com.tianzige.marketplace.repository.financial;

import com.tianzige.marketplace.model.financial.DataSource;
import com.tianzige.marketplace.model.financial.IncomeStatement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IncomeStatementRepository extends JpaRepository<IncomeStatement, UUID> {

    @EntityGraph(attributePaths = {"financialPeriod", "financialPeriod.company"})
    Optional<IncomeStatement> findWithPeriodById(UUID id);

    List<IncomeStatement> findByFinancialPeriodId(UUID financialPeriodId);

    Optional<IncomeStatement> findByFinancialPeriodIdAndDataSource(
            UUID financialPeriodId,
            DataSource dataSource
    );

    Page<IncomeStatement> findByFinancialPeriodCompanyId(UUID companyId, Pageable pageable);
}
