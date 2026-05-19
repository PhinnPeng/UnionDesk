package com.uniondesk.iam.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.uniondesk.auth.core.UserContext;
import com.uniondesk.auth.core.UserContextHolder;
import com.uniondesk.common.web.ApiExceptionHandler;
import com.uniondesk.iam.core.OrganizationService;
import com.uniondesk.iam.core.PermissionCodes;
import com.uniondesk.iam.core.RequirePermission;
import java.util.List;
import java.time.LocalDateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class OrganizationControllerTests {

    @AfterEach
    void clearUserContext() {
        UserContextHolder.clear();
    }

    @Test
    void listOrganizationsReturnsRowsForSuperAdmin() throws Exception {
        OrganizationService organizationService = mock(OrganizationService.class);
        when(organizationService.listOrganizations()).thenReturn(List.of(
                new OrganizationService.OrganizationUnit(
                        1L,
                        "platform-root",
                        "平台组织",
                        null,
                        null,
                        1L,
                        "admin",
                        10,
                        1,
                        "平台组织根节点",
                        LocalDateTime.of(2026, 5, 15, 8, 0)),
                new OrganizationService.OrganizationUnit(
                        2L,
                        "platform-ops",
                        "平台运营部",
                        1L,
                        "平台组织",
                        1L,
                        "admin",
                        20,
                        1,
                        "负责平台账号与角色治理",
                        LocalDateTime.of(2026, 5, 15, 8, 5))));
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new OrganizationController(organizationService))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
        UserContextHolder.set(new UserContext(1L, "super_admin", null, "sid-1", "ud-admin-web"));

        mockMvc.perform(get("/api/v1/iam/organizations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("platform-root"))
                .andExpect(jsonPath("$[0].name").value("平台组织"))
                .andExpect(jsonPath("$[1].parentId").value(1))
                .andExpect(jsonPath("$[1].parentName").value("平台组织"))
                .andExpect(jsonPath("$[1].leaderName").value("admin"));
    }

    @Test
    void listOrganizationsUsesOrganizationReadPermission() throws NoSuchMethodException {
        RequirePermission requirePermission = OrganizationController.class
                .getMethod("listOrganizations")
                .getAnnotation(RequirePermission.class);

        assertThat(requirePermission.value()).containsExactly(PermissionCodes.PLATFORM_ORGANIZATION_READ);
    }
}
