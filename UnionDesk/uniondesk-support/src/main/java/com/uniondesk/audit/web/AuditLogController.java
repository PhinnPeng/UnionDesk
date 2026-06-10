package com.uniondesk.audit.web;

import com.uniondesk.audit.core.AuditLogService;
import com.uniondesk.common.web.PageResult;
import com.uniondesk.iam.core.PermissionCodes;
import com.uniondesk.iam.core.RequirePermission;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping("/audit-logs")
    @RequirePermission(PermissionCodes.PLATFORM_LOG_AUDIT_READ)
    public PageResult<AuditDtos.AuditLogView> listPlatformAuditLogs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "20") int pageSize,
            @RequestParam(name = "domain_id", required = false) Long domainId,
            @RequestParam(name = "domainId", required = false) Long legacyDomainId,
            @RequestParam(required = false) String operator,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String ip,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String nickname,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        Long resolvedDomainId = domainId != null ? domainId : legacyDomainId;
        return auditLogService.listPlatformAuditLogs(page, pageSize, resolvedDomainId, operator, action, startTime, endTime, module, keyword, ip, username, nickname);
    }

    @GetMapping("/domains/{domainId}/audit-logs")
    @RequirePermission(PermissionCodes.PLATFORM_DOMAIN_CONTROL_AUDIT_LOG_READ)
    public PageResult<AuditDtos.AuditLogView> listDomainAuditLogs(
            @PathVariable long domainId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "20") int pageSize,
            @RequestParam(required = false) String operator,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        return auditLogService.listDomainAuditLogs(domainId, page, pageSize, operator, action, keyword, startTime, endTime);
    }
}
