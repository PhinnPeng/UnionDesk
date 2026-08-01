package com.uniondesk.auth.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uniondesk.auth.core.AuthClientService.AuthClient;
import com.uniondesk.auth.core.LoginAccountService.LoginAccount;
import com.uniondesk.auth.core.LoginConfigService.LoginConfig;
import com.uniondesk.auth.web.AuthDtos;
import com.uniondesk.domain.core.DomainCustomerService;
import com.uniondesk.domain.core.DomainService;
import com.uniondesk.domain.core.InvitationCodeService;
import com.uniondesk.domain.web.DomainDtos;
import com.uniondesk.iam.core.CustomerAccountService;
import com.uniondesk.iam.core.PlatformRoleService;
import com.uniondesk.iam.core.IamService;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTests {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-04-21T00:00:00Z"), ZoneOffset.UTC);

    @Mock
    private LoginAccountService loginAccountService;

    @Mock
    private AuthClientService authClientService;

    @Mock
    private LoginConfigService loginConfigService;

    @Mock
    private LoginSessionService loginSessionService;

    @Mock
    private LoginAuditService loginAuditService;

    @Mock
    private IamService iamService;

    @Mock
    private AuthCaptchaService authCaptchaService;

    @Mock
    private CustomerAccountService customerAccountService;

    @Mock
    private DomainCustomerService domainCustomerService;

    @Mock
    private DomainService domainService;

    @Mock
    private InvitationCodeService invitationCodeService;

    @Mock
    private AuthVersionService authVersionService;

    @Mock
    private PlatformRoleService platformRoleService;

    @Mock
    private UserConfigService userConfigService;

    private final JwtTokenService jwtTokenService = new JwtTokenService(
            new ObjectMapper(),
            "uniondesk-demo-jwt-secret-please-change-me",
            "uniondesk",
            Duration.ofHours(24),
            Duration.ofDays(7));

    private final PasswordEncoder passwordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                loginAccountService,
                authClientService,
                loginConfigService,
                loginSessionService,
                loginAuditService,
                iamService,
                jwtTokenService,
                passwordEncoder,
                authCaptchaService,
                customerAccountService,
                domainCustomerService,
                domainService,
                invitationCodeService,
                authVersionService,
                platformRoleService,
                userConfigService,
                CLOCK);
        org.mockito.Mockito.lenient()
                .when(userConfigService.getPreferredDefaultDomainId(org.mockito.ArgumentMatchers.anyLong()))
                .thenReturn(Optional.empty());
    }

    @Test
    void loginSucceedsWithEmailIdentifierAndCreatesSession() {
        LoginAccount account = new LoginAccount(1L, "customer", "13800000000", "customer@uniondesk.local",
                passwordEncoder.encode("customer123"), 1, "customer", "active");
        LoginConfig config = new LoginConfig(
                true,
                true,
                true,
                true,
                false,
                false,
                null,
                null,
                604800,
                10,
                LocalDateTime.now(CLOCK));

        when(authClientService.findByCode("ud-customer-web")).thenReturn(Optional.of(new AuthClient("ud-customer-web", "customer", 1)));
        when(loginConfigService.loadConfig()).thenReturn(config);
        when(loginAccountService.findByIdentifier("customer@uniondesk.local", LoginIdentifierType.EMAIL, "customer"))
                .thenReturn(Optional.of(account));
        when(loginAccountService.loadAccessibleDomainIds(1L, "customer", null)).thenReturn(List.of(1L));
        stubDomainView(1L);
        when(loginSessionService.createSession(any(LoginSessionService.CreateSessionCommand.class)))
                .thenAnswer(invocation -> invocation.getArgument(0, LoginSessionService.CreateSessionCommand.class).sid());

        AuthDtos.LoginResponse response = authService.login(
                new AuthDtos.LoginRequest("customer@uniondesk.local", "customer123", null, null),
                "ud-customer-web",
                "127.0.0.1",
                "JUnit");

        assertThat(response.sid()).isNotBlank();
        assertThat(response.accessToken()).contains(".");
        assertThat(response.user().username()).isEqualTo("customer");
        assertThat(response.user().roles()).containsExactly("customer");
        assertThat(response.clientCode()).isEqualTo("ud-customer-web");
        assertThat(response.accessToken()).isNotEqualTo(response.refreshToken());
        ArgumentCaptor<LoginSessionService.CreateSessionCommand> captor = ArgumentCaptor.forClass(LoginSessionService.CreateSessionCommand.class);
        verify(loginSessionService).createSession(captor.capture());
        assertThat(captor.getValue().sid()).isEqualTo(response.sid());
        assertThat(captor.getValue().clientCode()).isEqualTo("ud-customer-web");
    }

    @Test
    void loginFailsWithBadPasswordAndDoesNotCreateSession() {
        LoginAccount account = new LoginAccount(1L, "customer", "13800000000", "customer@uniondesk.local",
                passwordEncoder.encode("customer123"), 1, "customer", "active");
        LoginConfig config = new LoginConfig(
                true,
                true,
                true,
                true,
                false,
                false,
                null,
                null,
                604800,
                10,
                LocalDateTime.now(CLOCK));

        when(authClientService.findByCode("ud-customer-web")).thenReturn(Optional.of(new AuthClient("ud-customer-web", "customer", 1)));
        when(loginConfigService.loadConfig()).thenReturn(config);
        when(loginAccountService.findByIdentifier("customer", LoginIdentifierType.USERNAME, "customer"))
                .thenReturn(Optional.of(account));

        assertThatThrownBy(() -> authService.login(
                new AuthDtos.LoginRequest("customer", "wrong-password", null, null),
                "ud-customer-web",
                "127.0.0.1",
                "JUnit"))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage("invalid credentials");
        verify(loginAuditService).record(any());
    }

    @Test
    void loginFailsWhenNoRoleCanAccessClient() {
        LoginAccount account = new LoginAccount(2L, "admin", "13900000000", "admin@uniondesk.local",
                passwordEncoder.encode("admin123"), 1, "admin", "active");
        LoginConfig config = new LoginConfig(
                true,
                true,
                true,
                true,
                false,
                false,
                null,
                null,
                604800,
                10,
                LocalDateTime.now(CLOCK));

        when(authClientService.findByCode("ud-customer-web")).thenReturn(Optional.of(new AuthClient("ud-customer-web", "customer", 1)));
        when(loginConfigService.loadConfig()).thenReturn(config);
        when(loginAccountService.findByIdentifier("admin", LoginIdentifierType.USERNAME, "customer"))
                .thenReturn(Optional.of(account));
        when(iamService.listUserRoleCodesByClient(2L, "ud-customer-web")).thenReturn(List.of("customer"));
        when(loginAccountService.loadAccessibleDomainIds(2L, "customer", null)).thenReturn(List.of(1L));
        stubDomainView(1L);
        when(loginSessionService.createSession(any(LoginSessionService.CreateSessionCommand.class)))
                .thenAnswer(invocation -> invocation.getArgument(0, LoginSessionService.CreateSessionCommand.class).sid());

        AuthDtos.LoginResponse response = authService.login(
                new AuthDtos.LoginRequest("admin", "admin123", null, null),
                "ud-customer-web",
                "127.0.0.1",
                "JUnit");
        assertThat(response.clientCode()).isEqualTo("ud-customer-web");
    }

    @Test
    void loginConsumesCaptchaTokenWhenCaptchaEnabled() {
        LoginAccount account = new LoginAccount(2L, "admin", "13900000000", "admin@uniondesk.local",
                passwordEncoder.encode("admin123"), 1, "admin", "active");
        LoginConfig config = new LoginConfig(
                true,
                true,
                true,
                true,
                true,
                false,
                null,
                null,
                604800,
                10,
                LocalDateTime.now(CLOCK));

        when(authClientService.findByCode("ud-admin-web")).thenReturn(Optional.of(new AuthClient("ud-admin-web", "admin", 1)));
        when(loginConfigService.loadConfig()).thenReturn(config);
        when(loginAccountService.findByIdentifier("admin", LoginIdentifierType.USERNAME, "staff"))
                .thenReturn(Optional.of(account));
        when(iamService.listUserRoleCodesByClient(2L, "ud-admin-web")).thenReturn(List.of("super_admin"));
        when(loginAccountService.loadAccessibleDomainIds(2L, List.of("super_admin"))).thenReturn(List.of(1L));
        stubDomainView(1L);
        when(loginSessionService.createSession(any(LoginSessionService.CreateSessionCommand.class)))
                .thenAnswer(invocation -> invocation.getArgument(0, LoginSessionService.CreateSessionCommand.class).sid());

        AuthDtos.LoginResponse response = authService.login(
                new AuthDtos.LoginRequest("admin", "admin123", "captcha-token-1", null),
                "ud-admin-web",
                "127.0.0.1",
                "JUnit");

        assertThat(response.clientCode()).isEqualTo("ud-admin-web");
        verify(authCaptchaService).consumeToken("captcha-token-1");
    }

    @Test
    void refreshTokenReturnsNewTokenPairWhenSessionActive() {
        UserContext context = new UserContext(1L, "customer", 10L, "sid-100", "ud-customer-web");
        String refreshToken = jwtTokenService.issueRefreshToken(context);
        when(loginSessionService.validateAndTouch("sid-100", "ud-customer-web")).thenReturn(true);
        AuthDtos.RefreshResponse response = authService.refreshToken(refreshToken);
        assertThat(response.accessToken()).contains(".");
        assertThat(response.refreshToken()).contains(".");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresInSeconds()).isGreaterThan(0);
    }

    @Test
    void refreshTokenFailsWhenSessionRevoked() {
        UserContext context = new UserContext(1L, "customer", 10L, "sid-100", "ud-customer-web");
        String refreshToken = jwtTokenService.issueRefreshToken(context);
        when(loginSessionService.validateAndTouch("sid-100", "ud-customer-web")).thenReturn(false);
        assertThatThrownBy(() -> authService.refreshToken(refreshToken))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage("session expired or revoked");
    }

    @Test
    void refreshTokenFailsWithInvalidToken() {
        assertThatThrownBy(() -> authService.refreshToken("invalid.token.here"))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage("invalid refresh token");
    }

    @Test
    void currentUserReturnsAccountDetails() {
        LoginAccount account = new LoginAccount(1L, "customer", "13800000000", "customer@uniondesk.local",
                passwordEncoder.encode("customer123"), 1, "customer", "active");
        UserContext context = new UserContext(1L, "customer", 10L, "sid-100", "ud-customer-web");
        when(loginAccountService.findById(1L, "customer")).thenReturn(Optional.of(account));
        when(loginAccountService.loadAccessibleDomainIds(1L, "customer", null)).thenReturn(List.of(1L));
        stubDomainView(1L);
        AuthDtos.CurrentUserResponse response = authService.currentUser(context);
        assertThat(response.userId()).isEqualTo(1L);
        assertThat(response.username()).isEqualTo("customer");
        assertThat(response.role()).isEqualTo("customer");
        assertThat(response.roles()).containsExactly("customer");
    }

    @Test
    void stepUpSucceedsWithCorrectPassword() {
        LoginAccount account = new LoginAccount(1L, "admin", "13900000000", "admin@uniondesk.local",
                passwordEncoder.encode("admin123"), 1, "admin", "active");
        UserContext context = new UserContext(1L, "super_admin", 10L, "sid-100", "ud-admin-web");
        when(loginAccountService.findById(1L, "staff")).thenReturn(Optional.of(account));
        AuthDtos.StepUpResponse response = authService.stepUp(context, "admin123", null);
        assertThat(response.stepUpToken()).isNotBlank();
        assertThat(response.mode()).isEqualTo("session_15m");
        assertThat(response.expiresInSeconds()).isEqualTo(900);
        assertThat(response.reusePolicy()).isEqualTo("session_15m");
    }

    @Test
    void stepUpFailsWithWrongPassword() {
        LoginAccount account = new LoginAccount(1L, "admin", "13900000000", "admin@uniondesk.local",
                passwordEncoder.encode("admin123"), 1, "admin", "active");
        UserContext context = new UserContext(1L, "super_admin", 10L, "sid-100", "ud-admin-web");
        when(loginAccountService.findById(1L, "staff")).thenReturn(Optional.of(account));
        assertThatThrownBy(() -> authService.stepUp(context, "wrong-password", null))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage("invalid credentials");
    }

    @Test
    void loginRejectsMissingCaptchaTokenWhenCaptchaEnabled() {
        LoginConfig config = new LoginConfig(
                true,
                true,
                true,
                true,
                true,
                false,
                null,
                null,
                604800,
                10,
                LocalDateTime.now(CLOCK));

        when(authClientService.findByCode("ud-admin-web")).thenReturn(Optional.of(new AuthClient("ud-admin-web", "admin", 1)));
        when(loginConfigService.loadConfig()).thenReturn(config);

        assertThatThrownBy(() -> authService.login(
                new AuthDtos.LoginRequest("admin", "admin123", null, null),
                "ud-admin-web",
                "127.0.0.1",
                "JUnit"))
                .isInstanceOf(AuthCaptchaException.class)
                .hasMessage("captcha required");
    }

    @Test
    void loginUsesPreferredDefaultDomainWhenAccessible() {
        LoginAccount account = new LoginAccount(2L, "admin", "13900000000", "admin@uniondesk.local",
                passwordEncoder.encode("admin123"), 1, "admin", "active");
        LoginConfig config = new LoginConfig(
                true, true, true, true, false, false, null, null, 604800, 10, LocalDateTime.now(CLOCK));

        when(authClientService.findByCode("ud-admin-web")).thenReturn(Optional.of(new AuthClient("ud-admin-web", "admin", 1)));
        when(loginConfigService.loadConfig()).thenReturn(config);
        when(loginAccountService.findByIdentifier("admin", LoginIdentifierType.USERNAME, "staff"))
                .thenReturn(Optional.of(account));
        when(iamService.listUserRoleCodesByClient(2L, "ud-admin-web")).thenReturn(List.of("super_admin"));
        when(loginAccountService.loadAccessibleDomainIds(2L, List.of("super_admin"))).thenReturn(List.of(10L, 20L));
        stubDomainView(10L);
        stubDomainView(20L);
        when(userConfigService.getPreferredDefaultDomainId(2L)).thenReturn(Optional.of(20L));
        when(loginSessionService.createSession(any(LoginSessionService.CreateSessionCommand.class)))
                .thenAnswer(invocation -> invocation.getArgument(0, LoginSessionService.CreateSessionCommand.class).sid());

        AuthDtos.LoginResponse response = authService.login(
                new AuthDtos.LoginRequest("admin", "admin123", null, null),
                "ud-admin-web",
                "127.0.0.1",
                "JUnit");

        assertThat(response.defaultBusinessDomainId()).isEqualTo(20L);
        assertThat(response.preferredDefaultDomainId()).isEqualTo(20L);
    }

    @Test
    void loginFallsBackToFirstAccessibleWhenPreferredInvalid() {
        LoginAccount account = new LoginAccount(2L, "admin", "13900000000", "admin@uniondesk.local",
                passwordEncoder.encode("admin123"), 1, "admin", "active");
        LoginConfig config = new LoginConfig(
                true, true, true, true, false, false, null, null, 604800, 10, LocalDateTime.now(CLOCK));

        when(authClientService.findByCode("ud-admin-web")).thenReturn(Optional.of(new AuthClient("ud-admin-web", "admin", 1)));
        when(loginConfigService.loadConfig()).thenReturn(config);
        when(loginAccountService.findByIdentifier("admin", LoginIdentifierType.USERNAME, "staff"))
                .thenReturn(Optional.of(account));
        when(iamService.listUserRoleCodesByClient(2L, "ud-admin-web")).thenReturn(List.of("super_admin"));
        when(loginAccountService.loadAccessibleDomainIds(2L, List.of("super_admin"))).thenReturn(List.of(10L, 20L));
        stubDomainView(10L);
        stubDomainView(20L);
        when(userConfigService.getPreferredDefaultDomainId(2L)).thenReturn(Optional.of(999L));
        when(loginSessionService.createSession(any(LoginSessionService.CreateSessionCommand.class)))
                .thenAnswer(invocation -> invocation.getArgument(0, LoginSessionService.CreateSessionCommand.class).sid());

        AuthDtos.LoginResponse response = authService.login(
                new AuthDtos.LoginRequest("admin", "admin123", null, null),
                "ud-admin-web",
                "127.0.0.1",
                "JUnit");

        assertThat(response.defaultBusinessDomainId()).isEqualTo(10L);
        assertThat(response.preferredDefaultDomainId()).isNull();
    }

    @Test
    void setDefaultDomainRejectsInaccessibleDomain() {
        UserContext context = new UserContext(2L, "super_admin", 10L, "sid-100", "ud-admin-web");
        when(iamService.listUserRoleCodesByClient(2L, "ud-admin-web")).thenReturn(List.of("super_admin"));
        when(loginAccountService.loadAccessibleDomainIds(2L, "staff", List.of("super_admin"))).thenReturn(List.of(10L));
        stubDomainView(10L);

        assertThatThrownBy(() -> authService.setDefaultDomain(context, 999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("无权访问该业务域");
        verify(userConfigService, never()).upsertPreferredDefaultDomainId(anyLong(), anyLong());
    }

    @Test
    void setDefaultDomainPersistsPreferenceWithoutSwitchingSession() {
        UserContext context = new UserContext(2L, "super_admin", 10L, "sid-100", "ud-admin-web");
        when(iamService.listUserRoleCodesByClient(2L, "ud-admin-web")).thenReturn(List.of("super_admin"));
        when(loginAccountService.loadAccessibleDomainIds(2L, "staff", List.of("super_admin"))).thenReturn(List.of(10L, 20L));
        stubDomainView(10L);
        stubDomainView(20L);

        AuthDtos.SetDefaultDomainResponse response = authService.setDefaultDomain(context, 20L);

        assertThat(response.preferredDefaultDomainId()).isEqualTo(20L);
        verify(userConfigService).upsertPreferredDefaultDomainId(2L, 20L);
        verify(loginSessionService, never()).updateBusinessDomainAndRefreshToken(anyString(), anyLong(), anyString());
    }

    @Test
    void switchDomainRejectsInaccessibleDomain() {
        UserContext context = new UserContext(2L, "super_admin", 10L, "sid-100", "ud-admin-web");
        when(iamService.listUserRoleCodesByClient(2L, "ud-admin-web")).thenReturn(List.of("super_admin"));
        when(loginAccountService.loadAccessibleDomainIds(2L, "staff", List.of("super_admin"))).thenReturn(List.of(10L));
        stubDomainView(10L);

        assertThatThrownBy(() -> authService.switchDomain(context, 999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("无权访问该业务域");
        verify(loginSessionService, never()).updateBusinessDomainAndRefreshToken(anyString(), anyLong(), anyString());
    }

    @Test
    void switchDomainIssuesTokensForAccessibleDomain() {
        UserContext context = new UserContext(2L, "super_admin", 10L, "sid-100", "ud-admin-web");
        when(iamService.listUserRoleCodesByClient(2L, "ud-admin-web")).thenReturn(List.of("super_admin"));
        when(loginAccountService.loadAccessibleDomainIds(2L, "staff", List.of("super_admin"))).thenReturn(List.of(10L, 20L));
        stubDomainView(10L);
        stubDomainView(20L);
        when(loginSessionService.updateBusinessDomainAndRefreshToken(eq("sid-100"), eq(20L), anyString())).thenReturn(1);

        AuthDtos.SwitchDomainResponse response = authService.switchDomain(context, 20L);

        assertThat(response.businessDomainId()).isEqualTo(20L);
        assertThat(response.accessToken()).contains(".");
        assertThat(response.refreshToken()).contains(".");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        verify(loginSessionService).updateBusinessDomainAndRefreshToken(eq("sid-100"), eq(20L), anyString());
    }

    private void stubDomainView(long domainId) {
        when(domainService.getDomain(domainId)).thenReturn(new DomainDtos.DomainView(
                domainId,
                "default",
                "Default Domain",
                null,
                null,
                List.of("global"),
                "enabled",
                "enabled",
                1,
                null,
                null,
                null,
                null,
                null,
                null,
                null));
    }
}
