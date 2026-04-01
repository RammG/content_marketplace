package com.tianzige.marketplace.graphql.controller;

import com.tianzige.marketplace.graphql.input.CreateDataItemInput;
import com.tianzige.marketplace.graphql.input.UpdateDataItemInput;
import com.tianzige.marketplace.graphql.pagination.ConnectionUtils;
import com.tianzige.marketplace.model.content.ContentDocument;
import com.tianzige.marketplace.model.dir.DataItem;
import com.tianzige.marketplace.service.ContentService;
import com.tianzige.marketplace.service.DataItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class DataItemGraphQLController {

    private final DataItemService dataItemService;
    private final ContentService contentService;

    private static final int DEFAULT_PAGE_SIZE = 20;

    @QueryMapping
    public DataItem dataItem(@Argument UUID id) {
        return dataItemService.findById(id).orElse(null);
    }

    @QueryMapping
    public ConnectionUtils.ConnectionWithCount<DataItem> dataItems(
            @Argument String provider,
            @Argument DataItem.SourceType sourceType,
            @Argument String query,
            @Argument Integer first,
            @Argument String after) {

        int pageSize = first != null ? first : DEFAULT_PAGE_SIZE;
        int pageNumber = ConnectionUtils.getPageNumber(after, pageSize);
        PageRequest pageRequest = PageRequest.of(pageNumber, pageSize);

        Page<DataItem> page;
        if (query != null && !query.isEmpty()) {
            page = dataItemService.search(query, pageRequest);
        } else if (provider != null) {
            List<DataItem> providerItems = dataItemService.findByProvider(provider);
            page = new org.springframework.data.domain.PageImpl<>(providerItems, pageRequest, providerItems.size());
        } else if (sourceType != null) {
            List<DataItem> sourceTypeItems = dataItemService.findBySourceType(sourceType);
            page = new org.springframework.data.domain.PageImpl<>(sourceTypeItems, pageRequest, sourceTypeItems.size());
        } else {
            page = dataItemService.findAll(pageRequest);
        }

        return ConnectionUtils.toConnectionWithCount(page);
    }

    // Nested resolver: DataItem.contentDocuments
    @SchemaMapping(typeName = "DataItem", field = "contentDocuments")
    public List<ContentDocument> contentDocuments(DataItem dataItem) {
        return contentService.findByDataItemId(dataItem.getId().toString())
                .map(List::of)
                .orElse(List.of());
    }

    @MutationMapping
    public DataItem createDataItem(@Argument CreateDataItemInput input) {
        DataItem dataItem = new DataItem();
        dataItem.setName(input.getName());
        dataItem.setSourceType(input.getSourceType());
        dataItem.setSourceFormat(input.getSourceFormat());
        dataItem.setContentType(input.getContentType());
        dataItem.setProvider(input.getProvider());
        dataItem.setVersion(input.getVersion());
        dataItem.setDescription(input.getDescription());
        dataItem.setMetadata(input.getMetadata());
        return dataItemService.create(dataItem);
    }

    @MutationMapping
    public DataItem updateDataItem(@Argument UUID id, @Argument UpdateDataItemInput input) {
        DataItem existing = dataItemService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("DataItem not found: " + id));

        if (input.getName() != null) existing.setName(input.getName());
        if (input.getSourceType() != null) existing.setSourceType(input.getSourceType());
        if (input.getSourceFormat() != null) existing.setSourceFormat(input.getSourceFormat());
        if (input.getContentType() != null) existing.setContentType(input.getContentType());
        if (input.getProvider() != null) existing.setProvider(input.getProvider());
        if (input.getVersion() != null) existing.setVersion(input.getVersion());
        if (input.getDescription() != null) existing.setDescription(input.getDescription());
        if (input.getMetadata() != null) existing.setMetadata(input.getMetadata());

        return dataItemService.update(id, existing);
    }

    @MutationMapping
    public boolean deleteDataItem(@Argument UUID id) {
        dataItemService.delete(id);
        return true;
    }
}
