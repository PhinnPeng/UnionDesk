package com.uniondesk.notification.core;

import com.uniondesk.common.web.PageResult;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class NotificationTemplateService {

    private final JdbcTemplate jdbcTemplate;

    public NotificationTemplateService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(readOnly = true)
    public PageResult<NotificationTemplateView> list(long domainId, int page, int pageSize) {
        int normalizedPage = Math.max(page, 1);
        int normalizedPageSize = normalizePageSize(pageSize);
        long offset = (normalizedPage - 1L) * normalizedPageSize;
        long total = countByDomain(domainId);
        List<NotificationTemplateView> items = jdbcTemplate.query("""
                        SELECT
                            id,
                            scope_type,
                            scope_id,
                            event_category,
                            channel,
                            code,
                            title_template,
                            content_template,
                            is_security,
                            is_unsubscribable,
                            status,
                            created_at,
                            updated_at
                        FROM notification_template
                        WHERE scope_type = 'domain' AND scope_id = ?
                        ORDER BY id DESC
                        LIMIT ? OFFSET ?
                        """,
                this::mapNotificationTemplateView,
                domainId,
                normalizedPageSize,
                offset);
        return new PageResult<>(total, items);
    }

    @Transactional
    public NotificationTemplateView update(long domainId, long templateId, UpdateNotificationTemplateCommand command) {
        jdbcTemplate.update("""
                        UPDATE notification_template
                        SET event_category = ?,
                            channel = ?,
                            code = ?,
                            title_template = ?,
                            content_template = ?,
                            is_security = ?,
                            is_unsubscribable = ?,
                            status = ?,
                            updated_at = CURRENT_TIMESTAMP(3)
                        WHERE id = ? AND scope_type = 'domain' AND scope_id = ?
                        """,
                normalizeText(command.eventCategory(), "notification"),
                normalizeText(command.channel(), "email"),
                normalizeText(command.code(), "template"),
                normalizeText(command.titleTemplate(), ""),
                normalizeText(command.contentTemplate(), ""),
                command.isSecurity() != null && command.isSecurity(),
                command.isUnsubscribable() != null && command.isUnsubscribable(),
                normalizeText(command.status(), "active"),
                templateId,
                domainId);
        return load(domainId, templateId);
    }

    @Transactional(readOnly = true)
    public NotificationTemplateView load(long domainId, long templateId) {
        return jdbcTemplate.queryForObject("""
                        SELECT
                            id,
                            scope_type,
                            scope_id,
                            event_category,
                            channel,
                            code,
                            title_template,
                            content_template,
                            is_security,
                            is_unsubscribable,
                            status,
                            created_at,
                            updated_at
                        FROM notification_template
                        WHERE id = ? AND scope_type = 'domain' AND scope_id = ?
                        LIMIT 1
                        """,
                this::mapNotificationTemplateView,
                templateId,
                domainId);
    }

    private long countByDomain(long domainId) {
        Long total = jdbcTemplate.queryForObject("""
                        SELECT COUNT(*)
                        FROM notification_template
                        WHERE scope_type = 'domain' AND scope_id = ?
                        """,
                Long.class,
                domainId);
        return total == null ? 0L : total;
    }

    private NotificationTemplateView mapNotificationTemplateView(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        return new NotificationTemplateView(
                rs.getLong("id"),
                rs.getString("scope_type"),
                rs.getLong("scope_id"),
                rs.getString("event_category"),
                rs.getString("channel"),
                rs.getString("code"),
                rs.getString("title_template"),
                rs.getString("content_template"),
                rs.getInt("is_security") == 1,
                rs.getInt("is_unsubscribable") == 1,
                rs.getString("status"),
                rs.getTimestamp("created_at").toLocalDateTime(),
                rs.getTimestamp("updated_at").toLocalDateTime());
    }

    private int normalizePageSize(int pageSize) {
        return Math.min(Math.max(pageSize, 1), 100);
    }

    private String normalizeText(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value.trim() : defaultValue;
    }

    public record NotificationTemplateView(
            long id,
            String scopeType,
            long scopeId,
            String eventCategory,
            String channel,
            String code,
            String titleTemplate,
            String contentTemplate,
            boolean isSecurity,
            boolean isUnsubscribable,
            String status,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
    }

    public record UpdateNotificationTemplateCommand(
            String eventCategory,
            String channel,
            String code,
            String titleTemplate,
            String contentTemplate,
            Boolean isSecurity,
            Boolean isUnsubscribable,
            String status) {
    }
}
