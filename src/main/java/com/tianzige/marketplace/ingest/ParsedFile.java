package com.tianzige.marketplace.ingest;

import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.Map;

@Value
@Builder
public class ParsedFile {
    String filename;
    List<FieldSchema> fields;
    List<Map<String, Object>> rows;
    long totalRows;

    @Value
    @Builder
    public static class FieldSchema {
        String name;
        String type;  // string | integer | decimal | boolean
        boolean nullable;
    }
}
