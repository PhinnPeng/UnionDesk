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
    @RequirePermission(value = PermissionCodes.DOMAIN_ROLE_READ, domainIdParam = "domainId")
    public DomainRoleDtos.DomainRoleListView listRoles(@PathVariable("domainId") long domainId) {
        List<DomainRoleDtos.DomainRoleView> items = domainRoleService.listRoles(domainId);
        return new DomainRoleDtos.DomainRoleListView(items.size(), items);
    }

    @PostMapping("/roles")
    @ResponseStatus(HttpStatus.CREATED)
    @RequirePermission(value = PermissionCodes.DOMAIN_ROLE_CREATE, domainIdParam = "domainId")
    public DomainRoleDtos.DomainRoleView createRole(
            @PathVariable("domainId") long domainId,
            @Valid @RequestBody DomainRoleDtos.CreateDomainRoleRequest request) {
        return domainRoleService.createRole(domainId, request);
    }

    @PutMapping("/roles/{roleId}")
    @RequirePermission(value = PermissionCodes.DOMAIN_ROLE_UPDATE, domainIdParam = "domainId")
    public DomainRoleDtos.DomainRoleView updateRole(
            @PathVariable("domainId") long domainId,
            @PathVariable("roleId") long roleId,
            @Valid @RequestBody DomainRoleDtos.UpdateDomainRoleRequest request) {
        return domainRoleService.updateRole(domainId, roleId, request);
    }

    @GetMapping("/roles/{roleId}/permissions")
    @RequirePermission(value = PermissionCodes.DOMAIN_ROLE_PERMISSION_READ, domainIdParam = "domainId")
    public DomainRoleDtos.DomainRolePermissionView getRolePermissions(
            @PathVariable("domainId") long domainId,
            @PathVariable("roleId") long roleId) {
        return domainRoleService.getRolePermissions(domainId, roleId);
    }

    @PutMapping("/roles/{roleId}/permissions")
    @RequirePermission(value = PermissionCodes.DOMAIN_ROLE_PERMISSION_UPDATE, domainIdParam = "domainId")
    public DomainRoleDtos.DomainRolePermissionView updateRolePermissions(
            @PathVariable("domainId") long domainId,
            @PathVariable("roleId") long roleId,
            @Valid @RequestBody DomainRoleDtos.UpdateDomainRolePermissionRequest request) {
        return domainRoleService.updateRolePermissions(domainId, roleId, request);
    }

    @DeleteMapping("/roles/{roleId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequirePermission(value = PermissionCodes.DOMAIN_ROLE_DELETE, domainIdParam = "domainId")
    public void deleteRole(
            @PathVariable("domainId") long domainId,
            @PathVariable("roleId") long roleId) {
        domainRoleService.deleteRole(domainId, roleId);
    }

    @GetMapping("/permission-items")
    @RequirePermission(value = PermissionCodes.DOMAIN_ROLE_READ, domainIdParam = "domainId")
    public DomainRoleDtos.PermissionItemListView listPermissionItems(@PathVariable("domainId") long domainId) {
        List<DomainRoleDtos.PermissionItemView> items = domainRoleService.listPermissionItems(domainId);
        return new DomainRoleDtos.PermissionItemListView(items.size(), items);
    }
}
