package com.uniondesk.audit.core;

import static org.assertj.core.api.Assertions.assertThat;

import com.uniondesk.auth.core.UserContext;
import com.uniondesk.auth.core.UserContextHolder;
import com.uniondesk.domain.core.DomainRoleService;
import com.uniondesk.domain.core.DomainService;
import com.uniondesk.domain.web.DomainDtos;
import com.uniondesk.domain.web.DomainRoleDtos;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * P0-② 审计日志 point 端点标记集成测试。
 *
 * <p>审计写入方：
 * <ul>
 *   <li>域角色 create（{@link DomainRoleService#createRole}，事件驱动 AFTER_COMMIT 异步落库）→ point='domain'</li>
 *   <li>平台域 create（{@link DomainService#createDomain}，同步落库）→ point='platform'</li>
 * </ul>
 *
 * <p>注意：本测试不使用 {@code @Transactional}（AFTER_COMMIT 异步监听器在事务回滚时不触发），
 * 由 {@link #tearDown()} 清理本测试产生的数据。
 */
@SpringBootTest
@ActiveProfiles("test")
class AuditLogPointTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DomainRoleService domainRoleService;

    @Autowired
    private DomainService domainService;

    private final List<Long> createdDomainIds = new ArrayList<>();

    @BeforeEach
    void setUp() {
        UserContextHolder.set(new UserContext(2L, "super_admin", null, "sid-test", "ud-admin-web"));
    }

    @AfterEach
    void tearDown() {
        UserContextHolder.clear();
        if (!createdDomainIds.isEmpty()) {
            deleteDomainData(createdDomainIds);
        }
    }

    @Test
    void domainRoleCreateWritesAuditWithDomainPoint() throws Exception {
        String unique = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        long domainId = insertDomain("pt-" + unique);
        String roleCode = "pt-role-" + unique;
        String roleName = "审计测试角色";
        domainRoleService.createRole(domainId, new DomainRoleDtos.CreateDomainRoleRequest(roleCode, roleName));

        Map<String, Object> row = awaitAuditRow("domain.role.create", domainId);
        assertThat(row.get("point")).isEqualTo("domain");
        assertThat(row.get("action")).isEqualTo("domain.role.create");
        assertThat(row.get("target")).isEqualTo(roleName + "-" + roleCode);
        assertThat(String.valueOf(row.get("business_domain_id"))).isEqualTo(String.valueOf(domainId));
    }

    @Test
    void platformDomainCreateWritesAuditWithPlatformPoint() {
        String unique = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String code = "ptd-" + unique;
        DomainDtos.DomainCreateResponse created = domainService.createDomain(new DomainDtos.CreateDomainRequest(
                code,
                "审计平台域测试",
                null,
                "/default-domain-logo.svg",
                List.of("public"),
                "allowed",
                "allowed",
                null));
        createdDomainIds.add(created.id());

        Map<String, Object> row = jdbcTemplate.queryForMap("""
                        SELECT point, action, target
                        FROM audit_log
                        WHERE business_domain_id = ? AND action = 'platform.domain.create'
                        ORDER BY id DESC
                        LIMIT 1
                        """,
                created.id());
        assertThat(row.get("point")).isEqualTo("platform");
        assertThat(row.get("action")).isEqualTo("platform.domain.create");
    }

    private long insertDomain(String code) {
        jdbcTemplate.update("""
                INSERT INTO business_domain (code, name, description, visibility_policy, status, created_at, updated_at, visibility_policy_codes)
                VALUES (?, ?, '审计测试业务域', 'public', 1, CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3), '["public"]')
                """, code, code);
        Long id = jdbcTemplate.queryForObject(
                "SELECT id FROM business_domain WHERE code = ? LIMIT 1", Long.class, code);
        if (id == null) {
            throw new IllegalStateException("test business domain insert failed");
        }
        createdDomainIds.add(id);
        return id;
    }

    private Map<String, Object> awaitAuditRow(String action, long domainId) throws InterruptedException {
        for (int i = 0; i < 100; i++) {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                            SELECT point, action, target, business_domain_id
                            FROM audit_log
                            WHERE business_domain_id = ? AND action = ?
                            ORDER BY id DESC
                            LIMIT 1
                            """,
                    domainId, action);
            if (!rows.isEmpty()) {
                return rows.get(0);
            }
            Thread.sleep(100);
        }
        throw new AssertionError("audit row not written within timeout: action=" + action);
    }

    private void deleteDomainData(List<Long> domainIds) {
        String placeholders = String.join(",", Collections.nCopies(domainIds.size(), "?"));
        jdbcTemplate.update("DELETE FROM audit_log WHERE business_domain_id IN (" + placeholders + ")",
                domainIds.toArray());
        jdbcTemplate.update("""
                        DELETE FROM domain_member_role
                        WHERE domain_member_id IN (
                            SELECT id FROM domain_member WHERE business_domain_id IN (%s)
                        )
                        """.formatted(placeholders), domainIds.toArray());
        jdbcTemplate.update("DELETE FROM domain_member WHERE business_domain_id IN (" + placeholders + ")",
                domainIds.toArray());
        jdbcTemplate.update("""
                        DELETE FROM domain_role_permission
                        WHERE domain_role_id IN (
                            SELECT id FROM domain_role WHERE business_domain_id IN (%s)
                        )
                        """.formatted(placeholders), domainIds.toArray());
        jdbcTemplate.update("DELETE FROM domain_role WHERE business_domain_id IN (" + placeholders + ")",
                domainIds.toArray());
        jdbcTemplate.update("DELETE FROM iam_role_binding WHERE business_domain_id IN (" + placeholders + ")",
                domainIds.toArray());
        jdbcTemplate.update("DELETE FROM business_domain WHERE id IN (" + placeholders + ")",
                domainIds.toArray());
    }
}
