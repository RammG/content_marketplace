package com.tianzige.marketplace.ingest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Infers field types from sampled TSV rows.
 * Priority: integer > decimal > boolean > string
 */
public class TsvSchemaInferrer {

    public List<ParsedFile.FieldSchema> infer(List<String> headers, List<Map<String, Object>> sampleRows) {
        List<ParsedFile.FieldSchema> fields = new ArrayList<>();
        for (String header : headers) {
            boolean nullable = false;
            String inferredType = "string";

            List<String> values = sampleRows.stream()
                    .map(row -> row.getOrDefault(header, "").toString().trim())
                    .toList();

            long nonEmpty = values.stream().filter(v -> !v.isEmpty()).count();
            nullable = nonEmpty < values.size();

            if (nonEmpty > 0) {
                List<String> nonEmptyValues = values.stream().filter(v -> !v.isEmpty()).toList();
                if (allMatch(nonEmptyValues, this::isInteger)) {
                    inferredType = "integer";
                } else if (allMatch(nonEmptyValues, this::isDecimal)) {
                    inferredType = "decimal";
                } else if (allMatch(nonEmptyValues, this::isBoolean)) {
                    inferredType = "boolean";
                }
            }

            fields.add(ParsedFile.FieldSchema.builder()
                    .name(header)
                    .type(inferredType)
                    .nullable(nullable)
                    .build());
        }
        return fields;
    }

    private boolean allMatch(List<String> values, java.util.function.Predicate<String> predicate) {
        return !values.isEmpty() && values.stream().allMatch(predicate);
    }

    private boolean isInteger(String value) {
        try {
            Long.parseLong(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean isDecimal(String value) {
        try {
            Double.parseDouble(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean isBoolean(String value) {
        return value.equals("0") || value.equals("1")
                || value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false");
    }
}
