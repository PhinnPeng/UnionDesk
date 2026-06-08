package com.uniondesk.auth.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.uniondesk.auth.entity.AuthLoginSessionPo;
import com.uniondesk.auth.repository.LoginSessionRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class LoginSessionServiceTest {

    @Test
    void validateAndTouchRefreshesExpiresAtWithSlidingWindow() {
        Clock clock = Clock.fixed(Instant.parse("2026-05-09T04:00:00Z"), ZoneOffset.UTC);
        LoginSessionRepository loginSessionRepository = mock(LoginSessionRepository.class);
        LoginConfigService loginConfigService = mock(LoginConfigService.class);
        LoginSessionService service = new LoginSessionService(loginSessionRepository, clock, loginConfigService);
        LocalDateTime now = LocalDateTime.of(2026, 5, 9, 4, 0);
        LocalDateTime currentExpiresAt = now.plusHours(1);
        LocalDateTime expectedExpiresAt = now.plusSeconds(7_200);
        AuthLoginSessionPo activeState = new AuthLoginSessionPo();
        activeState.setSessionStatus("active");
        activeState.setExpiresAt(currentExpiresAt);
        activeState.setClientCode("ud-admin-web");
        activeState.setUserId(1L);
        activeState.setAccountType("admin");

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
        when(loginSessionRepository.findBySidAndType("sid-1", "login")).thenReturn(Optional.of(activeState));

        boolean valid = service.validateAndTouch("sid-1", "ud-admin-web");

        assertThat(valid).isTrue();
        ArgumentCaptor<LocalDateTime> expiresCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(loginSessionRepository).updateLastSeenAndExpires(
                eq("sid-1"),
                eq("login"),
                eq(now),
                expiresCaptor.capture());
        assertThat(expiresCaptor.getValue()).isEqualTo(expectedExpiresAt);
    }
}
