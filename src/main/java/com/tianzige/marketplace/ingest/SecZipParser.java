package com.tianzige.marketplace.ingest;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Parses an SEC quarterly ZIP file (EDGAR Financial Statements Data Set).
 * Each TSV file in the ZIP is parsed into a {@link ParsedFile} containing
 * the inferred schema and up to {@code maxRowsPerFile} rows.
 */
@Slf4j
@Component
public class SecZipParser {

    private static final Set<String> SUPPORTED_FILES =
            Set.of("sub.txt", "tag.txt", "num.txt", "pre.txt");

    private final TsvSchemaInferrer schemaInferrer = new TsvSchemaInferrer();

    /**
     * @param zipStream      input stream of the ZIP file
     * @param maxRowsPerFile maximum data rows to load per file (0 = unlimited)
     * @return list of parsed files, one per supported TSV entry in the ZIP
     */
    public List<ParsedFile> parse(InputStream zipStream, int maxRowsPerFile) throws IOException {
        List<ParsedFile> results = new ArrayList<>();

        try (ZipInputStream zip = new ZipInputStream(zipStream)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                String name = new File(entry.getName()).getName(); // strip any path prefix
                if (!SUPPORTED_FILES.contains(name)) {
                    zip.closeEntry();
                    continue;
                }

                log.info("Parsing SEC file: {}", name);
                ParsedFile parsed = parseTsv(name, zip, maxRowsPerFile);
                results.add(parsed);
                log.info("Parsed {} rows from {} (schema: {} fields)", parsed.getRows().size(), name, parsed.getFields().size());
                zip.closeEntry();
            }
        }

        return results;
    }

    private ParsedFile parseTsv(String filename, InputStream stream, int maxRowsPerFile) throws IOException {
        // ZipInputStream must not be closed here — only the entry is consumed
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"));

        String headerLine = reader.readLine();
        if (headerLine == null) {
            return ParsedFile.builder()
                    .filename(filename)
                    .fields(Collections.emptyList())
                    .rows(Collections.emptyList())
                    .totalRows(0)
                    .build();
        }

        List<String> headers = Arrays.asList(headerLine.split("\t", -1));

        List<Map<String, Object>> rows = new ArrayList<>();
        long totalRows = 0;
        String line;

        while ((line = reader.readLine()) != null) {
            totalRows++;
            if (maxRowsPerFile > 0 && rows.size() >= maxRowsPerFile) {
                continue; // keep counting total but stop collecting
            }

            String[] values = line.split("\t", -1);
            Map<String, Object> row = new LinkedHashMap<>();
            for (int i = 0; i < headers.size(); i++) {
                String value = i < values.length ? values[i].trim() : "";
                row.put(headers.get(i), value.isEmpty() ? null : value);
            }
            rows.add(row);
        }

        // Use collected rows as the inference sample
        List<ParsedFile.FieldSchema> fields = schemaInferrer.infer(headers, rows);

        return ParsedFile.builder()
                .filename(filename)
                .fields(fields)
                .rows(rows)
                .totalRows(totalRows)
                .build();
    }
}
