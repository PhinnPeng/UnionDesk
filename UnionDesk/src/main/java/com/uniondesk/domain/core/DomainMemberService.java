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
import com.uniondesk.iam.core.IdentityPresentationService;
import com.uniondesk.iam.core.StaffAccountService;

@Service
public class DomainMemberService {

    private final JdbcTemplate jdbcTemplate;
    private final DomainService domainService;
    private final IdentityPresentationService identityPresentationService;
    private final StaffAccountService staffAccountService;

    public DomainMemberService(
            JdbcTemplate jdbcTemplate,
            DomainService domainService,
            IdentityPresentationService identityPresentationService,
            StaffAccountService staffAccountService) {
        this.jdbcTemplate = jdbcTemplate;
        this.domainService = domainService;
        this.identityPresentationService = identityPresentationService;
        this.staffAccountService = staffAccountService;
    }

    public PageResult<DomainMemberDtos.DomainMemberView> listMembers(
            long domainId,
            int page,
            int pageSize,
            String status,
            String keyword,
            LocalDateTime createdFrom,
            LocalDateTime createdTo) {
        requireDomain(domainId);
        MemberQuery query = buildMemberQuery(domainId, status, keyword, createdFrom, createdTo);
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
                            dm.created_at,
                            sa.username,
                            sa.phone,
                            sa.email
                        FROM domain_member dm
                        JOIN staff_account sa ON sa.id = dm.staff_account_id
                        %s
                        ORDER BY dm.id DESC
                        LIMIT ? OFFSET ?
                        """.formatted(query.whereClause()),
                this::mapMemberRow,
                pagingArgs(query.args(), page, pageSize));
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
        List<String> newRoleCodes = loadRoleCodesByIds(domainId, normalizeIds(request.role_ids()));
        guardSingleDomainOwner(domainId, memberId, newRoleCodes);
        replaceMemberRoles(domainId, memberId, request.role_ids());
        return getMember(domainId, memberId);
    }

    public PageResult<DomainMemberDtos.StaffCandidateView> listStaffCandidates(
            long domainId,
            int page,
            int pageSize,
            String keyword) {
        requireDomain(domainId);
        StaffCandidateQuery query = buildStaffCandidateQuery(domainId, keyword);
        List<StaffCandidateRow> rows = jdbcTemplate.query("""
                        SELECT
                            sa.id,
                            sa.username,
                            sa.real_name,
                            sa.nickname,
                            sa.phone,
                            sa.email,
                            sa.status
                        FROM staff_account sa
                        %s
                        ORDER BY sa.id DESC
                        LIMIT ? OFFSET ?
                        """.formatted(query.whereClause()),
                this::mapStaffCandidateRow,
                pagingArgs(query.args(), page, pageSize));
        return new PageResult<>(countStaffCandidates(query), rows.stream().map(this::toStaffCandidateView).toList());
    }

    @Transactional
    public DomainMemberDtos.DomainMemberView createMemberWithStaff(
            long domainId,
            DomainMemberDtos.CreateMemberWithStaffRequest request) {
        requireDomain(domainId);
        List<Long> roleIds = normalizeIds(request.role_ids());
        if (roleIds.isEmpty()) {
            throw new IllegalArgumentException("角色不能为空");
        }
        List<String> roleCodes = loadRoleCodesByIds(domainId, roleIds);
        guardSingleDomainOwner(domainId, -1L, roleCodes);
        StaffAccountService.StaffAccount created = staffAccountService.create(new StaffAccountService.CreateStaffCommand(
                request.username(),
                request.real_name(),
                request.nickname(),
                request.phone(),
                request.email(),
                request.password(),
                roleCodes,
                List.of(domainId)));
        long memberId = requireMemberId(domainId, created.id());
        return getMember(domainId, memberId);
    }

    @Transactional
    public DomainMemberDtos.DomainMemberView updateMemberStatus(
            long domainId,
            long memberId,
            DomainMemberDtos.UpdateDomainMemberStatusRequest request) {
        requireDomain(domainId);
        String status = normalizeMemberStatus(request.status());
        if (status == null) {
            throw new IllegalArgumentException("无效的成员状态");
        }
        loadMember(domainId, memberId);
        if ("disabled".equals(status)) {
            List<String> roleCodes = loadMemberRoleCodes(memberId);
            if (roleCodes.contains("domain_admin")) {
                guardLastDomainAdmin(domainId, memberId);
            }
            if (roleCodes.contains("super_admin")) {
                guardLastDomainSuperAdmin(domainId, memberId);
            }
        }
        int updated = jdbcTemplate.update("""
                        UPDATE domain_member
                        SET status = ?,
                            disabled_at = CASE WHEN ? = 'disabled' THEN CURRENT_TIMESTAMP(3) ELSE NULL END,
                            activated_at = CASE WHEN ? = 'active' THEN CURRENT_TIMESTAMP(3) ELSE activated_at END,
                            updated_at = CURRENT_TIMESTAMP(3)
                        WHERE id = ?
                          AND business_domain_id = ?
                          AND deleted_at IS NULL
                        """,
                status,
                status,
                status,
                memberId,
                domainId);
        if (updated == 0) {
            throw new IllegalArgumentException("domain member not found");
        }
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
        guardSingleDomainOwner(domainId, memberId, newRoleCodes);
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
            throw new IllegalStateException("请先指定另一位业务域所有人");
        }
    }

    void guardSingleDomainOwner(long domainId, long memberId, List<String> newRoleCodes) {
        if (!newRoleCodes.contains("super_admin")) {
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
        if (otherCount != null && otherCount > 0) {
            throw new IllegalStateException("该业务域已存在所有人，请先转让后再授权");
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

    private MemberQuery buildMemberQuery(long domainId, String status, String keyword, LocalDateTime createdFrom, LocalDateTime createdTo) {
        List<String> conditions = new ArrayList<>();
        List<Object> args = new ArrayList<>();
        conditions.add("dm.business_domain_id = ?");
        args.add(domainId);
        conditions.add("dm.deleted_at IS NULL");
        String resolvedStatus = normalizeMemberStatus(status);
        if (resolvedStatus != null) {
            conditions.add("dm.status = ?");
            args.add(resolvedStatus);
        }
        if (StringUtils.hasText(keyword)) {
            String like = "%" + keyword.trim() + "%";
            conditions.add("(sa.username LIKE ? OR sa.phone LIKE ? OR sa.email LIKE ? OR sa.real_name LIKE ? OR sa.nickname LIKE ?)");
            args.add(like);
            args.add(like);
            args.add(like);
            args.add(like);
            args.add(like);
        }
        if (createdFrom != null) {
            conditions.add("dm.created_at >= ?");
            args.add(Timestamp.valueOf(createdFrom));
        }
        if (createdTo != null) {
            conditions.add("dm.created_at <= ?");
            args.add(Timestamp.valueOf(createdTo));
        }
        String whereClause = conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions);
        return new MemberQuery(whereClause, List.copyOf(args));
    }

    private StaffCandidateQuery buildStaffCandidateQuery(long domainId, String keyword) {
        List<String> conditions = new ArrayList<>();
        List<Object> args = new ArrayList<>();
        conditions.add("sa.status = 'active'");
        conditions.add("""
                NOT EXISTS (
                    SELECT 1
                    FROM domain_member dm
                    WHERE dm.staff_account_id = sa.id
                      AND dm.business_domain_id = ?
                      AND dm.deleted_at IS NULL
                )
                """);
        args.add(domainId);
        if (StringUtils.hasText(keyword)) {
            String like = "%" + keyword.trim() + "%";
            conditions.add("(sa.username LIKE ? OR sa.phone LIKE ? OR sa.email LIKE ? OR sa.real_name LIKE ? OR sa.nickname LIKE ?)");
            args.add(like);
            args.add(like);
            args.add(like);
            args.add(like);
            args.add(like);
        }
        return new StaffCandidateQuery(" WHERE " + String.join(" AND ", conditions), List.copyOf(args));
    }

    private long countStaffCandidates(StaffCandidateQuery query) {
        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM staff_account sa%s".formatted(query.whereClause()),
                Long.class,
                query.args().toArray());
        return total == null ? 0L : total;
    }

    private Object[] pagingArgs(List<Object> args, int page, int pageSize) {
        int normalizedPage = Math.max(page, 1);
        int normalizedPageSize = Math.max(pageSize, 1);
        List<Object> paging = new ArrayList<>(args);
        paging.add(normalizedPageSize);
        paging.add((normalizedPage - 1L) * normalizedPageSize);
        return paging.toArray();
    }

    private MemberRow mapMemberRow(ResultSet rs, int rowNum) throws SQLException {
        return new MemberRow(
                rs.getLong("id"),
                rs.getLong("staff_account_id"),
                rs.getLong("business_domain_id"),
                rs.getString("username"),
                rs.getString("phone"),
                rs.getString("email"),
                rs.getString("status"),
                rs.getString("source"),
                toLocalDateTime(rs.getTimestamp("activated_at")),
                toLocalDateTime(rs.getTimestamp("disabled_at")),
                toLocalDateTime(rs.getTimestamp("deleted_at")),
                toLocalDateTime(rs.getTimestamp("created_at")));
    }

    private StaffCandidateRow mapStaffCandidateRow(ResultSet rs, int rowNum) throws SQLException {
        return new StaffCandidateRow(
                rs.getLong("id"),
                rs.getString("username"),
                rs.getString("real_name"),
                rs.getString("nickname"),
                rs.getString("phone"),
                rs.getString("email"),
                rs.getString("status"));
    }

    private DomainMemberDtos.StaffCandidateView toStaffCandidateView(StaffCandidateRow row) {
        return new DomainMemberDtos.StaffCandidateView(
                row.id(),
                row.username(),
                row.realName(),
                row.nickname(),
                row.phone(),
                row.email(),
                row.status());
    }

    private DomainMemberDtos.DomainMemberView toView(MemberRow row, List<DomainRoleDtos.DomainRoleView> roles) {
        IdentityPresentationService.ResolvedStaffDomainView presentation =
                identityPresentationService.resolveStaffInDomain(row.staffAccountId(), row.businessDomainId());
        return new DomainMemberDtos.DomainMemberView(
                row.id(),
                row.staffAccountId(),
                row.businessDomainId(),
                row.username(),
                presentation.realName(),
                presentation.nickname(),
                row.phone(),
                row.email(),
                row.status(),
                row.source(),
                row.activatedAt(),
                row.disabledAt(),
                row.deletedAt(),
                row.createdAt(),
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
                                dm.created_at,
                                sa.username,
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

    private String normalizeMemberStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return null;
        }
        String normalized = status.trim().toLowerCase();
        if ("active".equals(normalized) || "disabled".equals(normalized)) {
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
            String username,
            String phone,
            String email,
            String status,
            String source,
            LocalDateTime activatedAt,
            LocalDateTime disabledAt,
            LocalDateTime deletedAt,
            LocalDateTime createdAt) {
    }

    private record StaffCandidateRow(
            long id,
            String username,
            String realName,
            String nickname,
            String phone,
            String email,
            String status) {
    }

    private record MemberQuery(String whereClause, List<Object> args) {
    }

    private record StaffCandidateQuery(String whereClause, List<Object> args) {
    }
}
