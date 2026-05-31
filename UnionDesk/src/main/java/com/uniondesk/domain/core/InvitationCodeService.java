package com.uniondesk.domain.core;

import com.uniondesk.common.web.PageResult;
import com.uniondesk.domain.web.DomainDtos;
import com.uniondesk.domain.web.InvitationCodeDtos;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class InvitationCodeService {

    private final JdbcTemplate jdbcTemplate;
    private final DomainService domainService;

    public InvitationCodeService(JdbcTemplate jdbcTemplate, DomainService domainService) {
        this.jdbcTemplate = jdbcTemplate;
        this.domainService = domainService;
    }

    public PageResult<InvitationCodeDtos.InvitationCodeView> listInvitationCodes(long domainId, int page, int pageSize) {
        loadDomain(domainId);
        int normalizedPage = Math.max(page, 1);
        int normalizedPageSize = Math.max(pageSize, 1);
        return new PageResult<>(
                countInvitationCodes(domainId),
                jdbcTemplate.query("""
                                SELECT
                                    id,
                                    business_domain_id,
                                    code,
                                    channel,
                                    expires_at,
                                    max_uses,
                                    used_count,
                                    status,
                                    created_at,
                                    updated_at
                                FROM invitation_code
                                WHERE business_domain_id = ?
                                ORDER BY id DESC
                                LIMIT ? OFFSET ?
                                """,
                        this::mapInvitationCodeView,
                        domainId,
                        normalizedPageSize,
                        (normalizedPage - 1L) * normalizedPageSize));
    }

    @Transactional
    public InvitationCodeDtos.InvitationCodeView createInvitationCode(
            long domainId,
            InvitationCodeDtos.CreateInvitationCodeRequest request) {
        DomainDtos.DomainView domain = loadDomain(domainId);
        if (!DomainAccessPolicy.isAllowed(domain.invitation_enabled())) {
            throw DomainErrorCodes.INVITATION_DISALLOWED.toException();
        }
        String code = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        jdbcTemplate.update("""
                        INSERT INTO invitation_code (
                            business_domain_id,
                            code,
                            channel,
                            expires_at,
                            max_uses,
                            used_count,
                            status,
                            created_at,
                            updated_at
                        )
                        VALUES (?, ?, ?, ?, ?, 0, 'active', CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3))
                        """,
                domainId,
                code,
                normalizeText(request.channel()),
                request.expiresAt() == null ? null : Timestamp.valueOf(request.expiresAt()),
                request.maxUses());
        Long id = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        if (id == null) {
            throw new IllegalStateException("invitation code create failed");
        }
        return loadInvitationCodeById(domainId, id);
    }

    @Transactional
    public void deleteInvitationCode(long domainId, long codeId) {
        loadDomain(domainId);
        int updated = jdbcTemplate.update("""
                        UPDATE invitation_code
                        SET status = 'inactive',
                            updated_at = CURRENT_TIMESTAMP(3)
                        WHERE id = ?
                          AND business_domain_id = ?
                          AND status <> 'inactive'
                        """,
                codeId,
                domainId);
        if (updated == 0) {
            throw new IllegalArgumentException("invitation code not found");
        }
    }

    @Transactional
    public InvitationCodeDtos.InvitationCodeView validateAndUse(long domainId, String code) {
        if (!StringUtils.hasText(code)) {
            throw new IllegalArgumentException("invitation code is required");
        }
        InvitationCodeRow invitationCode = loadActiveInvitationCode(domainId, code.trim());
        LocalDateTime now = LocalDateTime.now();
        if (invitationCode.expiresAt() != null && !invitationCode.expiresAt().isAfter(now)) {
            throw new IllegalArgumentException("invitation code expired");
        }
        if (invitationCode.maxUses() != null && invitationCode.usedCount() >= invitationCode.maxUses()) {
            throw new IllegalArgumentException("invitation code used up");
        }
        jdbcTemplate.update("""
                        UPDATE invitation_code
                        SET used_count = used_count + 1,
                            updated_at = CURRENT_TIMESTAMP(3)
                        WHERE id = ?
                        """,
                invitationCode.id());
        return loadInvitationCodeById(domainId, invitationCode.id());
    }

    private long countInvitationCodes(long domainId) {
        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM invitation_code WHERE business_domain_id = ?",
                Long.class,
                domainId);
        return total == null ? 0L : total;
    }

    private InvitationCodeDtos.InvitationCodeView loadInvitationCodeById(long domainId, long codeId) {
        try {
            return jdbcTemplate.queryForObject("""
                            SELECT
                                id,
                                business_domain_id,
                                code,
                                channel,
                                expires_at,
                                max_uses,
                                used_count,
                                status,
                                created_at,
                                updated_at
                            FROM invitation_code
                            WHERE id = ?
                              AND business_domain_id = ?
                            LIMIT 1
                            """,
                    this::mapInvitationCodeView,
                    codeId,
                    domainId);
        } catch (EmptyResultDataAccessException ex) {
            throw new IllegalArgumentException("invitation code not found");
        }
    }

    private InvitationCodeRow loadActiveInvitationCode(long domainId, String code) {
        try {
            return jdbcTemplate.queryForObject("""
                            SELECT
                                id,
                                business_domain_id,
                                code,
                                channel,
                                expires_at,
                                max_uses,
                                used_count,
                                status
                            FROM invitation_code
                            WHERE business_domain_id = ?
                              AND code = ?
                              AND status = 'active'
                            LIMIT 1
                            """,
                    (rs, rowNum) -> new InvitationCodeRow(
                            rs.getLong("id"),
                            rs.getLong("business_domain_id"),
                            rs.getString("code"),
                            rs.getString("channel"),
                            toLocalDateTime(rs.getTimestamp("expires_at")),
                            rs.getObject("max_uses", Integer.class),
                            rs.getInt("used_count"),
                            rs.getString("status")),
                    domainId,
                    code);
        } catch (EmptyResultDataAccessException ex) {
            throw new IllegalArgumentException("invitation code not found");
        }
    }

    private InvitationCodeDtos.InvitationCodeView mapInvitationCodeView(ResultSet rs, int rowNum) throws SQLException {
        return new InvitationCodeDtos.InvitationCodeView(
                rs.getLong("id"),
                rs.getLong("business_domain_id"),
                rs.getString("code"),
                rs.getString("channel"),
                toLocalDateTime(rs.getTimestamp("expires_at")),
                rs.getObject("max_uses", Integer.class),
                rs.getInt("used_count"),
                rs.getString("status"),
                toLocalDateTime(rs.getTimestamp("created_at")),
                toLocalDateTime(rs.getTimestamp("updated_at")));
    }

    private DomainDtos.DomainView loadDomain(long domainId) {
        return domainService.getDomain(domainId);
    }

    private String normalizeText(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private LocalDateTime toLocalDateTime(java.sql.Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private record InvitationCodeRow(
            long id,
            long businessDomainId,
            String code,
            String channel,
            LocalDateTime expiresAt,
            Integer maxUses,
            int usedCount,
            String status) {
    }
}
