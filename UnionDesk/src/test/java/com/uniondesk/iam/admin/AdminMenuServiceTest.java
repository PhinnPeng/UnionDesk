package com.uniondesk.iam.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.spy;

import java.sql.ResultSet;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.ResultSetExtractor;

class AdminMenuServiceTest {

    @Test
    void listMenuTreePreservesScopeFromRows() throws Exception {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        AdminMenuService service = new AdminMenuService(jdbcTemplate, Clock.systemUTC());
        when(jdbcTemplate.query(
                org.mockito.ArgumentMatchers.<String>argThat(sql -> sql != null && sql.contains("SELECT parent_id, permission_code")),
                org.mockito.ArgumentMatchers.<ResultSetExtractor<Map<Long, String>>>any()))
                .thenReturn(Map.of());
        when(jdbcTemplate.query(
                org.mockito.ArgumentMatchers.<String>argThat(sql -> sql != null && sql.contains("FROM iam_admin_menu") && sql.contains("ORDER BY order_no, id")),
                org.mockito.ArgumentMatchers.<RowMapper<AdminMenuService.AdminMenuNode>>any()))
                .thenAnswer(invocation -> {
                    RowMapper<AdminMenuService.AdminMenuNode> mapper = invocation.getArgument(1);
                    ResultSet rs = mock(ResultSet.class);
                    when(rs.getLong("id")).thenReturn(1L);
                    when(rs.getString("code")).thenReturn("ADM0000000001");
                    when(rs.getString("node_type")).thenReturn("menu");
                    when(rs.getString("scope")).thenReturn("platform");
                    when(rs.getString("name")).thenReturn("权限管理");
                    when(rs.getString("route_path")).thenReturn("/platform/permission");
                    when(rs.getString("component_key")).thenReturn("./platform/permission");
                    when(rs.getString("permission_code")).thenReturn("platform.role.read");
                    when(rs.getObject("parent_id", Long.class)).thenReturn(null);
                    when(rs.getInt("order_no")).thenReturn(1);
                    when(rs.getString("icon")).thenReturn("SafetyCertificateOutlined");
                    when(rs.getInt("hidden")).thenReturn(0);
                    when(rs.getInt("status")).thenReturn(1);
                    when(rs.getInt("required")).thenReturn(0);
                    return List.of(mapper.mapRow(rs, 0));
                });

        List<AdminMenuService.AdminMenuNode> nodes = service.listMenuTree();

        assertThat(nodes).singleElement().satisfies(node -> {
            assertThat(node.scope()).isEqualTo("platform");
            assertThat(node.routePath()).isEqualTo("/platform/permission");
        });
    }

    @Test
    void loadPermissionSnapshotPreservesScopeFromRows() throws Exception {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        AdminMenuService service = new AdminMenuService(jdbcTemplate, Clock.systemUTC());
        when(jdbcTemplate.query(
                org.mockito.ArgumentMatchers.<String>argThat(sql -> sql != null && sql.contains("FROM iam_admin_role_menu_relation") && sql.contains("JOIN iam_admin_menu m")),
                org.mockito.ArgumentMatchers.<RowMapper<AdminMenuService.AdminMenuNode>>any(),
                org.mockito.ArgumentMatchers.<Object[]>any()))
                .thenAnswer(invocation -> {
                    RowMapper<AdminMenuService.AdminMenuNode> mapper = invocation.getArgument(1);
                    ResultSet rs = mock(ResultSet.class);
                    when(rs.getLong("id")).thenReturn(2L);
                    when(rs.getString("code")).thenReturn("ADM0000000002");
                    when(rs.getString("node_type")).thenReturn("menu");
                    when(rs.getString("scope")).thenReturn("platform");
                    when(rs.getString("name")).thenReturn("平台角色");
                    when(rs.getString("route_path")).thenReturn("/platform/role");
                    when(rs.getString("component_key")).thenReturn("./system/role");
                    when(rs.getString("permission_code")).thenReturn("platform.role.read");
                    when(rs.getObject("parent_id", Long.class)).thenReturn(null);
                    when(rs.getInt("order_no")).thenReturn(2);
                    when(rs.getString("icon")).thenReturn("TeamOutlined");
                    when(rs.getInt("hidden")).thenReturn(0);
                    when(rs.getInt("status")).thenReturn(1);
                    when(rs.getInt("required")).thenReturn(0);
                    return List.of(mapper.mapRow(rs, 0));
                });

        AdminMenuService.PermissionSnapshotData snapshot = service.loadPermissionSnapshot(List.of("super_admin"));

        assertThat(snapshot.menus()).singleElement().satisfies(node -> {
            assertThat(node.scope()).isEqualTo("platform");
            assertThat(node.routePath()).isEqualTo("/platform/role");
        });
    }

    @Test
    void loadPermissionSnapshotFiltersCatalogNodesAndKeepsHiddenFlag() throws Exception {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        AdminMenuService service = new AdminMenuService(jdbcTemplate, Clock.systemUTC());
        when(jdbcTemplate.query(
                org.mockito.ArgumentMatchers.<String>argThat(sql -> sql != null && sql.contains("FROM iam_admin_role_menu_relation") && sql.contains("JOIN iam_admin_menu m")),
                org.mockito.ArgumentMatchers.<RowMapper<AdminMenuService.AdminMenuNode>>any(),
                org.mockito.ArgumentMatchers.<Object[]>any()))
                .thenAnswer(invocation -> {
                    RowMapper<AdminMenuService.AdminMenuNode> mapper = invocation.getArgument(1);

                    ResultSet catalog = mock(ResultSet.class);
                    when(catalog.getLong("id")).thenReturn(1L);
                    when(catalog.getString("code")).thenReturn("ADM0000000001");
                    when(catalog.getString("node_type")).thenReturn("catalog");
                    when(catalog.getString("scope")).thenReturn("platform");
                    when(catalog.getString("name")).thenReturn("平台管理");
                    when(catalog.getString("route_path")).thenReturn(null);
                    when(catalog.getString("component_key")).thenReturn(null);
                    when(catalog.getString("permission_code")).thenReturn(null);
                    when(catalog.getObject("parent_id", Long.class)).thenReturn(null);
                    when(catalog.getInt("order_no")).thenReturn(1);
                    when(catalog.getString("icon")).thenReturn(null);
                    when(catalog.getInt("hidden")).thenReturn(0);
                    when(catalog.getInt("status")).thenReturn(1);
                    when(catalog.getInt("required")).thenReturn(0);

                    ResultSet menu = mock(ResultSet.class);
                    when(menu.getLong("id")).thenReturn(2L);
                    when(menu.getString("code")).thenReturn("ADM0000000002");
                    when(menu.getString("node_type")).thenReturn("menu");
                    when(menu.getString("scope")).thenReturn("platform");
                    when(menu.getString("name")).thenReturn("平台首页");
                    when(menu.getString("route_path")).thenReturn("/platform/home");
                    when(menu.getString("component_key")).thenReturn("./platform/home");
                    when(menu.getString("permission_code")).thenReturn("platform.home.read");
                    when(menu.getObject("parent_id", Long.class)).thenReturn(null);
                    when(menu.getInt("order_no")).thenReturn(2);
                    when(menu.getString("icon")).thenReturn("HomeOutlined");
                    when(menu.getInt("hidden")).thenReturn(0);
                    when(menu.getInt("status")).thenReturn(1);
                    when(menu.getInt("required")).thenReturn(0);

                    ResultSet hiddenMenu = mock(ResultSet.class);
                    when(hiddenMenu.getLong("id")).thenReturn(3L);
                    when(hiddenMenu.getString("code")).thenReturn("ADM0000000003");
                    when(hiddenMenu.getString("node_type")).thenReturn("menu");
                    when(hiddenMenu.getString("scope")).thenReturn("platform");
                    when(hiddenMenu.getString("name")).thenReturn("工单详情");
                    when(hiddenMenu.getString("route_path")).thenReturn("/platform/ticket-detail");
                    when(hiddenMenu.getString("component_key")).thenReturn("./platform/ticket-detail");
                    when(hiddenMenu.getString("permission_code")).thenReturn("platform.ticket.read");
                    when(hiddenMenu.getObject("parent_id", Long.class)).thenReturn(null);
                    when(hiddenMenu.getInt("order_no")).thenReturn(3);
                    when(hiddenMenu.getString("icon")).thenReturn("ProfileOutlined");
                    when(hiddenMenu.getInt("hidden")).thenReturn(1);
                    when(hiddenMenu.getInt("status")).thenReturn(1);
                    when(hiddenMenu.getInt("required")).thenReturn(0);

                    return List.of(
                            mapper.mapRow(catalog, 0),
                            mapper.mapRow(menu, 1),
                            mapper.mapRow(hiddenMenu, 2));
                });

        AdminMenuService.PermissionSnapshotData snapshot = service.loadPermissionSnapshot(List.of("platform_admin"));

        assertThat(snapshot.menus()).hasSize(2);
        assertThat(snapshot.menus()).anySatisfy(node -> {
            assertThat(node.nodeType()).isEqualTo("menu");
            assertThat(node.hidden()).isFalse();
            assertThat(node.routePath()).isEqualTo("/platform/home");
        });
        assertThat(snapshot.menus()).anySatisfy(node -> {
            assertThat(node.nodeType()).isEqualTo("menu");
            assertThat(node.hidden()).isTrue();
            assertThat(node.routePath()).isEqualTo("/platform/ticket-detail");
        });
    }

    @Test
    void replaceRolePermissionsAutoCarriesRequiredPlatformHomeForGlobalRole() throws Exception {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        AdminMenuService service = new AdminMenuService(jdbcTemplate, Clock.systemUTC());
        int roleId = 7;
        long homeMenuId = 11L;
        long homeButtonId = 12L;

        when(jdbcTemplate.queryForObject(
                argThat(sql -> sql != null && sql.contains("SELECT COUNT(*) FROM role WHERE id = ?")),
                org.mockito.ArgumentMatchers.<Class<Integer>>eq(Integer.class),
                org.mockito.ArgumentMatchers.<Object[]>any()))
                .thenReturn(1);
        when(jdbcTemplate.queryForObject(
                argThat(sql -> sql != null && sql.contains("SELECT scope FROM role WHERE id = ?")),
                org.mockito.ArgumentMatchers.<Class<String>>eq(String.class),
                org.mockito.ArgumentMatchers.<Object[]>any()))
                .thenReturn("global");
        when(jdbcTemplate.query(
                argThat(sql -> sql != null && sql.contains("SELECT id") && sql.contains("node_type = 'menu'") && sql.contains("required = 1") && sql.contains("scope = ?")),
                org.mockito.ArgumentMatchers.<RowMapper<Long>>any(),
                org.mockito.ArgumentMatchers.<Object[]>any()))
                .thenReturn(List.of(homeMenuId));
        when(jdbcTemplate.query(
                argThat(sql -> sql != null && sql.contains("SELECT id") && sql.contains("parent_id IN") && sql.contains("node_type = 'button'") && sql.contains("required = 1")),
                org.mockito.ArgumentMatchers.<RowMapper<Long>>any(),
                org.mockito.ArgumentMatchers.<Object[]>any()))
                .thenReturn(List.of(homeButtonId));
        when(jdbcTemplate.query(
                argThat(sql -> sql != null && sql.contains("FROM iam_admin_role_menu_relation relation") && sql.contains("menu.node_type = 'menu'")),
                org.mockito.ArgumentMatchers.<RowMapper<Long>>any(),
                org.mockito.ArgumentMatchers.<Object[]>any()))
                .thenReturn(List.of(homeMenuId));
        when(jdbcTemplate.query(
                argThat(sql -> sql != null && sql.contains("FROM iam_admin_role_menu_relation relation") && sql.contains("menu.node_type = 'button'")),
                org.mockito.ArgumentMatchers.<RowMapper<Long>>any(),
                org.mockito.ArgumentMatchers.<Object[]>any()))
                .thenReturn(List.of(homeButtonId));

        AdminMenuService.RolePermissions permissions = service.replaceRolePermissions(roleId, List.of(), List.of());

        assertThat(permissions.menuIds()).contains(homeMenuId);
        assertThat(permissions.buttonIds()).contains(homeButtonId);
        verify(jdbcTemplate).update(
                argThat(sql -> sql != null && sql.contains("INSERT INTO iam_admin_role_menu_relation") && sql.contains("VALUES (?, ?, CURRENT_TIMESTAMP(3))")),
                org.mockito.ArgumentMatchers.eq(roleId),
                org.mockito.ArgumentMatchers.eq(homeMenuId));
        verify(jdbcTemplate).update(
                argThat(sql -> sql != null && sql.contains("INSERT INTO iam_admin_role_menu_relation") && sql.contains("VALUES (?, ?, CURRENT_TIMESTAMP(3))")),
                org.mockito.ArgumentMatchers.eq(roleId),
                org.mockito.ArgumentMatchers.eq(homeButtonId));
    }

    @Test
    void updateMenuAllowsRequiredMenu() throws Exception {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        AdminMenuService service = new AdminMenuService(jdbcTemplate, Clock.systemUTC());
        AdminMenuService.AdminMenuNode requiredHome = new AdminMenuService.AdminMenuNode(
                11L,
                "ADM0000000011",
                "menu",
                "platform",
                "平台首页",
                "/platform/home",
                "./platform/home",
                "platform.home.read",
                null,
                1,
                "HomeOutlined",
                false,
                1,
                true,
                List.of());

        when(jdbcTemplate.queryForObject(
                argThat(sql -> sql != null && sql.contains("FROM iam_admin_menu") && sql.contains("WHERE id = ?") && sql.contains("LIMIT 1")),
                org.mockito.ArgumentMatchers.<RowMapper<AdminMenuService.AdminMenuNode>>any(),
                org.mockito.ArgumentMatchers.<Object[]>any()))
                .thenReturn(requiredHome);

        when(jdbcTemplate.queryForObject(
                argThat(sql -> sql != null && sql.contains("WHERE parent_id = ?") && sql.contains("required = 1")),
                org.mockito.ArgumentMatchers.<RowMapper<AdminMenuService.AdminMenuNode>>any(),
                org.mockito.ArgumentMatchers.<Object[]>any()))
                .thenThrow(new org.springframework.dao.EmptyResultDataAccessException(1));

        AdminMenuService.AdminMenuNode updated = service.updateMenu(
                11L,
                new AdminMenuService.UpdateAdminMenuCommand(
                        null,
                        "新的首页名称",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null));

        assertThat(updated).isSameAs(requiredHome);
        verify(jdbcTemplate).update(
                argThat(sql -> sql != null && sql.contains("UPDATE iam_admin_menu")),
                org.mockito.ArgumentMatchers.eq("menu"),
                org.mockito.ArgumentMatchers.eq("platform"),
                org.mockito.ArgumentMatchers.eq("新的首页名称"),
                org.mockito.ArgumentMatchers.eq("/platform/home"),
                org.mockito.ArgumentMatchers.eq("./platform/home"),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.eq(1),
                org.mockito.ArgumentMatchers.eq("HomeOutlined"),
                org.mockito.ArgumentMatchers.eq(0),
                org.mockito.ArgumentMatchers.eq(1),
                org.mockito.ArgumentMatchers.eq(11L));
    }

    @Test
    void updateMenuRejectsRequiredNodeTypeChangeBeforeDatabaseConstraint() throws Exception {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        AdminMenuService service = new AdminMenuService(jdbcTemplate, Clock.systemUTC());
        AdminMenuService.AdminMenuNode requiredMenu = new AdminMenuService.AdminMenuNode(
                38L,
                "ADM0000000038",
                "menu",
                "platform",
                "菜单管理",
                "/platform/menu",
                "./system/menu",
                "platform.menu.read",
                null,
                20,
                "MenuOutlined",
                false,
                1,
                true,
                List.of());

        when(jdbcTemplate.queryForObject(
                argThat(sql -> sql != null && sql.contains("FROM iam_admin_menu") && sql.contains("WHERE id = ?") && sql.contains("LIMIT 1")),
                org.mockito.ArgumentMatchers.<RowMapper<AdminMenuService.AdminMenuNode>>any(),
                org.mockito.ArgumentMatchers.<Object[]>any()))
                .thenReturn(requiredMenu);

        assertThatThrownBy(() -> service.updateMenu(
                38L,
                new AdminMenuService.UpdateAdminMenuCommand(
                        "catalog",
                        "菜单管理",
                        null,
                        null,
                        null,
                        "platform",
                        null,
                        20,
                        "MenuOutlined",
                        false,
                        1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("系统必需菜单节点不允许修改类型");
    }

    @Test
    void createMenuRejectsPlatformRouteAlias() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        AdminMenuService service = new AdminMenuService(jdbcTemplate, Clock.systemUTC());

        assertThatThrownBy(() -> service.createMenu(new AdminMenuService.CreateAdminMenuCommand(
                "menu",
                "角色管理",
                "/system/roles",
                "./system/role",
                null,
                "platform",
                null,
                1,
                "TeamOutlined",
                false,
                1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("请使用规范路由路径：/platform/role");
    }

    @Test
    void createMenuRejectsCanonicalDuplicateRoutePath() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        AdminMenuService service = new AdminMenuService(jdbcTemplate, Clock.systemUTC());
        when(jdbcTemplate.queryForList(
                org.mockito.ArgumentMatchers.<String>argThat(sql -> sql != null
                        && sql.contains("SELECT route_path, scope")
                        && sql.contains("route_path IS NOT NULL"))))
                .thenReturn(List.of(Map.of("route_path", "/platform/roles", "scope", "platform")));

        assertThatThrownBy(() -> service.createMenu(new AdminMenuService.CreateAdminMenuCommand(
                "menu",
                "角色管理",
                "/platform/role",
                "./system/role",
                null,
                "platform",
                null,
                1,
                "TeamOutlined",
                false,
                1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("路由路径已被其他菜单使用");
    }

    @Test
    void updateMenuIgnoresLegacyBusinessRouteAliasWhenCheckingDuplicates() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        AdminMenuService service = new AdminMenuService(jdbcTemplate, Clock.systemUTC());
        AdminMenuService.AdminMenuNode existingMenu = new AdminMenuService.AdminMenuNode(
                38L,
                "ADM0000000038",
                "menu",
                "platform",
                "角色管理",
                "/platform/role",
                "./system/role",
                null,
                11L,
                11,
                "TeamOutlined",
                false,
                1,
                false,
                List.of());

        when(jdbcTemplate.queryForObject(
                argThat(sql -> sql != null && sql.contains("FROM iam_admin_menu") && sql.contains("WHERE id = ?") && sql.contains("LIMIT 1")),
                org.mockito.ArgumentMatchers.<RowMapper<AdminMenuService.AdminMenuNode>>any(),
                org.mockito.ArgumentMatchers.<Object[]>any()))
                .thenReturn(existingMenu, existingMenu);
        when(jdbcTemplate.queryForObject(
                argThat(sql -> sql != null && sql.contains("WHERE parent_id = ?") && sql.contains("required = 1")),
                org.mockito.ArgumentMatchers.<RowMapper<AdminMenuService.AdminMenuNode>>any(),
                org.mockito.ArgumentMatchers.<Object[]>any()))
                .thenThrow(new org.springframework.dao.EmptyResultDataAccessException(1));
        when(jdbcTemplate.queryForList(
                argThat(sql -> sql != null
                        && sql.contains("SELECT route_path, scope")
                        && sql.contains("route_path IS NOT NULL")),
                org.mockito.ArgumentMatchers.<Object[]>any()))
                .thenReturn(List.of(Map.of("route_path", "/system/roles", "scope", "business")));

        AdminMenuService.AdminMenuNode updated = service.updateMenu(
                38L,
                new AdminMenuService.UpdateAdminMenuCommand(
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null));

        assertThat(updated).isSameAs(existingMenu);
    }

    @Test
    void deleteMenuRemovesRoleBindingsAndMenuRow() throws Exception {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        AdminMenuService service = new AdminMenuService(jdbcTemplate, Clock.systemUTC());
        AdminMenuService.AdminMenuNode deletableMenu = new AdminMenuService.AdminMenuNode(
                11L,
                "ADM0000000011",
                "menu",
                "platform",
                "平台首页",
                "/platform/home",
                "./platform/home",
                "platform.home.read",
                null,
                1,
                "HomeOutlined",
                false,
                1,
                false,
                List.of());

        when(jdbcTemplate.queryForObject(
                argThat(sql -> sql != null && sql.contains("FROM iam_admin_menu") && sql.contains("WHERE id = ?") && sql.contains("LIMIT 1")),
                org.mockito.ArgumentMatchers.<RowMapper<AdminMenuService.AdminMenuNode>>any(),
                org.mockito.ArgumentMatchers.<Object[]>any()))
                .thenReturn(deletableMenu);
        when(jdbcTemplate.queryForObject(
                argThat(sql -> sql != null && sql.contains("SELECT COUNT(*) FROM iam_admin_menu WHERE parent_id = ?")),
                org.mockito.ArgumentMatchers.<Class<Integer>>eq(Integer.class),
                org.mockito.ArgumentMatchers.<Object[]>any()))
                .thenReturn(0);

        service.deleteMenu(11L);

        verify(jdbcTemplate).update("DELETE FROM iam_admin_role_menu_relation WHERE menu_id = ?", 11L);
        verify(jdbcTemplate).update("DELETE FROM iam_admin_menu WHERE id = ?", 11L);
    }
}
