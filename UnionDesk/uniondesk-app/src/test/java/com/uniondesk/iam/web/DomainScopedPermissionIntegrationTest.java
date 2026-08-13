package com.uniondesk.iam.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uniondesk.auth.core.AuthCaptchaService;
import com.uniondesk.support.IntegrationAuthSupport;
import java.util.UUID;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * P0-① 目标域校验（US-S1-08）集成测试。
 *
 * <p>验证 {@code @RequirePermission(domainIdParam)} 生效后：
 * <ul>
 *   <li>A 域 domain_admin 访问 B 域端点 → 403（跨域越权被封堵）</li>
 *   <li>A 域 domain_admin 访问 A 域端点 → 200（本域不受影响）</li>
 *   <li>平台 super_admin 访问任意域端点 → 200（平台豁免保持）</li>
 *   <li>未声明 domainIdParam 的端点（域列表）行为不变 → 200</li>
 *   <li>非法 domainId 路径 → 403（fail-closed）</li>
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class DomainScopedPermissionIntegrationTest {

    private static final String DOMAIN_ADMIN_PASSWORD = "domainadmin123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private AuthCaptchaService authCaptchaService;

    private long domainA;
    private long domainB;
    private String domainAdminToken;

    @BeforeEach
    void setUp() throws Exception {
        IntegrationAuthSupport.mockCaptchaBypass(authCaptchaService);
        String unique = UUID.randomUUID().toString().substring(0, 8);
        domainA = insertDomain("it-" + unique + "-a");
        domainB = insertDomain("it-" + unique + "-b");
        insertDomainAdmin(domainA, "da-" + unique);
        ensurePermission("domain_admin", "domain.member.read", "domain");
        ensurePermission("super_admin", "platform.domain.control.member.read", "platform");
        ensurePermission("platform_admin", "platform.domain.control.member.read", "platform");
        ensurePermission("super_admin", "platform.domain.list.read", "platform");
        ensurePermission("platform_admin", "platform.domain.list.read", "platform");
        domainAdminToken = loginAccessToken("da-" + unique, DOMAIN_ADMIN_PASSWORD);
    }

    @Test
    void domainAdminOfDomainAForbiddenOnDomainB() throws Exception {
        mockMvc.perform(get("/api/v1/admin/domains/{domainId}/members", domainB)
                        .header("X-UD-Client-Code", IntegrationAuthSupport.ADMIN_CLIENT_CODE)
                        .header("Authorization", IntegrationAuthSupport.bearer(domainAdminToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("40301"))
                .andExpect(jsonPath("$.message").value("无操作权限"));
    }

    @Test
    void domainAdminOfDomainAAllowedOnOwnDomain() throws Exception {
        mockMvc.perform(get("/api/v1/admin/domains/{domainId}/members", domainA)
                        .header("X-UD-Client-Code", IntegrationAuthSupport.ADMIN_CLIENT_CODE)
                        .header("Authorization", IntegrationAuthSupport.bearer(domainAdminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"));
    }

    @Test
    void platformSuperAdminExemptFromDomainScope() throws Exception {
        String adminToken = loginAccessToken("admin", "admin123");
        mockMvc.perform(get("/api/v1/admin/domains/{domainId}/members", domainB)
                        .header("X-UD-Client-Code", IntegrationAuthSupport.ADMIN_CLIENT_CODE)
                        .header("Authorization", IntegrationAuthSupport.bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"));
    }

    @Test
    void endpointWithoutDomainIdParamKeepsOriginalBehavior() throws Exception {
        String adminToken = loginAccessToken("admin", "admin123");
        mockMvc.perform(get("/api/v1/admin/domains")
                        .header("X-UD-Client-Code", IntegrationAuthSupport.ADMIN_CLIENT_CODE)
                        .header("Authorization", IntegrationAuthSupport.bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"));
    }

    @Test
    void invalidDomainIdPathRejectedWithForbidden() throws Exception {
        String adminToken = loginAccessToken("admin", "admin123");
        mockMvc.perform(get("/api/v1/admin/domains/abc/members")
                        .header("X-UD-Client-Code", IntegrationAuthSupport.ADMIN_CLIENT_CODE)
                        .header("Authorization", IntegrationAuthSupport.bearer(adminToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("40301"));
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

    private void insertDomainAdmin(long domainId, String username) {
        String phone = "139" + String.format("%08d", Math.floorMod(username.hashCode(), 100000000));
        jdbcTemplate.update("""
                INSERT INTO identity_subject (subject_type, phone, status)
                VALUES ('person', ?, 'active')
                """, phone);
        Long subjectId = jdbcTemplate.queryForObject(
                "SELECT id FROM identity_subject WHERE phone = ? LIMIT 1", Long.class, phone);
        jdbcTemplate.update("""
                INSERT INTO staff_account (subject_id, username, real_name, nickname, phone, email, password_hash,
                    must_change_password, status, employment_status, source, auth_version, password_changed_at, created_at, updated_at)
                VALUES (?, ?, '域管理员', '域管理员', ?, ?, CONCAT('{noop}', ?), 0, 'active', 'active', 'local', 1,
                    CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3))
                """, subjectId, username, phone, username + "@uniondesk.local", DOMAIN_ADMIN_PASSWORD);
        Long staffId = jdbcTemplate.queryForObject(
                "SELECT id FROM staff_account WHERE username = ? LIMIT 1", Long.class, username);
        jdbcTemplate.update("""
                INSERT INTO domain_role (business_domain_id, code, name, preset, created_at, updated_at)
                VALUES (?, 'domain_admin', '业务域管理员', 1, CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3))
                """, domainId);
        Long domainRoleId = jdbcTemplate.queryForObject(
                "SELECT id FROM domain_role WHERE business_domain_id = ? AND code = 'domain_admin' LIMIT 1",
                Long.class, domainId);
        jdbcTemplate.update("""
                INSERT INTO domain_member (staff_account_id, business_domain_id, status, source, activated_at, created_at, updated_at)
                VALUES (?, ?, 'active', 'manual', CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3))
                """, staffId, domainId);
        Long memberId = jdbcTemplate.queryForObject(
                "SELECT id FROM domain_member WHERE staff_account_id = ? AND business_domain_id = ? LIMIT 1",
                Long.class, staffId, domainId);
        jdbcTemplate.update("""
                INSERT INTO domain_member_role (domain_member_id, domain_role_id, created_at)
                VALUES (?, ?, CURRENT_TIMESTAMP(3))
                """, memberId, domainRoleId);
    }

    private void ensurePermission(String roleCode, String permissionCode, String permissionScope) {
        jdbcTemplate.update("""
                INSERT INTO iam_permission (code, name, description, permission_scope, resource_code, action_code, status, created_at, updated_at)
                SELECT ?, ?, ?, ?, ?, ?, 1, CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3)
                FROM DUAL
                WHERE NOT EXISTS (SELECT 1 FROM iam_permission WHERE code = ?)
                """, permissionCode, permissionCode, permissionCode, permissionScope,
                "it-" + permissionCode, "it-" + permissionCode, permissionCode);
        jdbcTemplate.update("""
                INSERT IGNORE INTO iam_role_permission (role_id, permission_id, created_at)
                SELECT r.id, p.id, CURRENT_TIMESTAMP(3)
                FROM role r
                JOIN iam_permission p ON p.code = ?
                WHERE r.code = ?
                """, permissionCode, roleCode);
    }

    private String loginAccessToken(String username, String password) throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/login")
                        .header("X-UD-Client-Code", IntegrationAuthSupport.ADMIN_CLIENT_CODE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "%s",
                                  "captcha_token": "%s"
                                }
                                """.formatted(username, password, IntegrationAuthSupport.TEST_CAPTCHA_TOKEN)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(body).path("data").path("accessToken").asText();
    }
}
