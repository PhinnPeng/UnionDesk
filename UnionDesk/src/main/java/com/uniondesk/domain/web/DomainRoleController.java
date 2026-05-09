package com.uniondesk.domain.web;

import com.uniondesk.domain.core.DomainRoleService;
import com.uniondesk.iam.core.PermissionCodes;
import com.uniondesk.iam.core.RequirePermission;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/domains/{domainId}")
public class DomainRoleController {

    private final DomainRoleService domainRoleService;

    public DomainRoleController(DomainRoleService domainRoleService) {
        this.domainRoleService = domainRoleService;
    }

    @GetMapping("/roles")
    @RequirePermission(PermissionCodes.DOMAIN_ROLE_READ)
    public List<DomainRoleDtos.DomainRoleView> listRoles(@PathVariable("domainId") long domainId) {
        return domainRoleService.listRoles(domainId);
    }

    @PostMapping("/roles")
    @ResponseStatus(HttpStatus.CREATED)
    @RequirePermission(PermissionCodes.DOMAIN_ROLE_CREATE)
    public DomainRoleDtos.DomainRoleView createRole(
            @PathVariable("domainId") long domainId,
            @Valid @RequestBody DomainRoleDtos.CreateDomainRoleRequest request) {
        return domainRoleService.createRole(domainId, request);
    }

    @PutMapping("/roles/{roleId}")
    @RequirePermission(PermissionCodes.DOMAIN_ROLE_UPDATE)
    public DomainRoleDtos.DomainRoleView updateRole(
            @PathVariable("domainId") long domainId,
            @PathVariable("roleId") long roleId,
            @Valid @RequestBody DomainRoleDtos.UpdateDomainRoleRequest request) {
        return domainRoleService.updateRole(domainId, roleId, request);
    }

    @GetMapping("/roles/{roleId}/permissions")
    @RequirePermission(PermissionCodes.DOMAIN_ROLE_PERMISSION_READ)
    public DomainRoleDtos.DomainRolePermissionView getRolePermissions(
            @PathVariable("domainId") long domainId,
            @PathVariable("roleId") long roleId) {
        return domainRoleService.getRolePermissions(domainId, roleId);
    }

    @PutMapping("/roles/{roleId}/permissions")
    @RequirePermission(PermissionCodes.DOMAIN_ROLE_PERMISSION_UPDATE)
    public DomainRoleDtos.DomainRolePermissionView updateRolePermissions(
            @PathVariable("domainId") long domainId,
            @PathVariable("roleId") long roleId,
            @Valid @RequestBody DomainRoleDtos.UpdateDomainRolePermissionRequest request) {
        return domainRoleService.updateRolePermissions(domainId, roleId, request);
    }

    @DeleteMapping("/roles/{roleId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequirePermission(PermissionCodes.DOMAIN_ROLE_DELETE)
    public void deleteRole(
            @PathVariable("domainId") long domainId,
            @PathVariable("roleId") long roleId) {
        domainRoleService.deleteRole(domainId, roleId);
    }

    @GetMapping("/permission-items")
    @RequirePermission(PermissionCodes.DOMAIN_ROLE_READ)
    public List<DomainRoleDtos.PermissionItemView> listPermissionItems(@PathVariable("domainId") long domainId) {
        return domainRoleService.listPermissionItems(domainId);
    }
}
