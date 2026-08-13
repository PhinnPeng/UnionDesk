package com.uniondesk.domain.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uniondesk.auth.core.AuthCaptchaService;
import com.uniondesk.support.IntegrationAuthSupport;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * P1-2 跨域批量停用集成测试（08-11-group-role-management，design §5/AC3/AC7）：
 * 逐域部分成功（TR-04）+ 逐域审计（point=platform）+ step-up 令牌缺失/无效 → 403。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class StaffDomainMemberBatchIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private AuthCaptchaService authCaptchaService;

    @BeforeEach
    void setUp() {
        IntegrationAuthSupport.mockCaptchaBypass(authCaptchaService);
        IntegrationAuthSupport.ensureActiveDefaultDomain(jdbcTemplate);
        ensureBatchStatusPermission();
    }

    @Test
    void batchStatusDisablesMemberPerDomainWithPartialSuccessAndAudit() throws Exception {
        long domainA = IntegrationAuthSupport.activeDefaultDomainId(jdbcTemplate);
        long domainB = insertTestDomain("bs_domain_b");
        long staffId = IntegrationAuthSupport.insertDomainStaff(
                jdbcTemplate, domainA, "bs_staff_a", "admin123", "ops");
        String adminToken = IntegrationAuthSupport.adminAccessToken(mockMvc, objectMapper);
        String stepUpToken = stepUpToken(adminToken);

        mockMvc.perform(post("/api/v1/admin/staff/{staffId}/domain-members/batch-status", staffId)
                        .header("X-UD-Client-Code", IntegrationAuthSupport.ADMIN_CLIENT_CODE)
                        .header("Authorization", IntegrationAuthSupport.bearer(adminToken))
                        .header("X-UD-Step-Up-Token", stepUpToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "domain_ids": [%d, %d],
                                  "status": "disabled"
                                }
                                """.formatted(domainA, domainB)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.success.length()").value(1))
                .andExpect(jsonPath("$.data.success[0]").value((int) domainA))
                .andExpect(jsonPath("$.data.failed.length()").value(1))
                .andExpect(jsonPath("$.data.failed[0].domain_id").value((int) domainB))
                .andExpect(jsonPath("$.data.failed[0].reason").value(
                        org.hamcrest.Matchers.containsString("未加入")));

        // 域 A 成员已停用
        String status = jdbcTemplate.queryForObject(
                """
                        SELECT status
                        FROM domain_member
                        WHERE staff_account_id = ? AND business_domain_id = ?
                        """,
                String.class,
                staffId,
                domainA);
        Assertions.assertThat(status).isEqualTo("disabled");

        // 逐域写审计（point=platform，business_domain_id 落库）
        Integer auditRows = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM audit_log
                        WHERE action = 'platform.user.domain_batch_status'
                          AND point = 'platform'
                          AND business_domain_id IN (?, ?)
                        """,
                Integer.class,
                domainA,
                domainB);
        Assertions.assertThat(auditRows).isEqualTo(2);
    }

    @Test
    void batchStatusWithoutStepUpTokenReturnsForbidden() throws Exception {
        long domainA = IntegrationAuthSupport.activeDefaultDomainId(jdbcTemplate);
        long staffId = IntegrationAuthSupport.insertDomainStaff(
                jdbcTemplate, domainA, "bs_staff_b", "admin123", "ops");
        String adminToken = IntegrationAuthSupport.adminAccessToken(mockMvc, objectMapper);

        mockMvc.perform(post("/api/v1/admin/staff/{staffId}/domain-members/batch-status", staffId)
                        .header("X-UD-Client-Code", IntegrationAuthSupport.ADMIN_CLIENT_CODE)
                        .header("Authorization", IntegrationAuthSupport.bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "domain_ids": [%d],
                                  "status": "disabled"
                                }
                                """.formatted(domainA)))
                .andExpect(status().isForbidden());
    }

    @Test
    void batchStatusWithInvalidStepUpTokenReturnsForbidden() throws Exception {
        long domainA = IntegrationAuthSupport.activeDefaultDomainId(jdbcTemplate);
        long staffId = IntegrationAuthSupport.insertDomainStaff(
                jdbcTemplate, domainA, "bs_staff_c", "admin123", "ops");
        String adminToken = IntegrationAuthSupport.adminAccessToken(mockMvc, objectMapper);

        mockMvc.perform(post("/api/v1/admin/staff/{staffId}/domain-members/batch-status", staffId)
                        .header("X-UD-Client-Code", IntegrationAuthSupport.ADMIN_CLIENT_CODE)
                        .header("Authorization", IntegrationAuthSupport.bearer(adminToken))
                        .header("X-UD-Step-Up-Token", "not-a-real-step-up-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "domain_ids": [%d],
                                  "status": "disabled"
                                }
                                """.formatted(domainA)))
                .andExpect(status().isForbidden());
    }

    private String stepUpToken(String adminToken) throws Exception {
        String response = mockMvc.perform(post("/api/v1/auth/step-up")
                        .header("X-UD-Client-Code", IntegrationAuthSupport.ADMIN_CLIENT_CODE)
                        .header("Authorization", IntegrationAuthSupport.bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "password": "admin123",
                                  "operation_code": "staff.domain_batch_status"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).path("data").get("stepUpToken").asText();
    }

    private long insertTestDomain(String code) {
        jdbcTemplate.update("""
                        INSERT INTO business_domain (code, name, visibility_policy, status, created_at, updated_at)
                        VALUES (?, ?, 'global', 1, CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3))
                        """,
                code,
                code);
        Long domainId = jdbcTemplate.queryForObject(
                "SELECT id FROM business_domain WHERE code = ? LIMIT 1",
                Long.class,
                code);
        if (domainId == null) {
            throw new IllegalStateException("test domain not created: " + code);
        }
        return domainId;
    }

    private void ensureBatchStatusPermission() {
        jdbcTemplate.update("""
                        INSERT IGNORE INTO iam_permission (
                            code, name, description, permission_scope, resource_code, action_code,
                            http_method, path_pattern, status
                        )
                        VALUES (
                            'platform.user.domain_batch_status', '跨域批量停用成员', '跨域批量停用成员', 'platform',
                            'platform.user.domain_batch_status', 'platform.user.domain_batch_status',
                            'POST', '/api/v1/admin/staff/*/domain-members/batch-status', 1
                        )
                        """);
        jdbcTemplate.update("""
                        INSERT IGNORE INTO iam_role_permission (role_id, permission_id, created_at)
                        SELECT r.id, p.id, CURRENT_TIMESTAMP(3)
                        FROM role r
                        JOIN iam_permission p ON p.code = ? AND p.status = 1
                        WHERE r.code IN ('super_admin', 'platform_admin')
                        """,
                "platform.user.domain_batch_status");
    }
}
