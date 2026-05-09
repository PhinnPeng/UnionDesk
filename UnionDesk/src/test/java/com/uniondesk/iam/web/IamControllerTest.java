package com.uniondesk.iam.web;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.uniondesk.auth.core.UserContext;
import com.uniondesk.auth.core.UserContextHolder;
import com.uniondesk.iam.admin.AdminMenuService;
import com.uniondesk.iam.core.IamService;
import java.time.Clock;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class IamControllerTest {

    private final IamService iamService = mock(IamService.class);
    private final AdminMenuService adminMenuService = mock(AdminMenuService.class);

    @AfterEach
    void clearContext() {
        UserContextHolder.clear();
    }

    @Test
    void currentPermissionSnapshotIncludesMenuScopeAndPermissionCode() throws Exception {
        MockMvc mockMvc = mockMvc();
        UserContextHolder.set(new UserContext(1L, "super_admin", null, "sid-1", "ud-admin-web"));
        when(iamService.loadPermissionSnapshot(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new IamService.PermissionSnapshot(
                        new IamService.UserSummary(1L, "admin", "13800000000", "admin@example.com"),
                        "ud-admin-web",
                        List.of("super_admin"),
                        List.of(new IamService.DomainSummary(10L, "domain-10", "业务域")),
                        List.of(new IamService.IamResource(
                                2L,
                                "menu",
                                "platform_role",
                                "平台角色",
                                "ud-admin-web",
                                "platform",
                                null,
                                "/platform/role",
                                null,
                                2,
                                "TeamOutlined",
                                "./system/role",
                                false,
                                1,
                                "platform.menu.read")),
                        List.of(),
                        Clock.systemUTC().instant().toString()));

        mockMvc.perform(get("/api/v1/iam/me/permission-snapshot"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.menus[0].scope").value("platform"))
                .andExpect(jsonPath("$.menus[0].component").value("./system/role"))
                .andExpect(jsonPath("$.menus[0].permissionCode").value("platform.menu.read"));
    }

    private MockMvc mockMvc() {
        return MockMvcBuilders.standaloneSetup(new IamController(iamService, adminMenuService))
                .build();
    }
}
