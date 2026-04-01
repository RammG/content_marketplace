package com.tianzige.marketplace.repository.financial;

import com.tianzige.marketplace.model.financial.BalanceSheet;
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
public interface BalanceSheetRepository extends JpaRepository<BalanceSheet, UUID> {

    @EntityGraph(attributePaths = {"financialPeriod", "financialPeriod.company"})
    Optional<BalanceSheet> findWithPeriodById(UUID id);

    List<BalanceSheet> findByFinancialPeriodId(UUID financialPeriodId);

    Optional<BalanceSheet> findByFinancialPeriodIdAndDataSource(
            UUID financialPeriodId,
            DataSource dataSource
    );

    Page<BalanceSheet> findByFinancialPeriodCompanyId(UUID companyId, Pageable pageable);
}
