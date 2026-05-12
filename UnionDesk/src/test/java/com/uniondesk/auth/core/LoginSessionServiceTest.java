package com.uniondesk.auth.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Constructor;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

class LoginSessionServiceTest {

    @Test
    void validateAndTouchRefreshesExpiresAtWithSlidingWindow() throws Exception {
        Clock clock = Clock.fixed(Instant.parse("2026-05-09T04:00:00Z"), ZoneOffset.UTC);
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        LoginConfigService loginConfigService = mock(LoginConfigService.class);
        LoginSessionService service = new LoginSessionService(jdbcTemplate, clock, loginConfigService);
        LocalDateTime now = LocalDateTime.of(2026, 5, 9, 4, 0);
        LocalDateTime currentExpiresAt = now.plusHours(1);
        LocalDateTime expectedExpiresAt = now.plusSeconds(7_200);
        Object activeState = newSessionState("active", currentExpiresAt, "ud-admin-web", 1L, "admin");

        when(loginConfigService.loadConfig()).thenReturn(new LoginConfigService.LoginConfig(
                true,
                true,
                true,
                true,
                false,
                false,
                null,
                null,
                7_200,
                10,
                LocalDateTime.of(2026, 5, 1, 0, 0)));
        when(jdbcTemplate.queryForObject(
                anyString(),
                org.mockito.ArgumentMatchers.<RowMapper>any(),
                any(Object[].class)))
                .thenReturn(activeState);

        boolean valid = service.validateAndTouch("sid-1", "ud-admin-web");

        assertThat(valid).isTrue();
        ArgumentCaptor<Object[]> argsCaptor = ArgumentCaptor.forClass(Object[].class);
        verify(jdbcTemplate).update(contains("expires_at = ?"), argsCaptor.capture());
        assertThat(argsCaptor.getValue()).containsExactly(
                now,
                expectedExpiresAt,
                "sid-1",
                "login");
    }

    private static Object newSessionState(
            String sessionStatus,
            LocalDateTime expiresAt,
            String clientCode,
            long userId,
            String accountType) throws Exception {
        Class<?> stateClass = Class.forName("com.uniondesk.auth.core.LoginSessionService$SessionState");
        Constructor<?> constructor = stateClass.getDeclaredConstructor(
                String.class,
                LocalDateTime.class,
                String.class,
                long.class,
                String.class);
        constructor.setAccessible(true);
        return constructor.newInstance(sessionStatus, expiresAt, clientCode, userId, accountType);
    }
}
