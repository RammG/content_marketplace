package com.tianzige.marketplace.ingest;

import com.tianzige.marketplace.AbstractIntegrationTest;
import com.tianzige.marketplace.model.content.ContentDocument;
import com.tianzige.marketplace.model.dir.DataItem;
import com.tianzige.marketplace.repository.ContentRepository;
import com.tianzige.marketplace.repository.DataItemRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SecDatasetIngestIT extends AbstractIntegrationTest {

    /** Rows per file to ingest during tests — keeps the suite fast. */
    private static final int MAX_ROWS_PER_FILE = 200;
    private static final String QUARTER = "2025Q4";
    private static final String ZIP_PATH = "2025q4.zip";

    @Autowired
    private SecDatasetIngestService ingestService;

    @Autowired
    private DataItemRepository dataItemRepository;

    @Autowired
    private ContentRepository contentRepository;

    @AfterEach
    void cleanup() {
        contentRepository.deleteAll();
        dataItemRepository.deleteAll();
    }

    // ── DIR registration ────────────────────────────────────────────────────

    @Test
    void ingest_registersOneDataItemPerFile() throws Exception {
        IngestSummary summary = runIngest();

        List<DataItem> dataItems = dataItemRepository.findAll();
        assertThat(dataItems).hasSize(4);
        assertThat(dataItems).extracting(DataItem::getProvider).containsOnly("SEC");
        assertThat(dataItems).extracting(DataItem::getVersion).containsOnly(QUARTER);
        assertThat(dataItems).extracting(DataItem::getSourceFormat)
                .containsOnly("text/tab-separated-values");
    }

    @Test
    void ingest_dirMetadataContainsFieldNamesAndTypes() throws Exception {
        runIngest();

        DataItem subItem = findDataItemByFilename("sub.txt");
        assertThat(subItem).isNotNull();

        Map<String, Object> metadata = subItem.getMetadata();
        assertThat(metadata).containsKey("fields");
        assertThat(metadata).containsKey("fieldCount");
        assertThat(metadata).containsKey("totalRows");
        assertThat(metadata).containsKey("quarter");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> fields = (List<Map<String, Object>>) metadata.get("fields");
        assertThat(fields).isNotEmpty();

        // sub.txt known fields
        List<String> fieldNames = fields.stream()
                .map(f -> f.get("name").toString())
                .toList();
        assertThat(fieldNames).contains("adsh", "cik", "name", "sic", "form", "period", "filed");

        // Every field has a type and nullable flag
        for (Map<String, Object> field : fields) {
            assertThat(field).containsKeys("name", "type", "nullable");
            assertThat(field.get("type").toString()).isIn("string", "integer", "decimal", "boolean");
        }
    }

    @Test
    void ingest_dirMetadataForTagFile_containsExpectedFields() throws Exception {
        runIngest();

        DataItem tagItem = findDataItemByFilename("tag.txt");
        assertThat(tagItem).isNotNull();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> fields =
                (List<Map<String, Object>>) tagItem.getMetadata().get("fields");

        List<String> fieldNames = fields.stream().map(f -> f.get("name").toString()).toList();
        assertThat(fieldNames).contains("tag", "version", "custom", "abstract", "datatype", "tlabel", "doc");
    }

    @Test
    void ingest_dirMetadataForNumFile_containsExpectedFields() throws Exception {
        runIngest();

        DataItem numItem = findDataItemByFilename("num.txt");
        assertThat(numItem).isNotNull();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> fields =
                (List<Map<String, Object>>) numItem.getMetadata().get("fields");

        List<String> fieldNames = fields.stream().map(f -> f.get("name").toString()).toList();
        assertThat(fieldNames).contains("adsh", "tag", "version", "ddate", "qtrs", "uom", "value");
    }

    @Test
    void ingest_dataItemLinkedToElasticsearch() throws Exception {
        runIngest();

        List<DataItem> items = dataItemRepository.findAll();
        assertThat(items).allMatch(item -> item.getElasticsearchId() != null
                && !item.getElasticsearchId().isBlank());
    }

    // ── Elasticsearch indexing ───────────────────────────────────────────────

    @Test
    void ingest_indexesContentDocumentsForAllFiles() throws Exception {
        IngestSummary summary = runIngest();

        long esCount = contentRepository.count();
        assertThat(esCount).isEqualTo(summary.totalDocumentsIndexed());
        assertThat(summary.totalDocumentsIndexed()).isGreaterThan(0);
    }

    @Test
    void ingest_contentDocumentsHaveCorrectProvider() throws Exception {
        runIngest();

        List<ContentDocument> docs = contentRepository.findByProvider("SEC");
        assertThat(docs).isNotEmpty();
    }

    @Test
    void ingest_contentDocumentsLinkedToDataItemId() throws Exception {
        runIngest();

        DataItem subItem = findDataItemByFilename("sub.txt");
        assertThat(subItem).isNotNull();

        List<ContentDocument> subDocs = contentRepository.findBySourceType("TSV");
        assertThat(subDocs).isNotEmpty();
        assertThat(subDocs).allMatch(doc -> doc.getDataItemId() != null);
    }

    @Test
    void ingest_subFileDocuments_containCompanyNameInTitle() throws Exception {
        runIngest();

        DataItem subItem = findDataItemByFilename("sub.txt");
        assertThat(subItem).isNotNull();

        List<ContentDocument> docs = contentRepository.findByProvider("SEC");
        List<ContentDocument> subDocs = docs.stream()
                .filter(d -> d.getDataItemId().equals(subItem.getId().toString()))
                .toList();

        assertThat(subDocs).isNotEmpty();
        // sub.txt titles are formatted as "CompanyName (adsh)"
        assertThat(subDocs.get(0).getTitle()).isNotBlank();
    }

    @Test
    void ingest_contentDocumentsHaveExtractedFields() throws Exception {
        runIngest();

        DataItem tagItem = findDataItemByFilename("tag.txt");
        assertThat(tagItem).isNotNull();

        List<ContentDocument> docs = contentRepository.findByProvider("SEC");
        ContentDocument tagDoc = docs.stream()
                .filter(d -> d.getDataItemId().equals(tagItem.getId().toString()))
                .findFirst()
                .orElseThrow();

        assertThat(tagDoc.getExtractedFields()).containsKey("tag");
        assertThat(tagDoc.getExtractedFields()).containsKey("datatype");
        assertThat(tagDoc.getRawContent()).isNotBlank();
    }

    @Test
    void ingest_respectsMaxRowsPerFile() throws Exception {
        IngestSummary summary = runIngest();

        for (IngestSummary.FileResult result : summary.getFileResults()) {
            assertThat(result.getDocumentsIndexed()).isLessThanOrEqualTo(MAX_ROWS_PER_FILE);
            // totalRows reflects the actual file size, not the limit
            assertThat(result.getTotalRowsInFile()).isGreaterThanOrEqualTo(result.getDocumentsIndexed());
        }
    }

    @Test
    void ingest_summaryReportsCorrectFilesProcessed() throws Exception {
        IngestSummary summary = runIngest();

        assertThat(summary.getQuarter()).isEqualTo(QUARTER);
        assertThat(summary.totalFilesProcessed()).isEqualTo(4);
        assertThat(summary.getFileResults())
                .extracting(IngestSummary.FileResult::getFilename)
                .containsExactlyInAnyOrder("sub.txt", "tag.txt", "num.txt", "pre.txt");
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private IngestSummary runIngest() throws Exception {
        InputStream zipStream = new ClassPathResource(ZIP_PATH).getInputStream();
        return ingestService.ingest(zipStream, QUARTER, MAX_ROWS_PER_FILE);
    }

    private DataItem findDataItemByFilename(String filename) {
        return dataItemRepository.findAll().stream()
                .filter(item -> {
                    Object fn = item.getMetadata() != null ? item.getMetadata().get("filename") : null;
                    return filename.equals(fn);
                })
                .findFirst()
                .orElse(null);
    }
}
