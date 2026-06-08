package com.uniondesk.ticket.web;

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

import com.uniondesk.ticket.core.TicketConfigService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class TicketConfigControllerTests {

    private final TicketConfigService ticketConfigService = mock(TicketConfigService.class);

    @Test
    void listTicketTypesReturnsRows() throws Exception {
        MockMvc mockMvc = mockMvc();
        when(ticketConfigService.listTicketTypes(1L)).thenReturn(List.of(
                new TicketConfigDtos.TicketTypeView("11", "1", "default", "默认类型", List.of("title"), "active")));

        mockMvc.perform(get("/api/v1/admin/domains/1/ticket-types"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("default"))
                .andExpect(jsonPath("$[0].status").value("active"));
        verify(ticketConfigService).listTicketTypes(1L);
    }

    @Test
    void createTicketTemplateReturnsCreatedResource() throws Exception {
        MockMvc mockMvc = mockMvc();
        when(ticketConfigService.createTicketTemplate(eq(1L), any())).thenReturn(new TicketConfigDtos.TicketTemplateView(
                "21",
                "1",
                "常见问题",
                "internal",
                "11",
                List.of("title"),
                "content",
                10));

        mockMvc.perform(post("/api/v1/admin/domains/1/ticket-templates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "常见问题",
                                  "type": "internal",
                                  "type_id": "11",
                                  "fields_snapshot": ["title"],
                                  "content": "content",
                                  "sort_order": 10
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("21"))
                .andExpect(jsonPath("$.type_id").value("11"));
        verify(ticketConfigService).createTicketTemplate(eq(1L), any());
    }

    @Test
    void quickReplyAliasRoutesShareTheSameServiceCall() throws Exception {
        MockMvc mockMvc = mockMvc();
        when(ticketConfigService.createQuickReply(eq(1L), any())).thenReturn(new TicketConfigDtos.QuickReplyView(
                "31",
                "1",
                "问候语",
                "您好，请问有什么可以帮您",
                "ticket",
                1,
                "2026-05-03T12:00:00"));

        mockMvc.perform(post("/api/v1/admin/domains/1/quick-reply-templates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "问候语",
                                  "content": "您好，请问有什么可以帮您",
                                  "scope": "ticket",
                                  "sort_order": 1
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("31"))
                .andExpect(jsonPath("$.scope").value("ticket"));
        verify(ticketConfigService).createQuickReply(eq(1L), any());
    }

    @Test
    void updatePriorityLevelReturnsUpdatedResource() throws Exception {
        MockMvc mockMvc = mockMvc();
        when(ticketConfigService.updatePriorityLevel(eq(1L), eq(5L), any())).thenReturn(new TicketConfigDtos.PriorityLevelView(
                "5",
                "1",
                "高",
                "高",
                "#ff0000",
                30,
                true));

        mockMvc.perform(put("/api/v1/admin/domains/1/priority-levels/5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "高",
                                  "display_label": "高",
                                  "color": "#ff0000",
                                  "sort_order": 30,
                                  "is_default": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("5"))
                .andExpect(jsonPath("$.is_default").value(true));
        verify(ticketConfigService).updatePriorityLevel(eq(1L), eq(5L), any());
    }

    @Test
    void deleteTicketTypeReturnsNoContent() throws Exception {
        MockMvc mockMvc = mockMvc();

        mockMvc.perform(delete("/api/v1/admin/domains/1/ticket-types/99"))
                .andExpect(status().isNoContent());
        verify(ticketConfigService).deleteTicketType(1L, 99L);
    }

    private MockMvc mockMvc() {
        return MockMvcBuilders.standaloneSetup(new TicketConfigController(ticketConfigService)).build();
    }
}
