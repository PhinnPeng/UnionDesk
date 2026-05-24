package com.uniondesk.domain.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.uniondesk.auth.core.UserContext;
import com.uniondesk.auth.core.UserContextHolder;
import com.uniondesk.domain.web.DomainDtos;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DomainServiceBootstrapTests {

    @Autowired
    private DomainService domainService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        UserContextHolder.set(new UserContext(2L, "platform_admin", null, "sid-test", "ud-admin-web"));
    }

    @AfterEach
    void tearDown() {
        UserContextHolder.clear();
    }

    @Test
    void createDomainGrantsCreatorAsDomainSuperAdmin() {
        String code = "bootstrap-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        DomainDtos.DomainCreateResponse created = domainService.createDomain(new DomainDtos.CreateDomainRequest(
                code,
                "Bootstrap Domain",
                "integration test",
                "/default-domain-logo.svg",
                List.of("public"),
                "open"));

        long domainId = created.id();
        assertNotNull(domainId);

        Integer presetRoleCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM domain_role WHERE business_domain_id = ? AND preset = 1",
                Integer.class,
                domainId);
        assertEquals(3, presetRoleCount);

        Long memberId = jdbcTemplate.queryForObject("""
                        SELECT dm.id
                        FROM domain_member dm
                        WHERE dm.business_domain_id = ?
                          AND dm.staff_account_id = 2
                          AND dm.deleted_at IS NULL
                        LIMIT 1
                        """,
                Long.class,
                domainId);
        assertNotNull(memberId);

        Integer superAdminBindingCount = jdbcTemplate.queryForObject("""
                        SELECT COUNT(*)
                        FROM domain_member_role dmr
                        JOIN domain_role dr ON dr.id = dmr.domain_role_id
                        WHERE dmr.domain_member_id = ?
                          AND dr.code = 'super_admin'
                        """,
                Integer.class,
                memberId);
        assertEquals(1, superAdminBindingCount);

        Integer legacyBindingCount = jdbcTemplate.queryForObject("""
                        SELECT COUNT(*)
                        FROM user_domain_role udr
                        JOIN role r ON r.id = udr.role_id
                        WHERE udr.user_id = 2
                          AND udr.business_domain_id = ?
                          AND r.code = 'super_admin'
                        """,
                Integer.class,
                domainId);
        assertEquals(1, legacyBindingCount);

        String source = jdbcTemplate.queryForObject(
                "SELECT source FROM domain_member WHERE id = ?",
                String.class,
                memberId);
        assertEquals("domain_create", source);

        Integer globalSuperAdminCount = jdbcTemplate.queryForObject("""
                        SELECT COUNT(*)
                        FROM user_global_role ugr
                        JOIN role r ON r.id = ugr.role_id
                        WHERE ugr.user_id = 2
                          AND r.code = 'super_admin'
                        """,
                Integer.class);
        assertTrue(globalSuperAdminCount >= 1, "seed admin may already have global super_admin");
    }
}
