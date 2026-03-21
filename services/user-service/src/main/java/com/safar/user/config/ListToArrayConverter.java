package com.safar.user.config;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Converts {@code List<String>} to/from PostgreSQL array literal syntax
 * (e.g. {@code {"wifi","pool"}}) stored as a TEXT column.
 *
 * <p>Using a converter rather than {@code columnDefinition = "TEXT[]"} keeps
 * the mapping transparent to both PostgreSQL (production) and H2 (tests),
 * where native array types are not supported.</p>
 */
@Converter
public class ListToArrayConverter implements AttributeConverter<List<String>, String> {

    @Override
    public String convertToDatabaseColumn(List<String> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        String[] quoted = list.stream()
                .map(s -> "\"" + s.replace("\"", "\\\"") + "\"")
                .toArray(String[]::new);
        return "{" + String.join(",", quoted) + "}";
    }

    @Override
    public List<String> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return Collections.emptyList();
        }
        // Strip surrounding braces
        String stripped = dbData.replaceAll("^\\{|\\}$", "");
        if (stripped.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.stream(stripped.split(","))
                .map(s -> s.replaceAll("^\"|\"$", "").trim())
                .toList();
    }
}
