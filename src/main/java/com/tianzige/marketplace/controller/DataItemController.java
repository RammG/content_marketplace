package com.tianzige.marketplace.controller;

import com.tianzige.marketplace.model.dir.DataItem;
import com.tianzige.marketplace.service.DataItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/data-items")
@RequiredArgsConstructor
@Tag(name = "Data Item Registry", description = "APIs for managing data item metadata in the DIR")
public class DataItemController {

    private final DataItemService dataItemService;

    @PostMapping
    @Operation(summary = "Register a new data item", description = "Creates a new data item in the registry")
    public ResponseEntity<DataItem> create(@RequestBody DataItem dataItem) {
        DataItem created = dataItemService.create(dataItem);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    @Operation(summary = "List all data items", description = "Returns a paginated list of all data items")
    public ResponseEntity<Page<DataItem>> findAll(Pageable pageable) {
        return ResponseEntity.ok(dataItemService.findAll(pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get data item by ID", description = "Returns a single data item by its UUID")
    public ResponseEntity<DataItem> findById(@PathVariable UUID id) {
        return dataItemService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/by-provider/{provider}")
    @Operation(summary = "Get data items by provider", description = "Returns all data items from a specific provider")
    public ResponseEntity<List<DataItem>> findByProvider(@PathVariable String provider) {
        return ResponseEntity.ok(dataItemService.findByProvider(provider));
    }

    @GetMapping("/by-source-type/{sourceType}")
    @Operation(summary = "Get data items by source type", description = "Returns all data items of a specific source type (XML, JSON, CSV, etc.)")
    public ResponseEntity<List<DataItem>> findBySourceType(@PathVariable DataItem.SourceType sourceType) {
        return ResponseEntity.ok(dataItemService.findBySourceType(sourceType));
    }

    @GetMapping("/search")
    @Operation(summary = "Search data items", description = "Search data items by name, provider, or description")
    public ResponseEntity<Page<DataItem>> search(@RequestParam String query, Pageable pageable) {
        return ResponseEntity.ok(dataItemService.search(query, pageable));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update data item", description = "Updates an existing data item")
    public ResponseEntity<DataItem> update(@PathVariable UUID id, @RequestBody DataItem dataItem) {
        try {
            DataItem updated = dataItemService.update(id, dataItem);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PatchMapping("/{id}/link-elasticsearch")
    @Operation(summary = "Link to Elasticsearch", description = "Associates a data item with its Elasticsearch document ID")
    public ResponseEntity<DataItem> linkToElasticsearch(
            @PathVariable UUID id,
            @RequestParam String elasticsearchId) {
        try {
            DataItem updated = dataItemService.linkToElasticsearch(id, elasticsearchId);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete data item", description = "Deletes a data item from the registry")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        dataItemService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
