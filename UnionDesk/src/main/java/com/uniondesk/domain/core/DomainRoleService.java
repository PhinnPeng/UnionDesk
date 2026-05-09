package com.uniondesk.domain.core;

import com.uniondesk.domain.web.DomainRoleDtos;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class DomainRoleService {

    private final JdbcTemplate jdbcTemplate;
    private final DomainService domainService;

    public DomainRoleService(JdbcTemplate jdbcTemplate, DomainService domainService) {
        this.jdbcTemplate = jdbcTemplate;
        this.domainService = domainService;
    }

    public List<DomainRoleDtos.DomainRoleView> listRoles(long domainId) {
        requireDomain(domainId);
        return jdbcTemplate.query("""
                        SELECT id, business_domain_id, code, name, preset
                        FROM domain_role
                        WHERE business_domain_id = ?
                        ORDER BY id DESC
                        """,
                this::mapRoleView,
                domainId);
    }

    @Transactional
    public DomainRoleDtos.DomainRoleView createRole(long domainId, DomainRoleDtos.CreateDomainRoleRequest request) {
        requireDomain(domainId);
        jdbcTemplate.update("""
                        INSERT INTO domain_role (
                            business_domain_id,
                            code,
                            name,
                            preset,
                            created_at,
                            updated_at
                        )
                        VALUES (?, ?, ?, 0, CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3))
                        """,
                domainId,
                request.code().trim(),
                request.name().trim());
        return loadRole(domainId, requireRoleId(domainId, request.code().trim()));
    }

    @Transactional
    public DomainRoleDtos.DomainRoleView updateRole(long domainId, long roleId, DomainRoleDtos.UpdateDomainRoleRequest request) {
        DomainRoleDtos.DomainRoleView existing = loadRole(domainId, roleId);
        String code = StringUtils.hasText(request.code()) ? request.code().trim() : existing.code();
        String name = StringUtils.hasText(request.name()) ? request.name().trim() : existing.name();
        jdbcTemplate.update("""
                        UPDATE domain_role
                        SET code = ?,
                            name = ?,
                            updated_at = CURRENT_TIMESTAMP(3)
                        WHERE id = ?
                          AND business_domain_id = ?
                        """,
                code,
                name,
                roleId,
                domainId);
        return loadRole(domainId, roleId);
    }

    public DomainRoleDtos.DomainRolePermissionView getRolePermissions(long domainId, long roleId) {
        DomainRoleDtos.DomainRoleView role = loadRole(domainId, roleId);
        return new DomainRoleDtos.DomainRolePermissionView(
                role.id(),
                role.code(),
                role.name(),
                loadPermissionItems(roleId));
    }

    @Transactional
    public DomainRoleDtos.DomainRolePermissionView updateRolePermissions(
            long domainId,
            long roleId,
            DomainRoleDtos.UpdateDomainRolePermissionRequest request) {
        loadRole(domainId, roleId);
        List<Long> permissionItemIds = normalizeIds(request.permission_item_ids());
        if (!permissionItemIds.isEmpty()) {
            ensurePermissionItemsExist(permissionItemIds);
        }
        jdbcTemplate.update("DELETE FROM domain_role_permission WHERE domain_role_id = ?", roleId);
        for (Long permissionItemId : permissionItemIds) {
            jdbcTemplate.update("""
                            INSERT INTO domain_role_permission (domain_role_id, permission_item_id, created_at)
                            VALUES (?, ?, CURRENT_TIMESTAMP(3))
                            """,
                    roleId,
                    permissionItemId);
        }
        return getRolePermissions(domainId, roleId);
    }

    @Transactional
    public void deleteRole(long domainId, long roleId) {
        DomainRoleDtos.DomainRoleView role = loadRole(domainId, roleId);
        if (role.preset()) {
            throw new IllegalArgumentException("preset role cannot be deleted");
        }
        Integer memberCount = jdbcTemplate.queryForObject("""
                        SELECT COUNT(*)
                        FROM domain_member_role dmr
                        JOIN domain_member dm ON dm.id = dmr.domain_member_id
                        WHERE dmr.domain_role_id = ?
                          AND dm.business_domain_id = ?
                          AND dm.deleted_at IS NULL
                        """,
                Integer.class,
                roleId,
                domainId);
        if (memberCount != null && memberCount > 0) {
            throw new IllegalStateException("role is still bound to members");
        }
        jdbcTemplate.update("DELETE FROM domain_role_permission WHERE domain_role_id = ?", roleId);
        jdbcTemplate.update("""
                        DELETE FROM domain_role
                        WHERE id = ?
                          AND business_domain_id = ?
                        """,
                roleId,
                domainId);
    }

    public List<DomainRoleDtos.PermissionItemView> listPermissionItems(long domainId) {
        requireDomain(domainId);
        return jdbcTemplate.query("""
                        SELECT id, code, name, module, type
                        FROM permission_item
                        ORDER BY module, type, id
                        """,
                this::mapPermissionItemView);
    }

    private DomainRoleDtos.DomainRoleView loadRole(long domainId, long roleId) {
        try {
            return jdbcTemplate.queryForObject("""
                            SELECT id, business_domain_id, code, name, preset
                            FROM domain_role
                            WHERE id = ?
                              AND business_domain_id = ?
                            LIMIT 1
                            """,
                    this::mapRoleView,
                    roleId,
                    domainId);
        } catch (EmptyResultDataAccessException ex) {
            throw new IllegalArgumentException("domain role not found");
        }
    }

    private List<DomainRoleDtos.PermissionItemView> loadPermissionItems(long roleId) {
        return jdbcTemplate.query("""
                        SELECT pi.id, pi.code, pi.name, pi.module, pi.type
                        FROM domain_role_permission drp
                        JOIN permission_item pi ON pi.id = drp.permission_item_id
                        WHERE drp.domain_role_id = ?
                        ORDER BY pi.module, pi.type, pi.id
                        """,
                this::mapPermissionItemView,
                roleId);
    }

    private void ensurePermissionItemsExist(List<Long> permissionItemIds) {
        String placeholders = String.join(",", permissionItemIds.stream().map(id -> "?").toList());
        Long count = jdbcTemplate.queryForObject("""
                        SELECT COUNT(*)
                        FROM permission_item
                        WHERE id IN (%s)
                        """.formatted(placeholders),
                Long.class,
                permissionItemIds.toArray());
        if (count == null || count != permissionItemIds.size()) {
            throw new IllegalArgumentException("permission item not found");
        }
    }

    private long requireRoleId(long domainId, String code) {
        Long roleId = jdbcTemplate.queryForObject("""
                        SELECT id
                        FROM domain_role
                        WHERE business_domain_id = ?
                          AND code = ?
                        ORDER BY id DESC
                        LIMIT 1
                        """,
                Long.class,
                domainId,
                code);
        if (roleId == null) {
            throw new IllegalStateException("domain role create failed");
        }
        return roleId;
    }

    private DomainRoleDtos.DomainRoleView mapRoleView(ResultSet rs, int rowNum) throws SQLException {
        return new DomainRoleDtos.DomainRoleView(
                rs.getLong("id"),
                rs.getLong("business_domain_id"),
                rs.getString("code"),
                rs.getString("name"),
                rs.getInt("preset") == 1);
    }

    private DomainRoleDtos.PermissionItemView mapPermissionItemView(ResultSet rs, int rowNum) throws SQLException {
        return new DomainRoleDtos.PermissionItemView(
                rs.getLong("id"),
                rs.getString("code"),
                rs.getString("name"),
                rs.getString("module"),
                rs.getString("type"));
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

    private void requireDomain(long domainId) {
        domainService.getDomain(domainId);
    }
}
