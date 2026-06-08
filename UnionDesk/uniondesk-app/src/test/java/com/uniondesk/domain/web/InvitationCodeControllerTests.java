package com.uniondesk.domain.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.uniondesk.common.web.PageResult;
import com.uniondesk.domain.core.InvitationCodeService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class InvitationCodeControllerTests {

    private final InvitationCodeService invitationCodeService = mock(InvitationCodeService.class);

    @Test
    void listInvitationCodesReturnsPageResult() throws Exception {
        MockMvc mockMvc = mockMvc();
        when(invitationCodeService.listInvitationCodes(10L, 1, 20)).thenReturn(new PageResult<>(
                1,
                List.of(invitationCodeView(1L, "ABCD1234"))));

        mockMvc.perform(get("/api/v1/admin/domains/10/invitation-codes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.list[0].code").value("ABCD1234"));
    }

    @Test
    void createInvitationCodeReturnsView() throws Exception {
        MockMvc mockMvc = mockMvc();
        when(invitationCodeService.createInvitationCode(anyLong(), any())).thenReturn(invitationCodeView(2L, "EFGH5678"));

        mockMvc.perform(post("/api/v1/admin/domains/10/invitation-codes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "channel": "wechat",
                                  "expiresAt": "2026-05-10T12:00:00",
                                  "maxUses": 10
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.code").value("EFGH5678"));
        verify(invitationCodeService).createInvitationCode(anyLong(), any());
    }

    @Test
    void deleteInvitationCodeReturnsNoContent() throws Exception {
        MockMvc mockMvc = mockMvc();

        mockMvc.perform(delete("/api/v1/admin/domains/10/invitation-codes/1"))
                .andExpect(status().isNoContent());
        verify(invitationCodeService).deleteInvitationCode(10L, 1L);
    }

    private MockMvc mockMvc() {
        return MockMvcBuilders.standaloneSetup(new InvitationCodeController(invitationCodeService)).build();
    }

    private InvitationCodeDtos.InvitationCodeView invitationCodeView(long id, String code) {
        LocalDateTime now = LocalDateTime.parse("2026-05-03T12:00:00");
        return new InvitationCodeDtos.InvitationCodeView(
                id,
                10L,
                code,
                "wechat",
                LocalDateTime.parse("2026-05-10T12:00:00"),
                10,
                0,
                "active",
                now,
                now);
    }
}
