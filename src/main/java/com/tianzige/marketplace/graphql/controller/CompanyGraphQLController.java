package com.tianzige.marketplace.graphql.controller;

import com.tianzige.marketplace.graphql.input.CompanyFilter;
import com.tianzige.marketplace.graphql.input.CreateCompanyInput;
import com.tianzige.marketplace.graphql.input.UpdateCompanyInput;
import com.tianzige.marketplace.graphql.pagination.ConnectionUtils;
import com.tianzige.marketplace.model.financial.*;
import com.tianzige.marketplace.service.financial.CompanyService;
import com.tianzige.marketplace.service.financial.EstimateService;
import com.tianzige.marketplace.service.financial.FinancialStatementService;
import com.tianzige.marketplace.service.financial.SecFilingService;
import graphql.relay.Connection;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class CompanyGraphQLController {

    private final CompanyService companyService;
    private final FinancialStatementService financialStatementService;
    private final SecFilingService secFilingService;
    private final EstimateService estimateService;

    private static final int DEFAULT_PAGE_SIZE = 20;

    @QueryMapping
    public Company company(@Argument UUID id) {
        return companyService.findById(id).orElse(null);
    }

    @QueryMapping
    public Company companyByTicker(@Argument String ticker) {
        return companyService.findByTicker(ticker).orElse(null);
    }

    @QueryMapping
    public Company companyByCik(@Argument String cik) {
        return companyService.findByCik(cik).orElse(null);
    }

    @QueryMapping
    public ConnectionUtils.ConnectionWithCount<Company> companies(
            @Argument Integer first,
            @Argument String after,
            @Argument CompanyFilter filter) {

        int pageSize = first != null ? first : DEFAULT_PAGE_SIZE;
        int pageNumber = ConnectionUtils.getPageNumber(after, pageSize);

        Page<Company> page;
        if (filter != null) {
            page = companyService.findByFilters(
                    filter.getTicker(),
                    filter.getCik(),
                    filter.getName(),
                    filter.getExchange(),
                    filter.getSector(),
                    filter.getIndustry(),
                    PageRequest.of(pageNumber, pageSize)
            );
        } else {
            page = companyService.findAll(PageRequest.of(pageNumber, pageSize));
        }

        return ConnectionUtils.toConnectionWithCount(page);
    }

    // Nested resolver: Company.financialPeriods
    @SchemaMapping(typeName = "Company", field = "financialPeriods")
    public ConnectionUtils.ConnectionWithCount<FinancialPeriod> financialPeriods(
            Company company,
            @Argument PeriodType periodType,
            @Argument Integer fiscalYear,
            @Argument Integer first,
            @Argument String after) {

        int pageSize = first != null ? first : DEFAULT_PAGE_SIZE;
        int pageNumber = ConnectionUtils.getPageNumber(after, pageSize);

        Page<FinancialPeriod> page = financialStatementService.findPeriodsByCompanyId(
                company.getId(), periodType, fiscalYear,
                PageRequest.of(pageNumber, pageSize)
        );

        return ConnectionUtils.toConnectionWithCount(page);
    }

    // Nested resolver: Company.secFilings
    @SchemaMapping(typeName = "Company", field = "secFilings")
    public ConnectionUtils.ConnectionWithCount<SecFiling> secFilings(
            Company company,
            @Argument String formType,
            @Argument Integer first,
            @Argument String after) {

        int pageSize = first != null ? first : DEFAULT_PAGE_SIZE;
        int pageNumber = ConnectionUtils.getPageNumber(after, pageSize);

        Page<SecFiling> page = secFilingService.findByFilters(
                company.getId(), formType, null, null,
                PageRequest.of(pageNumber, pageSize)
        );

        return ConnectionUtils.toConnectionWithCount(page);
    }

    // Nested resolver: Company.brokerEstimates
    @SchemaMapping(typeName = "Company", field = "brokerEstimates")
    public ConnectionUtils.ConnectionWithCount<BrokerEstimate> brokerEstimates(
            Company company,
            @Argument EstimateMetric metric,
            @Argument Integer first,
            @Argument String after) {

        int pageSize = first != null ? first : DEFAULT_PAGE_SIZE;
        int pageNumber = ConnectionUtils.getPageNumber(after, pageSize);

        Page<BrokerEstimate> page = estimateService.findBrokerEstimatesByCompanyId(
                company.getId(), metric, null, null,
                PageRequest.of(pageNumber, pageSize)
        );

        return ConnectionUtils.toConnectionWithCount(page);
    }

    // Nested resolver: Company.consensusEstimates
    @SchemaMapping(typeName = "Company", field = "consensusEstimates")
    public ConnectionUtils.ConnectionWithCount<ConsensusEstimate> consensusEstimates(
            Company company,
            @Argument EstimateMetric metric,
            @Argument Integer first,
            @Argument String after) {

        int pageSize = first != null ? first : DEFAULT_PAGE_SIZE;
        int pageNumber = ConnectionUtils.getPageNumber(after, pageSize);

        Page<ConsensusEstimate> page = estimateService.findConsensusEstimatesByCompanyId(
                company.getId(), metric, null, null,
                PageRequest.of(pageNumber, pageSize)
        );

        return ConnectionUtils.toConnectionWithCount(page);
    }

    // Nested resolver: Company.latestAnnualPeriod
    @SchemaMapping(typeName = "Company", field = "latestAnnualPeriod")
    public FinancialPeriod latestAnnualPeriod(Company company) {
        return financialStatementService.findLatestPeriod(company.getId(), PeriodType.ANNUAL)
                .orElse(null);
    }

    // Nested resolver: Company.latestQuarterlyPeriod
    @SchemaMapping(typeName = "Company", field = "latestQuarterlyPeriod")
    public FinancialPeriod latestQuarterlyPeriod(Company company) {
        return financialStatementService.findLatestPeriod(company.getId(), PeriodType.QUARTERLY)
                .orElse(null);
    }

    @MutationMapping
    public Company createCompany(@Argument CreateCompanyInput input) {
        Company company = new Company();
        company.setTicker(input.getTicker());
        company.setCik(input.getCik());
        company.setName(input.getName());
        company.setExchange(input.getExchange());
        company.setSector(input.getSector());
        company.setIndustry(input.getIndustry());
        company.setSic(input.getSic());
        company.setCountryOfIncorporation(input.getCountryOfIncorporation());
        company.setMetadata(input.getMetadata());
        return companyService.create(company);
    }

    @MutationMapping
    public Company updateCompany(@Argument UUID id, @Argument UpdateCompanyInput input) {
        return companyService.update(
                id,
                input.getTicker(),
                input.getCik(),
                input.getName(),
                input.getExchange(),
                input.getSector(),
                input.getIndustry(),
                input.getSic(),
                input.getCountryOfIncorporation(),
                input.getMetadata()
        );
    }

    @MutationMapping
    public boolean deleteCompany(@Argument UUID id) {
        companyService.delete(id);
        return true;
    }
}
