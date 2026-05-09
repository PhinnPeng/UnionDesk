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
        String sql = switch (normalizeAccountType(accountType)) {
            case "customer" -> switch (type) {
                case USERNAME -> customerLookupSql("login_name");
                case EMAIL -> customerLookupSql("email");
                case MOBILE -> customerLookupSql("phone");
            };
            default -> switch (type) {
                case USERNAME -> """
                        SELECT id, username, mobile, email, password_hash, status, account_type, employment_status
                        FROM user_account
                        WHERE LOWER(username) = LOWER(?)
                        LIMIT 1
                        """;
                case EMAIL -> """
                        SELECT id, username, mobile, email, password_hash, status, account_type, employment_status
                        FROM user_account
                        WHERE LOWER(email) = LOWER(?)
                        LIMIT 1
                        """;
                case MOBILE -> """
                        SELECT id, username, mobile, email, password_hash, status, account_type, employment_status
                        FROM user_account
                        WHERE mobile = ?
                        LIMIT 1
                        """;
            };
        };
        return jdbcTemplate.query(sql,
                (rs, rowNum) -> new LoginAccount(
                        rs.getLong("id"),
                        rs.getString("username"),
                        rs.getString("mobile"),
                        rs.getString("email"),
                        rs.getString("password_hash"),
                        rs.getInt("status"),
                        rs.getString("account_type"),
                        rs.getString("employment_status")),
                identifier.trim()).stream().findFirst();
    }

    public Optional<LoginAccount> findById(long userId) {
        return findById(userId, "staff");
    }

    public Optional<LoginAccount> findById(long userId, String accountType) {
        String sql = "customer".equalsIgnoreCase(normalizeAccountType(accountType))
                ? """
                        SELECT
                            id,
                            login_name AS username,
                            phone AS mobile,
                            email,
                            password_hash,
                            CASE WHEN status = 'active' THEN 1 ELSE 0 END AS status,
                            'customer' AS account_type,
                            status AS employment_status
                        FROM customer_account
                        WHERE id = ?
                        LIMIT 1
                        """
                : """
                        SELECT id, username, mobile, email, password_hash, status, account_type, employment_status
                        FROM user_account
                        WHERE id = ?
                        LIMIT 1
                        """;
        return jdbcTemplate.query(sql,
                (rs, rowNum) -> new LoginAccount(
                        rs.getLong("id"),
                        rs.getString("username"),
                        rs.getString("mobile"),
                        rs.getString("email"),
                        rs.getString("password_hash"),
                        rs.getInt("status"),
                        rs.getString("account_type"),
                        rs.getString("employment_status")),
                userId).stream().findFirst();
    }

    public List<String> loadRoleCodes(long userId) {
        List<String> roles = new ArrayList<>(jdbcTemplate.query("""
                        SELECT DISTINCT role_code
                        FROM (
                            SELECT r.code AS role_code
                            FROM role r
                            JOIN user_global_role ugr ON ugr.role_id = r.id
                            WHERE ugr.user_id = ?
                            UNION
                            SELECT r.code AS role_code
                            FROM role r
                            JOIN user_domain_role udr ON udr.role_id = r.id
                            WHERE udr.user_id = ?
                        ) roles
                        """,
                (rs, rowNum) -> rs.getString("role_code"),
                userId,
                userId));
        roles.sort(Comparator.comparingInt(LoginAccountService::rolePriority));
        return List.copyOf(roles);
    }

    public List<Long> loadAccessibleDomainIds(long userId, List<String> roleCodes) {
        if (roleCodes == null || roleCodes.isEmpty()) {
            return List.of();
        }
        if (roleCodes.contains("super_admin")) {
            return jdbcTemplate.queryForList("SELECT id FROM business_domain ORDER BY id", Long.class);
        }
        String rolePlaceholders = String.join(",", roleCodes.stream().map(role -> "?").toList());
        List<Object> args = new ArrayList<>();
        args.add(userId);
        args.addAll(roleCodes);
        return jdbcTemplate.queryForList("""
                        SELECT DISTINCT udr.business_domain_id
                        FROM user_domain_role udr
                        JOIN role r ON r.id = udr.role_id
                        WHERE udr.user_id = ?
                          AND r.code IN (%s)
                        ORDER BY udr.business_domain_id
                        """.formatted(rolePlaceholders), Long.class, args.toArray());
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
        return loadAccessibleDomainIds(userId, roleCodes);
    }

    private String customerLookupSql(String columnName) {
        return """
                SELECT
                    id,
                    login_name AS username,
                    phone AS mobile,
                    email,
                    password_hash,
                    CASE WHEN status = 'active' THEN 1 ELSE 0 END AS status,
                    'customer' AS account_type,
                    status AS employment_status
                FROM customer_account
                WHERE LOWER(%s) = LOWER(?)
                LIMIT 1
                """.formatted(columnName);
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
            case "domain_admin" -> 1;
            case "agent" -> 2;
            case "customer" -> 3;
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
