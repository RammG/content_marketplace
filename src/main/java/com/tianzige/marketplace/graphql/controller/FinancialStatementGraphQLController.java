package com.tianzige.marketplace.graphql.controller;

import com.tianzige.marketplace.model.financial.BalanceSheet;
import com.tianzige.marketplace.model.financial.CashFlowStatement;
import com.tianzige.marketplace.model.financial.IncomeStatement;
import com.tianzige.marketplace.service.financial.FinancialStatementService;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class FinancialStatementGraphQLController {

    private final FinancialStatementService financialStatementService;

    @QueryMapping
    public BalanceSheet balanceSheet(@Argument UUID id) {
        return financialStatementService.findBalanceSheetById(id).orElse(null);
    }

    @QueryMapping
    public IncomeStatement incomeStatement(@Argument UUID id) {
        return financialStatementService.findIncomeStatementById(id).orElse(null);
    }

    @QueryMapping
    public CashFlowStatement cashFlowStatement(@Argument UUID id) {
        return financialStatementService.findCashFlowStatementById(id).orElse(null);
    }
}
