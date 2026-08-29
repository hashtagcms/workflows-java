package org.hashtagcms.workflows.model;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.List;
import java.util.Map;

/** Shared Jackson mapper + JPA converters for the JSON columns. */
public final class JsonSupport {

    public static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonSupport() {}

    @Converter
    public static class MapConverter implements AttributeConverter<Map<String, Object>, String> {
        @Override
        public String convertToDatabaseColumn(Map<String, Object> attribute) {
            if (attribute == null) return null;
            try {
                return MAPPER.writeValueAsString(attribute);
            } catch (Exception e) {
                throw new IllegalStateException("Could not serialise JSON map", e);
            }
        }

        @Override
        public Map<String, Object> convertToEntityAttribute(String dbData) {
            if (dbData == null || dbData.isBlank()) return null;
            try {
                return MAPPER.readValue(dbData, new TypeReference<>() {});
            } catch (Exception e) {
                throw new IllegalStateException("Could not read JSON map: " + dbData, e);
            }
        }
    }

    @Converter
    public static class ListConverter implements AttributeConverter<List<Object>, String> {
        @Override
        public String convertToDatabaseColumn(List<Object> attribute) {
            if (attribute == null) return null;
            try {
                return MAPPER.writeValueAsString(attribute);
            } catch (Exception e) {
                throw new IllegalStateException("Could not serialise JSON list", e);
            }
        }

        @Override
        public List<Object> convertToEntityAttribute(String dbData) {
            if (dbData == null || dbData.isBlank()) return null;
            try {
                return MAPPER.readValue(dbData, new TypeReference<>() {});
            } catch (Exception e) {
                throw new IllegalStateException("Could not read JSON list: " + dbData, e);
            }
        }
    }

    @Converter
    public static class StringMapConverter implements AttributeConverter<Map<String, String>, String> {
        @Override
        public String convertToDatabaseColumn(Map<String, String> attribute) {
            if (attribute == null) return null;
            try {
                return MAPPER.writeValueAsString(attribute);
            } catch (Exception e) {
                throw new IllegalStateException("Could not serialise JSON string-map", e);
            }
        }

        @Override
        public Map<String, String> convertToEntityAttribute(String dbData) {
            if (dbData == null || dbData.isBlank()) return null;
            try {
                return MAPPER.readValue(dbData, new TypeReference<>() {});
            } catch (Exception e) {
                throw new IllegalStateException("Could not read JSON string-map: " + dbData, e);
            }
        }
    }
}
