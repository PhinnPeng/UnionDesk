package com.uniondesk.notification.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uniondesk.auth.core.UserContext;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class NotificationCenterService {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final boolean smtpEnabled;

    public NotificationCenterService(
            JdbcTemplate jdbcTemplate,
            ObjectMapper objectMapper,
            Clock clock,
            @Value("${uniondesk.notification.smtp-enabled:false}") boolean smtpEnabled) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.clock = clock;
        this.smtpEnabled = smtpEnabled;
    }

    @Transactional
    public NotificationDispatchResult notifyTicketCreated(long businessDomainId, long ticketId, long recipientUserId, long operatorUserId) {
        return sendTicketNotification(
                businessDomainId,
                ticketId,
                recipientUserId,
                operatorUserId,
                "ticket.created",
                "工单已创建",
                "您的工单已提交，客服会尽快处理。",
                "/api/v1/domains/%d/tickets/my/%d".formatted(businessDomainId, ticketId));
    }

    @Transactional
    public NotificationDispatchResult notifyTicketStatusChanged(
            long businessDomainId,
            long ticketId,
            long recipientUserId,
            long operatorUserId,
            String status) {
        return sendTicketNotification(
                businessDomainId,
                ticketId,
                recipientUserId,
                operatorUserId,
                "ticket.status.changed",
                "工单状态更新",
                "您的工单状态已变更为 %s。".formatted(status),
                "/api/v1/domains/%d/tickets/my/%d".formatted(businessDomainId, ticketId));
    }

    @Transactional
    public NotificationDispatchResult notifyTicketReply(
            long businessDomainId,
            long ticketId,
            long recipientUserId,
            long operatorUserId,
            String replyType) {
        return sendTicketNotification(
                businessDomainId,
                ticketId,
                recipientUserId,
                operatorUserId,
                "ticket.reply",
                "工单新回复",
                "您的工单收到一条%s回复。".formatted(replyType == null ? "" : replyType),
                "/api/v1/domains/%d/tickets/my/%d".formatted(businessDomainId, ticketId));
    }

    @Transactional
    public NotificationDispatchResult notifyTicketMerged(
            long businessDomainId,
            long sourceTicketId,
            long recipientUserId,
            long operatorUserId,
            long targetTicketId) {
        return sendTicketNotification(
                businessDomainId,
                sourceTicketId,
                recipientUserId,
                operatorUserId,
                "ticket.merged",
                "工单已合并",
                "您的工单已合并到 #%d。".formatted(targetTicketId),
                "/api/v1/domains/%d/tickets/my/%d".formatted(businessDomainId, sourceTicketId));
    }

    @Transactional
    public long unreadCount(long recipientUserId) {
        Long count = jdbcTemplate.queryForObject("""
                        SELECT COUNT(*)
                        FROM inbox_message
                        WHERE recipient_subject_id = ? AND is_read = 0 AND deleted_at IS NULL
                        """,
                Long.class,
                ensureIdentitySubject(recipientUserId));
        return count == null ? 0L : count;
    }

    @Transactional
    public List<InboxMessageView> listInboxMessages(long recipientUserId, boolean unreadOnly, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 500));
        String sql = """
                SELECT
                    id,
                    notification_log_id,
                    recipient_subject_id,
                    portal_type,
                    business_domain_id,
                    title,
                    content,
                    jump_url,
                    is_read,
                    read_at,
                    created_at,
                    updated_at
                FROM inbox_message
                WHERE recipient_subject_id = ?
                  AND deleted_at IS NULL
                """;
        if (unreadOnly) {
            sql += " AND is_read = 0";
        }
        sql += " ORDER BY created_at DESC, id DESC LIMIT " + safeLimit;
        return jdbcTemplate.query(sql,
                (rs, rowNum) -> new InboxMessageView(
                        rs.getLong("id"),
                        rs.getObject("notification_log_id", Long.class),
                        rs.getLong("recipient_subject_id"),
                        rs.getString("portal_type"),
                        rs.getObject("business_domain_id", Long.class),
                        rs.getString("title"),
                        rs.getString("content"),
                        rs.getString("jump_url"),
                        rs.getInt("is_read") == 1,
                        toLocalDateTime(rs.getTimestamp("read_at")),
                        toLocalDateTime(rs.getTimestamp("created_at")),
                        toLocalDateTime(rs.getTimestamp("updated_at"))),
                ensureIdentitySubject(recipientUserId));
    }

    @Transactional
    public int markRead(long recipientUserId, long inboxMessageId) {
        return jdbcTemplate.update("""
                        UPDATE inbox_message
                        SET is_read = 1, read_at = COALESCE(read_at, CURRENT_TIMESTAMP(3))
                        WHERE id = ? AND recipient_subject_id = ? AND deleted_at IS NULL
                        """,
                inboxMessageId,
                ensureIdentitySubject(recipientUserId));
    }

    @Transactional
    public int markReadBatch(long recipientUserId, List<Long> messageIds) {
        if (messageIds == null || messageIds.isEmpty()) {
            return 0;
        }
        String placeholders = String.join(",", messageIds.stream().map(id -> "?").toList());
        List<Object> args = new java.util.ArrayList<>();
        args.add(ensureIdentitySubject(recipientUserId));
        args.addAll(messageIds);
        return jdbcTemplate.update("""
                        UPDATE inbox_message
                        SET is_read = 1, read_at = COALESCE(read_at, CURRENT_TIMESTAMP(3))
                        WHERE recipient_subject_id = ?
                          AND id IN (%s)
                          AND deleted_at IS NULL
                        """.formatted(placeholders),
                args.toArray());
    }

    private NotificationDispatchResult sendTicketNotification(
            long businessDomainId,
            long sourceId,
            long recipientUserId,
            long operatorUserId,
            String templateCode,
            String title,
            String content,
            String jumpUrl) {
        long recipientSubjectId = ensureIdentitySubject(recipientUserId);
        long operatorSubjectId = ensureIdentitySubject(operatorUserId);
        LocalDateTime now = LocalDateTime.now(clock);
        String payloadJson = serializeJson(Map.of(
                "business_domain_id", businessDomainId,
                "source_id", sourceId,
                "recipient_user_id", recipientUserId,
                "operator_user_id", operatorUserId,
                "template_code", templateCode,
                "title", title,
                "content", content,
                "jump_url", jumpUrl));
        String status = smtpEnabled ? "sent" : "degraded";
        String lastError = smtpEnabled ? null : "smtp disabled";

        jdbcTemplate.update("""
                        INSERT INTO notification_log (
                            business_domain_id, source_type, source_id, channel, recipient_subject_id,
                            portal_type, template_code, payload_json, status, retry_count, last_error, next_retry_at, sent_at
                        )
                        VALUES (?, 'ticket', ?, 'email', ?, 'customer', ?, ?, ?, 0, ?, ?, ?)
                        """,
                businessDomainId,
                sourceId,
                recipientSubjectId,
                templateCode,
                payloadJson,
                status,
                lastError,
                smtpEnabled ? null : now.plusMinutes(10),
                smtpEnabled ? now : null);

        long notificationLogId = loadLatestNotificationLogId(recipientSubjectId, sourceId, templateCode);
        jdbcTemplate.update("""
                        INSERT INTO inbox_message (
                            notification_log_id, recipient_subject_id, portal_type, business_domain_id,
                            title, content, jump_url, is_read, read_at
                        )
                        VALUES (?, ?, 'customer', ?, ?, ?, ?, 0, NULL)
                        """,
                notificationLogId,
                recipientSubjectId,
                businessDomainId,
                title,
                content,
                jumpUrl);

        long inboxMessageId = loadLatestInboxMessageId(recipientSubjectId, notificationLogId);
        recordAudit(
                businessDomainId,
                operatorSubjectId,
                "staff",
                "notification:" + sourceId,
                "notification.send",
                Map.of("template_code", templateCode, "recipient_user_id", recipientUserId, "status", status),
                status);
        return new NotificationDispatchResult(notificationLogId, inboxMessageId, status);
    }

    private long loadLatestNotificationLogId(long recipientSubjectId, long sourceId, String templateCode) {
        Long notificationLogId = jdbcTemplate.queryForObject("""
                        SELECT id
                        FROM notification_log
                        WHERE recipient_subject_id = ? AND source_id = ? AND template_code = ?
                        ORDER BY id DESC
                        LIMIT 1
                        """,
                Long.class,
                recipientSubjectId,
                sourceId,
                templateCode);
        if (notificationLogId == null) {
            throw new IllegalStateException("notification log not found");
        }
        return notificationLogId;
    }

    private long loadLatestInboxMessageId(long recipientSubjectId, long notificationLogId) {
        Long inboxMessageId = jdbcTemplate.queryForObject("""
                        SELECT id
                        FROM inbox_message
                        WHERE recipient_subject_id = ? AND notification_log_id = ?
                        ORDER BY id DESC
                        LIMIT 1
                        """,
                Long.class,
                recipientSubjectId,
                notificationLogId);
        if (inboxMessageId == null) {
            throw new IllegalStateException("inbox message not found");
        }
        return inboxMessageId;
    }

    private long ensureIdentitySubject(long userId) {
        Long subjectId = resolveAccountSubjectId("customer_account", userId);
        if (subjectId != null) {
            return subjectId;
        }
        subjectId = resolveAccountSubjectId("staff_account", userId);
        if (subjectId != null) {
            return subjectId;
        }
        Long identitySubjectId;
        try {
            identitySubjectId = jdbcTemplate.queryForObject("""
                            SELECT id
                            FROM identity_subject
                            WHERE id = ?
                            LIMIT 1
                            """,
                    Long.class,
                    userId);
        } catch (EmptyResultDataAccessException ignored) {
            identitySubjectId = null;
        }
        if (identitySubjectId != null) {
            return identitySubjectId;
        }
        String phone = null;
        try {
            phone = jdbcTemplate.queryForObject("""
                            SELECT COALESCE(NULLIF(mobile, ''), CONCAT('user-', id))
                            FROM user_account
                            WHERE id = ?
                            LIMIT 1
                            """,
                    String.class,
                    userId);
        } catch (EmptyResultDataAccessException ignored) {
            // fall back to a deterministic placeholder phone number
        }
        if (!StringUtils.hasText(phone)) {
            phone = "user-" + userId;
        }
        try {
            jdbcTemplate.update("""
                            INSERT INTO identity_subject (id, subject_type, phone, status)
                            VALUES (?, 'person', ?, 'active')
                            """,
                    userId,
                    phone);
            return userId;
        } catch (DuplicateKeyException ignored) {
            Long existingSubjectId = jdbcTemplate.queryForObject("""
                            SELECT id
                            FROM identity_subject
                            WHERE phone = ?
                            LIMIT 1
                            """,
                    Long.class,
                    phone);
            if (existingSubjectId != null) {
                return existingSubjectId;
            }
            throw ignored;
        }
    }

    private Long resolveAccountSubjectId(String tableName, long accountId) {
        try {
            return jdbcTemplate.queryForObject("""
                            SELECT subject_id
                            FROM %s
                            WHERE id = ?
                            LIMIT 1
                            """.formatted(tableName),
                    Long.class,
                    accountId);
        } catch (EmptyResultDataAccessException ignored) {
            return null;
        }
    }

    private void recordAudit(
            long businessDomainId,
            long operatorSubjectId,
            String actorType,
            String target,
            String action,
            Map<String, Object> detail,
            String result) {
        jdbcTemplate.update("""
                        INSERT INTO audit_log (
                            business_domain_id, operator_subject_id, operator_actor_type, target, action,
                            detail, result, request_id
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                businessDomainId,
                operatorSubjectId,
                actorType,
                target,
                action,
                serializeJson(detail),
                result,
                null);
    }

    private String serializeJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("invalid json payload", ex);
        }
    }

    private LocalDateTime toLocalDateTime(java.sql.Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    public record NotificationDispatchResult(long notificationLogId, long inboxMessageId, String status) {
    }

    public record InboxMessageView(
            long id,
            Long notificationLogId,
            long recipientSubjectId,
            String portalType,
            Long businessDomainId,
            String title,
            String content,
            String jumpUrl,
            boolean read,
            LocalDateTime readAt,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
    }
}
