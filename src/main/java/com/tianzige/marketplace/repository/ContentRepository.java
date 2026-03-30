package com.tianzige.marketplace.repository;

import com.tianzige.marketplace.model.content.ContentDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContentRepository extends ElasticsearchRepository<ContentDocument, String> {

    Optional<ContentDocument> findByDataItemId(String dataItemId);

    List<ContentDocument> findByProvider(String provider);

    List<ContentDocument> findBySourceType(String sourceType);

    Page<ContentDocument> findByContentType(String contentType, Pageable pageable);

    @Query("{\"multi_match\": {\"query\": \"?0\", \"fields\": [\"rawContent\", \"normalizedContent\", \"title\", \"summary\"]}}")
    Page<ContentDocument> searchByContent(String query, Pageable pageable);

    @Query("{\"bool\": {\"must\": [{\"match\": {\"provider\": \"?0\"}}, {\"multi_match\": {\"query\": \"?1\", \"fields\": [\"rawContent\", \"title\", \"summary\"]}}]}}")
    Page<ContentDocument> searchByProviderAndContent(String provider, String query, Pageable pageable);
}
