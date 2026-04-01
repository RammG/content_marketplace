package com.tianzige.marketplace.service.financial;

import com.tianzige.marketplace.model.financial.*;
import com.tianzige.marketplace.repository.financial.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FinancialStatementService {

    private final FinancialPeriodRepository financialPeriodRepository;
    private final BalanceSheetRepository balanceSheetRepository;
    private final IncomeStatementRepository incomeStatementRepository;
    private final CashFlowStatementRepository cashFlowStatementRepository;

    // Financial Period methods
    @Transactional(readOnly = true)
    public Optional<FinancialPeriod> findPeriodById(UUID id) {
        return financialPeriodRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Page<FinancialPeriod> findPeriodsByCompanyId(
            UUID companyId,
            PeriodType periodType,
            Integer fiscalYear,
            Pageable pageable) {
        return financialPeriodRepository.findByCompanyIdAndFilters(
                companyId, periodType, fiscalYear, pageable
        );
    }

    @Transactional(readOnly = true)
    public Optional<FinancialPeriod> findLatestPeriod(UUID companyId, PeriodType periodType) {
        List<FinancialPeriod> periods = financialPeriodRepository.findLatestByCompanyIdAndPeriodType(
                companyId, periodType, PageRequest.of(0, 1)
        );
        return periods.isEmpty() ? Optional.empty() : Optional.of(periods.get(0));
    }

    // Balance Sheet methods
    @Transactional(readOnly = true)
    public Optional<BalanceSheet> findBalanceSheetById(UUID id) {
        return balanceSheetRepository.findWithPeriodById(id);
    }

    @Transactional(readOnly = true)
    public List<BalanceSheet> findBalanceSheetsByPeriodId(UUID periodId) {
        return balanceSheetRepository.findByFinancialPeriodId(periodId);
    }

    // Income Statement methods
    @Transactional(readOnly = true)
    public Optional<IncomeStatement> findIncomeStatementById(UUID id) {
        return incomeStatementRepository.findWithPeriodById(id);
    }

    @Transactional(readOnly = true)
    public List<IncomeStatement> findIncomeStatementsByPeriodId(UUID periodId) {
        return incomeStatementRepository.findByFinancialPeriodId(periodId);
    }

    // Cash Flow Statement methods
    @Transactional(readOnly = true)
    public Optional<CashFlowStatement> findCashFlowStatementById(UUID id) {
        return cashFlowStatementRepository.findWithPeriodById(id);
    }

    @Transactional(readOnly = true)
    public List<CashFlowStatement> findCashFlowStatementsByPeriodId(UUID periodId) {
        return cashFlowStatementRepository.findByFinancialPeriodId(periodId);
    }
}
