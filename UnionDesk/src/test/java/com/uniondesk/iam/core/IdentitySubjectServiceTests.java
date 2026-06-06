package com.uniondesk.iam.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.ResultSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

@ExtendWith(MockitoExtension.class)
class IdentitySubjectServiceTests {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private IdentitySubjectService service;

    @BeforeEach
    void setUp() {
        service = new IdentitySubjectService(jdbcTemplate);
    }

    @Test
    void resolveSubjectIdByPhoneReturnsExisting() {
        stubMergedInto(9L, null);
        when(jdbcTemplate.queryForObject(any(String.class), eq(Long.class), eq("13800000000"))).thenReturn(9L);

        assertThat(service.resolveSubjectIdByPhone("13800000000")).isEqualTo(9L);
    }

    @Test
    void resolveEffectiveSubjectIdFollowsMergeChain() {
        stubMergedInto(1L, 2L);
        stubMergedInto(2L, null);

        assertThat(service.resolveEffectiveSubjectId(1L)).isEqualTo(2L);
    }

    @Test
    void requireActiveSubjectRejectsMergedSubject() {
        stubMergedInto(3L, null);
        when(jdbcTemplate.queryForObject(
                any(String.class),
                any(RowMapper.class),
                eq(3L)))
                .thenAnswer(invocation -> {
                    RowMapper<?> mapper = invocation.getArgument(1);
                    ResultSet rs = mock(ResultSet.class);
                    when(rs.getLong("id")).thenReturn(3L);
                    when(rs.getString("status")).thenReturn("active");
                    when(rs.getObject("merged_into_id")).thenReturn(4L);
                    when(rs.getLong("merged_into_id")).thenReturn(4L);
                    return mapper.mapRow(rs, 0);
                });

        assertThatThrownBy(() -> service.requireActiveSubject(3L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("身份主体不可用");
    }

    @Test
    void resolveSubjectIdByPhoneCreatesWhenMissing() {
        when(jdbcTemplate.queryForObject(any(String.class), eq(Long.class), eq("13900000000")))
                .thenThrow(new EmptyResultDataAccessException(1));
        when(jdbcTemplate.queryForObject(eq("SELECT LAST_INSERT_ID()"), eq(Long.class))).thenReturn(100L);

        assertThat(service.resolveSubjectIdByPhone("13900000000")).isEqualTo(100L);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).update(sqlCaptor.capture(), eq("13900000000"));
        assertThat(sqlCaptor.getValue()).contains("INSERT INTO identity_subject");
    }

    private void stubMergedInto(long subjectId, Long mergedInto) {
        when(jdbcTemplate.queryForObject(any(String.class), eq(Long.class), eq(subjectId)))
                .thenAnswer(invocation -> {
                    String sql = invocation.getArgument(0, String.class);
                    if (sql.contains("merged_into_id")) {
                        return mergedInto;
                    }
                    return subjectId;
                });
    }
}
