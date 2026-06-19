package com.uniondesk.ticket.core;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class FormSchemaValidatorTests {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void injectsSystemFieldsWhenMissing() {
        Map<String, Object> schema = FormSchemaValidator.mergeAndValidate(null, objectMapper);
        Map<String, Object> properties = (Map<String, Object>) schema.get("properties");
        assertThat(properties).containsKeys("title", "description");
    }

    @Test
    void mergesMissingDescriptionFromSystemDefault() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", Map.of("title", Map.of(
                "type", "string",
                "title", "标题",
                "x-component", "Input",
                "required", true)));

        Map<String, Object> merged = FormSchemaValidator.mergeAndValidate(schema, objectMapper);
        Map<String, Object> properties = (Map<String, Object>) merged.get("properties");
        assertThat(properties).containsKeys("title", "description");
    }
}
