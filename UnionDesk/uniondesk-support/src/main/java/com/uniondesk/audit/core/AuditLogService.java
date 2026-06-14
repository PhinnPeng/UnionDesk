package com.uniondesk.audit.core;

import com.uniondesk.audit.entity.AuditLogViewPo;
import com.uniondesk.audit.entity.LoginLogViewPo;
import com.uniondesk.audit.repository.AuditLogRepository;
import com.uniondesk.audit.web.AuditDtos;
import com.uniondesk.common.audit.AuditActionCatalog;
import com.uniondesk.common.web.PageResult;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional(readOnly = true)
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public PageResult<AuditDtos.AuditLogView> listPlatformAuditLogs(
            int page,
            int pageSize,
            Long domainId,
            String operator,
            String action,
            LocalDateTime startTime,
            LocalDateTime endTime) {
        return listPlatformAuditLogs(page, pageSize, domainId, operator, action, startTime, endTime,
                null, null, null, null, null);
    }

    public PageResult<AuditDtos.AuditLogView> listPlatformAuditLogs(
            int page,
            int pageSize,
            Long domainId,
            String operator,
            String action,
            LocalDateTime startTime,
            LocalDateTime endTime,
            String module,
            String keyword,
            String ip,
            String username,
            String nickname) {
        long total = auditLogRepository.countAuditLogs(
                domainId,
                blankToNull(operator),
                blankToNull(action),
                blankToNull(module),
                blankToNull(keyword),
                blankToNull(ip),
                blankToNull(username),
                blankToNull(nickname),
                startTime,
                endTime);
        List<AuditLogViewPo> rows = auditLogRepository.findAuditLogs(
                domainId,
                blankToNull(operator),
                blankToNull(action),
                blankToNull(module),
                blankToNull(keyword),
                blankToNull(ip),
                blankToNull(username),
                blankToNull(nickname),
                startTime,
                endTime,
                page,
                pageSize);
        return new PageResult<>(total, rows.stream().map(this::toAuditLogView).toList());
    }

    public PageResult<AuditDtos.AuditLogView> listDomainAuditLogs(
            long domainId,
            int page,
            int pageSize,
            String operator,
            String action,
            String keyword,
            LocalDateTime startTime,
            LocalDateTime endTime) {
        return listPlatformAuditLogs(page, pageSize, domainId, operator, action, startTime, endTime,
                null, keyword, null, null, null);
    }

    public PageResult<AuditDtos.LoginLogView> listDomainLoginLogs(
            long domainId,
            int page,
            int pageSize,
            String portalType,
            String result,
            String keyword,
            String operator,
            String action,
            LocalDateTime startTime,
            LocalDateTime endTime) {
        String resolvedKeyword = StringUtils.hasText(keyword) ? keyword : operator;
        String resolvedResult = StringUtils.hasText(result) ? result : action;
        return listPlatformLoginLogs(
                page, pageSize, null, portalType, resolvedResult, startTime, endTime,
                resolvedKeyword, null, null, null, domainId, null, "LOGIN");
    }

    public PageResult<AuditDtos.LoginLogView> listPlatformLoginLogs(
            int page,
            int pageSize,
            Long subjectId,
            String portalType,
            String result,
            LocalDateTime startTime,
            LocalDateTime endTime) {
        return listPlatformLoginLogs(
                page, pageSize, subjectId, portalType, result, startTime, endTime,
                null, null, null, null, null, null, "LOGIN");
    }

    public PageResult<AuditDtos.LoginLogView> listPlatformLoginLogs(
            int page,
            int pageSize,
            Long subjectId,
            String portalType,
            String result,
            LocalDateTime startTime,
            LocalDateTime endTime,
            String keyword,
            String ip,
            String username,
            String nickname) {
        return listPlatformLoginLogs(
                page, pageSize, subjectId, portalType, result, startTime, endTime,
                keyword, ip, username, nickname, null, null, "LOGIN");
    }

    public PageResult<AuditDtos.LoginLogView> listPlatformLoginLogs(
            int page,
            int pageSize,
            Long subjectId,
            String portalType,
            String result,
            LocalDateTime startTime,
            LocalDateTime endTime,
            String keyword,
            String ip,
            String username,
            String nickname,
            Long businessDomainId,
            String clientCode,
            String eventType) {
        String normalizedPortalType = normalizeLower(portalType);
        String normalizedClientCode = normalizeTrim(clientCode);
        String normalizedEventType = normalizeUpper(eventType);
        String normalizedResult = normalizeLower(result);

        long total = auditLogRepository.countLoginLogs(
                subjectId, businessDomainId,
                normalizedPortalType, normalizedClientCode, normalizedEventType,
                normalizedResult,
                blankToNull(keyword), blankToNull(ip),
                blankToNull(username), blankToNull(nickname),
                startTime, endTime);
        List<LoginLogViewPo> rows = auditLogRepository.findLoginLogs(
                subjectId, businessDomainId,
                normalizedPortalType, normalizedClientCode, normalizedEventType,
                normalizedResult,
                blankToNull(keyword), blankToNull(ip),
                blankToNull(username), blankToNull(nickname),
                startTime, endTime,
                page, pageSize);
        return new PageResult<>(total, rows.stream().map(this::toLoginLogView).toList());
    }

    private AuditDtos.AuditLogView toAuditLogView(AuditLogViewPo po) {
        String detail = po.getDetail();
        return new AuditDtos.AuditLogView(
                po.getId(),
                po.getBusinessDomainId(),
                po.getOperatorSubjectId(),
                po.getOperatorName(),
                po.getOperatorActorType(),
                po.getTarget(),
                po.getAction(),
                AuditActionCatalog.resolveActionLabel(po.getAction(), detail),
                detail,
                po.getResult(),
                po.getOccurredAt(),
                po.getRequestId(),
                po.getIp());
    }

    private AuditDtos.LoginLogView toLoginLogView(LoginLogViewPo po) {
        return new AuditDtos.LoginLogView(
                po.getId(),
                po.getSubjectId(),
                po.getOperatorName(),
                po.getBusinessDomainId(),
                po.getDomainName(),
                po.getLoginName(),
                po.getPortalType(),
                po.getClientCode(),
                po.getEventType(),
                po.getIp(),
                po.getUserAgent(),
                po.getResult(),
                po.getFailReason(),
                po.getCreatedAt());
    }

    private String blankToNull(String value) {
        return StringUtils.hasText(value) ? value : null;
    }

    private String normalizeLower(String value) {
        return StringUtils.hasText(value) ? value.trim().toLowerCase() : null;
    }

    private String normalizeUpper(String value) {
        return StringUtils.hasText(value) ? value.trim().toUpperCase() : null;
    }

    private String normalizeTrim(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
