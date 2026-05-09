package com.uniondesk.iam.admin;

import com.uniondesk.iam.admin.AdminPermissionCatalog.PermissionDefinition;
import java.sql.PreparedStatement;
import java.sql.Statement;
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
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
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

    private final JdbcTemplate jdbcTemplate;
    private final Clock clock;

    public AdminMenuService(JdbcTemplate jdbcTemplate, Clock clock) {
        this.jdbcTemplate = jdbcTemplate;
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

    public boolean handlesApiRequest(String method, String requestPath) {
        return AdminPermissionCatalog.findByRequest(method, requestPath).isPresent();
    }

    public boolean isApiAllowed(String roleCode, String method, String requestPath) {
        Optional<PermissionDefinition> matched = AdminPermissionCatalog.findByRequest(method, requestPath);
        if (matched.isEmpty()) {
            return false;
        }
        return loadGrantedPermissionCodesByRole(roleCode).contains(matched.get().code());
    }

    public List<AdminMenuNode> listMenuTree() {
        Map<Long, String> requiredPermissionByMenuId = loadRequiredPermissionByMenuId();
        List<AdminMenuNode> nodes = jdbcTemplate.query("""
                        SELECT
                            id,
                            code,
                            node_type,
                            scope,
                            name,
                            route_path,
                            component_key,
                            permission_code,
                            parent_id,
                            order_no,
                            icon,
                            hidden,
                            status,
                            required
                        FROM iam_admin_menu
                        ORDER BY order_no, id
                        """,
                (rs, rowNum) -> mapNode(rs, requiredPermissionByMenuId));
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
        String placeholders = String.join(",", normalizedRoleCodes.stream().map(code -> "?").toList());
        List<AdminMenuNode> authorizedNodes = jdbcTemplate.query("""
                        SELECT DISTINCT
                            m.id,
                            m.code,
                            m.node_type,
                            m.scope,
                            m.name,
                            m.route_path,
                            m.component_key,
                            m.permission_code,
                            m.parent_id,
                            m.order_no,
                            m.icon,
                            m.hidden,
                            m.status,
                            m.required
                        FROM iam_admin_role_menu_relation relation
                        JOIN role r ON r.id = relation.role_id
                        JOIN iam_admin_menu m ON m.id = relation.menu_id
                        WHERE r.code IN (%s)
                          AND m.status = 1
                        ORDER BY m.order_no, m.id
                        """.formatted(placeholders),
                (rs, rowNum) -> mapNode(rs),
                normalizedRoleCodes.toArray());
        List<AdminMenuNode> menuNodes = authorizedNodes.stream()
                .filter(node -> NODE_TYPE_MENU.equals(node.nodeType()))
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
        return new PermissionSnapshotData(menuNodes, actionNodes);
    }

    @Transactional
    public AdminMenuNode createMenu(CreateAdminMenuCommand command) {
        ValidatedNode validated = validateNode(command.nodeType(), command.name(), command.routePath(), command.componentKey(),
                command.permissionCode(), command.scope(), command.parentId(), command.orderNo(), command.icon(), command.hidden(), command.status(), null);
        long nodeId = insertNode(validated);
        String generatedCode = generateNodeCode(nodeId);
        jdbcTemplate.update("UPDATE iam_admin_menu SET code = ? WHERE id = ?", generatedCode, nodeId);
        if (NODE_TYPE_MENU.equals(validated.nodeType())) {
            insertRequiredButton(nodeId, validated.permissionCode(), validated.scope());
        }
        return findNodeById(nodeId).orElseThrow(() -> new IllegalStateException("menu create failed"));
    }

    @Transactional
    public AdminMenuNode updateMenu(long menuId, UpdateAdminMenuCommand command) {
        AdminMenuNode existing = findNodeById(menuId).orElseThrow(() -> new IllegalArgumentException("menu not found"));
        String nextNodeType = command.nodeType() == null ? existing.nodeType() : command.nodeType();
        if (!existing.nodeType().equals(nextNodeType)) {
            throw new IllegalArgumentException("node type change is not supported");
        }
        String existingMenuPermissionCode = NODE_TYPE_MENU.equals(existing.nodeType())
                ? findRequiredButton(menuId).map(AdminMenuNode::permissionCode).orElse(null)
                : existing.permissionCode();
        ValidatedNode validated = validateNode(
                nextNodeType,
                command.name() == null ? existing.name() : command.name(),
                command.routePath() == null ? existing.routePath() : command.routePath(),
                command.componentKey() == null ? existing.componentKey() : command.componentKey(),
                command.permissionCode() == null ? existingMenuPermissionCode : command.permissionCode(),
                command.scope() == null ? existing.scope() : command.scope(),
                command.parentId() == null ? existing.parentId() : command.parentId(),
                command.orderNo() == null ? existing.orderNo() : command.orderNo(),
                command.icon() == null ? existing.icon() : command.icon(),
                command.hidden() == null ? existing.hidden() : command.hidden(),
                command.status() == null ? existing.status() : command.status(),
                menuId);
        jdbcTemplate.update("""
                        UPDATE iam_admin_menu
                        SET
                            scope = ?,
                            name = ?,
                            route_path = ?,
                            component_key = ?,
                            permission_code = ?,
                            parent_id = ?,
                            order_no = ?,
                            icon = ?,
                            hidden = ?,
                            status = ?
                        WHERE id = ?
                        """,
                validated.scope(),
                validated.name(),
                validated.routePath(),
                validated.componentKey(),
                validated.permissionCode(),
                validated.parentId(),
                validated.orderNo(),
                validated.icon(),
                validated.hidden() ? 1 : 0,
                validated.status(),
                menuId);
        if (NODE_TYPE_MENU.equals(existing.nodeType())) {
            updateRequiredButton(menuId, validated.permissionCode(), validated.scope());
        }
        return findNodeById(menuId).orElseThrow(() -> new IllegalArgumentException("menu not found"));
    }

    @Transactional
    public void deleteMenu(long menuId) {
        AdminMenuNode existing = findNodeById(menuId).orElseThrow(() -> new IllegalArgumentException("menu not found"));
        if (existing.required()) {
            throw new IllegalArgumentException("required button cannot be deleted");
        }
        Integer childCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM iam_admin_menu WHERE parent_id = ?",
                Integer.class,
                menuId);
        if (childCount != null && childCount > 0) {
            throw new IllegalArgumentException("menu has children, delete child nodes first");
        }
        Integer relationCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM iam_admin_role_menu_relation WHERE menu_id = ?",
                Integer.class,
                menuId);
        if (relationCount != null && relationCount > 0) {
            throw new IllegalArgumentException("menu is bound to roles, unbind first");
        }
        jdbcTemplate.update("DELETE FROM iam_admin_menu WHERE id = ?", menuId);
    }

    public RolePermissions loadRolePermissions(int roleId) {
        assertRoleExists(roleId);
        List<Long> menuIds = jdbcTemplate.query("""
                        SELECT menu_id
                        FROM iam_admin_role_menu_relation relation
                        JOIN iam_admin_menu menu ON menu.id = relation.menu_id
                        WHERE relation.role_id = ?
                          AND menu.node_type = 'menu'
                        ORDER BY menu.order_no, menu.id
                        """,
                (rs, rowNum) -> rs.getLong("menu_id"),
                roleId);
        List<Long> buttonIds = jdbcTemplate.query("""
                        SELECT menu_id
                        FROM iam_admin_role_menu_relation relation
                        JOIN iam_admin_menu menu ON menu.id = relation.menu_id
                        WHERE relation.role_id = ?
                          AND menu.node_type = 'button'
                        ORDER BY menu.order_no, menu.id
                        """,
                (rs, rowNum) -> rs.getLong("menu_id"),
                roleId);
        return new RolePermissions(roleId, menuIds, buttonIds);
    }

    @Transactional
    public RolePermissions replaceRolePermissions(int roleId, List<Long> menuIds, List<Long> buttonIds) {
        assertRoleExists(roleId);
        Set<Long> normalizedMenuIds = menuIds == null ? new LinkedHashSet<>() : new LinkedHashSet<>(menuIds);
        Set<Long> normalizedButtonIds = buttonIds == null ? new LinkedHashSet<>() : new LinkedHashSet<>(buttonIds);
        ensureNodeType(normalizedMenuIds, NODE_TYPE_MENU);
        ensureNodeType(normalizedButtonIds, NODE_TYPE_BUTTON);
        if (!normalizedButtonIds.isEmpty()) {
            String buttonPlaceholders = String.join(",", normalizedButtonIds.stream().map(id -> "?").toList());
            List<Long> parentMenuIds = jdbcTemplate.query(
                    "SELECT DISTINCT parent_id FROM iam_admin_menu WHERE id IN (%s)".formatted(buttonPlaceholders),
                    (rs, rowNum) -> rs.getLong("parent_id"),
                    normalizedButtonIds.toArray());
            normalizedMenuIds.addAll(parentMenuIds);
        }
        if (!normalizedMenuIds.isEmpty()) {
            String menuPlaceholders = String.join(",", normalizedMenuIds.stream().map(id -> "?").toList());
            List<Long> requiredButtonIds = jdbcTemplate.query("""
                            SELECT id
                            FROM iam_admin_menu
                            WHERE parent_id IN (%s)
                              AND node_type = 'button'
                              AND required = 1
            """.formatted(menuPlaceholders),
                    (rs, rowNum) -> rs.getLong("id"),
                    normalizedMenuIds.toArray());
            normalizedButtonIds.addAll(requiredButtonIds);
        }
        ensureRoleCanOwnButtonPermissions(roleId, normalizedButtonIds);
        jdbcTemplate.update("DELETE FROM iam_admin_role_menu_relation WHERE role_id = ?", roleId);
        for (Long menuId : normalizedMenuIds) {
            jdbcTemplate.update("""
                            INSERT INTO iam_admin_role_menu_relation (role_id, menu_id, created_at)
                            VALUES (?, ?, CURRENT_TIMESTAMP(3))
                            """,
                    roleId,
                    menuId);
        }
        for (Long buttonId : normalizedButtonIds) {
            jdbcTemplate.update("""
                            INSERT INTO iam_admin_role_menu_relation (role_id, menu_id, created_at)
                            VALUES (?, ?, CURRENT_TIMESTAMP(3))
                            """,
                    roleId,
                    buttonId);
        }
        replaceRolePermissionRows(roleId, normalizedButtonIds);
        return loadRolePermissions(roleId);
    }

    private Set<String> loadGrantedPermissionCodesByRole(String roleCode) {
        if (!StringUtils.hasText(roleCode)) {
            return Set.of();
        }
        return new LinkedHashSet<>(jdbcTemplate.query("""
                        SELECT DISTINCT menu.permission_code
                        FROM iam_admin_role_menu_relation relation
                        JOIN role r ON r.id = relation.role_id
                        JOIN iam_admin_menu menu ON menu.id = relation.menu_id
                        WHERE r.code = ?
                          AND menu.node_type = 'button'
                          AND menu.status = 1
                          AND menu.permission_code IS NOT NULL
                        """,
                (rs, rowNum) -> rs.getString("permission_code"),
                roleCode.trim()));
    }

    private long insertNode(ValidatedNode validated) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                            INSERT INTO iam_admin_menu (
                                code,
                                node_type,
                                scope,
                                name,
                                route_path,
                                component_key,
                                permission_code,
                                parent_id,
                                order_no,
                                icon,
                                hidden,
                                status,
                                required
                            )
                            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                            """,
                    Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, generateTempCode());
            statement.setString(2, validated.nodeType());
            statement.setString(3, validated.scope());
            statement.setString(4, validated.name());
            statement.setString(5, validated.routePath());
            statement.setString(6, validated.componentKey());
            statement.setString(7, validated.permissionCode());
            if (validated.parentId() == null) {
                statement.setObject(8, null);
            } else {
                statement.setLong(8, validated.parentId());
            }
            statement.setInt(9, validated.orderNo());
            statement.setString(10, validated.icon());
            statement.setInt(11, validated.hidden() ? 1 : 0);
            statement.setInt(12, validated.status());
            statement.setInt(13, 0);
            return statement;
        }, keyHolder);
        Number generatedId = keyHolder.getKey();
        if (generatedId == null) {
            throw new IllegalStateException("menu create failed");
        }
        return generatedId.longValue();
    }

    private void insertRequiredButton(long parentMenuId, String permissionCode, String scope) {
        PermissionDefinition definition = requirePermissionDefinition(permissionCode);
        long buttonId = insertButton(parentMenuId, definition, true, scope);
        jdbcTemplate.update("UPDATE iam_admin_menu SET code = ? WHERE id = ?", generateNodeCode(buttonId), buttonId);
    }

    private void updateRequiredButton(long menuId, String permissionCode, String scope) {
        PermissionDefinition definition = requirePermissionDefinition(permissionCode);
        Optional<AdminMenuNode> requiredButton = findRequiredButton(menuId);
        if (requiredButton.isEmpty()) {
            insertRequiredButton(menuId, permissionCode, scope);
            return;
        }
        ensurePermissionCodeAvailable(permissionCode, requiredButton.get().id());
        jdbcTemplate.update("""
                        UPDATE iam_admin_menu
                        SET
                            scope = ?,
                            name = ?,
                            permission_code = ?,
                            status = 1,
                            hidden = 0
                        WHERE id = ?
                        """,
                scope,
                definition.name(),
                definition.code(),
                requiredButton.get().id());
    }

    private long insertButton(long parentMenuId, PermissionDefinition definition, boolean required, String scope) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                            INSERT INTO iam_admin_menu (
                                code,
                                node_type,
                                scope,
                                name,
                                route_path,
                                component_key,
                                permission_code,
                                parent_id,
                                order_no,
                                icon,
                                hidden,
                                status,
                                required
                            )
                            VALUES (?, 'button', ?, ?, NULL, NULL, ?, ?, 0, NULL, 0, 1, ?)
                            """,
                    Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, generateTempCode());
            statement.setString(2, scope);
            statement.setString(3, definition.name());
            statement.setString(4, definition.code());
            statement.setLong(5, parentMenuId);
            statement.setInt(6, required ? 1 : 0);
            return statement;
        }, keyHolder);
        Number generatedId = keyHolder.getKey();
        if (generatedId == null) {
            throw new IllegalStateException("button create failed");
        }
        return generatedId.longValue();
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
            throw new IllegalArgumentException("unsupported node type");
        }
        String normalizedName = normalizeRequired(name, "name");
        String normalizedRoutePath = StringUtils.hasText(routePath) ? normalizeRoute(routePath) : null;
        String normalizedComponentKey = StringUtils.hasText(componentKey) ? componentKey.trim() : null;
        String normalizedPermissionCode = StringUtils.hasText(permissionCode) ? permissionCode.trim() : null;
        String normalizedScope = StringUtils.hasText(scope) ? normalizeScope(scope) : null;
        Long normalizedParentId = parentId;
        if (normalizedParentId != null) {
            AdminMenuNode parent = findNodeById(normalizedParentId).orElseThrow(() -> new IllegalArgumentException("parent menu not found"));
            if (selfId != null && selfId.equals(normalizedParentId)) {
                throw new IllegalArgumentException("parent menu is invalid");
            }
            if (NODE_TYPE_BUTTON.equals(normalizedNodeType) && !NODE_TYPE_MENU.equals(parent.nodeType())) {
                throw new IllegalArgumentException("button parent must be menu");
            }
            if (NODE_TYPE_MENU.equals(normalizedNodeType)
                    && !NODE_TYPE_MENU.equals(parent.nodeType())
                    && !NODE_TYPE_CATALOG.equals(parent.nodeType())) {
                throw new IllegalArgumentException("menu parent must be catalog or menu");
            }
            if (NODE_TYPE_CATALOG.equals(normalizedNodeType) && !NODE_TYPE_CATALOG.equals(parent.nodeType())) {
                throw new IllegalArgumentException("catalog parent must be catalog");
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
                throw new IllegalArgumentException("catalog does not support route, component or permission code");
            }
        }
        if (NODE_TYPE_MENU.equals(normalizedNodeType)) {
            if (!StringUtils.hasText(normalizedRoutePath) || !StringUtils.hasText(normalizedComponentKey)) {
                throw new IllegalArgumentException("menu requires routePath and componentKey");
            }
            normalizedPermissionCode = normalizeRequired(normalizedPermissionCode, "permissionCode");
            ensureRoutePathAvailable(normalizedRoutePath, selfId);
            ensurePermissionDefinitionExists(normalizedPermissionCode);
            if (selfId == null) {
                ensurePermissionCodeAvailable(normalizedPermissionCode, null);
            }
        }
        if (NODE_TYPE_BUTTON.equals(normalizedNodeType)) {
            if (!StringUtils.hasText(normalizedPermissionCode)) {
                throw new IllegalArgumentException("button requires permissionCode");
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

    private void ensureRoutePathAvailable(String routePath, Long selfId) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                SELECT COUNT(*)
                FROM iam_admin_menu
                WHERE route_path = ?
                """);
        args.add(routePath);
        if (selfId != null) {
            sql.append(" AND id <> ?");
            args.add(selfId);
        }
        Integer count = jdbcTemplate.queryForObject(sql.toString(), Integer.class, args.toArray());
        if (count != null && count > 0) {
            throw new IllegalArgumentException("routePath already exists");
        }
    }

    private void ensurePermissionDefinitionExists(String permissionCode) {
        if (AdminPermissionCatalog.findByCode(permissionCode).isEmpty()) {
            throw new IllegalArgumentException("permissionCode not found");
        }
    }

    private String normalizeScope(String scope) {
        String normalizedScope = normalizeRequired(scope, "scope").toLowerCase(Locale.ROOT);
        if (!List.of("platform", "business").contains(normalizedScope)) {
            throw new IllegalArgumentException("unsupported scope");
        }
        return normalizedScope;
    }

    private PermissionDefinition requirePermissionDefinition(String permissionCode) {
        return AdminPermissionCatalog.findByCode(permissionCode)
                .orElseThrow(() -> new IllegalArgumentException("permissionCode not found"));
    }

    private void ensurePermissionCodeAvailable(String permissionCode, Long selfId) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                SELECT COUNT(*)
                FROM iam_admin_menu
                WHERE permission_code = ?
                """);
        args.add(permissionCode);
        if (selfId != null) {
            sql.append(" AND id <> ?");
            args.add(selfId);
        }
        Integer count = jdbcTemplate.queryForObject(sql.toString(), Integer.class, args.toArray());
        if (count != null && count > 0) {
            throw new IllegalArgumentException("permissionCode already exists");
        }
    }

    private void ensureNodeType(Set<Long> nodeIds, String expectedType) {
        if (nodeIds.isEmpty()) {
            return;
        }
        String placeholders = String.join(",", nodeIds.stream().map(id -> "?").toList());
        List<Object> args = new ArrayList<>(nodeIds);
        args.add(expectedType);
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM iam_admin_menu WHERE id IN (%s) AND node_type = ?".formatted(placeholders),
                Long.class,
                args.toArray());
        if (count == null || count != nodeIds.size()) {
            throw new IllegalArgumentException(expectedType + " node not found");
        }
    }

    private void ensureRoleCanOwnButtonPermissions(int roleId, Set<Long> buttonIds) {
        if (buttonIds.isEmpty()) {
            return;
        }
        String roleScope = jdbcTemplate.queryForObject("SELECT scope FROM role WHERE id = ?", String.class, roleId);
        if ("global".equalsIgnoreCase(roleScope)) {
            return;
        }
        String placeholders = String.join(",", buttonIds.stream().map(id -> "?").toList());
        List<String> permissionCodes = jdbcTemplate.query(
                "SELECT permission_code FROM iam_admin_menu WHERE id IN (%s)".formatted(placeholders),
                (rs, rowNum) -> rs.getString("permission_code"),
                buttonIds.toArray());
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
            String catalogPlaceholders = String.join(",", catalogCodes.stream().map(code -> "?").toList());
            List<Object> deleteArgs = new ArrayList<>();
            deleteArgs.add(roleId);
            deleteArgs.addAll(catalogCodes);
            jdbcTemplate.update("""
                            DELETE rp
                            FROM iam_role_permission rp
                            JOIN iam_permission p ON p.id = rp.permission_id
                            WHERE rp.role_id = ?
                              AND p.code IN (%s)
                            """.formatted(catalogPlaceholders),
                    deleteArgs.toArray());
            if (buttonIds.isEmpty()) {
                return;
            }
            String buttonPlaceholders = String.join(",", buttonIds.stream().map(id -> "?").toList());
            List<String> selectedPermissionCodes = jdbcTemplate.query("""
                            SELECT permission_code
                            FROM iam_admin_menu
                            WHERE id IN (%s)
                              AND node_type = 'button'
                              AND permission_code IS NOT NULL
                            """.formatted(buttonPlaceholders),
                    (rs, rowNum) -> rs.getString("permission_code"),
                    buttonIds.toArray());
            if (selectedPermissionCodes.isEmpty()) {
                return;
            }
            String permissionPlaceholders = String.join(",", selectedPermissionCodes.stream().map(code -> "?").toList());
            List<Object> insertArgs = new ArrayList<>();
            insertArgs.add(roleId);
            insertArgs.addAll(selectedPermissionCodes);
            jdbcTemplate.update("""
                            INSERT IGNORE INTO iam_role_permission (role_id, permission_id)
                            SELECT ?, p.id
                            FROM iam_permission p
                            WHERE p.code IN (%s)
                            """.formatted(permissionPlaceholders),
                    insertArgs.toArray());
        } catch (DataAccessException ignored) {
        }
    }

    private void assertRoleExists(int roleId) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM role WHERE id = ?", Integer.class, roleId);
        if (count == null || count == 0) {
            throw new IllegalArgumentException("role not found");
        }
    }

    private Optional<AdminMenuNode> findNodeById(long menuId) {
        try {
            AdminMenuNode node = jdbcTemplate.queryForObject("""
                            SELECT
                                id,
                                code,
                                node_type,
                                scope,
                                name,
                                route_path,
                                component_key,
                                permission_code,
                                parent_id,
                                order_no,
                                icon,
                                hidden,
                                status,
                                required
                            FROM iam_admin_menu
                            WHERE id = ?
                            LIMIT 1
                            """,
                    (rs, rowNum) -> mapNode(rs),
                    menuId);
            return Optional.ofNullable(node);
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    private Optional<AdminMenuNode> findRequiredButton(long menuId) {
        try {
            AdminMenuNode node = jdbcTemplate.queryForObject("""
                            SELECT
                                id,
                                code,
                                node_type,
                                scope,
                                name,
                                route_path,
                                component_key,
                                permission_code,
                                parent_id,
                                order_no,
                                icon,
                                hidden,
                                status,
                                required
                            FROM iam_admin_menu
                            WHERE parent_id = ?
                              AND node_type = 'button'
                              AND required = 1
                            LIMIT 1
                            """,
                    (rs, rowNum) -> mapNode(rs),
                    menuId);
            return Optional.ofNullable(node);
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    private List<AdminMenuNode> buildTree(List<AdminMenuNode> nodes) {
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

    private AdminMenuNode mapNode(java.sql.ResultSet rs) throws java.sql.SQLException {
        return mapNode(rs, Map.of());
    }

    private AdminMenuNode mapNode(java.sql.ResultSet rs, Map<Long, String> requiredPermissionByMenuId) throws java.sql.SQLException {
        long nodeId = rs.getLong("id");
        String nodeType = rs.getString("node_type");
        String permissionCode = rs.getString("permission_code");
        if (NODE_TYPE_MENU.equals(nodeType)) {
            permissionCode = requiredPermissionByMenuId.getOrDefault(nodeId, permissionCode);
        }
        return new AdminMenuNode(
                nodeId,
                rs.getString("code"),
                nodeType,
                rs.getString("scope"),
                rs.getString("name"),
                rs.getString("route_path"),
                rs.getString("component_key"),
                permissionCode,
                rs.getObject("parent_id", Long.class),
                rs.getInt("order_no"),
                rs.getString("icon"),
                rs.getInt("hidden") == 1,
                rs.getInt("status"),
                rs.getInt("required") == 1,
                List.of());
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
        return jdbcTemplate.query("""
                        SELECT parent_id, permission_code
                        FROM iam_admin_menu
                        WHERE node_type = 'button'
                          AND required = 1
                          AND parent_id IS NOT NULL
                        """,
                rs -> {
                    Map<Long, String> permissions = new LinkedHashMap<>();
                    while (rs.next()) {
                        permissions.put(rs.getLong("parent_id"), rs.getString("permission_code"));
                    }
                    return permissions;
                });
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
