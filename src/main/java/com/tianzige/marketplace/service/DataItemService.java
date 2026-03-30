package com.tianzige.marketplace.service;

import com.tianzige.marketplace.model.dir.DataItem;
import com.tianzige.marketplace.repository.DataItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DataItemService {

    private final DataItemRepository dataItemRepository;

    @Transactional
    public DataItem create(DataItem dataItem) {
        log.debug("Creating data item: {}", dataItem.getName());
        return dataItemRepository.save(dataItem);
    }

    @Transactional(readOnly = true)
    public Optional<DataItem> findById(UUID id) {
        return dataItemRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Page<DataItem> findAll(Pageable pageable) {
        return dataItemRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public List<DataItem> findByProvider(String provider) {
        return dataItemRepository.findByProvider(provider);
    }

    @Transactional(readOnly = true)
    public List<DataItem> findBySourceType(DataItem.SourceType sourceType) {
        return dataItemRepository.findBySourceType(sourceType);
    }

    @Transactional(readOnly = true)
    public Page<DataItem> search(String query, Pageable pageable) {
        return dataItemRepository.searchByQuery(query, pageable);
    }

    @Transactional
    public DataItem update(UUID id, DataItem updatedDataItem) {
        return dataItemRepository.findById(id)
                .map(existing -> {
                    existing.setName(updatedDataItem.getName());
                    existing.setSourceType(updatedDataItem.getSourceType());
                    existing.setSourceFormat(updatedDataItem.getSourceFormat());
                    existing.setContentType(updatedDataItem.getContentType());
                    existing.setProvider(updatedDataItem.getProvider());
                    existing.setVersion(updatedDataItem.getVersion());
                    existing.setDescription(updatedDataItem.getDescription());
                    existing.setMetadata(updatedDataItem.getMetadata());
                    log.debug("Updating data item: {}", id);
                    return dataItemRepository.save(existing);
                })
                .orElseThrow(() -> new IllegalArgumentException("DataItem not found: " + id));
    }

    @Transactional
    public DataItem linkToElasticsearch(UUID id, String elasticsearchId) {
        return dataItemRepository.findById(id)
                .map(existing -> {
                    existing.setElasticsearchId(elasticsearchId);
                    log.debug("Linking data item {} to Elasticsearch document {}", id, elasticsearchId);
                    return dataItemRepository.save(existing);
                })
                .orElseThrow(() -> new IllegalArgumentException("DataItem not found: " + id));
    }

    @Transactional
    public void delete(UUID id) {
        log.debug("Deleting data item: {}", id);
        dataItemRepository.deleteById(id);
    }
}
