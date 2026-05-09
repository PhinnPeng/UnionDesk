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
            String detail,
            String result,
            LocalDateTime occurredAt,
            String requestId) {
    }

    public record LoginLogView(
            long id,
            Long subjectId,
            String operatorName,
            Long businessDomainId,
            String loginName,
            String portalType,
            String ip,
            String userAgent,
            String result,
            String failReason,
            LocalDateTime createdAt) {
    }
}
