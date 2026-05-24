package com.uniondesk.auth.core;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class LoginAuditService {

    private final JdbcTemplate jdbcTemplate;
    private final Clock clock;

    public LoginAuditService(JdbcTemplate jdbcTemplate, Clock clock) {
        this.jdbcTemplate = jdbcTemplate;
        this.clock = clock;
    }

    public void record(LoginLogCommand command) {
        jdbcTemplate.update("""
                        INSERT INTO login_log (
                            subject_id, portal_type, client_code, sid, event_type, business_domain_id,
                            login_name, login_identifier_masked, login_identifier_type,
                            ip, user_agent, result, fail_reason, created_at
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                command.subjectId(),
                normalizePortalType(command.portalType()),
                command.clientCode(),
                command.sid(),
                normalizeEventType(command.eventType()),
                command.businessDomainId(),
                command.loginName(),
                command.loginIdentifierMasked(),
                command.loginIdentifierType(),
                command.clientIp(),
                command.userAgent(),
                normalizeResult(command.result()),
                command.failReason(),
                LocalDateTime.now(clock));
    }

    public List<LoginLog> listRecentLogs(int limit, String eventType) {
        int cappedLimit = Math.max(1, Math.min(limit, 500));
        String normalizedEventType = normalizeEventType(eventType);
        return jdbcTemplate.query("""
                        SELECT
                            l.id,
                            l.sid,
                            l.subject_id,
                            l.login_name,
                            l.login_identifier_masked,
                            l.login_identifier_type,
                            l.event_type,
                            l.portal_type,
                            l.client_code,
                            l.business_domain_id,
                            l.result,
                            l.fail_reason,
                            l.ip,
                            l.user_agent,
                            l.created_at
                        FROM login_log l
                        WHERE l.event_type = ?
                        ORDER BY l.created_at DESC, l.id DESC
                        LIMIT ?
                        """,
                (rs, rowNum) -> new LoginLog(
                        rs.getLong("id"),
                        rs.getString("sid"),
                        rs.getObject("subject_id", Long.class),
                        rs.getString("login_name"),
                        rs.getString("login_identifier_masked"),
                        rs.getString("login_identifier_type"),
                        rs.getString("event_type"),
                        rs.getString("portal_type"),
                        rs.getString("client_code"),
                        rs.getObject("business_domain_id", Long.class),
                        rs.getString("result"),
                        rs.getString("fail_reason"),
                        rs.getString("ip"),
                        rs.getString("user_agent"),
                        rs.getTimestamp("created_at").toLocalDateTime()),
                normalizedEventType,
                cappedLimit);
    }

    public Long resolveCustomerSubjectId(long customerAccountId) {
        List<Long> ids = jdbcTemplate.queryForList(
                "SELECT subject_id FROM customer_account WHERE id = ? LIMIT 1",
                Long.class,
                customerAccountId);
        return ids.isEmpty() ? null : ids.get(0);
    }

    public String maskIdentifier(String identifier, String identifierType) {
        if (!StringUtils.hasText(identifier)) {
            return "***";
        }
        String value = identifier.trim();
        if ("MOBILE".equalsIgnoreCase(identifierType)) {
            return value.length() <= 7 ? "***" : value.substring(0, 3) + "****" + value.substring(value.length() - 4);
        }
        if ("EMAIL".equalsIgnoreCase(identifierType)) {
            int atIndex = value.indexOf('@');
            if (atIndex <= 1) {
                return "***" + value.substring(Math.max(atIndex, 0));
            }
            String localPart = value.substring(0, atIndex);
            return localPart.charAt(0) + "***" + localPart.charAt(localPart.length() - 1) + value.substring(atIndex);
        }
        return value.length() <= 2 ? "***" : value.charAt(0) + "***" + value.charAt(value.length() - 1);
    }

    public LoginLogCommand loginFailure(
            String sid,
            Long subjectId,
            String portalType,
            String clientCode,
            Long businessDomainId,
            String loginName,
            String identifierType,
            String identifier,
            String reason,
            String clientIp,
            String userAgent) {
        return new LoginLogCommand(
                sid,
                subjectId,
                portalType,
                clientCode,
                businessDomainId,
                loginName,
                maskIdentifier(identifier, identifierType),
                identifierType,
                "LOGIN",
                "failure",
                reason,
                clientIp,
                userAgent);
    }

    public LoginLogCommand loginSuccess(
            String sid,
            Long subjectId,
            String portalType,
            String clientCode,
            Long businessDomainId,
            String loginName,
            String identifierType,
            String identifier,
            String clientIp,
            String userAgent) {
        return new LoginLogCommand(
                sid,
                subjectId,
                portalType,
                clientCode,
                businessDomainId,
                loginName,
                maskIdentifier(identifier, identifierType),
                identifierType,
                "LOGIN",
                "success",
                null,
                clientIp,
                userAgent);
    }

    public LoginLogCommand sessionEvent(
            String sid,
            Long subjectId,
            String portalType,
            String clientCode,
            Long businessDomainId,
            String loginName,
            String identifierType,
            String identifier,
            String eventType,
            String result,
            String reason,
            String clientIp,
            String userAgent) {
        return new LoginLogCommand(
                sid,
                subjectId,
                portalType,
                clientCode,
                businessDomainId,
                loginName,
                maskIdentifier(identifier, identifierType),
                identifierType,
                eventType,
                result,
                reason,
                clientIp,
                userAgent);
    }

    private String normalizePortalType(String portalType) {
        if (!StringUtils.hasText(portalType)) {
            return "staff";
        }
        String normalized = portalType.trim().toLowerCase(Locale.ROOT);
        return "customer".equals(normalized) ? "customer" : "staff";
    }

    private String normalizeEventType(String eventType) {
        if (!StringUtils.hasText(eventType)) {
            return "LOGIN";
        }
        return eventType.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeResult(String result) {
        if (!StringUtils.hasText(result)) {
            return "failure";
        }
        String normalized = result.trim().toLowerCase(Locale.ROOT);
        if ("success".equals(normalized) || "failure".equals(normalized)) {
            return normalized;
        }
        if ("SUCCESS".equalsIgnoreCase(result)) {
            return "success";
        }
        return "failure";
    }

    public record LoginLogCommand(
            String sid,
            Long subjectId,
            String portalType,
            String clientCode,
            Long businessDomainId,
            String loginName,
            String loginIdentifierMasked,
            String loginIdentifierType,
            String eventType,
            String result,
            String failReason,
            String clientIp,
            String userAgent) {
    }

    public record LoginLog(
            long id,
            String sid,
            Long subjectId,
            String loginName,
            String loginIdentifierMasked,
            String loginIdentifierType,
            String eventType,
            String portalType,
            String clientCode,
            Long businessDomainId,
            String result,
            String failReason,
            String clientIp,
            String userAgent,
            LocalDateTime createdAt) {
    }
}
