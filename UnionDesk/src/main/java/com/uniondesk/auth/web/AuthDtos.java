package com.uniondesk.auth.web;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.uniondesk.common.demo.DemoDtos.BusinessDomainView;
import com.uniondesk.common.demo.DemoDtos.LoginUserView;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record LoginRequest(
            @JsonAlias({"identifier", "loginName", "login_name"})
            @NotBlank String username,
            @NotBlank String password,
            @JsonAlias({"captcha_token"})
            String captchaToken,
            @JsonAlias({"portal_type"})
            String portalType) {
        public LoginRequest(String username, String password) {
            this(username, password, null, null);
        }
    }

    public record RegisterRequest(
            @JsonAlias({"login_name"}) @NotBlank String loginName,
            @NotBlank String password,
            @JsonAlias({"display_name"}) String displayName,
            @NotBlank String phone,
            String email,
            @JsonAlias({"domain_id"}) Long domainId,
            @JsonAlias({"invitation_code"}) String invitationCode,
            @JsonAlias({"captcha_token"}) String captchaToken) {
    }

    public record RegisterResponse(
            String accessToken,
            String refreshToken,
            long accountId) {
    }

    public record PasswordResetRequest(
            @JsonAlias({"login_name"}) @NotBlank String loginName,
            @JsonAlias({"portal_type"}) @NotBlank String portalType,
            @JsonAlias({"captcha_token"}) String captchaToken) {
    }

    public record PasswordResetResponse(
            String channel,
            String hint) {
    }

    public record PasswordResetConfirmRequest(
            @NotBlank String token,
            @JsonAlias({"new_password"}) @NotBlank String newPassword) {
    }

    public record ChangePasswordRequest(
            @JsonAlias({"old_password"}) @NotBlank String oldPassword,
            @JsonAlias({"new_password"}) @NotBlank String newPassword) {
    }

    public record EmptyResponse() {
    }

    public record CaptchaChallengeResponse(
            String challengeId,
            long expiresInSeconds) {
    }

    public record CaptchaVerifyRequest(
            @NotBlank String challengeId,
            List<com.uniondesk.auth.core.AuthCaptchaService.TrackPoint> track) {
    }

    public record CaptchaVerifyResponse(
            String captchaToken,
            long expiresInSeconds) {
    }

    public record LoginResponse(
            String accessToken,
            String refreshToken,
            String sid,
            String role,
            String clientCode,
            String tokenType,
            long expiresInSeconds,
            String portalType,
            String subjectId,
            LoginUserView user,
            List<BusinessDomainView> accessibleDomains,
            long defaultBusinessDomainId) {
    }

    public record LoginConfigView(
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

    public record UpdateLoginConfigRequest(
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

    public record SessionView(
            long userId,
            String role,
            Long businessDomainId,
            String sid,
            String clientCode) {
    }

    public record OnlineSessionView(
            String sid,
            long userId,
            String clientCode,
            String username,
            String mobile,
            String email,
            String role,
            Long businessDomainId,
            String loginIdentifierMasked,
            String sessionStatus,
            LocalDateTime issuedAt,
            LocalDateTime expiresAt,
            LocalDateTime lastSeenAt,
            String clientIp,
            String userAgent) {
    }

    public record LoginLogView(
            long id,
            String sid,
            Long userId,
            String username,
            String loginIdentifierMasked,
            String loginIdentifierType,
            String eventType,
            String result,
            String reason,
            String clientIp,
            String userAgent,
            LocalDateTime createdAt) {
    }

    public record RefreshRequest(
            @NotBlank String refreshToken) {
    }

    public record RefreshResponse(
            String accessToken,
            String refreshToken,
            String tokenType,
            long expiresInSeconds) {
    }

    public record StepUpRequest(
            @NotBlank String password,
            String operationCode) {
    }

    public record StepUpResponse(
            String stepUpToken,
            String mode,
            long expiresInSeconds,
            String reusePolicy,
            String operationCode) {
    }

    public record CurrentUserResponse(
            long userId,
            String username,
            String mobile,
            String email,
            String role,
            String clientCode,
            Long businessDomainId,
            List<String> roles,
            String subjectId,
            String accountId,
            String accountType,
            String loginName,
            String displayName,
            String avatarUrl,
            String phone,
            List<String> platformRoles,
            CurrentDomainView currentDomain) {
    }

    public record CurrentDomainView(
            String id,
            String name,
            List<String> roles,
            List<String> permissions) {
    }
}
