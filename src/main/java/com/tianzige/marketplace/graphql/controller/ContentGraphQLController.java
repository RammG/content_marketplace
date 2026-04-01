package com.tianzige.marketplace.graphql.controller;

import com.tianzige.marketplace.graphql.input.StoreContentInput;
import com.tianzige.marketplace.graphql.input.UpdateContentInput;
import com.tianzige.marketplace.graphql.pagination.ConnectionUtils;
import com.tianzige.marketplace.model.content.ContentDocument;
import com.tianzige.marketplace.model.dir.DataItem;
import com.tianzige.marketplace.service.ContentService;
import com.tianzige.marketplace.service.DataItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class ContentGraphQLController {

    private final ContentService contentService;
    private final DataItemService dataItemService;

    private static final int DEFAULT_PAGE_SIZE = 20;

    @QueryMapping
    public ContentDocument contentDocument(@Argument String id) {
        return contentService.findById(id).orElse(null);
    }

    @QueryMapping
    public ConnectionUtils.ConnectionWithCount<ContentDocument> contentDocuments(
            @Argument String provider,
            @Argument String contentType,
            @Argument String query,
            @Argument Integer first,
            @Argument String after) {

        int pageSize = first != null ? first : DEFAULT_PAGE_SIZE;
        int pageNumber = ConnectionUtils.getPageNumber(after, pageSize);
        PageRequest pageRequest = PageRequest.of(pageNumber, pageSize);

        Page<ContentDocument> page;
        if (query != null && !query.isEmpty()) {
            if (provider != null) {
                page = contentService.searchByProvider(provider, query, pageRequest);
            } else {
                page = contentService.search(query, pageRequest);
            }
        } else if (provider != null) {
            var list = contentService.findByProvider(provider);
            page = new PageImpl<>(list, pageRequest, list.size());
        } else {
            page = contentService.search("*", pageRequest);
        }

        return ConnectionUtils.toConnectionWithCount(page);
    }

    // Nested resolver: ContentDocument.dataItem
    @SchemaMapping(typeName = "ContentDocument", field = "dataItem")
    public DataItem dataItem(ContentDocument content) {
        if (content.getDataItemId() == null) {
            return null;
        }
        try {
            UUID dataItemId = UUID.fromString(content.getDataItemId());
            return dataItemService.findById(dataItemId).orElse(null);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @MutationMapping
    public ContentDocument storeContent(@Argument StoreContentInput input) {
        ContentDocument content = new ContentDocument();
        content.setDataItemId(input.getDataItemId());
        content.setRawContent(input.getRawContent());
        content.setNormalizedContent(input.getNormalizedContent());
        content.setContentType(input.getContentType());
        content.setSourceType(input.getSourceType());
        content.setTitle(input.getTitle());
        content.setSummary(input.getSummary());
        content.setProvider(input.getProvider());
        content.setExtractedFields(input.getExtractedFields());
        return contentService.store(content);
    }

    @MutationMapping
    public ContentDocument updateContent(@Argument String id, @Argument UpdateContentInput input) {
        ContentDocument existing = contentService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("ContentDocument not found: " + id));

        if (input.getRawContent() != null) existing.setRawContent(input.getRawContent());
        if (input.getNormalizedContent() != null) existing.setNormalizedContent(input.getNormalizedContent());
        if (input.getContentType() != null) existing.setContentType(input.getContentType());
        if (input.getTitle() != null) existing.setTitle(input.getTitle());
        if (input.getSummary() != null) existing.setSummary(input.getSummary());
        if (input.getExtractedFields() != null) existing.setExtractedFields(input.getExtractedFields());

        return contentService.update(id, existing);
    }

    @MutationMapping
    public boolean deleteContent(@Argument String id) {
        contentService.delete(id);
        return true;
    }
}
