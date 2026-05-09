package com.uniondesk.domain.core;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DomainMemberServiceTests {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private DomainService domainService;

    private DomainMemberService domainMemberService;

    @BeforeEach
    void setUp() {
        domainMemberService = new DomainMemberService(jdbcTemplate, domainService);
    }

    @Test
    void guardLastDomainAdminRejectsRemovingLastHolder() {
        when(jdbcTemplate.query(eq("""
                        SELECT dr.code
                        FROM domain_member_role dmr
                        JOIN domain_role dr ON dr.id = dmr.domain_role_id
                        WHERE dmr.domain_member_id = ?
                        ORDER BY dr.id
                        """), any(org.springframework.jdbc.core.RowMapper.class), eq(11L)))
                .thenReturn(List.of("domain_admin"));
        when(jdbcTemplate.queryForObject(eq("""
                        SELECT COUNT(DISTINCT dm.id)
                        FROM domain_member dm
                        JOIN domain_member_role dmr ON dmr.domain_member_id = dm.id
                        JOIN domain_role dr ON dr.id = dmr.domain_role_id
                        WHERE dr.business_domain_id = ?
                          AND dr.code = 'domain_admin'
                          AND dm.status = 'active'
                          AND dm.deleted_at IS NULL
                          AND dm.id <> ?
                        """), eq(Integer.class), eq(1L), eq(11L)))
                .thenReturn(0);

        assertThatThrownBy(() -> domainMemberService.guardLastDomainAdmin(1L, 11L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("业务域管理员");
    }

    @Test
    void guardLastDomainSuperAdminRejectsRemovingLastHolder() {
        when(jdbcTemplate.query(eq("""
                        SELECT dr.code
                        FROM domain_member_role dmr
                        JOIN domain_role dr ON dr.id = dmr.domain_role_id
                        WHERE dmr.domain_member_id = ?
                        ORDER BY dr.id
                        """), any(org.springframework.jdbc.core.RowMapper.class), eq(11L)))
                .thenReturn(List.of("super_admin"));
        when(jdbcTemplate.queryForObject(eq("""
                        SELECT COUNT(DISTINCT dm.id)
                        FROM domain_member dm
                        JOIN domain_member_role dmr ON dmr.domain_member_id = dm.id
                        JOIN domain_role dr ON dr.id = dmr.domain_role_id
                        WHERE dr.business_domain_id = ?
                          AND dr.code = 'super_admin'
                          AND dm.status = 'active'
                          AND dm.deleted_at IS NULL
                          AND dm.id <> ?
                        """), eq(Integer.class), eq(1L), eq(11L)))
                .thenReturn(0);

        assertThatThrownBy(() -> domainMemberService.guardLastDomainSuperAdmin(1L, 11L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("业务域超级管理员");
    }
}
