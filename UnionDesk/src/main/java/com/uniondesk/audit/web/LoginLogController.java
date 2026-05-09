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
public class LoginLogController {

    private final AuditLogService auditLogService;

    public LoginLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping("/login-logs")
    @RequirePermission(PermissionCodes.PLATFORM_LOGIN_LOG_READ)
    public PageResult<AuditDtos.LoginLogView> listPlatformLoginLogs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "20") int pageSize,
            @RequestParam(name = "subject_id", required = false) Long subjectId,
            @RequestParam(name = "portal_type", required = false) String portalType,
            @RequestParam(required = false) String result,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        return auditLogService.listPlatformLoginLogs(page, pageSize, subjectId, portalType, result, startTime, endTime);
    }

    @GetMapping("/domains/{domainId}/login-logs")
    @RequirePermission(PermissionCodes.DOMAIN_LOGIN_LOG_READ)
    public PageResult<AuditDtos.LoginLogView> listLoginLogs(
            @PathVariable long domainId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "20") int pageSize,
            @RequestParam(required = false) String operator,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        return auditLogService.listDomainLoginLogs(domainId, page, pageSize, operator, action, startTime, endTime);
    }
}
