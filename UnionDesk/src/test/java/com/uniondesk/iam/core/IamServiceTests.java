package com.uniondesk.iam.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.uniondesk.auth.core.UserContext;
import com.uniondesk.iam.admin.AdminMenuService;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.crypto.password.PasswordEncoder;

class IamServiceTests {

    @Test
	void listUsersLoadsRoleCodesAndOrganizationIdsWithMysqlCompatibleDistinctOrdering() throws Exception {
		JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
		OrganizationService organizationService = mock(OrganizationService.class);
		IamService service = new IamService(
				jdbcTemplate,
				Clock.systemUTC(),
				mock(PasswordEncoder.class),
				mock(AdminMenuService.class),
				new PermissionScopePolicy(),
				organizationService);

        when(jdbcTemplate.query(
                org.mockito.ArgumentMatchers.<String>argThat(sql -> sql.contains("FROM user_account")),
                org.mockito.ArgumentMatchers.<RowMapper<IamService.UserAccount>>any()))
                .thenAnswer(invocation -> {
                    RowMapper<IamService.UserAccount> mapper = invocation.getArgument(1);
                    ResultSet rs = mock(ResultSet.class);
                    when(rs.getLong("id")).thenReturn(42L);
                    when(rs.getString("username")).thenReturn("admin");
                    when(rs.getString("mobile")).thenReturn("13800000000");
                    when(rs.getString("email")).thenReturn("admin@example.com");
                    when(rs.getString("remark")).thenReturn("核心账号");
                    when(rs.getString("account_type")).thenReturn("admin");
                    when(rs.getInt("status")).thenReturn(1);
                    when(rs.getString("employment_status")).thenReturn("active");
                    when(rs.getTimestamp("offboarded_at")).thenReturn(null);
                    when(rs.getObject("offboarded_by", Long.class)).thenReturn(null);
                    when(rs.getString("offboard_reason")).thenReturn(null);
                    return List.of(mapper.mapRow(rs, 0));
                });
        when(jdbcTemplate.query(
                org.mockito.ArgumentMatchers.<String>argThat(sql -> sql.contains("SELECT DISTINCT")
                        && sql.contains("role_order")
                        && sql.contains("ORDER BY role_order")),
                org.mockito.ArgumentMatchers.<RowMapper<String>>any(),
                eq(42L),
                eq(42L)))
                .thenAnswer(invocation -> {
                    RowMapper<String> mapper = invocation.getArgument(1);
                    ResultSet rs = mock(ResultSet.class);
                    when(rs.getString("code")).thenReturn("super_admin");
                    when(rs.getString("role_code")).thenReturn("super_admin");
                    return new ArrayList<>(List.of(mapper.mapRow(rs, 0)));
                });
        when(jdbcTemplate.queryForList(anyString(), eq(Long.class), eq(42L))).thenReturn(List.of(7L));
        when(organizationService.listUserOrganizationIds(42L)).thenReturn(List.of(8L));

        List<IamService.UserAccount> users = service.listUsers(false, null);

        assertThat(users).singleElement().satisfies(user -> {
            assertThat(user.roleCodes()).containsExactly("super_admin");
            assertThat(user.businessDomainIds()).containsExactly(7L);
            assertThat(user.organizationIds()).containsExactly(8L);
            assertThat(user.remark()).isEqualTo("核心账号");
        });
        verify(jdbcTemplate).query(
                org.mockito.ArgumentMatchers.<String>argThat(sql -> sql.contains("SELECT DISTINCT")
                        && sql.contains("role_order")
                        && sql.contains("ORDER BY role_order")),
                org.mockito.ArgumentMatchers.<RowMapper<String>>any(),
                eq(42L),
                eq(42L));
        verify(organizationService).listUserOrganizationIds(42L);
    }

    @Test
    void listUsersFallsBackToEmptyOrganizationIdsWhenLookupFails() throws Exception {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        OrganizationService organizationService = mock(OrganizationService.class);
        IamService service = new IamService(
                jdbcTemplate,
                Clock.systemUTC(),
                mock(PasswordEncoder.class),
                mock(AdminMenuService.class),
                new PermissionScopePolicy(),
                organizationService);

        when(jdbcTemplate.query(
                org.mockito.ArgumentMatchers.<String>argThat(sql -> sql.contains("FROM user_account")),
                org.mockito.ArgumentMatchers.<RowMapper<IamService.UserAccount>>any()))
                .thenAnswer(invocation -> {
                    RowMapper<IamService.UserAccount> mapper = invocation.getArgument(1);
                    ResultSet rs = mock(ResultSet.class);
                    when(rs.getLong("id")).thenReturn(42L);
                    when(rs.getString("username")).thenReturn("admin");
                    when(rs.getString("mobile")).thenReturn("13800000000");
                    when(rs.getString("email")).thenReturn("admin@example.com");
                    when(rs.getString("remark")).thenReturn("鏍稿績璐﹀彿");
                    when(rs.getString("account_type")).thenReturn("admin");
                    when(rs.getInt("status")).thenReturn(1);
                    when(rs.getString("employment_status")).thenReturn("active");
                    when(rs.getTimestamp("offboarded_at")).thenReturn(null);
                    when(rs.getObject("offboarded_by", Long.class)).thenReturn(null);
                    when(rs.getString("offboard_reason")).thenReturn(null);
                    return List.of(mapper.mapRow(rs, 0));
                });
        when(jdbcTemplate.query(
                org.mockito.ArgumentMatchers.<String>argThat(sql -> sql.contains("SELECT DISTINCT")
                        && sql.contains("role_order")
                        && sql.contains("ORDER BY role_order")),
                org.mockito.ArgumentMatchers.<RowMapper<String>>any(),
                eq(42L),
                eq(42L)))
                .thenAnswer(invocation -> {
                    RowMapper<String> mapper = invocation.getArgument(1);
                    ResultSet rs = mock(ResultSet.class);
                    when(rs.getString("code")).thenReturn("super_admin");
                    when(rs.getString("role_code")).thenReturn("super_admin");
                    return new ArrayList<>(List.of(mapper.mapRow(rs, 0)));
                });
        when(jdbcTemplate.queryForList(anyString(), eq(Long.class), eq(42L))).thenReturn(List.of(7L));
        org.mockito.Mockito.doThrow(new BadSqlGrammarException(
                        "load organization links",
                        "SELECT organization_id FROM user_organization WHERE user_id = ?",
                        new SQLException("Table 'user_organization' doesn't exist")))
                .when(organizationService)
                .listUserOrganizationIds(42L);

        List<IamService.UserAccount> users = service.listUsers(false, null);

        assertThat(users).singleElement().satisfies(user -> {
            assertThat(user.organizationIds()).isEmpty();
            assertThat(user.roleCodes()).containsExactly("super_admin");
        });
    }

    @Test
    void loadPermissionSnapshotKeepsPlatformMenusForGlobalRoles() throws Exception {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        AdminMenuService adminMenuService = mock(AdminMenuService.class);
        IamService service = new IamService(
                jdbcTemplate,
                Clock.systemUTC(),
                mock(PasswordEncoder.class),
                adminMenuService,
                new PermissionScopePolicy(),
                mock(OrganizationService.class));

        when(jdbcTemplate.query(
                org.mockito.ArgumentMatchers.<String>argThat(sql -> sql != null && sql.contains("SELECT DISTINCT r.code AS role_code")),
                org.mockito.ArgumentMatchers.<RowMapper<String>>any(),
                eq(42L),
                eq(42L)))
                .thenReturn(new ArrayList<>(List.of("super_admin", "domain_admin")));
        when(jdbcTemplate.query(
                org.mockito.ArgumentMatchers.<String>argThat(sql -> sql != null && sql.contains("FROM role") && sql.contains("WHERE code IN")),
                org.mockito.ArgumentMatchers.<RowMapper<Object>>any(),
                org.mockito.ArgumentMatchers.<Object[]>any()))
                .thenAnswer(invocation -> {
                    RowMapper<?> mapper = invocation.getArgument(1);
                    ResultSet rs = mock(ResultSet.class);
                    when(rs.getInt("id")).thenReturn(1);
                    when(rs.getString("code")).thenReturn("super_admin");
                    when(rs.getString("scope")).thenReturn("global");
                    return List.of(mapper.mapRow(rs, 0));
                });
        when(jdbcTemplate.query(
                org.mockito.ArgumentMatchers.<String>argThat(sql -> sql.contains("SELECT id, code, name FROM business_domain")),
                org.mockito.ArgumentMatchers.<RowMapper<IamService.DomainSummary>>any()))
                .thenReturn(List.of(new IamService.DomainSummary(7L, "domain-7", "业务域7")));
        when(adminMenuService.loadPermissionSnapshot(org.mockito.ArgumentMatchers.anyList()))
                .thenReturn(new AdminMenuService.PermissionSnapshotData(
                        List.of(
                                new AdminMenuService.AdminMenuNode(
                                        1L,
                                        "ADM0000000001",
                                        "menu",
                                        "platform",
                                        "平台菜单",
                                        "/platform/menu",
                                        "./platform/system/menu",
                                        "platform.menu.read",
                                        null,
                                        1,
                                        "MenuOutlined",
                                        false,
                                        1,
                                        false,
                                        List.of()),
                                new AdminMenuService.AdminMenuNode(
                                        2L,
                                        "ADM0000000002",
                                        "menu",
                                        "business",
                                        "业务菜单",
                                        "/system/menu",
                                        "./system/menu",
                                        "domain.menu.read",
                                        null,
                                        2,
                                        "MenuOutlined",
                                        false,
                                        1,
                                        false,
                                        List.of())),
                        List.of()));

        IamService.PermissionSnapshot snapshot = service.loadPermissionSnapshot(
                new UserContext(42L, "super_admin", 1L, "sid-1", "ud-admin-web"));

        assertThat(snapshot.menuTree()).singleElement().satisfies(menu -> {
            assertThat(menu.scope()).isEqualTo("platform");
            assertThat(menu.routePath()).isEqualTo("/platform/menu");
        });
    }

    @Test
    void loadPermissionSnapshotKeepsBusinessMenusForDomainRoles() throws Exception {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        AdminMenuService adminMenuService = mock(AdminMenuService.class);
        IamService service = new IamService(
                jdbcTemplate,
                Clock.systemUTC(),
                mock(PasswordEncoder.class),
                adminMenuService,
                new PermissionScopePolicy(),
                mock(OrganizationService.class));

        when(jdbcTemplate.query(
                org.mockito.ArgumentMatchers.<String>argThat(sql -> sql != null && sql.contains("SELECT DISTINCT r.code AS role_code")),
                org.mockito.ArgumentMatchers.<RowMapper<String>>any(),
                eq(42L),
                eq(42L)))
                .thenReturn(new ArrayList<>(List.of("domain_admin")));
        when(jdbcTemplate.query(
                org.mockito.ArgumentMatchers.<String>argThat(sql -> sql != null && sql.contains("FROM role") && sql.contains("WHERE code IN")),
                org.mockito.ArgumentMatchers.<RowMapper<Object>>any(),
                org.mockito.ArgumentMatchers.<Object[]>any()))
                .thenAnswer(invocation -> {
                    RowMapper<?> mapper = invocation.getArgument(1);
                    ResultSet rs = mock(ResultSet.class);
                    when(rs.getInt("id")).thenReturn(2);
                    when(rs.getString("code")).thenReturn("domain_admin");
                    when(rs.getString("scope")).thenReturn("domain");
                    return List.of(mapper.mapRow(rs, 0));
                });
        when(jdbcTemplate.query(
                org.mockito.ArgumentMatchers.<String>argThat(sql -> sql != null && sql.contains("FROM user_domain_role udr")),
                org.mockito.ArgumentMatchers.<RowMapper<IamService.DomainSummary>>any(),
                org.mockito.ArgumentMatchers.<Object[]>any()))
                .thenReturn(List.of(new IamService.DomainSummary(8L, "domain-8", "业务域8")));
        when(adminMenuService.loadPermissionSnapshot(org.mockito.ArgumentMatchers.anyList()))
                .thenReturn(new AdminMenuService.PermissionSnapshotData(
                        List.of(
                                new AdminMenuService.AdminMenuNode(
                                        1L,
                                        "ADM0000000001",
                                        "menu",
                                        "platform",
                                        "平台菜单",
                                        "/platform/menu",
                                        "./platform/system/menu",
                                        "platform.menu.read",
                                        null,
                                        1,
                                        "MenuOutlined",
                                        false,
                                        1,
                                        false,
                                        List.of()),
                                new AdminMenuService.AdminMenuNode(
                                        2L,
                                        "ADM0000000002",
                                        "menu",
                                        "business",
                                        "业务菜单",
                                        "/system/menu",
                                        "./system/menu",
                                        "domain.menu.read",
                                        null,
                                        2,
                                        "MenuOutlined",
                                        false,
                                        1,
                                        false,
                                        List.of())),
                        List.of()));

        IamService.PermissionSnapshot snapshot = service.loadPermissionSnapshot(
                new UserContext(42L, "domain_admin", 1L, "sid-1", "ud-admin-web"));

        assertThat(snapshot.menuTree()).singleElement().satisfies(menu -> {
            assertThat(menu.scope()).isEqualTo("business");
            assertThat(menu.routePath()).isEqualTo("/system/menu");
        });
    }

    @Test
    void loadPermissionSnapshotFallsBackToBusinessWhenRoleDefinitionIsMissing() throws Exception {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        AdminMenuService adminMenuService = mock(AdminMenuService.class);
        IamService service = new IamService(
                jdbcTemplate,
                Clock.systemUTC(),
                mock(PasswordEncoder.class),
                adminMenuService,
                new PermissionScopePolicy(),
                mock(OrganizationService.class));

        when(jdbcTemplate.query(
                org.mockito.ArgumentMatchers.<String>argThat(sql -> sql != null && sql.contains("SELECT DISTINCT r.code AS role_code")),
                org.mockito.ArgumentMatchers.<RowMapper<String>>any(),
                eq(42L),
                eq(42L)))
                .thenReturn(new ArrayList<>(List.of("ghost_role")));
        when(jdbcTemplate.query(
                org.mockito.ArgumentMatchers.<String>argThat(sql -> sql != null && sql.contains("FROM role") && sql.contains("WHERE code IN")),
                org.mockito.ArgumentMatchers.<RowMapper<Object>>any(),
                org.mockito.ArgumentMatchers.<Object[]>any()))
                .thenReturn(List.of());
        when(jdbcTemplate.query(
                org.mockito.ArgumentMatchers.<String>argThat(sql -> sql != null && sql.contains("SELECT id, code, name FROM business_domain")),
                org.mockito.ArgumentMatchers.<RowMapper<IamService.DomainSummary>>any()))
                .thenReturn(List.of());
        when(adminMenuService.loadPermissionSnapshot(org.mockito.ArgumentMatchers.anyList()))
                .thenReturn(new AdminMenuService.PermissionSnapshotData(
                        List.of(
                                new AdminMenuService.AdminMenuNode(
                                        1L,
                                        "ADM0000000001",
                                        "menu",
                                        "platform",
                                        "平台菜单",
                                        "/platform/menu",
                                        "./platform/system/menu",
                                        "platform.menu.read",
                                        null,
                                        1,
                                        "MenuOutlined",
                                        false,
                                        1,
                                        false,
                                        List.of()),
                                new AdminMenuService.AdminMenuNode(
                                        2L,
                                        "ADM0000000002",
                                        "menu",
                                        "business",
                                        "业务菜单",
                                        "/system/menu",
                                        "./system/menu",
                                        "domain.menu.read",
                                        null,
                                        2,
                                        "MenuOutlined",
                                        false,
                                        1,
                                        false,
                                        List.of())),
                        List.of()));

        IamService.PermissionSnapshot snapshot = service.loadPermissionSnapshot(
                new UserContext(42L, "ghost_role", 1L, "sid-1", "ud-admin-web"));

        assertThat(snapshot.menuTree()).singleElement().satisfies(menu -> {
            assertThat(menu.scope()).isEqualTo("business");
            assertThat(menu.routePath()).isEqualTo("/system/menu");
        });
    }

    @Test
    void updateRoleAllowsSystemRole() throws Exception {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        IamService service = new IamService(
                jdbcTemplate,
                Clock.systemUTC(),
                mock(PasswordEncoder.class),
                mock(AdminMenuService.class),
                new PermissionScopePolicy(),
                mock(OrganizationService.class));

        IamService.RoleView existing = new IamService.RoleView(11, "super_admin", "超级管理员", "global", true);
        IamService.RoleView updated = new IamService.RoleView(11, "super_admin_v2", "超级管理员2", "global", true);

        when(jdbcTemplate.queryForObject(
                argThat(sql -> sql != null && sql.contains("FROM role") && sql.contains("WHERE id = ?") && sql.contains("LIMIT 1")),
                org.mockito.ArgumentMatchers.<RowMapper<IamService.RoleView>>any(),
                org.mockito.ArgumentMatchers.<Object[]>any()))
                .thenReturn(existing, updated);

        IamService.RoleView result = service.updateRole(
                11,
                new IamService.UpdateRoleCommand("super_admin_v2", "超级管理员2", "global"));

        assertThat(result).isEqualTo(updated);
        verify(jdbcTemplate).update(
                argThat(sql -> sql != null && sql.contains("UPDATE role") && sql.contains("SET code = ?") && sql.contains("name = ?") && sql.contains("scope = ?")),
                eq("super_admin_v2"),
                eq("超级管理员2"),
                eq("global"),
                eq(11));
    }

    @Test
    void deleteRoleStillRejectsSystemRole() throws Exception {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        IamService service = new IamService(
                jdbcTemplate,
                Clock.systemUTC(),
                mock(PasswordEncoder.class),
                mock(AdminMenuService.class),
                new PermissionScopePolicy(),
                mock(OrganizationService.class));

        when(jdbcTemplate.queryForObject(
                argThat(sql -> sql != null && sql.contains("FROM role") && sql.contains("WHERE id = ?") && sql.contains("LIMIT 1")),
                org.mockito.ArgumentMatchers.<RowMapper<IamService.RoleView>>any(),
                org.mockito.ArgumentMatchers.<Object[]>any()))
                .thenReturn(new IamService.RoleView(11, "super_admin", "超级管理员", "global", true));

        assertThatThrownBy(() -> service.deleteRole(11))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("system role cannot be deleted");
    }
}
