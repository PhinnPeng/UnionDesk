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
import com.uniondesk.common.web.AccountAccessException;
import com.uniondesk.common.web.ErrorCodes;
import com.uniondesk.domain.core.DomainCustomerService;
import com.uniondesk.domain.core.DomainService;
import com.uniondesk.domain.core.InvitationCodeService;
import com.uniondesk.domain.web.DomainDtos;
import com.uniondesk.iam.core.CustomerAccountService;
import com.uniondesk.iam.core.PlatformRoleService;
import com.uniondesk.iam.core.IamService;
import com.uniondesk.notification.core.NotificationCenterService;
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

    @Mock
    private TrustedLoginIpService trustedLoginIpService;

    @Mock
    private NotificationCenterService notificationCenterService;

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
                trustedLoginIpService,
                notificationCenterService,
                CLOCK);
        org.mockito.Mockito.lenient()
                .when(userConfigService.getPreferredDefaultDomainId(org.mockito.ArgumentMatchers.anyLong()))
                .thenReturn(Optional.empty());
        org.mockito.Mockito.lenient()
                .when(trustedLoginIpService.isTrusted(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(true);
        org.mockito.Mockito.lenient()
                .when(trustedLoginIpService.normalizeIp(org.mockito.ArgumentMatchers.any()))
                .thenAnswer(invocation -> {
                    String ip = invocation.getArgument(0);
                    return ip == null ? "" : ip.trim();
                });
    }

    private LoginConfig loginConfigWithoutCaptcha() {
        return new LoginConfig(
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
                8,
                false,
                false,
                5,
                30,
                false,
                "",
                LocalDateTime.now(CLOCK));
    }

    @Test
    void loginSucceedsWithEmailIdentifierAndCreatesSession() {
        LoginAccount account = new LoginAccount(1L, "customer", "13800000000", "customer@uniondesk.local",
                passwordEncoder.encode("customer123"), 1, "customer", "active", 0);
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
                8,
                false,
                false,
                5,
                30,
                false,
                "",
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
                passwordEncoder.encode("customer123"), 1, "customer", "active", 0);
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
                8,
                false,
                false,
                5,
                30,
                false,
                "",
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
                passwordEncoder.encode("admin123"), 1, "admin", "active", 0);
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
                8,
                false,
                false,
                5,
                30,
                false,
                "",
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
        assertThat(response.riskLoginNotified()).isFalse();
        verify(trustedLoginIpService).upsertAndPrune(2L, "127.0.0.1");
    }

    @Test
    void customerLoginRejectsDisabledAccountWithAccountDisabledCode() {
        LoginAccount account = new LoginAccount(2L, "customer", "13900000000", "customer@uniondesk.local",
                passwordEncoder.encode("customer123"), 0, "customer", "disabled", 0);
        LoginConfig config = loginConfigWithoutCaptcha();

        when(authClientService.findByCode("ud-customer-web")).thenReturn(Optional.of(new AuthClient("ud-customer-web", "customer", 1)));
        when(loginConfigService.loadConfig()).thenReturn(config);
        when(loginAccountService.findByIdentifier("customer", LoginIdentifierType.USERNAME, "customer"))
                .thenReturn(Optional.of(account));

        assertThatThrownBy(() -> authService.login(
                new AuthDtos.LoginRequest("customer", "customer123", null, null),
                "ud-customer-web",
                "127.0.0.1",
                "JUnit"))
                .isInstanceOf(AccountAccessException.class)
                .hasMessage(ErrorCodes.AUTH_ACCOUNT_DISABLED.message());
    }

    @Test
    void customerLoginRejectsWhenAllDomainMembershipsDisabled() {
        LoginAccount account = new LoginAccount(2L, "customer", "13900000000", "customer@uniondesk.local",
                passwordEncoder.encode("customer123"), 1, "customer", "active", 0);
        LoginConfig config = loginConfigWithoutCaptcha();

        when(authClientService.findByCode("ud-customer-web")).thenReturn(Optional.of(new AuthClient("ud-customer-web", "customer", 1)));
        when(loginConfigService.loadConfig()).thenReturn(config);
        when(loginAccountService.findByIdentifier("customer", LoginIdentifierType.USERNAME, "customer"))
                .thenReturn(Optional.of(account));
        when(loginAccountService.loadAccessibleDomainIds(2L, "customer", null)).thenReturn(List.of());
        when(loginAccountService.hasAnyDomainMembership(2L)).thenReturn(true);

        assertThatThrownBy(() -> authService.login(
                new AuthDtos.LoginRequest("customer", "customer123", null, null),
                "ud-customer-web",
                "127.0.0.1",
                "JUnit"))
                .isInstanceOf(AccountAccessException.class)
                .hasMessage(ErrorCodes.AUTH_NO_ACCESSIBLE_DOMAIN.message());
    }

    @Test
    void customerLoginWithNoMembershipStillSucceedsWithoutDomain() {
        LoginAccount account = new LoginAccount(2L, "customer", "13900000000", "customer@uniondesk.local",
                passwordEncoder.encode("customer123"), 1, "customer", "active", 0);
        LoginConfig config = loginConfigWithoutCaptcha();

        when(authClientService.findByCode("ud-customer-web")).thenReturn(Optional.of(new AuthClient("ud-customer-web", "customer", 1)));
        when(loginConfigService.loadConfig()).thenReturn(config);
        when(loginAccountService.findByIdentifier("customer", LoginIdentifierType.USERNAME, "customer"))
                .thenReturn(Optional.of(account));
        when(iamService.listUserRoleCodesByClient(2L, "ud-customer-web")).thenReturn(List.of("customer"));
        when(loginAccountService.loadAccessibleDomainIds(2L, "customer", null)).thenReturn(List.of());
        when(loginAccountService.hasAnyDomainMembership(2L)).thenReturn(false);
        when(loginSessionService.createSession(any(LoginSessionService.CreateSessionCommand.class)))
                .thenAnswer(invocation -> invocation.getArgument(0, LoginSessionService.CreateSessionCommand.class).sid());

        AuthDtos.LoginResponse response = authService.login(
                new AuthDtos.LoginRequest("customer", "customer123", null, null),
                "ud-customer-web",
                "127.0.0.1",
                "JUnit");

        assertThat(response.defaultBusinessDomainId()).isEqualTo(0L);
        assertThat(response.accessibleDomains()).isEmpty();
    }

    @Test
    void customerLoginNotifiesRiskOnNewIp() {
        LoginAccount account = new LoginAccount(2L, "customer", "13900000000", "customer@uniondesk.local",
                passwordEncoder.encode("customer123"), 1, "customer", "active", 0);
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
                8,
                false,
                false,
                5,
                30,
                false,
                "",
                LocalDateTime.now(CLOCK));

        when(authClientService.findByCode("ud-customer-web")).thenReturn(Optional.of(new AuthClient("ud-customer-web", "customer", 1)));
        when(loginConfigService.loadConfig()).thenReturn(config);
        when(loginAccountService.findByIdentifier("customer", LoginIdentifierType.USERNAME, "customer"))
                .thenReturn(Optional.of(account));
        when(iamService.listUserRoleCodesByClient(2L, "ud-customer-web")).thenReturn(List.of("customer"));
        when(loginAccountService.loadAccessibleDomainIds(2L, "customer", null)).thenReturn(List.of(1L));
        stubDomainView(1L);
        when(loginSessionService.createSession(any(LoginSessionService.CreateSessionCommand.class)))
                .thenAnswer(invocation -> invocation.getArgument(0, LoginSessionService.CreateSessionCommand.class).sid());
        when(trustedLoginIpService.isTrusted(2L, "10.0.0.8")).thenReturn(false);
        when(notificationCenterService.notifyCustomerRiskLogin(
                eq(2L), eq(1L), eq("10.0.0.8"), anyString(), any(LocalDateTime.class)))
                .thenReturn(new NotificationCenterService.NotificationDispatchResult(9L, 11L, "sent"));

        AuthDtos.LoginResponse response = authService.login(
                new AuthDtos.LoginRequest("customer", "customer123", null, null),
                "ud-customer-web",
                "10.0.0.8",
                "Mozilla/5.0");

        assertThat(response.accessToken()).contains(".");
        assertThat(response.riskLoginNotified()).isTrue();
        verify(notificationCenterService).notifyCustomerRiskLogin(
                eq(2L), eq(1L), eq("10.0.0.8"), anyString(), any(LocalDateTime.class));
        verify(trustedLoginIpService).upsertAndPrune(2L, "10.0.0.8");
    }

    @Test
    void customerLoginRiskNotifyFailureDoesNotBlockLogin() {
        LoginAccount account = new LoginAccount(2L, "customer", "13900000000", "customer@uniondesk.local",
                passwordEncoder.encode("customer123"), 1, "customer", "active", 0);
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
                8,
                false,
                false,
                5,
                30,
                false,
                "",
                LocalDateTime.now(CLOCK));

        when(authClientService.findByCode("ud-customer-web")).thenReturn(Optional.of(new AuthClient("ud-customer-web", "customer", 1)));
        when(loginConfigService.loadConfig()).thenReturn(config);
        when(loginAccountService.findByIdentifier("customer", LoginIdentifierType.USERNAME, "customer"))
                .thenReturn(Optional.of(account));
        when(iamService.listUserRoleCodesByClient(2L, "ud-customer-web")).thenReturn(List.of("customer"));
        when(loginAccountService.loadAccessibleDomainIds(2L, "customer", null)).thenReturn(List.of(1L));
        stubDomainView(1L);
        when(loginSessionService.createSession(any(LoginSessionService.CreateSessionCommand.class)))
                .thenAnswer(invocation -> invocation.getArgument(0, LoginSessionService.CreateSessionCommand.class).sid());
        when(trustedLoginIpService.normalizeIp("10.0.0.9")).thenReturn("10.0.0.9");
        when(trustedLoginIpService.isTrusted(2L, "10.0.0.9")).thenReturn(false);
        when(notificationCenterService.notifyCustomerRiskLogin(
                eq(2L), eq(1L), eq("10.0.0.9"), anyString(), any(LocalDateTime.class)))
                .thenThrow(new RuntimeException("inbox unavailable"));

        AuthDtos.LoginResponse response = authService.login(
                new AuthDtos.LoginRequest("customer", "customer123", null, null),
                "ud-customer-web",
                "10.0.0.9",
                "Mozilla/5.0");

        assertThat(response.accessToken()).contains(".");
        assertThat(response.riskLoginNotified()).isFalse();
        verify(trustedLoginIpService).upsertAndPrune(2L, "10.0.0.9");
    }

    @Test
    void customerLoginWithZeroDomainsReturnsEmptyAccessibleDomains() {
        LoginAccount account = new LoginAccount(2L, "customer", "13900000000", "customer@uniondesk.local",
                passwordEncoder.encode("customer123"), 1, "customer", "active", 0);
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
                8,
                false,
                false,
                5,
                30,
                false,
                "",
                LocalDateTime.now(CLOCK));

        when(authClientService.findByCode("ud-customer-web")).thenReturn(Optional.of(new AuthClient("ud-customer-web", "customer", 1)));
        when(loginConfigService.loadConfig()).thenReturn(config);
        when(loginAccountService.findByIdentifier("customer", LoginIdentifierType.USERNAME, "customer"))
                .thenReturn(Optional.of(account));
        when(iamService.listUserRoleCodesByClient(2L, "ud-customer-web")).thenReturn(List.of("customer"));
        when(loginAccountService.loadAccessibleDomainIds(2L, "customer", null)).thenReturn(List.of());
        when(loginSessionService.createSession(any(LoginSessionService.CreateSessionCommand.class)))
                .thenAnswer(invocation -> invocation.getArgument(0, LoginSessionService.CreateSessionCommand.class).sid());
        when(trustedLoginIpService.normalizeIp("127.0.0.1")).thenReturn("127.0.0.1");
        when(trustedLoginIpService.isTrusted(2L, "127.0.0.1")).thenReturn(true);

        AuthDtos.LoginResponse response = authService.login(
                new AuthDtos.LoginRequest("customer", "customer123", null, null),
                "ud-customer-web",
                "127.0.0.1",
                "JUnit");

        assertThat(response.accessibleDomains()).isEmpty();
        assertThat(response.defaultBusinessDomainId()).isZero();
        assertThat(response.riskLoginNotified()).isFalse();
    }

    @Test
    void loginConsumesCaptchaTokenWhenCaptchaEnabled() {
        LoginAccount account = new LoginAccount(2L, "admin", "13900000000", "admin@uniondesk.local",
                passwordEncoder.encode("admin123"), 1, "admin", "active", 0);
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
                8,
                false,
                false,
                5,
                30,
                false,
                "",
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
                passwordEncoder.encode("customer123"), 1, "customer", "active", 0);
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
                passwordEncoder.encode("admin123"), 1, "admin", "active", 0);
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
                passwordEncoder.encode("admin123"), 1, "admin", "active", 0);
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
                8,
                false,
                false,
                5,
                30,
                false,
                "",
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
                passwordEncoder.encode("admin123"), 1, "admin", "active", 0);
        LoginConfig config = new LoginConfig(
                true, true, true, true, false, false, null, null, 604800, 10, 8, false, false, 5, 30, false, "", LocalDateTime.now(CLOCK));

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
                passwordEncoder.encode("admin123"), 1, "admin", "active", 0);
        LoginConfig config = new LoginConfig(
                true, true, true, true, false, false, null, null, 604800, 10, 8, false, false, 5, 30, false, "", LocalDateTime.now(CLOCK));

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

    private LoginConfig loginConfigWithSecurityPolicy(
            int passwordMinLength,
            boolean passwordRequireMixed,
            boolean lockEnabled,
            int maxAttempts,
            int lockMinutes,
            boolean ipWhitelistEnabled,
            String ipWhitelist) {
        return new LoginConfig(
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
                passwordMinLength,
                passwordRequireMixed,
                lockEnabled,
                maxAttempts,
                lockMinutes,
                ipWhitelistEnabled,
                ipWhitelist,
                LocalDateTime.now(CLOCK));
    }

    @Test
    void staffLoginRejectsWhenIpNotInWhitelist() {
        LoginAccount account = new LoginAccount(2L, "admin", "13900000000", "admin@uniondesk.local",
                passwordEncoder.encode("admin123"), 1, "admin", "active", 0);
        LoginConfig config = loginConfigWithSecurityPolicy(8, false, false, 5, 30, true, "10.0.0.1, 10.0.0.2");

        when(authClientService.findByCode("ud-admin-web")).thenReturn(Optional.of(new AuthClient("ud-admin-web", "admin", 1)));
        when(loginConfigService.loadConfig()).thenReturn(config);

        assertThatThrownBy(() -> authService.login(
                new AuthDtos.LoginRequest("admin", "admin123", null, null),
                "ud-admin-web",
                "127.0.0.1",
                "JUnit"))
                .isInstanceOf(AccountAccessException.class)
                .hasMessage(ErrorCodes.AUTH_IP_NOT_ALLOWED.message());
        verify(loginAuditService, never()).record(any());
    }

    @Test
    void staffLoginAllowsIpInWhitelist() {
        LoginAccount account = new LoginAccount(2L, "admin", "13900000000", "admin@uniondesk.local",
                passwordEncoder.encode("admin123"), 1, "admin", "active", 0);
        LoginConfig config = loginConfigWithSecurityPolicy(8, false, false, 5, 30, true, "10.0.0.1, 127.0.0.1");

        when(authClientService.findByCode("ud-admin-web")).thenReturn(Optional.of(new AuthClient("ud-admin-web", "admin", 1)));
        when(loginConfigService.loadConfig()).thenReturn(config);
        when(loginAccountService.findByIdentifier("admin", LoginIdentifierType.USERNAME, "staff"))
                .thenReturn(Optional.of(account));
        when(iamService.listUserRoleCodesByClient(2L, "ud-admin-web")).thenReturn(List.of("super_admin"));
        when(loginAccountService.loadAccessibleDomainIds(2L, List.of("super_admin"))).thenReturn(List.of(10L));
        stubDomainView(10L);
        when(loginSessionService.createSession(any(LoginSessionService.CreateSessionCommand.class)))
                .thenAnswer(invocation -> invocation.getArgument(0, LoginSessionService.CreateSessionCommand.class).sid());

        AuthDtos.LoginResponse response = authService.login(
                new AuthDtos.LoginRequest("admin", "admin123", null, null),
                "ud-admin-web",
                "127.0.0.1",
                "JUnit");

        assertThat(response.sid()).isNotBlank();
    }

    @Test
    void customerLoginRejectsWhenLockedByConsecutiveFailures() {
        LoginAccount account = new LoginAccount(1L, "customer", "13800000000", "customer@uniondesk.local",
                passwordEncoder.encode("customer123"), 1, "customer", "active", 0);
        LoginConfig config = loginConfigWithSecurityPolicy(8, false, true, 5, 30, false, "");

        when(authClientService.findByCode("ud-customer-web")).thenReturn(Optional.of(new AuthClient("ud-customer-web", "customer", 1)));
        when(loginConfigService.loadConfig()).thenReturn(config);
        when(loginAccountService.findByIdentifier("customer", LoginIdentifierType.USERNAME, "customer"))
                .thenReturn(Optional.of(account));
        when(loginAuditService.countRecentPasswordFailures(eq("customer"), eq("customer"), any(LocalDateTime.class))).thenReturn(5);

        assertThatThrownBy(() -> authService.login(
                new AuthDtos.LoginRequest("customer", "customer123", null, null),
                "ud-customer-web",
                "127.0.0.1",
                "JUnit"))
                .isInstanceOf(AccountAccessException.class)
                .hasMessage(ErrorCodes.AUTH_ACCOUNT_LOCKED.message());
        verify(loginSessionService, never()).createSession(any());
    }

    @Test
    void loginLockCheckSkippedWhenLockDisabled() {
        LoginAccount account = new LoginAccount(1L, "customer", "13800000000", "customer@uniondesk.local",
                passwordEncoder.encode("customer123"), 1, "customer", "active", 0);
        LoginConfig config = loginConfigWithSecurityPolicy(8, false, false, 5, 30, false, "");

        when(authClientService.findByCode("ud-customer-web")).thenReturn(Optional.of(new AuthClient("ud-customer-web", "customer", 1)));
        when(loginConfigService.loadConfig()).thenReturn(config);
        when(loginAccountService.findByIdentifier("customer", LoginIdentifierType.USERNAME, "customer"))
                .thenReturn(Optional.of(account));
        when(loginAccountService.loadAccessibleDomainIds(1L, "customer", null)).thenReturn(List.of(1L));
        stubDomainView(1L);
        when(loginSessionService.createSession(any(LoginSessionService.CreateSessionCommand.class)))
                .thenAnswer(invocation -> invocation.getArgument(0, LoginSessionService.CreateSessionCommand.class).sid());

        AuthDtos.LoginResponse response = authService.login(
                new AuthDtos.LoginRequest("customer", "customer123", null, null),
                "ud-customer-web",
                "127.0.0.1",
                "JUnit");

        assertThat(response.sid()).isNotBlank();
        verify(loginAuditService, never()).countRecentPasswordFailures(anyString(), anyString(), any(LocalDateTime.class));
    }

    @Test
    void changePasswordRejectsShortPassword() {
        LoginAccount account = new LoginAccount(1L, "customer", "13800000000", "customer@uniondesk.local",
                passwordEncoder.encode("customer123"), 1, "customer", "active", 0);
        LoginConfig config = loginConfigWithSecurityPolicy(8, false, false, 5, 30, false, "");
        UserContext context = new UserContext(1L, "customer", 10L, "sid-100", "ud-customer-web");

        when(loginConfigService.loadConfig()).thenReturn(config);
        when(loginAccountService.findById(1L, "customer")).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> authService.changePassword(context, "customer123", "short"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("新密码长度不能少于 8 位");
        verify(loginAccountService, never()).updatePassword(anyString(), anyLong(), anyString());
    }

    @Test
    void changePasswordRejectsNonMixedPasswordWhenRequired() {
        LoginAccount account = new LoginAccount(1L, "customer", "13800000000", "customer@uniondesk.local",
                passwordEncoder.encode("customer123"), 1, "customer", "active", 0);
        LoginConfig config = loginConfigWithSecurityPolicy(8, true, false, 5, 30, false, "");
        UserContext context = new UserContext(1L, "customer", 10L, "sid-100", "ud-customer-web");

        when(loginConfigService.loadConfig()).thenReturn(config);
        when(loginAccountService.findById(1L, "customer")).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> authService.changePassword(context, "customer123", "abcdefgh"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("新密码必须同时包含字母和数字");
        verify(loginAccountService, never()).updatePassword(anyString(), anyLong(), anyString());
    }

    @Test
    void resetPasswordRejectsShortPassword() {
        LoginConfig config = loginConfigWithSecurityPolicy(8, false, false, 5, 30, false, "");
        when(loginConfigService.loadConfig()).thenReturn(config);
        when(loginSessionService.consumePasswordResetToken("token-1")).thenReturn(new LoginSessionService.PasswordResetToken(
                "token-1",
                1L,
                "customer",
                "ud-customer-web",
                LocalDateTime.now(CLOCK).plusMinutes(30),
                "valid",
                "cus***"));

        assertThatThrownBy(() -> authService.resetPassword("token-1", "short"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("新密码长度不能少于 8 位");
        verify(loginAccountService, never()).updatePassword(anyString(), anyLong(), anyString());
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
