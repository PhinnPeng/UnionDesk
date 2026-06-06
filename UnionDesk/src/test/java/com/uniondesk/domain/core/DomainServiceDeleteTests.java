package com.uniondesk.domain.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.uniondesk.auth.core.UserContext;
import com.uniondesk.auth.core.UserContextHolder;
import com.uniondesk.domain.web.DomainDtos;
import java.sql.Timestamp;
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
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DomainServiceDeleteTests {

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
    void deleteDomainSetsDeletedAtAndAuditFieldsWithoutChangingStatus() {
        String code = "delete-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        DomainDtos.DomainCreateResponse created = domainService.createDomain(new DomainDtos.CreateDomainRequest(
                code,
                "Delete Test Domain",
                "delete semantics test",
                "/default-domain-logo.svg",
                List.of("public"),
                "allowed",
                "allowed"));
        long domainId = created.id();

        domainService.deleteDomain(domainId);

        Map<String, Object> row = jdbcTemplate.queryForMap("""
                        SELECT status, deleted_at, updated_at, updated_by
                        FROM business_domain
                        WHERE id = ?
                        """,
                domainId);
        assertEquals(1, ((Number) row.get("status")).intValue());
        assertNotNull(row.get("deleted_at"));
        assertNotNull(row.get("updated_at"));
        assertEquals(2L, ((Number) row.get("updated_by")).longValue());
    }

    @Test
    void deleteDomainPreservesDisabledStatus() {
        String code = "delete-disabled-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        DomainDtos.DomainCreateResponse created = domainService.createDomain(new DomainDtos.CreateDomainRequest(
                code,
                "Disabled Delete Test",
                null,
                "/default-domain-logo.svg",
                List.of("public"),
                "allowed",
                "allowed"));
        long domainId = created.id();
        jdbcTemplate.update("UPDATE business_domain SET status = 0 WHERE id = ?", domainId);

        domainService.deleteDomain(domainId);

        Integer status = jdbcTemplate.queryForObject(
                "SELECT status FROM business_domain WHERE id = ?",
                Integer.class,
                domainId);
        Timestamp deletedAt = jdbcTemplate.queryForObject(
                "SELECT deleted_at FROM business_domain WHERE id = ?",
                Timestamp.class,
                domainId);
        assertEquals(0, status);
        assertNotNull(deletedAt);
    }

    @Test
    void getDomainRejectsDeletedDomain() {
        String code = "delete-get-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        DomainDtos.DomainCreateResponse created = domainService.createDomain(new DomainDtos.CreateDomainRequest(
                code,
                "Deleted Get Test",
                null,
                "/default-domain-logo.svg",
                List.of("public"),
                "allowed",
                "allowed"));
        long domainId = created.id();
        domainService.deleteDomain(domainId);

        assertThrows(IllegalArgumentException.class, () -> domainService.getDomain(domainId));
    }
}
