package com.uniondesk.audit.web;

import java.time.LocalDateTime;

public final class AuditDtos {

    private AuditDtos() {
    }

    public record AuditLogView(
            long id,
            Long businessDomainId,
            Long operatorSubjectId,
            String operatorName,
            String operatorActorType,
            String target,
            String action,
            String actionLabel,
            String detail,
            String result,
            LocalDateTime occurredAt,
            String requestId,
            String ip) {
    }

    public record LoginLogView(
            long id,
            Long subjectId,
            String operatorName,
            Long businessDomainId,
            String domainName,
            String loginName,
            String portalType,
            String clientCode,
            String eventType,
            String ip,
            String userAgent,
            String result,
            String failReason,
            LocalDateTime createdAt) {
    }
}
