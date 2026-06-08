package com.uniondesk.auth.core;

import com.uniondesk.auth.entity.LoginLogPo;
import com.uniondesk.auth.repository.LoginAuditRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class LoginAuditService {

    private final LoginAuditRepository loginAuditRepository;
    private final Clock clock;

    public LoginAuditService(LoginAuditRepository loginAuditRepository, Clock clock) {
        this.loginAuditRepository = loginAuditRepository;
        this.clock = clock;
    }

    public void record(LoginLogCommand command) {
        LoginLogPo po = new LoginLogPo();
        po.setSubjectId(command.subjectId());
        po.setPortalType(normalizePortalType(command.portalType()));
        po.setClientCode(command.clientCode());
        po.setSid(command.sid());
        po.setEventType(normalizeEventType(command.eventType()));
        po.setBusinessDomainId(command.businessDomainId());
        po.setLoginName(command.loginName());
        po.setLoginIdentifierMasked(command.loginIdentifierMasked());
        po.setLoginIdentifierType(command.loginIdentifierType());
        po.setIp(command.clientIp());
        po.setUserAgent(command.userAgent());
        po.setResult(normalizeResult(command.result()));
        po.setFailReason(command.failReason());
        po.setCreatedAt(LocalDateTime.now(clock));
        loginAuditRepository.insert(po);
    }

    public List<LoginLog> listRecentLogs(int limit, String eventType) {
        int cappedLimit = Math.max(1, Math.min(limit, 500));
        return loginAuditRepository.findRecentByEventType(normalizeEventType(eventType), cappedLimit).stream()
                .map(this::toLoginLog)
                .toList();
    }

    public Long resolveCustomerSubjectId(long customerAccountId) {
        return loginAuditRepository.findSubjectIdByCustomerAccountId(customerAccountId);
    }

    public String maskIdentifier(String identifier, String identifierType) {
        if (!StringUtils.hasText(identifier)) {
            return "***";
        }
        String value = identifier.trim();
        if ("MOBILE".equalsIgnoreCase(identifierType)) {
            return value.length() <= 7 ? "***" : value.substring(0, 3) + "****" + value.substring(value.length() - 4);
        }
        if ("EMAIL".equalsIgnoreCase(identifierType)) {
            int atIndex = value.indexOf('@');
            if (atIndex <= 1) {
                return "***" + value.substring(Math.max(atIndex, 0));
            }
            String localPart = value.substring(0, atIndex);
            return localPart.charAt(0) + "***" + localPart.charAt(localPart.length() - 1) + value.substring(atIndex);
        }
        return value.length() <= 2 ? "***" : value.charAt(0) + "***" + value.charAt(value.length() - 1);
    }

    public LoginLogCommand loginFailure(
            String sid,
            Long subjectId,
            String portalType,
            String clientCode,
            Long businessDomainId,
            String loginName,
            String identifierType,
            String identifier,
            String reason,
            String clientIp,
            String userAgent) {
        return new LoginLogCommand(
                sid,
                subjectId,
                portalType,
                clientCode,
                businessDomainId,
                loginName,
                maskIdentifier(identifier, identifierType),
                identifierType,
                "LOGIN",
                "failure",
                reason,
                clientIp,
                userAgent);
    }

    public LoginLogCommand loginSuccess(
            String sid,
            Long subjectId,
            String portalType,
            String clientCode,
            Long businessDomainId,
            String loginName,
            String identifierType,
            String identifier,
            String clientIp,
            String userAgent) {
        return new LoginLogCommand(
                sid,
                subjectId,
                portalType,
                clientCode,
                businessDomainId,
                loginName,
                maskIdentifier(identifier, identifierType),
                identifierType,
                "LOGIN",
                "success",
                null,
                clientIp,
                userAgent);
    }

    public LoginLogCommand sessionEvent(
            String sid,
            Long subjectId,
            String portalType,
            String clientCode,
            Long businessDomainId,
            String loginName,
            String identifierType,
            String identifier,
            String eventType,
            String result,
            String reason,
            String clientIp,
            String userAgent) {
        return new LoginLogCommand(
                sid,
                subjectId,
                portalType,
                clientCode,
                businessDomainId,
                loginName,
                maskIdentifier(identifier, identifierType),
                identifierType,
                eventType,
                result,
                reason,
                clientIp,
                userAgent);
    }

    private LoginLog toLoginLog(LoginLogPo po) {
        return new LoginLog(
                po.getId(),
                po.getSid(),
                po.getSubjectId(),
                po.getLoginName(),
                po.getLoginIdentifierMasked(),
                po.getLoginIdentifierType(),
                po.getEventType(),
                po.getPortalType(),
                po.getClientCode(),
                po.getBusinessDomainId(),
                po.getResult(),
                po.getFailReason(),
                po.getIp(),
                po.getUserAgent(),
                po.getCreatedAt());
    }

    private String normalizePortalType(String portalType) {
        if (!StringUtils.hasText(portalType)) {
            return "staff";
        }
        String normalized = portalType.trim().toLowerCase(Locale.ROOT);
        return "customer".equals(normalized) ? "customer" : "staff";
    }

    private String normalizeEventType(String eventType) {
        if (!StringUtils.hasText(eventType)) {
            return "LOGIN";
        }
        return eventType.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeResult(String result) {
        if (!StringUtils.hasText(result)) {
            return "failure";
        }
        String normalized = result.trim().toLowerCase(Locale.ROOT);
        if ("success".equals(normalized) || "failure".equals(normalized)) {
            return normalized;
        }
        if ("SUCCESS".equalsIgnoreCase(result)) {
            return "success";
        }
        return "failure";
    }

    public record LoginLogCommand(
            String sid,
            Long subjectId,
            String portalType,
            String clientCode,
            Long businessDomainId,
            String loginName,
            String loginIdentifierMasked,
            String loginIdentifierType,
            String eventType,
            String result,
            String failReason,
            String clientIp,
            String userAgent) {
    }

    public record LoginLog(
            long id,
            String sid,
            Long subjectId,
            String loginName,
            String loginIdentifierMasked,
            String loginIdentifierType,
            String eventType,
            String portalType,
            String clientCode,
            Long businessDomainId,
            String result,
            String failReason,
            String clientIp,
            String userAgent,
            LocalDateTime createdAt) {
    }
}
