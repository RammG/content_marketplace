package com.tianzige.marketplace.graphql.input;

import com.tianzige.marketplace.model.dir.DataItem;
import lombok.Data;

import java.util.Map;

@Data
public class UpdateDataItemInput {
    private String name;
    private DataItem.SourceType sourceType;
    private String sourceFormat;
    private String contentType;
    private String provider;
    private String version;
    private String description;
    private Map<String, Object> metadata;
}
