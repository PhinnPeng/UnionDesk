package com.uniondesk.domain.web;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.uniondesk.domain.core.DomainRoleService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class PlatformDomainRoleControllerTests {

    private final DomainRoleService domainRoleService = mock(DomainRoleService.class);

    @Test
    void listPlatformDomainRolesReturnsArray() throws Exception {
        MockMvc mockMvc = mockMvc();
        when(domainRoleService.listRoles(1L)).thenReturn(List.of(new DomainRoleDtos.DomainRoleView(21L, 1L, "domain_admin", "业务域管理员", true)));

        mockMvc.perform(get("/api/v1/admin/domains/1/platform-roles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("domain_admin"));
        verify(domainRoleService).listRoles(1L);
    }

    @Test
    void getPlatformDomainRolePermissionsReturnsView() throws Exception {
        MockMvc mockMvc = mockMvc();
        when(domainRoleService.getRolePermissions(1L, 22L)).thenReturn(new DomainRoleDtos.DomainRolePermissionView(
                22L,
                "ops",
                "运营",
                List.of(new DomainRoleDtos.PermissionItemView(1L, "domain:config", "域配置管理", "domain", "button"))));

        mockMvc.perform(get("/api/v1/admin/domains/1/platform-roles/22/permissions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.permission_items[0].code").value("domain:config"));
        verify(domainRoleService).getRolePermissions(1L, 22L);
    }

    private MockMvc mockMvc() {
        return MockMvcBuilders.standaloneSetup(new PlatformDomainRoleController(domainRoleService)).build();
    }
}
