package com.uniondesk.auth.core;

import com.uniondesk.auth.entity.AuthLoginConfigPo;
import com.uniondesk.auth.repository.LoginConfigRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class LoginConfigService {

    private static final String PASSWORD_LOGIN_ENABLED = "password_login_enabled";
    private static final String USERNAME_LOGIN_ENABLED = "username_login_enabled";
    private static final String EMAIL_LOGIN_ENABLED = "email_login_enabled";
    private static final String MOBILE_LOGIN_ENABLED = "mobile_login_enabled";
    private static final String CAPTCHA_ENABLED = "captcha_enabled";
    private static final String WECHAT_LOGIN_ENABLED = "wechat_login_enabled";
    private static final String CAPTCHA_HINT = "captcha_hint";
    private static final String WECHAT_HINT = "wechat_hint";
    private static final String SESSION_TTL_SECONDS = "session_ttl_seconds";
    private static final String MAX_ACTIVE_SESSIONS_PER_USER = "max_active_sessions_per_user";

    private final LoginConfigRepository loginConfigRepository;
    private final Clock clock;

    public LoginConfigService(LoginConfigRepository loginConfigRepository, Clock clock) {
        this.loginConfigRepository = loginConfigRepository;
        this.clock = clock;
    }

    public LoginConfig loadConfig() {
        Map<String, String> values = new HashMap<>();
        List<AuthLoginConfigPo> rows = loginConfigRepository.findAll();
        for (AuthLoginConfigPo row : rows) {
            values.put(row.getConfigKey(), row.getConfigValue());
        }
        return new LoginConfig(
                getBoolean(values, PASSWORD_LOGIN_ENABLED, true),
                getBoolean(values, USERNAME_LOGIN_ENABLED, true),
                getBoolean(values, EMAIL_LOGIN_ENABLED, true),
                getBoolean(values, MOBILE_LOGIN_ENABLED, true),
                getBoolean(values, CAPTCHA_ENABLED, false),
                getBoolean(values, WECHAT_LOGIN_ENABLED, false),
                getString(values, CAPTCHA_HINT, null),
                getString(values, WECHAT_HINT, null),
                getInt(values, SESSION_TTL_SECONDS, 7 * 24 * 60 * 60),
                getInt(values, MAX_ACTIVE_SESSIONS_PER_USER, 10),
                latestUpdatedAt());
    }

    public LoginConfig updateConfig(UpdateLoginConfigCommand command) {
        if (command.passwordLoginEnabled() != null) {
            loginConfigRepository.upsert(PASSWORD_LOGIN_ENABLED, command.passwordLoginEnabled().toString());
        }
        if (command.usernameLoginEnabled() != null) {
            loginConfigRepository.upsert(USERNAME_LOGIN_ENABLED, command.usernameLoginEnabled().toString());
        }
        if (command.emailLoginEnabled() != null) {
            loginConfigRepository.upsert(EMAIL_LOGIN_ENABLED, command.emailLoginEnabled().toString());
        }
        if (command.mobileLoginEnabled() != null) {
            loginConfigRepository.upsert(MOBILE_LOGIN_ENABLED, command.mobileLoginEnabled().toString());
        }
        if (command.captchaEnabled() != null) {
            loginConfigRepository.upsert(CAPTCHA_ENABLED, command.captchaEnabled().toString());
        }
        if (command.wechatLoginEnabled() != null) {
            loginConfigRepository.upsert(WECHAT_LOGIN_ENABLED, command.wechatLoginEnabled().toString());
        }
        if (command.captchaHint() != null) {
            loginConfigRepository.upsert(CAPTCHA_HINT, command.captchaHint());
        }
        if (command.wechatHint() != null) {
            loginConfigRepository.upsert(WECHAT_HINT, command.wechatHint());
        }
        if (command.sessionTtlSeconds() != null) {
            loginConfigRepository.upsert(SESSION_TTL_SECONDS, command.sessionTtlSeconds().toString());
        }
        if (command.maxActiveSessionsPerUser() != null) {
            loginConfigRepository.upsert(MAX_ACTIVE_SESSIONS_PER_USER, command.maxActiveSessionsPerUser().toString());
        }
        return loadConfig();
    }

    private boolean getBoolean(Map<String, String> values, String key, boolean defaultValue) {
        String value = values.get(key);
        return StringUtils.hasText(value) ? Boolean.parseBoolean(value) : defaultValue;
    }

    private int getInt(Map<String, String> values, String key, int defaultValue) {
        String value = values.get(key);
        if (!StringUtils.hasText(value)) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    private String getString(Map<String, String> values, String key, String defaultValue) {
        String value = values.get(key);
        return StringUtils.hasText(value) ? value : defaultValue;
    }

    private LocalDateTime latestUpdatedAt() {
        LocalDateTime updatedAt = loginConfigRepository.findMaxUpdatedAt();
        if (updatedAt != null) {
            return updatedAt;
        }
        return LocalDateTime.now(clock.withZone(ZoneId.of("UTC")));
    }

    public record LoginConfig(
            boolean passwordLoginEnabled,
            boolean usernameLoginEnabled,
            boolean emailLoginEnabled,
            boolean mobileLoginEnabled,
            boolean captchaEnabled,
            boolean wechatLoginEnabled,
            String captchaHint,
            String wechatHint,
            int sessionTtlSeconds,
            int maxActiveSessionsPerUser,
            LocalDateTime updatedAt) {
    }

    public record UpdateLoginConfigCommand(
            Boolean passwordLoginEnabled,
            Boolean usernameLoginEnabled,
            Boolean emailLoginEnabled,
            Boolean mobileLoginEnabled,
            Boolean captchaEnabled,
            Boolean wechatLoginEnabled,
            String captchaHint,
            String wechatHint,
            Integer sessionTtlSeconds,
            Integer maxActiveSessionsPerUser) {
    }
}
