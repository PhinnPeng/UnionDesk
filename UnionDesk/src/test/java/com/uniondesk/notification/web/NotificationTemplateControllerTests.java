package com.uniondesk.notification.web;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.uniondesk.common.web.PageResult;
import com.uniondesk.notification.core.NotificationTemplateService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class NotificationTemplateControllerTests {

    @Test
    void listReturnsPageResult() throws Exception {
        NotificationTemplateService notificationTemplateService = mock(NotificationTemplateService.class);
        when(notificationTemplateService.list(1L, 1, 20)).thenReturn(new PageResult<>(1, List.of(
                new NotificationTemplateService.NotificationTemplateView(
                        9L,
                        "domain",
                        1L,
                        "ticket.create",
                        "email",
                        "ticket.create",
                        "工单创建",
                        "内容模板",
                        false,
                        true,
                        "active",
                        LocalDateTime.parse("2026-05-03T08:00:00"),
                        LocalDateTime.parse("2026-05-03T08:05:00")))));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new NotificationTemplateController(notificationTemplateService)).build();

        mockMvc.perform(get("/api/v1/admin/domains/1/notification-templates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.list[0].code").value("ticket.create"));

        verify(notificationTemplateService).list(1L, 1, 20);
    }

    @Test
    void updateReturnsView() throws Exception {
        NotificationTemplateService notificationTemplateService = mock(NotificationTemplateService.class);
        when(notificationTemplateService.update(org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.eq(9L), org.mockito.ArgumentMatchers.any()))
                .thenReturn(new NotificationTemplateService.NotificationTemplateView(
                        9L,
                        "domain",
                        1L,
                        "ticket.create",
                        "email",
                        "ticket.create",
                        "工单创建",
                        "内容模板",
                        false,
                        true,
                        "active",
                        LocalDateTime.parse("2026-05-03T08:00:00"),
                        LocalDateTime.parse("2026-05-03T08:05:00")));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new NotificationTemplateController(notificationTemplateService)).build();

        mockMvc.perform(put("/api/v1/admin/domains/1/notification-templates/9")
                        .contentType("application/json")
                        .content("""
                                {
                                  "eventCategory": "ticket.create",
                                  "channel": "email",
                                  "code": "ticket.create",
                                  "titleTemplate": "工单创建",
                                  "contentTemplate": "内容模板"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("ticket.create"));

        verify(notificationTemplateService).update(org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.eq(9L), org.mockito.ArgumentMatchers.any());
    }
}
