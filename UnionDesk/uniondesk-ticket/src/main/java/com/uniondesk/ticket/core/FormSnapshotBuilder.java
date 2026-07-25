package com.uniondesk.ticket.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uniondesk.ticket.entity.TicketAttributePo;
import com.uniondesk.ticket.entity.TicketTypeAttributeSlotPo;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.util.StringUtils;

@SuppressWarnings("unchecked")
public final class FormSnapshotBuilder {

    private FormSnapshotBuilder() {
    }

    public record SlotContext(
            TicketTypeAttributeSlotPo slot,
            TicketAttributePo attribute) {
    }

    public static Map<String, Object> build(
            String category,
            List<SlotContext> slots,
            ObjectMapper objectMapper) {
        if (slots != null && !slots.isEmpty() && containsBoundSystemAttribute(slots)) {
            return buildFromSlotContexts(category, slots, objectMapper);
        }
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        Map<String, Object> properties = new LinkedHashMap<>();
        int index = 0;
        index = appendSystemFields(properties, category, index);
        for (SlotContext context : slots) {
            if (!TicketTypeAttributeSlotPo.STATUS_ENABLED.equals(context.slot().getStatus())) {
                continue;
            }
            if (!TicketAttributePo.STATUS_ACTIVE.equals(context.attribute().getStatus())) {
                continue;
            }
            Map<String, Object> field = buildCustomField(context, objectMapper, index);
            properties.put(fieldKey(context.attribute()), field);
            index++;
        }
        schema.put("properties", properties);
        return schema;
    }

    public static Map<String, Object> buildFromSlotContexts(
            String category,
            List<SlotContext> slots,
            ObjectMapper objectMapper) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        Map<String, Object> properties = new LinkedHashMap<>();
        int index = 0;
        List<SlotContext> sorted = slots.stream()
                .sorted(java.util.Comparator.comparingInt(item -> item.slot().getSortOrder()))
                .toList();
        for (SlotContext context : sorted) {
            if (!TicketTypeAttributeSlotPo.STATUS_ENABLED.equals(context.slot().getStatus())) {
                continue;
            }
            if (!TicketAttributePo.STATUS_ACTIVE.equals(context.attribute().getStatus())) {
                continue;
            }
            Map<String, Object> slotConfig = readJsonMap(context.slot().getSlotConfig(), objectMapper);
            String systemKey = systemFieldKeyForAttribute(context.attribute());
            if ("title".equals(systemKey)) {
                properties.put("title", systemTitleField(index, slotConfig));
                index++;
                continue;
            }
            if ("description".equals(systemKey)) {
                properties.put("description", systemDescriptionField(index, slotConfig));
                index++;
                continue;
            }
            Map<String, Object> field = buildCustomField(context, objectMapper, index);
            if (systemKey != null) {
                field.put("x-system-field", true);
                field.put("x-system-key", systemKey);
                properties.put(systemKey, field);
            }
            else {
                properties.put(fieldKey(context.attribute()), field);
            }
            index++;
        }
        if (properties.isEmpty()) {
            index = appendSystemFields(properties, category, index);
        }
        schema.put("properties", properties);
        return schema;
    }

    private static boolean containsBoundSystemAttribute(List<SlotContext> slots) {
        for (SlotContext context : slots) {
            if (systemFieldKeyForAttribute(context.attribute()) != null) {
                return true;
            }
        }
        return false;
    }

    static String systemFieldKeyForAttribute(TicketAttributePo attribute) {
        if (attribute == null || !attribute.isSystem()) {
            return null;
        }
        if (StringUtils.hasText(attribute.getSystemKey())) {
            return attribute.getSystemKey().trim().toLowerCase(Locale.ROOT);
        }
        // 兼容未回填 system_key 的旧数据
        return switch (attribute.getName()) {
            case "标题" -> "title";
            case "描述" -> "description";
            case "优先级" -> "priority";
            case "处理人" -> "assignee";
            case "关注人" -> "watchers";
            default -> null;
        };
    }

    private static int appendSystemFields(Map<String, Object> properties, String category, int index) {
        String normalized = StringUtils.hasText(category) ? category.trim().toLowerCase() : "transaction";
        if ("feedback".equals(normalized)) {
            properties.put("description", systemDescriptionField(index, Map.of("required", true)));
            return index + 1;
        }
        properties.put("title", systemTitleField(index, Map.of("required", true)));
        properties.put("description", systemDescriptionField(index + 1, Map.of("required", true)));
        return index + 2;
    }

    private static Map<String, Object> systemTitleField(int index) {
        return systemTitleField(index, Map.of("required", true));
    }

    private static Map<String, Object> systemTitleField(int index, Map<String, Object> slotConfig) {
        Map<String, Object> field = new LinkedHashMap<>();
        field.put("type", "string");
        field.put("title", displayTitle(slotConfig, "标题"));
        field.put("x-component", "Input");
        field.put("x-decorator", "FormItem");
        field.put("required", Boolean.TRUE.equals(slotConfig.get("required")));
        field.put("x-system-field", true);
        field.put("x-display", "block");
        field.put("x-index", index);
        return field;
    }

    private static Map<String, Object> systemDescriptionField(int index) {
        return systemDescriptionField(index, Map.of("required", true));
    }

    private static Map<String, Object> systemDescriptionField(int index, Map<String, Object> slotConfig) {
        Map<String, Object> field = new LinkedHashMap<>();
        field.put("type", "string");
        field.put("title", displayTitle(slotConfig, "描述"));
        field.put("x-component", "Input.TextArea");
        field.put("x-decorator", "FormItem");
        field.put("required", Boolean.TRUE.equals(slotConfig.get("required")));
        field.put("x-system-field", true);
        field.put("x-display", "block");
        field.put("x-index", index);
        Map<String, Object> componentProps = new LinkedHashMap<>();
        componentProps.put("rows", 4);
        Object placeholder = slotConfig.get("placeholder");
        componentProps.put(
                "placeholder",
                placeholder == null || !StringUtils.hasText(String.valueOf(placeholder))
                        ? "请描述您的问题或建议"
                        : String.valueOf(placeholder).trim());
        field.put("x-component-props", componentProps);
        return field;
    }

    private static String displayTitle(Map<String, Object> slotConfig, String fallback) {
        Object displayName = slotConfig.get("display_name");
        if (displayName != null && StringUtils.hasText(String.valueOf(displayName))) {
            return String.valueOf(displayName).trim();
        }
        return fallback;
    }

    private static Map<String, Object> buildCustomField(
            SlotContext context,
            ObjectMapper objectMapper,
            int index) {
        TicketAttributePo attribute = context.attribute();
        Map<String, Object> slotConfig = readJsonMap(context.slot().getSlotConfig(), objectMapper);
        Map<String, Object> typeConfig = readJsonMap(attribute.getTypeConfig(), objectMapper);
        Map<String, Object> field = new LinkedHashMap<>();
        field.put("title", displayTitle(slotConfig, attribute.getName()));
        field.put("description", attribute.getDescription());
        field.put("x-decorator", "FormItem");
        field.put("x-display", "block");
        field.put("x-index", index);
        field.put("x-attribute-id", attribute.getId());
        field.put("required", Boolean.TRUE.equals(slotConfig.get("required")));
        if (Boolean.FALSE.equals(slotConfig.get("visible_to_customer"))) {
            field.put("x-visible-to-customer", false);
        }
        else {
            field.put("x-visible-to-customer", true);
        }
        String placeholder = slotConfig.get("placeholder") == null ? null : String.valueOf(slotConfig.get("placeholder"));
        switch (attribute.getFieldType()) {
            case "input" -> buildInputField(field, typeConfig, placeholder);
            case "select" -> buildSelectField(field, typeConfig, placeholder);
            case "switch" -> buildSwitchField(field);
            case "date" -> buildDateField(field, typeConfig, placeholder);
            case "member" -> buildMemberField(field, typeConfig, placeholder);
            default -> throw new IllegalArgumentException("不支持的字段类型: " + attribute.getFieldType());
        }
        applyDefault(field, slotConfig, objectMapper);
        return field;
    }

    static Object resolveLiteralDefault(Object defaultValueRaw, ObjectMapper objectMapper) {
        if (defaultValueRaw == null) {
            return null;
        }
        String raw = String.valueOf(defaultValueRaw);
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        try {
            Map<String, Object> envelope = objectMapper.readValue(raw, new TypeReference<LinkedHashMap<String, Object>>() {
            });
            Object mode = envelope.get("mode");
            if ("expression".equals(String.valueOf(mode))) {
                return null;
            }
            if ("literal".equals(String.valueOf(mode)) || envelope.containsKey("value")) {
                return envelope.get("value");
            }
        }
        catch (JsonProcessingException ignored) {
            // legacy plain string
        }
        return raw;
    }

    private static void applyDefault(
            Map<String, Object> field,
            Map<String, Object> slotConfig,
            ObjectMapper objectMapper) {
        Object resolved = resolveLiteralDefault(slotConfig.get("default_value"), objectMapper);
        if (resolved != null) {
            field.put("default", resolved);
        }
    }

    private static void buildInputField(Map<String, Object> field, Map<String, Object> typeConfig, String placeholder) {
        String format = typeConfig.get("format") == null ? "text" : String.valueOf(typeConfig.get("format"));
        boolean multiline = Boolean.TRUE.equals(typeConfig.get("multiline"));
        Map<String, Object> componentProps = new LinkedHashMap<>();
        if (StringUtils.hasText(placeholder)) {
            componentProps.put("placeholder", placeholder);
        }
        if ("integer".equals(format) || "decimal".equals(format)) {
            field.put("type", "number");
            field.put("x-component", "InputNumber");
            if ("decimal".equals(format)) {
                componentProps.put("precision", 2);
            }
            componentProps.put("style", Map.of("width", "100%"));
            String unit = typeConfig.get("unit") == null ? null : String.valueOf(typeConfig.get("unit")).trim();
            if (StringUtils.hasText(unit)) {
                componentProps.put("addonAfter", unit);
            }
            field.put("x-component-props", componentProps);
            return;
        }
        field.put("type", "string");
        field.put("x-component", multiline ? "Input.TextArea" : "Input");
        if (multiline) {
            componentProps.put("rows", 4);
        }
        field.put("x-component-props", componentProps);
        List<Map<String, Object>> validators = new ArrayList<>();
        if ("email".equals(format)) {
            validators.add(Map.of("format", "email", "message", "请输入有效邮箱"));
        }
        else if ("phone".equals(format)) {
            validators.add(Map.of("pattern", "^1\\d{10}$", "message", "请输入有效手机号"));
        }
        if (!validators.isEmpty()) {
            field.put("x-validator", validators);
        }
    }

    private static void buildSelectField(
            Map<String, Object> field,
            Map<String, Object> typeConfig,
            String placeholder) {
        boolean multiple = Boolean.TRUE.equals(typeConfig.get("multiple"));
        field.put("type", multiple ? "array" : "string");
        field.put("x-component", "Select");
        String optionsSource = typeConfig.get("options_source") == null
                ? null
                : String.valueOf(typeConfig.get("options_source")).trim();
        List<Map<String, Object>> options;
        if ("priority_levels".equals(optionsSource)) {
            field.put("x-options-source", "priority_levels");
            options = readOptionsOptional(typeConfig);
            if (options.isEmpty()) {
                options = standardPriorityOptions();
            }
        }
        else {
            options = readOptions(typeConfig);
        }
        field.put("enum", options);
        Map<String, Object> componentProps = new LinkedHashMap<>();
        if (StringUtils.hasText(placeholder)) {
            componentProps.put("placeholder", placeholder);
        }
        if (multiple) {
            componentProps.put("mode", "multiple");
        }
        field.put("x-component-props", componentProps);
    }

    private static void buildMemberField(
            Map<String, Object> field,
            Map<String, Object> typeConfig,
            String placeholder) {
        boolean multiple = Boolean.TRUE.equals(typeConfig.get("multiple"));
        field.put("type", multiple ? "array" : "number");
        field.put("x-component", "MemberPicker");
        String scopeMode = typeConfig.get("scope_mode") == null
                ? "auto"
                : String.valueOf(typeConfig.get("scope_mode")).trim();
        Map<String, Object> componentProps = new LinkedHashMap<>();
        componentProps.put("multiple", multiple);
        componentProps.put("scopeMode", scopeMode);
        if (StringUtils.hasText(placeholder)) {
            componentProps.put("placeholder", placeholder);
        }
        field.put("x-component-props", componentProps);
    }

    static List<Map<String, Object>> standardPriorityOptions() {
        return List.of(
                option("紧急", "urgent", "#f5222d", "urgent"),
                option("高", "high", "#fa8c16", "high"),
                option("中", "normal", "#1677ff", "normal"),
                option("低", "low", "#8c8c8c", "low"));
    }

    private static Map<String, Object> option(String label, String value, String color, String icon) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("label", label);
        item.put("value", value);
        item.put("color", color);
        item.put("icon", icon);
        return item;
    }

    private static void buildSwitchField(Map<String, Object> field) {
        field.put("type", "boolean");
        field.put("x-component", "Switch");
    }

    private static void buildDateField(Map<String, Object> field, Map<String, Object> typeConfig, String placeholder) {
        boolean withTime = Boolean.TRUE.equals(typeConfig.get("withTime"));
        field.put("type", "string");
        field.put("x-component", "DatePicker");
        Map<String, Object> componentProps = new LinkedHashMap<>();
        if (StringUtils.hasText(placeholder)) {
            componentProps.put("placeholder", placeholder);
        }
        if (withTime) {
            componentProps.put("showTime", true);
        }
        field.put("x-component-props", componentProps);
    }

    private static List<Map<String, Object>> readOptions(Map<String, Object> typeConfig) {
        List<Map<String, Object>> result = readOptionsOptional(typeConfig);
        if (result.isEmpty()) {
            throw new IllegalArgumentException("选项类至少配置一个选项");
        }
        return result;
    }

    private static List<Map<String, Object>> readOptionsOptional(Map<String, Object> typeConfig) {
        Object optionsObj = typeConfig.get("options");
        if (!(optionsObj instanceof List<?> options) || options.isEmpty()) {
            return List.of();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object item : options) {
            if (!(item instanceof Map<?, ?> option)) {
                throw new IllegalArgumentException("选项格式无效");
            }
            Map<String, Object> normalized = new LinkedHashMap<>();
            option.forEach((key, value) -> normalized.put(String.valueOf(key), value));
            result.add(normalized);
        }
        return result;
    }

    private static Map<String, Object> readJsonMap(String json, ObjectMapper objectMapper) {
        if (!StringUtils.hasText(json)) {
            return new LinkedHashMap<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<LinkedHashMap<String, Object>>() {
            });
        }
        catch (JsonProcessingException ex) {
            throw new IllegalStateException("invalid json payload", ex);
        }
    }

    public static String fieldKey(TicketAttributePo attribute) {
        return "attr_" + attribute.getId();
    }
}
