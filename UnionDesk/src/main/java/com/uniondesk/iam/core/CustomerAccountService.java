package com.uniondesk.iam.core;

import java.util.Optional;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class CustomerAccountService {

    private final JdbcTemplate jdbcTemplate;
    private final IdentitySubjectService identitySubjectService;
    private final PasswordEncoder passwordEncoder;

    public CustomerAccountService(
            JdbcTemplate jdbcTemplate,
            IdentitySubjectService identitySubjectService,
            PasswordEncoder passwordEncoder) {
        this.jdbcTemplate = jdbcTemplate;
        this.identitySubjectService = identitySubjectService;
        this.passwordEncoder = passwordEncoder;
    }

    public Optional<CustomerAccount> findById(long customerAccountId) {
        try {
            return Optional.of(jdbcTemplate.queryForObject("""
                            SELECT id, subject_id, username, nickname, phone, email, status
                            FROM customer_account
                            WHERE id = ?
                            LIMIT 1
                            """,
                    (rs, rowNum) -> new CustomerAccount(
                            rs.getLong("id"),
                            rs.getLong("subject_id"),
                            rs.getString("username"),
                            rs.getString("nickname"),
                            rs.getString("phone"),
                            rs.getString("email"),
                            rs.getString("status")),
                    customerAccountId));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    public void ensureUsernameAvailable(String username) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM customer_account WHERE username = ?",
                Integer.class,
                username.trim());
        if (count != null && count > 0) {
            throw new IllegalArgumentException("登录名已存在");
        }
    }

    @Transactional
    public long create(CreateCustomerCommand command) {
        String username = requireText(command.username(), "登录名不能为空");
        String phone = requireText(command.phone(), "手机号不能为空");
        String nickname = StringUtils.hasText(command.nickname()) ? command.nickname().trim() : username;
        String email = trimToNull(command.email());
        ensureUsernameAvailable(username);
        long subjectId = identitySubjectService.resolveSubjectIdByPhone(phone);
        identitySubjectService.requireActiveSubject(subjectId);
        try {
            jdbcTemplate.update("""
                            INSERT INTO customer_account (
                                subject_id,
                                username,
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
                            VALUES (?, ?, ?, NULL, ?, ?, ?, ?, 'active', 'local', 1, CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3))
                            """,
                    subjectId,
                    username,
                    nickname,
                    phone,
                    email,
                    passwordEncoder.encode(command.password()),
                    command.mustChangePassword() ? 1 : 0);
        } catch (DuplicateKeyException ex) {
            throw new IllegalArgumentException("登录名或手机号已存在");
        }
        Long id = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        if (id == null) {
            throw new IllegalStateException("客户账号创建失败");
        }
        return id;
    }

    public Optional<Long> findIdByUsernameOrPhone(String username, String phone) {
        return Optional.ofNullable(jdbcTemplate.query("""
                        SELECT id
                        FROM customer_account
                        WHERE username = ? OR phone = ?
                        LIMIT 1
                        """,
                rs -> rs.next() ? rs.getLong("id") : null,
                username,
                phone));
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

    public record CustomerAccount(
            long id,
            long subjectId,
            String username,
            String nickname,
            String phone,
            String email,
            String status) {
    }

    public record CreateCustomerCommand(
            String username,
            String nickname,
            String phone,
            String email,
            String password,
            boolean mustChangePassword) {
    }
}
