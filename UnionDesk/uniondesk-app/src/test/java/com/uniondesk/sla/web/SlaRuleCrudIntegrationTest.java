package com.uniondesk.sla.web;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uniondesk.auth.core.AuthCaptchaService;
import com.uniondesk.sla.core.SlaService;
import com.uniondesk.support.FixedClockTestConfiguration;
import com.uniondesk.support.IntegrationTestSupport;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import com.uniondesk.support.IntegrationAuthSupport;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(FixedClockTestConfiguration.class)
@Transactional
class SlaRuleCrudIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private SlaService slaService;

    @MockBean
    private AuthCaptchaService authCaptchaService;

    private String adminToken;

    @BeforeEach
    void setUp() throws Exception {
        IntegrationAuthSupport.mockCaptchaBypass(authCaptchaService);
        adminToken = adminAccessToken(mockMvc, objectMapper);
    }

    @Test
    void createUpdateAndListSlaRuleThroughController() throws Exception {
        long domainId = defaultDomainId(jdbcTemplate);
        long ticketTypeId = defaultTicketTypeId(jdbcTemplate, domainId);

        String createResponse = mockMvc.perform(post("/api/v1/admin/domains/{domainId}/sla-rules", domainId)
                        .header("Authorization", bearer())
                        .header("X-UD-Client-Code", "ud-admin-web")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SlaService.SlaRuleCommand(
                                "Initial SLA",
                                ticketTypeId,
                                null,
                                null,
                                30,
                                90,
                                false,
                                Map.of("raise_priority_to", "urgent", "sla_status", "escalated")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Initial SLA"))
                .andExpect(jsonPath("$.firstResponseMinutes").value(30))
                .andReturn()
                .getResponse()
                .getContentAsString();

        long ruleId = objectMapper.readTree(createResponse).get("id").asLong();

        mockMvc.perform(put("/api/v1/admin/domains/{domainId}/sla-rules/{ruleId}", domainId, ruleId)
                        .header("Authorization", bearer())
                        .header("X-UD-Client-Code", "ud-admin-web")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SlaService.SlaRuleCommand(
                                "Updated SLA",
                                ticketTypeId,
                                null,
                                null,
                                45,
                                120,
                                true,
                                Map.of("raise_priority_to", "high", "sla_status", "breached")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated SLA"))
                .andExpect(jsonPath("$.isUrgentConfig").value(true));

        mockMvc.perform(get("/api/v1/admin/domains/{domainId}/sla-rules", domainId)
                        .header("Authorization", bearer())
                        .header("X-UD-Client-Code", "ud-admin-web"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.list[0].name").value("Updated SLA"));
    }

    @Test
    void createAndUpdateSlaCalendarThroughController() throws Exception {
        long domainId = defaultDomainId(jdbcTemplate);

        String createResponse = mockMvc.perform(post("/api/v1/admin/domains/{domainId}/sla-calendars", domainId)
                        .header("Authorization", bearer())
                        .header("X-UD-Client-Code", "ud-admin-web")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SlaService.SlaCalendarCommand(
                                "Working Calendar",
                                Map.of("timezone", "UTC", "working_days", List.of(1, 2, 3, 4, 5))))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Working Calendar"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        long calendarId = objectMapper.readTree(createResponse).get("id").asLong();
        SlaService.SlaCalendarCommand updateCommand = new SlaService.SlaCalendarCommand(
                "Holiday Calendar",
                Map.of("timezone", "Asia/Shanghai", "working_days", List.of(1, 2, 3)));

        mockMvc.perform(put("/api/v1/admin/domains/{domainId}/sla-calendars/{calendarId}", domainId, calendarId)
                        .header("Authorization", bearer())
                        .header("X-UD-Client-Code", "ud-admin-web")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateCommand)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Holiday Calendar"));

        mockMvc.perform(get("/api/v1/admin/domains/{domainId}/sla-calendars", domainId)
                        .header("Authorization", bearer())
                        .header("X-UD-Client-Code", "ud-admin-web"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.list[0].config.timezone").value("Asia/Shanghai"));
    }

    @Test
    void listSlaRulesPaginatesWithinTheSameDomain() throws Exception {
        long domainId = defaultDomainId(jdbcTemplate);
        long ticketTypeId = defaultTicketTypeId(jdbcTemplate, domainId);

        createRule("Paging SLA A", domainId, ticketTypeId, 10, 20);
        createRule("Paging SLA B", domainId, ticketTypeId, 15, 30);

        mockMvc.perform(get("/api/v1/admin/domains/{domainId}/sla-rules", domainId)
                        .queryParam("page", "1")
                        .queryParam("page_size", "1")
                        .header("Authorization", bearer())
                        .header("X-UD-Client-Code", "ud-admin-web"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.list").isArray())
                .andExpect(jsonPath("$.list.length()").value(1));

        mockMvc.perform(get("/api/v1/admin/domains/{domainId}/sla-rules", domainId)
                        .queryParam("page", "2")
                        .queryParam("page_size", "1")
                        .header("Authorization", bearer())
                        .header("X-UD-Client-Code", "ud-admin-web"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.list.length()").value(1))
                .andExpect(jsonPath("$.list[0].name").value("Paging SLA A"));
    }

    private void createRule(String name, long domainId, long ticketTypeId, int firstResponseMinutes, int resolutionMinutes) {
        slaService.createSlaRule(
                domainId,
                new SlaService.SlaRuleCommand(
                        name,
                        ticketTypeId,
                        null,
                        null,
                        firstResponseMinutes,
                        resolutionMinutes,
                        false,
                        Map.of("raise_priority_to", "urgent", "sla_status", "escalated")));
    }

    private String bearer() {
        return "Bearer " + adminToken;
    }
}
