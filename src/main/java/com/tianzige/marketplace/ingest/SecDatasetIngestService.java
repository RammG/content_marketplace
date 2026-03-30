package com.tianzige.marketplace.ingest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tianzige.marketplace.model.content.ContentDocument;
import com.tianzige.marketplace.model.dir.DataItem;
import com.tianzige.marketplace.repository.ContentRepository;
import com.tianzige.marketplace.repository.DataItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Orchestrates ingestion of an SEC quarterly ZIP file:
 * <ol>
 *   <li>Parses each TSV file to extract schema and rows.</li>
 *   <li>Registers a {@link DataItem} in the DIR (PostgreSQL) with field metadata.</li>
 *   <li>Bulk-indexes each row as a {@link ContentDocument} in Elasticsearch.</li>
 *   <li>Links the DataItem to the Elasticsearch index name.</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SecDatasetIngestService {

    private static final int BATCH_SIZE = 500;
    private static final String PROVIDER = "SEC";
    private static final String ES_INDEX = "content_documents";

    private final SecZipParser zipParser;
    private final DataItemRepository dataItemRepository;
    private final ContentRepository contentRepository;
    private final ObjectMapper objectMapper;

    /**
     * @param zipStream      ZIP file input stream
     * @param quarter        e.g. "2025Q4"
     * @param maxRowsPerFile max rows to index per file (0 = unlimited)
     * @return summary of ingested files
     */
    @Transactional
    public IngestSummary ingest(InputStream zipStream, String quarter, int maxRowsPerFile) throws IOException {
        List<ParsedFile> parsedFiles = zipParser.parse(zipStream, maxRowsPerFile);

        List<IngestSummary.FileResult> results = new ArrayList<>();
        for (ParsedFile parsedFile : parsedFiles) {
            IngestSummary.FileResult result = ingestFile(parsedFile, quarter);
            results.add(result);
            log.info("Ingested {}: {} DIR entry, {} documents indexed", parsedFile.getFilename(),
                    result.getDataItemId(), result.getDocumentsIndexed());
        }

        return new IngestSummary(quarter, results);
    }

    private IngestSummary.FileResult ingestFile(ParsedFile parsedFile, String quarter) {
        // 1. Build DIR metadata
        Map<String, Object> metadata = buildDirMetadata(parsedFile, quarter);

        DataItem dataItem = DataItem.builder()
                .name("SEC " + quarter + " - " + parsedFile.getFilename())
                .sourceType(DataItem.SourceType.OTHER) // TSV
                .sourceFormat("text/tab-separated-values")
                .contentType("financial/sec-edgar")
                .provider(PROVIDER)
                .version(quarter)
                .description("SEC EDGAR Financial Statements Data Set - " + parsedFile.getFilename())
                .elasticsearchId(ES_INDEX)
                .metadata(metadata)
                .build();

        DataItem savedDataItem = dataItemRepository.save(dataItem);
        log.debug("Registered DataItem {} for {}", savedDataItem.getId(), parsedFile.getFilename());

        // 2. Index rows to Elasticsearch in batches
        int totalIndexed = indexRowsBatched(parsedFile, savedDataItem.getId().toString());

        return IngestSummary.FileResult.builder()
                .filename(parsedFile.getFilename())
                .dataItemId(savedDataItem.getId())
                .documentsIndexed(totalIndexed)
                .totalRowsInFile(parsedFile.getTotalRows())
                .fieldCount(parsedFile.getFields().size())
                .build();
    }

    private int indexRowsBatched(ParsedFile parsedFile, String dataItemId) {
        List<Map<String, Object>> rows = parsedFile.getRows();
        int indexed = 0;

        for (int i = 0; i < rows.size(); i += BATCH_SIZE) {
            List<Map<String, Object>> batch = rows.subList(i, Math.min(i + BATCH_SIZE, rows.size()));

            List<ContentDocument> docs = batch.stream()
                    .map(row -> toContentDocument(row, dataItemId, parsedFile))
                    .collect(Collectors.toList());

            contentRepository.saveAll(docs);
            indexed += docs.size();
            log.debug("Indexed batch {}/{} for {}", indexed, rows.size(), parsedFile.getFilename());
        }

        return indexed;
    }

    private ContentDocument toContentDocument(Map<String, Object> row, String dataItemId, ParsedFile parsedFile) {
        String rawJson = toJson(row);
        String title = extractTitle(row, parsedFile.getFilename());

        return ContentDocument.builder()
                .id(UUID.randomUUID().toString())
                .dataItemId(dataItemId)
                .rawContent(rawJson)
                .contentType("financial/sec-edgar")
                .sourceType("TSV")
                .provider(PROVIDER)
                .title(title)
                .extractedFields(new LinkedHashMap<>(row))
                .indexedAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private String extractTitle(Map<String, Object> row, String filename) {
        // Use the most meaningful identifying field per file type
        return switch (filename) {
            case "sub.txt" -> {
                String adsh = str(row, "adsh");
                String name = str(row, "name");
                yield name.isEmpty() ? adsh : name + " (" + adsh + ")";
            }
            case "tag.txt" -> {
                String tag = str(row, "tag");
                String label = str(row, "tlabel");
                yield label.isEmpty() ? tag : label + " [" + tag + "]";
            }
            case "num.txt" -> str(row, "adsh") + " / " + str(row, "tag");
            case "pre.txt" -> str(row, "adsh") + " / " + str(row, "tag") + " / " + str(row, "stmt");
            default -> filename;
        };
    }

    private Map<String, Object> buildDirMetadata(ParsedFile parsedFile, String quarter) {
        List<Map<String, Object>> fieldMeta = parsedFile.getFields().stream()
                .map(f -> {
                    Map<String, Object> fm = new LinkedHashMap<>();
                    fm.put("name", f.getName());
                    fm.put("type", f.getType());
                    fm.put("nullable", f.isNullable());
                    return fm;
                })
                .collect(Collectors.toList());

        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("filename", parsedFile.getFilename());
        meta.put("quarter", quarter);
        meta.put("fields", fieldMeta);
        meta.put("fieldCount", parsedFile.getFields().size());
        meta.put("totalRows", parsedFile.getTotalRows());
        meta.put("indexedRows", parsedFile.getRows().size());
        return meta;
    }

    private String toJson(Map<String, Object> row) {
        try {
            return objectMapper.writeValueAsString(row);
        } catch (Exception e) {
            return row.toString();
        }
    }

    private String str(Map<String, Object> row, String key) {
        Object v = row.get(key);
        return v == null ? "" : v.toString();
    }
}
