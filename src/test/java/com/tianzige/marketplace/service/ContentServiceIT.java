package com.tianzige.marketplace.service;

import com.tianzige.marketplace.AbstractIntegrationTest;
import com.tianzige.marketplace.model.content.ContentDocument;
import com.tianzige.marketplace.repository.ContentRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ContentServiceIT extends AbstractIntegrationTest {

    @Autowired
    private ContentService contentService;

    @Autowired
    private ContentRepository contentRepository;

    @AfterEach
    void cleanup() {
        contentRepository.deleteAll();
    }

    @Test
    void store_persistsDocumentWithTimestamps() {
        ContentDocument document = buildDocument(UUID.randomUUID().toString(), "Bloomberg");

        ContentDocument stored = contentService.store(document);

        assertThat(stored.getId()).isNotNull();
        assertThat(stored.getIndexedAt()).isNotNull();
        assertThat(stored.getUpdatedAt()).isNotNull();
    }

    @Test
    void store_generatesIdIfAbsent() {
        ContentDocument document = buildDocument(UUID.randomUUID().toString(), "Bloomberg");
        document.setId(null);

        ContentDocument stored = contentService.store(document);

        assertThat(stored.getId()).isNotNull();
    }

    @Test
    void findById_existingDocument_returnsDocument() {
        ContentDocument saved = contentService.store(buildDocument(UUID.randomUUID().toString(), "Reuters"));

        Optional<ContentDocument> result = contentService.findById(saved.getId());

        assertThat(result).isPresent()
                .get()
                .extracting(ContentDocument::getProvider)
                .isEqualTo("Reuters");
    }

    @Test
    void findById_unknownId_returnsEmpty() {
        Optional<ContentDocument> result = contentService.findById("non-existent-id");
        assertThat(result).isEmpty();
    }

    @Test
    void findByDataItemId_returnsLinkedDocument() {
        String dataItemId = UUID.randomUUID().toString();
        contentService.store(buildDocument(dataItemId, "Bloomberg"));

        Optional<ContentDocument> result = contentService.findByDataItemId(dataItemId);

        assertThat(result).isPresent()
                .get()
                .extracting(ContentDocument::getDataItemId)
                .isEqualTo(dataItemId);
    }

    @Test
    void findByProvider_returnsAllProviderDocuments() {
        contentService.store(buildDocument(UUID.randomUUID().toString(), "Bloomberg"));
        contentService.store(buildDocument(UUID.randomUUID().toString(), "Bloomberg"));
        contentService.store(buildDocument(UUID.randomUUID().toString(), "Reuters"));

        List<ContentDocument> results = contentService.findByProvider("Bloomberg");

        assertThat(results).hasSize(2)
                .allMatch(doc -> doc.getProvider().equals("Bloomberg"));
    }

    @Test
    void update_existingDocument_updatesFields() {
        ContentDocument saved = contentService.store(buildDocument(UUID.randomUUID().toString(), "Bloomberg"));

        saved.setTitle("Updated Title");
        saved.setSummary("Updated summary");
        ContentDocument updated = contentService.update(saved.getId(), saved);

        assertThat(updated.getTitle()).isEqualTo("Updated Title");
        assertThat(updated.getSummary()).isEqualTo("Updated summary");
        assertThat(updated.getUpdatedAt()).isAfterOrEqualTo(saved.getIndexedAt());
    }

    @Test
    void update_unknownId_throwsException() {
        ContentDocument document = buildDocument(UUID.randomUUID().toString(), "Bloomberg");

        assertThatThrownBy(() -> contentService.update("non-existent-id", document))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ContentDocument not found");
    }

    @Test
    void delete_existingDocument_removesFromRepository() {
        ContentDocument saved = contentService.store(buildDocument(UUID.randomUUID().toString(), "Bloomberg"));

        contentService.delete(saved.getId());

        assertThat(contentRepository.findById(saved.getId())).isEmpty();
    }

    @Test
    void search_fullTextSearch_returnsResults() throws InterruptedException {
        ContentDocument equityDoc = buildDocument(UUID.randomUUID().toString(), "Bloomberg");
        equityDoc.setTitle("Nasdaq Equity Prices");
        equityDoc.setRawContent("Daily equity price data for Nasdaq 100 constituents");

        ContentDocument bondDoc = buildDocument(UUID.randomUUID().toString(), "Reuters");
        bondDoc.setTitle("US Treasury Bonds");
        bondDoc.setRawContent("Daily yield curve for US treasury bonds");

        contentService.store(equityDoc);
        contentService.store(bondDoc);

        // Allow Elasticsearch to index
        Thread.sleep(1000);

        Page<ContentDocument> results = contentService.search("equity", PageRequest.of(0, 10));

        assertThat(results.getContent()).isNotEmpty();
    }

    private ContentDocument buildDocument(String dataItemId, String provider) {
        return ContentDocument.builder()
                .id(UUID.randomUUID().toString())
                .dataItemId(dataItemId)
                .title("Equity Price Feed")
                .provider(provider)
                .contentType("application/json")
                .sourceType("JSON")
                .rawContent("{\"symbol\": \"AAPL\", \"price\": 182.50}")
                .summary("Daily equity price data")
                .extractedFields(Map.of("asset_class", "equity", "region", "US"))
                .build();
    }
}
