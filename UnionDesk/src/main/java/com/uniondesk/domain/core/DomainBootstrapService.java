package com.uniondesk.domain.core;

import java.util.List;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DomainBootstrapService {

    private static final List<PresetRole> PRESET_ROLES = List.of(
            new PresetRole("super_admin", "业务域所有人"),
            new PresetRole("domain_admin", "业务域管理员"),
            new PresetRole("agent", "客服"));

    private final JdbcTemplate jdbcTemplate;

    public DomainBootstrapService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public BootstrapResult bootstrapNewDomain(long domainId, long creatorUserId) {
        seedPresetRoles(domainId);
        seedSuperAdminAllPermissionItems(domainId);
        long staffAccountId = resolveStaffAccountId(creatorUserId);
        grantCreatorSuperAdmin(domainId, creatorUserId, staffAccountId);
        return new BootstrapResult(staffAccountId, "super_admin");
    }

    void seedPresetRoles(long domainId) {
        for (PresetRole preset : PRESET_ROLES) {
            jdbcTemplate.update("""
                            INSERT INTO domain_role (
                                business_domain_id, code, name, preset, created_at, updated_at
                            )
                            SELECT ?, ?, ?, 1, CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3)
                            FROM DUAL
                            WHERE NOT EXISTS (
                                SELECT 1
                                FROM domain_role
                                WHERE business_domain_id = ?
                                  AND code = ?
                            )
                            """,
                    domainId,
                    preset.code(),
                    preset.name(),
                    domainId,
                    preset.code());
        }
    }

    long resolveStaffAccountId(long userId) {
        try {
            Long staffId = jdbcTemplate.queryForObject(
                    "SELECT id FROM staff_account WHERE id = ? LIMIT 1",
                    Long.class,
                    userId);
            if (staffId != null) {
                return staffId;
            }
        } catch (EmptyResultDataAccessException ignored) {
            // fall through
        }

        String username;
        try {
            username = jdbcTemplate.queryForObject(
                    "SELECT username FROM user_account WHERE id = ? LIMIT 1",
                    String.class,
                    userId);
        } catch (EmptyResultDataAccessException ex) {
            throw new IllegalStateException("creator user account not found: " + userId);
        }
        if (!StringUtils.hasText(username)) {
            throw new IllegalStateException("creator user account has no username: " + userId);
        }

        try {
            Long staffId = jdbcTemplate.queryForObject(
                    "SELECT id FROM staff_account WHERE username = ? LIMIT 1",
                    Long.class,
                    username.trim());
            if (staffId != null) {
                return staffId;
            }
        } catch (EmptyResultDataAccessException ex) {
            throw new IllegalStateException("staff account not found for creator user: " + userId);
        }
        throw new IllegalStateException("staff account not found for creator user: " + userId);
    }

    void grantCreatorSuperAdmin(long domainId, long creatorUserId, long staffAccountId) {
        Long memberId = findActiveMemberId(domainId, staffAccountId);
        if (memberId == null) {
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
                            VALUES (?, ?, 'active', 'domain_create', CURRENT_TIMESTAMP(3), NULL, NULL, CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3))
                            """,
                    staffAccountId,
                    domainId);
            memberId = requireActiveMemberId(domainId, staffAccountId);
        }

        long superAdminRoleId = requireDomainRoleId(domainId, "super_admin");
        jdbcTemplate.update("""
                        INSERT INTO domain_member_role (domain_member_id, domain_role_id, created_at)
                        SELECT ?, ?, CURRENT_TIMESTAMP(3)
                        FROM DUAL
                        WHERE NOT EXISTS (
                            SELECT 1
                            FROM domain_member_role
                            WHERE domain_member_id = ?
                              AND domain_role_id = ?
                        )
                        """,
                memberId,
                superAdminRoleId,
                memberId,
                superAdminRoleId);

        int legacySuperAdminRoleId = requireLegacyRoleId("super_admin");
        syncCreatorDomainSuperAdminBinding(domainId, creatorUserId, legacySuperAdminRoleId);
    }

    void seedSuperAdminAllPermissionItems(long domainId) {
        long superAdminRoleId = requireDomainRoleId(domainId, "super_admin");
        jdbcTemplate.update("""
                        INSERT INTO domain_role_permission (domain_role_id, permission_item_id, created_at)
                        SELECT ?, pi.id, CURRENT_TIMESTAMP(3)
                        FROM permission_item pi
                        WHERE NOT EXISTS (
                            SELECT 1
                            FROM domain_role_permission drp
                            WHERE drp.domain_role_id = ?
                              AND drp.permission_item_id = pi.id
                        )
                        """,
                superAdminRoleId,
                superAdminRoleId);
    }

    void syncCreatorDomainSuperAdminBinding(long domainId, long creatorUserId, int legacySuperAdminRoleId) {
        jdbcTemplate.update("""
                        INSERT INTO iam_role_binding (
                            user_id,
                            role_id,
                            binding_scope,
                            business_domain_id,
                            status,
                            created_at,
                            updated_at
                        )
                        SELECT ?, ?, 'domain', ?, 1, CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3)
                        FROM DUAL
                        WHERE NOT EXISTS (
                            SELECT 1
                            FROM iam_role_binding
                            WHERE user_id = ?
                              AND role_id = ?
                              AND binding_scope = 'domain'
                              AND business_domain_id = ?
                        )
                        """,
                creatorUserId,
                legacySuperAdminRoleId,
                domainId,
                creatorUserId,
                legacySuperAdminRoleId,
                domainId);
    }

    private Long findActiveMemberId(long domainId, long staffAccountId) {
        try {
            return jdbcTemplate.queryForObject("""
                            SELECT id
                            FROM domain_member
                            WHERE business_domain_id = ?
                              AND staff_account_id = ?
                              AND deleted_at IS NULL
                            LIMIT 1
                            """,
                    Long.class,
                    domainId,
                    staffAccountId);
        } catch (EmptyResultDataAccessException ex) {
            return null;
        }
    }

    private long requireActiveMemberId(long domainId, long staffAccountId) {
        Long memberId = findActiveMemberId(domainId, staffAccountId);
        if (memberId == null) {
            throw new IllegalStateException("domain member bootstrap failed");
        }
        return memberId;
    }

    private long requireDomainRoleId(long domainId, String code) {
        try {
            Long roleId = jdbcTemplate.queryForObject("""
                            SELECT id
                            FROM domain_role
                            WHERE business_domain_id = ?
                              AND code = ?
                            LIMIT 1
                            """,
                    Long.class,
                    domainId,
                    code);
            if (roleId == null) {
                throw new IllegalStateException("domain role not found: " + code);
            }
            return roleId;
        } catch (EmptyResultDataAccessException ex) {
            throw new IllegalStateException("domain role not found: " + code);
        }
    }

    private int requireLegacyRoleId(String code) {
        try {
            Integer roleId = jdbcTemplate.queryForObject(
                    "SELECT id FROM role WHERE code = ? LIMIT 1",
                    Integer.class,
                    code);
            if (roleId == null) {
                throw new IllegalStateException("legacy role not found: " + code);
            }
            return roleId;
        } catch (EmptyResultDataAccessException ex) {
            throw new IllegalStateException("legacy role not found: " + code);
        }
    }

    public record BootstrapResult(long staffAccountId, String grantedRole) {
    }

    private record PresetRole(String code, String name) {
    }
}
