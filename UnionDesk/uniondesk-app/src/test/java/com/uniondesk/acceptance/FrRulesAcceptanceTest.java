package com.uniondesk.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uniondesk.auth.core.AuthCaptchaService;
import com.uniondesk.auth.core.LoginAuditService;
import com.uniondesk.support.FixedClockTestConfiguration;
import com.uniondesk.support.IntegrationAuthSupport;
import com.uniondesk.support.IntegrationTestSupport;
import com.uniondesk.ticket.core.TicketService;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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

/**
 * FR 规则场景化端到端验收测试（S8 任务 19 双端验收固化，对应 foundation-rules.md §5.1）。
 *
 * <p>本类新建用例：
 * <ul>
 *   <li>FR-06 工单最小闭环：注册 → 入域 → 提单 201 → 员工认领/回复 → 客户查进度/回复 → 关闭 → 评价 200 → 重复评价 400</li>
 *   <li>FR-02 跨域拒绝（工单端点）：A 域 domain_admin 访问 B 域工单列表 → 403 + 中文；成员端点由
 *       {@code DomainScopedPermissionIntegrationTest} 覆盖，此处不重复</li>
 *   <li>FR-05 未入域拒绝：客户注册但无 domain_customer 关系 → 提单 403 + 中文；「入另一域」由
 *       {@code CustomerTicketPermissionIntegrationTest#customerNotInDomainCannotCreateTicket} 覆盖，此处不重复</li>
 *   <li>FR-01 未授权 403：customer 角色调用平台端点 /api/v1/admin/staff → 403 + 中文</li>
 *   <li>FR-03 按钮权限：无 ticket.close 权限的角色（customer）调用关闭端点 → 403 + 中文（前端按钮不可见属组件测试，不在本类范围）</li>
 * </ul>
 *
 * <p>FR-07 集团角色（P1 后）由既有测试完整覆盖，本类引用不重复实现：
 * {@code RoleTemplateIntegrationTest}（模板创建 + 多域 apply 生成实例 + 锁定字段 403）、
 * {@code StaffDomainMemberBatchIntegrationTest}（step-up 令牌 + 跨域批量停用逐域部分成功）。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(FixedClockTestConfiguration.class)
@Transactional
class FrRulesAcceptanceTest extends IntegrationTestSupport {

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

    /** FR-06：1 个业务域工单最小闭环（提单 → 处理 → 客户可查 → 关闭 → 评价 → 重复评价拒绝）。 */
    @Test
    void fr06TicketClosedLoopEndToEnd() throws Exception {
        long domainId = defaultDomainId(jdbcTemplate);
        long ticketTypeId = defaultTicketTypeId(jdbcTemplate, domainId);
        // 注册客户（真实 register API）并自动入域
        String customerToken = IntegrationAuthSupport.registerCustomerAccessToken(
                mockMvc, objectMapper, jdbcTemplate, domainId,
                "fr06_customer_" + uniqueSuffix(), "customer123").accessToken();
        // 新建域内员工（agent，SQL 建号 + 真实登录）
        String agentToken = staffToken(domainId, "fr06_agent_" + uniqueSuffix(), "agent");

        // 客户提单 → 201
        long ticketId = createTicket(customerToken, domainId, ticketTypeId, "闭环验收工单", "登录失败需要处理");

        // 员工认领
        mockMvc.perform(post("/api/v1/admin/domains/{domainId}/tickets/{ticketId}/claim", domainId, ticketId)
                        .header("Authorization", IntegrationAuthSupport.bearer(agentToken))
                        .header("X-UD-Client-Code", IntegrationAuthSupport.ADMIN_CLIENT_CODE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TicketService.ClaimTicketCommand(1L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(ticketId));

        // 员工回复
        mockMvc.perform(post("/api/v1/admin/domains/{domainId}/tickets/{ticketId}/replies", domainId, ticketId)
                        .header("Authorization", IntegrationAuthSupport.bearer(agentToken))
                        .header("X-UD-Client-Code", IntegrationAuthSupport.ADMIN_CLIENT_CODE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TicketService.ReplyTicketCommand(
                                ticketVersion(ticketId), "已收到，正在处理中。", null, List.of(), false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").exists());

        // 客户查看进度：状态为处理中，且能看到员工回复
        mockMvc.perform(get("/api/v1/domains/{domainId}/tickets/my/{ticketId}", domainId, ticketId)
                        .header("Authorization", IntegrationAuthSupport.bearer(customerToken))
                        .header("X-UD-Client-Code", IntegrationAuthSupport.CUSTOMER_CLIENT_CODE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ticket.status").value("processing"))
                .andExpect(jsonPath("$.data.replies[0].content").value("已收到，正在处理中。"));

        // 客户补充回复
        mockMvc.perform(post("/api/v1/domains/{domainId}/tickets/my/{ticketId}/replies", domainId, ticketId)
                        .header("Authorization", IntegrationAuthSupport.bearer(customerToken))
                        .header("X-UD-Client-Code", IntegrationAuthSupport.CUSTOMER_CLIENT_CODE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TicketService.ReplyTicketCommand(
                                ticketVersion(ticketId), "好的，麻烦尽快。", null, List.of(), false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").exists());

        // 员工关闭工单
        mockMvc.perform(patch("/api/v1/admin/domains/{domainId}/tickets/{ticketId}/status", domainId, ticketId)
                        .header("Authorization", IntegrationAuthSupport.bearer(agentToken))
                        .header("X-UD-Client-Code", IntegrationAuthSupport.ADMIN_CLIENT_CODE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TicketService.ChangeTicketStatusCommand(
                                "closed", ticketVersion(ticketId), null, "已处理完成"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(ticketId));

        // 客户评价成功
        mockMvc.perform(post("/api/v1/domains/{domainId}/tickets/my/{ticketId}/satisfaction", domainId, ticketId)
                        .header("Authorization", IntegrationAuthSupport.bearer(customerToken))
                        .header("X-UD-Client-Code", IntegrationAuthSupport.CUSTOMER_CLIENT_CODE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"rating": 5, "comment": "处理及时，满意"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").exists());

        // 重复评价 → 400 + 中文
        mockMvc.perform(post("/api/v1/domains/{domainId}/tickets/my/{ticketId}/satisfaction", domainId, ticketId)
                        .header("Authorization", IntegrationAuthSupport.bearer(customerToken))
                        .header("X-UD-Client-Code", IntegrationAuthSupport.CUSTOMER_CLIENT_CODE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"rating": 1, "comment": "重复提交"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("该工单已评价，请勿重复提交"));
    }

    /** FR-02：A 域 domain_admin 访问 B 域工单端点 → 403 + 中文；本域端点不受影响（成员端点由 DomainScopedPermissionIntegrationTest 覆盖）。 */
    @Test
    void fr02DomainAdminForbiddenOnOtherDomainTicketEndpoints() throws Exception {
        long domainA = insertDomain("fr02_a_" + uniqueSuffix());
        long domainB = insertDomain("fr02_b_" + uniqueSuffix());
        String adminToken = staffToken(domainA, "fr02_admin_" + uniqueSuffix(), "domain_admin");

        // 跨域访问 B 域工单列表 → 403 + 中文
        mockMvc.perform(get("/api/v1/admin/domains/{domainId}/tickets", domainB)
                        .header("Authorization", IntegrationAuthSupport.bearer(adminToken))
                        .header("X-UD-Client-Code", IntegrationAuthSupport.ADMIN_CLIENT_CODE))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("40301"))
                .andExpect(jsonPath("$.message").value("无操作权限"));

        // 本域端点保持可用 → 200（assigned_to_me=true 规避既有 listAdminTickets 三元拆箱 NPE，见任务备注）
        mockMvc.perform(get("/api/v1/admin/domains/{domainId}/tickets", domainA)
                        .param("assigned_to_me", "true")
                        .header("Authorization", IntegrationAuthSupport.bearer(adminToken))
                        .header("X-UD-Client-Code", IntegrationAuthSupport.ADMIN_CLIENT_CODE))
                .andExpect(status().isOk());
    }

    /** FR-05：客户注册但未入域（删除 domain_customer 关系）访问该域提单 → 403 + 中文。 */
    @Test
    void fr05RegisteredCustomerWithoutDomainMembershipCannotCreateTicket() throws Exception {
        long domainId = defaultDomainId(jdbcTemplate);
        long ticketTypeId = defaultTicketTypeId(jdbcTemplate, domainId);
        IntegrationAuthSupport.RegisterCustomerResult customer = IntegrationAuthSupport.registerCustomerAccessToken(
                mockMvc, objectMapper, jdbcTemplate, domainId,
                "fr05_customer_" + uniqueSuffix(), "customer123");

        // 移除入域关系，模拟「已注册但未入域」
        int deleted = jdbcTemplate.update(
                "DELETE FROM domain_customer WHERE customer_account_id = ? AND business_domain_id = ?",
                customer.accountId(), domainId);
        assertThat(deleted).as("应删除客户入域关系").isEqualTo(1);

        mockMvc.perform(post("/api/v1/domains/{domainId}/tickets", domainId)
                        .header("Authorization", IntegrationAuthSupport.bearer(customer.accessToken()))
                        .header("X-UD-Client-Code", IntegrationAuthSupport.CUSTOMER_CLIENT_CODE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TicketService.CreateTicketCommand(
                                ticketTypeId, "未入域提单", "不应成功", Map.of("channel", "web"), List.of(),
                                null, null, "web", null, List.of()))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("40301"))
                .andExpect(jsonPath("$.message").value("无操作权限"));
    }

    /** FR-01：customer 角色调用平台端点（/api/v1/admin/staff）→ 403 + 中文。 */
    @Test
    void fr01CustomerRoleForbiddenOnPlatformStaffEndpoint() throws Exception {
        long domainId = defaultDomainId(jdbcTemplate);
        String customerToken = IntegrationAuthSupport.registerCustomerAccessToken(
                mockMvc, objectMapper, jdbcTemplate, domainId,
                "fr01_customer_" + uniqueSuffix(), "customer123").accessToken();

        // 客户 token 绑定 ud-customer-web 客户端，需使用客户客户端码通过鉴权层
        mockMvc.perform(get("/api/v1/admin/staff")
                        .header("Authorization", IntegrationAuthSupport.bearer(customerToken))
                        .header("X-UD-Client-Code", IntegrationAuthSupport.CUSTOMER_CLIENT_CODE))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("40301"))
                .andExpect(jsonPath("$.message").value("无操作权限"));
    }

    /** FR-03：无 ticket.close 权限的角色（customer 有 ticket.create 但无 ticket.close）调用关闭端点 → 403 + 中文。 */
    @Test
    void fr03RoleWithoutClosePermissionForbiddenOnCloseEndpoint() throws Exception {
        long domainId = defaultDomainId(jdbcTemplate);
        long ticketTypeId = defaultTicketTypeId(jdbcTemplate, domainId);
        String customerToken = IntegrationAuthSupport.registerCustomerAccessToken(
                mockMvc, objectMapper, jdbcTemplate, domainId,
                "fr03_customer_" + uniqueSuffix(), "customer123").accessToken();
        long ticketId = createTicket(customerToken, domainId, ticketTypeId, "关闭权限工单", "客户无关闭权限");

        mockMvc.perform(patch("/api/v1/admin/domains/{domainId}/tickets/{ticketId}/status", domainId, ticketId)
                        .header("Authorization", IntegrationAuthSupport.bearer(customerToken))
                        .header("X-UD-Client-Code", IntegrationAuthSupport.CUSTOMER_CLIENT_CODE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TicketService.ChangeTicketStatusCommand(
                                "closed", ticketVersion(ticketId), null, "客户尝试关闭"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("40301"))
                .andExpect(jsonPath("$.message").value("无操作权限"));
    }

    // ---- 辅助方法 ----

    private String staffToken(long domainId, String loginName, String roleCode) throws Exception {
        IntegrationAuthSupport.insertDomainStaff(jdbcTemplate, domainId, loginName, "admin123", roleCode);
        return IntegrationAuthSupport.loginAccessToken(
                mockMvc, objectMapper, IntegrationAuthSupport.ADMIN_CLIENT_CODE, loginName, "admin123");
    }

    private long createTicket(String customerToken, long domainId, long ticketTypeId, String title, String description) throws Exception {
        String response = mockMvc.perform(post("/api/v1/domains/{domainId}/tickets", domainId)
                        .header("Authorization", IntegrationAuthSupport.bearer(customerToken))
                        .header("X-UD-Client-Code", IntegrationAuthSupport.CUSTOMER_CLIENT_CODE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TicketService.CreateTicketCommand(ticketTypeId,
                                title, description, Map.of("channel", "web"), List.of(),
                                null, null, "web", null, List.of()))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).path("data").get("id").asLong();
    }

    private long ticketVersion(long ticketId) {
        Long version = jdbcTemplate.queryForObject("SELECT version FROM ticket WHERE id = ?", Long.class, ticketId);
        assertThat(version).as("工单版本号应存在").isNotNull();
        return version;
    }

    private long insertDomain(String code) {
        jdbcTemplate.update("""
                INSERT INTO business_domain (code, name, description, visibility_policy, status, created_at, updated_at, visibility_policy_codes)
                VALUES (?, ?, '测试业务域', 'public', 1, CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3), '["public"]')
                """, code, code);
        Long id = jdbcTemplate.queryForObject(
                "SELECT id FROM business_domain WHERE code = ? LIMIT 1", Long.class, code);
        return id;
    }

    private String uniqueSuffix() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
