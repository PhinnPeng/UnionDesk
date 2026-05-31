package com.uniondesk.domain.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uniondesk.auth.core.UserContext;
import com.uniondesk.auth.core.UserContextHolder;
import com.uniondesk.common.web.PageResult;
import com.uniondesk.domain.web.DomainDtos;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class DomainService {

    private static final List<String> DEFAULT_VISIBILITY_POLICY_CODES = List.of("public");

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final DomainBootstrapService domainBootstrapService;

    public DomainService(
            JdbcTemplate jdbcTemplate,
            ObjectMapper objectMapper,
            DomainBootstrapService domainBootstrapService) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.domainBootstrapService = domainBootstrapService;
    }

    public PageResult<DomainDtos.DomainView> listAdminDomains(
            int page,
            int pageSize,
            String status,
            String keyword,
            LocalDateTime createdFrom,
            LocalDateTime createdTo) {
        DomainQuery query = buildDomainQuery(status, keyword, createdFrom, createdTo, false, false);
        return new PageResult<>(
                countDomains(query),
                jdbcTemplate.query("""
                                SELECT
                                    d.id,
                                    d.code,
                                    d.name,
                                    d.description,
                                    d.logo,
                                    d.visibility_policy_codes,
                                    d.registration_enabled,
                                    d.invitation_enabled,
                                    d.status,
                                    d.created_at,
                                    d.updated_at,
                                    d.deleted_at,
                                    d.created_by,
                                    d.updated_by,
                                    uc.username AS creator_name,
                                    uu.username AS updater_name
                                FROM business_domain d
                                LEFT JOIN user_account uc ON uc.id = d.created_by
                                LEFT JOIN user_account uu ON uu.id = d.updated_by
                                %s
                                ORDER BY d.id DESC
                                LIMIT ? OFFSET ?
                                """.formatted(query.whereClause()),
                        this::mapDomainView,
                        pagingArgs(query, page, pageSize)));
    }

    public PageResult<DomainDtos.DomainBriefView> listCustomerDomains(int page, int pageSize, String keyword) {
        DomainQuery query = buildDomainQuery("active", keyword, null, null, false, true);
        return new PageResult<>(
                countDomains(query),
                jdbcTemplate.query("""
                                SELECT
                                    d.id,
                                    d.code,
                                    d.name,
                                    d.logo
                                FROM business_domain d
                                %s
                                ORDER BY d.id DESC
                                LIMIT ? OFFSET ?
                                """.formatted(query.whereClause()),
                        (rs, rowNum) -> new DomainDtos.DomainBriefView(
                                rs.getLong("id"),
                                rs.getString("code"),
                                rs.getString("name"),
                                rs.getString("logo")),
                        pagingArgs(query, page, pageSize)));
    }

    public DomainDtos.DomainView getDomain(long id) {
        try {
            return jdbcTemplate.queryForObject("""
                            SELECT
                                d.id,
                                d.code,
                                d.name,
                                d.description,
                                d.logo,
                                d.visibility_policy_codes,
                                d.registration_enabled,
                                d.invitation_enabled,
                                d.status,
                                d.created_at,
                                d.updated_at,
                                d.deleted_at,
                                d.created_by,
                                d.updated_by,
                                uc.username AS creator_name,
                                uu.username AS updater_name
                            FROM business_domain d
                            LEFT JOIN user_account uc ON uc.id = d.created_by
                            LEFT JOIN user_account uu ON uu.id = d.updated_by
                            WHERE d.id = ?
                              AND d.deleted_at IS NULL
                            LIMIT 1
                            """,
                    this::mapDomainView,
                    id);
        } catch (EmptyResultDataAccessException ex) {
            throw new IllegalArgumentException("business domain not found");
        }
    }

    @Transactional
    public DomainDtos.DomainCreateResponse createDomain(DomainDtos.CreateDomainRequest request) {
        UserContext context = UserContextHolder.requireCurrent();
        List<String> visibilityPolicyCodes = normalizeVisibilityPolicyCodes(request.visibility_policy_codes());
        String registrationEnabled = DomainAccessPolicy.normalize(request.registration_enabled());
        String invitationEnabled = DomainAccessPolicy.normalize(request.invitation_enabled());
        String legacyVisibilityPolicy = visibilityPolicyCodes.isEmpty() ? "public" : visibilityPolicyCodes.getFirst();
        jdbcTemplate.update("""
                        INSERT INTO business_domain (
                            code, name, description, visibility_policy, status, created_at, updated_at,
                            registration_enabled, invitation_enabled, visibility_policy_codes, logo, deleted_at, created_by
                        )
                        VALUES (?, ?, ?, ?, 1, CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3), ?, ?, ?, ?, NULL, ?)
                        """,
                request.code().trim(),
                request.name().trim(),
                normalizeDescription(request.description()),
                legacyVisibilityPolicy,
                registrationEnabled,
                invitationEnabled,
                toJson(visibilityPolicyCodes),
                request.logo(),
                context.userId());

        Long id = jdbcTemplate.queryForObject("SELECT id FROM business_domain WHERE code = ? LIMIT 1", Long.class, request.code().trim());
        if (id == null) {
            throw new IllegalStateException("business domain create failed");
        }

        DomainBootstrapService.BootstrapResult bootstrap = domainBootstrapService.bootstrapNewDomain(id, context.userId());
        Map<String, Object> auditPayload = new HashMap<>();
        auditPayload.put("code", request.code());
        auditPayload.put("name", request.name());
        auditPayload.put("creator_user_id", context.userId());
        auditPayload.put("staff_account_id", bootstrap.staffAccountId());
        auditPayload.put("granted_role", bootstrap.grantedRole());
        recordAudit(id, context, "domain:" + request.code(), "domain.create", auditPayload, "success");

        return new DomainDtos.DomainCreateResponse(id, request.code().trim());
    }

    @Transactional
    public DomainDtos.DomainView updateDomain(long id, DomainDtos.UpdateDomainRequest request) {
        UserContext context = UserContextHolder.requireCurrent();
        DomainDtos.DomainView existing = getDomain(id);
        String code = StringUtils.hasText(request.code()) ? request.code().trim() : existing.code();
        String name = StringUtils.hasText(request.name()) ? request.name().trim() : existing.name();
        String logo = request.logo() == null ? existing.logo() : request.logo();
        String description = request.description() == null ? existing.description() : normalizeDescription(request.description());
        List<String> visibilityPolicyCodes = request.visibility_policy_codes() == null
                ? existing.visibility_policy_codes()
                : normalizeVisibilityPolicyCodes(request.visibility_policy_codes());
        String registrationEnabled = request.registration_enabled() == null
                ? existing.registration_enabled()
                : DomainAccessPolicy.normalize(request.registration_enabled());
        String invitationEnabled = request.invitation_enabled() == null
                ? existing.invitation_enabled()
                : DomainAccessPolicy.normalize(request.invitation_enabled());
        int status = request.status() == null ? existing.status() : request.status();
        String legacyVisibilityPolicy = visibilityPolicyCodes.isEmpty() ? "public" : visibilityPolicyCodes.getFirst();

        jdbcTemplate.update("""
                        UPDATE business_domain
                        SET code = ?, name = ?, description = ?, logo = ?, visibility_policy = ?, visibility_policy_codes = ?,
                            registration_enabled = ?, invitation_enabled = ?, status = ?, updated_at = CURRENT_TIMESTAMP(3), updated_by = ?
                        WHERE id = ?
                          AND deleted_at IS NULL
                        """,
                code,
                name,
                description,
                logo,
                legacyVisibilityPolicy,
                toJson(visibilityPolicyCodes),
                registrationEnabled,
                invitationEnabled,
                status,
                context.userId(),
                id);

        recordAudit(id, context, "domain:" + code, "domain.update",
                Map.of("id", id, "name", name, "status", status), "success");

        return getDomain(id);
    }

    @Transactional
    public void deleteDomain(long id) {
        UserContext context = UserContextHolder.requireCurrent();
        DomainDtos.DomainView existing = getDomain(id);
        int updated = jdbcTemplate.update("""
                        UPDATE business_domain
                        SET deleted_at = CURRENT_TIMESTAMP(3), status = 0, updated_at = CURRENT_TIMESTAMP(3), updated_by = ?
                        WHERE id = ?
                          AND deleted_at IS NULL
                        """,
                context.userId(),
                id);
        if (updated == 0) {
            throw new IllegalArgumentException("business domain not found");
        }

        recordAudit(id, context, "domain:" + existing.code(), "domain.delete",
                Map.of("id", id, "code", existing.code()), "success");
    }

    private void recordAudit(
            long businessDomainId,
            UserContext context,
            String target,
            String action,
            Map<String, Object> detail,
            String result) {
        Long auditDomainId = businessDomainId > 0 ? businessDomainId : null;
        try {
            jdbcTemplate.update("""
                            INSERT INTO audit_log (
                                business_domain_id, operator_subject_id, operator_actor_type, target, action,
                                detail, result, request_id
                            )
                            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                            """,
                    auditDomainId,
                    ensureIdentitySubject(context.userId()),
                    "staff",
                    target,
                    action,
                    toJsonMap(detail),
                    result,
                    context.sessionId());
        } catch (RuntimeException ex) {
            // 审计写入失败不应阻断业务域主流程
        }
    }

    private long ensureIdentitySubject(long userId) {
        try {
            Long existing = jdbcTemplate.queryForObject("""
                            SELECT id
                            FROM identity_subject
                            WHERE id = ?
                            LIMIT 1
                            """,
                    Long.class,
                    userId);
            if (existing != null) {
                return existing;
            }
        } catch (EmptyResultDataAccessException ignored) {
            // create below
        }
        String phone = jdbcTemplate.queryForObject("""
                        SELECT COALESCE(NULLIF(mobile, ''), CONCAT('user-', ?))
                        FROM user_account
                        WHERE id = ?
                        LIMIT 1
                        """,
                String.class,
                userId,
                userId);
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
        } catch (DuplicateKeyException ignored) {
            Long subjectId = jdbcTemplate.queryForObject("""
                            SELECT id
                            FROM identity_subject
                            WHERE phone = ?
                            LIMIT 1
                            """,
                    Long.class,
                    phone);
            if (subjectId != null) {
                return subjectId;
            }
        }
        return userId;
    }

    private String toJsonMap(Map<String, Object> detail) {
        try {
            return objectMapper.writeValueAsString(detail);
        } catch (JsonProcessingException ex) {
            return "{}";
        }
    }

    private long countDomains(DomainQuery query) {
        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM business_domain d %s".formatted(query.whereClause()),
                Long.class,
                query.args().toArray());
        return total == null ? 0L : total;
    }

    private DomainQuery buildDomainQuery(String status, String keyword, LocalDateTime createdFrom, LocalDateTime createdTo, boolean includeDeleted, boolean customerVisibleOnly) {
        List<String> conditions = new ArrayList<>();
        List<Object> args = new ArrayList<>();
        if (!includeDeleted) {
            conditions.add("d.deleted_at IS NULL");
        }
        if (customerVisibleOnly) {
            conditions.add("d.status = 1");
        } else {
            Integer resolvedStatus = resolveStatus(status);
            if (resolvedStatus != null) {
                conditions.add("d.status = ?");
                args.add(resolvedStatus);
            }
        }
        if (StringUtils.hasText(keyword)) {
            conditions.add("(d.code LIKE ? OR d.name LIKE ?)");
            String like = "%" + keyword.trim() + "%";
            args.add(like);
            args.add(like);
        }
        if (createdFrom != null) {
            conditions.add("d.created_at >= ?");
            args.add(createdFrom);
        }
        if (createdTo != null) {
            conditions.add("d.created_at <= ?");
            args.add(createdTo);
        }
        String whereClause = conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions);
        return new DomainQuery(whereClause, List.copyOf(args));
    }

    private Object[] pagingArgs(DomainQuery query, int page, int pageSize) {
        int normalizedPage = Math.max(page, 1);
        int normalizedPageSize = Math.max(pageSize, 1);
        List<Object> args = new ArrayList<>(query.args());
        args.add(normalizedPageSize);
        args.add((normalizedPage - 1L) * normalizedPageSize);
        return args.toArray();
    }

    private DomainDtos.DomainView mapDomainView(ResultSet rs, int rowNum) throws SQLException {
        return new DomainDtos.DomainView(
                rs.getLong("id"),
                rs.getString("code"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getString("logo"),
                readVisibilityPolicyCodes(rs.getString("visibility_policy_codes")),
                DomainAccessPolicy.normalize(rs.getString("registration_enabled")),
                DomainAccessPolicy.normalize(rs.getString("invitation_enabled")),
                rs.getInt("status"),
                toLocalDateTime(rs.getTimestamp("created_at")),
                toLocalDateTime(rs.getTimestamp("updated_at")),
                toLocalDateTime(rs.getTimestamp("deleted_at")),
                rs.getObject("created_by", Long.class),
                rs.getObject("updated_by", Long.class),
                rs.getString("creator_name"),
                rs.getString("updater_name"));
    }

    private List<String> readVisibilityPolicyCodes(String json) {
        if (!StringUtils.hasText(json)) {
            return DEFAULT_VISIBILITY_POLICY_CODES;
        }
        String trimmed = json.trim();
        try {
            if (trimmed.startsWith("[")) {
                List<String> values = objectMapper.readValue(trimmed, new TypeReference<List<String>>() {
                });
                return values == null || values.isEmpty() ? DEFAULT_VISIBILITY_POLICY_CODES : List.copyOf(values);
            }
            if (trimmed.startsWith("\"")) {
                String single = objectMapper.readValue(trimmed, String.class);
                return normalizeVisibilityPolicyCodes(List.of(single));
            }
            return normalizeVisibilityPolicyCodes(List.of(trimmed));
        } catch (JsonProcessingException ex) {
            return normalizeVisibilityPolicyCodes(List.of(trimmed.replace("\"", "")));
        }
    }

    private String toJson(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values == null || values.isEmpty() ? DEFAULT_VISIBILITY_POLICY_CODES : values);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("failed to serialize visibility_policy_codes", ex);
        }
    }

    private List<String> normalizeVisibilityPolicyCodes(List<String> values) {
        List<String> normalized = values == null ? List.of() : values.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .toList();
        return normalized.isEmpty() ? DEFAULT_VISIBILITY_POLICY_CODES : normalized;
    }

    private String normalizeDescription(String description) {
        if (!StringUtils.hasText(description)) {
            return null;
        }
        String trimmed = description.trim();
        return trimmed.length() > 512 ? trimmed.substring(0, 512) : trimmed;
    }

    private Integer resolveStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return null;
        }
        String normalized = status.trim().toLowerCase();
        if ("active".equals(normalized) || "enabled".equals(normalized) || "1".equals(normalized)) {
            return 1;
        }
        if ("disabled".equals(normalized) || "inactive".equals(normalized) || "0".equals(normalized)) {
            return 0;
        }
        if ("deleted".equals(normalized)) {
            return 0;
        }
        return null;
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private record DomainQuery(String whereClause, List<Object> args) {
    }
}
