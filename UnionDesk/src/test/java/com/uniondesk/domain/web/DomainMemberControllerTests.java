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

import com.uniondesk.common.web.PageResult;
import com.uniondesk.domain.core.DomainMemberService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class DomainMemberControllerTests {

    private final DomainMemberService domainMemberService = mock(DomainMemberService.class);

    @Test
    void listMembersReturnsPageResult() throws Exception {
        MockMvc mockMvc = mockMvc();
        when(domainMemberService.listMembers(1L, 1, 20, null, null, null, null)).thenReturn(new PageResult<>(
                1,
                List.of(memberView(11L))));

        mockMvc.perform(get("/api/v1/admin/domains/1/members"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.list[0].username").value("staff-1"))
                .andExpect(jsonPath("$.list[0].roles[0].code").value("domain_admin"));
    }

    @Test
    void createMemberReturnsView() throws Exception {
        MockMvc mockMvc = mockMvc();
        when(domainMemberService.createMember(eq(1L), any())).thenReturn(memberView(11L));

        mockMvc.perform(post("/api/v1/admin/domains/1/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "staff_account_id": 1001,
                                  "role_ids": [21, 22]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(11))
                .andExpect(jsonPath("$.staff_account_id").value(1001));
        verify(domainMemberService).createMember(eq(1L), any());
    }

    @Test
    void updateMemberRolesReturnsView() throws Exception {
        MockMvc mockMvc = mockMvc();
        when(domainMemberService.updateMemberRoles(eq(1L), eq(11L), any())).thenReturn(memberView(11L));

        mockMvc.perform(put("/api/v1/admin/domains/1/members/11/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "role_ids": [21]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(11));
        verify(domainMemberService).updateMemberRoles(eq(1L), eq(11L), any());
    }

    @Test
    void deleteMemberReturnsNoContent() throws Exception {
        MockMvc mockMvc = mockMvc();

        mockMvc.perform(delete("/api/v1/admin/domains/1/members/11"))
                .andExpect(status().isNoContent());
        verify(domainMemberService).deleteMember(1L, 11L);
    }

    @Test
    void listStaffCandidatesReturnsPageResult() throws Exception {
        MockMvc mockMvc = mockMvc();
        when(domainMemberService.listStaffCandidates(1L, 1, 20, null)).thenReturn(new PageResult<>(
                1,
                List.of(new DomainMemberDtos.StaffCandidateView(1001L, "staff-1", "张三", "小王", "13800000000", "a@b.com", "active"))));

        mockMvc.perform(get("/api/v1/admin/domains/1/members/staff-candidates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.list[0].username").value("staff-1"));
    }

    @Test
    void updateMemberStatusReturnsView() throws Exception {
        MockMvc mockMvc = mockMvc();
        when(domainMemberService.updateMemberStatus(eq(1L), eq(11L), any())).thenReturn(memberView(11L));

        mockMvc.perform(put("/api/v1/admin/domains/1/members/11/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "disabled"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(11));
    }

    @Test
    void createMemberWithStaffReturnsView() throws Exception {
        MockMvc mockMvc = mockMvc();
        when(domainMemberService.createMemberWithStaff(eq(1L), any())).thenReturn(memberView(11L));

        mockMvc.perform(post("/api/v1/admin/domains/1/members/with-staff")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "new-staff",
                                  "phone": "13800001111",
                                  "password": "Passw0rd!",
                                  "role_ids": [21]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(11));
    }

    private MockMvc mockMvc() {
        return MockMvcBuilders.standaloneSetup(new DomainMemberController(domainMemberService)).build();
    }

    private DomainMemberDtos.DomainMemberView memberView(long id) {
        LocalDateTime now = LocalDateTime.of(2026, 5, 3, 12, 0);
        return new DomainMemberDtos.DomainMemberView(
                id,
                1001L,
                1L,
                "staff-1",
                "张三",
                "客服小王",
                "13800000000",
                "staff@example.com",
                "active",
                "manual",
                now,
                null,
                null,
                now,
                List.of(new DomainRoleDtos.DomainRoleView(21L, 1L, "domain_admin", "业务域管理员", true)));
    }
}
