package com.uniondesk.auth.web;

import com.uniondesk.auth.core.AuthCaptchaService;
import com.uniondesk.auth.core.AuthClientHeaders;
import com.uniondesk.auth.core.AuthService;
import com.uniondesk.auth.core.LoginAuditService.LoginLog;
import com.uniondesk.auth.core.LoginConfigService.LoginConfig;
import com.uniondesk.auth.core.LoginConfigService.UpdateLoginConfigCommand;
import com.uniondesk.auth.core.LoginSessionService.OnlineSession;
import com.uniondesk.auth.core.UserContext;
import com.uniondesk.auth.core.UserContextHolder;
import com.uniondesk.common.web.ErrorCodes;
import com.uniondesk.iam.core.PermissionCodes;
import com.uniondesk.iam.core.RequirePermission;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final AuthCaptchaService authCaptchaService;

    public AuthController(AuthService authService, AuthCaptchaService authCaptchaService) {
        this.authService = authService;
        this.authCaptchaService = authCaptchaService;
    }

    @PostMapping("/captcha/challenge")
    public AuthDtos.CaptchaChallengeResponse captchaChallenge() {
        AuthCaptchaService.CaptchaChallenge challenge = authCaptchaService.createChallenge();
        return new AuthDtos.CaptchaChallengeResponse(challenge.challengeId(), challenge.expiresInSeconds());
    }

    @PostMapping("/captcha/verify")
    public AuthDtos.CaptchaVerifyResponse verifyCaptcha(@Valid @RequestBody AuthDtos.CaptchaVerifyRequest request) {
        AuthCaptchaService.CaptchaToken token = authCaptchaService.verify(request.challengeId(), request.track());
        return new AuthDtos.CaptchaVerifyResponse(token.captchaToken(), token.expiresInSeconds());
    }

    @PostMapping("/login")
    public AuthDtos.LoginResponse login(
            @Valid @RequestBody AuthDtos.LoginRequest request,
            @RequestHeader(AuthClientHeaders.CLIENT_CODE_HEADER) String clientCode,
            HttpServletRequest httpRequest) {
        return authService.login(request, clientCode, httpRequest.getRemoteAddr(), httpRequest.getHeader("User-Agent"));
    }

    @PostMapping("/register")
    public AuthDtos.RegisterResponse register(
            @RequestHeader(AuthClientHeaders.CLIENT_CODE_HEADER) String clientCode,
            @Valid @RequestBody AuthDtos.RegisterRequest request,
            HttpServletRequest httpRequest) {
        return authService.register(request, clientCode, httpRequest.getRemoteAddr(), httpRequest.getHeader("User-Agent"));
    }

    @PostMapping("/password/reset-request")
    public AuthDtos.PasswordResetResponse requestPasswordReset(
            @RequestHeader(AuthClientHeaders.CLIENT_CODE_HEADER) String clientCode,
            @Valid @RequestBody AuthDtos.PasswordResetRequest request,
            HttpServletRequest httpRequest) {
        return authService.requestPasswordReset(request, clientCode, httpRequest.getRemoteAddr(), httpRequest.getHeader("User-Agent"));
    }

    @PostMapping("/password/reset")
    public AuthDtos.EmptyResponse resetPassword(@Valid @RequestBody AuthDtos.PasswordResetConfirmRequest request) {
        authService.resetPassword(request.token(), request.newPassword());
        return new AuthDtos.EmptyResponse();
    }

    @PutMapping("/password")
    public AuthDtos.EmptyResponse changePassword(@Valid @RequestBody AuthDtos.ChangePasswordRequest request) {
        authService.changePassword(requireCurrentUser(), request.oldPassword(), request.newPassword());
        return new AuthDtos.EmptyResponse();
    }

    @PostMapping("/refresh")
    public AuthDtos.RefreshResponse refresh(@Valid @RequestBody AuthDtos.RefreshRequest request) {
        return authService.refreshToken(request.refreshToken());
    }

    @GetMapping("/me")
    public AuthDtos.CurrentUserResponse me() {
        return authService.currentUser(requireCurrentUser());
    }

    @PostMapping("/step-up")
    public AuthDtos.StepUpResponse stepUp(@Valid @RequestBody AuthDtos.StepUpRequest request) {
        return authService.stepUp(requireCurrentUser(), request.password(), request.operationCode());
    }

    @GetMapping("/login-config")
    public AuthDtos.LoginConfigView loginConfig() {
        return toLoginConfigView(authService.currentConfig());
    }

    @PutMapping("/login-config")
    @RequirePermission(PermissionCodes.PLATFORM_PERMISSION_MANAGE)
    public AuthDtos.LoginConfigView updateLoginConfig(@Valid @RequestBody AuthDtos.UpdateLoginConfigRequest request) {
        LoginConfig config = authService.updateConfig(new UpdateLoginConfigCommand(
                request.passwordLoginEnabled(),
                request.usernameLoginEnabled(),
                request.emailLoginEnabled(),
                request.mobileLoginEnabled(),
                request.captchaEnabled(),
                request.wechatLoginEnabled(),
                request.captchaHint(),
                request.wechatHint(),
                request.sessionTtlSeconds(),
                request.maxActiveSessionsPerUser()));
        return toLoginConfigView(config);
    }

    @GetMapping("/session")
    public AuthDtos.SessionView currentSession() {
        return authService.currentSession(requireCurrentUser());
    }

    @GetMapping("/online-sessions")
    @RequirePermission(PermissionCodes.PLATFORM_USER_READ)
    public List<AuthDtos.OnlineSessionView> listOnlineSessions(@RequestParam(defaultValue = "100") int limit) {
        return authService.listOnlineSessions(limit).stream()
                .map(this::toOnlineSessionView)
                .toList();
    }

    @PostMapping("/online-sessions/{sid}/revoke")
    @RequirePermission(PermissionCodes.PLATFORM_USER_DISABLE)
    public void revokeSession(@PathVariable String sid, HttpServletRequest httpRequest) {
        int updated = authService.revokeSession(sid, "admin_revoke", httpRequest.getRemoteAddr(), httpRequest.getHeader("User-Agent"));
        if (updated == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ErrorCodes.NOT_FOUND.message());
        }
    }

    @PostMapping("/users/{userId}/revoke-sessions")
    @RequirePermission(PermissionCodes.PLATFORM_USER_DISABLE)
    public void revokeSessionsByUser(@PathVariable long userId, HttpServletRequest httpRequest) {
        int updated = authService.revokeSessionsByUser(userId, "admin_revoke_all", httpRequest.getRemoteAddr(), httpRequest.getHeader("User-Agent"));
        if (updated == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ErrorCodes.NOT_FOUND.message());
        }
    }

    @GetMapping("/login-logs")
    @RequirePermission(PermissionCodes.PLATFORM_USER_READ)
    public List<AuthDtos.LoginLogView> listLoginLogs(@RequestParam(defaultValue = "100") int limit) {
        return authService.listLoginLogs(limit).stream()
                .map(this::toLoginLogView)
                .toList();
    }

    @PostMapping("/logout")
    public void logout(HttpServletRequest httpRequest) {
        authService.logoutCurrentSession(
                requireCurrentUser(),
                httpRequest.getRemoteAddr(),
                httpRequest.getHeader("User-Agent"));
    }

    private UserContext requireCurrentUser() {
        try {
            return UserContextHolder.requireCurrent();
        } catch (IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, ErrorCodes.UNAUTHORIZED.message(), ex);
        }
    }

    private AuthDtos.LoginConfigView toLoginConfigView(LoginConfig config) {
        return new AuthDtos.LoginConfigView(
                config.passwordLoginEnabled(),
                config.usernameLoginEnabled(),
                config.emailLoginEnabled(),
                config.mobileLoginEnabled(),
                config.captchaEnabled(),
                config.wechatLoginEnabled(),
                config.captchaHint(),
                config.wechatHint(),
                config.sessionTtlSeconds(),
                config.maxActiveSessionsPerUser(),
                config.updatedAt());
    }

    private AuthDtos.OnlineSessionView toOnlineSessionView(OnlineSession session) {
        return new AuthDtos.OnlineSessionView(
                session.sid(),
                session.userId(),
                session.clientCode(),
                session.username(),
                session.mobile(),
                session.email(),
                session.roleCode(),
                session.businessDomainId(),
                session.loginIdentifierMasked(),
                session.sessionStatus(),
                session.issuedAt(),
                session.expiresAt(),
                session.lastSeenAt(),
                session.clientIp(),
                session.userAgent());
    }

    private AuthDtos.LoginLogView toLoginLogView(LoginLog log) {
        return new AuthDtos.LoginLogView(
                log.id(),
                log.sid(),
                log.userId(),
                log.username(),
                log.loginIdentifierMasked(),
                log.loginIdentifierType(),
                log.eventType(),
                log.result(),
                log.reason(),
                log.clientIp(),
                log.userAgent(),
                log.createdAt());
    }
}
