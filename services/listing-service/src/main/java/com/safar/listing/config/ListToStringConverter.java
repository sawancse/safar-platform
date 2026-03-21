package com.safar.listing.config;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Converter
public class ListToStringConverter implements AttributeConverter<List<String>, String> {

    @Override
    public String convertToDatabaseColumn(List<String> list) {
        if (list == null || list.isEmpty()) return null;
        return "{" + String.join(",", list.stream()
                .map(s -> "\"" + s.replace("\"", "\\\"") + "\"")
                .toArray(String[]::new)) + "}";
    }

    @Override
    public List<String> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) return Collections.emptyList();
        String stripped = dbData.replaceAll("^\\{|\\}$", "");
        if (stripped.isBlank()) return Collections.emptyList();
        return Arrays.stream(stripped.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)"))
                .map(s -> s.replaceAll("^\"|\"$", "").trim())
                .toList();
    }
}
