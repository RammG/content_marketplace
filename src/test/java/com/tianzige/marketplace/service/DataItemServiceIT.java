package com.tianzige.marketplace.service;

import com.tianzige.marketplace.AbstractIntegrationTest;
import com.tianzige.marketplace.model.dir.DataItem;
import com.tianzige.marketplace.repository.DataItemRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DataItemServiceIT extends AbstractIntegrationTest {

    @Autowired
    private DataItemService dataItemService;

    @Autowired
    private DataItemRepository dataItemRepository;

    @AfterEach
    void cleanup() {
        dataItemRepository.deleteAll();
    }

    @Test
    void create_persistsDataItem() {
        DataItem dataItem = buildDataItem("Bloomberg Equity Feed", "Bloomberg", DataItem.SourceType.JSON);

        DataItem created = dataItemService.create(dataItem);

        assertThat(created.getId()).isNotNull();
        assertThat(created.getCreatedAt()).isNotNull();
        assertThat(created.getUpdatedAt()).isNotNull();
    }

    @Test
    void findById_existingItem_returnsItem() {
        DataItem saved = dataItemService.create(buildDataItem("Feed", "Provider", DataItem.SourceType.JSON));

        Optional<DataItem> result = dataItemService.findById(saved.getId());

        assertThat(result).isPresent()
                .get()
                .extracting(DataItem::getName)
                .isEqualTo("Feed");
    }

    @Test
    void findById_unknownId_returnsEmpty() {
        Optional<DataItem> result = dataItemService.findById(java.util.UUID.randomUUID());
        assertThat(result).isEmpty();
    }

    @Test
    void findAll_returnsPaginatedItems() {
        dataItemService.create(buildDataItem("Feed A", "ProviderA", DataItem.SourceType.JSON));
        dataItemService.create(buildDataItem("Feed B", "ProviderB", DataItem.SourceType.XML));
        dataItemService.create(buildDataItem("Feed C", "ProviderC", DataItem.SourceType.CSV));

        Page<DataItem> page = dataItemService.findAll(PageRequest.of(0, 2));

        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getContent()).hasSize(2);
    }

    @Test
    void findByProvider_returnsProviderItems() {
        dataItemService.create(buildDataItem("Feed A", "Bloomberg", DataItem.SourceType.JSON));
        dataItemService.create(buildDataItem("Feed B", "Bloomberg", DataItem.SourceType.XML));
        dataItemService.create(buildDataItem("Feed C", "Reuters", DataItem.SourceType.CSV));

        List<DataItem> results = dataItemService.findByProvider("Bloomberg");

        assertThat(results).hasSize(2)
                .allMatch(item -> item.getProvider().equals("Bloomberg"));
    }

    @Test
    void update_existingItem_updatesFields() {
        DataItem saved = dataItemService.create(buildDataItem("Old Name", "OldProvider", DataItem.SourceType.JSON));

        DataItem updatePayload = buildDataItem("New Name", "NewProvider", DataItem.SourceType.XML);
        DataItem updated = dataItemService.update(saved.getId(), updatePayload);

        assertThat(updated.getName()).isEqualTo("New Name");
        assertThat(updated.getProvider()).isEqualTo("NewProvider");
        assertThat(updated.getSourceType()).isEqualTo(DataItem.SourceType.XML);
    }

    @Test
    void update_unknownId_throwsException() {
        DataItem updatePayload = buildDataItem("Name", "Provider", DataItem.SourceType.JSON);

        assertThatThrownBy(() -> dataItemService.update(java.util.UUID.randomUUID(), updatePayload))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("DataItem not found");
    }

    @Test
    void linkToElasticsearch_updatesElasticsearchId() {
        DataItem saved = dataItemService.create(buildDataItem("Feed", "Provider", DataItem.SourceType.JSON));

        DataItem linked = dataItemService.linkToElasticsearch(saved.getId(), "es-doc-xyz789");

        assertThat(linked.getElasticsearchId()).isEqualTo("es-doc-xyz789");
        assertThat(dataItemRepository.findByElasticsearchId("es-doc-xyz789")).isPresent();
    }

    @Test
    void delete_existingItem_removesFromRepository() {
        DataItem saved = dataItemService.create(buildDataItem("Feed", "Provider", DataItem.SourceType.JSON));

        dataItemService.delete(saved.getId());

        assertThat(dataItemRepository.findById(saved.getId())).isEmpty();
    }

    private DataItem buildDataItem(String name, String provider, DataItem.SourceType sourceType) {
        return DataItem.builder()
                .name(name)
                .provider(provider)
                .sourceType(sourceType)
                .version("1.0")
                .description(name + " - test item")
                .metadata(Map.of("region", "US", "frequency", "daily"))
                .build();
    }
}
