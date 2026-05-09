package com.uniondesk.domain.core;

import com.uniondesk.common.web.PageResult;
import com.uniondesk.domain.web.DomainMemberDtos;
import com.uniondesk.domain.web.DomainRoleDtos;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class DomainMemberService {

    private final JdbcTemplate jdbcTemplate;
    private final DomainService domainService;

    public DomainMemberService(JdbcTemplate jdbcTemplate, DomainService domainService) {
        this.jdbcTemplate = jdbcTemplate;
        this.domainService = domainService;
    }

    public PageResult<DomainMemberDtos.DomainMemberView> listMembers(long domainId, int page, int pageSize, String status, String keyword) {
        requireDomain(domainId);
        MemberQuery query = buildMemberQuery(domainId, status, keyword);
        List<MemberRow> rows = jdbcTemplate.query("""
                        SELECT
                            dm.id,
                            dm.staff_account_id,
                            dm.business_domain_id,
                            dm.status,
                            dm.source,
                            dm.activated_at,
                            dm.disabled_at,
                            dm.deleted_at,
                            sa.login_name,
                            sa.phone,
                            sa.email
                        FROM domain_member dm
                        JOIN staff_account sa ON sa.id = dm.staff_account_id
                        %s
                        ORDER BY dm.id DESC
                        LIMIT ? OFFSET ?
                        """.formatted(query.whereClause()),
                this::mapMemberRow,
                pagingArgs(query, page, pageSize));
        Map<Long, List<DomainRoleDtos.DomainRoleView>> rolesByMemberId = loadRolesByMemberIds(rows.stream()
                .map(MemberRow::id)
                .toList());
        return new PageResult<>(
                countMembers(query),
                rows.stream().map(row -> toView(row, rolesByMemberId.getOrDefault(row.id(), List.of()))).toList());
    }

    @Transactional
    public DomainMemberDtos.DomainMemberView createMember(long domainId, DomainMemberDtos.CreateDomainMemberRequest request) {
        requireDomain(domainId);
        long staffAccountId = Objects.requireNonNull(request.staff_account_id(), "staff_account_id");
        requireStaffAccount(staffAccountId);
        if (memberExists(domainId, staffAccountId)) {
            throw new IllegalArgumentException("domain member already exists");
        }
        jdbcTemplate.update("""
                        INSERT INTO domain_member (
                            staff_account_id,
                            business_domain_id,
                            status,
                            source,
                            activated_at,
                            disabled_at,
                            deleted_at,
                            created_at,
                            updated_at
                        )
                        VALUES (?, ?, 'active', 'manual', CURRENT_TIMESTAMP(3), NULL, NULL, CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3))
                        """,
                staffAccountId,
                domainId);
        long memberId = requireMemberId(domainId, staffAccountId);
        replaceMemberRoles(domainId, memberId, request.role_ids());
        return getMember(domainId, memberId);
    }

    @Transactional
    public DomainMemberDtos.DomainMemberView updateMemberRoles(long domainId, long memberId, DomainMemberDtos.UpdateDomainMemberRolesRequest request) {
        loadMember(domainId, memberId);
        List<String> currentRoleCodes = loadMemberRoleCodes(memberId);
        List<Long> newRoleIds = normalizeIds(request.role_ids());
        List<String> newRoleCodes = loadRoleCodesByIds(domainId, newRoleIds);
        if (currentRoleCodes.contains("domain_admin") && !newRoleCodes.contains("domain_admin")) {
            guardLastDomainAdmin(domainId, memberId);
        }
        if (currentRoleCodes.contains("super_admin") && !newRoleCodes.contains("super_admin")) {
            guardLastDomainSuperAdmin(domainId, memberId);
        }
        replaceMemberRoles(domainId, memberId, newRoleIds);
        return getMember(domainId, memberId);
    }

    @Transactional
    public void deleteMember(long domainId, long memberId) {
        loadMember(domainId, memberId);
        List<String> currentRoleCodes = loadMemberRoleCodes(memberId);
        if (currentRoleCodes.contains("domain_admin")) {
            guardLastDomainAdmin(domainId, memberId);
        }
        if (currentRoleCodes.contains("super_admin")) {
            guardLastDomainSuperAdmin(domainId, memberId);
        }
        int updated = jdbcTemplate.update("""
                        UPDATE domain_member
                        SET status = 'deleted',
                            deleted_at = CURRENT_TIMESTAMP(3),
                            updated_at = CURRENT_TIMESTAMP(3)
                        WHERE id = ?
                          AND business_domain_id = ?
                          AND deleted_at IS NULL
                        """,
                memberId,
                domainId);
        if (updated == 0) {
            throw new IllegalArgumentException("domain member not found");
        }
    }

    void guardLastDomainAdmin(long domainId, long memberId) {
        if (!loadMemberRoleCodes(memberId).contains("domain_admin")) {
            return;
        }
        Integer otherCount = jdbcTemplate.queryForObject("""
                        SELECT COUNT(DISTINCT dm.id)
                        FROM domain_member dm
                        JOIN domain_member_role dmr ON dmr.domain_member_id = dm.id
                        JOIN domain_role dr ON dr.id = dmr.domain_role_id
                        WHERE dr.business_domain_id = ?
                          AND dr.code = 'domain_admin'
                          AND dm.status = 'active'
                          AND dm.deleted_at IS NULL
                          AND dm.id <> ?
                        """,
                Integer.class,
                domainId,
                memberId);
        if (otherCount == null || otherCount == 0) {
            throw new IllegalStateException("请先指定另一位业务域管理员");
        }
    }

    void guardLastDomainSuperAdmin(long domainId, long memberId) {
        if (!loadMemberRoleCodes(memberId).contains("super_admin")) {
            return;
        }
        Integer otherCount = jdbcTemplate.queryForObject("""
                        SELECT COUNT(DISTINCT dm.id)
                        FROM domain_member dm
                        JOIN domain_member_role dmr ON dmr.domain_member_id = dm.id
                        JOIN domain_role dr ON dr.id = dmr.domain_role_id
                        WHERE dr.business_domain_id = ?
                          AND dr.code = 'super_admin'
                          AND dm.status = 'active'
                          AND dm.deleted_at IS NULL
                          AND dm.id <> ?
                        """,
                Integer.class,
                domainId,
                memberId);
        if (otherCount == null || otherCount == 0) {
            throw new IllegalStateException("请先指定另一位业务域超级管理员");
        }
    }

    private DomainMemberDtos.DomainMemberView getMember(long domainId, long memberId) {
        MemberRow row = loadMember(domainId, memberId);
        List<DomainRoleDtos.DomainRoleView> roles = loadRolesByMemberIds(List.of(memberId)).getOrDefault(memberId, List.of());
        return toView(row, roles);
    }

    private void replaceMemberRoles(long domainId, long memberId, List<Long> roleIds) {
        List<Long> normalizedRoleIds = normalizeIds(roleIds);
        if (!normalizedRoleIds.isEmpty()) {
            ensureRolesBelongToDomain(domainId, normalizedRoleIds);
        }
        jdbcTemplate.update("DELETE FROM domain_member_role WHERE domain_member_id = ?", memberId);
        for (Long roleId : normalizedRoleIds) {
            jdbcTemplate.update("""
                            INSERT INTO domain_member_role (domain_member_id, domain_role_id, created_at)
                            VALUES (?, ?, CURRENT_TIMESTAMP(3))
                            """,
                    memberId,
                    roleId);
        }
    }

    private List<String> loadMemberRoleCodes(long memberId) {
        return jdbcTemplate.query("""
                        SELECT dr.code
                        FROM domain_member_role dmr
                        JOIN domain_role dr ON dr.id = dmr.domain_role_id
                        WHERE dmr.domain_member_id = ?
                        ORDER BY dr.id
                        """,
                (rs, rowNum) -> rs.getString("code"),
                memberId);
    }

    private List<String> loadRoleCodesByIds(long domainId, List<Long> roleIds) {
        if (roleIds.isEmpty()) {
            return List.of();
        }
        String placeholders = String.join(",", roleIds.stream().map(id -> "?").toList());
        List<Object> args = new ArrayList<>();
        args.add(domainId);
        args.addAll(roleIds);
        return jdbcTemplate.query("""
                        SELECT code
                        FROM domain_role
                        WHERE business_domain_id = ?
                          AND id IN (%s)
                        ORDER BY id
                        """.formatted(placeholders),
                (rs, rowNum) -> rs.getString("code"),
                args.toArray());
    }

    private void ensureRolesBelongToDomain(long domainId, List<Long> roleIds) {
        String placeholders = String.join(",", roleIds.stream().map(id -> "?").toList());
        List<Object> args = new ArrayList<>();
        args.add(domainId);
        args.addAll(roleIds);
        Long count = jdbcTemplate.queryForObject("""
                        SELECT COUNT(*)
                        FROM domain_role
                        WHERE business_domain_id = ?
                          AND id IN (%s)
                        """.formatted(placeholders),
                Long.class,
                args.toArray());
        if (count == null || count != roleIds.size()) {
            throw new IllegalArgumentException("domain role not found");
        }
    }

    private Map<Long, List<DomainRoleDtos.DomainRoleView>> loadRolesByMemberIds(List<Long> memberIds) {
        if (memberIds.isEmpty()) {
            return Map.of();
        }
        String placeholders = String.join(",", memberIds.stream().map(id -> "?").toList());
        return jdbcTemplate.query("""
                        SELECT
                            dmr.domain_member_id,
                            dr.id,
                            dr.business_domain_id,
                            dr.code,
                            dr.name,
                            dr.preset
                        FROM domain_member_role dmr
                        JOIN domain_role dr ON dr.id = dmr.domain_role_id
                        WHERE dmr.domain_member_id IN (%s)
                        ORDER BY dmr.domain_member_id, dr.id
                        """.formatted(placeholders),
                (ResultSet rs) -> {
                    Map<Long, List<DomainRoleDtos.DomainRoleView>> grouped = new LinkedHashMap<>();
                    while (rs.next()) {
                        long memberId = rs.getLong("domain_member_id");
                        grouped.computeIfAbsent(memberId, ignored -> new ArrayList<>()).add(new DomainRoleDtos.DomainRoleView(
                                rs.getLong("id"),
                                rs.getLong("business_domain_id"),
                                rs.getString("code"),
                                rs.getString("name"),
                                rs.getInt("preset") == 1));
                    }
                    return grouped;
                },
                memberIds.toArray());
    }

    private long countMembers(MemberQuery query) {
        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM domain_member dm JOIN staff_account sa ON sa.id = dm.staff_account_id%s".formatted(query.whereClause()),
                Long.class,
                query.args().toArray());
        return total == null ? 0L : total;
    }

    private MemberQuery buildMemberQuery(long domainId, String status, String keyword) {
        List<String> conditions = new ArrayList<>();
        List<Object> args = new ArrayList<>();
        conditions.add("dm.business_domain_id = ?");
        args.add(domainId);
        conditions.add("dm.deleted_at IS NULL");
        String resolvedStatus = normalizeStatus(status);
        if (resolvedStatus != null) {
            conditions.add("dm.status = ?");
            args.add(resolvedStatus);
        }
        if (StringUtils.hasText(keyword)) {
            String like = "%" + keyword.trim() + "%";
            conditions.add("(sa.login_name LIKE ? OR sa.phone LIKE ? OR sa.email LIKE ?)");
            args.add(like);
            args.add(like);
            args.add(like);
        }
        String whereClause = conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions);
        return new MemberQuery(whereClause, List.copyOf(args));
    }

    private Object[] pagingArgs(MemberQuery query, int page, int pageSize) {
        int normalizedPage = Math.max(page, 1);
        int normalizedPageSize = Math.max(pageSize, 1);
        List<Object> args = new ArrayList<>(query.args());
        args.add(normalizedPageSize);
        args.add((normalizedPage - 1L) * normalizedPageSize);
        return args.toArray();
    }

    private MemberRow mapMemberRow(ResultSet rs, int rowNum) throws SQLException {
        return new MemberRow(
                rs.getLong("id"),
                rs.getLong("staff_account_id"),
                rs.getLong("business_domain_id"),
                rs.getString("login_name"),
                rs.getString("phone"),
                rs.getString("email"),
                rs.getString("status"),
                rs.getString("source"),
                toLocalDateTime(rs.getTimestamp("activated_at")),
                toLocalDateTime(rs.getTimestamp("disabled_at")),
                toLocalDateTime(rs.getTimestamp("deleted_at")));
    }

    private DomainMemberDtos.DomainMemberView toView(MemberRow row, List<DomainRoleDtos.DomainRoleView> roles) {
        return new DomainMemberDtos.DomainMemberView(
                row.id(),
                row.staffAccountId(),
                row.businessDomainId(),
                row.loginName(),
                row.phone(),
                row.email(),
                row.status(),
                row.source(),
                row.activatedAt(),
                row.disabledAt(),
                row.deletedAt(),
                roles);
    }

    private MemberRow loadMember(long domainId, long memberId) {
        try {
            return jdbcTemplate.queryForObject("""
                            SELECT
                                dm.id,
                                dm.staff_account_id,
                                dm.business_domain_id,
                                dm.status,
                                dm.source,
                                dm.activated_at,
                                dm.disabled_at,
                                dm.deleted_at,
                                sa.login_name,
                                sa.phone,
                                sa.email
                            FROM domain_member dm
                            JOIN staff_account sa ON sa.id = dm.staff_account_id
                            WHERE dm.id = ?
                              AND dm.business_domain_id = ?
                              AND dm.deleted_at IS NULL
                            LIMIT 1
                            """,
                    this::mapMemberRow,
                    memberId,
                    domainId);
        } catch (EmptyResultDataAccessException ex) {
            throw new IllegalArgumentException("domain member not found");
        }
    }

    private boolean memberExists(long domainId, long staffAccountId) {
        Integer count = jdbcTemplate.queryForObject("""
                        SELECT COUNT(*)
                        FROM domain_member
                        WHERE business_domain_id = ?
                          AND staff_account_id = ?
                          AND deleted_at IS NULL
                        """,
                Integer.class,
                domainId,
                staffAccountId);
        return count != null && count > 0;
    }

    private long requireMemberId(long domainId, long staffAccountId) {
        Long memberId = jdbcTemplate.queryForObject("""
                        SELECT id
                        FROM domain_member
                        WHERE business_domain_id = ?
                          AND staff_account_id = ?
                          AND deleted_at IS NULL
                        ORDER BY id DESC
                        LIMIT 1
                        """,
                Long.class,
                domainId,
                staffAccountId);
        if (memberId == null) {
            throw new IllegalStateException("domain member create failed");
        }
        return memberId;
    }

    private void requireStaffAccount(long staffAccountId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM staff_account WHERE id = ?",
                Integer.class,
                staffAccountId);
        if (count == null || count == 0) {
            throw new IllegalArgumentException("staff account not found");
        }
    }

    private void requireDomain(long domainId) {
        domainService.getDomain(domainId);
    }

    private String normalizeStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return null;
        }
        String normalized = status.trim().toLowerCase();
        if ("active".equals(normalized) || "disabled".equals(normalized) || "deleted".equals(normalized)) {
            return normalized;
        }
        return null;
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private List<Long> normalizeIds(List<Long> values) {
        if (values == null) {
            return List.of();
        }
        Set<Long> unique = new LinkedHashSet<>();
        for (Long value : values) {
            if (value != null) {
                unique.add(value);
            }
        }
        return List.copyOf(unique);
    }

    private record MemberRow(
            long id,
            long staffAccountId,
            long businessDomainId,
            String loginName,
            String phone,
            String email,
            String status,
            String source,
            LocalDateTime activatedAt,
            LocalDateTime disabledAt,
            LocalDateTime deletedAt) {
    }

    private record MemberQuery(String whereClause, List<Object> args) {
    }
}
