package com.uniondesk.domain.web;

import com.uniondesk.domain.core.DomainRoleService;
import com.uniondesk.iam.core.PermissionCodes;
import com.uniondesk.iam.core.RequirePermission;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 平台端业务域控制台：域角色只读访问（与 {@link DomainRoleController} 域内接口解耦）。
 */
@RestController
@RequestMapping("/api/v1/admin/domains/{domainId}/platform-roles")
public class PlatformDomainRoleController {

    private final DomainRoleService domainRoleService;

    public PlatformDomainRoleController(DomainRoleService domainRoleService) {
        this.domainRoleService = domainRoleService;
    }

    @GetMapping
    @RequirePermission(PermissionCodes.PLATFORM_DOMAIN_ROLES_READ)
    public List<DomainRoleDtos.DomainRoleView> listPlatformDomainRoles(@PathVariable("domainId") long domainId) {
        return domainRoleService.listRoles(domainId);
    }

    @GetMapping("/{roleId}/permissions")
    @RequirePermission(PermissionCodes.PLATFORM_DOMAIN_ROLES_PERMISSIONS_READ)
    public DomainRoleDtos.DomainRolePermissionView getPlatformDomainRolePermissions(
            @PathVariable("domainId") long domainId,
            @PathVariable("roleId") long roleId) {
        return domainRoleService.getRolePermissions(domainId, roleId);
    }
}
