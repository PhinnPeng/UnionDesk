package com.uniondesk.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uniondesk.auth.core.AuthCaptchaService;
import java.util.List;
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
        return objectMapper.readTree(response).path("data").get("accessToken").asText();
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
        long accountId = json.path("data").get("accountId").asLong();
        grantCustomerDomainPermissions(jdbcTemplate, accountId, domainId);
        return new RegisterCustomerResult(json.path("data").get("accessToken").asText(), accountId);
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
     * 确保客户账号在域内存在 active 的 domain_customer 关系（注册流程已建，此方法用于兜底/幂等）。
     */
    public static void grantCustomerDomainPermissions(JdbcTemplate jdbcTemplate, long customerAccountId, long domainId) {
        jdbcTemplate.update("""
                        INSERT INTO domain_customer (
                            customer_account_id, business_domain_id, status, source,
                            activated_at, disabled_at, deleted_at, created_at, updated_at
                        )
                        VALUES (?, ?, 'active', 'self_register',
                            CURRENT_TIMESTAMP(3), NULL, NULL, CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3))
                        ON DUPLICATE KEY UPDATE
                            status = 'active',
                            deleted_at = NULL,
                            activated_at = COALESCE(activated_at, CURRENT_TIMESTAMP(3))
                        """,
                customerAccountId,
                domainId);
    }

    /**
     * 新建业务域员工账号（真实登录用）：
     * 插入 identity_subject + staff_account（{noop} 测试密码占位）+ domain_member(active) + domain_member_role 绑定，
     * 并确保 domain_role 存在（preset 角色直接复用，缺失按需插入）。
     * 返回 staff_account.id，可配合 {@link #loginAccessToken} 真实登录换取 accessToken。
     */
    public static long insertDomainStaff(
            JdbcTemplate jdbcTemplate,
            long domainId,
            String loginName,
            String password,
            String roleCode) {
        String phone = "139" + String.format("%08d", Math.floorMod(loginName.hashCode(), 100000000));
        jdbcTemplate.update("""
                        INSERT INTO identity_subject (subject_type, phone, status)
                        VALUES ('person', ?, 'active')
                        """,
                phone);
        Long subjectId = jdbcTemplate.queryForObject(
                "SELECT id FROM identity_subject WHERE phone = ? LIMIT 1",
                Long.class,
                phone);
        if (subjectId == null) {
            throw new IllegalStateException("identity subject not created: " + loginName);
        }
        jdbcTemplate.update("""
                        INSERT INTO staff_account (
                            subject_id, username, real_name, nickname, phone, email, password_hash,
                            must_change_password, status, employment_status, source, auth_version,
                            password_changed_at, created_at, updated_at
                        )
                        VALUES (?, ?, ?, ?, ?, ?, CONCAT('{noop}', ?), 0, 'active', 'active', 'test', 1,
                            CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3))
                        """,
                subjectId,
                loginName,
                loginName,
                loginName,
                phone,
                loginName + "@uniondesk.local",
                password);
        Long staffAccountId = jdbcTemplate.queryForObject(
                "SELECT id FROM staff_account WHERE username = ? LIMIT 1",
                Long.class,
                loginName);
        if (staffAccountId == null) {
            throw new IllegalStateException("staff account not created: " + loginName);
        }
        jdbcTemplate.update("""
                        INSERT INTO domain_member (
                            staff_account_id, business_domain_id, status, source,
                            activated_at, disabled_at, deleted_at, created_at, updated_at
                        )
                        VALUES (?, ?, 'active', 'test',
                            CURRENT_TIMESTAMP(3), NULL, NULL, CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3))
                        """,
                staffAccountId,
                domainId);
        Long domainMemberId = jdbcTemplate.queryForObject(
                """
                        SELECT id
                        FROM domain_member
                        WHERE staff_account_id = ? AND business_domain_id = ?
                        LIMIT 1
                        """,
                Long.class,
                staffAccountId,
                domainId);
        Long domainRoleId = ensureDomainRole(jdbcTemplate, domainId, roleCode);
        if (domainMemberId == null) {
            throw new IllegalStateException("domain member not created: " + loginName);
        }
        jdbcTemplate.update("""
                        INSERT INTO domain_member_role (domain_member_id, domain_role_id, created_at)
                        VALUES (?, ?, CURRENT_TIMESTAMP(3))
                        """,
                domainMemberId,
                domainRoleId);
        return staffAccountId;
    }

    /**
     * 确保域内存在指定 code 的 domain_role（preset 角色复用，缺失按需插入），返回其 id。
     */
    private static long ensureDomainRole(JdbcTemplate jdbcTemplate, long domainId, String roleCode) {
        List<Long> ids = jdbcTemplate.query(
                """
                        SELECT id
                        FROM domain_role
                        WHERE business_domain_id = ? AND code = ?
                        LIMIT 1
                        """,
                (rs, rowNum) -> rs.getLong(1),
                domainId,
                roleCode);
        if (!ids.isEmpty()) {
            return ids.get(0);
        }
        String roleName = roleCode;
        List<String> names = jdbcTemplate.query(
                "SELECT name FROM role WHERE code = ? LIMIT 1",
                (rs, rowNum) -> rs.getString(1),
                roleCode);
        if (!names.isEmpty()) {
            roleName = names.get(0);
        }
        jdbcTemplate.update("""
                        INSERT INTO domain_role (business_domain_id, code, name, preset, created_at, updated_at)
                        VALUES (?, ?, ?, 0, CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3))
                        """,
                domainId,
                roleCode,
                roleName);
        Long domainRoleId = jdbcTemplate.queryForObject(
                """
                        SELECT id
                        FROM domain_role
                        WHERE business_domain_id = ? AND code = ?
                        LIMIT 1
                        """,
                Long.class,
                domainId,
                roleCode);
        if (domainRoleId == null) {
            throw new IllegalStateException("domain role not created: " + roleCode);
        }
        return domainRoleId;
    }
}
