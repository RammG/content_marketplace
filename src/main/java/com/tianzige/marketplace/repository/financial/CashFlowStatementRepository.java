package com.tianzige.marketplace.repository.financial;

import com.tianzige.marketplace.model.financial.CashFlowStatement;
import com.tianzige.marketplace.model.financial.DataSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CashFlowStatementRepository extends JpaRepository<CashFlowStatement, UUID> {

    @EntityGraph(attributePaths = {"financialPeriod", "financialPeriod.company"})
    Optional<CashFlowStatement> findWithPeriodById(UUID id);

    List<CashFlowStatement> findByFinancialPeriodId(UUID financialPeriodId);

    Optional<CashFlowStatement> findByFinancialPeriodIdAndDataSource(
            UUID financialPeriodId,
            DataSource dataSource
    );

    Page<CashFlowStatement> findByFinancialPeriodCompanyId(UUID companyId, Pageable pageable);
}
