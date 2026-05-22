package com.uniondesk.iam.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.uniondesk.auth.core.AuthVersionService;
import com.uniondesk.auth.core.UserContext;
import com.uniondesk.auth.core.UserContextHolder;
import com.uniondesk.common.web.ApiExceptionHandler;
import com.uniondesk.iam.core.IamService;
import com.uniondesk.iam.core.PlatformRoleService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class StaffControllerTests {

    private final IamService iamService = mock(IamService.class);
    private final PlatformRoleService platformRoleService = mock(PlatformRoleService.class);
    private final AuthVersionService authVersionService = mock(AuthVersionService.class);

    @AfterEach
    void clearContext() {
        UserContextHolder.clear();
    }

    // P1: StaffController.listStaff 当前为内存分页，数据量 >1000 时需迁移为 SQL 分页
    @Test
    void listStaffReturnsRowsForPlatformAdmin() throws Exception {
        MockMvc mockMvc = mockMvc();
        UserContextHolder.set(new UserContext(2L, "super_admin", 10L, "sid-1", "ud-admin-web"));
        when(platformRoleService.getCurrentPlatformRoles(2L)).thenReturn(List.of("platform_admin"));
        when(iamService.listUsers(false, null)).thenReturn(List.of(staffAccount(1L, "admin", 1, "active")));
        when(iamService.listUsers(true, null)).thenReturn(List.of());
        when(platformRoleService.getCurrentPlatformRoles(1L)).thenReturn(List.of("domain_admin"));

        mockMvc.perform(get("/api/v1/admin/staff"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.list[0].loginName").value("admin"))
                .andExpect(jsonPath("$.list[0].employmentStatus").value("active"))
                .andExpect(jsonPath("$.list[0].platformRoles[0]").value("domain_admin"));
    }

    @Test
    void updateStaffStatusToDisabledSucceeds() throws Exception {
        MockMvc mockMvc = mockMvc();
        UserContextHolder.set(new UserContext(2L, "super_admin", 10L, "sid-1", "ud-admin-web"));
        when(platformRoleService.getCurrentPlatformRoles(2L)).thenReturn(List.of("platform_admin"));
        when(iamService.offboardUser(1L, 2L, "status_update"))
                .thenReturn(staffAccount(1L, "admin", 0, "disabled"));
        when(platformRoleService.getCurrentPlatformRoles(1L)).thenReturn(List.of("domain_admin"));

        mockMvc.perform(put("/api/v1/admin/staff/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "disabled"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(0))
                .andExpect(jsonPath("$.employmentStatus").value("disabled"));
        verify(authVersionService).incrementVersion(1L, "admin");
    }

    @Test
    void updateStaffStatusToActiveSucceeds() throws Exception {
        MockMvc mockMvc = mockMvc();
        when(iamService.restoreUser(1L)).thenReturn(staffAccount(1L, "admin", 1, "active"));
        when(platformRoleService.getCurrentPlatformRoles(1L)).thenReturn(List.of("domain_admin"));

        mockMvc.perform(put("/api/v1/admin/staff/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "active"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(1))
                .andExpect(jsonPath("$.employmentStatus").value("active"));
        verify(authVersionService).incrementVersion(1L, "admin");
    }

    @Test
    void createStaffReturnsCreatedStaff() throws Exception {
        MockMvc mockMvc = mockMvc();
        UserContextHolder.set(new UserContext(2L, "super_admin", 10L, "sid-1", "ud-admin-web"));
        when(platformRoleService.getCurrentPlatformRoles(2L)).thenReturn(List.of("platform_admin"));
        when(iamService.createUser(any())).thenReturn(staffAccount(3L, "new-staff", 1, "active"));
        when(platformRoleService.getCurrentPlatformRoles(3L)).thenReturn(List.of());

        mockMvc.perform(post("/api/v1/admin/staff")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "loginName": "new-staff",
                                  "phone": "13800000003",
                                  "email": "new-staff@uniondesk.local",
                                  "password": "password123",
                                  "accountType": "admin",
                                  "roleCodes": ["platform_admin"],
                                  "businessDomainIds": []
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.loginName").value("new-staff"))
                .andExpect(jsonPath("$.status").value(1));
        verify(iamService).createUser(argThat(command -> "new-staff".equals(command.username())
                && "admin".equals(command.accountType())));
    }

    @Test
    void getStaffReturnsDetailRow() throws Exception {
        MockMvc mockMvc = mockMvc();
        UserContextHolder.set(new UserContext(2L, "super_admin", 10L, "sid-1", "ud-admin-web"));
        when(platformRoleService.getCurrentPlatformRoles(2L)).thenReturn(List.of("platform_admin"));
        when(iamService.loadUser(1L)).thenReturn(Optional.of(staffAccount(1L, "admin", 1, "active")));
        when(platformRoleService.getCurrentPlatformRoles(1L)).thenReturn(List.of("platform_admin"));

        mockMvc.perform(get("/api/v1/admin/staff/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loginName").value("admin"))
                .andExpect(jsonPath("$.platformRoles[0]").value("platform_admin"));
    }

    @Test
    void updateStaffReturnsUpdatedRowAndInvalidatesVersion() throws Exception {
        MockMvc mockMvc = mockMvc();
        UserContextHolder.set(new UserContext(2L, "super_admin", 10L, "sid-1", "ud-admin-web"));
        when(platformRoleService.getCurrentPlatformRoles(2L)).thenReturn(List.of("platform_admin"));
        when(iamService.updateUser(eq(1L), any())).thenReturn(staffAccount(1L, "admin-updated", 1, "active"));
        when(iamService.listUsers(false, null)).thenReturn(List.of(staffAccount(1L, "admin", 1, "active")));
        when(iamService.listUsers(true, null)).thenReturn(List.of());
        when(platformRoleService.getCurrentPlatformRoles(1L)).thenReturn(List.of("platform_admin"));

        mockMvc.perform(put("/api/v1/admin/staff/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "loginName": "admin-updated",
                                  "phone": "13800000001",
                                  "email": "admin-updated@uniondesk.local",
                                  "password": "new-password",
                                  "accountType": "admin",
                                  "roleCodes": ["super_admin"],
                                  "businessDomainIds": [],
                                  "status": 1
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loginName").value("admin-updated"));
        verify(authVersionService).incrementVersion(1L, "admin");
    }

    @Test
    void disableStaffReturnsUpdatedRow() throws Exception {
        MockMvc mockMvc = mockMvc();
        UserContextHolder.set(new UserContext(2L, "super_admin", 10L, "sid-1", "ud-admin-web"));
        when(platformRoleService.getCurrentPlatformRoles(2L)).thenReturn(List.of("platform_admin"));
        when(iamService.offboardUser(1L, 2L, "manual")).thenReturn(staffAccount(1L, "admin", 0, "offboarded"));
        when(iamService.listUsers(false, null)).thenReturn(List.of());
        when(iamService.listUsers(true, null)).thenReturn(List.of(staffAccount(1L, "admin", 0, "offboarded")));
        when(platformRoleService.getCurrentPlatformRoles(1L)).thenReturn(List.of("platform_admin"));

        mockMvc.perform(post("/api/v1/admin/staff/1/disable").param("reason", "manual"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employmentStatus").value("offboarded"));
        verify(authVersionService).incrementVersion(1L, "admin");
    }

    @Test
    void updatePlatformRolesRequiresStepUpToken() throws Exception {
        MockMvc mockMvc = mockMvc();
        UserContextHolder.set(new UserContext(2L, "super_admin", 10L, "sid-1", "ud-admin-web"));
        when(platformRoleService.getCurrentPlatformRoles(2L)).thenReturn(List.of("platform_admin"));

        mockMvc.perform(put("/api/v1/admin/staff/1/platform-roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "roleCodes": ["platform_admin"]
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("40301"))
                .andExpect(jsonPath("$.message").value("无操作权限"));
    }

    @Test
    void updatePlatformRolesRejectsInvalidRoleSet() throws Exception {
        MockMvc mockMvc = mockMvc();
        UserContextHolder.set(new UserContext(2L, "super_admin", 10L, "sid-1", "ud-admin-web"));
        when(platformRoleService.getCurrentPlatformRoles(2L)).thenReturn(List.of("platform_admin"));
        when(iamService.loadUser(1L)).thenReturn(Optional.of(staffAccount(1L, "admin", 1, "active")));
        when(platformRoleService.getCurrentPlatformRoles(1L)).thenReturn(List.of("platform_admin"));
        org.mockito.Mockito.doThrow(new IllegalStateException("role invalid"))
                .when(platformRoleService)
                .assignPlatformRoles(1L, List.of("platform_admin"));

        mockMvc.perform(put("/api/v1/admin/staff/1/platform-roles")
                        .header("X-UD-Step-Up-Token", "step-up-token-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "roleCodes": ["platform_admin"]
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    private MockMvc mockMvc() {
        return MockMvcBuilders.standaloneSetup(new StaffController(iamService, platformRoleService, authVersionService))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    private IamService.UserAccount staffAccount(long id, String loginName, int status, String employmentStatus) {
        return new IamService.UserAccount(
                id,
                loginName,
                "Staff " + loginName,
                "138000000" + id,
                loginName + "@uniondesk.local",
                null,
                "admin",
                status,
                employmentStatus,
                null,
                null,
                null,
                List.of("super_admin"),
                List.of(10L),
                List.of());
    }
}
