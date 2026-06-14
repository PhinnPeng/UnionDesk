package com.uniondesk.iam.web;

import com.uniondesk.auth.core.UserContext;
import com.uniondesk.auth.core.UserContextHolder;
import com.uniondesk.common.web.ErrorCodes;
import com.uniondesk.iam.admin.AdminMenuService;
import com.uniondesk.iam.admin.AdminMenuService.AdminMenuNode;
import com.uniondesk.iam.admin.AdminMenuService.CreateAdminMenuCommand;
import com.uniondesk.iam.admin.AdminMenuService.RolePermissions;
import com.uniondesk.iam.admin.AdminMenuService.UpdateAdminMenuCommand;
import com.uniondesk.iam.core.IamService;
import com.uniondesk.iam.core.IamService.CreateResourceCommand;
import com.uniondesk.iam.core.IamService.CreateRoleCommand;
import com.uniondesk.iam.core.IamService.CreateUserCommand;
import com.uniondesk.iam.core.IamService.IamResource;
import com.uniondesk.iam.core.IamService.PermissionSnapshot;
import com.uniondesk.iam.core.IamService.RoleView;
import com.uniondesk.iam.core.IamService.UpdateResourceCommand;
import com.uniondesk.iam.core.IamService.UpdateRoleCommand;
import com.uniondesk.iam.core.IamService.UpdateUserCommand;
import com.uniondesk.iam.core.IamService.UserAccount;
import com.uniondesk.iam.core.PermissionCodes;
import com.uniondesk.iam.core.RequirePermission;
import jakarta.validation.Valid;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/iam")
public class IamController {

    private final IamService iamService;
    private final AdminMenuService adminMenuService;

    public IamController(IamService iamService, AdminMenuService adminMenuService) {
        this.iamService = iamService;
        this.adminMenuService = adminMenuService;
    }

    @GetMapping("/resources")
    @RequirePermission(PermissionCodes.PLATFORM_IAM_RESOURCE_READ)
    public List<IamDtos.ResourceView> listResources(
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) String clientScope) {
        return iamService.listResources(resourceType, clientScope).stream()
                .map(this::toResourceView)
                .toList();
    }

    @PostMapping("/resources")
    @ResponseStatus(HttpStatus.CREATED)
    @RequirePermission(PermissionCodes.PLATFORM_IAM_RESOURCE_CREATE)
    public IamDtos.ResourceView createResource(@Valid @RequestBody IamDtos.CreateResourceRequest request) {
        IamResource resource = iamService.createResource(new CreateResourceCommand(
                request.resourceType(),
                request.resourceCode(),
                request.resourceName(),
                request.clientScope(),
                request.httpMethod(),
                request.pathPattern(),
                request.parentId(),
                request.orderNo(),
                request.icon(),
                request.component(),
                request.hidden(),
                request.status()));
        return toResourceView(resource);
    }

    @PutMapping("/resources/{id}")
    @RequirePermission(PermissionCodes.PLATFORM_IAM_RESOURCE_UPDATE)
    public IamDtos.ResourceView updateResource(@PathVariable long id, @Valid @RequestBody IamDtos.UpdateResourceRequest request) {
        IamResource resource = iamService.updateResource(id, new UpdateResourceCommand(
                request.resourceType(),
                request.resourceCode(),
                request.resourceName(),
                request.clientScope(),
                request.httpMethod(),
                request.pathPattern(),
                request.parentId(),
                request.orderNo(),
                request.icon(),
                request.component(),
                request.hidden(),
                request.status()));
        return toResourceView(resource);
    }

    @GetMapping("/roles/{roleId}/resources")
    @RequirePermission(PermissionCodes.PLATFORM_IAM_ROLE_RESOURCE_READ)
    public List<IamDtos.ResourceView> listRoleResources(@PathVariable int roleId) {
        return iamService.listRoleResources(roleId).stream()
                .map(this::toResourceView)
                .toList();
    }

    @PutMapping("/roles/{roleId}/resources")
    @RequirePermission(PermissionCodes.PLATFORM_IAM_ROLE_RESOURCE_UPDATE)
    public List<IamDtos.ResourceView> replaceRoleResources(
            @PathVariable int roleId,
            @Valid @RequestBody IamDtos.ReplaceRoleResourcesRequest request) {
        return iamService.replaceRoleResources(roleId, request.resourceIds()).stream()
                .map(this::toResourceView)
                .toList();
    }

    @GetMapping("/menus/tree")
    @RequirePermission({
            PermissionCodes.PLATFORM_MENU_READ,
            PermissionCodes.DOMAIN_MENU_READ
    })
    public Object listMenusTree(@RequestParam(name = "scope", required = false) String scope) {
        if (StringUtils.hasText(scope)) {
            return adminMenuService.listMenuTree(scope).stream()
                    .map(this::toMenuTreeNodeView)
                    .toList();
        }
        Map<String, List<IamDtos.MenuTreeNodeView>> response = new LinkedHashMap<>();
        response.put("platform", adminMenuService.listMenuTree("platform").stream()
                .map(this::toMenuTreeNodeView)
                .toList());
        response.put("business", adminMenuService.listMenuTree("business").stream()
                .map(this::toMenuTreeNodeView)
                .toList());
        return response;
    }

    @PostMapping("/menus")
    @ResponseStatus(HttpStatus.CREATED)
    @RequirePermission({
            PermissionCodes.PLATFORM_MENU_CREATE,
            PermissionCodes.DOMAIN_MENU_CREATE
    })
    public IamDtos.ResourceView createMenu(@Valid @RequestBody IamDtos.CreateMenuRequest request) {
        AdminMenuNode node = adminMenuService.createMenu(new CreateAdminMenuCommand(
                request.nodeType(),
                request.name(),
                request.routePath(),
                request.componentKey(),
                request.permissionCode(),
                request.scope(),
                request.parentId(),
                request.orderNo(),
                request.icon(),
                request.hidden(),
                request.status()));
        return toResourceView(node);
    }

    @PutMapping("/menus/{menuId}")
    @RequirePermission({
            PermissionCodes.PLATFORM_MENU_UPDATE,
            PermissionCodes.DOMAIN_MENU_UPDATE
    })
    public IamDtos.ResourceView updateMenu(@PathVariable long menuId, @Valid @RequestBody IamDtos.UpdateMenuRequest request) {
        AdminMenuNode node = adminMenuService.updateMenu(menuId, new UpdateAdminMenuCommand(
                request.nodeType(),
                request.name(),
                request.routePath(),
                request.componentKey(),
                request.permissionCode(),
                request.scope(),
                request.parentId(),
                request.orderNo(),
                request.icon(),
                request.hidden(),
                request.status()));
        return toResourceView(node);
    }

    @DeleteMapping("/menus/{menuId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequirePermission({
            PermissionCodes.PLATFORM_MENU_DELETE,
            PermissionCodes.DOMAIN_MENU_DELETE
    })
    public void deleteMenu(@PathVariable long menuId) {
        adminMenuService.deleteMenu(menuId);
        iamService.evictAuthorizationCache();
    }

    @GetMapping("/admin-permission-codes")
    @RequirePermission({
            PermissionCodes.PLATFORM_MENU_READ,
            PermissionCodes.PLATFORM_ADMIN_PERMISSION_CODES_READ
    })
    public List<IamDtos.AdminPermissionCodeView> listAdminPermissionCodes(@RequestParam(required = false) String scope) {
        return adminMenuService.listPermissionCodes(scope).stream()
                .map(definition -> new IamDtos.AdminPermissionCodeView(
                        definition.code(),
                        definition.name(),
                        definition.permissionScope(),
                        definition.httpMethod(),
                        definition.pathPattern()))
                .toList();
    }

    @GetMapping("/roles")
    @RequirePermission(PermissionCodes.PLATFORM_ROLE_READ)
    public List<IamDtos.RoleView> listRoles() {
        return iamService.listRoles().stream().map(this::toRoleView).toList();
    }

    @PostMapping("/roles")
    @ResponseStatus(HttpStatus.CREATED)
    @RequirePermission(PermissionCodes.PLATFORM_ROLE_CREATE)
    public IamDtos.RoleView createRole(@Valid @RequestBody IamDtos.CreateRoleRequest request) {
        RoleView role = iamService.createRole(new CreateRoleCommand(request.code(), request.name(), request.scope()));
        return toRoleView(role);
    }

    @PutMapping("/roles/{roleId}")
    @RequirePermission(PermissionCodes.PLATFORM_ROLE_UPDATE)
    public IamDtos.RoleView updateRole(@PathVariable int roleId, @Valid @RequestBody IamDtos.UpdateRoleRequest request) {
        RoleView role = iamService.updateRole(roleId, new UpdateRoleCommand(request.code(), request.name(), request.scope()));
        return toRoleView(role);
    }

    @DeleteMapping("/roles/{roleId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequirePermission(PermissionCodes.PLATFORM_ROLE_DELETE)
    public void deleteRole(@PathVariable int roleId) {
        iamService.deleteRole(roleId);
    }

    @GetMapping("/roles/{roleId}/permissions")
    @RequirePermission(PermissionCodes.PLATFORM_ROLE_PERMISSION_READ)
    public IamDtos.RolePermissionsView getRolePermissions(@PathVariable int roleId) {
        RolePermissions permissions = adminMenuService.loadRolePermissions(roleId);
        return new IamDtos.RolePermissionsView(
                permissions.roleId(),
                permissions.menuIds(),
                permissions.buttonIds());
    }

    @PutMapping("/roles/{roleId}/permissions")
    @RequirePermission(PermissionCodes.PLATFORM_ROLE_PERMISSION_UPDATE)
    public IamDtos.RolePermissionsView replaceRolePermissions(
            @PathVariable int roleId,
            @Valid @RequestBody IamDtos.ReplaceRolePermissionsRequest request) {
        RolePermissions permissions = adminMenuService.replaceRolePermissions(
                roleId,
                request.menuIds(),
                request.buttonIds());
        return new IamDtos.RolePermissionsView(
                permissions.roleId(),
                permissions.menuIds(),
                permissions.buttonIds());
    }

    @GetMapping("/users")
    @RequirePermission({PermissionCodes.PLATFORM_USER_READ, PermissionCodes.DOMAIN_USER_READ})
    public List<IamDtos.UserAccountView> listUsers(@RequestParam(required = false) Long organizationId) {
        return iamService.listUsers(false, organizationId).stream().map(this::toUserAccountView).toList();
    }

    @PostMapping("/users")
    @ResponseStatus(HttpStatus.CREATED)
    @RequirePermission({PermissionCodes.PLATFORM_USER_CREATE, PermissionCodes.DOMAIN_USER_CREATE})
    public IamDtos.UserAccountView createUser(@Valid @RequestBody IamDtos.CreateUserRequest request) {
        UserContext context = requireCurrentContext();
        if (iamService.containsGlobalRole(request.roleCodes())) {
            requirePermission(context, PermissionCodes.PLATFORM_USER_CREATE, List.of());
        } else {
            requirePermission(context, PermissionCodes.DOMAIN_USER_CREATE, request.businessDomainIds());
        }
        UserAccount user = iamService.createUser(new CreateUserCommand(
                request.username(),
                request.nickname(),
                request.mobile(),
                request.email(),
                request.remark(),
                request.password(),
                request.accountType(),
                request.roleCodes(),
                request.businessDomainIds(),
                request.organizationIds()));
        return toUserAccountView(user);
    }

    @PutMapping("/users/{userId}")
    @RequirePermission({PermissionCodes.PLATFORM_USER_UPDATE, PermissionCodes.DOMAIN_USER_UPDATE})
    public IamDtos.UserAccountView updateUser(@PathVariable long userId, @Valid @RequestBody IamDtos.UpdateUserRequest request) {
        UserContext context = requireCurrentContext();
        requireUserMutationPermission(
                context,
                userId,
                request.roleCodes(),
                request.businessDomainIds(),
                PermissionCodes.PLATFORM_USER_UPDATE,
                PermissionCodes.DOMAIN_USER_UPDATE);
        UserAccount user = iamService.updateUser(userId, new UpdateUserCommand(
                request.username(),
                request.nickname(),
                request.mobile(),
                request.email(),
                request.remark(),
                request.password(),
                request.accountType(),
                request.roleCodes(),
                request.businessDomainIds(),
                request.status(),
                request.organizationIds()));
        return toUserAccountView(user);
    }

    @PostMapping("/users/{userId}/offboard")
    @RequirePermission(PermissionCodes.PLATFORM_USER_DISABLE)
    public IamDtos.UserAccountView offboardUser(@PathVariable long userId, @RequestBody(required = false) IamDtos.OffboardUserRequest request) {
        UserContext context = requireCurrentContext();
        UserAccount user = iamService.offboardUser(
                userId,
                context.userId(),
                request == null ? null : request.reason());
        return toUserAccountView(user);
    }

    @PostMapping("/users/{userId}/restore")
    @RequirePermission(PermissionCodes.PLATFORM_USER_RESTORE)
    public IamDtos.UserAccountView restoreUser(@PathVariable long userId) {
        return toUserAccountView(iamService.restoreUser(userId));
    }

    @GetMapping("/users/offboard-pool")
    @RequirePermission(PermissionCodes.PLATFORM_USER_OFFBOARD_POOL_READ)
    public List<IamDtos.UserAccountView> listOffboardPool() {
        return iamService.listUsers(true, null).stream().map(this::toUserAccountView).toList();
    }

    @DeleteMapping("/users/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequirePermission({PermissionCodes.PLATFORM_USER_DELETE, PermissionCodes.DOMAIN_USER_REMOVE})
    public void deleteUserPermanently(@PathVariable long userId) {
        UserContext context = requireCurrentContext();
        UserAccount existing = iamService.loadUser(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ErrorCodes.NOT_FOUND.message()));
        if (iamService.containsGlobalRole(existing.roleCodes())) {
            requirePermission(context, PermissionCodes.PLATFORM_USER_DELETE, List.of());
        } else {
            requirePermission(context, PermissionCodes.DOMAIN_USER_REMOVE, existing.businessDomainIds());
        }
        iamService.deleteUserPermanently(userId);
    }

    @GetMapping("/me/menu-resources")
    public List<IamDtos.ResourceView> currentMenuResources() {
        return iamService.listCurrentMenuResources(requireCurrentContext()).stream()
                .map(this::toResourceView)
                .toList();
    }

    @GetMapping("/me/permission-snapshot")
    public IamDtos.PermissionSnapshotView currentPermissionSnapshot() {
        PermissionSnapshot snapshot = iamService.loadPermissionSnapshot(requireCurrentContext());
        List<IamDtos.MenuView> menuTree = snapshot.menuTree().stream()
                .map(this::toMenuView)
                .toList();
        return new IamDtos.PermissionSnapshotView(
                new IamDtos.UserView(
                        snapshot.user().id(),
                        snapshot.user().username(),
                        snapshot.user().mobile(),
                        snapshot.user().email()),
                snapshot.clientCode(),
                snapshot.roles(),
                snapshot.domains().stream()
                        .map(domain -> new IamDtos.DomainView(domain.id(), domain.code(), domain.name()))
                        .toList(),
                menuTree,
                snapshot.actions().stream()
                        .map(action -> new IamDtos.ActionView(
                                action.resourceCode(),
                                action.resourceName(),
                                action.httpMethod(),
                                action.pathPattern()))
                        .toList(),
                snapshot.issuedAt());
    }

    private UserContext requireCurrentContext() {
        return UserContextHolder.current()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, ErrorCodes.UNAUTHORIZED.message()));
    }

    private void requirePermission(UserContext context, String permissionCode, List<Long> businessDomainIds) {
        if (!iamService.hasPermissionForDomains(context, permissionCode, businessDomainIds)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, ErrorCodes.FORBIDDEN.message());
        }
    }

    private void requireUserMutationPermission(
            UserContext context,
            long userId,
            List<String> requestedRoleCodes,
            List<Long> requestedBusinessDomainIds,
            String platformPermissionCode,
            String domainPermissionCode) {
        if (requestedRoleCodes != null && !requestedRoleCodes.isEmpty()) {
            if (iamService.containsGlobalRole(requestedRoleCodes)) {
                requirePermission(context, platformPermissionCode, List.of());
                return;
            }
            requirePermission(
                    context,
                    domainPermissionCode,
                    requestedBusinessDomainIds != null ? requestedBusinessDomainIds : List.of());
            return;
        }
        UserAccount existing = iamService.loadUser(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, ErrorCodes.NOT_FOUND.message()));
        if (iamService.containsGlobalRole(existing.roleCodes())) {
            requirePermission(context, platformPermissionCode, List.of());
        } else {
            requirePermission(context, domainPermissionCode, existing.businessDomainIds());
        }
    }

    private IamDtos.ResourceView toResourceView(IamResource resource) {
        return new IamDtos.ResourceView(
                resource.id(),
                resource.resourceType(),
                resource.resourceCode(),
                resource.resourceName(),
                resource.clientScope(),
                resource.httpMethod(),
                resource.pathPattern(),
                resource.parentId(),
                resource.orderNo(),
                resource.icon(),
                resource.component(),
                resource.hidden(),
                resource.status());
    }

    private IamDtos.ResourceView toResourceView(AdminMenuNode node) {
        return new IamDtos.ResourceView(
                node.id(),
                node.nodeType(),
                node.code(),
                node.name(),
                "ud-admin-web",
                null,
                node.routePath(),
                node.parentId(),
                node.orderNo(),
                node.icon(),
                node.componentKey(),
                node.hidden(),
                node.status());
    }

    private IamDtos.MenuTreeNodeView toMenuTreeNodeView(AdminMenuNode node) {
        return new IamDtos.MenuTreeNodeView(
                node.id(),
                node.code(),
                node.nodeType(),
                node.scope(),
                node.name(),
                node.routePath(),
                node.componentKey(),
                node.permissionCode(),
                node.parentId(),
                node.orderNo(),
                node.icon(),
                node.hidden(),
                node.status(),
                node.required(),
                node.children().stream().map(this::toMenuTreeNodeView).toList());
    }

    private IamDtos.MenuView toMenuView(AdminMenuNode node) {
        return new IamDtos.MenuView(
                node.id(),
                node.code(),
                node.name(),
                node.routePath(),
                node.parentId(),
                node.orderNo(),
                node.icon(),
                node.componentKey(),
                node.scope(),
                node.hidden(),
                node.permissionCode(),
                node.children().stream().map(this::toMenuView).toList());
    }

    private IamDtos.RoleView toRoleView(RoleView role) {
        return new IamDtos.RoleView(role.id(), role.code(), role.name(), role.scope(), role.system());
    }

    private IamDtos.UserAccountView toUserAccountView(UserAccount user) {
        return new IamDtos.UserAccountView(
                user.id(),
                user.username(),
                user.nickname(),
                user.mobile(),
                user.email(),
                user.remark(),
                user.accountType(),
                user.status(),
                user.employmentStatus(),
                user.roleCodes(),
                user.businessDomainIds(),
                user.organizationIds(),
                user.offboardedAt(),
                user.offboardedBy(),
                user.offboardReason());
    }
}
