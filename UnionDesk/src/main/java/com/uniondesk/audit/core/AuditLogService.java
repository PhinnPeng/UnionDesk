package com.uniondesk.audit.core;

import com.uniondesk.audit.web.AuditDtos;
import com.uniondesk.common.web.PageResult;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional(readOnly = true)
public class AuditLogService {

    private static final int MAX_PAGE_SIZE = 100;

    private final JdbcTemplate jdbcTemplate;

    public AuditLogService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public PageResult<AuditDtos.AuditLogView> listPlatformAuditLogs(
            int page,
            int pageSize,
            Long domainId,
            String operator,
            String action,
            LocalDateTime startTime,
            LocalDateTime endTime) {
        return listPlatformAuditLogs(page, pageSize, domainId, operator, action, startTime, endTime, null, null, null, null, null);
    }

    public PageResult<AuditDtos.AuditLogView> listPlatformAuditLogs(
            int page,
            int pageSize,
            Long domainId,
            String operator,
            String action,
            LocalDateTime startTime,
            LocalDateTime endTime,
            String module,
            String keyword,
            String ip,
            String username,
            String nickname) {
        AuditQuery query = buildAuditQuery(domainId, operator, action, startTime, endTime, module, keyword, ip, username, nickname);
        return new PageResult<>(
                countAuditLogs(query),
                jdbcTemplate.query("""
                                SELECT
                                    a.id,
                                    a.business_domain_id,
                                    a.operator_subject_id,
                                    COALESCE(ua.username, s.phone, 'system') AS operator_name,
                                    a.operator_actor_type,
                                    a.target,
                                    a.action,
                                    CAST(a.detail AS CHAR) AS detail,
                                    a.result,
                                    a.occurred_at,
                                    a.request_id,
                                    als.client_ip AS ip
                                FROM audit_log a
                                LEFT JOIN identity_subject s ON s.id = a.operator_subject_id
                                LEFT JOIN user_account ua ON ua.id = a.operator_subject_id
                                LEFT JOIN auth_login_session als ON als.sid = a.request_id
                                %s
                                ORDER BY a.occurred_at DESC, a.id DESC
                                LIMIT ? OFFSET ?
                                """.formatted(query.whereClause()),
                        this::mapAuditLogView,
                        pagingArgs(query, page, pageSize)));
    }

    public PageResult<AuditDtos.AuditLogView> listDomainAuditLogs(
            long domainId,
            int page,
            int pageSize,
            String operator,
            String action,
            LocalDateTime startTime,
            LocalDateTime endTime) {
        return listPlatformAuditLogs(page, pageSize, domainId, operator, action, startTime, endTime);
    }

    public PageResult<AuditDtos.LoginLogView> listDomainLoginLogs(
            long domainId,
            int page,
            int pageSize,
            String operator,
            String action,
            LocalDateTime startTime,
            LocalDateTime endTime) {
        LoginQuery query = buildDomainLoginQuery(domainId, operator, action, startTime, endTime);
        return new PageResult<>(
                countLoginLogs(query),
                jdbcTemplate.query("""
                                SELECT
                                    l.id,
                                    l.subject_id,
                                    COALESCE(ua.username, l.login_name, 'system') AS operator_name,
                                    l.business_domain_id,
                                    l.login_name,
                                    l.portal_type,
                                    l.ip,
                                    l.user_agent,
                                    l.result,
                                    l.fail_reason,
                                    l.created_at
                                FROM login_log l
                                LEFT JOIN user_account ua ON ua.id = l.subject_id
                                %s
                                ORDER BY l.created_at DESC, l.id DESC
                                LIMIT ? OFFSET ?
                                """.formatted(query.whereClause()),
                        this::mapLoginLogView,
                        pagingArgs(query, page, pageSize)));
    }

    public PageResult<AuditDtos.LoginLogView> listPlatformLoginLogs(
            int page,
            int pageSize,
            Long subjectId,
            String portalType,
            String result,
            LocalDateTime startTime,
            LocalDateTime endTime) {
        return listPlatformLoginLogs(page, pageSize, subjectId, portalType, result, startTime, endTime, null, null, null, null);
    }

    public PageResult<AuditDtos.LoginLogView> listPlatformLoginLogs(
            int page,
            int pageSize,
            Long subjectId,
            String portalType,
            String result,
            LocalDateTime startTime,
            LocalDateTime endTime,
            String keyword,
            String ip,
            String username,
            String nickname) {
        LoginQuery query = buildPlatformLoginQuery(subjectId, portalType, result, startTime, endTime, keyword, ip, username, nickname);
        return new PageResult<>(
                countLoginLogs(query),
                jdbcTemplate.query("""
                                SELECT
                                    l.id,
                                    l.subject_id,
                                    COALESCE(ua.username, l.login_name, 'system') AS operator_name,
                                    l.business_domain_id,
                                    l.login_name,
                                    l.portal_type,
                                    l.ip,
                                    l.user_agent,
                                    l.result,
                                    l.fail_reason,
                                    l.created_at
                                FROM login_log l
                                LEFT JOIN user_account ua ON ua.id = l.subject_id
                                %s
                                ORDER BY l.created_at DESC, l.id DESC
                                LIMIT ? OFFSET ?
                                """.formatted(query.whereClause()),
                        this::mapLoginLogView,
                        pagingArgs(query, page, pageSize)));
    }

    private long countAuditLogs(AuditQuery query) {
        Long total = jdbcTemplate.queryForObject("""
                        SELECT COUNT(*)
                        FROM audit_log a
                        LEFT JOIN identity_subject s ON s.id = a.operator_subject_id
                        LEFT JOIN user_account ua ON ua.id = a.operator_subject_id
                        LEFT JOIN auth_login_session als ON als.sid = a.request_id
                        %s
                        """.formatted(query.whereClause()),
                Long.class,
                query.args().toArray());
        return total == null ? 0L : total;
    }

    private long countLoginLogs(LoginQuery query) {
        Long total = jdbcTemplate.queryForObject("""
                        SELECT COUNT(*)
                        FROM login_log l
                        LEFT JOIN user_account ua ON ua.id = l.subject_id
                        %s
                        """.formatted(query.whereClause()),
                Long.class,
                query.args().toArray());
        return total == null ? 0L : total;
    }

    private AuditQuery buildAuditQuery(
            Long domainId,
            String operator,
            String action,
            LocalDateTime startTime,
            LocalDateTime endTime,
            String module,
            String keyword,
            String ip,
            String username,
            String nickname) {
        List<String> conditions = new ArrayList<>();
        List<Object> args = new ArrayList<>();
        if (domainId != null) {
            conditions.add("a.business_domain_id = ?");
            args.add(domainId);
        }
        if (StringUtils.hasText(operator)) {
            String like = "%" + operator.trim() + "%";
            conditions.add("(ua.username LIKE ? OR s.phone LIKE ? OR a.operator_actor_type LIKE ? OR CAST(a.operator_subject_id AS CHAR) LIKE ?)");
            args.add(like);
            args.add(like);
            args.add(like);
            args.add(like);
        }
        if (StringUtils.hasText(action)) {
            conditions.add("a.action LIKE ?");
            args.add("%" + action.trim() + "%");
        }
        if (StringUtils.hasText(module)) {
            conditions.add("a.action LIKE ?");
            args.add(module.trim() + "%");
        }
        if (StringUtils.hasText(keyword)) {
            String like = "%" + keyword.trim() + "%";
            conditions.add("(ua.username LIKE ? OR ua.mobile LIKE ? OR ua.email LIKE ? OR s.phone LIKE ?)");
            args.add(like);
            args.add(like);
            args.add(like);
            args.add(like);
        }
        if (StringUtils.hasText(ip)) {
            conditions.add("als.client_ip LIKE ?");
            args.add("%" + ip.trim() + "%");
        }
        if (StringUtils.hasText(username)) {
            conditions.add("ua.username LIKE ?");
            args.add("%" + username.trim() + "%");
        }
        if (StringUtils.hasText(nickname)) {
            conditions.add("ua.nickname LIKE ?");
            args.add("%" + nickname.trim() + "%");
        }
        if (startTime != null) {
            conditions.add("a.occurred_at >= ?");
            args.add(startTime);
        }
        if (endTime != null) {
            conditions.add("a.occurred_at <= ?");
            args.add(endTime);
        }
        return new AuditQuery(whereClause(conditions), List.copyOf(args));
    }

    private LoginQuery buildDomainLoginQuery(Long domainId, String operator, String action, LocalDateTime startTime, LocalDateTime endTime) {
        List<String> conditions = new ArrayList<>();
        List<Object> args = new ArrayList<>();
        if (domainId != null) {
            conditions.add("l.business_domain_id = ?");
            args.add(domainId);
        }
        if (StringUtils.hasText(operator)) {
            String like = "%" + operator.trim() + "%";
            conditions.add("(ua.username LIKE ? OR l.login_name LIKE ? OR l.portal_type LIKE ? OR CAST(l.subject_id AS CHAR) LIKE ?)");
            args.add(like);
            args.add(like);
            args.add(like);
            args.add(like);
        }
        if (StringUtils.hasText(action)) {
            conditions.add("l.result LIKE ?");
            args.add("%" + action.trim() + "%");
        }
        if (startTime != null) {
            conditions.add("l.created_at >= ?");
            args.add(startTime);
        }
        if (endTime != null) {
            conditions.add("l.created_at <= ?");
            args.add(endTime);
        }
        return new LoginQuery(whereClause(conditions), List.copyOf(args));
    }

    private LoginQuery buildPlatformLoginQuery(
            Long subjectId,
            String portalType,
            String result,
            LocalDateTime startTime,
            LocalDateTime endTime,
            String keyword,
            String ip,
            String username,
            String nickname) {
        List<String> conditions = new ArrayList<>();
        List<Object> args = new ArrayList<>();
        if (subjectId != null) {
            conditions.add("l.subject_id = ?");
            args.add(subjectId);
        }
        if (StringUtils.hasText(portalType)) {
            conditions.add("l.portal_type = ?");
            args.add(portalType.trim());
        }
        if (StringUtils.hasText(result)) {
            conditions.add("l.result = ?");
            args.add(result.trim());
        }
        if (StringUtils.hasText(keyword)) {
            String like = "%" + keyword.trim() + "%";
            conditions.add("(ua.username LIKE ? OR ua.mobile LIKE ? OR ua.email LIKE ? OR l.login_name LIKE ?)");
            args.add(like);
            args.add(like);
            args.add(like);
            args.add(like);
        }
        if (StringUtils.hasText(ip)) {
            conditions.add("l.ip LIKE ?");
            args.add("%" + ip.trim() + "%");
        }
        if (StringUtils.hasText(username)) {
            conditions.add("ua.username LIKE ?");
            args.add("%" + username.trim() + "%");
        }
        if (StringUtils.hasText(nickname)) {
            conditions.add("ua.nickname LIKE ?");
            args.add("%" + nickname.trim() + "%");
        }
        if (startTime != null) {
            conditions.add("l.created_at >= ?");
            args.add(startTime);
        }
        if (endTime != null) {
            conditions.add("l.created_at <= ?");
            args.add(endTime);
        }
        return new LoginQuery(whereClause(conditions), List.copyOf(args));
    }

    private Object[] pagingArgs(AuditQuery query, int page, int pageSize) {
        int normalizedPage = Math.max(page, 1);
        int normalizedPageSize = normalizePageSize(pageSize);
        List<Object> args = new ArrayList<>(query.args());
        args.add(normalizedPageSize);
        args.add((normalizedPage - 1L) * normalizedPageSize);
        return args.toArray();
    }

    private Object[] pagingArgs(LoginQuery query, int page, int pageSize) {
        int normalizedPage = Math.max(page, 1);
        int normalizedPageSize = normalizePageSize(pageSize);
        List<Object> args = new ArrayList<>(query.args());
        args.add(normalizedPageSize);
        args.add((normalizedPage - 1L) * normalizedPageSize);
        return args.toArray();
    }

    private int normalizePageSize(int pageSize) {
        return Math.min(Math.max(pageSize, 1), MAX_PAGE_SIZE);
    }

    private String whereClause(List<String> conditions) {
        return conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions);
    }

    private AuditDtos.AuditLogView mapAuditLogView(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        return new AuditDtos.AuditLogView(
                rs.getLong("id"),
                rs.getObject("business_domain_id", Long.class),
                rs.getObject("operator_subject_id", Long.class),
                rs.getString("operator_name"),
                rs.getString("operator_actor_type"),
                rs.getString("target"),
                rs.getString("action"),
                rs.getString("detail"),
                rs.getString("result"),
                toLocalDateTime(rs.getTimestamp("occurred_at")),
                rs.getString("request_id"),
                rs.getString("ip"));
    }

    private AuditDtos.LoginLogView mapLoginLogView(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        return new AuditDtos.LoginLogView(
                rs.getLong("id"),
                rs.getObject("subject_id", Long.class),
                rs.getString("operator_name"),
                rs.getObject("business_domain_id", Long.class),
                rs.getString("login_name"),
                rs.getString("portal_type"),
                rs.getString("ip"),
                rs.getString("user_agent"),
                rs.getString("result"),
                rs.getString("fail_reason"),
                toLocalDateTime(rs.getTimestamp("created_at")));
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private record AuditQuery(String whereClause, List<Object> args) {
    }

    private record LoginQuery(String whereClause, List<Object> args) {
    }
}
