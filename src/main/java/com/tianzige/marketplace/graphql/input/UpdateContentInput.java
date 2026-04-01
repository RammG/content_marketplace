package com.tianzige.marketplace.graphql.input;

import lombok.Data;

import java.util.Map;

@Data
public class UpdateContentInput {
    private String rawContent;
    private String normalizedContent;
    private String contentType;
    private String title;
    private String summary;
    private Map<String, Object> extractedFields;
}
