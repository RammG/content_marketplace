package com.tianzige.marketplace.repository;

import com.tianzige.marketplace.AbstractIntegrationTest;
import com.tianzige.marketplace.model.content.ContentDocument;
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

class ContentRepositoryIT extends AbstractIntegrationTest {

    @Autowired
    private ContentRepository contentRepository;

    @AfterEach
    void cleanup() {
        contentRepository.deleteAll();
    }

    @Test
    void save_persistsDocumentToElasticsearch() {
        ContentDocument document = buildDocument(UUID.randomUUID().toString(), "Bloomberg");

        ContentDocument saved = contentRepository.save(document);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getProvider()).isEqualTo("Bloomberg");
        assertThat(saved.getExtractedFields()).containsEntry("asset_class", "equity");
    }

    @Test
    void findByDataItemId_returnsLinkedDocument() {
        String dataItemId = UUID.randomUUID().toString();
        contentRepository.save(buildDocument(dataItemId, "Bloomberg"));

        Optional<ContentDocument> result = contentRepository.findByDataItemId(dataItemId);

        assertThat(result).isPresent()
                .get()
                .extracting(ContentDocument::getDataItemId)
                .isEqualTo(dataItemId);
    }

    @Test
    void findByDataItemId_unknownId_returnsEmpty() {
        Optional<ContentDocument> result = contentRepository.findByDataItemId("non-existent-id");
        assertThat(result).isEmpty();
    }

    @Test
    void findByProvider_returnsAllProviderDocuments() {
        contentRepository.save(buildDocument(UUID.randomUUID().toString(), "Bloomberg"));
        contentRepository.save(buildDocument(UUID.randomUUID().toString(), "Bloomberg"));
        contentRepository.save(buildDocument(UUID.randomUUID().toString(), "Reuters"));

        List<ContentDocument> results = contentRepository.findByProvider("Bloomberg");

        assertThat(results).hasSize(2)
                .allMatch(doc -> doc.getProvider().equals("Bloomberg"));
    }

    @Test
    void findBySourceType_returnsFilteredDocuments() {
        ContentDocument xmlDoc = buildDocument(UUID.randomUUID().toString(), "Reuters");
        xmlDoc.setSourceType("XML");
        ContentDocument jsonDoc = buildDocument(UUID.randomUUID().toString(), "Bloomberg");
        jsonDoc.setSourceType("JSON");

        contentRepository.save(xmlDoc);
        contentRepository.save(jsonDoc);

        List<ContentDocument> xmlDocs = contentRepository.findBySourceType("XML");

        assertThat(xmlDocs).hasSize(1)
                .first()
                .extracting(ContentDocument::getSourceType)
                .isEqualTo("XML");
    }

    @Test
    void findByContentType_returnsPaginatedDocuments() {
        contentRepository.save(buildDocument(UUID.randomUUID().toString(), "Bloomberg"));
        contentRepository.save(buildDocument(UUID.randomUUID().toString(), "Reuters"));

        Page<ContentDocument> page = contentRepository.findByContentType("application/json", PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(2);
    }

    @Test
    void searchByContent_matchesRawContentAndTitle() throws InterruptedException {
        ContentDocument equityDoc = buildDocument(UUID.randomUUID().toString(), "Bloomberg");
        equityDoc.setTitle("S&P 500 Daily Prices");
        equityDoc.setRawContent("Daily equity prices for S&P 500 index constituents");

        ContentDocument bondDoc = buildDocument(UUID.randomUUID().toString(), "Reuters");
        bondDoc.setTitle("US Treasury Yields");
        bondDoc.setRawContent("Daily yield curve data for US treasury bonds");

        contentRepository.save(equityDoc);
        contentRepository.save(bondDoc);

        // Allow Elasticsearch to index documents
        Thread.sleep(1000);

        Page<ContentDocument> results = contentRepository.searchByContent("equity", PageRequest.of(0, 10));

        assertThat(results.getContent()).isNotEmpty()
                .allMatch(doc -> doc.getTitle().contains("S&P 500") || doc.getRawContent().contains("equity"));
    }

    private ContentDocument buildDocument(String dataItemId, String provider) {
        return ContentDocument.builder()
                .id(UUID.randomUUID().toString())
                .dataItemId(dataItemId)
                .title("Equity Price Feed")
                .provider(provider)
                .contentType("application/json")
                .sourceType("JSON")
                .rawContent("{\"symbol\": \"AAPL\", \"price\": 182.50, \"date\": \"2026-03-29\"}")
                .summary("Daily equity price data")
                .extractedFields(Map.of("asset_class", "equity", "region", "US"))
                .build();
    }
}
