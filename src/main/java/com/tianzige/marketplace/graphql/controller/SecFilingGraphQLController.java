package com.tianzige.marketplace.graphql.controller;

import com.tianzige.marketplace.graphql.pagination.ConnectionUtils;
import com.tianzige.marketplace.model.financial.SecFiling;
import com.tianzige.marketplace.service.financial.SecFilingService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class SecFilingGraphQLController {

    private final SecFilingService secFilingService;

    private static final int DEFAULT_PAGE_SIZE = 20;

    @QueryMapping
    public SecFiling secFiling(@Argument UUID id) {
        return secFilingService.findById(id).orElse(null);
    }

    @QueryMapping
    public SecFiling secFilingByAdsh(@Argument String adsh) {
        return secFilingService.findByAdsh(adsh).orElse(null);
    }

    @QueryMapping
    public ConnectionUtils.ConnectionWithCount<SecFiling> secFilings(
            @Argument UUID companyId,
            @Argument String formType,
            @Argument Integer first,
            @Argument String after) {

        int pageSize = first != null ? first : DEFAULT_PAGE_SIZE;
        int pageNumber = ConnectionUtils.getPageNumber(after, pageSize);

        Page<SecFiling> page = secFilingService.findByFilters(
                companyId, formType, null, null,
                PageRequest.of(pageNumber, pageSize)
        );

        return ConnectionUtils.toConnectionWithCount(page);
    }
}
