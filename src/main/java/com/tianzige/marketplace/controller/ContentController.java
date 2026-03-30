package com.tianzige.marketplace.controller;

import com.tianzige.marketplace.model.content.ContentDocument;
import com.tianzige.marketplace.service.ContentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/contents")
@RequiredArgsConstructor
@Tag(name = "Content Store", description = "APIs for managing content documents in Elasticsearch")
public class ContentController {

    private final ContentService contentService;

    @PostMapping
    @Operation(summary = "Store content", description = "Stores a new content document in Elasticsearch")
    public ResponseEntity<ContentDocument> store(@RequestBody ContentDocument document) {
        ContentDocument stored = contentService.store(document);
        return ResponseEntity.status(HttpStatus.CREATED).body(stored);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get content by ID", description = "Returns a single content document by its ID")
    public ResponseEntity<ContentDocument> findById(@PathVariable String id) {
        return contentService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/by-data-item/{dataItemId}")
    @Operation(summary = "Get content by data item ID", description = "Returns the content document linked to a data item")
    public ResponseEntity<ContentDocument> findByDataItemId(@PathVariable String dataItemId) {
        return contentService.findByDataItemId(dataItemId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/by-provider/{provider}")
    @Operation(summary = "Get contents by provider", description = "Returns all content documents from a specific provider")
    public ResponseEntity<List<ContentDocument>> findByProvider(@PathVariable String provider) {
        return ResponseEntity.ok(contentService.findByProvider(provider));
    }

    @GetMapping("/by-content-type/{contentType}")
    @Operation(summary = "Get contents by content type", description = "Returns paginated content documents of a specific content type")
    public ResponseEntity<Page<ContentDocument>> findByContentType(
            @PathVariable String contentType,
            Pageable pageable) {
        return ResponseEntity.ok(contentService.findByContentType(contentType, pageable));
    }

    @GetMapping("/search")
    @Operation(summary = "Search content", description = "Full-text search across content documents")
    public ResponseEntity<Page<ContentDocument>> search(@RequestParam String query, Pageable pageable) {
        return ResponseEntity.ok(contentService.search(query, pageable));
    }

    @GetMapping("/search/by-provider")
    @Operation(summary = "Search content by provider", description = "Full-text search within a specific provider's content")
    public ResponseEntity<Page<ContentDocument>> searchByProvider(
            @RequestParam String provider,
            @RequestParam String query,
            Pageable pageable) {
        return ResponseEntity.ok(contentService.searchByProvider(provider, query, pageable));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update content", description = "Updates an existing content document")
    public ResponseEntity<ContentDocument> update(@PathVariable String id, @RequestBody ContentDocument document) {
        try {
            ContentDocument updated = contentService.update(id, document);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete content", description = "Deletes a content document from Elasticsearch")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        contentService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
