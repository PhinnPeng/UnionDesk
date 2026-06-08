package com.uniondesk.domain.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.uniondesk.common.web.ApiExceptionHandler;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class DomainRoleControllerTests {

    private final com.uniondesk.domain.core.DomainRoleService domainRoleService = mock(com.uniondesk.domain.core.DomainRoleService.class);

    @Test
    void listRolesReturnsArray() throws Exception {
        MockMvc mockMvc = mockMvc();
        when(domainRoleService.listRoles(1L)).thenReturn(List.of(new DomainRoleDtos.DomainRoleView(21L, 1L, "domain_admin", "业务域管理员", true)));

        mockMvc.perform(get("/api/v1/admin/domains/1/roles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("domain_admin"));
    }

    @Test
    void createRoleReturnsView() throws Exception {
        MockMvc mockMvc = mockMvc();
        when(domainRoleService.createRole(eq(1L), any())).thenReturn(new DomainRoleDtos.DomainRoleView(22L, 1L, "ops", "运营", false));

        mockMvc.perform(post("/api/v1/admin/domains/1/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "ops",
                                  "name": "运营"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("ops"));
        verify(domainRoleService).createRole(eq(1L), any());
    }

    @Test
    void updateRoleReturnsView() throws Exception {
        MockMvc mockMvc = mockMvc();
        when(domainRoleService.updateRole(eq(1L), eq(22L), any())).thenReturn(new DomainRoleDtos.DomainRoleView(22L, 1L, "ops", "运营升级", false));

        mockMvc.perform(put("/api/v1/admin/domains/1/roles/22")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "运营升级"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("运营升级"));
        verify(domainRoleService).updateRole(eq(1L), eq(22L), any());
    }

    @Test
    void getRolePermissionsReturnsView() throws Exception {
        MockMvc mockMvc = mockMvc();
        when(domainRoleService.getRolePermissions(1L, 22L)).thenReturn(new DomainRoleDtos.DomainRolePermissionView(
                22L,
                "ops",
                "运营",
                List.of(new DomainRoleDtos.PermissionItemView(1L, "domain:config", "域配置管理", "domain", "button"))));

        mockMvc.perform(get("/api/v1/admin/domains/1/roles/22/permissions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.permission_items[0].code").value("domain:config"));
    }

    @Test
    void updateRolePermissionsReturnsView() throws Exception {
        MockMvc mockMvc = mockMvc();
        when(domainRoleService.updateRolePermissions(eq(1L), eq(22L), any())).thenReturn(new DomainRoleDtos.DomainRolePermissionView(
                22L,
                "ops",
                "运营",
                List.of(new DomainRoleDtos.PermissionItemView(1L, "domain:config", "域配置管理", "domain", "button"))));

        mockMvc.perform(put("/api/v1/admin/domains/1/roles/22/permissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "permission_item_ids": [1]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.permission_items[0].id").value(1));
        verify(domainRoleService).updateRolePermissions(eq(1L), eq(22L), any());
    }

    @Test
    void deleteRoleReturnsNoContent() throws Exception {
        MockMvc mockMvc = mockMvc();

        mockMvc.perform(delete("/api/v1/admin/domains/1/roles/22"))
                .andExpect(status().isNoContent());
        verify(domainRoleService).deleteRole(1L, 22L);
    }

    @Test
    void deletePresetRoleReturnsBadRequest() throws Exception {
        MockMvc mockMvc = mockMvcWithAdvice();
        org.mockito.Mockito.doThrow(new IllegalArgumentException("preset role cannot be deleted"))
                .when(domainRoleService)
                .deleteRole(1L, 11L);

        mockMvc.perform(delete("/api/v1/admin/domains/1/roles/11"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("40002"));
    }

    @Test
    void listPermissionItemsReturnsArray() throws Exception {
        MockMvc mockMvc = mockMvc();
        when(domainRoleService.listPermissionItems(1L)).thenReturn(List.of(new DomainRoleDtos.PermissionItemView(
                1L, "domain:config", "域配置管理", "domain", "button")));

        mockMvc.perform(get("/api/v1/admin/domains/1/permission-items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("domain:config"));
    }

    private MockMvc mockMvc() {
        return MockMvcBuilders.standaloneSetup(new DomainRoleController(domainRoleService)).build();
    }

    private MockMvc mockMvcWithAdvice() {
        return MockMvcBuilders.standaloneSetup(new DomainRoleController(domainRoleService))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }
}
