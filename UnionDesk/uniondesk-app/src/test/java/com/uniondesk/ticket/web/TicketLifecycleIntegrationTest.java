package com.uniondesk.ticket.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uniondesk.auth.core.AuthCaptchaService;
import com.uniondesk.auth.core.LoginAuditService;
import com.uniondesk.support.FixedClockTestConfiguration;
import com.uniondesk.support.IntegrationAuthSupport;
import com.uniondesk.support.IntegrationTestSupport;
import com.uniondesk.ticket.core.TicketService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(FixedClockTestConfiguration.class)
@Transactional
class TicketLifecycleIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private AuthCaptchaService authCaptchaService;

    @SpyBean
    private LoginAuditService loginAuditService;

    @BeforeEach
    void setUp() {
        IntegrationAuthSupport.mockCaptchaBypass(authCaptchaService);
        doNothing().when(loginAuditService).record(any());
    }

    @Test
    void customerCanCreateAndWithdrawTicketThroughHttpLifecycle() throws Exception {
        long domainId = defaultDomainId(jdbcTemplate);
        long ticketTypeId = defaultTicketTypeId(jdbcTemplate, domainId);
        String customerToken = registerCustomer(domainId, "ticket_customer_withdraw", "customer123");

        long ticketId = createTicket(customerToken, domainId, ticketTypeId, "客户提单", "请帮我处理一下登录问题");

        mockMvc.perform(post("/api/v1/domains/{domainId}/tickets/my/{ticketId}/withdraw", domainId, ticketId)
                        .header("Authorization", IntegrationAuthSupport.bearer(customerToken))
                        .header("X-UD-Client-Code", IntegrationAuthSupport.CUSTOMER_CLIENT_CODE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TicketService.WithdrawTicketCommand(1L, "已经解决"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ticketId));

        mockMvc.perform(get("/api/v1/domains/{domainId}/tickets/my/{ticketId}", domainId, ticketId)
                        .header("Authorization", IntegrationAuthSupport.bearer(customerToken))
                        .header("X-UD-Client-Code", IntegrationAuthSupport.CUSTOMER_CLIENT_CODE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticket.status").value("withdrawn"))
                .andExpect(jsonPath("$.history.length()").value(2))
                .andExpect(jsonPath("$.history[0].action").value("create"))
                .andExpect(jsonPath("$.history[1].action").value("withdraw"));

        Integer historyCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ticket_history WHERE ticket_id = ?",
                Integer.class,
                ticketId);
        assertThat(historyCount).isEqualTo(2);
    }

    @Test
    void staffCanClaimReplyAndCloseTicketThroughHttpLifecycle() throws Exception {
        long domainId = defaultDomainId(jdbcTemplate);
        long ticketTypeId = defaultTicketTypeId(jdbcTemplate, domainId);
        String customerToken = registerCustomer(domainId, "ticket_customer_claim", "customer123");
        String adminToken = IntegrationAuthSupport.loginAccessToken(
                mockMvc, objectMapper, IntegrationAuthSupport.ADMIN_CLIENT_CODE, "admin", "admin123");

        long ticketId = createTicket(customerToken, domainId, ticketTypeId, "领取后回复", "需要客服跟进");

        mockMvc.perform(post("/api/v1/admin/domains/{domainId}/tickets/{ticketId}/claim", domainId, ticketId)
                        .header("Authorization", IntegrationAuthSupport.bearer(adminToken))
                        .header("X-UD-Client-Code", IntegrationAuthSupport.ADMIN_CLIENT_CODE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TicketService.ClaimTicketCommand(1L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ticketId));

        mockMvc.perform(post("/api/v1/admin/domains/{domainId}/tickets/{ticketId}/replies", domainId, ticketId)
                        .header("Authorization", IntegrationAuthSupport.bearer(adminToken))
                        .header("X-UD-Client-Code", IntegrationAuthSupport.ADMIN_CLIENT_CODE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TicketService.ReplyTicketCommand(
                                ticketVersion(ticketId),
                                "已经收到，我们在处理。",
                                null,
                                List.of()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists());

        mockMvc.perform(patch("/api/v1/admin/domains/{domainId}/tickets/{ticketId}/status", domainId, ticketId)
                        .header("Authorization", IntegrationAuthSupport.bearer(adminToken))
                        .header("X-UD-Client-Code", IntegrationAuthSupport.ADMIN_CLIENT_CODE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TicketService.ChangeTicketStatusCommand(
                                "closed",
                                ticketVersion(ticketId),
                                null,
                                "已关闭"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ticketId));

        mockMvc.perform(get("/api/v1/admin/domains/{domainId}/tickets/{ticketId}", domainId, ticketId)
                        .header("Authorization", IntegrationAuthSupport.bearer(adminToken))
                        .header("X-UD-Client-Code", IntegrationAuthSupport.ADMIN_CLIENT_CODE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticket.status").value("closed"))
                .andExpect(jsonPath("$.ticket.version").value(4))
                .andExpect(jsonPath("$.replies.length()").value(1))
                .andExpect(jsonPath("$.history.length()").value(4))
                .andExpect(jsonPath("$.history[0].action").value("create"))
                .andExpect(jsonPath("$.history[1].action").value("claim"))
                .andExpect(jsonPath("$.history[2].action").value("reply"))
                .andExpect(jsonPath("$.history[3].action").value("status_change"));

        Integer replyCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ticket_reply WHERE ticket_id = ?",
                Integer.class,
                ticketId);
        assertThat(replyCount).isEqualTo(1);
    }

    @Test
    void staffCanAssignReplyAndMergeTicketThroughHttpLifecycle() throws Exception {
        long domainId = defaultDomainId(jdbcTemplate);
        long ticketTypeId = defaultTicketTypeId(jdbcTemplate, domainId);
        String customerToken = registerCustomer(domainId, "ticket_customer_merge", "customer123");
        String adminToken = IntegrationAuthSupport.loginAccessToken(
                mockMvc, objectMapper, IntegrationAuthSupport.ADMIN_CLIENT_CODE, "admin", "admin123");

        long sourceTicketId = createTicket(customerToken, domainId, ticketTypeId, "待合并工单", "这是源工单");
        long targetTicketId = createTicket(customerToken, domainId, ticketTypeId, "主工单", "这是目标工单");

        mockMvc.perform(post("/api/v1/admin/domains/{domainId}/tickets/{ticketId}/claim", domainId, sourceTicketId)
                        .header("Authorization", IntegrationAuthSupport.bearer(adminToken))
                        .header("X-UD-Client-Code", IntegrationAuthSupport.ADMIN_CLIENT_CODE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TicketService.ClaimTicketCommand(1L))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/admin/domains/{domainId}/tickets/{ticketId}/assign", domainId, sourceTicketId)
                        .header("Authorization", IntegrationAuthSupport.bearer(adminToken))
                        .header("X-UD-Client-Code", IntegrationAuthSupport.ADMIN_CLIENT_CODE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TicketService.AssignTicketCommand(
                                ticketVersion(sourceTicketId),
                                2L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(sourceTicketId));

        mockMvc.perform(post("/api/v1/admin/domains/{domainId}/tickets/{ticketId}/replies", domainId, sourceTicketId)
                        .header("Authorization", IntegrationAuthSupport.bearer(adminToken))
                        .header("X-UD-Client-Code", IntegrationAuthSupport.ADMIN_CLIENT_CODE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TicketService.ReplyTicketCommand(
                                ticketVersion(sourceTicketId),
                                "这个问题我继续跟进。",
                                null,
                                List.of()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists());

        mockMvc.perform(post("/api/v1/admin/domains/{domainId}/tickets/{ticketId}/merge", domainId, sourceTicketId)
                        .header("Authorization", IntegrationAuthSupport.bearer(adminToken))
                        .header("X-UD-Client-Code", IntegrationAuthSupport.ADMIN_CLIENT_CODE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TicketService.MergeTicketCommand(
                                ticketVersion(sourceTicketId),
                                targetTicketId,
                                "重复工单"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(sourceTicketId));

        mockMvc.perform(get("/api/v1/admin/domains/{domainId}/tickets/{ticketId}", domainId, sourceTicketId)
                        .header("Authorization", IntegrationAuthSupport.bearer(adminToken))
                        .header("X-UD-Client-Code", IntegrationAuthSupport.ADMIN_CLIENT_CODE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticket.status").value("merged"))
                .andExpect(jsonPath("$.ticket.version").value(5))
                .andExpect(jsonPath("$.history.length()").value(5))
                .andExpect(jsonPath("$.history[0].action").value("create"))
                .andExpect(jsonPath("$.history[1].action").value("claim"))
                .andExpect(jsonPath("$.history[2].action").value("assign"))
                .andExpect(jsonPath("$.history[3].action").value("reply"))
                .andExpect(jsonPath("$.history[4].action").value("merge"));

        Long relationTargetId = jdbcTemplate.queryForObject(
                "SELECT target_ticket_id FROM ticket_relation WHERE source_ticket_id = ? ORDER BY id DESC LIMIT 1",
                Long.class,
                sourceTicketId);
        assertThat(relationTargetId).isEqualTo(targetTicketId);
    }

    @Test
    void claimTicketRejectsVersionConflictThroughController() throws Exception {
        long domainId = defaultDomainId(jdbcTemplate);
        long ticketTypeId = defaultTicketTypeId(jdbcTemplate, domainId);
        String customerToken = registerCustomer(domainId, "ticket_customer_conflict", "customer123");
        String adminToken = IntegrationAuthSupport.loginAccessToken(
                mockMvc, objectMapper, IntegrationAuthSupport.ADMIN_CLIENT_CODE, "admin", "admin123");

        long ticketId = createTicket(customerToken, domainId, ticketTypeId, "版本冲突", "请不要重复处理");

        mockMvc.perform(post("/api/v1/admin/domains/{domainId}/tickets/{ticketId}/claim", domainId, ticketId)
                        .header("Authorization", IntegrationAuthSupport.bearer(adminToken))
                        .header("X-UD-Client-Code", IntegrationAuthSupport.ADMIN_CLIENT_CODE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TicketService.ClaimTicketCommand(2L))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("工单已被他人修改，请刷新"));
    }

    @Test
    void closeTicketRejectsIllegalStatusTransitionThroughController() throws Exception {
        long domainId = defaultDomainId(jdbcTemplate);
        long ticketTypeId = defaultTicketTypeId(jdbcTemplate, domainId);
        String customerToken = registerCustomer(domainId, "ticket_customer_close", "customer123");
        String adminToken = IntegrationAuthSupport.loginAccessToken(
                mockMvc, objectMapper, IntegrationAuthSupport.ADMIN_CLIENT_CODE, "admin", "admin123");

        long ticketId = createTicket(customerToken, domainId, ticketTypeId, "非法流转", "直接关闭应该被拒绝");

        mockMvc.perform(patch("/api/v1/admin/domains/{domainId}/tickets/{ticketId}/status", domainId, ticketId)
                        .header("Authorization", IntegrationAuthSupport.bearer(adminToken))
                        .header("X-UD-Client-Code", IntegrationAuthSupport.ADMIN_CLIENT_CODE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TicketService.ChangeTicketStatusCommand(
                                "closed",
                                1L,
                                null,
                                "直接关闭"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("状态流转不合法"));
    }

    private String registerCustomer(long domainId, String username, String password) throws Exception {
        return IntegrationAuthSupport.registerCustomerAccessToken(
                mockMvc, objectMapper, jdbcTemplate, domainId, username, password).accessToken();
    }

    private long createTicket(String customerToken, long domainId, long ticketTypeId, String title, String description) throws Exception {
        String response = mockMvc.perform(post("/api/v1/domains/{domainId}/tickets", domainId)
                        .header("Authorization", IntegrationAuthSupport.bearer(customerToken))
                        .header("X-UD-Client-Code", IntegrationAuthSupport.CUSTOMER_CLIENT_CODE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TicketService.CreateTicketCommand(
                                ticketTypeId,
                                title,
                                description,
                                Map.of("channel", "web"),
                                List.of(),
                                null,
                                null,
                                "web"))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return readJson(response).get("id").asLong();
    }

    private long ticketVersion(long ticketId) {
        Long version = jdbcTemplate.queryForObject("SELECT version FROM ticket WHERE id = ?", Long.class, ticketId);
        assertThat(version).as("ticket version should exist").isNotNull();
        return version;
    }

    private JsonNode readJson(String response) throws Exception {
        return objectMapper.readTree(response);
    }
}
