package com.uniondesk.audit.semantics;

import com.uniondesk.common.audit.AuditActionCodes;
import com.uniondesk.common.audit.AuditDetailTextBuilder;
import com.uniondesk.common.audit.AuditTargetFormatter;
import com.uniondesk.common.event.DomainMemberStatusChangedEvent;
import com.uniondesk.common.event.RolePermissionsChangedEvent;
import com.uniondesk.iam.repository.AdminMenuRepository;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class AuditLogSemanticsListener {

    private final AuditLogWriter auditLogWriter;
    private final AuditMenuPathResolver menuPathResolver;
    private final AdminMenuRepository adminMenuRepository;

    public AuditLogSemanticsListener(
            AuditLogWriter auditLogWriter,
            AuditMenuPathResolver menuPathResolver,
            AdminMenuRepository adminMenuRepository) {
        this.auditLogWriter = auditLogWriter;
        this.menuPathResolver = menuPathResolver;
        this.adminMenuRepository = adminMenuRepository;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRolePermissionsChanged(RolePermissionsChangedEvent event) {
        String roleScope = adminMenuRepository.findRoleScopeById(event.roleId());
        String menuScope = "global".equalsIgnoreCase(roleScope) ? "platform" : "business";
        List<Long> beforeAll = mergeIds(event.beforeMenuIds(), event.beforeButtonIds());
        List<Long> afterAll = mergeIds(event.afterMenuIds(), event.afterButtonIds());
        List<String> addedPaths = menuPathResolver.resolveAddedPaths(beforeAll, afterAll, menuScope);
        List<String> removedPaths = menuPathResolver.resolveRemovedPaths(beforeAll, afterAll, menuScope);
        if (addedPaths.isEmpty() && removedPaths.isEmpty()) {
            return;
        }
        String detail = AuditDetailTextBuilder.buildRolePermissionDetail(
                event.roleName(),
                event.roleCode(),
                addedPaths,
                removedPaths);
        Long domainId = event.businessDomainId() > 0 ? event.businessDomainId() : null;
        auditLogWriter.write(
                domainId,
                event.operatorUserId(),
                "staff",
                AuditTargetFormatter.formatRole(event.roleName(), event.roleCode()),
                AuditActionCodes.PLATFORM_ROLE_PERMISSIONS_UPDATE,
                detail,
                "success",
                null);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDomainMemberStatusChanged(DomainMemberStatusChangedEvent event) {
        String memberTarget = AuditTargetFormatter.formatMember(null, null, event.memberId());
        String detail = AuditDetailTextBuilder.buildMemberStatusDetail(
                memberTarget,
                event.previousStatus(),
                event.newStatus());
        auditLogWriter.write(
                event.businessDomainId(),
                event.operatorUserId(),
                "staff",
                memberTarget,
                AuditActionCodes.PLATFORM_DOMAIN_MEMBER_UPDATE_STATUS,
                detail,
                "success",
                null);
    }

    private List<Long> mergeIds(List<Long> menuIds, List<Long> buttonIds) {
        Set<Long> merged = new LinkedHashSet<>();
        if (menuIds != null) {
            merged.addAll(menuIds);
        }
        if (buttonIds != null) {
            merged.addAll(buttonIds);
        }
        return new ArrayList<>(merged);
    }
}
