package com.uniondesk.ticket.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;

@SuppressWarnings("unchecked")
public final class FormSchemaValidator {

    private FormSchemaValidator() {
    }

    public static Map<String, Object> mergeAndValidate(Object formSchema, ObjectMapper objectMapper) {
        Map<String, Object> schema = normalizeSchema(formSchema, objectMapper);
        Object propertiesObj = schema.get("properties");
        if (!(propertiesObj instanceof Map<?, ?> properties)) {
            throw new IllegalArgumentException("表单 schema 格式无效");
        }
        Map<String, Object> merged = new LinkedHashMap<>(DefaultFormSchemaProvider.defaultSchema(objectMapper));
        Map<String, Object> mergedProperties = new LinkedHashMap<>();
        properties.forEach((key, value) -> mergedProperties.put(String.valueOf(key), value));
        Map<String, Object> defaultProperties = (Map<String, Object>) merged.get("properties");
        defaultProperties.forEach(mergedProperties::put);
        merged.put("properties", mergedProperties);
        validateSystemFields(mergedProperties);
        return merged;
    }

    private static Map<String, Object> normalizeSchema(Object formSchema, ObjectMapper objectMapper) {
        if (formSchema == null) {
            return DefaultFormSchemaProvider.defaultSchema(objectMapper);
        }
        if (formSchema instanceof Map<?, ?> map) {
            Map<String, Object> copy = new LinkedHashMap<>();
            map.forEach((key, value) -> copy.put(String.valueOf(key), value));
            return copy;
        }
        throw new IllegalArgumentException("表单 schema 格式无效");
    }

    private static void validateSystemFields(Map<String, Object> properties) {
        validateSystemField(properties, "title", "标题", "Input");
        validateSystemField(properties, "description", "详细描述", "Input.TextArea");
    }

    private static void validateSystemField(
            Map<String, Object> properties, String key, String label, String component) {
        Object fieldObj = properties.get(key);
        if (!(fieldObj instanceof Map<?, ?> field)) {
            throw new IllegalArgumentException("工单类型必须包含「" + label + "」系统字段");
        }
        if (!Boolean.TRUE.equals(field.get("required"))) {
            throw new IllegalArgumentException("系统字段「" + label + "」不可改为非必填");
        }
        String xComponent = field.get("x-component") == null ? null : String.valueOf(field.get("x-component"));
        if (!component.equals(xComponent)) {
            throw new IllegalArgumentException("系统字段「" + label + "」组件类型不可变更");
        }
    }
}
