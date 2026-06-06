package com.uniondesk.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uniondesk.auth.core.AuthCaptchaService;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 集成测试鉴权前置：验证码绕过、登录/注册获取 accessToken。
 */
public final class IntegrationAuthSupport {

    public static final String ADMIN_CLIENT_CODE = "ud-admin-web";
    public static final String CUSTOMER_CLIENT_CODE = "ud-customer-web";
    public static final String TEST_CAPTCHA_TOKEN = "test-captcha-token";

    private IntegrationAuthSupport() {
    }

    public static void mockCaptchaBypass(AuthCaptchaService authCaptchaService) {
        doNothing().when(authCaptchaService).consumeToken(anyString());
    }

    public static String bearer(String accessToken) {
        return "Bearer " + accessToken;
    }

    public static String loginAccessToken(
            MockMvc mockMvc,
            ObjectMapper objectMapper,
            String clientCode,
            String username,
            String password) throws Exception {
        String response = mockMvc.perform(post("/api/v1/auth/login")
                        .header("X-UD-Client-Code", clientCode)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "%s",
                                  "captcha_token": "%s"
                                }
                                """.formatted(username, password, TEST_CAPTCHA_TOKEN)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).get("accessToken").asText();
    }

    public static String adminAccessToken(MockMvc mockMvc, ObjectMapper objectMapper) throws Exception {
        return loginAccessToken(mockMvc, objectMapper, ADMIN_CLIENT_CODE, "admin", "admin123");
    }

    public static long activeDefaultDomainId(JdbcTemplate jdbcTemplate) {
        ensureActiveDefaultDomain(jdbcTemplate);
        Long domainId = jdbcTemplate.queryForObject(
                """
                        SELECT id
                        FROM business_domain
                        WHERE code = 'default'
                          AND deleted_at IS NULL
                        LIMIT 1
                        """,
                Long.class);
        if (domainId == null) {
            throw new IllegalStateException("active default business domain not found");
        }
        return domainId;
    }

    public static void ensureActiveDefaultDomain(JdbcTemplate jdbcTemplate) {
        jdbcTemplate.update("""
                        UPDATE business_domain
                        SET deleted_at = NULL,
                            registration_enabled = 'allowed',
                            invitation_enabled = 'allowed'
                        WHERE code = 'default'
                        """);
    }

    public static void ensureSuperAdminDomainControlPermissions(JdbcTemplate jdbcTemplate) {
        jdbcTemplate.update("""
                        INSERT IGNORE INTO iam_role_permission (role_id, permission_id, created_at)
                        SELECT r.id, p.id, CURRENT_TIMESTAMP(3)
                        FROM role r
                        JOIN iam_permission p ON p.code = ? AND p.status = 1
                        WHERE r.code = 'super_admin'
                        """,
                "platform.domain.control.general.update");
        jdbcTemplate.update("""
                        INSERT IGNORE INTO iam_role_permission (role_id, permission_id, created_at)
                        SELECT r.id, p.id, CURRENT_TIMESTAMP(3)
                        FROM role r
                        JOIN iam_permission p ON p.code = ? AND p.status = 1
                        WHERE r.code = 'super_admin'
                        """,
                "platform.domain.control.general.update-status");
    }

    public record RegisterCustomerResult(String accessToken, long accountId) {
    }

    public static RegisterCustomerResult registerCustomerAccessToken(
            MockMvc mockMvc,
            ObjectMapper objectMapper,
            JdbcTemplate jdbcTemplate,
            long domainId,
            String username,
            String password) throws Exception {
        ensureDomainRegistrationAllowed(jdbcTemplate, domainId);
        String phoneSuffix = String.format("%04d", Math.abs(username.hashCode()) % 10000);
        String phone = "1380000" + phoneSuffix;
        String email = username + "@uniondesk.local";
        String response = mockMvc.perform(post("/api/v1/auth/register")
                        .header("X-UD-Client-Code", CUSTOMER_CLIENT_CODE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "loginName": "%s",
                                  "password": "%s",
                                  "displayName": "%s",
                                  "phone": "%s",
                                  "email": "%s",
                                  "domainId": %d,
                                  "captchaToken": "%s"
                                }
                                """.formatted(username, password, username, phone, email, domainId, TEST_CAPTCHA_TOKEN)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode json = objectMapper.readTree(response);
        long accountId = json.get("accountId").asLong();
        grantCustomerDomainPermissions(jdbcTemplate, accountId, domainId);
        return new RegisterCustomerResult(json.get("accessToken").asText(), accountId);
    }

    public static void ensureDomainRegistrationAllowed(JdbcTemplate jdbcTemplate, long domainId) {
        jdbcTemplate.update("""
                        UPDATE business_domain
                        SET registration_enabled = 'allowed',
                            invitation_enabled = 'allowed',
                            deleted_at = NULL
                        WHERE id = ?
                        """,
                domainId);
    }

    /**
     * 为客户账号绑定 domain 级 customer 角色，使 ticket.create 等权限生效。
     * IAM 绑定表仍引用 user_account，需同步桥接行（待 IAM FK 迁移后移除）。
     */
    public static void grantCustomerDomainPermissions(JdbcTemplate jdbcTemplate, long customerAccountId, long domainId) {
        jdbcTemplate.update("""
                        INSERT INTO user_account (id, username, mobile, email, password_hash, status, account_type)
                        SELECT ca.id, ca.username, ca.phone, ca.email, ca.password_hash, 1, 'customer'
                        FROM customer_account ca
                        WHERE ca.id = ?
                        ON DUPLICATE KEY UPDATE
                            username = VALUES(username),
                            mobile = VALUES(mobile),
                            email = VALUES(email),
                            password_hash = VALUES(password_hash),
                            status = VALUES(status),
                            account_type = VALUES(account_type)
                        """,
                customerAccountId);
        jdbcTemplate.update("""
                        INSERT INTO iam_role_binding (user_id, role_id, binding_scope, business_domain_id, status)
                        SELECT ?, r.id, 'domain', ?, 1
                        FROM role r
                        WHERE r.code = 'customer'
                        ON DUPLICATE KEY UPDATE
                            binding_scope = VALUES(binding_scope),
                            business_domain_id = VALUES(business_domain_id),
                            status = VALUES(status)
                        """,
                customerAccountId,
                domainId);
    }
}
