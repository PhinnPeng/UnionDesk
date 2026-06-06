package com.uniondesk.iam.core;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class StaffAccountService {

    private final JdbcTemplate jdbcTemplate;
    private final IdentitySubjectService identitySubjectService;
    private final PasswordEncoder passwordEncoder;

    public StaffAccountService(
            JdbcTemplate jdbcTemplate,
            IdentitySubjectService identitySubjectService,
            PasswordEncoder passwordEncoder) {
        this.jdbcTemplate = jdbcTemplate;
        this.identitySubjectService = identitySubjectService;
        this.passwordEncoder = passwordEncoder;
    }

    public List<StaffAccount> listAll() {
        return jdbcTemplate.query("""
                        SELECT
                            id,
                            subject_id,
                            username,
                            real_name,
                            nickname,
                            avatar_url,
                            phone,
                            email,
                            status,
                            source,
                            auth_version
                        FROM staff_account
                        ORDER BY id DESC
                        """,
                this::mapStaffAccount);
    }

    public Optional<StaffAccount> findById(long staffAccountId) {
        try {
            return Optional.of(jdbcTemplate.queryForObject("""
                            SELECT
                                id,
                                subject_id,
                                username,
                                real_name,
                                nickname,
                                avatar_url,
                                phone,
                                email,
                                status,
                                source,
                                auth_version
                            FROM staff_account
                            WHERE id = ?
                            LIMIT 1
                            """,
                    this::mapStaffAccount,
                    staffAccountId));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    @Transactional
    public StaffAccount create(CreateStaffCommand command) {
        String username = requireText(command.username(), "登录账号不能为空");
        String phone = requireText(command.phone(), "手机号不能为空");
        String password = requireText(command.password(), "密码不能为空");
        String realName = trimToNull(command.realName());
        String nickname = trimToNull(command.nickname());
        String email = trimToNull(command.email());
        long subjectId = identitySubjectService.resolveSubjectIdByPhone(phone);
        identitySubjectService.requireActiveSubject(subjectId);
        try {
            jdbcTemplate.update("""
                            INSERT INTO staff_account (
                                subject_id,
                                username,
                                real_name,
                                nickname,
                                avatar_url,
                                phone,
                                email,
                                password_hash,
                                must_change_password,
                                status,
                                source,
                                auth_version,
                                password_changed_at,
                                created_at,
                                updated_at
                            )
                            VALUES (?, ?, ?, ?, NULL, ?, ?, ?, 0, 'active', 'local', 1, CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3))
                            """,
                    subjectId,
                    username,
                    realName,
                    nickname,
                    phone,
                    email,
                    passwordEncoder.encode(password));
        } catch (DuplicateKeyException ex) {
            throw new IllegalArgumentException("登录账号或手机号已存在");
        }
        Long id = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        if (id == null) {
            throw new IllegalStateException("员工账号创建失败");
        }
        bindDomainMemberships(id, command.businessDomainIds(), command.roleCodes());
        return findById(id).orElseThrow(() -> new IllegalStateException("员工账号创建失败"));
    }

    @Transactional
    public StaffAccount update(long staffAccountId, UpdateStaffCommand command) {
        findById(staffAccountId).orElseThrow(() -> new IllegalArgumentException("员工账号不存在"));
        List<String> assignments = new ArrayList<>();
        List<Object> args = new ArrayList<>();
        if (command.username() != null) {
            assignments.add("username = ?");
            args.add(requireText(command.username(), "登录账号不能为空"));
        }
        if (command.realName() != null) {
            assignments.add("real_name = ?");
            args.add(trimToNull(command.realName()));
        }
        if (command.nickname() != null) {
            assignments.add("nickname = ?");
            args.add(trimToNull(command.nickname()));
        }
        if (command.phone() != null) {
            assignments.add("phone = ?");
            args.add(requireText(command.phone(), "手机号不能为空"));
        }
        if (command.email() != null) {
            assignments.add("email = ?");
            args.add(trimToNull(command.email()));
        }
        if (command.password() != null) {
            assignments.add("password_hash = ?");
            args.add(passwordEncoder.encode(requireText(command.password(), "密码不能为空")));
            assignments.add("password_changed_at = CURRENT_TIMESTAMP(3)");
        }
        if (command.status() != null) {
            assignments.add("status = ?");
            args.add(mapStatus(command.status()));
        }
        if (!assignments.isEmpty()) {
            assignments.add("updated_at = CURRENT_TIMESTAMP(3)");
            args.add(staffAccountId);
            try {
                jdbcTemplate.update(
                        "UPDATE staff_account SET " + String.join(", ", assignments) + " WHERE id = ?",
                        args.toArray());
            } catch (DuplicateKeyException ex) {
                throw new IllegalArgumentException("登录账号或手机号已存在");
            }
        }
        if (command.roleCodes() != null || command.businessDomainIds() != null) {
            List<String> roleCodes = command.roleCodes() != null ? command.roleCodes() : listDomainRoleCodes(staffAccountId);
            List<Long> domainIds = command.businessDomainIds() != null ? command.businessDomainIds() : listBusinessDomainIds(staffAccountId);
            bindDomainMemberships(staffAccountId, domainIds, roleCodes);
        }
        return findById(staffAccountId).orElseThrow(() -> new IllegalArgumentException("员工账号不存在"));
    }

    @Transactional
    public StaffAccount disable(long staffAccountId) {
        jdbcTemplate.update("""
                        UPDATE staff_account
                        SET status = 'disabled',
                            updated_at = CURRENT_TIMESTAMP(3)
                        WHERE id = ?
                        """,
                staffAccountId);
        jdbcTemplate.update("""
                        UPDATE auth_login_session
                        SET session_status = 'revoked',
                            revoked_at = CURRENT_TIMESTAMP(3),
                            revoked_reason = 'staff_disabled'
                        WHERE user_id = ?
                          AND account_type = 'staff'
                          AND session_status = 'active'
                        """,
                staffAccountId);
        return findById(staffAccountId).orElseThrow(() -> new IllegalArgumentException("员工账号不存在"));
    }

    @Transactional
    public StaffAccount restore(long staffAccountId) {
        jdbcTemplate.update("""
                        UPDATE staff_account
                        SET status = 'active',
                            updated_at = CURRENT_TIMESTAMP(3)
                        WHERE id = ?
                        """,
                staffAccountId);
        return findById(staffAccountId).orElseThrow(() -> new IllegalArgumentException("员工账号不存在"));
    }

    public List<Long> listBusinessDomainIds(long staffAccountId) {
        return jdbcTemplate.queryForList("""
                        SELECT DISTINCT business_domain_id
                        FROM domain_member
                        WHERE staff_account_id = ?
                          AND deleted_at IS NULL
                        ORDER BY business_domain_id
                        """,
                Long.class,
                staffAccountId);
    }

    public List<String> listDomainRoleCodes(long staffAccountId) {
        return jdbcTemplate.query("""
                        SELECT DISTINCT dr.code
                        FROM domain_member dm
                        JOIN domain_member_role dmr ON dmr.domain_member_id = dm.id
                        JOIN domain_role dr ON dr.id = dmr.domain_role_id
                        WHERE dm.staff_account_id = ?
                          AND dm.deleted_at IS NULL
                        ORDER BY dr.code
                        """,
                (rs, rowNum) -> rs.getString("code"),
                staffAccountId);
    }

    private void bindDomainMemberships(long staffAccountId, List<Long> businessDomainIds, List<String> roleCodes) {
        if (businessDomainIds == null || businessDomainIds.isEmpty()) {
            return;
        }
        if (roleCodes == null || roleCodes.isEmpty()) {
            throw new IllegalArgumentException("角色不能为空");
        }
        Set<Long> domainIds = new LinkedHashSet<>(businessDomainIds.stream().filter(Objects::nonNull).toList());
        Set<String> codes = new LinkedHashSet<>(roleCodes.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .toList());
        for (Long domainId : domainIds) {
            ensureDomainExists(domainId);
            long memberId = ensureDomainMember(domainId, staffAccountId);
            jdbcTemplate.update("DELETE FROM domain_member_role WHERE domain_member_id = ?", memberId);
            for (String code : codes) {
                Long domainRoleId = findDomainRoleId(domainId, code);
                if (domainRoleId == null) {
                    throw new IllegalArgumentException("业务域角色不存在：" + code);
                }
                jdbcTemplate.update("""
                                INSERT INTO domain_member_role (domain_member_id, domain_role_id, created_at)
                                VALUES (?, ?, CURRENT_TIMESTAMP(3))
                                """,
                        memberId,
                        domainRoleId);
            }
        }
    }

    private long ensureDomainMember(long domainId, long staffAccountId) {
        Long memberId = jdbcTemplate.query("""
                        SELECT id
                        FROM domain_member
                        WHERE business_domain_id = ?
                          AND staff_account_id = ?
                          AND deleted_at IS NULL
                        LIMIT 1
                        """,
                rs -> rs.next() ? rs.getLong("id") : null,
                domainId,
                staffAccountId);
        if (memberId != null) {
            return memberId;
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
        memberId = jdbcTemplate.queryForObject("""
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
        if (memberId == null) {
            throw new IllegalStateException("域成员创建失败");
        }
        return memberId;
    }

    private void ensureDomainExists(long domainId) {
        Integer count = jdbcTemplate.queryForObject("""
                        SELECT COUNT(*)
                        FROM business_domain
                        WHERE id = ?
                          AND deleted_at IS NULL
                        """,
                Integer.class,
                domainId);
        if (count == null || count == 0) {
            throw new IllegalArgumentException("业务域不存在");
        }
    }

    private Long findDomainRoleId(long domainId, String code) {
        try {
            return jdbcTemplate.queryForObject("""
                            SELECT id
                            FROM domain_role
                            WHERE business_domain_id = ?
                              AND code = ?
                            LIMIT 1
                            """,
                    Long.class,
                    domainId,
                    code);
        } catch (EmptyResultDataAccessException ex) {
            return null;
        }
    }

    private StaffAccount mapStaffAccount(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        return new StaffAccount(
                rs.getLong("id"),
                rs.getLong("subject_id"),
                rs.getString("username"),
                rs.getString("real_name"),
                rs.getString("nickname"),
                rs.getString("avatar_url"),
                rs.getString("phone"),
                rs.getString("email"),
                rs.getString("status"),
                rs.getString("source"),
                rs.getInt("auth_version"));
    }

    private static String mapStatus(Integer status) {
        if (status == null) {
            throw new IllegalArgumentException("状态无效");
        }
        return status == 0 ? "disabled" : "active";
    }

    private static String requireText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private static String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    public record StaffAccount(
            long id,
            long subjectId,
            String username,
            String realName,
            String nickname,
            String avatarUrl,
            String phone,
            String email,
            String status,
            String source,
            int authVersion) {
    }

    public record CreateStaffCommand(
            String username,
            String realName,
            String nickname,
            String phone,
            String email,
            String password,
            List<String> roleCodes,
            List<Long> businessDomainIds) {
    }

    public record UpdateStaffCommand(
            String username,
            String realName,
            String nickname,
            String phone,
            String email,
            String password,
            Integer status,
            List<String> roleCodes,
            List<Long> businessDomainIds) {
    }
}
