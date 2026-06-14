package com.uniondesk.iam.core;

import com.uniondesk.auth.core.UserContext;
import com.uniondesk.iam.admin.AdminMenuService;
import com.uniondesk.iam.admin.AdminPermissionCatalog;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import com.uniondesk.iam.entity.EffectivePermissionGrantPo;
import com.uniondesk.iam.entity.IamResourcePo;
import com.uniondesk.iam.entity.RolePo;
import com.uniondesk.iam.entity.UserAccountPo;
import com.uniondesk.iam.entity.UserSummaryPo;
import com.uniondesk.iam.mapper.RoleMapper.BusinessDomainSummary;
import com.uniondesk.iam.repository.IamRepository;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class IamService {

    private static final Duration CACHE_TTL = Duration.ofSeconds(30);
    private static final List<String> PROTECTED_ROLE_CODES = List.of("platform_admin", "super_admin");

    private final IamRepository iamRepository;
    private final Clock clock;
    private final PasswordEncoder passwordEncoder;
    private final AdminMenuService adminMenuService;
    private final PermissionScopePolicy permissionScopePolicy;
    private final OrganizationService organizationService;
    private final Map<String, CacheEntry<List<IamResource>>> menuResourceCache = new ConcurrentHashMap<>();

    public IamService(
            IamRepository iamRepository,
            Clock clock,
            PasswordEncoder passwordEncoder,
            AdminMenuService adminMenuService,
            PermissionScopePolicy permissionScopePolicy,
            OrganizationService organizationService) {
        this.iamRepository = iamRepository;
        this.clock = clock;
        this.passwordEncoder = passwordEncoder;
        this.adminMenuService = adminMenuService;
        this.permissionScopePolicy = permissionScopePolicy;
        this.organizationService = organizationService;
    }

    public boolean hasAnyPermission(UserContext context, List<String> permissionCodes) {
        return hasAnyPermission(context, permissionCodes, null);
    }

    public boolean hasPermissionForDomains(UserContext context, String permissionCode, List<Long> businessDomainIds) {
        if (businessDomainIds == null || businessDomainIds.isEmpty()) {
            return hasAnyPermission(context, List.of(permissionCode));
        }
        return businessDomainIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .allMatch(domainId -> hasAnyPermission(context, List.of(permissionCode), domainId));
    }

    public boolean containsGlobalRole(List<String> roleCodes) {
        if (roleCodes == null || roleCodes.isEmpty()) {
            return false;
        }
        return loadRoleDefinitions(roleCodes).values().stream()
                .anyMatch(role -> "global".equals(role.scope()));
    }

    private boolean hasAnyPermission(UserContext context, List<String> permissionCodes, Long targetBusinessDomainId) {
        if (context == null || permissionCodes == null || permissionCodes.isEmpty()) {
            return false;
        }
        List<String> normalizedCodes = permissionCodes.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .toList();
        if (normalizedCodes.isEmpty()) {
            return false;
        }
        try {
            List<EffectivePermissionGrant> grants = loadEffectivePermissionGrants(context.userId(), normalizedCodes);
            return grants.stream().anyMatch(grant -> permissionScopePolicy.isPermissionEffective(
                    grant.roleLevel(),
                    grant.bindingScope(),
                    grant.businessDomainId(),
                    grant.permissionScope(),
                    targetBusinessDomainId));
        } catch (DataAccessException ex) {
            return false;
        }
    }

    public List<IamResource> listCurrentMenuResources(UserContext context) {
        if (context == null) {
            return List.of();
        }
        String cacheKey = cacheKey(context.role(), context.clientCode());
        return getOrLoad(menuResourceCache, cacheKey, () -> iamRepository
                .findMenuByRoleAndClient(context.role(), context.clientCode()).stream()
                .map(this::toIamResource)
                .toList());
    }

    public List<IamResource> listCurrentActionResources(UserContext context) {
        if (context == null) {
            return List.of();
        }
        String cacheKey = cacheKey(context.role(), context.clientCode());
        return getOrLoad(menuResourceCache, cacheKey + "#actions", () -> iamRepository
                .findActionByRoleAndClient(context.role(), context.clientCode()).stream()
                .map(this::toIamResource)
                .toList());
    }

    public List<String> listUserRoleCodesByClient(long userId, String clientCode) {
        if (userId <= 0 || !StringUtils.hasText(clientCode)) {
            return List.of();
        }
        List<String> roleCodes;
        if ("ud-admin-web".equalsIgnoreCase(clientCode)) {
            roleCodes = iamRepository.findUserRoleCodesByClientAdmin(userId);
        } else {
            roleCodes = iamRepository.findUserRoleCodesByClientOther(userId, clientCode.trim().toLowerCase());
        }
        if (roleCodes == null || roleCodes.isEmpty()) {
            return List.of();
        }
        List<String> sortedRoleCodes = new java.util.ArrayList<>(roleCodes);
        sortedRoleCodes.sort(Comparator.comparingInt(IamService::rolePriority));
        return List.copyOf(sortedRoleCodes);
    }

    public PermissionSnapshot loadPermissionSnapshot(UserContext context) {
        if (context == null) {
            throw new IllegalArgumentException("context is required");
        }
        UserSummary user = loadCurrentUserSummary(context.userId());
        List<String> roles = listUserRoleCodesByClient(context.userId(), context.clientCode());
        List<DomainSummary> domains = loadDomainSummaries(context.userId(), roles);
        List<AdminMenuService.AdminMenuNode> menuTree = List.of();
        List<IamResource> actions;
        if ("ud-admin-web".equalsIgnoreCase(context.clientCode())) {
            AdminMenuService.PermissionSnapshotData snapshotData = adminMenuService.loadPermissionSnapshot(roles);
            String activeMenuScope = resolveAdminMenuScope(context.role());
            List<AdminMenuService.AdminMenuNode> activeMenus = snapshotData.menus().stream()
                    .filter(menu -> activeMenuScope.equals(menu.scope()))
                    .toList();
            menuTree = AdminMenuService.buildTree(activeMenus);
            List<AdminMenuService.GrantedPermission> scopedActions = snapshotData.actions().stream()
                    .filter(action -> matchesSnapshotActionScope(action.permissionCode(), activeMenuScope))
                    .toList();
            actions = scopedActions.stream()
                    .map(action -> new IamResource(
                            action.id(),
                            "action",
                            action.permissionCode(),
                            action.name(),
                            context.clientCode(),
                            null,
                            action.httpMethod(),
                            action.pathPattern(),
                            action.parentId(),
                            action.orderNo(),
                            null,
                            null,
                            false,
                            1,
                            null))
                    .toList();
        } else {
            actions = loadResourcesForRoles(roles, context.clientCode(), List.of("action", "api"));
        }
        return new PermissionSnapshot(
                user,
                context.clientCode(),
                roles,
                domains,
                menuTree,
                actions,
                Instant.now(clock).toString());
    }

    private String resolveAdminMenuScope(String roleCode) {
        if (!StringUtils.hasText(roleCode)) {
            return "business";
        }
        try {
            Map<String, RoleDefinition> roles = loadRoleDefinitions(List.of(roleCode));
            RoleDefinition role = roles.get(roleCode);
            if (role == null) {
                return "business";
            }
            return "global".equals(role.scope()) ? "platform" : "business";
        }
        catch (IllegalArgumentException ex) {
            return "business";
        }
    }

    private boolean matchesSnapshotActionScope(String permissionCode, String activeMenuScope) {
        if (!StringUtils.hasText(permissionCode)) {
            return false;
        }
        String normalizedCode = permissionCode.trim();
        if ("business".equals(activeMenuScope)) {
            return !normalizedCode.startsWith("platform.");
        }
        return AdminPermissionCatalog.findByCode(normalizedCode).isPresent();
    }

    public List<IamResource> listResources(String resourceType, String clientScope) {
        String normalizedType = null;
        if (StringUtils.hasText(resourceType)) {
            normalizedType = resourceType.trim().toLowerCase();
            if ("api".equals(normalizedType)) {
                normalizedType = "action";
            }
        }
        String normalizedScope = StringUtils.hasText(clientScope) ? clientScope.trim().toLowerCase() : null;
        return iamRepository.findResources(normalizedType, normalizedScope).stream()
                .map(this::toIamResource)
                .toList();
    }

    public IamResource createResource(CreateResourceCommand command) {
        ValidatedResource validated = validateResource(
                command.resourceType(),
                command.resourceCode(),
                command.resourceName(),
                command.clientScope(),
                command.httpMethod(),
                command.pathPattern(),
                command.parentId(),
                command.orderNo(),
                command.icon(),
                command.component(),
                command.hidden(),
                command.status());
        iamRepository.insertResource(toIamResourcePo(validated));
        evictAuthorizationCache();
        return findResourceByCode(validated.resourceCode())
                .orElseThrow(() -> new IllegalStateException("resource create failed"));
    }

    public IamResource updateResource(long resourceId, UpdateResourceCommand command) {
        IamResource existing = findResourceById(resourceId)
                .orElseThrow(() -> new IllegalArgumentException("resource not found"));
        ValidatedResource validated = validateResource(
                command.resourceType() != null ? command.resourceType() : existing.resourceType(),
                command.resourceCode() != null ? command.resourceCode() : existing.resourceCode(),
                command.resourceName() != null ? command.resourceName() : existing.resourceName(),
                command.clientScope() != null ? command.clientScope() : existing.clientScope(),
                command.httpMethod() != null ? command.httpMethod() : existing.httpMethod(),
                command.pathPattern() != null ? command.pathPattern() : existing.pathPattern(),
                command.parentId() != null ? command.parentId() : existing.parentId(),
                command.orderNo() != null ? command.orderNo() : existing.orderNo(),
                command.icon() != null ? command.icon() : existing.icon(),
                command.component() != null ? command.component() : existing.component(),
                command.hidden() != null ? command.hidden() : existing.hidden(),
                command.status() != null ? command.status() : existing.status());
        IamResourcePo po = toIamResourcePo(validated);
        po.setId(resourceId);
        iamRepository.updateResource(po);
        evictAuthorizationCache();
        return findResourceById(resourceId)
                .orElseThrow(() -> new IllegalArgumentException("resource not found"));
    }

    public List<IamResource> listRoleResources(int roleId) {
        assertRoleExists(roleId);
        return iamRepository.findResourcesByRoleId(roleId).stream().map(this::toIamResource).toList();
    }

    @Transactional
    public List<IamResource> replaceRoleResources(int roleId, List<Long> resourceIds) {
        assertRoleExists(roleId);
        Set<Long> deduplicatedIds = resourceIds == null ? Set.of() : new LinkedHashSet<>(resourceIds);
        ensureResourceIdsExist(deduplicatedIds);
        iamRepository.deleteRoleResources(roleId);
        for (Long resourceId : deduplicatedIds) {
            iamRepository.insertRoleResource(roleId, resourceId);
        }
        evictAuthorizationCache();
        return listRoleResources(roleId);
    }

    public List<MenuTreeNode> listMenuTree(String clientScope) {
        List<IamResource> resources = iamRepository.findMenuTree(clientScope).stream()
                .map(this::toIamResource)
                .toList();
        Map<Long, MenuTreeNodeBuilder> byId = new LinkedHashMap<>();
        for (IamResource resource : resources) {
            byId.put(resource.id(), new MenuTreeNodeBuilder(resource));
        }
        List<MenuTreeNodeBuilder> roots = new ArrayList<>();
        for (MenuTreeNodeBuilder node : byId.values()) {
            Long parentId = node.resource.parentId();
            if (parentId == null || !byId.containsKey(parentId)) {
                roots.add(node);
                continue;
            }
            byId.get(parentId).children.add(node);
        }
        roots.sort(Comparator.comparingInt(left -> left.resource.orderNo()));
        return roots.stream().map(MenuTreeNodeBuilder::toView).toList();
    }

    public IamResource createMenu(CreateMenuCommand command) {
        CreateResourceCommand createCommand = new CreateResourceCommand(
                "menu",
                command.resourceCode(),
                command.resourceName(),
                command.clientScope(),
                null,
                command.path(),
                command.parentId(),
                command.orderNo(),
                command.icon(),
                command.component(),
                command.hidden(),
                command.status());
        return createResource(createCommand);
    }

    public IamResource updateMenu(long menuId, UpdateMenuCommand command) {
        IamResource existing = findResourceById(menuId)
                .orElseThrow(() -> new IllegalArgumentException("menu not found"));
        if (!"menu".equals(existing.resourceType())) {
            throw new IllegalArgumentException("resource is not menu");
        }
        UpdateResourceCommand updateCommand = new UpdateResourceCommand(
                "menu",
                command.resourceCode(),
                command.resourceName(),
                command.clientScope(),
                null,
                command.path(),
                command.parentId(),
                command.orderNo(),
                command.icon(),
                command.component(),
                command.hidden(),
                command.status());
        return updateResource(menuId, updateCommand);
    }

    @Transactional
    public void deleteMenu(long menuId) {
        IamResource existing = findResourceById(menuId)
                .orElseThrow(() -> new IllegalArgumentException("menu not found"));
        if (!"menu".equals(existing.resourceType())) {
            throw new IllegalArgumentException("resource is not menu");
        }
        if (iamRepository.countResourceChildren(menuId) > 0) {
            throw new IllegalArgumentException("menu has children, delete child menus first");
        }
        if (iamRepository.countResourceBindings(menuId) > 0) {
            throw new IllegalArgumentException("menu is bound to roles, unbind first");
        }
        iamRepository.deleteResource(menuId);
        evictAuthorizationCache();
    }

    public List<RoleView> listRoles() {
        return iamRepository.findAllRoles().stream().map(this::toRoleView).toList();
    }

    public RoleView createRole(CreateRoleCommand command) {
        String normalizedCode = normalize(command.code(), "code");
        String normalizedName = normalize(command.name(), "name");
        String normalizedScope = normalize(command.scope(), "scope").toLowerCase();
        if (!List.of("global", "domain").contains(normalizedScope)) {
            throw new IllegalArgumentException("unsupported role scope");
        }
        try {
            RolePo po = new RolePo();
            po.setCode(normalizedCode);
            po.setName(normalizedName);
            po.setScope(normalizedScope);
            po.setIsSystem(0);
            iamRepository.insertRole(po);
        } catch (DuplicateKeyException ex) {
            throw new IllegalArgumentException("role code already exists");
        }
        return findRoleByCode(normalizedCode)
                .orElseThrow(() -> new IllegalStateException("role create failed"));
    }

    public RoleView updateRole(int roleId, UpdateRoleCommand command) {
        RoleView existing = findRoleById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("role not found"));
        String code = command.code() != null ? normalize(command.code(), "code") : existing.code();
        String name = command.name() != null ? normalize(command.name(), "name") : existing.name();
        String scope = command.scope() != null ? normalize(command.scope(), "scope").toLowerCase() : existing.scope();
        if (!List.of("global", "domain").contains(scope)) {
            throw new IllegalArgumentException("unsupported role scope");
        }
        try {
            RolePo po = new RolePo();
            po.setId(roleId);
            po.setCode(code);
            po.setName(name);
            po.setScope(scope);
            iamRepository.updateRole(po);
        } catch (DuplicateKeyException ex) {
            throw new IllegalArgumentException("role code already exists");
        }
        return findRoleById(roleId).orElseThrow(() -> new IllegalArgumentException("role not found"));
    }

    @Transactional
    public void deleteRole(int roleId) {
        RoleView existing = findRoleById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("role not found"));
        if (existing.system()) {
            throw new IllegalArgumentException("system role cannot be deleted");
        }
        if (iamRepository.countUserGlobalRoleBindings(roleId) > 0
                || iamRepository.countUserDomainRoleBindings(roleId) > 0) {
            throw new IllegalArgumentException("role is bound to users, unbind first");
        }
        iamRepository.deleteRolePermissions(roleId);
        iamRepository.deleteRoleBindings(roleId);
        iamRepository.deleteRoleResources(roleId);
        iamRepository.deleteRole(roleId);
        evictAuthorizationCache();
    }

    public RolePermissions loadRolePermissions(int roleId) {
        assertRoleExists(roleId);
        List<Long> menuResourceIds = iamRepository.findMenuResourceIdsByRole(roleId);
        List<Long> actionResourceIds = iamRepository.findActionResourceIdsByRole(roleId);
        return new RolePermissions(roleId, menuResourceIds, actionResourceIds);
    }

    @Transactional
    public RolePermissions replaceRolePermissions(int roleId, List<Long> menuResourceIds, List<Long> actionResourceIds) {
        assertRoleExists(roleId);
        Set<Long> menus = menuResourceIds == null ? Set.of() : new LinkedHashSet<>(menuResourceIds);
        Set<Long> actions = actionResourceIds == null ? Set.of() : new LinkedHashSet<>(actionResourceIds);
        ensureResourceIdsByType(menus, "menu");
        ensureResourceIdsByType(actions, "action");
        iamRepository.deleteRoleResources(roleId);
        for (Long resourceId : menus) {
            iamRepository.insertRoleResource(roleId, resourceId);
        }
        for (Long resourceId : actions) {
            iamRepository.insertRoleResource(roleId, resourceId);
        }
        evictAuthorizationCache();
        return loadRolePermissions(roleId);
    }

    public List<UserAccount> listUsers(boolean offboardedOnly, Long organizationId) {
        final List<Long> targetOrgIds;
        if (organizationId != null) {
            targetOrgIds = organizationService.collectDescendantOrgIds(organizationId);
        } else {
            targetOrgIds = List.of();
        }
        List<UserAccount> users = iamRepository.findUsersByEmploymentStatus(offboardedOnly).stream()
                .map(po -> toUserAccount(po, listUserRoleCodes(po.getId()), listUserDomainIds(po.getId()),
                        listUserOrganizationIdsOrEmpty(po.getId())))
                .toList();
        return users.stream()
                .filter(user -> targetOrgIds.isEmpty() || user.organizationIds().stream().anyMatch(targetOrgIds::contains))
                .toList();
    }

    @Transactional
    public UserAccount createUser(CreateUserCommand command) {
        String accountType = normalize(command.accountType(), "accountType").toLowerCase();
        if ("admin".equals(accountType)) {
            throw new IllegalArgumentException("请使用 /api/v1/admin/staff 创建员工账号");
        }
        if ("customer".equals(accountType)) {
            throw new IllegalArgumentException("请使用客户注册或域客户 API 创建客户账号");
        }
        throw new IllegalArgumentException("unsupported account type");
    }

    @Transactional
    public UserAccount updateUser(long userId, UpdateUserCommand command) {
        UserAccount existing = loadUser(userId).orElseThrow(() -> new IllegalArgumentException("user not found"));
        if (command.username() != null || command.nickname() != null || command.mobile() != null
                || command.email() != null || command.remark() != null || command.password() != null
                || command.accountType() != null || command.status() != null) {
            if (command.accountType() != null) {
                String accountType = normalize(command.accountType(), "accountType").toLowerCase();
                if (!List.of("admin", "customer").contains(accountType)) {
                    throw new IllegalArgumentException("unsupported account type");
                }
            }
            try {
                iamRepository.updateUserSelective(
                        userId,
                        command.username() != null ? normalize(command.username(), "username") : null,
                        command.nickname() != null ? (StringUtils.hasText(command.nickname()) ? command.nickname().trim() : null) : null,
                        command.mobile() != null ? normalize(command.mobile(), "mobile") : null,
                        command.email() != null ? (StringUtils.hasText(command.email()) ? command.email().trim() : null) : null,
                        command.remark() != null ? normalizeOptional(command.remark()) : null,
                        command.password() != null ? passwordEncoder.encode(normalize(command.password(), "password")) : null,
                        command.accountType() != null ? normalize(command.accountType(), "accountType").toLowerCase() : null,
                        command.status());
            } catch (DuplicateKeyException ex) {
                throw new IllegalArgumentException("username/mobile/email already exists");
            }
        }
        if (command.roleCodes() != null || command.businessDomainIds() != null || command.organizationIds() != null) {
            List<String> roleCodes = command.roleCodes() != null
                    ? command.roleCodes().stream().filter(StringUtils::hasText).map(String::trim).toList()
                    : existing.roleCodes();
            List<Long> businessDomainIds = command.businessDomainIds() != null
                    ? command.businessDomainIds().stream().filter(Objects::nonNull).distinct().toList()
                    : existing.businessDomainIds();
            List<Long> organizationIds = command.organizationIds() != null
                    ? command.organizationIds().stream().filter(Objects::nonNull).distinct().toList()
                    : existing.organizationIds();
            replaceUserRoleBindings(userId, roleCodes, businessDomainIds);
            organizationService.replaceUserOrganizations(userId, organizationIds);
        }
        return loadUser(userId).orElseThrow(() -> new IllegalArgumentException("user not found"));
    }

    @Transactional
    public UserAccount offboardUser(long userId, long operatorUserId, String reason) {
        UserAccount existing = loadUser(userId).orElseThrow(() -> new IllegalArgumentException("user not found"));
        if ("offboarded".equals(existing.employmentStatus())) {
            return existing;
        }
        guardLastProtectedRoleHolder(userId);
        LocalDateTime now = LocalDateTime.now(clock);
        iamRepository.offboardUser(userId, now, operatorUserId, StringUtils.hasText(reason) ? reason.trim() : null);
        iamRepository.revokeSessionsOnOffboard(userId, now);
        return loadUser(userId).orElseThrow(() -> new IllegalArgumentException("user not found"));
    }

    @Transactional
    public UserAccount restoreUser(long userId) {
        UserAccount existing = loadUser(userId).orElseThrow(() -> new IllegalArgumentException("user not found"));
        if ("active".equals(existing.employmentStatus())) {
            return existing;
        }
        iamRepository.restoreUser(userId);
        return loadUser(userId).orElseThrow(() -> new IllegalArgumentException("user not found"));
    }

    @Transactional
    public void deleteUserPermanently(long userId) {
        UserAccount existing = loadUser(userId).orElseThrow(() -> new IllegalArgumentException("user not found"));
        if (!"offboarded".equals(existing.employmentStatus())) {
            throw new IllegalArgumentException("user must be offboarded before permanent delete");
        }
        if (iamRepository.countTicketReferences(userId) > 0 || iamRepository.countConsultationReferences(userId) > 0) {
            throw new IllegalArgumentException("user has ticket/consultation references and cannot be deleted");
        }
        iamRepository.clearOffboardedBy(userId);
        iamRepository.deleteUserDomainRoles(userId);
        iamRepository.deleteUserGlobalRoles(userId);
        iamRepository.deleteUserOrganizations(userId);
        iamRepository.deleteLoginLogsByUsername(userId);
        iamRepository.deleteSessions(userId);
        iamRepository.deleteUser(userId);
    }

    public void evictAuthorizationCache() {
        menuResourceCache.clear();
    }

    private List<IamResource> loadResourcesForRoles(List<String> roleCodes, String clientCode, List<String> resourceTypes) {
        if (roleCodes == null || roleCodes.isEmpty() || resourceTypes == null || resourceTypes.isEmpty()) {
            return List.of();
        }
        return iamRepository.findResourcesForRoles(roleCodes, resourceTypes, clientCode).stream()
                .map(this::toIamResource)
                .toList();
    }

    private List<DomainSummary> loadDomainSummaries(long userId, List<String> roleCodes) {
        if (roleCodes == null || roleCodes.isEmpty()) {
            return List.of();
        }
        if (roleCodes.contains("super_admin")) {
            return iamRepository.findDomainSummariesForSuperAdmin().stream()
                    .map(this::toDomainSummary)
                    .toList();
        }
        return iamRepository.findDomainSummariesForUser(userId, roleCodes).stream()
                .map(this::toDomainSummary)
                .toList();
    }

    private UserSummary loadCurrentUserSummary(long userId) {
        UserSummaryPo po = iamRepository.findUserSummaryById(userId)
                .orElseThrow(() -> new IllegalArgumentException("user not found"));
        return new UserSummary(po.getId(), po.getUsername(), po.getMobile(), po.getEmail());
    }

    private List<String> listUserRoleCodes(long userId) {
        List<String> roleCodes = new ArrayList<>(iamRepository.findUserRoleCodes(userId));
        roleCodes.sort(Comparator.comparingInt(IamService::rolePriority));
        return roleCodes;
    }

    private List<Long> listUserOrganizationIdsOrEmpty(long userId) {
        try {
            List<Long> organizationIds = organizationService.listUserOrganizationIds(userId);
            return organizationIds == null ? List.of() : organizationIds;
        } catch (DataAccessException ex) {
            // Organization links are optional for reads; missing relation data should not
            // prevent the platform user list from rendering.
            return List.of();
        }
    }

    private void guardLastProtectedRoleHolder(long userId) {
        List<String> currentRoles = listUserRoleCodes(userId);
        for (String protectedRole : PROTECTED_ROLE_CODES) {
            if (currentRoles.contains(protectedRole)) {
                guardLastProtectedRoleHolder(userId, protectedRole);
            }
        }
    }

    private void guardLastProtectedRoleHolder(long userId, String roleCode) {
        if (iamRepository.countProtectedRoleHoldersExcluding(roleCode, userId) == 0) {
            throw new IllegalArgumentException("cannot remove the last " + roleCode);
        }
    }

    private List<Long> listUserDomainIds(long userId) {
        return iamRepository.findUserDomainIds(userId);
    }

    public Optional<UserAccount> loadUser(long userId) {
        return iamRepository.findUserById(userId)
                .map(po -> toUserAccount(po, listUserRoleCodes(userId), listUserDomainIds(userId),
                        listUserOrganizationIdsOrEmpty(userId)));
    }

    private void replaceUserRoleBindings(long userId, List<String> roleCodes, List<Long> businessDomainIds) {
        if (roleCodes == null || roleCodes.isEmpty()) {
            throw new IllegalArgumentException("roleCodes is required");
        }
        List<String> currentRoles = listUserRoleCodes(userId);
        Set<String> newRoleSet = new LinkedHashSet<>(roleCodes);
        for (String protectedRole : PROTECTED_ROLE_CODES) {
            if (currentRoles.contains(protectedRole) && !newRoleSet.contains(protectedRole)) {
                guardLastProtectedRoleHolder(userId, protectedRole);
            }
        }
        Map<String, RoleDefinition> roleMap = loadRoleDefinitions(roleCodes);
        Set<Long> domainIds = businessDomainIds == null ? Set.of() : new LinkedHashSet<>(businessDomainIds);
        boolean hasDomainRole = roleMap.values().stream().anyMatch(role -> "domain".equals(role.scope()));
        if (hasDomainRole && domainIds.isEmpty()) {
            throw new IllegalArgumentException("domain scoped roles require businessDomainIds");
        }
        ensureDomainsExist(domainIds);
        iamRepository.deleteUserGlobalRoles(userId);
        iamRepository.deleteUserDomainRoles(userId);
        iamRepository.deleteUserRoleBindings(userId);
        for (RoleDefinition role : roleMap.values()) {
            if ("global".equals(role.scope())) {
                iamRepository.insertUserGlobalRole(userId, role.id());
                iamRepository.insertRoleBindingGlobal(userId, role.id());
                continue;
            }
            for (Long domainId : domainIds) {
                iamRepository.insertUserDomainRole(userId, role.id(), domainId);
                iamRepository.insertRoleBindingDomain(userId, role.id(), domainId);
            }
        }
        evictAuthorizationCache();
    }

    private Map<String, RoleDefinition> loadRoleDefinitions(List<String> roleCodes) {
        List<String> normalizedCodes = roleCodes.stream().map(code -> normalize(code, "roleCode")).distinct().toList();
        List<RoleDefinition> definitions = iamRepository.findRolesByCodes(normalizedCodes).stream()
                .map(po -> new RoleDefinition(po.getId(), po.getCode(), po.getScope()))
                .toList();
        if (definitions.size() != normalizedCodes.size()) {
            throw new IllegalArgumentException("role not found");
        }
        Map<String, RoleDefinition> byCode = new LinkedHashMap<>();
        for (RoleDefinition definition : definitions) {
            byCode.put(definition.code(), definition);
        }
        return byCode;
    }

    private void ensureDomainsExist(Set<Long> domainIds) {
        if (domainIds.isEmpty()) {
            return;
        }
        if (iamRepository.countDomainsByIds(new ArrayList<>(domainIds)) != domainIds.size()) {
            throw new IllegalArgumentException("business domain not found");
        }
    }

    private void ensureResourceIdsExist(Set<Long> resourceIds) {
        if (resourceIds.isEmpty()) {
            return;
        }
        if (iamRepository.countResourcesByIds(new ArrayList<>(resourceIds)) != resourceIds.size()) {
            throw new IllegalArgumentException("resource not found");
        }
    }

    private void ensureResourceIdsByType(Set<Long> resourceIds, String resourceType) {
        if (resourceIds.isEmpty()) {
            return;
        }
        if (iamRepository.countResourcesByIdsAndType(new ArrayList<>(resourceIds), resourceType) != resourceIds.size()) {
            throw new IllegalArgumentException(resourceType + " resource not found");
        }
    }

    private List<EffectivePermissionGrant> loadEffectivePermissionGrants(long userId, List<String> permissionCodes) {
        return iamRepository.findEffectiveGrants(userId, permissionCodes).stream()
                .map(this::toEffectivePermissionGrant)
                .toList();
    }

    private <T> T getOrLoad(Map<String, CacheEntry<T>> cache, String key, Supplier<T> supplier) {
        CacheEntry<T> cached = cache.get(key);
        Instant now = Instant.now(clock);
        if (cached != null && cached.expiresAt().isAfter(now)) {
            return cached.value();
        }
        synchronized (cache) {
            CacheEntry<T> latest = cache.get(key);
            if (latest != null && latest.expiresAt().isAfter(now)) {
                return latest.value();
            }
            T loaded = supplier.get();
            cache.put(key, new CacheEntry<>(loaded, now.plus(CACHE_TTL)));
            return loaded;
        }
    }

    private String cacheKey(String roleCode, String clientCode) {
        return (roleCode == null ? "" : roleCode.trim().toLowerCase())
                + "@"
                + (clientCode == null ? "" : clientCode.trim().toLowerCase());
    }

    private Optional<IamResource> findResourceById(long resourceId) {
        return iamRepository.findResourceById(resourceId).map(this::toIamResource);
    }

    private Optional<IamResource> findResourceByCode(String resourceCode) {
        return iamRepository.findResourceByCode(resourceCode).map(this::toIamResource);
    }

    private Optional<RoleView> findRoleById(int roleId) {
        return iamRepository.findRoleById(roleId).map(this::toRoleView);
    }

    private Optional<RoleView> findRoleByCode(String roleCode) {
        return iamRepository.findRoleByCode(roleCode).map(this::toRoleView);
    }

    private void assertRoleExists(int roleId) {
        if (iamRepository.countRoleById(roleId) == 0) {
            throw new IllegalArgumentException("role not found");
        }
    }

    private ValidatedResource validateResource(
            String resourceType,
            String resourceCode,
            String resourceName,
            String clientScope,
            String httpMethod,
            String pathPattern,
            Long parentId,
            Integer orderNo,
            String icon,
            String component,
            Boolean hidden,
            Integer status) {
        String normalizedType = normalize(resourceType, "resourceType").toLowerCase();
        String normalizedCode = normalize(resourceCode, "resourceCode");
        String normalizedName = normalize(resourceName, "resourceName");
        String normalizedScope = normalize(clientScope, "clientScope").toLowerCase();
        Integer normalizedStatus = status == null ? 1 : status;
        if ("api".equals(normalizedType)) {
            normalizedType = "action";
        }
        if (!List.of("menu", "action").contains(normalizedType)) {
            throw new IllegalArgumentException("unsupported resource type");
        }
        if (!List.of("ud-admin-web", "ud-customer-web", "all").contains(normalizedScope)) {
            throw new IllegalArgumentException("unsupported client scope");
        }
        String normalizedMethod = null;
        String normalizedPath = StringUtils.hasText(pathPattern) ? pathPattern.trim() : null;
        if ("action".equals(normalizedType)) {
            if (!StringUtils.hasText(httpMethod) || !StringUtils.hasText(normalizedPath)) {
                throw new IllegalArgumentException("action resource requires method and pathPattern");
            }
            normalizedMethod = httpMethod.trim().toUpperCase();
            if (!List.of("GET", "POST", "PUT", "PATCH", "DELETE").contains(normalizedMethod)) {
                throw new IllegalArgumentException("unsupported http method");
            }
            parentId = null;
        } else if (!StringUtils.hasText(normalizedPath)) {
            throw new IllegalArgumentException("menu resource requires path");
        }
        if (parentId != null) {
            IamResource parent = findResourceById(parentId).orElseThrow(() -> new IllegalArgumentException("parent menu not found"));
            if (!"menu".equals(parent.resourceType())) {
                throw new IllegalArgumentException("parent resource must be menu");
            }
        }
        return new ValidatedResource(
                normalizedType,
                normalizedCode,
                normalizedName,
                normalizedScope,
                normalizedMethod,
                normalizedPath,
                parentId,
                orderNo == null ? 0 : orderNo,
                StringUtils.hasText(icon) ? icon.trim() : null,
                StringUtils.hasText(component) ? component.trim() : null,
                hidden != null && hidden,
                normalizedStatus);
    }

    private IamResource toIamResource(IamResourcePo po) {
        return new IamResource(
                po.getId(),
                po.getResourceType(),
                po.getResourceCode(),
                po.getResourceName(),
                po.getClientScope(),
                null,
                po.getHttpMethod(),
                po.getPathPattern(),
                po.getParentId(),
                po.getOrderNo() == null ? 0 : po.getOrderNo(),
                po.getIcon(),
                po.getComponent(),
                po.getHidden() != null && po.getHidden() == 1,
                po.getStatus() == null ? 0 : po.getStatus(),
                null);
    }

    private IamResourcePo toIamResourcePo(ValidatedResource validated) {
        IamResourcePo po = new IamResourcePo();
        po.setResourceType(validated.resourceType());
        po.setResourceCode(validated.resourceCode());
        po.setResourceName(validated.resourceName());
        po.setClientScope(validated.clientScope());
        po.setHttpMethod(validated.httpMethod());
        po.setPathPattern(validated.pathPattern());
        po.setParentId(validated.parentId());
        po.setOrderNo(validated.orderNo());
        po.setIcon(validated.icon());
        po.setComponent(validated.component());
        po.setHidden(validated.hidden() ? 1 : 0);
        po.setStatus(validated.status());
        return po;
    }

    private RoleView toRoleView(RolePo po) {
        return new RoleView(
                po.getId(),
                po.getCode(),
                po.getName(),
                po.getScope(),
                po.getIsSystem() != null && po.getIsSystem() == 1);
    }

    private DomainSummary toDomainSummary(BusinessDomainSummary summary) {
        return new DomainSummary(summary.id(), summary.code(), summary.name());
    }

    private UserAccount toUserAccount(UserAccountPo po, List<String> roleCodes, List<Long> domainIds, List<Long> orgIds) {
        return new UserAccount(
                po.getId(),
                po.getUsername(),
                po.getNickname(),
                po.getMobile(),
                po.getEmail(),
                po.getRemark(),
                po.getAccountType(),
                po.getStatus() == null ? 0 : po.getStatus(),
                po.getEmploymentStatus(),
                po.getOffboardedAt() == null ? null : po.getOffboardedAt().toString(),
                po.getOffboardedBy(),
                po.getOffboardReason(),
                roleCodes,
                domainIds,
                orgIds);
    }

    private EffectivePermissionGrant toEffectivePermissionGrant(EffectivePermissionGrantPo po) {
        return new EffectivePermissionGrant(
                po.getRoleLevel(),
                po.getBindingScope(),
                po.getBusinessDomainId(),
                po.getPermissionScope());
    }

    private String normalize(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    private String normalizeOptional(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private static int rolePriority(String role) {
        return switch (role) {
            case "super_admin" -> 0;
            case "domain_admin" -> 1;
            case "agent" -> 2;
            case "customer" -> 3;
            default -> 10;
        };
    }

    private record CacheEntry<T>(T value, Instant expiresAt) {
    }

    private record ValidatedResource(
            String resourceType,
            String resourceCode,
            String resourceName,
            String clientScope,
            String httpMethod,
            String pathPattern,
            Long parentId,
            int orderNo,
            String icon,
            String component,
            boolean hidden,
            int status) {
    }

    private static final class MenuTreeNodeBuilder {
        private final IamResource resource;
        private final List<MenuTreeNodeBuilder> children = new ArrayList<>();

        private MenuTreeNodeBuilder(IamResource resource) {
            this.resource = resource;
        }

        private MenuTreeNode toView() {
            children.sort(Comparator.comparingInt(left -> left.resource.orderNo()));
            return new MenuTreeNode(
                    resource.id(),
                    resource.resourceCode(),
                    resource.resourceName(),
                    resource.pathPattern(),
                    resource.clientScope(),
                    resource.parentId(),
                    resource.orderNo(),
                    resource.icon(),
                    resource.component(),
                    resource.hidden(),
                    resource.status(),
                    children.stream().map(MenuTreeNodeBuilder::toView).toList());
        }
    }

    private record RoleDefinition(int id, String code, String scope) {
    }

    public record IamResource(
            long id,
            String resourceType,
            String resourceCode,
            String resourceName,
            String clientScope,
            String scope,
            String httpMethod,
            String pathPattern,
            Long parentId,
            int orderNo,
            String icon,
            String component,
            boolean hidden,
            int status,
            String permissionCode) {
    }

    public record MenuTreeNode(
            long id,
            String code,
            String name,
            String path,
            String clientScope,
            Long parentId,
            int orderNo,
            String icon,
            String component,
            boolean hidden,
            int status,
            List<MenuTreeNode> children) {
    }

    public record CreateResourceCommand(
            String resourceType,
            String resourceCode,
            String resourceName,
            String clientScope,
            String httpMethod,
            String pathPattern,
            Long parentId,
            Integer orderNo,
            String icon,
            String component,
            Boolean hidden,
            Integer status) {
    }

    public record UpdateResourceCommand(
            String resourceType,
            String resourceCode,
            String resourceName,
            String clientScope,
            String httpMethod,
            String pathPattern,
            Long parentId,
            Integer orderNo,
            String icon,
            String component,
            Boolean hidden,
            Integer status) {
    }

    public record CreateMenuCommand(
            String resourceCode,
            String resourceName,
            String path,
            String clientScope,
            Long parentId,
            Integer orderNo,
            String icon,
            String component,
            Boolean hidden,
            Integer status) {
    }

    public record UpdateMenuCommand(
            String resourceCode,
            String resourceName,
            String path,
            String clientScope,
            Long parentId,
            Integer orderNo,
            String icon,
            String component,
            Boolean hidden,
            Integer status) {
    }

    public record RoleView(
            int id,
            String code,
            String name,
            String scope,
            boolean system) {
    }

    public record CreateRoleCommand(
            String code,
            String name,
            String scope) {
    }

    public record UpdateRoleCommand(
            String code,
            String name,
            String scope) {
    }

    public record RolePermissions(
            int roleId,
            List<Long> menuResourceIds,
            List<Long> actionResourceIds) {
    }

    public record UserAccount(
            long id,
            String username,
            String nickname,
            String mobile,
            String email,
            String remark,
            String accountType,
            int status,
            String employmentStatus,
            String offboardedAt,
            Long offboardedBy,
            String offboardReason,
            List<String> roleCodes,
            List<Long> businessDomainIds,
            List<Long> organizationIds) {
    }

    public record CreateUserCommand(
            String username,
            String nickname,
            String mobile,
            String email,
            String remark,
            String password,
            String accountType,
            List<String> roleCodes,
            List<Long> businessDomainIds,
            List<Long> organizationIds) {
    }

    public record UpdateUserCommand(
            String username,
            String nickname,
            String mobile,
            String email,
            String remark,
            String password,
            String accountType,
            List<String> roleCodes,
            List<Long> businessDomainIds,
            Integer status,
            List<Long> organizationIds) {
    }

    public record UserSummary(
            long id,
            String username,
            String mobile,
            String email) {
    }

    public record DomainSummary(
            long id,
            String code,
            String name) {
    }

    public record PermissionSnapshot(
            UserSummary user,
            String clientCode,
            List<String> roles,
            List<DomainSummary> domains,
            List<AdminMenuService.AdminMenuNode> menuTree,
            List<IamResource> actions,
            String issuedAt) {
    }

    private record EffectivePermissionGrant(
            String roleLevel,
            String bindingScope,
            Long businessDomainId,
            String permissionScope) {
    }
}
