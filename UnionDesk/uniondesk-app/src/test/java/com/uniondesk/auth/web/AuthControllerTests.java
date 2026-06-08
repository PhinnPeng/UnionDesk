package com.uniondesk.auth.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.uniondesk.auth.core.AuthService;
import com.uniondesk.auth.core.AuthCaptchaService;
import com.uniondesk.auth.core.AuthenticationFailedException;
import com.uniondesk.auth.core.LoginConfigService.LoginConfig;
import com.uniondesk.auth.core.LoginSessionService.OnlineSession;
import com.uniondesk.auth.core.UserContext;
import com.uniondesk.auth.core.UserContextHolder;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AuthControllerTests {

    @AfterEach
    void clearUserContext() {
        UserContextHolder.clear();
    }

    @Test
    void captchaChallengeReturnsChallengeId() throws Exception {
        AuthService authService = mock(AuthService.class);
        AuthCaptchaService authCaptchaService = mock(AuthCaptchaService.class);
        when(authCaptchaService.createChallenge())
                .thenReturn(new AuthCaptchaService.CaptchaChallenge("challenge-1", 120));
        MockMvc mockMvc = mockMvc(authService, authCaptchaService);

        mockMvc.perform(post("/api/v1/auth/captcha/challenge"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.challengeId").value("challenge-1"))
                .andExpect(jsonPath("$.expiresInSeconds").value(120));
    }

    @Test
    void captchaVerifyReturnsToken() throws Exception {
        AuthService authService = mock(AuthService.class);
        AuthCaptchaService authCaptchaService = mock(AuthCaptchaService.class);
        when(authCaptchaService.verify(
                org.mockito.ArgumentMatchers.eq("challenge-1"),
                org.mockito.ArgumentMatchers.anyList()))
                .thenReturn(new AuthCaptchaService.CaptchaToken("captcha-token-1", 120));
        MockMvc mockMvc = mockMvc(authService, authCaptchaService);

        mockMvc.perform(post("/api/v1/auth/captcha/verify")
                        .contentType("application/json")
                        .content("""
                                {
                                  "challengeId": "challenge-1",
                                  "track": [
                                    {"x": 0, "t": 0},
                                    {"x": 320, "t": 900}
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.captchaToken").value("captcha-token-1"))
                .andExpect(jsonPath("$.expiresInSeconds").value(120));
    }

    @Test
    void loginFailureMapsToUnauthorized() throws Exception {
        AuthService authService = org.mockito.Mockito.mock(AuthService.class);
        when(authService.login(
                org.mockito.ArgumentMatchers.any(AuthDtos.LoginRequest.class),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString()))
                .thenThrow(new AuthenticationFailedException("invalid credentials"));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(authService, mock(AuthCaptchaService.class)))
                .setControllerAdvice(new com.uniondesk.common.web.ApiExceptionHandler())
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .header("X-UD-Client-Code", "ud-customer-web")
                        .header("User-Agent", "JUnit")
                        .content("{\"username\":\"customer\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("10001"))
                .andExpect(jsonPath("$.message").value("用户名或密码错误"));
    }

    @Test
    void registerReturnsTokensOnSuccess() throws Exception {
        AuthService authService = mock(AuthService.class);
        when(authService.register(
                any(AuthDtos.RegisterRequest.class),
                org.mockito.ArgumentMatchers.eq("ud-customer-web"),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(new AuthDtos.RegisterResponse("access-token-1", "refresh-token-1", 101L));
        MockMvc mockMvc = mockMvc(authService);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType("application/json")
                        .header("X-UD-Client-Code", "ud-customer-web")
                        .header("User-Agent", "JUnit")
                        .content("""
                                {
                                  "loginName": "new-user",
                                  "password": "password123",
                                  "phone": "13800000001",
                                  "email": "new-user@uniondesk.local"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token-1"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token-1"))
                .andExpect(jsonPath("$.accountId").value(101));
    }

    @Test
    void registerWithMissingFieldsReturnsBadRequest() throws Exception {
        MockMvc mockMvc = mockMvc(mock(AuthService.class));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType("application/json")
                        .header("X-UD-Client-Code", "ud-customer-web")
                        .header("User-Agent", "JUnit")
                        .content("""
                                {
                                  "loginName": "new-user"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void requestPasswordResetReturnsChannel() throws Exception {
        AuthService authService = mock(AuthService.class);
        when(authService.requestPasswordReset(
                any(AuthDtos.PasswordResetRequest.class),
                org.mockito.ArgumentMatchers.eq("ud-customer-web"),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(new AuthDtos.PasswordResetResponse("email", "c***@uniondesk.local"));
        MockMvc mockMvc = mockMvc(authService);

        mockMvc.perform(post("/api/v1/auth/password/reset-request")
                        .contentType("application/json")
                        .header("X-UD-Client-Code", "ud-customer-web")
                        .header("User-Agent", "JUnit")
                        .content("""
                                {
                                  "loginName": "admin",
                                  "portalType": "ud-customer-web"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.channel").value("email"))
                .andExpect(jsonPath("$.hint").value("c***@uniondesk.local"));
    }

    @Test
    void changePasswordWithoutAuthReturnsUnauthorized() throws Exception {
        MockMvc mockMvc = mockMvc(mock(AuthService.class));

        mockMvc.perform(put("/api/v1/auth/password")
                        .contentType("application/json")
                        .content("""
                                {
                                  "oldPassword": "old-password",
                                  "newPassword": "new-password"
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void sessionReturnsCurrentContext() throws Exception {
        AuthService authService = mock(AuthService.class);
        MockMvc mockMvc = mockMvc(authService);
        UserContextHolder.set(new UserContext(2L, "super_admin", 10L, "sid-123", "ud-admin-web"));
        when(authService.currentSession(any(UserContext.class)))
                .thenReturn(new AuthDtos.SessionView(2L, "super_admin", 10L, "sid-123", "ud-admin-web"));

        mockMvc.perform(get("/api/v1/auth/session"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(2))
                .andExpect(jsonPath("$.role").value("super_admin"))
                .andExpect(jsonPath("$.businessDomainId").value(10))
                .andExpect(jsonPath("$.sid").value("sid-123"))
                .andExpect(jsonPath("$.clientCode").value("ud-admin-web"));
    }

    @Test
    void sessionWithoutAuthenticationReturnsUnauthorized() throws Exception {
        MockMvc mockMvc = mockMvc(mock(AuthService.class));

        mockMvc.perform(get("/api/v1/auth/session"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("40101"))
                .andExpect(jsonPath("$.message").value("未登录或登录已过期"));
    }

    @Test
    void updateLoginConfigReturnsNewFlagsForAdmin() throws Exception {
        AuthService authService = mock(AuthService.class);
        MockMvc mockMvc = mockMvc(authService);
        UserContextHolder.set(new UserContext(2L, "super_admin", 10L, "sid-123", "ud-admin-web"));
        when(authService.updateConfig(any()))
                .thenReturn(new LoginConfig(
                        true,
                        true,
                        true,
                        true,
                        true,
                        true,
                        "captcha-hint",
                        "wechat-hint",
                        7200,
                        5,
                        LocalDateTime.parse("2026-04-21T08:00:00")));

        mockMvc.perform(put("/api/v1/auth/login-config")
                        .contentType("application/json")
                        .content("""
                                {
                                  "captchaEnabled": true,
                                  "wechatLoginEnabled": true,
                                  "captchaHint": "captcha-hint",
                                  "wechatHint": "wechat-hint"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.captchaEnabled").value(true))
                .andExpect(jsonPath("$.wechatLoginEnabled").value(true))
                .andExpect(jsonPath("$.captchaHint").value("captcha-hint"))
                .andExpect(jsonPath("$.wechatHint").value("wechat-hint"));
        verify(authService).updateConfig(any());
    }

    @Test
    void updateLoginConfigReturnsUpdatedFlags() throws Exception {
        AuthService authService = mock(AuthService.class);
        MockMvc mockMvc = mockMvc(authService);
        UserContextHolder.set(new UserContext(1L, "customer", 10L, "sid-123", "ud-customer-web"));
        when(authService.updateConfig(any()))
                .thenReturn(new LoginConfig(
                        true,
                        true,
                        true,
                        true,
                        true,
                        true,
                        "captcha-hint",
                        "wechat-hint",
                        7200,
                        5,
                        LocalDateTime.parse("2026-04-21T08:00:00")));

        mockMvc.perform(put("/api/v1/auth/login-config")
                        .contentType("application/json")
                        .content("{\"captchaEnabled\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.captchaEnabled").value(true))
                .andExpect(jsonPath("$.wechatLoginEnabled").value(true));
        verify(authService).updateConfig(any());
    }

    @Test
    void listOnlineSessionsReturnsRowsForAdmin() throws Exception {
        AuthService authService = mock(AuthService.class);
        MockMvc mockMvc = mockMvc(authService);
        UserContextHolder.set(new UserContext(2L, "super_admin", 10L, "sid-123", "ud-admin-web"));
        when(authService.listOnlineSessions(25)).thenReturn(List.of(new OnlineSession(
                "sid-1",
                1L,
                "ud-customer-web",
                "customer",
                "13800000000",
                "customer@uniondesk.local",
                "customer",
                10L,
                "c***r",
                "active",
                LocalDateTime.parse("2026-04-21T08:00:00"),
                LocalDateTime.parse("2026-04-28T08:00:00"),
                LocalDateTime.parse("2026-04-21T08:05:00"),
                "127.0.0.1",
                "JUnit")));

        mockMvc.perform(get("/api/v1/auth/online-sessions").param("limit", "25"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sid").value("sid-1"))
                .andExpect(jsonPath("$[0].username").value("customer"))
                .andExpect(jsonPath("$[0].clientCode").value("ud-customer-web"))
                .andExpect(jsonPath("$[0].sessionStatus").value("active"));
    }

    @Test
    void revokeSessionReturnsNotFoundWhenSidMissing() throws Exception {
        AuthService authService = mock(AuthService.class);
        MockMvc mockMvc = mockMvc(authService);
        UserContextHolder.set(new UserContext(2L, "super_admin", 10L, "sid-123", "ud-admin-web"));
        when(authService.revokeSession("sid-404", "admin_revoke", "127.0.0.1", "JUnit")).thenReturn(0);

        mockMvc.perform(post("/api/v1/auth/online-sessions/sid-404/revoke")
                        .header("User-Agent", "JUnit"))
                .andExpect(status().isNotFound());
    }

    @Test
    void revokeSessionsByUserSucceedsForAdmin() throws Exception {
        AuthService authService = mock(AuthService.class);
        MockMvc mockMvc = mockMvc(authService);
        UserContextHolder.set(new UserContext(2L, "super_admin", 10L, "sid-123", "ud-admin-web"));
        when(authService.revokeSessionsByUser(1L, "admin_revoke_all", "127.0.0.1", "JUnit")).thenReturn(2);

        mockMvc.perform(post("/api/v1/auth/users/1/revoke-sessions")
                        .header("User-Agent", "JUnit"))
                .andExpect(status().isOk());
        verify(authService).revokeSessionsByUser(1L, "admin_revoke_all", "127.0.0.1", "JUnit");
    }

    @Test
    void refreshReturnsNewTokenPair() throws Exception {
        AuthService authService = mock(AuthService.class);
        when(authService.refreshToken("old-refresh-token"))
                .thenReturn(new AuthDtos.RefreshResponse("new-access", "new-refresh", "Bearer", 86400));
        MockMvc mockMvc = mockMvc(authService);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType("application/json")
                        .content("{\"refreshToken\":\"old-refresh-token\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access"))
                .andExpect(jsonPath("$.refreshToken").value("new-refresh"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresInSeconds").value(86400));
    }

    @Test
    void refreshWithInvalidTokenReturnsUnauthorized() throws Exception {
        AuthService authService = mock(AuthService.class);
        when(authService.refreshToken("bad-token"))
                .thenThrow(new com.uniondesk.auth.core.AuthenticationFailedException("invalid refresh token"));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(authService, mock(AuthCaptchaService.class)))
                .setControllerAdvice(new com.uniondesk.common.web.ApiExceptionHandler())
                .build();

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType("application/json")
                        .content("{\"refreshToken\":\"bad-token\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("10001"))
                .andExpect(jsonPath("$.message").value("用户名或密码错误"));
    }

    @Test
    void meReturnsCurrentUserInfo() throws Exception {
        AuthService authService = mock(AuthService.class);
        MockMvc mockMvc = mockMvc(authService);
        UserContextHolder.set(new UserContext(1L, "customer", 10L, "sid-1", "ud-customer-web"));
        when(authService.currentUser(any(UserContext.class)))
                .thenReturn(new AuthDtos.CurrentUserResponse(
                        1L, "customer", "13800000000", "customer@uniondesk.local",
                        "customer", "ud-customer-web", 10L, java.util.List.of("customer"),
                        "1", "1", "customer", "customer", "customer", null, "13800000000",
                        java.util.List.of(), null));

        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.username").value("customer"))
                .andExpect(jsonPath("$.role").value("customer"))
                .andExpect(jsonPath("$.roles[0]").value("customer"));
    }

    @Test
    void meWithoutAuthReturnsUnauthorized() throws Exception {
        MockMvc mockMvc = mockMvc(mock(AuthService.class));

        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("40101"))
                .andExpect(jsonPath("$.message").value("未登录或登录已过期"));
    }

    @Test
    void stepUpReturnsTokenOnSuccess() throws Exception {
        AuthService authService = mock(AuthService.class);
        MockMvc mockMvc = mockMvc(authService);
        UserContextHolder.set(new UserContext(1L, "super_admin", 10L, "sid-1", "ud-admin-web"));
        when(authService.stepUp(any(UserContext.class), org.mockito.ArgumentMatchers.eq("admin123"), any()))
                .thenReturn(new AuthDtos.StepUpResponse("step-up-token-1", "session_15m", 900, "session_15m", null));

        mockMvc.perform(post("/api/v1/auth/step-up")
                        .contentType("application/json")
                        .content("{\"password\":\"admin123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stepUpToken").value("step-up-token-1"))
                .andExpect(jsonPath("$.mode").value("session_15m"))
                .andExpect(jsonPath("$.expiresInSeconds").value(900));
    }

    @Test
    void stepUpWithoutAuthReturnsUnauthorized() throws Exception {
        MockMvc mockMvc = mockMvc(mock(AuthService.class));

        mockMvc.perform(post("/api/v1/auth/step-up")
                        .contentType("application/json")
                        .content("{\"password\":\"admin123\"}"))
                .andExpect(status().isUnauthorized());
    }

    private MockMvc mockMvc(AuthService authService) {
        return mockMvc(authService, mock(AuthCaptchaService.class));
    }

    private MockMvc mockMvc(AuthService authService, AuthCaptchaService authCaptchaService) {
        return MockMvcBuilders.standaloneSetup(new AuthController(authService, authCaptchaService))
                .setControllerAdvice(new com.uniondesk.common.web.ApiExceptionHandler())
                .build();
    }
}
