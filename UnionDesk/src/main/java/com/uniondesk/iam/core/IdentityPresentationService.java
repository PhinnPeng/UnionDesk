package com.uniondesk.iam.core;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class IdentityPresentationService {

    private final JdbcTemplate jdbcTemplate;

    public IdentityPresentationService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public ResolvedStaffDomainView resolveStaffInDomain(long staffAccountId, long domainId) {
        StaffRow staff = loadStaff(staffAccountId);
        if (staff == null) {
            throw new IllegalArgumentException("员工账号不存在");
        }
        DomainMemberPresentationRow member = loadDomainMember(staffAccountId, domainId);
        String nickname = firstNonBlank(
                member == null ? null : member.domainNickname(),
                staff.nickname(),
                staff.realName(),
                staff.username());
        return new ResolvedStaffDomainView(
                staff.realName(),
                nickname,
                firstNonBlank(member == null ? null : member.domainAvatarUrl(), staff.avatarUrl()),
                firstNonBlank(member == null ? null : member.domainContactPhone(), staff.phone()),
                firstNonBlank(member == null ? null : member.domainContactEmail(), staff.email()));
    }

    private StaffRow loadStaff(long staffAccountId) {
        try {
            return jdbcTemplate.queryForObject("""
                            SELECT username, real_name, nickname, avatar_url, phone, email
                            FROM staff_account
                            WHERE id = ?
                            LIMIT 1
                            """,
                    (rs, rowNum) -> new StaffRow(
                            rs.getString("username"),
                            rs.getString("real_name"),
                            rs.getString("nickname"),
                            rs.getString("avatar_url"),
                            rs.getString("phone"),
                            rs.getString("email")),
                    staffAccountId);
        } catch (EmptyResultDataAccessException ex) {
            return null;
        }
    }

    private DomainMemberPresentationRow loadDomainMember(long staffAccountId, long domainId) {
        try {
            return jdbcTemplate.queryForObject("""
                            SELECT domain_nickname, domain_avatar_url, domain_contact_phone, domain_contact_email
                            FROM domain_member
                            WHERE staff_account_id = ?
                              AND business_domain_id = ?
                              AND deleted_at IS NULL
                            LIMIT 1
                            """,
                    (rs, rowNum) -> new DomainMemberPresentationRow(
                            rs.getString("domain_nickname"),
                            rs.getString("domain_avatar_url"),
                            rs.getString("domain_contact_phone"),
                            rs.getString("domain_contact_email")),
                    staffAccountId,
                    domainId);
        } catch (EmptyResultDataAccessException ex) {
            return null;
        }
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

    public record ResolvedStaffDomainView(
            String realName,
            String nickname,
            String avatarUrl,
            String contactPhone,
            String contactEmail) {
    }

    private record StaffRow(
            String username,
            String realName,
            String nickname,
            String avatarUrl,
            String phone,
            String email) {
    }

    private record DomainMemberPresentationRow(
            String domainNickname,
            String domainAvatarUrl,
            String domainContactPhone,
            String domainContactEmail) {
    }
}
