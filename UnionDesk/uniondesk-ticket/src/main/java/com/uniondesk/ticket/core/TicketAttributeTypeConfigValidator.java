package com.uniondesk.ticket.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.util.StringUtils;

@SuppressWarnings("unchecked")
public final class TicketAttributeTypeConfigValidator {

    private static final Set<String> INPUT_FORMATS = Set.of("text", "email", "phone", "integer", "decimal");
    private static final Set<String> FIELD_TYPES = Set.of("input", "select", "switch", "date", "member");

    private TicketAttributeTypeConfigValidator() {
    }

    public static Map<String, Object> validateAndNormalize(
            String fieldType,
            Object typeConfig,
            ObjectMapper objectMapper) {
        String normalizedType = requiredFieldType(fieldType);
        Map<String, Object> config = normalizeConfig(typeConfig, objectMapper);
        return switch (normalizedType) {
            case "input" -> validateInput(config);
            case "select" -> validateSelect(config);
            case "switch" -> validateEmpty(config);
            case "date" -> validateDate(config);
            case "member" -> validateMember(config);
            default -> throw new IllegalArgumentException("不支持的字段类型");
        };
    }

    private static String requiredFieldType(String fieldType) {
        if (!StringUtils.hasText(fieldType)) {
            throw new IllegalArgumentException("field_type is required");
        }
        String normalized = fieldType.trim().toLowerCase(Locale.ROOT);
        if (!FIELD_TYPES.contains(normalized)) {
            throw new IllegalArgumentException("不支持的字段类型");
        }
        return normalized;
    }

    private static Map<String, Object> normalizeConfig(Object typeConfig, ObjectMapper objectMapper) {
        if (typeConfig == null) {
            return new LinkedHashMap<>();
        }
        if (typeConfig instanceof Map<?, ?> map) {
            Map<String, Object> copy = new LinkedHashMap<>();
            map.forEach((key, value) -> copy.put(String.valueOf(key), value));
            return copy;
        }
        throw new IllegalArgumentException("type_config 格式无效");
    }

    private static Map<String, Object> validateInput(Map<String, Object> config) {
        String format = config.get("format") == null ? "text" : String.valueOf(config.get("format")).trim().toLowerCase(Locale.ROOT);
        if (!INPUT_FORMATS.contains(format)) {
            throw new IllegalArgumentException("输入格式无效");
        }
        boolean multiline = parseBoolean(config.get("multiline"), false);
        if (multiline && !"text".equals(format)) {
            throw new IllegalArgumentException("仅文本输入支持多行");
        }
        Map<String, Object> normalized = new LinkedHashMap<>();
        normalized.put("format", format);
        if (multiline) {
            normalized.put("multiline", true);
        }
        String unit = config.get("unit") == null ? null : String.valueOf(config.get("unit")).trim();
        if (StringUtils.hasText(unit)) {
            if (!"integer".equals(format) && !"decimal".equals(format)) {
                throw new IllegalArgumentException("仅数字输入支持单位");
            }
            normalized.put("unit", unit);
        }
        rejectUnexpectedKeys(config, normalized.keySet());
        return normalized;
    }

    private static Map<String, Object> validateSelect(Map<String, Object> config) {
        boolean multiple = parseBoolean(config.get("multiple"), false);
        String optionsSource = config.get("options_source") == null
                ? null
                : String.valueOf(config.get("options_source")).trim().toLowerCase(Locale.ROOT);
        if ("priority_levels".equals(optionsSource)) {
            Map<String, Object> normalized = new LinkedHashMap<>();
            normalized.put("options_source", "priority_levels");
            if (multiple) {
                normalized.put("multiple", true);
            }
            rejectUnexpectedKeys(config, normalized.keySet());
            return normalized;
        }
        Object optionsObj = config.get("options");
        if (!(optionsObj instanceof List<?> options) || options.isEmpty()) {
            throw new IllegalArgumentException("选项类至少配置一个选项");
        }
        Set<String> values = new HashSet<>();
        List<Map<String, Object>> normalizedOptions = new java.util.ArrayList<>();
        for (Object item : options) {
            if (!(item instanceof Map<?, ?> option)) {
                throw new IllegalArgumentException("选项格式无效");
            }
            String label = option.get("label") == null ? null : String.valueOf(option.get("label")).trim();
            String value = option.get("value") == null ? null : String.valueOf(option.get("value")).trim();
            if (!StringUtils.hasText(label) || !StringUtils.hasText(value)) {
                throw new IllegalArgumentException("选项 label 与 value 均不能为空");
            }
            if (!values.add(value)) {
                throw new IllegalArgumentException("选项 value 不能重复");
            }
            Map<String, Object> normalizedOption = new LinkedHashMap<>();
            normalizedOption.put("label", label);
            normalizedOption.put("value", value);
            String color = option.get("color") == null ? null : String.valueOf(option.get("color")).trim();
            if (StringUtils.hasText(color)) {
                if (!color.matches("^#[0-9A-Fa-f]{6}$")) {
                    throw new IllegalArgumentException("选项颜色格式无效");
                }
                normalizedOption.put("color", color.toLowerCase(Locale.ROOT));
            }
            String icon = option.get("icon") == null ? null : String.valueOf(option.get("icon")).trim();
            if (StringUtils.hasText(icon)) {
                normalizedOption.put("icon", icon);
            }
            normalizedOptions.add(normalizedOption);
        }
        Map<String, Object> normalized = new LinkedHashMap<>();
        normalized.put("options", normalizedOptions);
        if (multiple) {
            normalized.put("multiple", true);
        }
        rejectUnexpectedKeys(config, normalized.keySet());
        return normalized;
    }

    private static Map<String, Object> validateMember(Map<String, Object> config) {
        boolean multiple = parseBoolean(config.get("multiple"), false);
        String scopeMode = config.get("scope_mode") == null
                ? "auto"
                : String.valueOf(config.get("scope_mode")).trim().toLowerCase(Locale.ROOT);
        if (!Set.of("auto", "domain", "platform").contains(scopeMode)) {
            throw new IllegalArgumentException("成员选择范围无效");
        }
        Map<String, Object> normalized = new LinkedHashMap<>();
        if (multiple) {
            normalized.put("multiple", true);
        }
        normalized.put("scope_mode", scopeMode);
        rejectUnexpectedKeys(config, Set.of("multiple", "scope_mode"));
        return normalized;
    }

    private static Map<String, Object> validateDate(Map<String, Object> config) {
        boolean withTime = parseBoolean(config.get("withTime"), false);
        Map<String, Object> normalized = new LinkedHashMap<>();
        if (withTime) {
            normalized.put("withTime", true);
        }
        rejectUnexpectedKeys(config, normalized.keySet());
        return normalized;
    }

    private static Map<String, Object> validateEmpty(Map<String, Object> config) {
        if (!config.isEmpty()) {
            throw new IllegalArgumentException("该字段类型不支持额外配置");
        }
        return new LinkedHashMap<>();
    }

    private static boolean parseBoolean(Object value, boolean defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }

    private static void rejectUnexpectedKeys(Map<String, Object> config, Set<String> allowedKeys) {
        for (String key : config.keySet()) {
            if (!allowedKeys.contains(key)) {
                throw new IllegalArgumentException("type_config 包含不支持的字段: " + key);
            }
        }
    }
}

