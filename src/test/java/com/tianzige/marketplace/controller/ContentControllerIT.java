package com.tianzige.marketplace.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tianzige.marketplace.AbstractIntegrationTest;
import com.tianzige.marketplace.model.content.ContentDocument;
import com.tianzige.marketplace.repository.ContentRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
class ContentControllerIT extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ContentRepository contentRepository;

    @AfterEach
    void cleanup() {
        contentRepository.deleteAll();
    }

    @Test
    void storeContent_returnsCreated() throws Exception {
        ContentDocument document = buildDocument(UUID.randomUUID().toString(), "Bloomberg");

        mockMvc.perform(post("/api/v1/contents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(document)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.provider").value("Bloomberg"))
                .andExpect(jsonPath("$.title").value("Equity Price Feed"));
    }

    @Test
    void findById_existingDocument_returnsContent() throws Exception {
        ContentDocument saved = contentRepository.save(buildDocument(UUID.randomUUID().toString(), "Reuters"));

        mockMvc.perform(get("/api/v1/contents/{id}", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value("Reuters"));
    }

    @Test
    void findById_unknownId_returnsNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/contents/{id}", "non-existent-id"))
                .andExpect(status().isNotFound());
    }

    @Test
    void findByDataItemId_returnsLinkedDocument() throws Exception {
        String dataItemId = UUID.randomUUID().toString();
        contentRepository.save(buildDocument(dataItemId, "Bloomberg"));

        mockMvc.perform(get("/api/v1/contents/by-data-item/{dataItemId}", dataItemId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dataItemId").value(dataItemId));
    }

    @Test
    void findByProvider_returnsAllProviderDocuments() throws Exception {
        contentRepository.save(buildDocument(UUID.randomUUID().toString(), "Bloomberg"));
        contentRepository.save(buildDocument(UUID.randomUUID().toString(), "Bloomberg"));
        contentRepository.save(buildDocument(UUID.randomUUID().toString(), "Reuters"));

        mockMvc.perform(get("/api/v1/contents/by-provider/Bloomberg"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void search_fullTextSearch_returnsMatchingDocuments() throws Exception {
        ContentDocument equityDoc = buildDocument(UUID.randomUUID().toString(), "Bloomberg");
        equityDoc.setTitle("S&P 500 Equity Prices");
        equityDoc.setRawContent("Daily equity price data for S&P 500 constituents");

        ContentDocument bondDoc = buildDocument(UUID.randomUUID().toString(), "Reuters");
        bondDoc.setTitle("US Treasury Bond Yields");
        bondDoc.setRawContent("Daily yield curves for US treasury bonds");

        contentRepository.save(equityDoc);
        contentRepository.save(bondDoc);

        // Allow Elasticsearch to index
        Thread.sleep(1000);

        mockMvc.perform(get("/api/v1/contents/search?query=equity"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void update_existingDocument_returnsUpdated() throws Exception {
        ContentDocument saved = contentRepository.save(buildDocument(UUID.randomUUID().toString(), "Bloomberg"));
        saved.setTitle("Updated Title");
        saved.setSummary("Updated summary for the document");

        mockMvc.perform(put("/api/v1/contents/{id}", saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(saved)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Title"))
                .andExpect(jsonPath("$.summary").value("Updated summary for the document"));
    }

    @Test
    void delete_existingDocument_returnsNoContent() throws Exception {
        ContentDocument saved = contentRepository.save(buildDocument(UUID.randomUUID().toString(), "Bloomberg"));

        mockMvc.perform(delete("/api/v1/contents/{id}", saved.getId()))
                .andExpect(status().isNoContent());

        assertThat(contentRepository.findById(saved.getId())).isEmpty();
    }

    private ContentDocument buildDocument(String dataItemId, String provider) {
        return ContentDocument.builder()
                .dataItemId(dataItemId)
                .title("Equity Price Feed")
                .provider(provider)
                .contentType("application/json")
                .sourceType("JSON")
                .rawContent("{\"symbol\": \"AAPL\", \"price\": 182.50}")
                .summary("Daily equity price feed")
                .extractedFields(Map.of("asset_class", "equity", "region", "US"))
                .build();
    }
}
