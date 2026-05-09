package com.uniondesk.ticket.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uniondesk.ticket.web.TicketConfigDtos;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class TicketConfigService {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public TicketConfigService(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public List<TicketConfigDtos.TicketTypeView> listTicketTypes(long domainId) {
        return jdbcTemplate.query("""
                        SELECT id, business_domain_id, code, name, status_flow_config
                        FROM ticket_type
                        WHERE business_domain_id = ?
                        ORDER BY id ASC
                        """,
                this::mapTicketTypeView,
                domainId);
    }

    @Transactional
    public TicketConfigDtos.TicketTypeView createTicketType(long domainId, TicketConfigDtos.CreateTicketTypeRequest request) {
        String code = requiredText(request.code(), "code");
        String name = requiredText(request.name(), "name");
        Object dynamicFields = request.dynamic_fields();
        jdbcTemplate.update("""
                        INSERT INTO ticket_type (
                            business_domain_id, code, name, status_flow_config, created_at, updated_at
                        )
                        VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3))
                        """,
                domainId,
                code,
                name,
                toJson(dynamicFields));
        long id = lastInsertId();
        return new TicketConfigDtos.TicketTypeView(String.valueOf(id), String.valueOf(domainId), code, name, dynamicFields, "active");
    }

    @Transactional
    public TicketConfigDtos.TicketTypeView updateTicketType(long domainId, long typeId, TicketConfigDtos.UpdateTicketTypeRequest request) {
        TicketTypeRow existing = loadTicketType(domainId, typeId);
        String name = StringUtils.hasText(request.name()) ? request.name().trim() : existing.name();
        Object dynamicFields = request.dynamic_fields() == null ? existing.dynamicFields() : request.dynamic_fields();
        jdbcTemplate.update("""
                        UPDATE ticket_type
                        SET name = ?, status_flow_config = ?, updated_at = CURRENT_TIMESTAMP(3)
                        WHERE id = ? AND business_domain_id = ?
                        """,
                name,
                toJson(dynamicFields),
                typeId,
                domainId);
        return new TicketConfigDtos.TicketTypeView(existing.id(), existing.domainId(), existing.code(), name, dynamicFields, existing.status());
    }

    @Transactional
    public void deleteTicketType(long domainId, long typeId) {
        int updated = jdbcTemplate.update("""
                        DELETE FROM ticket_type
                        WHERE id = ? AND business_domain_id = ?
                        """,
                typeId,
                domainId);
        if (updated == 0) {
            throw new IllegalArgumentException("ticket type not found");
        }
    }

    public List<TicketConfigDtos.TicketTemplateView> listTicketTemplates(long domainId) {
        return jdbcTemplate.query("""
                        SELECT id, business_domain_id, ticket_type_id, scope, name, content_json, sort_order
                        FROM ticket_template
                        WHERE business_domain_id = ?
                        ORDER BY sort_order ASC, id ASC
                        """,
                this::mapTicketTemplateView,
                domainId);
    }

    @Transactional
    public TicketConfigDtos.TicketTemplateView createTicketTemplate(long domainId, TicketConfigDtos.CreateTicketTemplateRequest request) {
        String name = requiredText(request.name(), "name");
        String scope = normalizeTemplateType(request.type());
        long ticketTypeId = request.type_id() == null ? findDefaultTicketTypeId(domainId) : parseLong(request.type_id(), "type_id");
        int sortOrder = request.sort_order() == null ? 0 : request.sort_order();
        Map<String, Object> contentJson = buildTicketTemplateContent(request.fields_snapshot(), request.content());
        jdbcTemplate.update("""
                        INSERT INTO ticket_template (
                            business_domain_id, ticket_type_id, scope, name, content_json, sort_order, status, created_at, updated_at
                        )
                        VALUES (?, ?, ?, ?, ?, ?, 'active', CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3))
                        """,
                domainId,
                ticketTypeId,
                scope,
                name,
                toJson(contentJson),
                sortOrder);
        long id = lastInsertId();
        return new TicketConfigDtos.TicketTemplateView(
                String.valueOf(id),
                String.valueOf(domainId),
                name,
                toResponseTemplateType(scope),
                String.valueOf(ticketTypeId),
                request.fields_snapshot(),
                request.content(),
                sortOrder);
    }

    @Transactional
    public TicketConfigDtos.TicketTemplateView updateTicketTemplate(long domainId, long templateId, TicketConfigDtos.UpdateTicketTemplateRequest request) {
        TicketTemplateRow existing = loadTicketTemplate(domainId, templateId);
        String name = StringUtils.hasText(request.name()) ? request.name().trim() : existing.name();
        String scope = StringUtils.hasText(request.type()) ? normalizeTemplateType(request.type()) : existing.scope();
        long ticketTypeId = request.type_id() == null ? existing.ticketTypeId() : parseLong(request.type_id(), "type_id");
        TicketTemplateContent existingContent = readTicketTemplateContent(existing.contentJson());
        Object fieldsSnapshot = request.fields_snapshot() == null ? existingContent.fieldsSnapshot() : request.fields_snapshot();
        String content = request.content() == null ? existingContent.content() : request.content();
        int sortOrder = request.sort_order() == null ? existing.sortOrder() : request.sort_order();
        jdbcTemplate.update("""
                        UPDATE ticket_template
                        SET ticket_type_id = ?, scope = ?, name = ?, content_json = ?, sort_order = ?, updated_at = CURRENT_TIMESTAMP(3)
                        WHERE id = ? AND business_domain_id = ?
                        """,
                ticketTypeId,
                scope,
                name,
                toJson(buildTicketTemplateContent(fieldsSnapshot, content)),
                sortOrder,
                templateId,
                domainId);
        return new TicketConfigDtos.TicketTemplateView(
                existing.id(),
                existing.domainId(),
                name,
                toResponseTemplateType(scope),
                String.valueOf(ticketTypeId),
                fieldsSnapshot,
                content,
                sortOrder);
    }

    @Transactional
    public void deleteTicketTemplate(long domainId, long templateId) {
        int updated = jdbcTemplate.update("""
                        DELETE FROM ticket_template
                        WHERE id = ? AND business_domain_id = ?
                        """,
                templateId,
                domainId);
        if (updated == 0) {
            throw new IllegalArgumentException("ticket template not found");
        }
    }

    public List<TicketConfigDtos.QuickReplyView> listQuickReplies(long domainId) {
        return jdbcTemplate.query("""
                        SELECT id, business_domain_id, title, content, scope_type, sort_order, created_at
                        FROM quick_reply_template
                        WHERE business_domain_id = ?
                        ORDER BY sort_order ASC, id ASC
                        """,
                this::mapQuickReplyView,
                domainId);
    }

    @Transactional
    public TicketConfigDtos.QuickReplyView createQuickReply(long domainId, TicketConfigDtos.CreateQuickReplyRequest request) {
        String title = requiredText(request.title(), "title");
        String content = requiredText(request.content(), "content");
        String scope = normalizeQuickReplyScope(request.scope());
        int sortOrder = request.sort_order() == null ? 0 : request.sort_order();
        jdbcTemplate.update("""
                        INSERT INTO quick_reply_template (
                            business_domain_id, scope_type, title, content, category, status, sort_order, created_by, created_at, updated_at
                        )
                        VALUES (?, ?, ?, ?, NULL, 'active', ?, NULL, CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3))
                        """,
                domainId,
                scope,
                title,
                content,
                sortOrder);
        long id = lastInsertId();
        return loadQuickReply(domainId, id);
    }

    @Transactional
    public TicketConfigDtos.QuickReplyView updateQuickReply(long domainId, long replyId, TicketConfigDtos.UpdateQuickReplyRequest request) {
        QuickReplyRow existing = loadQuickReplyRow(domainId, replyId);
        String title = StringUtils.hasText(request.title()) ? request.title().trim() : existing.title();
        String content = StringUtils.hasText(request.content()) ? request.content().trim() : existing.content();
        String scope = StringUtils.hasText(request.scope()) ? normalizeQuickReplyScope(request.scope()) : existing.scope();
        int sortOrder = request.sort_order() == null ? existing.sortOrder() : request.sort_order();
        jdbcTemplate.update("""
                        UPDATE quick_reply_template
                        SET scope_type = ?, title = ?, content = ?, sort_order = ?, updated_at = CURRENT_TIMESTAMP(3)
                        WHERE id = ? AND business_domain_id = ?
                        """,
                scope,
                title,
                content,
                sortOrder,
                replyId,
                domainId);
        return new TicketConfigDtos.QuickReplyView(
                existing.id(),
                existing.domainId(),
                title,
                content,
                scope,
                sortOrder,
                toDateTimeString(existing.createdAt()));
    }

    @Transactional
    public void deleteQuickReply(long domainId, long replyId) {
        int updated = jdbcTemplate.update("""
                        DELETE FROM quick_reply_template
                        WHERE id = ? AND business_domain_id = ?
                        """,
                replyId,
                domainId);
        if (updated == 0) {
            throw new IllegalArgumentException("quick reply not found");
        }
    }

    public List<TicketConfigDtos.PriorityLevelView> listPriorityLevels(long domainId) {
        return jdbcTemplate.query("""
                        SELECT id, business_domain_id, name, sort_order, is_default
                        FROM ticket_priority_level
                        WHERE business_domain_id = ?
                        ORDER BY sort_order ASC, id ASC
                        """,
                this::mapPriorityLevelView,
                domainId);
    }

    @Transactional
    public TicketConfigDtos.PriorityLevelView createPriorityLevel(long domainId, TicketConfigDtos.CreatePriorityLevelRequest request) {
        String label = resolvePriorityLabel(request.name(), request.display_label());
        int sortOrder = request.sort_order() == null ? 0 : request.sort_order();
        boolean isDefault = Boolean.TRUE.equals(request.is_default());
        if (isDefault) {
            clearPriorityDefaults(domainId, null);
        }
        jdbcTemplate.update("""
                        INSERT INTO ticket_priority_level (
                            business_domain_id, code, name, sort_order, is_default, status, created_at, updated_at
                        )
                        VALUES (?, ?, ?, ?, ?, 'active', CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3))
                        """,
                domainId,
                label,
                label,
                sortOrder,
                isDefault ? 1 : 0);
        long id = lastInsertId();
        return new TicketConfigDtos.PriorityLevelView(String.valueOf(id), String.valueOf(domainId), label, label, request.color(), sortOrder, isDefault);
    }

    @Transactional
    public TicketConfigDtos.PriorityLevelView updatePriorityLevel(long domainId, long levelId, TicketConfigDtos.UpdatePriorityLevelRequest request) {
        PriorityLevelRow existing = loadPriorityLevel(domainId, levelId);
        String label = StringUtils.hasText(request.display_label())
                ? request.display_label().trim()
                : (StringUtils.hasText(request.name()) ? request.name().trim() : existing.name());
        int sortOrder = request.sort_order() == null ? existing.sortOrder() : request.sort_order();
        boolean isDefault = request.is_default() == null ? existing.isDefault() : request.is_default();
        if (isDefault) {
            clearPriorityDefaults(domainId, levelId);
        }
        jdbcTemplate.update("""
                        UPDATE ticket_priority_level
                        SET code = ?, name = ?, sort_order = ?, is_default = ?, updated_at = CURRENT_TIMESTAMP(3)
                        WHERE id = ? AND business_domain_id = ?
                        """,
                label,
                label,
                sortOrder,
                isDefault ? 1 : 0,
                levelId,
                domainId);
        return new TicketConfigDtos.PriorityLevelView(existing.id(), existing.domainId(), label, label, request.color(), sortOrder, isDefault);
    }

    @Transactional
    public void deletePriorityLevel(long domainId, long levelId) {
        int updated = jdbcTemplate.update("""
                        DELETE FROM ticket_priority_level
                        WHERE id = ? AND business_domain_id = ?
                        """,
                levelId,
                domainId);
        if (updated == 0) {
            throw new IllegalArgumentException("priority level not found");
        }
    }

    private TicketConfigDtos.TicketTypeView mapTicketTypeView(ResultSet rs, int rowNum) throws SQLException {
        return new TicketConfigDtos.TicketTypeView(
                String.valueOf(rs.getLong("id")),
                String.valueOf(rs.getLong("business_domain_id")),
                rs.getString("code"),
                rs.getString("name"),
                readJsonObject(rs.getString("status_flow_config")),
                "active");
    }

    private TicketTypeRow loadTicketType(long domainId, long typeId) {
        try {
            return jdbcTemplate.queryForObject("""
                            SELECT id, business_domain_id, code, name, status_flow_config
                            FROM ticket_type
                            WHERE id = ? AND business_domain_id = ?
                            LIMIT 1
                            """,
                    (rs, rowNum) -> new TicketTypeRow(
                            String.valueOf(rs.getLong("id")),
                            String.valueOf(rs.getLong("business_domain_id")),
                            rs.getString("code"),
                            rs.getString("name"),
                            readJsonObject(rs.getString("status_flow_config")),
                            "active"),
                    typeId,
                    domainId);
        } catch (EmptyResultDataAccessException ex) {
            throw new IllegalArgumentException("ticket type not found");
        }
    }

    private TicketConfigDtos.TicketTemplateView mapTicketTemplateView(ResultSet rs, int rowNum) throws SQLException {
        TicketTemplateContent content = readTicketTemplateContent(rs.getString("content_json"));
        return new TicketConfigDtos.TicketTemplateView(
                String.valueOf(rs.getLong("id")),
                String.valueOf(rs.getLong("business_domain_id")),
                rs.getString("name"),
                toResponseTemplateType(rs.getString("scope")),
                String.valueOf(rs.getLong("ticket_type_id")),
                content.fieldsSnapshot(),
                content.content(),
                rs.getInt("sort_order"));
    }

    private TicketTemplateRow loadTicketTemplate(long domainId, long templateId) {
        try {
            return jdbcTemplate.queryForObject("""
                            SELECT id, business_domain_id, ticket_type_id, scope, name, content_json, sort_order
                            FROM ticket_template
                            WHERE id = ? AND business_domain_id = ?
                            LIMIT 1
                            """,
                    (rs, rowNum) -> new TicketTemplateRow(
                            String.valueOf(rs.getLong("id")),
                            String.valueOf(rs.getLong("business_domain_id")),
                            rs.getLong("ticket_type_id"),
                            rs.getString("scope"),
                            rs.getString("name"),
                            rs.getString("content_json"),
                            rs.getInt("sort_order")),
                    templateId,
                    domainId);
        } catch (EmptyResultDataAccessException ex) {
            throw new IllegalArgumentException("ticket template not found");
        }
    }

    private TicketConfigDtos.QuickReplyView mapQuickReplyView(ResultSet rs, int rowNum) throws SQLException {
        return new TicketConfigDtos.QuickReplyView(
                String.valueOf(rs.getLong("id")),
                String.valueOf(rs.getLong("business_domain_id")),
                rs.getString("title"),
                rs.getString("content"),
                rs.getString("scope_type"),
                rs.getObject("sort_order") == null ? null : rs.getInt("sort_order"),
                toDateTimeString(rs.getTimestamp("created_at")));
    }

    private TicketConfigDtos.QuickReplyView loadQuickReply(long domainId, long replyId) {
        try {
            return jdbcTemplate.queryForObject("""
                            SELECT id, business_domain_id, title, content, scope_type, sort_order, created_at
                            FROM quick_reply_template
                            WHERE id = ? AND business_domain_id = ?
                            LIMIT 1
                            """,
                    this::mapQuickReplyView,
                    replyId,
                    domainId);
        } catch (EmptyResultDataAccessException ex) {
            throw new IllegalArgumentException("quick reply not found");
        }
    }

    private QuickReplyRow loadQuickReplyRow(long domainId, long replyId) {
        try {
            return jdbcTemplate.queryForObject("""
                            SELECT id, business_domain_id, title, content, scope_type, sort_order, created_at
                            FROM quick_reply_template
                            WHERE id = ? AND business_domain_id = ?
                            LIMIT 1
                            """,
                    (rs, rowNum) -> new QuickReplyRow(
                            String.valueOf(rs.getLong("id")),
                            String.valueOf(rs.getLong("business_domain_id")),
                            rs.getString("title"),
                            rs.getString("content"),
                            rs.getString("scope_type"),
                            rs.getInt("sort_order"),
                            rs.getTimestamp("created_at")),
                    replyId,
                    domainId);
        } catch (EmptyResultDataAccessException ex) {
            throw new IllegalArgumentException("quick reply not found");
        }
    }

    private TicketConfigDtos.PriorityLevelView mapPriorityLevelView(ResultSet rs, int rowNum) throws SQLException {
        String label = rs.getString("name");
        return new TicketConfigDtos.PriorityLevelView(
                String.valueOf(rs.getLong("id")),
                String.valueOf(rs.getLong("business_domain_id")),
                label,
                label,
                null,
                rs.getObject("sort_order") == null ? null : rs.getInt("sort_order"),
                rs.getInt("is_default") == 1);
    }

    private PriorityLevelRow loadPriorityLevel(long domainId, long levelId) {
        try {
            return jdbcTemplate.queryForObject("""
                            SELECT id, business_domain_id, name, sort_order, is_default, status
                            FROM ticket_priority_level
                            WHERE id = ? AND business_domain_id = ?
                            LIMIT 1
                            """,
                    (rs, rowNum) -> new PriorityLevelRow(
                            String.valueOf(rs.getLong("id")),
                            String.valueOf(rs.getLong("business_domain_id")),
                            rs.getString("name"),
                            rs.getInt("sort_order"),
                            rs.getInt("is_default") == 1,
                            rs.getString("status")),
                    levelId,
                    domainId);
        } catch (EmptyResultDataAccessException ex) {
            throw new IllegalArgumentException("priority level not found");
        }
    }

    private void clearPriorityDefaults(long domainId, Long keepLevelId) {
        if (keepLevelId == null) {
            jdbcTemplate.update("""
                            UPDATE ticket_priority_level
                            SET is_default = 0, updated_at = CURRENT_TIMESTAMP(3)
                            WHERE business_domain_id = ?
                            """,
                    domainId);
            return;
        }
        jdbcTemplate.update("""
                        UPDATE ticket_priority_level
                        SET is_default = 0, updated_at = CURRENT_TIMESTAMP(3)
                        WHERE business_domain_id = ? AND id <> ?
                        """,
                domainId,
                keepLevelId);
    }

    private long findDefaultTicketTypeId(long domainId) {
        Long id = jdbcTemplate.queryForObject("""
                        SELECT id
                        FROM ticket_type
                        WHERE business_domain_id = ?
                        ORDER BY id ASC
                        LIMIT 1
                        """,
                Long.class,
                domainId);
        if (id == null) {
            throw new IllegalStateException("ticket type required");
        }
        return id;
    }

    private Map<String, Object> buildTicketTemplateContent(Object fieldsSnapshot, String content) {
        Map<String, Object> payload = new LinkedHashMap<>();
        if (fieldsSnapshot != null) {
            payload.put("fields_snapshot", fieldsSnapshot);
        }
        if (content != null) {
            payload.put("content", content);
        }
        return payload;
    }

    private TicketTemplateContent readTicketTemplateContent(String contentJson) {
        if (!StringUtils.hasText(contentJson)) {
            return new TicketTemplateContent(null, null);
        }
        try {
            Map<String, Object> content = objectMapper.readValue(contentJson, new TypeReference<Map<String, Object>>() {
            });
            return new TicketTemplateContent(content.get("fields_snapshot"), content.get("content") == null ? null : String.valueOf(content.get("content")));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("invalid ticket template content", ex);
        }
    }

    private Object readJsonObject(String json) {
        if (!StringUtils.hasText(json)) {
            return null;
        }
        try {
            return objectMapper.readValue(json, Object.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("invalid json payload", ex);
        }
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("failed to serialize json payload", ex);
        }
    }

    private String normalizeTemplateType(String type) {
        if (!StringUtils.hasText(type)) {
            return "internal";
        }
        String normalized = type.trim().toLowerCase();
        if ("customer_content".equals(normalized) || "customer".equals(normalized)) {
            return "customer";
        }
        return "internal";
    }

    private String toResponseTemplateType(String scope) {
        return "customer".equalsIgnoreCase(scope) ? "customer_content" : "internal";
    }

    private String normalizeQuickReplyScope(String scope) {
        if (!StringUtils.hasText(scope)) {
            return "ticket";
        }
        String normalized = scope.trim().toLowerCase();
        return switch (normalized) {
            case "ticket", "consultation", "all" -> normalized;
            default -> "ticket";
        };
    }

    private String resolvePriorityLabel(String name, String displayLabel) {
        if (StringUtils.hasText(displayLabel)) {
            return displayLabel.trim();
        }
        if (StringUtils.hasText(name)) {
            return name.trim();
        }
        throw new IllegalArgumentException("priority label required");
    }

    private String requiredText(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    private long parseLong(String value, String fieldName) {
        try {
            return Long.parseLong(requiredText(value, fieldName));
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(fieldName + " must be a number", ex);
        }
    }

    private long lastInsertId() {
        Long id = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        if (id == null) {
            throw new IllegalStateException("failed to resolve generated id");
        }
        return id;
    }

    private String toDateTimeString(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime().toString();
    }

    private String toDateTimeString(LocalDateTime value) {
        return value == null ? null : value.toString();
    }

    private record TicketTypeRow(String id, String domainId, String code, String name, Object dynamicFields, String status) {
    }

    private record TicketTemplateRow(String id, String domainId, long ticketTypeId, String scope, String name, String contentJson, int sortOrder) {
    }

    private record TicketTemplateContent(Object fieldsSnapshot, String content) {
    }

    private record QuickReplyRow(String id, String domainId, String title, String content, String scope, int sortOrder, Timestamp createdAt) {
    }

    private record PriorityLevelRow(String id, String domainId, String name, int sortOrder, boolean isDefault, String status) {
    }
}
