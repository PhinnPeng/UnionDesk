package com.uniondesk.blockedword.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.uniondesk.blockedword.web.BlockedWordDtos;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BlockedWordServiceTests {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private BlockedWordService blockedWordService;

    @BeforeEach
    void setUp() {
        blockedWordService = new BlockedWordService(jdbcTemplate);
    }

    @Test
    void listBlockedWordsMapsRows() throws Exception {
        when(jdbcTemplate.query(argThat(sql -> sql != null && sql.contains("FROM blocked_word")), any(RowMapper.class), any(Object[].class)))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    RowMapper<BlockedWordDtos.BlockedWordView> mapper = invocation.getArgument(1);
                    java.sql.ResultSet rs = org.mockito.Mockito.mock(java.sql.ResultSet.class);
                    when(rs.getLong("id")).thenReturn(11L);
                    when(rs.getString("word")).thenReturn("spam");
                    when(rs.getTimestamp("created_at")).thenReturn(java.sql.Timestamp.valueOf(LocalDateTime.of(2026, 5, 3, 12, 0)));
                    return List.of(mapper.mapRow(rs, 0));
                });

        List<BlockedWordDtos.BlockedWordView> rows = blockedWordService.listBlockedWords(1L);

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).word()).isEqualTo("spam");
    }

    @Test
    void createBlockedWordTrimsWordAndInsertsRow() {
        when(jdbcTemplate.update(argThat(sql -> sql != null && sql.contains("INSERT INTO blocked_word")), any(Object[].class)))
                .thenReturn(1);
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class))).thenReturn(11L);
        when(jdbcTemplate.queryForObject(argThat(sql -> sql != null && sql.contains("FROM blocked_word")), any(RowMapper.class), any(Object[].class)))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    RowMapper<BlockedWordDtos.BlockedWordView> mapper = invocation.getArgument(1);
                    java.sql.ResultSet rs = org.mockito.Mockito.mock(java.sql.ResultSet.class);
                    when(rs.getLong("id")).thenReturn(11L);
                    when(rs.getString("word")).thenReturn("spam");
                    when(rs.getTimestamp("created_at")).thenReturn(java.sql.Timestamp.valueOf(LocalDateTime.of(2026, 5, 3, 12, 0)));
                    return mapper.mapRow(rs, 0);
                });

        BlockedWordDtos.BlockedWordView created = blockedWordService.createBlockedWord(1L, "  spam  ");

        assertThat(created.id()).isEqualTo(11L);
        assertThat(created.word()).isEqualTo("spam");
        ArgumentCaptor<Object[]> args = ArgumentCaptor.forClass(Object[].class);
        verify(jdbcTemplate).update(argThat(sql -> sql != null && sql.contains("INSERT INTO blocked_word")), args.capture());
        assertThat(args.getValue()[0]).isEqualTo(1L);
        assertThat(args.getValue()[1]).isEqualTo("spam");
    }

    @Test
    void deleteBlockedWordDeletesRow() {
        when(jdbcTemplate.update(argThat(sql -> sql != null && sql.contains("DELETE FROM blocked_word")), any(Object[].class)))
                .thenReturn(1);

        blockedWordService.deleteBlockedWord(1L, 11L);

        ArgumentCaptor<Object[]> args = ArgumentCaptor.forClass(Object[].class);
        verify(jdbcTemplate).update(argThat(sql -> sql != null && sql.contains("DELETE FROM blocked_word")), args.capture());
        assertThat(args.getValue()[0]).isEqualTo(11L);
        assertThat(args.getValue()[1]).isEqualTo(1L);
    }

    @Test
    void createBlockedWordRejectsBlankWord() {
        assertThatThrownBy(() -> blockedWordService.createBlockedWord(1L, "   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("word");
    }
}
