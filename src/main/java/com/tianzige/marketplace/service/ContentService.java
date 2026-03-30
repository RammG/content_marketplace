package com.tianzige.marketplace.service;

import com.tianzige.marketplace.model.content.ContentDocument;
import com.tianzige.marketplace.repository.ContentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContentService {

    private final ContentRepository contentRepository;

    public ContentDocument store(ContentDocument document) {
        if (document.getId() == null) {
            document.setId(UUID.randomUUID().toString());
        }
        document.setIndexedAt(LocalDateTime.now());
        document.setUpdatedAt(LocalDateTime.now());
        log.debug("Storing content document: {}", document.getId());
        return contentRepository.save(document);
    }

    public Optional<ContentDocument> findById(String id) {
        return contentRepository.findById(id);
    }

    public Optional<ContentDocument> findByDataItemId(String dataItemId) {
        return contentRepository.findByDataItemId(dataItemId);
    }

    public List<ContentDocument> findByProvider(String provider) {
        return contentRepository.findByProvider(provider);
    }

    public Page<ContentDocument> findByContentType(String contentType, Pageable pageable) {
        return contentRepository.findByContentType(contentType, pageable);
    }

    public Page<ContentDocument> search(String query, Pageable pageable) {
        log.debug("Searching content with query: {}", query);
        return contentRepository.searchByContent(query, pageable);
    }

    public Page<ContentDocument> searchByProvider(String provider, String query, Pageable pageable) {
        log.debug("Searching content for provider {} with query: {}", provider, query);
        return contentRepository.searchByProviderAndContent(provider, query, pageable);
    }

    public ContentDocument update(String id, ContentDocument updatedDocument) {
        return contentRepository.findById(id)
                .map(existing -> {
                    existing.setRawContent(updatedDocument.getRawContent());
                    existing.setNormalizedContent(updatedDocument.getNormalizedContent());
                    existing.setContentType(updatedDocument.getContentType());
                    existing.setTitle(updatedDocument.getTitle());
                    existing.setSummary(updatedDocument.getSummary());
                    existing.setExtractedFields(updatedDocument.getExtractedFields());
                    existing.setUpdatedAt(LocalDateTime.now());
                    log.debug("Updating content document: {}", id);
                    return contentRepository.save(existing);
                })
                .orElseThrow(() -> new IllegalArgumentException("ContentDocument not found: " + id));
    }

    public void delete(String id) {
        log.debug("Deleting content document: {}", id);
        contentRepository.deleteById(id);
    }
}
