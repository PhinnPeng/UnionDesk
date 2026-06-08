package com.uniondesk.auth.core;

import com.uniondesk.auth.core.LoginAccountService.LoginAccount;
import com.uniondesk.auth.core.AuthClientService.AuthClient;
import com.uniondesk.auth.core.LoginConfigService.LoginConfig;
import com.uniondesk.auth.core.LoginSessionService.OnlineSession;
import com.uniondesk.auth.web.AuthDtos;
import com.uniondesk.common.demo.DemoDtos.BusinessDomainView;
import com.uniondesk.common.demo.DemoDtos.LoginUserView;
import com.uniondesk.domain.core.DomainCustomerService;
import com.uniondesk.domain.core.DomainService;
import com.uniondesk.domain.core.DomainAccessPolicy;
import com.uniondesk.domain.core.DomainErrorCodes;
import com.uniondesk.domain.core.InvitationCodeService;
import com.uniondesk.domain.web.DomainCustomerDtos;
import com.uniondesk.domain.web.DomainDtos;
import com.uniondesk.iam.core.CustomerAccountService;
import com.uniondesk.iam.core.IamService;
import com.uniondesk.iam.core.PlatformRoleService;
import com.uniondesk.auth.core.AuthVersionService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AuthService {

    private final LoginAccountService loginAccountService;
    private final AuthClientService authClientService;
    private final LoginConfigService loginConfigService;
    private final LoginSessionService loginSessionService;
    private final LoginAuditService loginAuditService;
    private final IamService iamService;
    private final JwtTokenService jwtTokenService;
    private final PasswordEncoder passwordEncoder;
    private final AuthCaptchaService authCaptchaService;
    private final CustomerAccountService customerAccountService;
    private final DomainCustomerService domainCustomerService;
    private final DomainService domainService;
    private final InvitationCodeService invitationCodeService;
    private final AuthVersionService authVersionService;
    private final PlatformRoleService platformRoleService;
    private final Clock clock;

    public AuthService(
            LoginAccountService loginAccountService,
            AuthClientService authClientService,
            LoginConfigService loginConfigService,
            LoginSessionService loginSessionService,
            LoginAuditService loginAuditService,
            IamService iamService,
            JwtTokenService jwtTokenService,
            PasswordEncoder passwordEncoder,
            AuthCaptchaService authCaptchaService,
            CustomerAccountService customerAccountService,
            DomainCustomerService domainCustomerService,
            DomainService domainService,
            InvitationCodeService invitationCodeService,
            AuthVersionService authVersionService,
            PlatformRoleService platformRoleService,
            Clock clock) {
        this.loginAccountService = loginAccountService;
        this.authClientService = authClientService;
        this.loginConfigService = loginConfigService;
        this.loginSessionService = loginSessionService;
        this.loginAuditService = loginAuditService;
        this.iamService = iamService;
        this.jwtTokenService = jwtTokenService;
        this.passwordEncoder = passwordEncoder;
        this.authCaptchaService = authCaptchaService;
        this.customerAccountService = customerAccountService;
        this.domainCustomerService = domainCustomerService;
        this.domainService = domainService;
        this.invitationCodeService = invitationCodeService;
        this.authVersionService = authVersionService;
        this.platformRoleService = platformRoleService;
        this.clock = clock;
    }

    public AuthDtos.LoginResponse login(AuthDtos.LoginRequest request, String clientCode, String clientIp, String userAgent) {
        if (!StringUtils.hasText(clientCode)) {
            throw new IllegalArgumentException("missing client code");
        }
        AuthClient authClient = authClientService.findByCode(clientCode)
                .orElseThrow(() -> new AuthenticationFailedException("invalid client"));
        if (authClient.status() != 1) {
            throw new AuthenticationFailedException("invalid client");
        }

        LoginIdentifierType identifierType = LoginIdentifierType.detect(request.username());
        LoginConfig config = loginConfigService.loadConfig();
        if (config.captchaEnabled()) {
            if (!StringUtils.hasText(request.captchaToken())) {
                throw new AuthCaptchaException("captcha required");
            }
            authCaptchaService.consumeToken(request.captchaToken());
        }
        String portalType = portalTypeForClient(authClient);
        if (!isIdentifierTypeEnabled(config, identifierType) || !config.passwordLoginEnabled()) {
            loginAuditService.record(loginAuditService.loginFailure(
                    null,
                    null,
                    portalType,
                    clientCode,
                    null,
                    request.username(),
                    identifierType.name(),
                    request.username(),
                    "login_disabled",
                    clientIp,
                    userAgent));
            throw new AuthenticationFailedException("invalid credentials");
        }

        if ("customer".equalsIgnoreCase(authClient.allowedAccountType())) {
            return loginCustomer(request, authClient, identifierType, clientIp, userAgent, config);
        }

        LoginAccount account = loginAccountService.findByIdentifier(request.username(), identifierType, "staff")
                .orElseThrow(() -> failLogin(
                        null, null, identifierType, request.username(), "account_not_found",
                        clientIp, userAgent, clientCode, portalType, null, null));

        if (account.status() != 1) {
            throw failLogin(
                    account.id(), account.username(), identifierType, request.username(), "account_disabled",
                    clientIp, userAgent, clientCode, portalType, null, null);
        }
        if ("offboarded".equalsIgnoreCase(account.employmentStatus())) {
            throw failLogin(
                    account.id(), account.username(), identifierType, request.username(), "account_offboarded",
                    clientIp, userAgent, clientCode, portalType, null, null);
        }
        if (!passwordEncoder.matches(request.password(), account.passwordHash())) {
            throw failLogin(
                    account.id(), account.username(), identifierType, request.username(), "password_mismatch",
                    clientIp, userAgent, clientCode, portalType, null, null);
        }
        List<String> roles = iamService.listUserRoleCodesByClient(account.id(), authClient.clientCode());
        String effectiveRole = roles.isEmpty() ? "customer" : roles.get(0);
        List<String> responseRoles = roles.isEmpty() ? List.of("customer") : roles;
        List<Long> accessibleDomainIds = loginAccountService.loadAccessibleDomainIds(account.id(), roles);
        List<BusinessDomainView> accessibleDomains = resolveAccessibleDomains(accessibleDomainIds);
        long defaultBusinessDomainId = accessibleDomains.isEmpty()
                ? loginAccountService.resolveDefaultDomainId()
                : accessibleDomains.get(0).id();

        String sid = UUID.randomUUID().toString();
        UserContext userContext = new UserContext(account.id(), effectiveRole, defaultBusinessDomainId, sid, authClient.clientCode());
        LocalDateTime sessionExpiresAt = LocalDateTime.now(clock).plusSeconds(config.sessionTtlSeconds());
        String refreshToken = jwtTokenService.issueRefreshToken(userContext);
        loginSessionService.createSession(new LoginSessionService.CreateSessionCommand(
                sid,
                account.id(),
                authClient.clientCode(),
                "staff",
                effectiveRole,
                defaultBusinessDomainId,
                maskIdentifier(request.username(), identifierType),
                sessionExpiresAt,
                sha256(refreshToken),
                clientIp,
                userAgent));
        String accessToken = jwtTokenService.issueAccessToken(userContext);
        String effectivePortalType = determinePortalType(request.portalType(), effectiveRole);
        loginAuditService.record(loginAuditService.loginSuccess(
                sid,
                null,
                effectivePortalType,
                authClient.clientCode(),
                defaultBusinessDomainId,
                account.username(),
                identifierType.name(),
                request.username(),
                clientIp,
                userAgent));

        String subjectId = String.valueOf(account.id()); // TODO: 从 identity_subject 获取真实 subject_id
        
        return new AuthDtos.LoginResponse(
                accessToken,
                refreshToken,
                sid,
                effectiveRole,
                authClient.clientCode(),
                "Bearer",
                jwtTokenService.accessTokenTtl().toSeconds(),
                effectivePortalType,
                subjectId,
                new LoginUserView(account.id(), account.username(), account.mobile(), account.email(), responseRoles),
                accessibleDomains,
                defaultBusinessDomainId);
    }
    
    private String determinePortalType(String requestedPortalType, String role) {
        if (requestedPortalType != null) {
            return requestedPortalType;
        }
        // 鏍规嵁瑙掕壊鎺ㄦ柇 portal_type
        if ("customer".equalsIgnoreCase(role)) {
            return "customer";
        }
        return "staff";
    }

    private AuthDtos.LoginResponse loginCustomer(
            AuthDtos.LoginRequest request,
            AuthClient authClient,
            LoginIdentifierType identifierType,
            String clientIp,
            String userAgent,
            LoginConfig config) {
        String portalType = portalTypeForClient(authClient);
        LoginAccount account = loginAccountService.findByIdentifier(request.username(), identifierType, "customer")
                .orElseThrow(() -> failLogin(
                        null, null, identifierType, request.username(), "account_not_found",
                        clientIp, userAgent, authClient.clientCode(), portalType, null, null));
        if (account.status() != 1) {
            throw failLogin(
                    account.id(), account.username(), identifierType, request.username(), "account_disabled",
                    clientIp, userAgent, authClient.clientCode(), portalType, null, null);
        }
        if (!passwordEncoder.matches(request.password(), account.passwordHash())) {
            throw failLogin(
                    account.id(), account.username(), identifierType, request.username(), "password_mismatch",
                    clientIp, userAgent, authClient.clientCode(), portalType, null, null);
        }

        List<String> roles = iamService.listUserRoleCodesByClient(account.id(), authClient.clientCode());
        String effectiveRole = roles.isEmpty() ? "customer" : roles.get(0);
        List<String> responseRoles = roles.isEmpty() ? List.of("customer") : roles;
        List<Long> accessibleDomainIds = loginAccountService.loadAccessibleDomainIds(account.id(), "customer", null);
        List<BusinessDomainView> accessibleDomains = resolveAccessibleDomains(accessibleDomainIds);
        long defaultBusinessDomainId = accessibleDomains.isEmpty()
                ? loginAccountService.resolveDefaultDomainId()
                : accessibleDomains.get(0).id();
        String sid = UUID.randomUUID().toString();
        UserContext userContext = new UserContext(account.id(), effectiveRole, defaultBusinessDomainId, sid, authClient.clientCode());
        LocalDateTime sessionExpiresAt = LocalDateTime.now(clock).plusSeconds(config.sessionTtlSeconds());
        String refreshToken = jwtTokenService.issueRefreshToken(userContext);
        loginSessionService.createSession(new LoginSessionService.CreateSessionCommand(
                sid,
                account.id(),
                authClient.clientCode(),
                "customer",
                effectiveRole,
                defaultBusinessDomainId,
                maskIdentifier(request.username(), identifierType),
                sessionExpiresAt,
                sha256(refreshToken),
                clientIp,
                userAgent));
        String accessToken = jwtTokenService.issueAccessToken(userContext);
        Long subjectId = loginAuditService.resolveCustomerSubjectId(account.id());
        String effectivePortalType = determinePortalType(request.portalType(), effectiveRole);
        loginAuditService.record(loginAuditService.loginSuccess(
                sid,
                subjectId,
                effectivePortalType,
                authClient.clientCode(),
                defaultBusinessDomainId,
                account.username(),
                identifierType.name(),
                request.username(),
                clientIp,
                userAgent));
        return new AuthDtos.LoginResponse(
                accessToken,
                refreshToken,
                sid,
                effectiveRole,
                authClient.clientCode(),
                "Bearer",
                jwtTokenService.accessTokenTtl().toSeconds(),
                effectivePortalType,
                subjectId == null ? String.valueOf(account.id()) : String.valueOf(subjectId),
                new LoginUserView(account.id(), account.username(), account.mobile(), account.email(), responseRoles),
                accessibleDomains,
                defaultBusinessDomainId);
    }

    @Transactional
    public AuthDtos.RegisterResponse register(AuthDtos.RegisterRequest request, String clientCode, String clientIp, String userAgent) {
        if (!StringUtils.hasText(clientCode)) {
            throw new IllegalArgumentException("missing client code");
        }
        AuthClient authClient = authClientService.findByCode(clientCode)
                .orElseThrow(() -> new AuthenticationFailedException("invalid client"));
        if (!"customer".equalsIgnoreCase(authClient.allowedAccountType())) {
            throw new AuthenticationFailedException("invalid client");
        }
        LoginConfig config = loginConfigService.loadConfig();
        if (config.captchaEnabled() && StringUtils.hasText(request.captchaToken())) {
            authCaptchaService.consumeToken(request.captchaToken());
        }
        String username = request.loginName().trim();
        String phone = request.phone().trim();
        String nickname = StringUtils.hasText(request.displayName()) ? request.displayName().trim() : username;
        long accountId = customerAccountService.create(new CustomerAccountService.CreateCustomerCommand(
                username,
                nickname,
                phone,
                request.email(),
                request.password(),
                false));
        if (request.domainId() != null) {
            DomainDtos.DomainView domain = domainService.getDomain(request.domainId());
            boolean registrationAllowed = DomainAccessPolicy.isAllowed(domain.registration_enabled());
            boolean invitationAllowed = DomainAccessPolicy.isAllowed(domain.invitation_enabled());
            boolean hasInvitation = StringUtils.hasText(request.invitationCode());
            if (hasInvitation) {
                if (!invitationAllowed) {
                    throw DomainErrorCodes.INVITATION_DISALLOWED.toException();
                }
                invitationCodeService.validateAndUse(request.domainId(), request.invitationCode());
            } else if (!registrationAllowed) {
                throw DomainErrorCodes.REGISTRATION_DISALLOWED.toException();
            }
            domainCustomerService.addCustomer(
                    request.domainId(),
                    new DomainCustomerDtos.CreateDomainCustomerRequest(
                            accountId,
                            hasInvitation ? "invitation" : "self_register"));
        }
        AuthDtos.LoginResponse loginResponse = loginCustomer(
                new AuthDtos.LoginRequest(username, request.password(), null, "customer"),
                authClient,
                LoginIdentifierType.USERNAME,
                clientIp,
                userAgent,
                config);
        return new AuthDtos.RegisterResponse(loginResponse.accessToken(), loginResponse.refreshToken(), accountId);
    }

    public AuthDtos.PasswordResetResponse requestPasswordReset(
            AuthDtos.PasswordResetRequest request,
            String clientCode,
            String clientIp,
            String userAgent) {
        AuthClient authClient = authClientService.findByCode(clientCode)
                .orElseThrow(() -> new AuthenticationFailedException("invalid client"));
        LoginIdentifierType identifierType = LoginIdentifierType.detect(request.loginName());
        String normalizedPortal = StringUtils.hasText(request.portalType())
                ? request.portalType().trim().toLowerCase(Locale.ROOT)
                : "customer";
        boolean customerPortal = "customer".equals(normalizedPortal) || "ud-customer-web".equalsIgnoreCase(clientCode);
        String channel = customerPortal ? "inbox" : "email";
        String hint = "如果账号存在，重置链接已发送";
        LocalDateTime expiresAt = LocalDateTime.now(clock).plusMinutes(30);
        String token = UUID.randomUUID().toString().replace("-", "");
        String accountType = customerPortal ? "customer" : "staff";
        loginAccountService.findByIdentifier(request.loginName(), identifierType, accountType).ifPresent(account ->
                loginSessionService.createPasswordResetToken(new LoginSessionService.CreatePasswordResetTokenCommand(
                        token,
                        account.id(),
                        accountType,
                        authClient.clientCode(),
                        maskIdentifier(request.loginName(), identifierType),
                        expiresAt,
                        clientIp,
                        userAgent)));
        return new AuthDtos.PasswordResetResponse(channel, hint);
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        LoginSessionService.PasswordResetToken resetToken = loginSessionService.consumePasswordResetToken(token);
        if (resetToken == null) {
            throw new AuthenticationFailedException("invalid reset token");
        }
        loginAccountService.updatePassword(resetToken.accountType(), resetToken.accountId(), newPassword);
        authVersionService.incrementVersion(resetToken.accountId(), resetToken.accountType());
    }

    @Transactional
    public void changePassword(UserContext context, String oldPassword, String newPassword) {
        String accountType = "ud-customer-web".equalsIgnoreCase(context.clientCode()) ? "customer" : "staff";
        LoginAccount account = loginAccountService.findById(context.userId(), accountType)
                .orElseThrow(() -> new AuthenticationFailedException("account not found"));
        if (!passwordEncoder.matches(oldPassword, account.passwordHash())) {
            throw new AuthenticationFailedException("invalid credentials");
        }
        loginAccountService.updatePassword(accountType, account.id(), newPassword);
        authVersionService.incrementVersion(account.id(), accountType);
    }

    public AuthDtos.RefreshResponse refreshToken(String refreshToken) {
        UserContext oldContext;
        try {
            oldContext = jwtTokenService.parseRefreshToken(refreshToken);
        } catch (IllegalArgumentException ex) {
            throw new AuthenticationFailedException("invalid refresh token");
        }
        if (!loginSessionService.validateAndTouch(oldContext.sessionId(), oldContext.clientCode())) {
            throw new AuthenticationFailedException("session expired or revoked");
        }
        String newAccessToken = jwtTokenService.issueAccessToken(oldContext);
        String newRefreshToken = jwtTokenService.issueRefreshToken(oldContext);
        return new AuthDtos.RefreshResponse(
                newAccessToken,
                newRefreshToken,
                "Bearer",
                jwtTokenService.accessTokenTtl().toSeconds());
    }

    public AuthDtos.CurrentUserResponse currentUser(UserContext context) {
        if ("ud-customer-web".equalsIgnoreCase(context.clientCode())) {
            return currentUserForCustomer(context);
        }
        LoginAccount account = loginAccountService.findById(context.userId(), "staff")
                .orElseThrow(() -> new AuthenticationFailedException("account not found"));
        List<String> roles = iamService.listUserRoleCodesByClient(context.userId(), context.clientCode());
        String subjectId = String.valueOf(account.id());
        String accountId = String.valueOf(account.id());
        String accountType = determineAccountType(roles);
        List<String> platformRoles = platformRoleService.getCurrentPlatformRoles(account.id());
        AuthDtos.CurrentDomainView currentDomain = null;
        if (context.businessDomainId() != null && context.businessDomainId() > 0) {
            currentDomain = new AuthDtos.CurrentDomainView(
                    String.valueOf(context.businessDomainId()),
                    "Default Domain",
                    List.of(),
                    List.of());
        }
        return new AuthDtos.CurrentUserResponse(
                account.id(),
                account.username(),
                account.mobile(),
                account.email(),
                context.role(),
                context.clientCode(),
                context.businessDomainId(),
                roles,
                subjectId,
                accountId,
                accountType,
                account.username(),
                account.username(),
                null,
                account.mobile(),
                platformRoles,
                currentDomain);
    }

    private AuthDtos.CurrentUserResponse currentUserForCustomer(UserContext context) {
        LoginAccount account = loginAccountService.findById(context.userId(), "customer")
                .orElseThrow(() -> new AuthenticationFailedException("account not found"));
        List<Long> domainIds = loginAccountService.loadAccessibleDomainIds(context.userId(), "customer", null);
        List<BusinessDomainView> accessibleDomains = resolveAccessibleDomains(domainIds);
        Long currentDomainId = context.businessDomainId();
        AuthDtos.CurrentDomainView currentDomain = null;
        if (currentDomainId != null && currentDomainId > 0) {
            currentDomain = new AuthDtos.CurrentDomainView(
                    String.valueOf(currentDomainId),
                    "customer",
                    List.of("customer"),
                    List.of());
        }
        return new AuthDtos.CurrentUserResponse(
                account.id(),
                account.username(),
                account.mobile(),
                account.email(),
                "customer",
                context.clientCode(),
                currentDomainId,
                List.of("customer"),
                String.valueOf(account.id()),
                String.valueOf(account.id()),
                "customer",
                account.username(),
                account.username(),
                null,
                account.mobile(),
                List.of(),
                currentDomain);
    }

    private String determineAccountType(List<String> roles) {
        if (roles.contains("customer")) {
            return "customer";
        }
        return "staff";
    }

    public AuthDtos.StepUpResponse stepUp(UserContext context, String password, String operationCode) {
        String accountType = "ud-customer-web".equalsIgnoreCase(context.clientCode()) ? "customer" : "staff";
        LoginAccount account = loginAccountService.findById(context.userId(), accountType)
                .orElseThrow(() -> new AuthenticationFailedException("account not found"));
        if (!passwordEncoder.matches(password, account.passwordHash())) {
            throw new AuthenticationFailedException("invalid credentials");
        }
        String reusePolicy = determineReusePolicy(operationCode);
        String stepUpToken = UUID.randomUUID().toString();
        return new AuthDtos.StepUpResponse(stepUpToken, "session_15m", 900, reusePolicy, operationCode);
    }

    private String determineReusePolicy(String operationCode) {
        if (operationCode == null) {
            return "session_15m";
        }
        // 楂樺嵄鎿嶄綔浣跨敤 one_time
        List<String> oneTimeOperations = List.of(
            "delete_domain",
            "merge_identity",
            "delete_staff_permanent",
            "delete_customer_permanent",
            "export_all_data",
            "modify_global_security"
        );
        return oneTimeOperations.contains(operationCode) ? "one_time" : "session_15m";
    }

    public AuthDtos.SessionView currentSession(UserContext context) {
        return new AuthDtos.SessionView(
                context.userId(),
                context.role(),
                context.businessDomainId(),
                context.sessionId(),
                context.clientCode());
    }

    public void logoutCurrentSession(UserContext context, String clientIp, String userAgent) {
        loginSessionService.revokeSession(context.sessionId(), "user_logout");
        loginAuditService.record(loginAuditService.sessionEvent(
                context.sessionId(),
                null,
                portalTypeForClientCode(context.clientCode()),
                context.clientCode(),
                context.businessDomainId(),
                null,
                "USERNAME",
                null,
                "LOGOUT",
                "success",
                "user_logout",
                clientIp,
                userAgent));
    }

    public List<OnlineSession> listOnlineSessions(int limit) {
        return loginSessionService.listOnlineSessions(limit);
    }

    public int revokeSession(String sid, String reason, String clientIp, String userAgent) {
        int updated = loginSessionService.revokeSession(sid, reason);
        if (updated > 0) {
            loginAuditService.record(loginAuditService.sessionEvent(
                    sid,
                    null,
                    "staff",
                    "ud-admin-web",
                    null,
                    null,
                    "USERNAME",
                    null,
                    "FORCE_LOGOUT",
                    "success",
                    reason,
                    clientIp,
                    userAgent));
        }
        return updated;
    }

    public int revokeSessionsByUser(long userId, String reason, String clientIp, String userAgent) {
        int updated = loginSessionService.revokeSessionsByUser(userId, reason);
        if (updated > 0) {
            loginAuditService.record(loginAuditService.sessionEvent(
                    null,
                    null,
                    "staff",
                    "ud-admin-web",
                    null,
                    null,
                    "USERNAME",
                    null,
                    "FORCE_LOGOUT",
                    "success",
                    reason,
                    clientIp,
                    userAgent));
        }
        return updated;
    }

    public LoginConfig currentConfig() {
        return loginConfigService.loadConfig();
    }

    public LoginConfig updateConfig(LoginConfigService.UpdateLoginConfigCommand command) {
        return loginConfigService.updateConfig(command);
    }

    private AuthenticationFailedException failLogin(
            Long userId,
            String username,
            LoginIdentifierType identifierType,
            String identifier,
            String reason,
            String clientIp,
            String userAgent,
            String clientCode,
            String portalType,
            Long businessDomainId,
            Long subjectId) {
        loginAuditService.record(loginAuditService.loginFailure(
                null,
                subjectId,
                portalType,
                clientCode,
                businessDomainId,
                username != null ? username : identifier,
                identifierType.name(),
                identifier,
                reason,
                clientIp,
                userAgent));
        return new AuthenticationFailedException("invalid credentials");
    }

    private String portalTypeForClient(AuthClient authClient) {
        return "customer".equalsIgnoreCase(authClient.allowedAccountType()) ? "customer" : "staff";
    }

    private String portalTypeForClientCode(String clientCode) {
        return "ud-customer-web".equalsIgnoreCase(clientCode) ? "customer" : "staff";
    }

    private boolean isIdentifierTypeEnabled(LoginConfig config, LoginIdentifierType type) {
        return switch (type) {
            case USERNAME -> config.usernameLoginEnabled();
            case EMAIL -> config.emailLoginEnabled();
            case MOBILE -> config.mobileLoginEnabled();
        };
    }

    private List<BusinessDomainView> resolveAccessibleDomains(List<Long> accessibleDomainIds) {
        if (accessibleDomainIds.isEmpty()) {
            return filterDomains(List.of(loginAccountService.resolveDefaultDomainId()));
        }
        return filterDomains(accessibleDomainIds);
    }

    private List<BusinessDomainView> filterDomains(List<Long> accessibleDomainIds) {
        return accessibleDomainIds.stream()
                .distinct()
                .sorted()
                .map(this::toBusinessDomainView)
                .collect(Collectors.toList());
    }

    private BusinessDomainView toBusinessDomainView(long domainId) {
        DomainDtos.DomainView domain = domainService.getDomain(domainId);
        List<String> visibilityPolicyCodes = domain.visibility_policy_codes();
        String visibilityPolicy = visibilityPolicyCodes == null || visibilityPolicyCodes.isEmpty()
                ? "public"
                : visibilityPolicyCodes.getFirst();
        return new BusinessDomainView(
                domain.id(),
                domain.code(),
                domain.name(),
                visibilityPolicy,
                domain.status(),
                0L,
                0L,
                0L,
                0L);
    }

    private String maskIdentifier(String identifier, LoginIdentifierType type) {
        return loginAuditService.maskIdentifier(identifier, type.name());
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return bytesToHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            builder.append(Character.forDigit((value >> 4) & 0xF, 16));
            builder.append(Character.forDigit(value & 0xF, 16));
        }
        return builder.toString();
    }
}
