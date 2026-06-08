package com.uniondesk.auth.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

class JwtAuthenticationFilterTests {

    private static final String SECRET = "uniondesk-demo-jwt-secret-please-change-me";

    private final JwtTokenService jwtTokenService = new JwtTokenService(
            new ObjectMapper(),
            SECRET,
            "uniondesk",
            Duration.ofHours(24),
            Duration.ofDays(7));

    private final LoginSessionService loginSessionService = mock(LoginSessionService.class);
    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(
            jwtTokenService, loginSessionService, new ObjectMapper());

    @AfterEach
    void cleanup() {
        UserContextHolder.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void populatesUserContextAndAuthenticationForBearerToken() throws ServletException, IOException {
        UserContext expected = new UserContext(7L, "agent", 11L, "sid-7", "ud-admin-web");
        String token = jwtTokenService.issueAccessToken(expected);
        when(loginSessionService.validateAndTouch("sid-7", "ud-admin-web")).thenReturn(true);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/tickets");
        request.addHeader("Authorization", "Bearer " + token);
        request.addHeader(AuthClientHeaders.CLIENT_CODE_HEADER, "ud-admin-web");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<Authentication> authenticationRef = new AtomicReference<>();
        AtomicReference<UserContext> contextRef = new AtomicReference<>();
        FilterChain chain = (req, res) -> {
            authenticationRef.set(SecurityContextHolder.getContext().getAuthentication());
            contextRef.set(UserContextHolder.current().orElse(null));
        };

        filter.doFilter(request, response, chain);

        assertThat(authenticationRef.get()).isNotNull();
        assertThat(authenticationRef.get().getPrincipal()).isEqualTo(expected);
        assertThat(authenticationRef.get().getAuthorities()).extracting("authority")
                .containsExactly("ROLE_AGENT");
        assertThat(contextRef.get()).isEqualTo(expected);
    }

    @Test
    void leavesAnonymousRequestUnauthenticated() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/health");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<Authentication> authenticationRef = new AtomicReference<>();
        AtomicReference<UserContext> contextRef = new AtomicReference<>();
        FilterChain chain = (req, res) -> {
            authenticationRef.set(SecurityContextHolder.getContext().getAuthentication());
            contextRef.set(UserContextHolder.current().orElse(null));
        };

        filter.doFilter(request, response, chain);

        assertThat(authenticationRef.get()).isNull();
        assertThat(contextRef.get()).isNull();
    }

    @Test
    void allowsAnonymousLoginConfigRequest() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/auth/login-config");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<Boolean> chainInvoked = new AtomicReference<>(false);
        AtomicReference<Authentication> authenticationRef = new AtomicReference<>();
        AtomicReference<UserContext> contextRef = new AtomicReference<>();
        FilterChain chain = (req, res) -> {
            chainInvoked.set(true);
            authenticationRef.set(SecurityContextHolder.getContext().getAuthentication());
            contextRef.set(UserContextHolder.current().orElse(null));
        };

        filter.doFilter(request, response, chain);

        assertThat(chainInvoked.get()).isTrue();
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(authenticationRef.get()).isNull();
        assertThat(contextRef.get()).isNull();
    }

    @Test
    void leavesProtectedRequestUnauthenticatedWhenTokenMissing() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/tickets");
        request.addHeader(AuthClientHeaders.CLIENT_CODE_HEADER, "ud-admin-web");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<Authentication> authenticationRef = new AtomicReference<>();
        AtomicReference<UserContext> contextRef = new AtomicReference<>();
        FilterChain chain = (req, res) -> {
            authenticationRef.set(SecurityContextHolder.getContext().getAuthentication());
            contextRef.set(UserContextHolder.current().orElse(null));
        };

        filter.doFilter(request, response, chain);

        assertThat(authenticationRef.get()).isNull();
        assertThat(contextRef.get()).isNull();
    }

    @Test
    void rejectsRevokedSessionSid() throws ServletException, IOException {
        UserContext expected = new UserContext(7L, "agent", 11L, "sid-revoked", "ud-admin-web");
        String token = jwtTokenService.issueAccessToken(expected);
        when(loginSessionService.validateAndTouch("sid-revoked", "ud-admin-web")).thenReturn(false);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/tickets");
        request.addHeader("Authorization", "Bearer " + token);
        request.addHeader(AuthClientHeaders.CLIENT_CODE_HEADER, "ud-admin-web");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<Authentication> authenticationRef = new AtomicReference<>();
        AtomicReference<UserContext> contextRef = new AtomicReference<>();
        FilterChain chain = (req, res) -> {
            authenticationRef.set(SecurityContextHolder.getContext().getAuthentication());
            contextRef.set(UserContextHolder.current().orElse(null));
        };

        filter.doFilter(request, response, chain);

        assertThat(authenticationRef.get()).isNull();
        assertThat(contextRef.get()).isNull();
    }

    @Test
    void rejectsProtectedRequestWithoutClientCodeHeader() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/tickets");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<Boolean> chainInvoked = new AtomicReference<>(false);
        FilterChain chain = (req, res) -> chainInvoked.set(true);

        filter.doFilter(request, response, chain);

        assertThat(chainInvoked.get()).isFalse();
        assertThat(response.getStatus()).isEqualTo(401);
    }
}
