package com.tianzige.marketplace.graphql.input;

import lombok.Data;

import java.util.Map;

@Data
public class StoreContentInput {
    private String dataItemId;
    private String rawContent;
    private String normalizedContent;
    private String contentType;
    private String sourceType;
    private String title;
    private String summary;
    private String provider;
    private Map<String, Object> extractedFields;
}
