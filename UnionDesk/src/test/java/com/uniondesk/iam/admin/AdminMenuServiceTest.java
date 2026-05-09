package com.uniondesk.iam.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
                org.mockito.ArgumentMatchers.<String>argThat(sql -> sql.contains("SELECT parent_id, permission_code")),
                org.mockito.ArgumentMatchers.<ResultSetExtractor<Map<Long, String>>>any()))
                .thenReturn(Map.of());
        when(jdbcTemplate.query(
                org.mockito.ArgumentMatchers.<String>argThat(sql -> sql.contains("FROM iam_admin_menu") && sql.contains("ORDER BY order_no, id")),
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
                org.mockito.ArgumentMatchers.<String>argThat(sql -> sql.contains("FROM iam_admin_role_menu_relation") && sql.contains("JOIN iam_admin_menu m")),
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
}
