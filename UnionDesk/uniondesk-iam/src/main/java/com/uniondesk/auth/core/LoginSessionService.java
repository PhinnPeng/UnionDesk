package com.uniondesk.auth.core;

import com.uniondesk.auth.entity.AuthLoginSessionPo;
import com.uniondesk.auth.entity.OnlineSessionPo;
import com.uniondesk.auth.repository.LoginSessionRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class LoginSessionService {

    private static final String SESSION_TYPE_LOGIN = "login";
    private static final String SESSION_TYPE_STEP_UP = "step_up";
    private static final String SESSION_TYPE_PASSWORD_RESET = "password_reset";

    private final LoginSessionRepository loginSessionRepository;
    private final Clock clock;
    private final LoginConfigService loginConfigService;

    public LoginSessionService(
            LoginSessionRepository loginSessionRepository,
            Clock clock,
            LoginConfigService loginConfigService) {
        this.loginSessionRepository = loginSessionRepository;
        this.clock = clock;
        this.loginConfigService = loginConfigService;
    }

    public String createSession(CreateSessionCommand command) {
        LocalDateTime now = LocalDateTime.now(clock);
        AuthLoginSessionPo po = baseSession(command.sid(), SESSION_TYPE_LOGIN, command.accountType(), command.userId(),
                command.clientCode(), command.roleCode(), command.businessDomainId(), command.loginIdentifierMasked(), now, command.expiresAt());
        po.setRefreshTokenHash(command.refreshTokenHash());
        po.setClientIp(command.clientIp());
        po.setUserAgent(command.userAgent());
        loginSessionRepository.insertSession(po);
        return command.sid();
    }

    public String createStepUpToken(CreateStepUpTokenCommand command) {
        LocalDateTime now = LocalDateTime.now(clock);
        AuthLoginSessionPo po = baseSession(command.token(), SESSION_TYPE_STEP_UP, command.accountType(), command.userId(),
                command.clientCode(), command.roleCode(), command.businessDomainId(), command.loginIdentifierMasked(), now, command.expiresAt());
        po.setClientIp(command.clientIp());
        po.setUserAgent(command.userAgent());
        loginSessionRepository.insertSession(po);
        return command.token();
    }

    public String createPasswordResetToken(CreatePasswordResetTokenCommand command) {
        LocalDateTime now = LocalDateTime.now(clock);
        AuthLoginSessionPo po = baseSession(command.token(), SESSION_TYPE_PASSWORD_RESET, command.accountType(), command.accountId(),
                command.clientCode(), null, null, command.loginIdentifierMasked(), now, command.expiresAt());
        po.setClientIp(command.clientIp());
        po.setUserAgent(command.userAgent());
        loginSessionRepository.insertSession(po);
        return command.token();
    }

    public boolean validateStepUpToken(String token, long userId, String clientCode) {
        if (!StringUtils.hasText(token) || !StringUtils.hasText(clientCode)) {
            return false;
        }
        return validateToken(token, userId, clientCode, SESSION_TYPE_STEP_UP);
    }

    public PasswordResetToken consumePasswordResetToken(String token) {
        if (!StringUtils.hasText(token)) {
            return null;
        }
        return loginSessionRepository.findBySidAndType(token, SESSION_TYPE_PASSWORD_RESET)
                .filter(resetToken -> {
                    LocalDateTime now = LocalDateTime.now(clock);
                    return "active".equalsIgnoreCase(resetToken.getSessionStatus())
                            && resetToken.getExpiresAt().isAfter(now);
                })
                .map(resetToken -> {
                    LocalDateTime now = LocalDateTime.now(clock);
                    loginSessionRepository.revokeBySid(token, SESSION_TYPE_PASSWORD_RESET, now, "password_reset_used");
                    return new PasswordResetToken(
                            resetToken.getSid(),
                            resetToken.getUserId(),
                            resetToken.getAccountType(),
                            resetToken.getClientCode(),
                            resetToken.getExpiresAt(),
                            resetToken.getSessionStatus(),
                            resetToken.getLoginIdentifierMasked());
                })
                .orElse(null);
    }

    public boolean validateAndTouch(String sid, String clientCode) {
        return validateAndTouchInternal(sid, clientCode, SESSION_TYPE_LOGIN);
    }

    public boolean validateAndTouch(String sid) {
        return validateAndTouchInternal(sid, null, SESSION_TYPE_LOGIN);
    }

    public int revokeSession(String sid, String reason) {
        return loginSessionRepository.revokeBySid(sid, SESSION_TYPE_LOGIN, LocalDateTime.now(clock), reason);
    }

    public int revokeSessionsByUser(long userId, String reason) {
        return loginSessionRepository.revokeByUserId(userId, SESSION_TYPE_LOGIN, LocalDateTime.now(clock), reason);
    }

    public List<OnlineSession> listOnlineSessions(int limit) {
        int cappedLimit = Math.max(1, Math.min(limit, 500));
        return loginSessionRepository.findOnlineSessions(SESSION_TYPE_LOGIN, cappedLimit).stream()
                .map(this::toOnlineSession)
                .toList();
    }

    private AuthLoginSessionPo baseSession(
            String sid,
            String sessionType,
            String accountType,
            long userId,
            String clientCode,
            String roleCode,
            Long businessDomainId,
            String loginIdentifierMasked,
            LocalDateTime now,
            LocalDateTime expiresAt) {
        AuthLoginSessionPo po = new AuthLoginSessionPo();
        po.setSid(sid);
        po.setSessionType(sessionType);
        po.setAccountType(accountType);
        po.setUserId(userId);
        po.setClientCode(clientCode);
        po.setRoleCode(roleCode);
        po.setBusinessDomainId(businessDomainId);
        po.setLoginIdentifierMasked(loginIdentifierMasked);
        po.setSessionStatus("active");
        po.setIssuedAt(now);
        po.setExpiresAt(expiresAt);
        po.setLastSeenAt(now);
        return po;
    }

    private boolean validateToken(String token, long userId, String clientCode, String sessionType) {
        return loginSessionRepository.findBySidAndType(token, sessionType)
                .filter(state -> isActiveForUser(state, userId, clientCode))
                .map(state -> {
                    loginSessionRepository.updateLastSeen(token, sessionType, LocalDateTime.now(clock));
                    return true;
                })
                .orElse(false);
    }

    private boolean validateAndTouchInternal(String sid, String clientCode, String sessionType) {
        if (!StringUtils.hasText(sid)) {
            return false;
        }
        return loginSessionRepository.findBySidAndType(sid, sessionType)
                .map(state -> {
                    LocalDateTime now = LocalDateTime.now(clock);
                    if (clientCode != null && !clientCode.equalsIgnoreCase(state.getClientCode())) {
                        return false;
                    }
                    if (!"active".equalsIgnoreCase(state.getSessionStatus())) {
                        return false;
                    }
                    if (!state.getExpiresAt().isAfter(now)) {
                        loginSessionRepository.expireBySid(sid, sessionType, now);
                        return false;
                    }
                    LocalDateTime newExpiresAt = now.plusSeconds(loginConfigService.loadConfig().sessionTtlSeconds());
                    loginSessionRepository.updateLastSeenAndExpires(sid, sessionType, now, newExpiresAt);
                    return true;
                })
                .orElse(false);
    }

    private boolean isActiveForUser(AuthLoginSessionPo state, long userId, String clientCode) {
        LocalDateTime now = LocalDateTime.now(clock);
        return "active".equalsIgnoreCase(state.getSessionStatus())
                && state.getUserId() == userId
                && clientCode.equalsIgnoreCase(state.getClientCode())
                && state.getExpiresAt().isAfter(now);
    }

    private OnlineSession toOnlineSession(OnlineSessionPo po) {
        return new OnlineSession(
                po.getSid(),
                po.getUserId(),
                po.getClientCode(),
                po.getUsername(),
                po.getMobile(),
                po.getEmail(),
                po.getRoleCode(),
                po.getBusinessDomainId(),
                po.getLoginIdentifierMasked(),
                po.getSessionStatus(),
                po.getIssuedAt(),
                po.getExpiresAt(),
                po.getLastSeenAt(),
                po.getClientIp(),
                po.getUserAgent());
    }

    public record CreateSessionCommand(
            String sid,
            long userId,
            String clientCode,
            String accountType,
            String roleCode,
            Long businessDomainId,
            String loginIdentifierMasked,
            LocalDateTime expiresAt,
            String refreshTokenHash,
            String clientIp,
            String userAgent) {
    }

    public record CreateStepUpTokenCommand(
            String token,
            long userId,
            String clientCode,
            String accountType,
            String roleCode,
            Long businessDomainId,
            String loginIdentifierMasked,
            LocalDateTime expiresAt,
            String clientIp,
            String userAgent) {
    }

    public record CreatePasswordResetTokenCommand(
            String token,
            long accountId,
            String accountType,
            String clientCode,
            String loginIdentifierMasked,
            LocalDateTime expiresAt,
            String clientIp,
            String userAgent) {
    }

    public record PasswordResetToken(
            String token,
            long accountId,
            String accountType,
            String clientCode,
            LocalDateTime expiresAt,
            String sessionStatus,
            String loginIdentifierMasked) {
    }

    public record OnlineSession(
            String sid,
            long userId,
            String clientCode,
            String username,
            String mobile,
            String email,
            String roleCode,
            Long businessDomainId,
            String loginIdentifierMasked,
            String sessionStatus,
            LocalDateTime issuedAt,
            LocalDateTime expiresAt,
            LocalDateTime lastSeenAt,
            String clientIp,
            String userAgent) {
    }
}
