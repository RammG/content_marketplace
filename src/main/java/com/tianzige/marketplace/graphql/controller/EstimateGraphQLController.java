package com.tianzige.marketplace.graphql.controller;

import com.tianzige.marketplace.graphql.pagination.ConnectionUtils;
import com.tianzige.marketplace.model.financial.BrokerEstimate;
import com.tianzige.marketplace.model.financial.ConsensusEstimate;
import com.tianzige.marketplace.model.financial.EstimateMetric;
import com.tianzige.marketplace.service.financial.EstimateService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class EstimateGraphQLController {

    private final EstimateService estimateService;

    private static final int DEFAULT_PAGE_SIZE = 20;

    @QueryMapping
    public ConnectionUtils.ConnectionWithCount<BrokerEstimate> brokerEstimates(
            @Argument UUID companyId,
            @Argument EstimateMetric metric,
            @Argument Integer fiscalYear,
            @Argument Integer first,
            @Argument String after) {

        int pageSize = first != null ? first : DEFAULT_PAGE_SIZE;
        int pageNumber = ConnectionUtils.getPageNumber(after, pageSize);

        Page<BrokerEstimate> page = estimateService.findBrokerEstimatesByCompanyId(
                companyId, metric, fiscalYear, null,
                PageRequest.of(pageNumber, pageSize)
        );

        return ConnectionUtils.toConnectionWithCount(page);
    }

    @QueryMapping
    public ConnectionUtils.ConnectionWithCount<ConsensusEstimate> consensusEstimates(
            @Argument UUID companyId,
            @Argument EstimateMetric metric,
            @Argument Integer fiscalYear,
            @Argument Integer first,
            @Argument String after) {

        int pageSize = first != null ? first : DEFAULT_PAGE_SIZE;
        int pageNumber = ConnectionUtils.getPageNumber(after, pageSize);

        Page<ConsensusEstimate> page = estimateService.findConsensusEstimatesByCompanyId(
                companyId, metric, fiscalYear, null,
                PageRequest.of(pageNumber, pageSize)
        );

        return ConnectionUtils.toConnectionWithCount(page);
    }
}
