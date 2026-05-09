package com.uniondesk.blockedword.core;

import com.uniondesk.blockedword.web.BlockedWordDtos;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional(readOnly = true)
public class BlockedWordService {

    private final JdbcTemplate jdbcTemplate;

    public BlockedWordService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<BlockedWordDtos.BlockedWordView> listBlockedWords(long domainId) {
        return jdbcTemplate.query("""
                        SELECT
                            id,
                            word,
                            created_at
                        FROM blocked_word
                        WHERE business_domain_id = ?
                        ORDER BY id DESC
                        """,
                this::mapBlockedWordView,
                domainId);
    }

    @Transactional
    public BlockedWordDtos.BlockedWordView createBlockedWord(long domainId, String word) {
        String normalizedWord = normalizeWord(word);
        jdbcTemplate.update("""
                        INSERT INTO blocked_word (
                            business_domain_id,
                            word,
                            created_at
                        )
                        VALUES (?, ?, CURRENT_TIMESTAMP(3))
                        """,
                domainId,
                normalizedWord);
        Long wordId = jdbcTemplate.queryForObject("""
                        SELECT id
                        FROM blocked_word
                        WHERE business_domain_id = ?
                          AND word = ?
                        ORDER BY id DESC
                        LIMIT 1
                        """,
                Long.class,
                domainId,
                normalizedWord);
        if (wordId == null) {
            throw new IllegalStateException("blocked word create failed");
        }
        return jdbcTemplate.queryForObject("""
                        SELECT
                            id,
                            word,
                            created_at
                        FROM blocked_word
                        WHERE id = ?
                          AND business_domain_id = ?
                        LIMIT 1
                        """,
                this::mapBlockedWordView,
                wordId,
                domainId);
    }

    @Transactional
    public void deleteBlockedWord(long domainId, long wordId) {
        int updated = jdbcTemplate.update("""
                        DELETE FROM blocked_word
                        WHERE id = ?
                          AND business_domain_id = ?
                        """,
                wordId,
                domainId);
        if (updated == 0) {
            throw new IllegalArgumentException("blocked word not found");
        }
    }

    private BlockedWordDtos.BlockedWordView mapBlockedWordView(ResultSet rs, int rowNum) throws SQLException {
        return new BlockedWordDtos.BlockedWordView(
                rs.getLong("id"),
                rs.getString("word"),
                toLocalDateTime(rs.getTimestamp("created_at")));
    }

    private String normalizeWord(String word) {
        if (!StringUtils.hasText(word)) {
            throw new IllegalArgumentException("blocked word is required");
        }
        return word.trim();
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
