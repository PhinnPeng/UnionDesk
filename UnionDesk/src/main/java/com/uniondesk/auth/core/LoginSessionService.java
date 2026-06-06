package com.uniondesk.auth.core;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class LoginSessionService {

    private static final String SESSION_TYPE_LOGIN = "login";
    private static final String SESSION_TYPE_STEP_UP = "step_up";
    private static final String SESSION_TYPE_PASSWORD_RESET = "password_reset";

    private final JdbcTemplate jdbcTemplate;
    private final Clock clock;
    private final LoginConfigService loginConfigService;

    public LoginSessionService(JdbcTemplate jdbcTemplate, Clock clock, LoginConfigService loginConfigService) {
        this.jdbcTemplate = jdbcTemplate;
        this.clock = clock;
        this.loginConfigService = loginConfigService;
    }

    public String createSession(CreateSessionCommand command) {
        LocalDateTime now = LocalDateTime.now(clock);
        jdbcTemplate.update("""
                        INSERT INTO auth_login_session (
                            sid, session_type, account_type, user_id, client_code, role_code, business_domain_id,
                            login_identifier_masked, session_status, issued_at, expires_at, last_seen_at,
                            refresh_token_hash, client_ip, user_agent
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'active', ?, ?, ?, ?, ?, ?)
                        """,
                command.sid(),
                SESSION_TYPE_LOGIN,
                command.accountType(),
                command.userId(),
                command.clientCode(),
                command.roleCode(),
                command.businessDomainId(),
                command.loginIdentifierMasked(),
                now,
                command.expiresAt(),
                now,
                command.refreshTokenHash(),
                command.clientIp(),
                command.userAgent());
        return command.sid();
    }

    public String createStepUpToken(CreateStepUpTokenCommand command) {
        LocalDateTime now = LocalDateTime.now(clock);
        jdbcTemplate.update("""
                        INSERT INTO auth_login_session (
                            sid, session_type, account_type, user_id, client_code, role_code, business_domain_id,
                            login_identifier_masked, session_status, issued_at, expires_at, last_seen_at,
                            refresh_token_hash, client_ip, user_agent
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'active', ?, ?, ?, NULL, ?, ?)
                        """,
                command.token(),
                SESSION_TYPE_STEP_UP,
                command.accountType(),
                command.userId(),
                command.clientCode(),
                command.roleCode(),
                command.businessDomainId(),
                command.loginIdentifierMasked(),
                now,
                command.expiresAt(),
                now,
                command.clientIp(),
                command.userAgent());
        return command.token();
    }

    public String createPasswordResetToken(CreatePasswordResetTokenCommand command) {
        LocalDateTime now = LocalDateTime.now(clock);
        jdbcTemplate.update("""
                        INSERT INTO auth_login_session (
                            sid, session_type, account_type, user_id, client_code, role_code, business_domain_id,
                            login_identifier_masked, session_status, issued_at, expires_at, last_seen_at,
                            refresh_token_hash, client_ip, user_agent
                        )
                        VALUES (?, ?, ?, ?, ?, NULL, NULL, ?, 'active', ?, ?, ?, NULL, ?, ?)
                        """,
                command.token(),
                SESSION_TYPE_PASSWORD_RESET,
                command.accountType(),
                command.accountId(),
                command.clientCode(),
                command.loginIdentifierMasked(),
                now,
                command.expiresAt(),
                now,
                command.clientIp(),
                command.userAgent());
        return command.token();
    }

    public boolean validateStepUpToken(String token, long userId, String clientCode) {
        if (!StringUtils.hasText(token) || !StringUtils.hasText(clientCode)) {
            return false;
        }
        return validateToken(token, userId, clientCode, SESSION_TYPE_STEP_UP);
    }

    public PasswordResetToken consumePasswordResetToken(String token) {
        if (!StringUtils.hasText(token)) {
            return null;
        }
        try {
            PasswordResetToken resetToken = jdbcTemplate.queryForObject("""
                            SELECT sid, user_id, account_type, client_code, expires_at, session_status, login_identifier_masked
                            FROM auth_login_session
                            WHERE sid = ?
                              AND session_type = ?
                            LIMIT 1
                            """,
                    (rs, rowNum) -> new PasswordResetToken(
                            rs.getString("sid"),
                            rs.getLong("user_id"),
                            rs.getString("account_type"),
                            rs.getString("client_code"),
                            rs.getTimestamp("expires_at").toLocalDateTime(),
                            rs.getString("session_status"),
                            rs.getString("login_identifier_masked")),
                    token,
                    SESSION_TYPE_PASSWORD_RESET);
            if (resetToken == null) {
                return null;
            }
            LocalDateTime now = LocalDateTime.now(clock);
            if (!"active".equalsIgnoreCase(resetToken.sessionStatus()) || !resetToken.expiresAt().isAfter(now)) {
                return null;
            }
            jdbcTemplate.update("""
                            UPDATE auth_login_session
                            SET session_status = 'revoked',
                                revoked_at = ?,
                                revoked_reason = 'password_reset_used'
                            WHERE sid = ?
                              AND session_type = ?
                              AND session_status = 'active'
                            """,
                    now,
                    token,
                    SESSION_TYPE_PASSWORD_RESET);
            return resetToken;
        } catch (EmptyResultDataAccessException ex) {
            return null;
        }
    }

    public boolean validateAndTouch(String sid, String clientCode) {
        return validateAndTouchInternal(sid, clientCode, SESSION_TYPE_LOGIN);
    }

    public boolean validateAndTouch(String sid) {
        return validateAndTouchInternal(sid, null, SESSION_TYPE_LOGIN);
    }

    public int revokeSession(String sid, String reason) {
        LocalDateTime now = LocalDateTime.now(clock);
        return jdbcTemplate.update("""
                        UPDATE auth_login_session
                        SET session_status = 'revoked',
                            revoked_at = ?,
                            revoked_reason = ?
                        WHERE sid = ?
                          AND session_status = 'active'
                          AND session_type = ?
                        """,
                now,
                reason,
                sid,
                SESSION_TYPE_LOGIN);
    }

    public int revokeSessionsByUser(long userId, String reason) {
        LocalDateTime now = LocalDateTime.now(clock);
        return jdbcTemplate.update("""
                        UPDATE auth_login_session
                        SET session_status = 'revoked',
                            revoked_at = ?,
                            revoked_reason = ?
                        WHERE user_id = ?
                          AND session_status = 'active'
                          AND session_type = ?
                        """,
                now,
                reason,
                userId,
                SESSION_TYPE_LOGIN);
    }

    public List<OnlineSession> listOnlineSessions(int limit) {
        int cappedLimit = Math.max(1, Math.min(limit, 500));
        return jdbcTemplate.query("""
                        SELECT
                            s.sid,
                            s.user_id,
                            s.client_code,
                            COALESCE(sa.username, c.username, c.nickname, CONCAT(s.account_type, '-', s.user_id)) AS username,
                            COALESCE(sa.phone, c.phone) AS mobile,
                            COALESCE(sa.email, c.email) AS email,
                            s.role_code,
                            s.business_domain_id,
                            s.login_identifier_masked,
                            s.session_status,
                            s.issued_at,
                            s.expires_at,
                            s.last_seen_at,
                            s.client_ip,
                            s.user_agent
                        FROM auth_login_session s
                        LEFT JOIN staff_account sa ON s.account_type = 'staff' AND sa.id = s.user_id
                        LEFT JOIN customer_account c ON s.account_type = 'customer' AND c.id = s.user_id
                        WHERE s.session_status = 'active'
                          AND s.session_type = ?
                          AND s.expires_at > CURRENT_TIMESTAMP(3)
                        ORDER BY COALESCE(s.last_seen_at, s.issued_at) DESC, s.issued_at DESC
                        LIMIT ?
                        """,
                (rs, rowNum) -> new OnlineSession(
                        rs.getString("sid"),
                        rs.getLong("user_id"),
                        rs.getString("client_code"),
                        rs.getString("username"),
                        rs.getString("mobile"),
                        rs.getString("email"),
                        rs.getString("role_code"),
                        rs.getObject("business_domain_id", Long.class),
                        rs.getString("login_identifier_masked"),
                        rs.getString("session_status"),
                        toLocalDateTime(rs.getTimestamp("issued_at")),
                        toLocalDateTime(rs.getTimestamp("expires_at")),
                        toLocalDateTime(rs.getTimestamp("last_seen_at")),
                        rs.getString("client_ip"),
                        rs.getString("user_agent")),
                SESSION_TYPE_LOGIN,
                cappedLimit);
    }

    private boolean validateToken(String token, long userId, String clientCode, String sessionType) {
        try {
            SessionState state = jdbcTemplate.queryForObject("""
                            SELECT session_status, expires_at, client_code, user_id, account_type
                            FROM auth_login_session
                            WHERE sid = ?
                              AND session_type = ?
                            """,
                    (rs, rowNum) -> new SessionState(
                            rs.getString("session_status"),
                            rs.getTimestamp("expires_at").toLocalDateTime(),
                            rs.getString("client_code"),
                            rs.getLong("user_id"),
                            rs.getString("account_type")),
                    token,
                    sessionType);
            if (state == null) {
                return false;
            }
            LocalDateTime now = LocalDateTime.now(clock);
            if (!"active".equalsIgnoreCase(state.sessionStatus())) {
                return false;
            }
            if (state.userId() != userId) {
                return false;
            }
            if (!clientCode.equalsIgnoreCase(state.clientCode())) {
                return false;
            }
            if (!state.expiresAt().isAfter(now)) {
                return false;
            }
            jdbcTemplate.update("""
                            UPDATE auth_login_session
                            SET last_seen_at = ?
                            WHERE sid = ?
                              AND session_type = ?
                              AND session_status = 'active'
                            """,
                    now,
                    token,
                    sessionType);
            return true;
        } catch (EmptyResultDataAccessException ex) {
            return false;
        }
    }

    private boolean validateAndTouchInternal(String sid, String clientCode, String sessionType) {
        if (!StringUtils.hasText(sid)) {
            return false;
        }
        try {
            SessionState state = jdbcTemplate.queryForObject("""
                            SELECT session_status, expires_at, client_code, user_id, account_type
                            FROM auth_login_session
                            WHERE sid = ?
                              AND session_type = ?
                            """,
                    (rs, rowNum) -> new SessionState(
                            rs.getString("session_status"),
                            rs.getTimestamp("expires_at").toLocalDateTime(),
                            rs.getString("client_code"),
                            rs.getLong("user_id"),
                            rs.getString("account_type")),
                    sid,
                    sessionType);
            if (state == null) {
                return false;
            }
            LocalDateTime now = LocalDateTime.now(clock);
            if (clientCode != null && !clientCode.equalsIgnoreCase(state.clientCode())) {
                return false;
            }
            if (!"active".equalsIgnoreCase(state.sessionStatus())) {
                return false;
            }
            if (!state.expiresAt().isAfter(now)) {
                jdbcTemplate.update("""
                                UPDATE auth_login_session
                                SET session_status = 'expired',
                                    revoked_at = ?,
                                    revoked_reason = 'expired'
                                WHERE sid = ?
                                  AND session_type = ?
                                  AND session_status = 'active'
                                """,
                                now,
                                sid,
                                sessionType);
                return false;
            }
            LocalDateTime newExpiresAt = now.plusSeconds(loginConfigService.loadConfig().sessionTtlSeconds());
            jdbcTemplate.update("""
                            UPDATE auth_login_session
                            SET last_seen_at = ?,
                                expires_at = ?
                            WHERE sid = ?
                              AND session_type = ?
                              AND session_status = 'active'
                            """,
                    now,
                    newExpiresAt,
                    sid,
                    sessionType);
            return true;
        } catch (EmptyResultDataAccessException ex) {
            return false;
        }
    }

    private LocalDateTime toLocalDateTime(java.sql.Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private record SessionState(
            String sessionStatus,
            LocalDateTime expiresAt,
            String clientCode,
            long userId,
            String accountType) {
    }

    public record CreateSessionCommand(
            String sid,
            long userId,
            String clientCode,
            String accountType,
            String roleCode,
            Long businessDomainId,
            String loginIdentifierMasked,
            LocalDateTime expiresAt,
            String refreshTokenHash,
            String clientIp,
            String userAgent) {
    }

    public record CreateStepUpTokenCommand(
            String token,
            long userId,
            String clientCode,
            String accountType,
            String roleCode,
            Long businessDomainId,
            String loginIdentifierMasked,
            LocalDateTime expiresAt,
            String clientIp,
            String userAgent) {
    }

    public record CreatePasswordResetTokenCommand(
            String token,
            long accountId,
            String accountType,
            String clientCode,
            String loginIdentifierMasked,
            LocalDateTime expiresAt,
            String clientIp,
            String userAgent) {
    }

    public record PasswordResetToken(
            String token,
            long accountId,
            String accountType,
            String clientCode,
            LocalDateTime expiresAt,
            String sessionStatus,
            String loginIdentifierMasked) {
    }

    public record OnlineSession(
            String sid,
            long userId,
            String clientCode,
            String username,
            String mobile,
            String email,
            String roleCode,
            Long businessDomainId,
            String loginIdentifierMasked,
            String sessionStatus,
            LocalDateTime issuedAt,
            LocalDateTime expiresAt,
            LocalDateTime lastSeenAt,
            String clientIp,
            String userAgent) {
    }
}
