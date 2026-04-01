package com.tianzige.marketplace.graphql.controller;

import com.tianzige.marketplace.graphql.pagination.ConnectionUtils;
import com.tianzige.marketplace.model.financial.*;
import com.tianzige.marketplace.service.financial.FinancialStatementService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class FinancialPeriodGraphQLController {

    private final FinancialStatementService financialStatementService;

    private static final int DEFAULT_PAGE_SIZE = 20;

    @QueryMapping
    public FinancialPeriod financialPeriod(@Argument UUID id) {
        return financialStatementService.findPeriodById(id).orElse(null);
    }

    @QueryMapping
    public ConnectionUtils.ConnectionWithCount<FinancialPeriod> financialPeriods(
            @Argument UUID companyId,
            @Argument PeriodType periodType,
            @Argument Integer fiscalYear,
            @Argument Integer first,
            @Argument String after) {

        int pageSize = first != null ? first : DEFAULT_PAGE_SIZE;
        int pageNumber = ConnectionUtils.getPageNumber(after, pageSize);

        Page<FinancialPeriod> page = financialStatementService.findPeriodsByCompanyId(
                companyId, periodType, fiscalYear,
                PageRequest.of(pageNumber, pageSize)
        );

        return ConnectionUtils.toConnectionWithCount(page);
    }

    // Nested resolver: FinancialPeriod.balanceSheets
    @SchemaMapping(typeName = "FinancialPeriod", field = "balanceSheets")
    public List<BalanceSheet> balanceSheets(FinancialPeriod period) {
        return financialStatementService.findBalanceSheetsByPeriodId(period.getId());
    }

    // Nested resolver: FinancialPeriod.incomeStatements
    @SchemaMapping(typeName = "FinancialPeriod", field = "incomeStatements")
    public List<IncomeStatement> incomeStatements(FinancialPeriod period) {
        return financialStatementService.findIncomeStatementsByPeriodId(period.getId());
    }

    // Nested resolver: FinancialPeriod.cashFlowStatements
    @SchemaMapping(typeName = "FinancialPeriod", field = "cashFlowStatements")
    public List<CashFlowStatement> cashFlowStatements(FinancialPeriod period) {
        return financialStatementService.findCashFlowStatementsByPeriodId(period.getId());
    }
}
