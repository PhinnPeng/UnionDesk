package com.uniondesk.domain.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uniondesk.auth.core.AuthCaptchaService;
import com.uniondesk.support.IntegrationAuthSupport;
import java.util.List;
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
 * 角色模板 P1-1 集成测试（08-11-group-role-management）：
 * 模板创建 + 多域 apply 生成实例 + 满额域跳过 + 锁定字段 403 + manual 同步版本推进。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class RoleTemplateIntegrationTest {

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
        ensureSuperAdminRoleTemplatePermissions();
    }

    @Test
    void applyTemplateToMultipleDomainsCreatesInstances() throws Exception {
        long domainA = IntegrationAuthSupport.activeDefaultDomainId(jdbcTemplate);
        long domainB = insertTestDomain("rt_domain_b");
        String accessToken = IntegrationAuthSupport.adminAccessToken(mockMvc, objectMapper);
        long permissionItemId = firstPermissionItemId();

        long templateId = createTemplate(accessToken, "rt_ops", "运营管理员", permissionItemId, "manual");

        mockMvc.perform(post("/api/v1/iam/role-templates/{templateId}/apply", templateId)
                        .header("X-UD-Client-Code", IntegrationAuthSupport.ADMIN_CLIENT_CODE)
                        .header("Authorization", IntegrationAuthSupport.bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "domain_ids": [%d, %d],
                                  "sync_mode": "immediate"
                                }
                                """.formatted(domainA, domainB)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.success.length()").value(2));

        Integer instanceCount = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM domain_role
                        WHERE template_id = ? AND code = 'rt_ops'
                        """,
                Integer.class,
                templateId);
        org.assertj.core.api.Assertions.assertThat(instanceCount).isEqualTo(2);

        Integer appliedCount = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM role_template_domain
                        WHERE template_id = ?
                        """,
                Integer.class,
                templateId);
        org.assertj.core.api.Assertions.assertThat(appliedCount).isEqualTo(2);

        Integer instanceVersion = jdbcTemplate.queryForObject(
                """
                        SELECT template_version
                        FROM domain_role
                        WHERE business_domain_id = ? AND code = 'rt_ops'
                        LIMIT 1
                        """,
                Integer.class,
                domainA);
        org.assertj.core.api.Assertions.assertThat(instanceVersion).isEqualTo(1);
    }

    @Test
    void applySkipsDomainWithFullCustomRoles() throws Exception {
        long domainA = IntegrationAuthSupport.activeDefaultDomainId(jdbcTemplate);
        long domainB = insertTestDomain("rt_domain_full");
        insertCustomRoles(domainB, 20);
        String accessToken = IntegrationAuthSupport.adminAccessToken(mockMvc, objectMapper);
        long permissionItemId = firstPermissionItemId();
        long templateId = createTemplate(accessToken, "rt_full", "满额测试模板", permissionItemId, "manual");

        mockMvc.perform(post("/api/v1/iam/role-templates/{templateId}/apply", templateId)
                        .header("X-UD-Client-Code", IntegrationAuthSupport.ADMIN_CLIENT_CODE)
                        .header("Authorization", IntegrationAuthSupport.bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "domain_ids": [%d, %d],
                                  "sync_mode": "immediate"
                                }
                                """.formatted(domainA, domainB)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.success.length()").value(1))
                .andExpect(jsonPath("$.data.success[0]").value((int) domainA))
                .andExpect(jsonPath("$.data.skipped.length()").value(1))
                .andExpect(jsonPath("$.data.skipped[0].domain_id").value((int) domainB))
                .andExpect(jsonPath("$.data.skipped[0].reason").value(org.hamcrest.Matchers.containsString("上限")));
    }

    @Test
    void domainCannotUpdateLockedPermissionPackageOfTemplateInstance() throws Exception {
        long domainId = IntegrationAuthSupport.activeDefaultDomainId(jdbcTemplate);
        String adminToken = IntegrationAuthSupport.adminAccessToken(mockMvc, objectMapper);
        long permissionItemId = firstPermissionItemId();
        long templateId = createTemplate(adminToken, "rt_locked", "锁定测试模板", permissionItemId, "manual");

        mockMvc.perform(post("/api/v1/iam/role-templates/{templateId}/apply", templateId)
                        .header("X-UD-Client-Code", IntegrationAuthSupport.ADMIN_CLIENT_CODE)
                        .header("Authorization", IntegrationAuthSupport.bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "domain_ids": [%d],
                                  "sync_mode": "immediate"
                                }
                                """.formatted(domainId)))
                .andExpect(status().isOk());

        Long roleId = jdbcTemplate.queryForObject(
                """
                        SELECT id
                        FROM domain_role
                        WHERE business_domain_id = ? AND code = 'rt_locked'
                        LIMIT 1
                        """,
                Long.class,
                domainId);

        // 域端管理员（domain 角色）修改模板实例权限包 → 403 + 中文（锁定字段）
        IntegrationAuthSupport.insertDomainStaff(
                jdbcTemplate, domainId, "rt_domain_admin", "admin123", "domain_admin");
        String domainAdminToken = IntegrationAuthSupport.loginAccessToken(
                mockMvc, objectMapper, IntegrationAuthSupport.ADMIN_CLIENT_CODE, "rt_domain_admin", "admin123");

        mockMvc.perform(put("/api/v1/admin/domains/{domainId}/roles/{roleId}/permissions", domainId, roleId)
                        .header("X-UD-Client-Code", IntegrationAuthSupport.ADMIN_CLIENT_CODE)
                        .header("Authorization", IntegrationAuthSupport.bearer(domainAdminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "permission_item_ids": []
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("锁定")));

        // 非锁定字段（名称）域端仍可微调
        mockMvc.perform(put("/api/v1/admin/domains/{domainId}/roles/{roleId}", domainId, roleId)
                        .header("X-UD-Client-Code", IntegrationAuthSupport.ADMIN_CLIENT_CODE)
                        .header("Authorization", IntegrationAuthSupport.bearer(domainAdminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "锁定测试模板-域内改名"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("锁定测试模板-域内改名"));
    }

    @Test
    void manualSyncPropagatesTemplateVersionToInstances() throws Exception {
        long domainId = IntegrationAuthSupport.activeDefaultDomainId(jdbcTemplate);
        String accessToken = IntegrationAuthSupport.adminAccessToken(mockMvc, objectMapper);
        long permissionItemId = firstPermissionItemId();
        long templateId = createTemplate(accessToken, "rt_sync", "同步测试模板", permissionItemId, "manual");

        mockMvc.perform(post("/api/v1/iam/role-templates/{templateId}/apply", templateId)
                        .header("X-UD-Client-Code", IntegrationAuthSupport.ADMIN_CLIENT_CODE)
                        .header("Authorization", IntegrationAuthSupport.bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "domain_ids": [%d],
                                  "sync_mode": "manual"
                                }
                                """.formatted(domainId)))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/v1/iam/role-templates/{templateId}", templateId)
                        .header("X-UD-Client-Code", IntegrationAuthSupport.ADMIN_CLIENT_CODE)
                        .header("Authorization", IntegrationAuthSupport.bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "同步测试模板-改",
                                  "sync_strategy": "manual",
                                  "locked_fields": ["permissions"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.version").value(2));

        mockMvc.perform(post("/api/v1/iam/role-templates/{templateId}/sync", templateId)
                        .header("X-UD-Client-Code", IntegrationAuthSupport.ADMIN_CLIENT_CODE)
                        .header("Authorization", IntegrationAuthSupport.bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "domain_ids": [%d]
                                }
                                """.formatted(domainId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.success.length()").value(1));

        Integer instanceVersion = jdbcTemplate.queryForObject(
                """
                        SELECT template_version
                        FROM domain_role
                        WHERE business_domain_id = ? AND code = 'rt_sync'
                        LIMIT 1
                        """,
                Integer.class,
                domainId);
        org.assertj.core.api.Assertions.assertThat(instanceVersion).isEqualTo(2);

        mockMvc.perform(get("/api/v1/iam/role-templates/{templateId}", templateId)
                        .header("X-UD-Client-Code", IntegrationAuthSupport.ADMIN_CLIENT_CODE)
                        .header("Authorization", IntegrationAuthSupport.bearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.applied_domains[0].instance_version").value(2));
    }

    private long createTemplate(
            String accessToken,
            String code,
            String name,
            long permissionItemId,
            String syncStrategy) throws Exception {
        String response = mockMvc.perform(post("/api/v1/iam/role-templates")
                        .header("X-UD-Client-Code", IntegrationAuthSupport.ADMIN_CLIENT_CODE)
                        .header("Authorization", IntegrationAuthSupport.bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "%s",
                                  "name": "%s",
                                  "description": "%s",
                                  "sync_strategy": "%s",
                                  "locked_fields": ["permissions"],
                                  "permission_item_ids": [%d]
                                }
                                """.formatted(code, name, name + "描述", syncStrategy, permissionItemId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).path("data").get("id").asLong();
    }

    private long firstPermissionItemId() {
        Long id = jdbcTemplate.queryForObject(
                "SELECT id FROM permission_item ORDER BY id ASC LIMIT 1",
                Long.class);
        if (id == null) {
            throw new IllegalStateException("permission_item catalog is empty");
        }
        return id;
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

    private void insertCustomRoles(long domainId, int count) {
        for (int i = 0; i < count; i++) {
            jdbcTemplate.update("""
                            INSERT INTO domain_role (business_domain_id, code, name, preset, created_at, updated_at)
                            VALUES (?, ?, ?, 0, CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3))
                            """,
                    domainId,
                    "rt_fill_" + i,
                    "填充角色" + i);
        }
    }

    private void ensureSuperAdminRoleTemplatePermissions() {
        for (String code : List.of(
                "platform.role_template.read",
                "platform.role_template.create",
                "platform.role_template.update",
                "platform.role_template.delete",
                "platform.role_template.apply",
                "platform.role_template.sync")) {
            jdbcTemplate.update("""
                            INSERT IGNORE INTO iam_role_permission (role_id, permission_id, created_at)
                            SELECT r.id, p.id, CURRENT_TIMESTAMP(3)
                            FROM role r
                            JOIN iam_permission p ON p.code = ? AND p.status = 1
                            WHERE r.code IN ('super_admin', 'platform_admin')
                            """,
                    code);
        }
    }
}
