package com.uniondesk.ticket.core;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;

public final class DefaultFormSchemaProvider {

    private static final String DEFAULT_SCHEMA_JSON = """
            {
              "type": "object",
              "properties": {
                "title": {
                  "type": "string",
                  "title": "标题",
                  "x-component": "Input",
                  "x-decorator": "FormItem",
                  "required": true,
                  "x-system-field": true,
                  "x-index": 0
                },
                "description": {
                  "type": "string",
                  "title": "详细描述",
                  "x-component": "Input.TextArea",
                  "x-decorator": "FormItem",
                  "required": true,
                  "x-component-props": { "rows": 4, "placeholder": "请描述您的问题或建议" },
                  "x-system-field": true,
                  "x-index": 1
                }
              }
            }
            """;

    private DefaultFormSchemaProvider() {
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> defaultSchema(ObjectMapper objectMapper) {
        try {
            return objectMapper.readValue(DEFAULT_SCHEMA_JSON, new TypeReference<LinkedHashMap<String, Object>>() {
            });
        } catch (Exception ex) {
            throw new IllegalStateException("invalid default form schema", ex);
        }
    }

    public static String defaultSchemaJson() {
        return DEFAULT_SCHEMA_JSON;
    }
}
