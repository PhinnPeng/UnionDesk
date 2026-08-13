package com.uniondesk.iam.web;

import static org.mockito.ArgumentMatchers.any;
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
import com.uniondesk.auth.core.LoginSessionService;
import com.uniondesk.auth.core.UserContext;
import com.uniondesk.auth.core.UserContextHolder;
import com.uniondesk.common.web.ApiExceptionHandler;
import com.uniondesk.iam.core.OrganizationService;
import com.uniondesk.iam.core.PlatformRoleService;
import com.uniondesk.iam.core.StaffAccountService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class StaffControllerTests {

    private final StaffAccountService staffAccountService = mock(StaffAccountService.class);
    private final PlatformRoleService platformRoleService = mock(PlatformRoleService.class);
    private final OrganizationService organizationService = mock(OrganizationService.class);
    private final AuthVersionService authVersionService = mock(AuthVersionService.class);
    private final LoginSessionService loginSessionService = mock(LoginSessionService.class);

    @AfterEach
    void clearContext() {
        UserContextHolder.clear();
    }

    @Test
    void listStaffReturnsRowsForPlatformAdmin() throws Exception {
        MockMvc mockMvc = mockMvc();
        UserContextHolder.set(new UserContext(2L, "super_admin", 10L, "sid-1", "ud-admin-web"));
        when(platformRoleService.getCurrentPlatformRoles(2L)).thenReturn(List.of("platform_admin"));
        when(staffAccountService.listAll()).thenReturn(List.of(staffAccount(1L, "admin", "active")));
        when(staffAccountService.listDomainRoleCodes(1L)).thenReturn(List.of("domain_admin"));
        when(staffAccountService.listBusinessDomainIds(1L)).thenReturn(List.of(10L));
        when(staffAccountService.listOrganizationIds(1L)).thenReturn(List.of());
        when(platformRoleService.getCurrentPlatformRoles(1L)).thenReturn(List.of("platform_admin"));

        mockMvc.perform(get("/api/v1/admin/staff"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.list[0].username").value("admin"))
                .andExpect(jsonPath("$.list[0].employmentStatus").value("active"))
                .andExpect(jsonPath("$.list[0].platformRoles[0]").value("platform_admin"));
    }

    @Test
    void updateStaffStatusToDisabledSucceeds() throws Exception {
        MockMvc mockMvc = mockMvc();
        UserContextHolder.set(new UserContext(2L, "super_admin", 10L, "sid-1", "ud-admin-web"));
        when(platformRoleService.getCurrentPlatformRoles(2L)).thenReturn(List.of("platform_admin"));
        when(staffAccountService.disable(1L)).thenReturn(staffAccount(1L, "admin", "disabled"));
        when(staffAccountService.listDomainRoleCodes(1L)).thenReturn(List.of("domain_admin"));
        when(staffAccountService.listBusinessDomainIds(1L)).thenReturn(List.of());
        when(staffAccountService.listOrganizationIds(1L)).thenReturn(List.of());
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
        verify(authVersionService).incrementVersion(1L, "staff");
    }

    @Test
    void updateStaffStatusToActiveSucceeds() throws Exception {
        MockMvc mockMvc = mockMvc();
        when(staffAccountService.restore(1L)).thenReturn(staffAccount(1L, "admin", "active"));
        when(staffAccountService.listDomainRoleCodes(1L)).thenReturn(List.of("domain_admin"));
        when(staffAccountService.listBusinessDomainIds(1L)).thenReturn(List.of());
        when(staffAccountService.listOrganizationIds(1L)).thenReturn(List.of());
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
        verify(authVersionService).incrementVersion(1L, "staff");
    }

    @Test
    void createStaffReturnsCreatedStaff() throws Exception {
        MockMvc mockMvc = mockMvc();
        UserContextHolder.set(new UserContext(2L, "super_admin", 10L, "sid-1", "ud-admin-web"));
        when(platformRoleService.getCurrentPlatformRoles(2L)).thenReturn(List.of("platform_admin"));
        when(staffAccountService.create(any())).thenReturn(staffAccount(3L, "new-staff", "active"));
        when(staffAccountService.listDomainRoleCodes(3L)).thenReturn(List.of());
        when(staffAccountService.listBusinessDomainIds(3L)).thenReturn(List.of());
        when(staffAccountService.listOrganizationIds(3L)).thenReturn(List.of());
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
                .andExpect(jsonPath("$.username").value("new-staff"))
                .andExpect(jsonPath("$.status").value(1));
        verify(staffAccountService).create(any());
    }

    @Test
    void getStaffReturnsDetailRow() throws Exception {
        MockMvc mockMvc = mockMvc();
        UserContextHolder.set(new UserContext(2L, "super_admin", 10L, "sid-1", "ud-admin-web"));
        when(platformRoleService.getCurrentPlatformRoles(2L)).thenReturn(List.of("platform_admin"));
        when(staffAccountService.findById(1L)).thenReturn(Optional.of(staffAccount(1L, "admin", "active")));
        when(staffAccountService.listDomainRoleCodes(1L)).thenReturn(List.of("super_admin"));
        when(staffAccountService.listBusinessDomainIds(1L)).thenReturn(List.of(10L));
        when(staffAccountService.listOrganizationIds(1L)).thenReturn(List.of());
        when(platformRoleService.getCurrentPlatformRoles(1L)).thenReturn(List.of("platform_admin"));

        mockMvc.perform(get("/api/v1/admin/staff/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.platformRoles[0]").value("platform_admin"));
    }

    @Test
    void updateStaffReturnsUpdatedRowAndInvalidatesVersion() throws Exception {
        MockMvc mockMvc = mockMvc();
        UserContextHolder.set(new UserContext(2L, "super_admin", 10L, "sid-1", "ud-admin-web"));
        when(platformRoleService.getCurrentPlatformRoles(2L)).thenReturn(List.of("platform_admin"));
        when(staffAccountService.update(eq(1L), any())).thenReturn(staffAccount(1L, "admin-updated", "active"));
        when(staffAccountService.listDomainRoleCodes(1L)).thenReturn(List.of("super_admin"));
        when(staffAccountService.listBusinessDomainIds(1L)).thenReturn(List.of());
        when(staffAccountService.listOrganizationIds(1L)).thenReturn(List.of());
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
                .andExpect(jsonPath("$.username").value("admin-updated"));
        verify(authVersionService).incrementVersion(1L, "staff");
    }

    @Test
    void disableStaffReturnsUpdatedRow() throws Exception {
        MockMvc mockMvc = mockMvc();
        UserContextHolder.set(new UserContext(2L, "super_admin", 10L, "sid-1", "ud-admin-web"));
        when(platformRoleService.getCurrentPlatformRoles(2L)).thenReturn(List.of("platform_admin"));
        when(staffAccountService.disable(1L)).thenReturn(staffAccount(1L, "admin", "disabled", "active"));
        when(staffAccountService.listDomainRoleCodes(1L)).thenReturn(List.of("platform_admin"));
        when(staffAccountService.listBusinessDomainIds(1L)).thenReturn(List.of());
        when(staffAccountService.listOrganizationIds(1L)).thenReturn(List.of());
        when(platformRoleService.getCurrentPlatformRoles(1L)).thenReturn(List.of("platform_admin"));

        mockMvc.perform(post("/api/v1/admin/staff/1/disable").param("reason", "manual"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employmentStatus").value("disabled"));
        verify(authVersionService).incrementVersion(1L, "staff");
    }

    @Test
    void offboardStaffReturnsOffboardedRowAndInvalidatesVersion() throws Exception {
        MockMvc mockMvc = mockMvc();
        UserContextHolder.set(new UserContext(2L, "super_admin", 10L, "sid-1", "ud-admin-web"));
        when(platformRoleService.getCurrentPlatformRoles(2L)).thenReturn(List.of("platform_admin"));
        when(staffAccountService.offboard(eq(1L), eq(2L), eq("离职")))
                .thenReturn(staffAccount(1L, "admin", "disabled", "offboarded"));
        when(staffAccountService.listDomainRoleCodes(1L)).thenReturn(List.of());
        when(staffAccountService.listBusinessDomainIds(1L)).thenReturn(List.of());
        when(staffAccountService.listOrganizationIds(1L)).thenReturn(List.of());
        when(platformRoleService.getCurrentPlatformRoles(1L)).thenReturn(List.of());

        mockMvc.perform(post("/api/v1/admin/staff/1/offboard")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": "离职"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employmentStatus").value("offboarded"))
                .andExpect(jsonPath("$.status").value(0));
        verify(staffAccountService).offboard(1L, 2L, "离职");
        verify(authVersionService).incrementVersion(1L, "staff");
    }

    @Test
    void listStaffFiltersByOffboardedStatusAndOrganizationId() throws Exception {
        MockMvc mockMvc = mockMvc();
        UserContextHolder.set(new UserContext(2L, "super_admin", 10L, "sid-1", "ud-admin-web"));
        when(platformRoleService.getCurrentPlatformRoles(2L)).thenReturn(List.of("platform_admin"));
        when(organizationService.collectDescendantOrgIds(10L)).thenReturn(List.of(10L, 11L));
        when(staffAccountService.listAll()).thenReturn(List.of(
                staffAccount(1L, "active-staff", "active", "active"),
                staffAccount(2L, "left-staff", "disabled", "offboarded")));
        when(staffAccountService.listDomainRoleCodes(2L)).thenReturn(List.of());
        when(staffAccountService.listBusinessDomainIds(2L)).thenReturn(List.of());
        when(staffAccountService.listOrganizationIds(1L)).thenReturn(List.of(99L));
        when(staffAccountService.listOrganizationIds(2L)).thenReturn(List.of(11L));
        when(platformRoleService.getCurrentPlatformRoles(2L)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/admin/staff")
                        .param("status", "offboarded")
                        .param("organizationId", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.list[0].username").value("left-staff"))
                .andExpect(jsonPath("$.list[0].employmentStatus").value("offboarded"));
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
        when(loginSessionService.validateStepUpToken("step-up-token-1", 2L, "ud-admin-web")).thenReturn(true);
        when(platformRoleService.getCurrentPlatformRoles(2L)).thenReturn(List.of("platform_admin"));
        when(staffAccountService.findById(1L)).thenReturn(Optional.of(staffAccount(1L, "admin", "active")));
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

    @Test
    void updatePlatformRolesRejectsInvalidStepUpToken() throws Exception {
        MockMvc mockMvc = mockMvc();
        UserContextHolder.set(new UserContext(2L, "super_admin", 10L, "sid-1", "ud-admin-web"));
        when(loginSessionService.validateStepUpToken("step-up-token-invalid", 2L, "ud-admin-web")).thenReturn(false);
        when(platformRoleService.getCurrentPlatformRoles(2L)).thenReturn(List.of("platform_admin"));
        when(staffAccountService.findById(1L)).thenReturn(Optional.of(staffAccount(1L, "admin", "active")));
        when(platformRoleService.getCurrentPlatformRoles(1L)).thenReturn(List.of("platform_admin"));

        mockMvc.perform(put("/api/v1/admin/staff/1/platform-roles")
                        .header("X-UD-Step-Up-Token", "step-up-token-invalid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "roleCodes": ["platform_admin"]
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("40301"));
    }

    private MockMvc mockMvc() {
        return MockMvcBuilders.standaloneSetup(new StaffController(
                        staffAccountService, platformRoleService, organizationService, authVersionService, loginSessionService))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    private StaffAccountService.StaffAccount staffAccount(long id, String username, String status) {
        return staffAccount(id, username, status, "active");
    }

    private StaffAccountService.StaffAccount staffAccount(
            long id, String username, String status, String employmentStatus) {
        return new StaffAccountService.StaffAccount(
                id,
                100L + id,
                username,
                "真实姓名",
                "昵称",
                null,
                "138000000" + id,
                username + "@uniondesk.local",
                status,
                employmentStatus,
                "offboarded".equalsIgnoreCase(employmentStatus) ? "2026-07-19T02:00:00" : null,
                "offboarded".equalsIgnoreCase(employmentStatus) ? 2L : null,
                "offboarded".equalsIgnoreCase(employmentStatus) ? "离职" : null,
                "local",
                1);
    }
}
