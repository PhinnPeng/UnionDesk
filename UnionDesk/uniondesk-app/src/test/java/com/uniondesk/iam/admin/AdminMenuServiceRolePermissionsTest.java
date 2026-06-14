package com.uniondesk.iam.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.uniondesk.iam.repository.AdminMenuRepository;
import java.time.Clock;
import java.util.List;
import org.junit.jupiter.api.Test;

class AdminMenuServiceRolePermissionsTest {

    @Test
    void replaceRolePermissionsAcceptsCatalogInMenuIds() {
        AdminMenuRepository adminMenuRepository = mock(AdminMenuRepository.class);
        AdminMenuService service = new AdminMenuService(adminMenuRepository, org.mockito.Mockito.mock(com.uniondesk.common.event.UnionDeskEventPublisher.class), Clock.systemUTC());
        int roleId = 4;
        long catalogId = 128L;
        long buttonId = 129L;

        when(adminMenuRepository.countRoleById(roleId)).thenReturn(1);
        when(adminMenuRepository.countByIdsAndNodeTypes(List.of(catalogId), List.of("menu", "catalog"))).thenReturn(1);
        when(adminMenuRepository.countByIdsAndNodeTypes(List.of(buttonId), List.of("button"))).thenReturn(1);
        when(adminMenuRepository.findRoleScopeById(roleId)).thenReturn("global");
        when(adminMenuRepository.findRequiredMenuIds("platform")).thenReturn(List.of());
        when(adminMenuRepository.findParentIdsByMenuIds(List.of(buttonId))).thenReturn(List.of(catalogId));
        when(adminMenuRepository.findRequiredButtonIdsByParentIds(anyList())).thenReturn(List.of());
        when(adminMenuRepository.findPermissionCodesByMenuIds(anyList())).thenReturn(List.of("platform.domain.control.blocked_word.read"));
        when(adminMenuRepository.findRoleMenuIdsByNodeTypes(roleId, List.of("menu", "catalog"))).thenReturn(List.of(catalogId));
        when(adminMenuRepository.findRoleMenuIds(roleId, "button")).thenReturn(List.of(buttonId));

        AdminMenuService.RolePermissions permissions = service.replaceRolePermissions(
                roleId,
                List.of(catalogId),
                List.of(buttonId));

        assertThat(permissions.menuIds()).contains(catalogId);
        assertThat(permissions.buttonIds()).contains(buttonId);
        verify(adminMenuRepository).batchInsertRoleMenuRelations(roleId, List.of(catalogId, buttonId));
    }

    @Test
    void replaceRolePermissionsRejectsButtonInMenuIds() {
        AdminMenuRepository adminMenuRepository = mock(AdminMenuRepository.class);
        AdminMenuService service = new AdminMenuService(adminMenuRepository, org.mockito.Mockito.mock(com.uniondesk.common.event.UnionDeskEventPublisher.class), Clock.systemUTC());

        when(adminMenuRepository.countRoleById(4)).thenReturn(1);
        when(adminMenuRepository.countByIdsAndNodeTypes(List.of(129L), List.of("menu", "catalog"))).thenReturn(0);

        assertThatThrownBy(() -> service.replaceRolePermissions(4, List.of(129L), List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("存在无效的菜单节点引用");
    }
}
