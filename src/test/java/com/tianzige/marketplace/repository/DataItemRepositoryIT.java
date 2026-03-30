package com.tianzige.marketplace.repository;

import com.tianzige.marketplace.AbstractIntegrationTest;
import com.tianzige.marketplace.model.dir.DataItem;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class DataItemRepositoryIT extends AbstractIntegrationTest {

    @Autowired
    private DataItemRepository dataItemRepository;

    @AfterEach
    void cleanup() {
        dataItemRepository.deleteAll();
    }

    @Test
    void save_persistsDataItemWithJsonbMetadata() {
        DataItem dataItem = DataItem.builder()
                .name("Bloomberg Equity Feed")
                .provider("Bloomberg")
                .sourceType(DataItem.SourceType.JSON)
                .version("1.0")
                .metadata(Map.of("region", "US", "asset_class", "equity", "frequency", "daily"))
                .build();

        DataItem saved = dataItemRepository.save(dataItem);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getMetadata()).containsEntry("region", "US");
        assertThat(saved.getMetadata()).containsEntry("asset_class", "equity");
    }

    @Test
    void findByProvider_returnsAllMatchingItems() {
        dataItemRepository.save(buildDataItem("Feed A", "Bloomberg", DataItem.SourceType.JSON));
        dataItemRepository.save(buildDataItem("Feed B", "Bloomberg", DataItem.SourceType.XML));
        dataItemRepository.save(buildDataItem("Feed C", "Reuters", DataItem.SourceType.CSV));

        List<DataItem> results = dataItemRepository.findByProvider("Bloomberg");

        assertThat(results).hasSize(2)
                .allMatch(item -> item.getProvider().equals("Bloomberg"));
    }

    @Test
    void findBySourceType_returnsFilteredItems() {
        dataItemRepository.save(buildDataItem("CSV Feed 1", "ProviderA", DataItem.SourceType.CSV));
        dataItemRepository.save(buildDataItem("CSV Feed 2", "ProviderB", DataItem.SourceType.CSV));
        dataItemRepository.save(buildDataItem("XML Feed", "ProviderC", DataItem.SourceType.XML));

        List<DataItem> csvItems = dataItemRepository.findBySourceType(DataItem.SourceType.CSV);

        assertThat(csvItems).hasSize(2)
                .allMatch(item -> item.getSourceType() == DataItem.SourceType.CSV);
    }

    @Test
    void findByElasticsearchId_returnsLinkedItem() {
        DataItem dataItem = dataItemRepository.save(buildDataItem("Feed", "Provider", DataItem.SourceType.JSON));
        dataItem.setElasticsearchId("es-doc-abc123");
        dataItemRepository.save(dataItem);

        Optional<DataItem> result = dataItemRepository.findByElasticsearchId("es-doc-abc123");

        assertThat(result).isPresent()
                .get()
                .extracting(DataItem::getName)
                .isEqualTo("Feed");
    }

    @Test
    void searchByQuery_matchesNameAndDescription() {
        dataItemRepository.save(buildDataItem("Equity Price Feed", "Bloomberg", DataItem.SourceType.JSON));
        dataItemRepository.save(buildDataItem("Bond Yield Data", "Reuters", DataItem.SourceType.XML));
        dataItemRepository.save(buildDataItem("FX Rate Feed", "Bloomberg", DataItem.SourceType.CSV));

        Page<DataItem> results = dataItemRepository.searchByQuery("Equity", PageRequest.of(0, 10));

        assertThat(results.getContent()).hasSize(1)
                .first()
                .extracting(DataItem::getName)
                .isEqualTo("Equity Price Feed");
    }

    @Test
    void searchByQuery_matchesProvider() {
        dataItemRepository.save(buildDataItem("Feed A", "Bloomberg", DataItem.SourceType.JSON));
        dataItemRepository.save(buildDataItem("Feed B", "Bloomberg", DataItem.SourceType.XML));
        dataItemRepository.save(buildDataItem("Feed C", "Reuters", DataItem.SourceType.CSV));

        Page<DataItem> results = dataItemRepository.searchByQuery("Bloomberg", PageRequest.of(0, 10));

        assertThat(results.getContent()).hasSize(2)
                .allMatch(item -> item.getProvider().equals("Bloomberg"));
    }

    private DataItem buildDataItem(String name, String provider, DataItem.SourceType sourceType) {
        return DataItem.builder()
                .name(name)
                .provider(provider)
                .sourceType(sourceType)
                .version("1.0")
                .description(name + " description")
                .metadata(Map.of("region", "US"))
                .build();
    }
}
