package com.uniondesk.auth.core;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class LoginAccountService {

    private final JdbcTemplate jdbcTemplate;

    public LoginAccountService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<LoginAccount> findByIdentifier(String identifier, LoginIdentifierType type) {
        return findByIdentifier(identifier, type, "staff");
    }

    public Optional<LoginAccount> findByIdentifier(String identifier, LoginIdentifierType type, String accountType) {
        String sql = "customer".equalsIgnoreCase(normalizeAccountType(accountType))
                ? switch (type) {
                    case USERNAME -> accountLookupSql("customer_account", "username");
                    case EMAIL -> accountLookupSql("customer_account", "email");
                    case MOBILE -> accountLookupSql("customer_account", "phone");
                }
                : switch (type) {
                    case USERNAME -> accountLookupSql("staff_account", "username");
                    case EMAIL -> accountLookupSql("staff_account", "email");
                    case MOBILE -> accountLookupSql("staff_account", "phone");
                };
        return jdbcTemplate.query(sql,
                (rs, rowNum) -> mapAccount(rs, normalizeAccountType(accountType)),
                identifier.trim()).stream().findFirst();
    }

    public Optional<LoginAccount> findById(long userId) {
        return findById(userId, "staff");
    }

    public Optional<LoginAccount> findById(long userId, String accountType) {
        String normalized = normalizeAccountType(accountType);
        String table = "customer".equals(normalized) ? "customer_account" : "staff_account";
        String sql = accountLookupSql(table, "id") + " AND id = ?";
        return jdbcTemplate.query(sql,
                (rs, rowNum) -> mapAccount(rs, normalized),
                userId).stream().findFirst();
    }

    public List<String> loadRoleCodes(long userId) {
        return loadRoleCodes(userId, "staff");
    }

    public List<String> loadRoleCodes(long userId, String accountType) {
        if ("customer".equalsIgnoreCase(normalizeAccountType(accountType))) {
            return List.of("customer");
        }
        List<String> roles = new ArrayList<>(jdbcTemplate.query("""
                        SELECT DISTINCT dr.code
                        FROM domain_member dm
                        JOIN domain_member_role dmr ON dmr.domain_member_id = dm.id
                        JOIN domain_role dr ON dr.id = dmr.domain_role_id
                        WHERE dm.staff_account_id = ?
                          AND dm.status = 'active'
                          AND dm.deleted_at IS NULL
                        """,
                (rs, rowNum) -> rs.getString("code"),
                userId));
        roles.addAll(jdbcTemplate.query("""
                        SELECT DISTINCT pr.code
                        FROM staff_account_platform_role sapr
                        JOIN platform_role pr ON pr.id = sapr.platform_role_id
                        WHERE sapr.staff_account_id = ?
                        """,
                (rs, rowNum) -> rs.getString("code"),
                userId));
        roles.sort(Comparator.comparingInt(LoginAccountService::rolePriority));
        return List.copyOf(roles);
    }

    public List<Long> loadAccessibleDomainIds(long userId, List<String> roleCodes) {
        return loadAccessibleDomainIds(userId, "staff", roleCodes);
    }

    public List<Long> loadAccessibleDomainIds(long userId, String accountType, List<String> roleCodes) {
        if ("customer".equalsIgnoreCase(normalizeAccountType(accountType))) {
            return jdbcTemplate.queryForList("""
                            SELECT business_domain_id
                            FROM domain_customer
                            WHERE customer_account_id = ?
                              AND status = 'active'
                              AND deleted_at IS NULL
                            ORDER BY business_domain_id
                            """,
                    Long.class,
                    userId);
        }
        if (roleCodes != null && (roleCodes.contains("super_admin") || roleCodes.contains("platform_admin"))) {
            return jdbcTemplate.queryForList("SELECT id FROM business_domain ORDER BY id", Long.class);
        }
        if (roleCodes == null || roleCodes.isEmpty()) {
            return List.of();
        }
        return jdbcTemplate.queryForList("""
                        SELECT DISTINCT dm.business_domain_id
                        FROM domain_member dm
                        JOIN domain_member_role dmr ON dmr.domain_member_id = dm.id
                        JOIN domain_role dr ON dr.id = dmr.domain_role_id
                        WHERE dm.staff_account_id = ?
                          AND dm.status = 'active'
                          AND dm.deleted_at IS NULL
                        ORDER BY dm.business_domain_id
                        """,
                Long.class,
                userId);
    }

    private String accountLookupSql(String table, String columnName) {
        String accountType = "customer_account".equals(table) ? "customer" : "staff";
        if ("id".equals(columnName)) {
            return """
                    SELECT
                        id,
                        username,
                        phone AS mobile,
                        email,
                        password_hash,
                        CASE WHEN status = 'active' THEN 1 ELSE 0 END AS status,
                        '%s' AS account_type,
                        status AS employment_status
                    FROM %s
                    WHERE 1 = 1
                    """.formatted(accountType, table);
        }
        return """
                SELECT
                    id,
                    username,
                    phone AS mobile,
                    email,
                    password_hash,
                    CASE WHEN status = 'active' THEN 1 ELSE 0 END AS status,
                    '%s' AS account_type,
                    status AS employment_status
                FROM %s
                WHERE LOWER(%s) = LOWER(?)
                LIMIT 1
                """.formatted(accountType, table, columnName);
    }

    private LoginAccount mapAccount(java.sql.ResultSet rs, String accountType) throws java.sql.SQLException {
        return new LoginAccount(
                rs.getLong("id"),
                rs.getString("username"),
                rs.getString("mobile"),
                rs.getString("email"),
                rs.getString("password_hash"),
                rs.getInt("status"),
                accountType,
                rs.getString("employment_status"));
    }

    private String normalizeAccountType(String accountType) {
        if (!StringUtils.hasText(accountType)) {
            return "staff";
        }
        String normalized = accountType.trim().toLowerCase();
        return switch (normalized) {
            case "admin", "staff" -> "staff";
            case "customer" -> "customer";
            default -> normalized;
        };
    }

    private static int rolePriority(String role) {
        return switch (role) {
            case "super_admin" -> 0;
            case "platform_admin" -> 1;
            case "domain_admin" -> 2;
            case "agent" -> 3;
            case "customer" -> 4;
            default -> 10;
        };
    }

    public record LoginAccount(
            long id,
            String username,
            String mobile,
            String email,
            String passwordHash,
            int status,
            String accountType,
            String employmentStatus) {
    }
}
