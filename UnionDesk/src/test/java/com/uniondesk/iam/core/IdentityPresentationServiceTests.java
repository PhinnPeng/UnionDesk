package com.uniondesk.iam.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import static org.mockito.ArgumentMatchers.argThat;

import java.sql.ResultSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

@ExtendWith(MockitoExtension.class)
class IdentityPresentationServiceTests {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private IdentityPresentationService service;

    @BeforeEach
    void setUp() {
        service = new IdentityPresentationService(jdbcTemplate);
    }

    @Test
    void resolveStaffInDomainUsesNicknameFallbackChain() {
        when(jdbcTemplate.queryForObject(
                argThat(sql -> sql.contains("FROM staff_account")),
                any(RowMapper.class),
                eq(10L)))
                .thenAnswer(invocation -> mapStaff(invocation.getArgument(1), "agent01", "张三", null));
        when(jdbcTemplate.queryForObject(
                argThat(sql -> sql.contains("FROM domain_member")),
                any(RowMapper.class),
                eq(10L),
                eq(20L)))
                .thenAnswer(invocation -> mapDomain(invocation.getArgument(1), "客服小王"));

        IdentityPresentationService.ResolvedStaffDomainView view = service.resolveStaffInDomain(10L, 20L);
        assertThat(view.realName()).isEqualTo("张三");
        assertThat(view.nickname()).isEqualTo("客服小王");
    }

    @Test
    void resolveStaffInDomainFallsBackToRealNameWhenNicknameMissing() {
        when(jdbcTemplate.queryForObject(
                argThat(sql -> sql.contains("FROM staff_account")),
                any(RowMapper.class),
                eq(11L)))
                .thenAnswer(invocation -> mapStaff(invocation.getArgument(1), "agent02", "李四", null));
        when(jdbcTemplate.queryForObject(
                argThat(sql -> sql.contains("FROM domain_member")),
                any(RowMapper.class),
                eq(11L),
                eq(21L)))
                .thenThrow(new EmptyResultDataAccessException(1));

        IdentityPresentationService.ResolvedStaffDomainView view = service.resolveStaffInDomain(11L, 21L);
        assertThat(view.nickname()).isEqualTo("李四");
    }

    private static Object mapStaff(RowMapper<?> mapper, String username, String realName, String nickname) throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("username")).thenReturn(username);
        when(rs.getString("real_name")).thenReturn(realName);
        when(rs.getString("nickname")).thenReturn(nickname);
        when(rs.getString("avatar_url")).thenReturn(null);
        when(rs.getString("phone")).thenReturn("13800000001");
        when(rs.getString("email")).thenReturn("a@test.com");
        return mapper.mapRow(rs, 0);
    }

    private static Object mapDomain(RowMapper<?> mapper, String domainNickname) throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("domain_nickname")).thenReturn(domainNickname);
        when(rs.getString("domain_avatar_url")).thenReturn(null);
        when(rs.getString("domain_contact_phone")).thenReturn(null);
        when(rs.getString("domain_contact_email")).thenReturn(null);
        return mapper.mapRow(rs, 0);
    }
}
