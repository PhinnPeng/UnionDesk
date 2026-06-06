package com.uniondesk.iam.core;

import java.util.HashSet;
import java.util.Set;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class IdentitySubjectService {

    private static final int MAX_MERGE_DEPTH = 16;

    private final JdbcTemplate jdbcTemplate;

    public IdentitySubjectService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public long resolveSubjectIdByPhone(String phone) {
        String normalized = requirePhone(phone);
        Long existing = findSubjectIdByPhone(normalized);
        if (existing != null) {
            return resolveEffectiveSubjectId(existing);
        }
        jdbcTemplate.update("""
                        INSERT INTO identity_subject (subject_type, phone, status, created_at, updated_at)
                        VALUES ('person', ?, 'active', CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3))
                        """,
                normalized);
        Long id = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        if (id == null) {
            throw new IllegalStateException("身份主体创建失败");
        }
        return id;
    }

    public long resolveEffectiveSubjectId(long subjectId) {
        long current = subjectId;
        Set<Long> visited = new HashSet<>();
        for (int depth = 0; depth < MAX_MERGE_DEPTH; depth++) {
            if (!visited.add(current)) {
                throw new IllegalStateException("无效的主体合并链");
            }
            Long mergedInto = findMergedIntoId(current);
            if (mergedInto == null) {
                return current;
            }
            current = mergedInto;
        }
        throw new IllegalStateException("无效的主体合并链");
    }

    public void requireActiveSubject(long subjectId) {
        long effectiveId = resolveEffectiveSubjectId(subjectId);
        SubjectRow row = loadSubject(effectiveId);
        if (row == null) {
            throw new IllegalArgumentException("身份主体不可用");
        }
        if (!"active".equalsIgnoreCase(row.status())) {
            throw new IllegalArgumentException("身份主体不可用");
        }
        if (row.mergedIntoId() != null) {
            throw new IllegalArgumentException("身份主体不可用");
        }
    }

    private Long findSubjectIdByPhone(String phone) {
        try {
            return jdbcTemplate.queryForObject("""
                            SELECT id
                            FROM identity_subject
                            WHERE phone = ?
                            LIMIT 1
                            """,
                    Long.class,
                    phone);
        } catch (EmptyResultDataAccessException ex) {
            return null;
        }
    }

    private Long findMergedIntoId(long subjectId) {
        try {
            return jdbcTemplate.queryForObject("""
                            SELECT merged_into_id
                            FROM identity_subject
                            WHERE id = ?
                            LIMIT 1
                            """,
                    Long.class,
                    subjectId);
        } catch (EmptyResultDataAccessException ex) {
            return null;
        }
    }

    private SubjectRow loadSubject(long subjectId) {
        try {
            return jdbcTemplate.queryForObject("""
                            SELECT id, status, merged_into_id
                            FROM identity_subject
                            WHERE id = ?
                            LIMIT 1
                            """,
                    (rs, rowNum) -> new SubjectRow(
                            rs.getLong("id"),
                            rs.getString("status"),
                            rs.getObject("merged_into_id") == null ? null : rs.getLong("merged_into_id")),
                    subjectId);
        } catch (EmptyResultDataAccessException ex) {
            return null;
        }
    }

    private String requirePhone(String phone) {
        if (!StringUtils.hasText(phone)) {
            throw new IllegalArgumentException("手机号不能为空");
        }
        return phone.trim();
    }

    private record SubjectRow(long id, String status, Long mergedIntoId) {
    }
}
