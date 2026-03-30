package com.tianzige.marketplace.model.content;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

import java.time.LocalDateTime;
import java.util.Map;

@Document(indexName = "content_documents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContentDocument {

    @Id
    private String id;

    @Field(type = FieldType.Keyword)
    private String dataItemId;

    @Field(type = FieldType.Text)
    private String rawContent;

    @Field(type = FieldType.Text)
    private String normalizedContent;

    @Field(type = FieldType.Keyword)
    private String contentType;

    @Field(type = FieldType.Keyword)
    private String sourceType;

    @Field(type = FieldType.Text)
    private String title;

    @Field(type = FieldType.Text)
    private String summary;

    @Field(type = FieldType.Keyword)
    private String provider;

    @Field(type = FieldType.Object)
    private Map<String, Object> extractedFields;

    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second)
    private LocalDateTime indexedAt;

    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second)
    private LocalDateTime updatedAt;
}
