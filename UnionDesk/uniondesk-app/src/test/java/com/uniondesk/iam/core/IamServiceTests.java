package com.uniondesk.iam.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.uniondesk.auth.core.UserContext;
import com.uniondesk.iam.admin.AdminMenuService;
import com.uniondesk.iam.entity.RolePo;
import com.uniondesk.iam.entity.UserAccountPo;
import com.uniondesk.iam.entity.UserSummaryPo;
import com.uniondesk.iam.mapper.RoleMapper.BusinessDomainSummary;
import com.uniondesk.iam.repository.IamRepository;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.security.crypto.password.PasswordEncoder;

class IamServiceTests {

    @Test
    void listUsersLoadsRoleCodesAndOrganizationIdsWithMysqlCompatibleDistinctOrdering() {
        IamRepository iamRepository = mock(IamRepository.class);
        OrganizationService organizationService = mock(OrganizationService.class);
        IamService service = newService(iamRepository, organizationService);

        when(iamRepository.findUsersByEmploymentStatus(false)).thenReturn(List.of(sampleUserPo()));
        when(iamRepository.findUserRoleCodes(42L)).thenReturn(List.of("super_admin"));
        when(iamRepository.findUserDomainIds(42L)).thenReturn(List.of(7L));
        when(organizationService.listUserOrganizationIds(42L)).thenReturn(List.of(8L));

        List<IamService.UserAccount> users = service.listUsers(false, null);

        assertThat(users).singleElement().satisfies(user -> {
            assertThat(user.roleCodes()).containsExactly("super_admin");
            assertThat(user.businessDomainIds()).containsExactly(7L);
            assertThat(user.organizationIds()).containsExactly(8L);
            assertThat(user.remark()).isEqualTo("核心账号");
        });
        verify(organizationService).listUserOrganizationIds(42L);
    }

    @Test
    void listUsersFallsBackToEmptyOrganizationIdsWhenLookupFails() {
        IamRepository iamRepository = mock(IamRepository.class);
        OrganizationService organizationService = mock(OrganizationService.class);
        IamService service = newService(iamRepository, organizationService);

        when(iamRepository.findUsersByEmploymentStatus(false)).thenReturn(List.of(sampleUserPo()));
        when(iamRepository.findUserRoleCodes(42L)).thenReturn(List.of("super_admin"));
        when(iamRepository.findUserDomainIds(42L)).thenReturn(List.of(7L));
        org.mockito.Mockito.doThrow(new BadSqlGrammarException(
                        "load organization links",
                        "SELECT organization_id FROM user_organization WHERE user_id = ?",
                        new java.sql.SQLException("Table 'user_organization' doesn't exist")))
                .when(organizationService)
                .listUserOrganizationIds(42L);

        List<IamService.UserAccount> users = service.listUsers(false, null);

        assertThat(users).singleElement().satisfies(user -> {
            assertThat(user.organizationIds()).isEmpty();
            assertThat(user.roleCodes()).containsExactly("super_admin");
        });
    }

    @Test
    void loadPermissionSnapshotKeepsPlatformMenusForGlobalRoles() {
        IamRepository iamRepository = mock(IamRepository.class);
        AdminMenuService adminMenuService = mock(AdminMenuService.class);
        IamService service = newService(iamRepository, mock(OrganizationService.class), adminMenuService);

        stubUserSummary(iamRepository);
        when(iamRepository.findUserRoleCodesByClientAdmin(42L)).thenReturn(List.of("super_admin", "domain_admin"));
        when(iamRepository.findRolesByCodes(anyList())).thenReturn(List.of(rolePo(1, "super_admin", "global", 1)));
        when(iamRepository.findDomainSummariesForSuperAdmin()).thenReturn(List.of(domainSummary(7L, "domain-7", "业务域7")));
        when(adminMenuService.loadPermissionSnapshot(anyList()))
                .thenReturn(snapshotWithPlatformAndBusinessMenus());

        IamService.PermissionSnapshot snapshot = service.loadPermissionSnapshot(
                new UserContext(42L, "super_admin", 1L, "sid-1", "ud-admin-web"));

        assertThat(snapshot.menuTree()).singleElement().satisfies(menu -> {
            assertThat(menu.scope()).isEqualTo("platform");
            assertThat(menu.routePath()).isEqualTo("/platform/menu");
        });
    }

    @Test
    void loadPermissionSnapshotKeepsBusinessMenusForDomainRoles() {
        IamRepository iamRepository = mock(IamRepository.class);
        AdminMenuService adminMenuService = mock(AdminMenuService.class);
        IamService service = newService(iamRepository, mock(OrganizationService.class), adminMenuService);

        stubUserSummary(iamRepository);
        when(iamRepository.findUserRoleCodesByClientAdmin(42L)).thenReturn(List.of("domain_admin"));
        when(iamRepository.findRolesByCodes(anyList())).thenReturn(List.of(rolePo(2, "domain_admin", "domain", 1)));
        when(iamRepository.findDomainSummariesForUser(eq(42L), anyList())).thenReturn(List.of(domainSummary(8L, "domain-8", "业务域8")));
        when(adminMenuService.loadPermissionSnapshot(anyList()))
                .thenReturn(snapshotWithPlatformAndBusinessMenus());

        IamService.PermissionSnapshot snapshot = service.loadPermissionSnapshot(
                new UserContext(42L, "domain_admin", 1L, "sid-1", "ud-admin-web"));

        assertThat(snapshot.menuTree()).singleElement().satisfies(menu -> {
            assertThat(menu.scope()).isEqualTo("business");
            assertThat(menu.routePath()).isEqualTo("/system/menu");
        });
    }

    @Test
    void loadPermissionSnapshotFallsBackToBusinessWhenRoleDefinitionIsMissing() {
        IamRepository iamRepository = mock(IamRepository.class);
        AdminMenuService adminMenuService = mock(AdminMenuService.class);
        IamService service = newService(iamRepository, mock(OrganizationService.class), adminMenuService);

        stubUserSummary(iamRepository);
        when(iamRepository.findUserRoleCodesByClientAdmin(42L)).thenReturn(List.of("ghost_role"));
        when(iamRepository.findRolesByCodes(anyList())).thenReturn(List.of());
        when(iamRepository.findDomainSummariesForUser(eq(42L), anyList())).thenReturn(List.of());
        when(adminMenuService.loadPermissionSnapshot(anyList()))
                .thenReturn(snapshotWithPlatformAndBusinessMenus());

        IamService.PermissionSnapshot snapshot = service.loadPermissionSnapshot(
                new UserContext(42L, "ghost_role", 1L, "sid-1", "ud-admin-web"));

        assertThat(snapshot.menuTree()).singleElement().satisfies(menu -> {
            assertThat(menu.scope()).isEqualTo("business");
            assertThat(menu.routePath()).isEqualTo("/system/menu");
        });
    }

    @Test
    void updateRoleAllowsSystemRole() {
        IamRepository iamRepository = mock(IamRepository.class);
        IamService service = newService(iamRepository, mock(OrganizationService.class));

        RolePo existing = rolePo(11, "super_admin", "global", 1);
        RolePo updated = rolePo(11, "super_admin_v2", "global", 1);
        when(iamRepository.findRoleById(11)).thenReturn(Optional.of(existing), Optional.of(updated));

        IamService.RoleView result = service.updateRole(
                11,
                new IamService.UpdateRoleCommand("super_admin_v2", "超级管理员2", "global"));

        assertThat(result.code()).isEqualTo("super_admin_v2");
        verify(iamRepository).updateRole(org.mockito.ArgumentMatchers.argThat(po ->
                po.getId() == 11
                        && "super_admin_v2".equals(po.getCode())
                        && "超级管理员2".equals(po.getName())
                        && "global".equals(po.getScope())));
    }

    @Test
    void deleteRoleStillRejectsSystemRole() {
        IamRepository iamRepository = mock(IamRepository.class);
        IamService service = newService(iamRepository, mock(OrganizationService.class));

        when(iamRepository.findRoleById(11)).thenReturn(Optional.of(rolePo(11, "super_admin", "global", 1)));

        assertThatThrownBy(() -> service.deleteRole(11))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("system role cannot be deleted");
    }

    private static IamService newService(IamRepository iamRepository, OrganizationService organizationService) {
        return newService(iamRepository, organizationService, mock(AdminMenuService.class));
    }

    private static IamService newService(
            IamRepository iamRepository,
            OrganizationService organizationService,
            AdminMenuService adminMenuService) {
        return new IamService(
                iamRepository,
                Clock.systemUTC(),
                mock(PasswordEncoder.class),
                adminMenuService,
                new PermissionScopePolicy(),
                organizationService);
    }

    private static void stubUserSummary(IamRepository iamRepository) {
        UserSummaryPo summary = new UserSummaryPo();
        summary.setId(42L);
        summary.setUsername("admin");
        summary.setMobile("13800000000");
        summary.setEmail("admin@example.com");
        when(iamRepository.findUserSummaryById(42L)).thenReturn(Optional.of(summary));
    }

    private static UserAccountPo sampleUserPo() {
        UserAccountPo po = new UserAccountPo();
        po.setId(42L);
        po.setUsername("admin");
        po.setMobile("13800000000");
        po.setEmail("admin@example.com");
        po.setRemark("核心账号");
        po.setAccountType("admin");
        po.setStatus(1);
        po.setEmploymentStatus("active");
        return po;
    }

    private static RolePo rolePo(int id, String code, String scope, int system) {
        RolePo po = new RolePo();
        po.setId(id);
        po.setCode(code);
        po.setName(code);
        po.setScope(scope);
        po.setIsSystem(system);
        return po;
    }

    private static BusinessDomainSummary domainSummary(long id, String code, String name) {
        return new BusinessDomainSummary(id, code, name);
    }

    private static AdminMenuService.PermissionSnapshotData snapshotWithPlatformAndBusinessMenus() {
        return new AdminMenuService.PermissionSnapshotData(
                List.of(
                        new AdminMenuService.AdminMenuNode(
                                1L, "ADM0000000001", "menu", "platform", "平台菜单",
                                "/platform/menu", "./platform/system/menu", "platform.menu.read",
                                null, 1, "MenuOutlined", false, 1, false, List.of()),
                        new AdminMenuService.AdminMenuNode(
                                2L, "ADM0000000002", "menu", "business", "业务菜单",
                                "/system/menu", "./system/menu", "domain.menu.read",
                                null, 2, "MenuOutlined", false, 1, false, List.of())),
                List.of());
    }
}
