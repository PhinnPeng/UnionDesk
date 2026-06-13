package com.uniondesk.iam.admin;

import com.uniondesk.iam.admin.AdminPermissionCatalog.PermissionDefinition;
import com.uniondesk.iam.entity.AdminMenuPo;
import com.uniondesk.iam.entity.RolePermissionRowPo;
import com.uniondesk.iam.entity.RoutePathRowPo;
import com.uniondesk.iam.repository.AdminMenuRepository;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.dao.DataAccessException;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AdminMenuService {

    private static final String NODE_TYPE_CATALOG = "catalog";
    private static final String NODE_TYPE_MENU = "menu";
    private static final String NODE_TYPE_BUTTON = "button";
    private static final List<String> SUPPORTED_NODE_TYPES = List.of(NODE_TYPE_CATALOG, NODE_TYPE_MENU, NODE_TYPE_BUTTON);

    private final AdminMenuRepository adminMenuRepository;
    private final Clock clock;

    public AdminMenuService(AdminMenuRepository adminMenuRepository, Clock clock) {
        this.adminMenuRepository = adminMenuRepository;
        this.clock = clock;
    }

    public List<PermissionDefinition> listPermissionCodes(@Nullable String scope) {
        if (!StringUtils.hasText(scope)) {
            return AdminPermissionCatalog.list();
        }
        String normalizedScope = scope.trim();
        return AdminPermissionCatalog.list().stream()
                .filter(permission -> normalizedScope.equals(permission.permissionScope()))
                .toList();
    }

    public List<AdminMenuNode> listMenuTree() {
        return listMenuTree(null);
    }

    public List<AdminMenuNode> listMenuTree(@Nullable String scope) {
        String normalizedScope = normalizeScopeFilter(scope);
        Map<Long, String> requiredPermissionByMenuId = loadRequiredPermissionByMenuId(normalizedScope);
        List<AdminMenuNode> nodes = adminMenuRepository.findAll(normalizedScope).stream()
                .map(po -> mapPo(po, requiredPermissionByMenuId))
                .toList();
        return buildTree(nodes);
    }

    public PermissionSnapshotData loadPermissionSnapshot(List<String> roleCodes) {
        if (roleCodes == null || roleCodes.isEmpty()) {
            return new PermissionSnapshotData(List.of(), List.of());
        }
        List<String> normalizedRoleCodes = roleCodes.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .toList();
        if (normalizedRoleCodes.isEmpty()) {
            return new PermissionSnapshotData(List.of(), List.of());
        }
        List<AdminMenuNode> authorizedNodes = adminMenuRepository.findAuthorizedByRoleCodes(normalizedRoleCodes).stream()
                .map(po -> mapPo(po, Map.of()))
                .toList();
        List<AdminMenuNode> authorizedMenus = authorizedNodes.stream()
                .filter(node -> NODE_TYPE_MENU.equals(node.nodeType()))
                .toList();
        // 自动补齐授权 menu 节点的所有祖先 catalog 节点，确保前端能构建完整的目录-菜单父子结构
        Map<Long, AdminMenuNode> menuNodesById = new LinkedHashMap<>();
        for (AdminMenuNode menu : authorizedMenus) {
            menuNodesById.put(menu.id(), menu);
            Long parentId = menu.parentId();
            while (parentId != null && !menuNodesById.containsKey(parentId)) {
                AdminMenuNode parent = findNodeById(parentId).orElse(null);
                if (parent == null || !NODE_TYPE_CATALOG.equals(parent.nodeType()) || parent.status() != 1) {
                    break;
                }
                menuNodesById.put(parent.id(), parent);
                parentId = parent.parentId();
            }
        }
        List<AdminMenuNode> menuNodes = menuNodesById.values().stream()
                .sorted(Comparator.comparingInt(AdminMenuNode::orderNo).thenComparingLong(AdminMenuNode::id))
                .toList();
        List<GrantedPermission> actionNodes = authorizedNodes.stream()
                .filter(node -> NODE_TYPE_BUTTON.equals(node.nodeType()))
                .map(node -> AdminPermissionCatalog.findByCode(node.permissionCode())
                        .map(definition -> new GrantedPermission(
                                node.id(),
                                node.name(),
                                definition.code(),
                                definition.httpMethod(),
                                definition.pathPattern(),
                                node.parentId(),
                                node.orderNo(),
                                node.required()))
                        .orElse(null))
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingInt(GrantedPermission::orderNo).thenComparingLong(GrantedPermission::id))
                .toList();
        List<GrantedPermission> mergedActions = mergeRolePermissionActions(normalizedRoleCodes, actionNodes);
        return new PermissionSnapshotData(menuNodes, mergedActions);
    }

    /**
     * 将 iam_role_permission 中的权限码并入 snapshot actions，避免仅分配 API 权限、未勾选菜单按钮时前端 AuthGuarded 无码。
     */
    private List<GrantedPermission> mergeRolePermissionActions(
            List<String> roleCodes,
            List<GrantedPermission> menuButtonActions) {
        Set<String> existingCodes = new LinkedHashSet<>();
        for (GrantedPermission action : menuButtonActions) {
            existingCodes.add(action.permissionCode());
        }
        List<GrantedPermission> merged = new ArrayList<>(menuButtonActions);
        long syntheticId = -1L;
        for (RolePermissionRow row : loadRolePermissionRows(roleCodes)) {
            if (!StringUtils.hasText(row.code()) || existingCodes.contains(row.code())) {
                continue;
            }
            Optional<PermissionDefinition> definition = AdminPermissionCatalog.findByCode(row.code());
            if (definition.isEmpty()) {
                continue;
            }
            PermissionDefinition def = definition.get();
            merged.add(new GrantedPermission(
                    syntheticId--,
                    StringUtils.hasText(row.name()) ? row.name() : row.code(),
                    def.code(),
                    def.httpMethod(),
                    def.pathPattern(),
                    null,
                    0,
                    false));
            existingCodes.add(row.code());
        }
        return merged;
    }

    private List<RolePermissionRow> loadRolePermissionRows(List<String> roleCodes) {
        if (roleCodes.isEmpty()) {
            return List.of();
        }
        return adminMenuRepository.findRolePermissionRows(roleCodes).stream()
                .map(this::toRolePermissionRow)
                .toList();
    }

    @Transactional
    public AdminMenuNode createMenu(CreateAdminMenuCommand command) {
        ValidatedNode validated = validateNode(command.nodeType(), command.name(), command.routePath(), command.componentKey(),
                command.permissionCode(), command.scope(), command.parentId(), command.orderNo(), command.icon(), command.hidden(), command.status(), null);
        long nodeId = insertNode(validated);
        String generatedCode = generateNodeCode(nodeId);
        adminMenuRepository.updateCode(nodeId, generatedCode);
        if (NODE_TYPE_MENU.equals(validated.nodeType()) && StringUtils.hasText(validated.permissionCode())) {
            insertRequiredButton(nodeId, validated.permissionCode(), validated.scope());
        }
        return findNodeById(nodeId).orElseThrow(() -> new IllegalStateException("menu create failed"));
    }

    @Transactional
    public AdminMenuNode updateMenu(long menuId, UpdateAdminMenuCommand command) {
        AdminMenuNode existing = findNodeById(menuId).orElseThrow(() -> new IllegalArgumentException("菜单不存在"));
        String nextNodeType = command.nodeType() == null ? existing.nodeType() : command.nodeType();
        if (existing.required() && !existing.nodeType().equals(nextNodeType)) {
            throw new IllegalArgumentException("系统必需菜单节点不允许修改类型");
        }
        if (NODE_TYPE_BUTTON.equals(nextNodeType) && !NODE_TYPE_BUTTON.equals(existing.nodeType())) {
            throw new IllegalArgumentException("不允许将菜单类型修改为按钮");
        }
        String existingMenuPermissionCode = NODE_TYPE_MENU.equals(existing.nodeType())
                ? findRequiredButton(menuId).map(AdminMenuNode::permissionCode).orElse(null)
                : existing.permissionCode();
        boolean isCatalog = NODE_TYPE_CATALOG.equals(nextNodeType);
        ValidatedNode validated = validateNode(
                nextNodeType,
                command.name() == null ? existing.name() : command.name(),
                isCatalog ? null : (command.routePath() == null ? existing.routePath() : command.routePath()),
                isCatalog ? null : (command.componentKey() == null ? existing.componentKey() : command.componentKey()),
                isCatalog ? null : (command.permissionCode() == null ? existingMenuPermissionCode : command.permissionCode()),
                command.scope() == null ? existing.scope() : command.scope(),
                command.parentId() == null ? existing.parentId() : command.parentId(),
                command.orderNo() == null ? existing.orderNo() : command.orderNo(),
                command.icon() == null ? existing.icon() : command.icon(),
                command.hidden() == null ? existing.hidden() : command.hidden(),
                command.status() == null ? existing.status() : command.status(),
                menuId);
        adminMenuRepository.update(toAdminMenuPo(menuId, validated));
        if (NODE_TYPE_MENU.equals(existing.nodeType())) {
            updateRequiredButton(menuId, validated.permissionCode(), validated.scope());
        }
        return findNodeById(menuId).orElseThrow(() -> new IllegalArgumentException("菜单不存在"));
    }

    @Transactional
    public void deleteMenu(long menuId) {
        AdminMenuNode existing = findNodeById(menuId).orElseThrow(() -> new IllegalArgumentException("菜单不存在"));
        if (adminMenuRepository.countByParentId(menuId) > 0) {
            throw new IllegalArgumentException("请先删除该菜单下的所有子节点");
        }
        adminMenuRepository.deleteRoleMenuRelations(menuId);
        adminMenuRepository.deleteById(menuId);
    }

    public RolePermissions loadRolePermissions(int roleId) {
        assertRoleExists(roleId);
        List<Long> menuIds = adminMenuRepository.findRoleMenuIdsByNodeTypes(
                roleId, List.of(NODE_TYPE_MENU, NODE_TYPE_CATALOG));
        List<Long> buttonIds = adminMenuRepository.findRoleMenuIds(roleId, NODE_TYPE_BUTTON);
        return new RolePermissions(roleId, menuIds, buttonIds);
    }

    @Transactional
    public RolePermissions replaceRolePermissions(int roleId, List<Long> menuIds, List<Long> buttonIds) {
        assertRoleExists(roleId);
        Set<Long> normalizedMenuIds = menuIds == null ? new LinkedHashSet<>() : new LinkedHashSet<>(menuIds);
        Set<Long> normalizedButtonIds = buttonIds == null ? new LinkedHashSet<>() : new LinkedHashSet<>(buttonIds);
        ensureNodeTypes(normalizedMenuIds, List.of(NODE_TYPE_MENU, NODE_TYPE_CATALOG));
        ensureNodeType(normalizedButtonIds, NODE_TYPE_BUTTON);
        RequiredMenuIds requiredMenuIds = loadRequiredMenuAndButtonIds(roleId);
        normalizedMenuIds.addAll(requiredMenuIds.menuIds());
        normalizedButtonIds.addAll(requiredMenuIds.buttonIds());
        if (!normalizedButtonIds.isEmpty()) {
            normalizedMenuIds.addAll(adminMenuRepository.findParentIdsByMenuIds(new ArrayList<>(normalizedButtonIds)));
        }
        if (!normalizedMenuIds.isEmpty()) {
            normalizedButtonIds.addAll(adminMenuRepository.findRequiredButtonIdsByParentIds(new ArrayList<>(normalizedMenuIds)));
        }
        ensureRoleCanOwnButtonPermissions(roleId, normalizedButtonIds);
        adminMenuRepository.deleteRoleMenuRelationsByRoleId(roleId);
        List<Long> allMenuRelationIds = new ArrayList<>(normalizedMenuIds.size() + normalizedButtonIds.size());
        allMenuRelationIds.addAll(normalizedMenuIds);
        allMenuRelationIds.addAll(normalizedButtonIds);
        adminMenuRepository.batchInsertRoleMenuRelations(roleId, allMenuRelationIds);
        replaceRolePermissionRows(roleId, normalizedButtonIds);
        return loadRolePermissions(roleId);
    }

    private RequiredMenuIds loadRequiredMenuAndButtonIds(int roleId) {
        String roleScope = adminMenuRepository.findRoleScopeById(roleId);
        String normalizedRoleScope = StringUtils.hasText(roleScope) ? roleScope.trim().toLowerCase(Locale.ROOT) : "business";
        String menuScope = "global".equals(normalizedRoleScope) ? "platform" : "business";
        List<Long> menuIds = adminMenuRepository.findRequiredMenuIds(menuScope);
        List<Long> buttonIds = menuIds.isEmpty()
                ? List.of()
                : adminMenuRepository.findRequiredButtonIdsByMenuIds(menuIds);
        return new RequiredMenuIds(menuIds, buttonIds);
    }

    private Set<String> loadGrantedPermissionCodesByRole(String roleCode) {
        if (!StringUtils.hasText(roleCode)) {
            return Set.of();
        }
        return new LinkedHashSet<>(adminMenuRepository.findGrantedPermissionCodes(roleCode.trim()));
    }

    private long insertNode(ValidatedNode validated) {
        AdminMenuPo po = toAdminMenuPo(null, validated);
        po.setCode(generateTempCode());
        po.setRequired(0);
        adminMenuRepository.insert(po);
        if (po.getId() == null) {
            throw new IllegalStateException("menu create failed");
        }
        return po.getId();
    }

    private void insertRequiredButton(long parentMenuId, String permissionCode, String scope) {
        PermissionDefinition definition = requirePermissionDefinition(permissionCode);
        long buttonId = insertButton(parentMenuId, definition, true, scope);
        adminMenuRepository.updateCode(buttonId, generateNodeCode(buttonId));
    }

    private void updateRequiredButton(long menuId, String permissionCode, String scope) {
        if (!StringUtils.hasText(permissionCode)) {
            return;
        }
        PermissionDefinition definition = requirePermissionDefinition(permissionCode);
        Optional<AdminMenuNode> requiredButton = findRequiredButton(menuId);
        if (requiredButton.isEmpty()) {
            insertRequiredButton(menuId, permissionCode, scope);
            return;
        }
        ensurePermissionCodeAvailable(permissionCode, requiredButton.get().id());
        AdminMenuPo buttonPo = adminMenuRepository.findById(requiredButton.get().id()).orElseThrow();
        buttonPo.setScope(scope);
        buttonPo.setName(definition.name());
        buttonPo.setPermissionCode(definition.code());
        buttonPo.setStatus(1);
        buttonPo.setHidden(0);
        adminMenuRepository.update(buttonPo);
    }

    private long insertButton(long parentMenuId, PermissionDefinition definition, boolean required, String scope) {
        AdminMenuPo po = new AdminMenuPo();
        po.setCode(generateTempCode());
        po.setNodeType(NODE_TYPE_BUTTON);
        po.setScope(scope);
        po.setName(definition.name());
        po.setPermissionCode(definition.code());
        po.setParentId(parentMenuId);
        po.setOrderNo(0);
        po.setHidden(0);
        po.setStatus(1);
        po.setRequired(required ? 1 : 0);
        adminMenuRepository.insert(po);
        if (po.getId() == null) {
            throw new IllegalStateException("按钮节点创建失败");
        }
        return po.getId();
    }

    private ValidatedNode validateNode(
            String nodeType,
            String name,
            String routePath,
            String componentKey,
            String permissionCode,
            String scope,
            Long parentId,
            Integer orderNo,
            String icon,
            Boolean hidden,
            Integer status,
            Long selfId) {
        String normalizedNodeType = normalizeRequired(nodeType, "nodeType").toLowerCase(Locale.ROOT);
        if (!SUPPORTED_NODE_TYPES.contains(normalizedNodeType)) {
            throw new IllegalArgumentException("不支持的节点类型");
        }
        String normalizedName = normalizeRequired(name, "name");
        String normalizedRoutePath = StringUtils.hasText(routePath) ? normalizeRoute(routePath) : null;
        String normalizedComponentKey = StringUtils.hasText(componentKey) ? componentKey.trim() : null;
        String normalizedPermissionCode = StringUtils.hasText(permissionCode) ? permissionCode.trim() : null;
        String normalizedScope = StringUtils.hasText(scope) ? normalizeScope(scope) : null;
        Long normalizedParentId = parentId;
        if (normalizedParentId != null) {
            AdminMenuNode parent = findNodeById(normalizedParentId).orElseThrow(() -> new IllegalArgumentException("父级菜单不存在"));
            if (selfId != null && selfId.equals(normalizedParentId)) {
                throw new IllegalArgumentException("不能将自身设为父级菜单");
            }
            if (NODE_TYPE_BUTTON.equals(normalizedNodeType) && !NODE_TYPE_MENU.equals(parent.nodeType())) {
                throw new IllegalArgumentException("按钮节点的父级必须是菜单节点");
            }
            if (NODE_TYPE_MENU.equals(normalizedNodeType)
                    && !NODE_TYPE_MENU.equals(parent.nodeType())
                    && !NODE_TYPE_CATALOG.equals(parent.nodeType())) {
                throw new IllegalArgumentException("菜单节点的父级必须是目录或菜单节点");
            }
            if (NODE_TYPE_CATALOG.equals(normalizedNodeType) && !NODE_TYPE_CATALOG.equals(parent.nodeType())) {
                throw new IllegalArgumentException("目录节点的父级必须是目录节点");
            }
            if (normalizedScope == null) {
                normalizedScope = parent.scope();
            }
        }
        if (normalizedScope == null) {
            normalizedScope = "business";
        }
        if (NODE_TYPE_CATALOG.equals(normalizedNodeType)) {
            if (normalizedRoutePath != null || normalizedComponentKey != null || normalizedPermissionCode != null) {
                throw new IllegalArgumentException("目录节点不支持路由路径、组件或权限码");
            }
        }
        if (NODE_TYPE_MENU.equals(normalizedNodeType)) {
            if (!StringUtils.hasText(normalizedRoutePath) || !StringUtils.hasText(normalizedComponentKey)) {
                throw new IllegalArgumentException("菜单节点必须填写路由路径和组件路径");
            }
            normalizedPermissionCode = null;
            ensureRoutePathAvailable(normalizedRoutePath, normalizedScope, selfId);
        }
        if (NODE_TYPE_BUTTON.equals(normalizedNodeType)) {
            if (!StringUtils.hasText(normalizedPermissionCode)) {
                throw new IllegalArgumentException("按钮节点必须填写权限码");
            }
            ensurePermissionCodeAvailable(normalizedPermissionCode, selfId);
            ensurePermissionDefinitionExists(normalizedPermissionCode);
            normalizedRoutePath = null;
            normalizedComponentKey = null;
        }
        return new ValidatedNode(
                normalizedScope,
                normalizedNodeType,
                normalizedName,
                normalizedRoutePath,
                normalizedComponentKey,
                normalizedPermissionCode,
                normalizedParentId,
                orderNo == null ? 0 : orderNo,
                StringUtils.hasText(icon) ? icon.trim() : null,
                hidden != null && hidden,
                status == null ? 1 : status);
    }

    public String normalizeRoute(String routePath) {
        String normalized = normalizeRequired(routePath, "routePath").toLowerCase(Locale.ROOT);
        normalized = normalized.replace('\\', '/').replaceAll("/+", "/");
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        return normalized;
    }

    private void ensureRoutePathAvailable(String routePath, String scope, Long selfId) {
        String canonicalRoutePath = canonicalRoutePath(routePath, scope);
        if (!routePath.equals(canonicalRoutePath)) {
            throw new IllegalArgumentException("请使用规范路由路径：" + canonicalRoutePath);
        }
        List<RoutePathRowPo> existingRouteRows = adminMenuRepository.findExistingRoutePaths(selfId);
        boolean exists = existingRouteRows.stream()
                .map(row -> {
                    String existingRoutePath = row.getRoutePath();
                    if (existingRoutePath == null) {
                        return null;
                    }
                    String existingScope = row.getScope() == null ? scope : row.getScope();
                    return canonicalRoutePath(existingRoutePath, existingScope);
                })
                .filter(Objects::nonNull)
                .anyMatch(canonicalRoutePath::equals);
        if (exists) {
            throw new IllegalArgumentException("路由路径已被其他菜单使用");
        }
    }

    private String canonicalRoutePath(String routePath, String scope) {
        if (!"platform".equalsIgnoreCase(scope)) {
            return switch (routePath) {
                case "/system/menus" -> "/system/menu";
                case "/system/roles" -> "/system/role";
                case "/system/users" -> "/system/user";
                case "/system/users/offboard-pool" -> "/system/user/offboard-pool";
                default -> routePath;
            };
        }
        return switch (routePath) {
            case "/platform/menus", "/system/menus" -> "/platform/menu";
            case "/platform/roles", "/system/roles" -> "/platform/role";
            case "/platform/users", "/system/users" -> "/platform/user";
            case "/platform/user/offboard-pool", "/system/user/offboard-pool", "/system/users/offboard-pool" -> "/platform/offboard-pool";
            default -> routePath;
        };
    }

    private void ensureRoutePathAvailable(String routePath, Long selfId) {
        if (adminMenuRepository.countByRoutePath(routePath, selfId) > 0) {
            throw new IllegalArgumentException("路由路径已被其他菜单使用");
        }
    }

    private void ensurePermissionDefinitionExists(String permissionCode) {
        if (AdminPermissionCatalog.findByCode(permissionCode).isEmpty()) {
            throw new IllegalArgumentException("权限码不存在，请从权限码列表中选择");
        }
    }

    private String normalizeScope(String scope) {
        String normalizedScope = normalizeRequired(scope, "scope").toLowerCase(Locale.ROOT);
        if (!List.of("platform", "business").contains(normalizedScope)) {
            throw new IllegalArgumentException("不支持的域类型，必须是 platform 或 business");
        }
        return normalizedScope;
    }

    private PermissionDefinition requirePermissionDefinition(String permissionCode) {
        return AdminPermissionCatalog.findByCode(permissionCode)
                .orElseThrow(() -> new IllegalArgumentException("权限码不存在"));
    }

    private void ensurePermissionCodeAvailable(String permissionCode, Long selfId) {
        if (adminMenuRepository.countByPermissionCode(permissionCode, selfId) > 0) {
            throw new IllegalArgumentException("权限码已被其他节点使用");
        }
    }

    private void ensureNodeType(Set<Long> nodeIds, String expectedType) {
        ensureNodeTypes(nodeIds, List.of(expectedType));
    }

    private void ensureNodeTypes(Set<Long> nodeIds, List<String> expectedTypes) {
        if (nodeIds.isEmpty()) {
            return;
        }
        if (adminMenuRepository.countByIdsAndNodeTypes(new ArrayList<>(nodeIds), expectedTypes) != nodeIds.size()) {
            throw new IllegalArgumentException("存在无效的菜单节点引用");
        }
    }

    private void ensureRoleCanOwnButtonPermissions(int roleId, Set<Long> buttonIds) {
        if (buttonIds.isEmpty()) {
            return;
        }
        String roleScope = adminMenuRepository.findRoleScopeById(roleId);
        if ("global".equalsIgnoreCase(roleScope)) {
            return;
        }
        List<String> permissionCodes = adminMenuRepository.findPermissionCodesByMenuIds(new ArrayList<>(buttonIds));
        for (String permissionCode : permissionCodes) {
            PermissionDefinition definition = requirePermissionDefinition(permissionCode);
            if (!"domain".equalsIgnoreCase(definition.permissionScope())) {
                throw new IllegalArgumentException("domain role cannot own platform permission");
            }
        }
    }

    private void replaceRolePermissionRows(int roleId, Set<Long> buttonIds) {
        try {
            List<String> catalogCodes = AdminPermissionCatalog.list().stream()
                    .map(PermissionDefinition::code)
                    .toList();
            adminMenuRepository.deleteRolePermissionsByCatalog(roleId, catalogCodes);
            if (buttonIds.isEmpty()) {
                return;
            }
            List<String> selectedPermissionCodes = adminMenuRepository.findPermissionCodesByMenuIds(new ArrayList<>(buttonIds)).stream()
                    .filter(StringUtils::hasText)
                    .toList();
            adminMenuRepository.insertRolePermissionsByCodes(roleId, selectedPermissionCodes);
        } catch (DataAccessException ignored) {
        }
    }

    private void assertRoleExists(int roleId) {
        if (adminMenuRepository.countRoleById(roleId) == 0) {
            throw new IllegalArgumentException("role not found");
        }
    }

    private Optional<AdminMenuNode> findNodeById(long menuId) {
        return adminMenuRepository.findById(menuId).map(po -> mapPo(po, Map.of()));
    }

    private Optional<AdminMenuNode> findRequiredButton(long menuId) {
        return adminMenuRepository.findRequiredButton(menuId).map(po -> mapPo(po, Map.of()));
    }

    public static List<AdminMenuNode> buildTree(List<AdminMenuNode> nodes) {
        Map<Long, AdminMenuNodeBuilder> byId = new LinkedHashMap<>();
        for (AdminMenuNode node : nodes) {
            byId.put(node.id(), new AdminMenuNodeBuilder(node));
        }
        List<AdminMenuNodeBuilder> roots = new ArrayList<>();
        for (AdminMenuNodeBuilder builder : byId.values()) {
            if (builder.node.parentId() == null || !byId.containsKey(builder.node.parentId())) {
                roots.add(builder);
                continue;
            }
            byId.get(builder.node.parentId()).children.add(builder);
        }
        roots.sort(Comparator
                .comparingInt((AdminMenuNodeBuilder left) -> left.node.orderNo())
                .thenComparingLong(left -> left.node.id()));
        return roots.stream().map(AdminMenuNodeBuilder::toNode).toList();
    }

    private AdminMenuNode mapPo(AdminMenuPo po, Map<Long, String> requiredPermissionByMenuId) {
        String permissionCode = po.getPermissionCode();
        if (NODE_TYPE_MENU.equals(po.getNodeType())) {
            permissionCode = requiredPermissionByMenuId.getOrDefault(po.getId(), permissionCode);
        }
        return new AdminMenuNode(
                po.getId(),
                po.getCode(),
                po.getNodeType(),
                po.getScope(),
                po.getName(),
                po.getRoutePath(),
                po.getComponentKey(),
                permissionCode,
                po.getParentId(),
                po.getOrderNo() == null ? 0 : po.getOrderNo(),
                po.getIcon(),
                po.getHidden() != null && po.getHidden() == 1,
                po.getStatus() == null ? 0 : po.getStatus(),
                po.getRequired() != null && po.getRequired() == 1,
                List.of());
    }

    private AdminMenuPo toAdminMenuPo(Long id, ValidatedNode validated) {
        AdminMenuPo po = new AdminMenuPo();
        po.setId(id);
        po.setNodeType(validated.nodeType());
        po.setScope(validated.scope());
        po.setName(validated.name());
        po.setRoutePath(validated.routePath());
        po.setComponentKey(validated.componentKey());
        po.setPermissionCode(validated.permissionCode());
        po.setParentId(validated.parentId());
        po.setOrderNo(validated.orderNo());
        po.setIcon(validated.icon());
        po.setHidden(validated.hidden() ? 1 : 0);
        po.setStatus(validated.status());
        return po;
    }

    private RolePermissionRow toRolePermissionRow(RolePermissionRowPo po) {
        return new RolePermissionRow(po.getCode(), po.getName(), po.getHttpMethod(), po.getPathPattern());
    }

    private String normalizeRequired(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    private String generateNodeCode(long nodeId) {
        return "ADM" + String.format(Locale.ROOT, "%010d", nodeId);
    }

    private String generateTempCode() {
        return "TMP-" + UUID.randomUUID();
    }

    private Map<Long, String> loadRequiredPermissionByMenuId() {
        return loadRequiredPermissionByMenuId(null);
    }

    private Map<Long, String> loadRequiredPermissionByMenuId(@Nullable String scope) {
        return adminMenuRepository.findRequiredPermissionByMenuId(scope);
    }

    private String normalizeScopeFilter(@Nullable String scope) {
        if (!StringUtils.hasText(scope)) {
            return null;
        }
        return scope.trim().toLowerCase(Locale.ROOT);
    }

    public record AdminMenuNode(
            long id,
            String code,
            String nodeType,
            String scope,
            String name,
            String routePath,
            String componentKey,
            String permissionCode,
            Long parentId,
            int orderNo,
            String icon,
            boolean hidden,
            int status,
            boolean required,
            List<AdminMenuNode> children) {
    }

    public record CreateAdminMenuCommand(
            String nodeType,
            String name,
            String routePath,
            String componentKey,
            String permissionCode,
            String scope,
            Long parentId,
            Integer orderNo,
            String icon,
            Boolean hidden,
            Integer status) {
    }

    public record UpdateAdminMenuCommand(
            String nodeType,
            String name,
            String routePath,
            String componentKey,
            String permissionCode,
            String scope,
            Long parentId,
            Integer orderNo,
            String icon,
            Boolean hidden,
            Integer status) {
    }

    public record RolePermissions(
            int roleId,
            List<Long> menuIds,
            List<Long> buttonIds) {
    }

    private record RequiredMenuIds(
            List<Long> menuIds,
            List<Long> buttonIds) {
    }

    public record GrantedPermission(
            long id,
            String name,
            String permissionCode,
            String httpMethod,
            String pathPattern,
            Long parentId,
            int orderNo,
            boolean required) {
    }

    public record PermissionSnapshotData(
            List<AdminMenuNode> menus,
            List<GrantedPermission> actions) {
    }

    private record RolePermissionRow(
            String code,
            String name,
            String httpMethod,
            String pathPattern) {
    }

    private record ValidatedNode(
            String scope,
            String nodeType,
            String name,
            String routePath,
            String componentKey,
            String permissionCode,
            Long parentId,
            int orderNo,
            String icon,
            boolean hidden,
            int status) {
    }

    private static final class AdminMenuNodeBuilder {
        private final AdminMenuNode node;
        private final List<AdminMenuNodeBuilder> children = new ArrayList<>();

        private AdminMenuNodeBuilder(AdminMenuNode node) {
            this.node = node;
        }

        private AdminMenuNode toNode() {
            children.sort(Comparator
                    .comparingInt((AdminMenuNodeBuilder left) -> left.node.orderNo())
                    .thenComparingLong(left -> left.node.id()));
            return new AdminMenuNode(
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
                    children.stream().map(AdminMenuNodeBuilder::toNode).toList());
        }
    }
}
