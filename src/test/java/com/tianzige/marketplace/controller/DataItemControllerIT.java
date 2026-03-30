package com.tianzige.marketplace.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tianzige.marketplace.AbstractIntegrationTest;
import com.tianzige.marketplace.model.dir.DataItem;
import com.tianzige.marketplace.repository.DataItemRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
class DataItemControllerIT extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DataItemRepository dataItemRepository;

    @AfterEach
    void cleanup() {
        dataItemRepository.deleteAll();
    }

    @Test
    void createDataItem_returnsCreated() throws Exception {
        DataItem dataItem = buildDataItem("Bloomberg Price Feed", "Bloomberg", DataItem.SourceType.JSON);

        mockMvc.perform(post("/api/v1/data-items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dataItem)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.name").value("Bloomberg Price Feed"))
                .andExpect(jsonPath("$.provider").value("Bloomberg"))
                .andExpect(jsonPath("$.sourceType").value("JSON"));
    }

    @Test
    void findById_existingId_returnsDataItem() throws Exception {
        DataItem saved = dataItemRepository.save(buildDataItem("Reuters XML Feed", "Reuters", DataItem.SourceType.XML));

        mockMvc.perform(get("/api/v1/data-items/{id}", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Reuters XML Feed"))
                .andExpect(jsonPath("$.provider").value("Reuters"));
    }

    @Test
    void findById_unknownId_returnsNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/data-items/{id}", "00000000-0000-0000-0000-000000000000"))
                .andExpect(status().isNotFound());
    }

    @Test
    void findAll_returnsPaginatedResults() throws Exception {
        dataItemRepository.save(buildDataItem("Feed A", "ProviderA", DataItem.SourceType.CSV));
        dataItemRepository.save(buildDataItem("Feed B", "ProviderB", DataItem.SourceType.JSON));

        mockMvc.perform(get("/api/v1/data-items?page=0&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void findByProvider_returnsMatchingItems() throws Exception {
        dataItemRepository.save(buildDataItem("Feed A", "Bloomberg", DataItem.SourceType.JSON));
        dataItemRepository.save(buildDataItem("Feed B", "Bloomberg", DataItem.SourceType.XML));
        dataItemRepository.save(buildDataItem("Feed C", "Reuters", DataItem.SourceType.CSV));

        mockMvc.perform(get("/api/v1/data-items/by-provider/Bloomberg"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void findBySourceType_returnsMatchingItems() throws Exception {
        dataItemRepository.save(buildDataItem("XML Feed 1", "Bloomberg", DataItem.SourceType.XML));
        dataItemRepository.save(buildDataItem("XML Feed 2", "Reuters", DataItem.SourceType.XML));
        dataItemRepository.save(buildDataItem("JSON Feed", "Reuters", DataItem.SourceType.JSON));

        mockMvc.perform(get("/api/v1/data-items/by-source-type/XML"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void search_matchesNameAndProvider() throws Exception {
        dataItemRepository.save(buildDataItem("Equity Price Feed", "Bloomberg", DataItem.SourceType.JSON));
        dataItemRepository.save(buildDataItem("Bond Yield Data", "Reuters", DataItem.SourceType.XML));

        mockMvc.perform(get("/api/v1/data-items/search?query=Equity"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Equity Price Feed"));
    }

    @Test
    void update_existingItem_returnsUpdated() throws Exception {
        DataItem saved = dataItemRepository.save(buildDataItem("Old Name", "OldProvider", DataItem.SourceType.JSON));
        saved.setName("New Name");
        saved.setProvider("NewProvider");

        mockMvc.perform(put("/api/v1/data-items/{id}", saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(saved)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New Name"))
                .andExpect(jsonPath("$.provider").value("NewProvider"));
    }

    @Test
    void linkToElasticsearch_updatesElasticsearchId() throws Exception {
        DataItem saved = dataItemRepository.save(buildDataItem("Feed", "Provider", DataItem.SourceType.JSON));

        mockMvc.perform(patch("/api/v1/data-items/{id}/link-elasticsearch", saved.getId())
                        .param("elasticsearchId", "es-doc-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.elasticsearchId").value("es-doc-123"));

        assertThat(dataItemRepository.findById(saved.getId()))
                .isPresent()
                .get()
                .extracting(DataItem::getElasticsearchId)
                .isEqualTo("es-doc-123");
    }

    @Test
    void delete_existingItem_returnsNoContent() throws Exception {
        DataItem saved = dataItemRepository.save(buildDataItem("Feed", "Provider", DataItem.SourceType.JSON));

        mockMvc.perform(delete("/api/v1/data-items/{id}", saved.getId()))
                .andExpect(status().isNoContent());

        assertThat(dataItemRepository.findById(saved.getId())).isEmpty();
    }

    private DataItem buildDataItem(String name, String provider, DataItem.SourceType sourceType) {
        return DataItem.builder()
                .name(name)
                .provider(provider)
                .sourceType(sourceType)
                .version("1.0")
                .description("Test data item")
                .metadata(Map.of("region", "US", "frequency", "daily"))
                .build();
    }
}
