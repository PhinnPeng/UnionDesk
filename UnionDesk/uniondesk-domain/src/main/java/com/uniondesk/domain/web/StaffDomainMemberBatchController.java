package com.uniondesk.domain.web;

import com.uniondesk.audit.semantics.AuditLogWriter;
import com.uniondesk.auth.core.LoginSessionService;
import com.uniondesk.auth.core.UserContext;
import com.uniondesk.auth.core.UserContextHolder;
import com.uniondesk.common.audit.AuditActionCodes;
import com.uniondesk.common.audit.AuditTargetFormatter;
import com.uniondesk.common.web.ErrorCodes;
import com.uniondesk.domain.core.DomainMemberService;
import com.uniondesk.iam.core.PermissionCodes;
import com.uniondesk.iam.core.RequirePermission;
import com.uniondesk.iam.core.StaffAccountService;
import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * P1-2 跨域批量停用（design §5）：{@code POST /api/v1/admin/staff/{staffId}/domain-members/batch-status}。
 *
 * <p>放置于 domain 模块而非 iam 模块的 StaffController：iam 不依赖 domain（避免循环依赖），
 * 与 RoleTemplateController（domain 模块承载 /api/v1/iam/role-templates）同构。
 * step-up 二次认证复用 P0 机制：{@code X-UD-Step-Up-Token} 经
 * {@link LoginSessionService#validateStepUpToken} 真实校验，无效/过期/缺失 → 403 + 中文。
 * 逐域事务（TR-04 部分成功）：每域独立事务，失败域携带中文原因；逐域写审计（point=platform）。
 */
@RestController
@RequestMapping("/api/v1/admin/staff")
public class StaffDomainMemberBatchController {

    private final DomainMemberService domainMemberService;
    private final StaffAccountService staffAccountService;
    private final LoginSessionService loginSessionService;
    private final AuditLogWriter auditLogWriter;

    public StaffDomainMemberBatchController(
            DomainMemberService domainMemberService,
            StaffAccountService staffAccountService,
            LoginSessionService loginSessionService,
            AuditLogWriter auditLogWriter) {
        this.domainMemberService = domainMemberService;
        this.staffAccountService = staffAccountService;
        this.loginSessionService = loginSessionService;
        this.auditLogWriter = auditLogWriter;
    }

    @PostMapping("/{staffId}/domain-members/batch-status")
    @RequirePermission(PermissionCodes.PLATFORM_USER_DOMAIN_BATCH_STATUS)
    public DomainMemberDtos.BatchStatusResult batchStatus(
            @PathVariable("staffId") long staffId,
            @Valid @RequestBody DomainMemberDtos.BatchStatusRequest request,
            @RequestHeader(value = "X-UD-Step-Up-Token", required = false) String stepUpToken) {
        requireStepUpToken(stepUpToken);
        if (!"disabled".equalsIgnoreCase(request.status())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "批量状态变更仅支持停用（disabled）");
        }
        StaffAccountService.StaffAccount staff = staffAccountService.findById(staffId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ErrorCodes.NOT_FOUND.message()));
        UserContext context = UserContextHolder.requireCurrent();
        List<Long> domainIds = deduplicate(request.domain_ids());
        List<Long> success = new ArrayList<>();
        List<DomainMemberDtos.BatchDomainFailure> failed = new ArrayList<>();
        for (Long domainId : domainIds) {
            String result;
            try {
                domainMemberService.disableDomainMemberByStaffAccount(staffId, domainId);
                success.add(domainId);
                result = "success";
            }
            catch (RuntimeException ex) {
                String reason = StringUtils.hasText(ex.getMessage()) ? ex.getMessage() : "操作失败";
                failed.add(new DomainMemberDtos.BatchDomainFailure(domainId, reason));
                result = "failed:" + reason;
            }
            writeBatchAudit(context, staff, domainIds, domainId, result);
        }
        return new DomainMemberDtos.BatchStatusResult(success, failed);
    }

    private void requireStepUpToken(String stepUpToken) {
        if (stepUpToken == null || stepUpToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, ErrorCodes.FORBIDDEN.message());
        }
        UserContext context = UserContextHolder.requireCurrent();
        if (!loginSessionService.validateStepUpToken(stepUpToken, context.userId(), context.clientCode())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, ErrorCodes.FORBIDDEN.message());
        }
    }

    /** 逐域写审计（design §5/§6）：business_domain_id + point=platform + 请求域集与逐域结果 */
    private void writeBatchAudit(
            UserContext context,
            StaffAccountService.StaffAccount staff,
            List<Long> requestDomainIds,
            long domainId,
            String result) {
        String detail = "跨域批量停用：请求域集 " + requestDomainIds + "，本域结果 " + result;
        auditLogWriter.write(
                domainId,
                context.userId(),
                "staff",
                "platform",
                AuditTargetFormatter.formatMember(staff.realName(), staff.username(), 0L),
                AuditActionCodes.PLATFORM_USER_DOMAIN_BATCH_STATUS,
                detail,
                "success",
                context.sessionId());
    }

    private List<Long> deduplicate(List<Long> values) {
        Set<Long> unique = new LinkedHashSet<>();
        if (values != null) {
            for (Long value : values) {
                if (value != null) {
                    unique.add(value);
                }
            }
        }
        return List.copyOf(unique);
    }
}
