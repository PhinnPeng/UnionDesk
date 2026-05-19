package com.uniondesk.iam.core;

import com.uniondesk.auth.core.UserContext;
import com.uniondesk.iam.admin.AdminMenuService;
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
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;

@Service
public class IamService {

    private static final Duration CACHE_TTL = Duration.ofSeconds(30);
    private static final List<String> PROTECTED_ROLE_CODES = List.of("platform_admin", "super_admin");

    private final JdbcTemplate jdbcTemplate;
    private final Clock clock;
    private final PasswordEncoder passwordEncoder;
    private final AdminMenuService adminMenuService;
    private final PermissionScopePolicy permissionScopePolicy;
    private final OrganizationService organizationService;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final Map<String, CacheEntry<List<ApiGrant>>> apiGrantCache = new ConcurrentHashMap<>();
    private final Map<String, CacheEntry<List<IamResource>>> menuResourceCache = new ConcurrentHashMap<>();

    public IamService(
            JdbcTemplate jdbcTemplate,
            Clock clock,
            PasswordEncoder passwordEncoder,
            AdminMenuService adminMenuService,
            PermissionScopePolicy permissionScopePolicy,
            OrganizationService organizationService) {
        this.jdbcTemplate = jdbcTemplate;
        this.clock = clock;
        this.passwordEncoder = passwordEncoder;
        this.adminMenuService = adminMenuService;
        this.permissionScopePolicy = permissionScopePolicy;
        this.organizationService = organizationService;
    }

    public boolean isApiAllowed(UserContext context, String method, String requestPath) {
        if (context == null || !StringUtils.hasText(method) || !StringUtils.hasText(requestPath)) {
            return false;
        }
        if (requestPath.startsWith("/api/v1/auth/")) {
            return true;
        }
        List<String> matchedPermissionCodes = findPermissionCodesByRequest(method, requestPath);
        if (!matchedPermissionCodes.isEmpty()) {
            return hasAnyPermission(context, matchedPermissionCodes);
        }
        if ("super_admin".equalsIgnoreCase(context.role()) && requestPath.startsWith("/api/v1/iam/")) {
            return true;
        }
        if (adminMenuService.handlesApiRequest(method, requestPath)) {
            return adminMenuService.isApiAllowed(context.role(), method, requestPath);
        }
        List<ApiGrant> grants = loadActionGrants(context.role(), context.clientCode());
        String normalizedMethod = method.trim().toUpperCase();
        return grants.stream()
                .filter(grant -> normalizedMethod.equals(grant.httpMethod()))
                .anyMatch(grant -> pathMatcher.match(grant.pathPattern(), requestPath));
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
        return getOrLoad(menuResourceCache, cacheKey, () -> jdbcTemplate.query("""
                        SELECT
                            ir.id,
                            ir.resource_type,
                            ir.resource_code,
                            ir.resource_name,
                            ir.client_scope,
                            ir.http_method,
                            ir.path_pattern,
                            ir.parent_id,
                            ir.order_no,
                            ir.icon,
                            ir.component,
                            ir.hidden,
                            ir.status
                        FROM role r
                        JOIN iam_role_resource irr ON irr.role_id = r.id
                        JOIN iam_resource ir ON ir.id = irr.resource_id
                        WHERE r.code = ?
                          AND ir.resource_type = 'menu'
                          AND ir.status = 1
                          AND (ir.client_scope = ? OR ir.client_scope = 'all')
                        ORDER BY ir.order_no, ir.id
                        """,
                (rs, rowNum) -> mapResource(rs),
                context.role(),
                context.clientCode()));
    }

    public List<IamResource> listCurrentActionResources(UserContext context) {
        if (context == null) {
            return List.of();
        }
        String cacheKey = cacheKey(context.role(), context.clientCode());
        return getOrLoad(menuResourceCache, cacheKey + "#actions", () -> jdbcTemplate.query("""
                        SELECT
                            ir.id,
                            ir.resource_type,
                            ir.resource_code,
                            ir.resource_name,
                            ir.client_scope,
                            ir.http_method,
                            ir.path_pattern,
                            ir.parent_id,
                            ir.order_no,
                            ir.icon,
                            ir.component,
                            ir.hidden,
                            ir.status
                        FROM role r
                        JOIN iam_role_resource irr ON irr.role_id = r.id
                        JOIN iam_resource ir ON ir.id = irr.resource_id
                        WHERE r.code = ?
                          AND ir.resource_type IN ('action', 'api')
                          AND ir.status = 1
                          AND (ir.client_scope = ? OR ir.client_scope = 'all')
                        ORDER BY ir.resource_code
                        """,
                (rs, rowNum) -> mapResource(rs),
                context.role(),
                context.clientCode()));
    }

    public List<String> listUserRoleCodesByClient(long userId, String clientCode) {
        if (userId <= 0 || !StringUtils.hasText(clientCode)) {
            return List.of();
        }
        List<String> roleCodes;
        if ("ud-admin-web".equalsIgnoreCase(clientCode)) {
            roleCodes = jdbcTemplate.query("""
                            SELECT DISTINCT r.code AS role_code
                            FROM role r
                            JOIN (
                                SELECT role_id
                                FROM user_global_role
                                WHERE user_id = ?
                                UNION
                                SELECT role_id
                                FROM user_domain_role
                                WHERE user_id = ?
                            ) ur ON ur.role_id = r.id
                            """,
                    (rs, rowNum) -> rs.getString("role_code"),
                    userId,
                    userId);
        } else {
            roleCodes = jdbcTemplate.query("""
                            SELECT DISTINCT r.code AS role_code
                            FROM role r
                            JOIN (
                                SELECT role_id
                                FROM user_global_role
                                WHERE user_id = ?
                                UNION
                                SELECT role_id
                                FROM user_domain_role
                                WHERE user_id = ?
                            ) ur ON ur.role_id = r.id
                            JOIN iam_role_resource irr ON irr.role_id = r.id
                            JOIN iam_resource ir ON ir.id = irr.resource_id
                            WHERE ir.status = 1
                              AND (ir.client_scope = ? OR ir.client_scope = 'all')
                            """,
                    (rs, rowNum) -> rs.getString("role_code"),
                    userId,
                    userId,
                    clientCode.trim().toLowerCase());
        }
        roleCodes.sort(Comparator.comparingInt(IamService::rolePriority));
        return List.copyOf(roleCodes);
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
            actions = snapshotData.actions().stream()
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

    public List<IamResource> listResources(String resourceType, String clientScope) {
        StringBuilder sql = new StringBuilder("""
                SELECT
                    id,
                    resource_type,
                    resource_code,
                    resource_name,
                    client_scope,
                    http_method,
                    path_pattern,
                    parent_id,
                    order_no,
                    icon,
                    component,
                    hidden,
                    status
                FROM iam_resource
                WHERE 1 = 1
                """);
        List<Object> args = new ArrayList<>();
        if (StringUtils.hasText(resourceType)) {
            String normalizedType = resourceType.trim().toLowerCase();
            if ("api".equals(normalizedType)) {
                normalizedType = "action";
            }
            sql.append(" AND resource_type = ?");
            args.add(normalizedType);
        }
        if (StringUtils.hasText(clientScope)) {
            sql.append(" AND client_scope = ?");
            args.add(clientScope.trim().toLowerCase());
        }
        sql.append(" ORDER BY resource_type, order_no, id");
        return jdbcTemplate.query(sql.toString(), (rs, rowNum) -> mapResource(rs), args.toArray());
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
        jdbcTemplate.update("""
                        INSERT INTO iam_resource (
                            resource_type, resource_code, resource_name, client_scope, http_method, path_pattern,
                            parent_id, order_no, icon, component, hidden, status
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                validated.resourceType(),
                validated.resourceCode(),
                validated.resourceName(),
                validated.clientScope(),
                validated.httpMethod(),
                validated.pathPattern(),
                validated.parentId(),
                validated.orderNo(),
                validated.icon(),
                validated.component(),
                validated.hidden() ? 1 : 0,
                validated.status());
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
        jdbcTemplate.update("""
                        UPDATE iam_resource
                        SET
                            resource_type = ?,
                            resource_code = ?,
                            resource_name = ?,
                            client_scope = ?,
                            http_method = ?,
                            path_pattern = ?,
                            parent_id = ?,
                            order_no = ?,
                            icon = ?,
                            component = ?,
                            hidden = ?,
                            status = ?
                        WHERE id = ?
                        """,
                validated.resourceType(),
                validated.resourceCode(),
                validated.resourceName(),
                validated.clientScope(),
                validated.httpMethod(),
                validated.pathPattern(),
                validated.parentId(),
                validated.orderNo(),
                validated.icon(),
                validated.component(),
                validated.hidden() ? 1 : 0,
                validated.status(),
                resourceId);
        evictAuthorizationCache();
        return findResourceById(resourceId)
                .orElseThrow(() -> new IllegalArgumentException("resource not found"));
    }

    public List<IamResource> listRoleResources(int roleId) {
        assertRoleExists(roleId);
        return jdbcTemplate.query("""
                        SELECT
                            ir.id,
                            ir.resource_type,
                            ir.resource_code,
                            ir.resource_name,
                            ir.client_scope,
                            ir.http_method,
                            ir.path_pattern,
                            ir.parent_id,
                            ir.order_no,
                            ir.icon,
                            ir.component,
                            ir.hidden,
                            ir.status
                        FROM iam_role_resource irr
                        JOIN iam_resource ir ON ir.id = irr.resource_id
                        WHERE irr.role_id = ?
                        ORDER BY ir.resource_type, ir.order_no, ir.id
                        """,
                (rs, rowNum) -> mapResource(rs),
                roleId);
    }

    @Transactional
    public List<IamResource> replaceRoleResources(int roleId, List<Long> resourceIds) {
        assertRoleExists(roleId);
        Set<Long> deduplicatedIds = resourceIds == null ? Set.of() : new LinkedHashSet<>(resourceIds);
        ensureResourceIdsExist(deduplicatedIds);
        jdbcTemplate.update("DELETE FROM iam_role_resource WHERE role_id = ?", roleId);
        for (Long resourceId : deduplicatedIds) {
            jdbcTemplate.update("""
                            INSERT INTO iam_role_resource (role_id, resource_id)
                            VALUES (?, ?)
                            """,
                    roleId,
                    resourceId);
        }
        evictAuthorizationCache();
        return listRoleResources(roleId);
    }

    public List<MenuTreeNode> listMenuTree(String clientScope) {
        List<IamResource> resources = jdbcTemplate.query("""
                        SELECT
                            id,
                            resource_type,
                            resource_code,
                            resource_name,
                            client_scope,
                            http_method,
                            path_pattern,
                            parent_id,
                            order_no,
                            icon,
                            component,
                            hidden,
                            status
                        FROM iam_resource
                        WHERE resource_type = 'menu'
                          AND (? IS NULL OR client_scope = ? OR client_scope = 'all')
                        ORDER BY order_no, id
                        """,
                (rs, rowNum) -> mapResource(rs),
                clientScope,
                clientScope);
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
        Integer childCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM iam_resource WHERE parent_id = ?",
                Integer.class,
                menuId);
        if (childCount != null && childCount > 0) {
            throw new IllegalArgumentException("menu has children, delete child menus first");
        }
        Integer bindingCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM iam_role_resource WHERE resource_id = ?",
                Integer.class,
                menuId);
        if (bindingCount != null && bindingCount > 0) {
            throw new IllegalArgumentException("menu is bound to roles, unbind first");
        }
        jdbcTemplate.update("DELETE FROM iam_resource WHERE id = ?", menuId);
        evictAuthorizationCache();
    }

    public List<RoleView> listRoles() {
        return jdbcTemplate.query("""
                        SELECT id, code, name, scope, is_system
                        FROM role
                        ORDER BY is_system DESC, id
                        """,
                (rs, rowNum) -> new RoleView(
                        rs.getInt("id"),
                        rs.getString("code"),
                        rs.getString("name"),
                        rs.getString("scope"),
                        rs.getInt("is_system") == 1));
    }

    public RoleView createRole(CreateRoleCommand command) {
        String normalizedCode = normalize(command.code(), "code");
        String normalizedName = normalize(command.name(), "name");
        String normalizedScope = normalize(command.scope(), "scope").toLowerCase();
        if (!List.of("global", "domain").contains(normalizedScope)) {
            throw new IllegalArgumentException("unsupported role scope");
        }
        try {
            jdbcTemplate.update("""
                            INSERT INTO role (code, name, scope, is_system)
                            VALUES (?, ?, ?, 0)
                            """,
                    normalizedCode,
                    normalizedName,
                    normalizedScope);
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
            jdbcTemplate.update("""
                            UPDATE role
                            SET code = ?, name = ?, scope = ?
                            WHERE id = ?
                            """,
                    code,
                    name,
                    scope,
                    roleId);
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
        Integer userGlobalCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user_global_role WHERE role_id = ?",
                Integer.class,
                roleId);
        Integer userDomainCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user_domain_role WHERE role_id = ?",
                Integer.class,
                roleId);
        if ((userGlobalCount != null && userGlobalCount > 0) || (userDomainCount != null && userDomainCount > 0)) {
            throw new IllegalArgumentException("role is bound to users, unbind first");
        }
        jdbcTemplate.update("DELETE FROM iam_role_permission WHERE role_id = ?", roleId);
        jdbcTemplate.update("DELETE FROM iam_role_binding WHERE role_id = ?", roleId);
        jdbcTemplate.update("DELETE FROM iam_role_resource WHERE role_id = ?", roleId);
        jdbcTemplate.update("DELETE FROM role WHERE id = ?", roleId);
        evictAuthorizationCache();
    }

    public RolePermissions loadRolePermissions(int roleId) {
        assertRoleExists(roleId);
        List<Long> menuResourceIds = jdbcTemplate.queryForList("""
                        SELECT ir.id
                        FROM iam_role_resource irr
                        JOIN iam_resource ir ON ir.id = irr.resource_id
                        WHERE irr.role_id = ?
                          AND ir.resource_type = 'menu'
                        ORDER BY ir.order_no, ir.id
                        """,
                Long.class,
                roleId);
        List<Long> actionResourceIds = jdbcTemplate.queryForList("""
                        SELECT ir.id
                        FROM iam_role_resource irr
                        JOIN iam_resource ir ON ir.id = irr.resource_id
                        WHERE irr.role_id = ?
                          AND ir.resource_type = 'action'
                        ORDER BY ir.resource_code
                        """,
                Long.class,
                roleId);
        return new RolePermissions(roleId, menuResourceIds, actionResourceIds);
    }

    @Transactional
    public RolePermissions replaceRolePermissions(int roleId, List<Long> menuResourceIds, List<Long> actionResourceIds) {
        assertRoleExists(roleId);
        Set<Long> menus = menuResourceIds == null ? Set.of() : new LinkedHashSet<>(menuResourceIds);
        Set<Long> actions = actionResourceIds == null ? Set.of() : new LinkedHashSet<>(actionResourceIds);
        ensureResourceIdsByType(menus, "menu");
        ensureResourceIdsByType(actions, "action");
        jdbcTemplate.update("DELETE FROM iam_role_resource WHERE role_id = ?", roleId);
        for (Long resourceId : menus) {
            jdbcTemplate.update("INSERT INTO iam_role_resource (role_id, resource_id) VALUES (?, ?)", roleId, resourceId);
        }
        for (Long resourceId : actions) {
            jdbcTemplate.update("INSERT INTO iam_role_resource (role_id, resource_id) VALUES (?, ?)", roleId, resourceId);
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
        String sql = """
                SELECT
                    id,
                    username,
                    mobile,
                    email,
                    remark,
                    account_type,
                    status,
                    employment_status,
                    offboarded_at,
                    offboarded_by,
                    offboard_reason
                FROM user_account
                WHERE employment_status %s
                ORDER BY id
                """.formatted(offboardedOnly ? "= 'offboarded'" : "<> 'offboarded'");
        List<UserAccount> users = jdbcTemplate.query(sql, (rs, rowNum) -> new UserAccount(
                rs.getLong("id"),
                rs.getString("username"),
                rs.getString("mobile"),
                rs.getString("email"),
                rs.getString("remark"),
                rs.getString("account_type"),
                rs.getInt("status"),
                rs.getString("employment_status"),
                rs.getTimestamp("offboarded_at") == null ? null : rs.getTimestamp("offboarded_at").toInstant().toString(),
                rs.getObject("offboarded_by", Long.class),
                rs.getString("offboard_reason"),
                listUserRoleCodes(rs.getLong("id")),
                listUserDomainIds(rs.getLong("id")),
                listUserOrganizationIdsOrEmpty(rs.getLong("id"))));
        return users.stream()
                .filter(user -> targetOrgIds.isEmpty() || user.organizationIds().stream().anyMatch(targetOrgIds::contains))
                .toList();
    }

    @Transactional
    public UserAccount createUser(CreateUserCommand command) {
        String username = normalize(command.username(), "username");
        String mobile = normalize(command.mobile(), "mobile");
        String accountType = normalize(command.accountType(), "accountType").toLowerCase();
        if (!List.of("admin", "customer").contains(accountType)) {
            throw new IllegalArgumentException("unsupported account type");
        }
        String password = normalize(command.password(), "password");
        String email = StringUtils.hasText(command.email()) ? command.email().trim() : null;
        String remark = normalizeOptional(command.remark());
        List<String> roleCodes = command.roleCodes() == null ? List.of() : command.roleCodes().stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .toList();
        List<Long> businessDomainIds = command.businessDomainIds() == null ? List.of() : command.businessDomainIds().stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        List<Long> organizationIds = command.organizationIds() == null ? List.of() : command.organizationIds().stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (roleCodes.isEmpty()) {
            throw new IllegalArgumentException("roleCodes is required");
        }
        try {
            jdbcTemplate.update("""
                            INSERT INTO user_account (
                                username, mobile, email, remark, password_hash, status, account_type, employment_status
                            )
                            VALUES (?, ?, ?, ?, ?, 1, ?, 'active')
                            """,
                    username,
                    mobile,
                    email,
                    remark,
                    passwordEncoder.encode(password),
                    accountType);
        } catch (DuplicateKeyException ex) {
            throw new IllegalArgumentException("username/mobile/email already exists");
        }
        Long userId = jdbcTemplate.queryForObject("SELECT id FROM user_account WHERE username = ? LIMIT 1", Long.class, username);
        if (userId == null) {
            throw new IllegalStateException("user create failed");
        }
        replaceUserRoleBindings(userId, roleCodes, businessDomainIds);
        organizationService.replaceUserOrganizations(userId, organizationIds);
        return loadUser(userId).orElseThrow(() -> new IllegalStateException("user load failed"));
    }

    @Transactional
    public UserAccount updateUser(long userId, UpdateUserCommand command) {
        UserAccount existing = loadUser(userId).orElseThrow(() -> new IllegalArgumentException("user not found"));
        List<String> assignments = new ArrayList<>();
        List<Object> args = new ArrayList<>();
        if (command.username() != null) {
            assignments.add("username = ?");
            args.add(normalize(command.username(), "username"));
        }
        if (command.mobile() != null) {
            assignments.add("mobile = ?");
            args.add(normalize(command.mobile(), "mobile"));
        }
        if (command.email() != null) {
            assignments.add("email = ?");
            args.add(StringUtils.hasText(command.email()) ? command.email().trim() : null);
        }
        if (command.remark() != null) {
            assignments.add("remark = ?");
            args.add(normalizeOptional(command.remark()));
        }
        if (command.password() != null) {
            assignments.add("password_hash = ?");
            args.add(passwordEncoder.encode(normalize(command.password(), "password")));
        }
        if (command.accountType() != null) {
            String accountType = normalize(command.accountType(), "accountType").toLowerCase();
            if (!List.of("admin", "customer").contains(accountType)) {
                throw new IllegalArgumentException("unsupported account type");
            }
            assignments.add("account_type = ?");
            args.add(accountType);
        }
        if (command.status() != null) {
            assignments.add("status = ?");
            args.add(command.status());
        }
        if (!assignments.isEmpty()) {
            String sql = "UPDATE user_account SET " + String.join(", ", assignments) + " WHERE id = ?";
            args.add(userId);
            try {
                jdbcTemplate.update(sql, args.toArray());
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
        jdbcTemplate.update("""
                        UPDATE user_account
                        SET employment_status = 'offboarded',
                            status = 0,
                            offboarded_at = ?,
                            offboarded_by = ?,
                            offboard_reason = ?
                        WHERE id = ?
                        """,
                now,
                operatorUserId,
                StringUtils.hasText(reason) ? reason.trim() : null,
                userId);
        jdbcTemplate.update("""
                        UPDATE auth_login_session
                        SET session_status = 'revoked',
                            revoked_at = ?,
                            revoked_reason = 'user_offboarded'
                        WHERE user_id = ?
                          AND session_status = 'active'
                        """,
                now,
                userId);
        return loadUser(userId).orElseThrow(() -> new IllegalArgumentException("user not found"));
    }

    @Transactional
    public UserAccount restoreUser(long userId) {
        UserAccount existing = loadUser(userId).orElseThrow(() -> new IllegalArgumentException("user not found"));
        if ("active".equals(existing.employmentStatus())) {
            return existing;
        }
        jdbcTemplate.update("""
                        UPDATE user_account
                        SET employment_status = 'active',
                            status = 1,
                            offboarded_at = NULL,
                            offboarded_by = NULL,
                            offboard_reason = NULL
                        WHERE id = ?
                        """,
                userId);
        return loadUser(userId).orElseThrow(() -> new IllegalArgumentException("user not found"));
    }

    @Transactional
    public void deleteUserPermanently(long userId) {
        UserAccount existing = loadUser(userId).orElseThrow(() -> new IllegalArgumentException("user not found"));
        if (!"offboarded".equals(existing.employmentStatus())) {
            throw new IllegalArgumentException("user must be offboarded before permanent delete");
        }
        Integer ticketRefCount = jdbcTemplate.queryForObject("""
                        SELECT COUNT(*)
                        FROM ticket
                        WHERE customer_id = ? OR assigned_to = ?
                        """,
                Integer.class,
                userId,
                userId);
        Integer consultationRefCount = jdbcTemplate.queryForObject("""
                        SELECT COUNT(*)
                        FROM consultation_session
                        WHERE customer_id = ? OR assigned_to = ?
                        """,
                Integer.class,
                userId,
                userId);
        if ((ticketRefCount != null && ticketRefCount > 0) || (consultationRefCount != null && consultationRefCount > 0)) {
            throw new IllegalArgumentException("user has ticket/consultation references and cannot be deleted");
        }
        jdbcTemplate.update("UPDATE user_account SET offboarded_by = NULL WHERE offboarded_by = ?", userId);
        jdbcTemplate.update("DELETE FROM user_domain_role WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM user_global_role WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM user_organization WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM auth_login_log WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM auth_login_session WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM user_account WHERE id = ?", userId);
    }

    public void evictAuthorizationCache() {
        apiGrantCache.clear();
        menuResourceCache.clear();
    }

    private List<ApiGrant> loadActionGrants(String roleCode, String clientCode) {
        String cacheKey = cacheKey(roleCode, clientCode);
        return getOrLoad(apiGrantCache, cacheKey, () -> jdbcTemplate.query("""
                        SELECT
                            UPPER(ir.http_method) AS http_method,
                            ir.path_pattern
                        FROM role r
                        JOIN iam_role_resource irr ON irr.role_id = r.id
                        JOIN iam_resource ir ON ir.id = irr.resource_id
                        WHERE r.code = ?
                          AND ir.resource_type IN ('action', 'api')
                          AND ir.status = 1
                          AND ir.http_method IS NOT NULL
                          AND ir.path_pattern IS NOT NULL
                          AND (ir.client_scope = ? OR ir.client_scope = 'all')
                        ORDER BY ir.resource_code
                        """,
                (rs, rowNum) -> new ApiGrant(
                        rs.getString("http_method"),
                        rs.getString("path_pattern")),
                roleCode,
                clientCode));
    }

    private List<IamResource> loadResourcesForRoles(List<String> roleCodes, String clientCode, List<String> resourceTypes) {
        if (roleCodes == null || roleCodes.isEmpty() || resourceTypes == null || resourceTypes.isEmpty()) {
            return List.of();
        }
        String rolePlaceholder = String.join(",", roleCodes.stream().map(code -> "?").toList());
        String typePlaceholder = String.join(",", resourceTypes.stream().map(type -> "?").toList());
        List<Object> args = new ArrayList<>();
        args.addAll(roleCodes);
        args.addAll(resourceTypes);
        args.add(clientCode);
        return jdbcTemplate.query("""
                        SELECT DISTINCT
                            ir.id,
                            ir.resource_type,
                            ir.resource_code,
                            ir.resource_name,
                            ir.client_scope,
                            ir.http_method,
                            ir.path_pattern,
                            ir.parent_id,
                            ir.order_no,
                            ir.icon,
                            ir.component,
                            ir.hidden,
                            ir.status
                        FROM role r
                        JOIN iam_role_resource irr ON irr.role_id = r.id
                        JOIN iam_resource ir ON ir.id = irr.resource_id
                        WHERE r.code IN (%s)
                          AND ir.resource_type IN (%s)
                          AND ir.status = 1
                          AND (ir.client_scope = ? OR ir.client_scope = 'all')
                        ORDER BY ir.resource_type, ir.order_no, ir.id
                        """.formatted(rolePlaceholder, typePlaceholder),
                (rs, rowNum) -> mapResource(rs),
                args.toArray());
    }

    private List<DomainSummary> loadDomainSummaries(long userId, List<String> roleCodes) {
        if (roleCodes == null || roleCodes.isEmpty()) {
            return List.of();
        }
        if (roleCodes.contains("super_admin")) {
            return jdbcTemplate.query("""
                            SELECT id, code, name
                            FROM business_domain
                            ORDER BY id
                            """,
                    (rs, rowNum) -> new DomainSummary(
                            rs.getLong("id"),
                            rs.getString("code"),
                            rs.getString("name")));
        }
        String rolePlaceholder = String.join(",", roleCodes.stream().map(code -> "?").toList());
        List<Object> args = new ArrayList<>();
        args.add(userId);
        args.addAll(roleCodes);
        return jdbcTemplate.query("""
                        SELECT DISTINCT d.id, d.code, d.name
                        FROM user_domain_role udr
                        JOIN role r ON r.id = udr.role_id
                        JOIN business_domain d ON d.id = udr.business_domain_id
                        WHERE udr.user_id = ?
                          AND r.code IN (%s)
                        ORDER BY d.id
                        """.formatted(rolePlaceholder),
                (rs, rowNum) -> new DomainSummary(
                        rs.getLong("id"),
                        rs.getString("code"),
                        rs.getString("name")),
                args.toArray());
    }

    private UserSummary loadCurrentUserSummary(long userId) {
        return jdbcTemplate.queryForObject("""
                        SELECT id, username, mobile, email
                        FROM user_account
                        WHERE id = ?
                        LIMIT 1
                        """,
                (rs, rowNum) -> new UserSummary(
                        rs.getLong("id"),
                        rs.getString("username"),
                        rs.getString("mobile"),
                        rs.getString("email")),
                userId);
    }

    private List<String> listUserRoleCodes(long userId) {
        List<String> roleCodes = jdbcTemplate.query("""
                        SELECT DISTINCT r.id AS role_order, r.code AS role_code
                        FROM role r
                        JOIN (
                            SELECT role_id FROM user_global_role WHERE user_id = ?
                            UNION
                            SELECT role_id FROM user_domain_role WHERE user_id = ?
                        ) rs ON rs.role_id = r.id
                        ORDER BY role_order
                        """,
                (rs, rowNum) -> rs.getString("role_code"),
                userId,
                userId);
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
        Integer otherHolderCount = jdbcTemplate.queryForObject("""
                        SELECT COUNT(DISTINCT ugr.user_id)
                        FROM user_global_role ugr
                        JOIN role r ON r.id = ugr.role_id
                        JOIN user_account ua ON ua.id = ugr.user_id
                        WHERE r.code = ?
                          AND ugr.user_id <> ?
                          AND ua.status = 1
                          AND ua.employment_status <> 'offboarded'
                        """,
                Integer.class,
                roleCode,
                userId);
        if (otherHolderCount == null || otherHolderCount == 0) {
            throw new IllegalArgumentException("cannot remove the last " + roleCode);
        }
    }

    private List<Long> listUserDomainIds(long userId) {
        return jdbcTemplate.queryForList("""
                        SELECT DISTINCT business_domain_id
                        FROM user_domain_role
                        WHERE user_id = ?
                        ORDER BY business_domain_id
                        """,
                Long.class,
                userId);
    }

    public Optional<UserAccount> loadUser(long userId) {
        try {
            UserAccount user = jdbcTemplate.queryForObject("""
                            SELECT
                                id,
                                username,
                                mobile,
                                email,
                                remark,
                                account_type,
                                status,
                                employment_status,
                                offboarded_at,
                                offboarded_by,
                                offboard_reason
                            FROM user_account
                            WHERE id = ?
                            LIMIT 1
                            """,
                            (rs, rowNum) -> new UserAccount(
                            rs.getLong("id"),
                            rs.getString("username"),
                            rs.getString("mobile"),
                            rs.getString("email"),
                            rs.getString("remark"),
                            rs.getString("account_type"),
                            rs.getInt("status"),
                            rs.getString("employment_status"),
                            rs.getTimestamp("offboarded_at") == null ? null : rs.getTimestamp("offboarded_at").toInstant().toString(),
                            rs.getObject("offboarded_by", Long.class),
                            rs.getString("offboard_reason"),
                            listUserRoleCodes(rs.getLong("id")),
                            listUserDomainIds(rs.getLong("id")),
                            listUserOrganizationIdsOrEmpty(rs.getLong("id"))),
                    userId);
            return Optional.of(user);
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
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
        jdbcTemplate.update("DELETE FROM user_global_role WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM user_domain_role WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM iam_role_binding WHERE user_id = ?", userId);
        for (RoleDefinition role : roleMap.values()) {
            if ("global".equals(role.scope())) {
                jdbcTemplate.update("INSERT INTO user_global_role (user_id, role_id) VALUES (?, ?)", userId, role.id());
                jdbcTemplate.update("""
                                INSERT INTO iam_role_binding (user_id, role_id, binding_scope, business_domain_id, status)
                                VALUES (?, ?, 'global', NULL, 1)
                                """,
                        userId,
                        role.id());
                continue;
            }
            for (Long domainId : domainIds) {
                jdbcTemplate.update(
                        "INSERT INTO user_domain_role (user_id, role_id, business_domain_id) VALUES (?, ?, ?)",
                        userId,
                        role.id(),
                        domainId);
                jdbcTemplate.update("""
                                INSERT INTO iam_role_binding (user_id, role_id, binding_scope, business_domain_id, status)
                                VALUES (?, ?, 'domain', ?, 1)
                                """,
                        userId,
                        role.id(),
                        domainId);
            }
        }
        evictAuthorizationCache();
    }

    private Map<String, RoleDefinition> loadRoleDefinitions(List<String> roleCodes) {
        List<String> normalizedCodes = roleCodes.stream().map(code -> normalize(code, "roleCode")).distinct().toList();
        String placeholders = String.join(",", normalizedCodes.stream().map(code -> "?").toList());
        List<RoleDefinition> definitions = jdbcTemplate.query("""
                        SELECT id, code, scope
                        FROM role
                        WHERE code IN (%s)
                        """.formatted(placeholders),
                (rs, rowNum) -> new RoleDefinition(
                        rs.getInt("id"),
                        rs.getString("code"),
                        rs.getString("scope")),
                normalizedCodes.toArray());
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
        String placeholders = String.join(",", domainIds.stream().map(id -> "?").toList());
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM business_domain WHERE id IN (%s)".formatted(placeholders),
                Long.class,
                domainIds.toArray());
        if (count == null || count != domainIds.size()) {
            throw new IllegalArgumentException("business domain not found");
        }
    }

    private void ensureResourceIdsExist(Set<Long> resourceIds) {
        if (resourceIds.isEmpty()) {
            return;
        }
        String placeholders = String.join(",", resourceIds.stream().map(id -> "?").toList());
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM iam_resource WHERE id IN (%s)".formatted(placeholders),
                Long.class,
                resourceIds.toArray());
        if (count == null || count != resourceIds.size()) {
            throw new IllegalArgumentException("resource not found");
        }
    }

    private void ensureResourceIdsByType(Set<Long> resourceIds, String resourceType) {
        if (resourceIds.isEmpty()) {
            return;
        }
        String placeholders = String.join(",", resourceIds.stream().map(id -> "?").toList());
        List<Object> args = new ArrayList<>();
        args.addAll(resourceIds);
        args.add(resourceType);
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM iam_resource WHERE id IN (%s) AND resource_type = ?".formatted(placeholders),
                Long.class,
                args.toArray());
        if (count == null || count != resourceIds.size()) {
            throw new IllegalArgumentException(resourceType + " resource not found");
        }
    }

    private List<String> findPermissionCodesByRequest(String method, String requestPath) {
        String normalizedMethod = method.trim().toUpperCase();
        try {
            return jdbcTemplate.query("""
                            SELECT code, path_pattern
                            FROM iam_permission
                            WHERE status = 1
                              AND http_method = ?
                              AND path_pattern IS NOT NULL
                            """,
                    (rs, rowNum) -> new RoutePermission(
                            rs.getString("code"),
                            rs.getString("path_pattern")),
                    normalizedMethod).stream()
                    .filter(permission -> pathMatcher.match(permission.pathPattern(), requestPath))
                    .map(RoutePermission::code)
                    .toList();
        } catch (DataAccessException ex) {
            return List.of();
        }
    }

    private List<EffectivePermissionGrant> loadEffectivePermissionGrants(long userId, List<String> permissionCodes) {
        String placeholders = String.join(",", permissionCodes.stream().map(code -> "?").toList());
        List<Object> args = new ArrayList<>();
        args.add(userId);
        args.addAll(permissionCodes);
        return jdbcTemplate.query("""
                        SELECT
                            r.scope AS role_level,
                            b.binding_scope,
                            b.business_domain_id,
                            p.permission_scope
                        FROM iam_role_binding b
                        JOIN role r ON r.id = b.role_id
                        JOIN iam_role_permission rp ON rp.role_id = r.id
                        JOIN iam_permission p ON p.id = rp.permission_id
                        WHERE b.user_id = ?
                          AND b.status = 1
                          AND p.status = 1
                          AND p.code IN (%s)
                          AND (b.effective_from IS NULL OR b.effective_from <= CURRENT_TIMESTAMP(3))
                          AND (b.effective_to IS NULL OR b.effective_to > CURRENT_TIMESTAMP(3))
                        """.formatted(placeholders),
                (rs, rowNum) -> new EffectivePermissionGrant(
                        rs.getString("role_level"),
                        rs.getString("binding_scope"),
                        rs.getObject("business_domain_id", Long.class),
                        rs.getString("permission_scope")),
                args.toArray());
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
        try {
            IamResource resource = jdbcTemplate.queryForObject("""
                            SELECT
                                id,
                                resource_type,
                                resource_code,
                                resource_name,
                                client_scope,
                                http_method,
                                path_pattern,
                                parent_id,
                                order_no,
                                icon,
                                component,
                                hidden,
                                status
                            FROM iam_resource
                            WHERE id = ?
                            LIMIT 1
                            """,
                    (rs, rowNum) -> mapResource(rs),
                    resourceId);
            return Optional.of(resource);
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    private Optional<IamResource> findResourceByCode(String resourceCode) {
        try {
            IamResource resource = jdbcTemplate.queryForObject("""
                            SELECT
                                id,
                                resource_type,
                                resource_code,
                                resource_name,
                                client_scope,
                                http_method,
                                path_pattern,
                                parent_id,
                                order_no,
                                icon,
                                component,
                                hidden,
                                status
                            FROM iam_resource
                            WHERE resource_code = ?
                            LIMIT 1
                            """,
                    (rs, rowNum) -> mapResource(rs),
                    resourceCode);
            return Optional.of(resource);
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    private Optional<RoleView> findRoleById(int roleId) {
        try {
            RoleView role = jdbcTemplate.queryForObject("""
                            SELECT id, code, name, scope, is_system
                            FROM role
                            WHERE id = ?
                            LIMIT 1
                            """,
                    (rs, rowNum) -> new RoleView(
                            rs.getInt("id"),
                            rs.getString("code"),
                            rs.getString("name"),
                            rs.getString("scope"),
                            rs.getInt("is_system") == 1),
                    roleId);
            return Optional.of(role);
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    private Optional<RoleView> findRoleByCode(String roleCode) {
        try {
            RoleView role = jdbcTemplate.queryForObject("""
                            SELECT id, code, name, scope, is_system
                            FROM role
                            WHERE code = ?
                            LIMIT 1
                            """,
                    (rs, rowNum) -> new RoleView(
                            rs.getInt("id"),
                            rs.getString("code"),
                            rs.getString("name"),
                            rs.getString("scope"),
                            rs.getInt("is_system") == 1),
                    roleCode);
            return Optional.of(role);
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    private void assertRoleExists(int roleId) {
        Integer exists = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM role WHERE id = ?",
                Integer.class,
                roleId);
        if (exists == null || exists == 0) {
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

    private IamResource mapResource(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new IamResource(
                rs.getLong("id"),
                rs.getString("resource_type"),
                rs.getString("resource_code"),
                rs.getString("resource_name"),
                rs.getString("client_scope"),
                null,
                rs.getString("http_method"),
                rs.getString("path_pattern"),
                rs.getObject("parent_id", Long.class),
                rs.getInt("order_no"),
                rs.getString("icon"),
                rs.getString("component"),
                rs.getInt("hidden") == 1,
                rs.getInt("status"),
                null);
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

    private record ApiGrant(String httpMethod, String pathPattern) {
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

    private record RoutePermission(
            String code,
            String pathPattern) {
    }

    private record EffectivePermissionGrant(
            String roleLevel,
            String bindingScope,
            Long businessDomainId,
            String permissionScope) {
    }
}
