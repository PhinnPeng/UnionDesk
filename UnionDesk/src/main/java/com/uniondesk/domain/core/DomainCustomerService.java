package com.uniondesk.domain.core;

import com.uniondesk.common.web.PageResult;
import com.uniondesk.domain.web.DomainCustomerDtos;
import com.uniondesk.domain.web.DomainDtos;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class DomainCustomerService {

    private final JdbcTemplate jdbcTemplate;
    private final DomainService domainService;
    private final Clock clock;
    private final PasswordEncoder passwordEncoder;

    public DomainCustomerService(
            JdbcTemplate jdbcTemplate,
            DomainService domainService,
            Clock clock,
            PasswordEncoder passwordEncoder) {
        this.jdbcTemplate = jdbcTemplate;
        this.domainService = domainService;
        this.clock = clock;
        this.passwordEncoder = passwordEncoder;
    }

    public PageResult<DomainCustomerDtos.DomainCustomerView> listCustomers(
            long domainId,
            int page,
            int pageSize,
            String status,
            String keyword) {
        loadDomain(domainId);
        CustomerQuery query = buildCustomerQuery(domainId, status, keyword);
        return new PageResult<>(
                countCustomers(query),
                jdbcTemplate.query("""
                                SELECT
                                    dc.id,
                                    dc.business_domain_id,
                                    dc.customer_account_id,
                                    ca.subject_id,
                                    ca.login_name,
                                    ca.display_name,
                                    ca.phone,
                                    ca.email,
                                    dc.status,
                                    dc.source,
                                    dc.activated_at,
                                    dc.disabled_at,
                                    dc.created_at,
                                    dc.updated_at
                                FROM domain_customer dc
                                JOIN customer_account ca ON ca.id = dc.customer_account_id
                                JOIN identity_subject s ON s.id = ca.subject_id
                                %s
                                ORDER BY dc.id DESC
                                LIMIT ? OFFSET ?
                                """.formatted(query.whereClause()),
                        this::mapCustomerView,
                        pagingArgs(query, page, pageSize)));
    }

    @Transactional
    public DomainCustomerDtos.DomainCustomerView addCustomer(long domainId, DomainCustomerDtos.CreateDomainCustomerRequest request) {
        loadDomain(domainId);
        loadCustomerAccount(request.customerAccountId());
        String normalizedSource = StringUtils.hasText(request.source()) ? request.source().trim() : "manual";
        return insertDomainCustomerLink(domainId, request.customerAccountId(), normalizedSource);
    }

    @Transactional
    public DomainCustomerDtos.DomainCustomerView addCustomerManual(
            long domainId,
            DomainCustomerDtos.CreateDomainCustomerManualRequest request) {
        loadDomain(domainId);
        String loginName = request.loginName().trim();
        String displayName = request.displayName().trim();
        String phone = request.phone().trim();
        String email = StringUtils.hasText(request.email()) ? request.email().trim() : null;
        ensureCustomerLoginNameAvailable(loginName);
        long subjectId = createIdentitySubject(phone);
        long customerAccountId = createCustomerAccount(subjectId, loginName, displayName, phone, email);
        return insertDomainCustomerLink(domainId, customerAccountId, "manual");
    }

    @Transactional
    public DomainCustomerDtos.BatchCreateDomainCustomersResult addCustomersFromStaff(
            long domainId,
            DomainCustomerDtos.CreateDomainCustomersFromStaffRequest request) {
        loadDomain(domainId);
        List<DomainCustomerDtos.DomainCustomerView> items = new ArrayList<>();
        int added = 0;
        int skipped = 0;
        for (Long staffAccountId : request.staffAccountIds()) {
            if (staffAccountId == null) {
                skipped += 1;
                continue;
            }
            StaffAccountRow staff = loadStaffAccountForDomain(domainId, staffAccountId);
            long customerAccountId = resolveOrCreateCustomerAccountFromStaff(staff);
            if (isCustomerInDomain(domainId, customerAccountId)) {
                throw new IllegalArgumentException(staff.loginName() + "：该员工已作为客户存在");
            }
            items.add(insertDomainCustomerLink(domainId, customerAccountId, "staff_import"));
            added += 1;
        }
        return new DomainCustomerDtos.BatchCreateDomainCustomersResult(added, skipped, items);
    }

    @Transactional
    public DomainCustomerDtos.DomainCustomerView updateCustomerStatus(
            long domainId,
            long customerId,
            DomainCustomerDtos.UpdateDomainCustomerStatusRequest request) {
        loadDomain(domainId);
        String status = normalizeStatus(request.status());
        DomainCustomerDtos.DomainCustomerView current = loadCustomerById(domainId, customerId);
        LocalDateTime now = now();
        Timestamp activatedAt = "active".equals(status) ? Timestamp.valueOf(now) : Timestamp.valueOf(current.activatedAt() == null ? now : current.activatedAt());
        Timestamp disabledAt = "disabled".equals(status) ? Timestamp.valueOf(now) : null;
        jdbcTemplate.update("""
                        UPDATE domain_customer
                        SET status = ?,
                            activated_at = ?,
                            disabled_at = ?,
                            updated_at = CURRENT_TIMESTAMP(3)
                        WHERE id = ?
                          AND business_domain_id = ?
                          AND deleted_at IS NULL
                        """,
                status,
                activatedAt,
                disabledAt,
                customerId,
                domainId);
        return new DomainCustomerDtos.DomainCustomerView(
                current.id(),
                current.businessDomainId(),
                current.customerAccountId(),
                current.subjectId(),
                current.loginName(),
                current.displayName(),
                current.phone(),
                current.email(),
                status,
                current.source(),
                "active".equals(status) ? now : current.activatedAt(),
                "disabled".equals(status) ? now : null,
                current.createdAt(),
                now);
    }

    private long countCustomers(CustomerQuery query) {
        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM domain_customer dc JOIN customer_account ca ON ca.id = dc.customer_account_id JOIN identity_subject s ON s.id = ca.subject_id%s".formatted(query.whereClause()),
                Long.class,
                query.args().toArray());
        return total == null ? 0L : total;
    }

    private CustomerQuery buildCustomerQuery(long domainId, String status, String keyword) {
        List<String> conditions = new ArrayList<>();
        List<Object> args = new ArrayList<>();
        conditions.add("dc.business_domain_id = ?");
        args.add(domainId);
        conditions.add("dc.deleted_at IS NULL");
        if (StringUtils.hasText(status)) {
            conditions.add("dc.status = ?");
            args.add(normalizeStatus(status));
        }
        if (StringUtils.hasText(keyword)) {
            conditions.add("(ca.login_name LIKE ? OR ca.display_name LIKE ? OR ca.phone LIKE ? OR ca.email LIKE ?)");
            String like = "%" + keyword.trim() + "%";
            args.add(like);
            args.add(like);
            args.add(like);
            args.add(like);
        }
        return new CustomerQuery(" WHERE " + String.join(" AND ", conditions), List.copyOf(args));
    }

    private Object[] pagingArgs(CustomerQuery query, int page, int pageSize) {
        int normalizedPage = Math.max(page, 1);
        int normalizedPageSize = Math.max(pageSize, 1);
        List<Object> args = new ArrayList<>(query.args());
        args.add(normalizedPageSize);
        args.add((normalizedPage - 1L) * normalizedPageSize);
        return args.toArray();
    }

    private DomainCustomerDtos.DomainCustomerView mapCustomerView(ResultSet rs, int rowNum) throws SQLException {
        return new DomainCustomerDtos.DomainCustomerView(
                rs.getLong("id"),
                rs.getLong("business_domain_id"),
                rs.getLong("customer_account_id"),
                rs.getLong("subject_id"),
                rs.getString("login_name"),
                rs.getString("display_name"),
                rs.getString("phone"),
                rs.getString("email"),
                rs.getString("status"),
                rs.getString("source"),
                toLocalDateTime(rs.getTimestamp("activated_at")),
                toLocalDateTime(rs.getTimestamp("disabled_at")),
                toLocalDateTime(rs.getTimestamp("created_at")),
                toLocalDateTime(rs.getTimestamp("updated_at")));
    }

    private DomainCustomerDtos.DomainCustomerView loadCustomerById(long domainId, long customerId) {
        try {
            return jdbcTemplate.queryForObject("""
                            SELECT
                                dc.id,
                                dc.business_domain_id,
                                dc.customer_account_id,
                                ca.subject_id,
                                ca.login_name,
                                ca.display_name,
                                ca.phone,
                                ca.email,
                                dc.status,
                                dc.source,
                                dc.activated_at,
                                dc.disabled_at,
                                dc.created_at,
                                dc.updated_at
                            FROM domain_customer dc
                            JOIN customer_account ca ON ca.id = dc.customer_account_id
                            JOIN identity_subject s ON s.id = ca.subject_id
                            WHERE dc.id = ?
                              AND dc.business_domain_id = ?
                              AND dc.deleted_at IS NULL
                            LIMIT 1
                            """,
                    this::mapCustomerView,
                    customerId,
                    domainId);
        } catch (EmptyResultDataAccessException ex) {
            throw new IllegalArgumentException("domain customer not found");
        }
    }

    private CustomerAccountRow loadCustomerAccount(long customerAccountId) {
        try {
            return jdbcTemplate.queryForObject("""
                            SELECT id, subject_id, login_name, display_name, phone, email
                            FROM customer_account
                            WHERE id = ?
                            LIMIT 1
                            """,
                    (rs, rowNum) -> new CustomerAccountRow(
                            rs.getLong("id"),
                            rs.getLong("subject_id"),
                            rs.getString("login_name"),
                            rs.getString("display_name"),
                            rs.getString("phone"),
                            rs.getString("email")),
                    customerAccountId);
        } catch (EmptyResultDataAccessException ex) {
            throw new IllegalArgumentException("customer account not found");
        }
    }

    private void ensureCustomerNotInDomain(long domainId, long customerAccountId) {
        Integer count = jdbcTemplate.queryForObject("""
                        SELECT COUNT(*)
                        FROM domain_customer
                        WHERE business_domain_id = ?
                          AND customer_account_id = ?
                          AND deleted_at IS NULL
                        """,
                Integer.class,
                domainId,
                customerAccountId);
        if (count != null && count > 0) {
            throw new IllegalArgumentException("该客户已在域内");
        }
    }

    private boolean isCustomerInDomain(long domainId, long customerAccountId) {
        Integer count = jdbcTemplate.queryForObject("""
                        SELECT COUNT(*)
                        FROM domain_customer
                        WHERE business_domain_id = ?
                          AND customer_account_id = ?
                          AND deleted_at IS NULL
                        """,
                Integer.class,
                domainId,
                customerAccountId);
        return count != null && count > 0;
    }

    private DomainCustomerDtos.DomainCustomerView insertDomainCustomerLink(
            long domainId,
            long customerAccountId,
            String source) {
        ensureCustomerNotInDomain(domainId, customerAccountId);
        LocalDateTime now = now();
        jdbcTemplate.update("""
                        INSERT INTO domain_customer (
                            customer_account_id,
                            business_domain_id,
                            status,
                            source,
                            activated_at,
                            disabled_at,
                            deleted_at,
                            created_at,
                            updated_at
                        )
                        VALUES (?, ?, 'active', ?, ?, NULL, NULL, CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3))
                        """,
                customerAccountId,
                domainId,
                source,
                Timestamp.valueOf(now));
        Long id = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        if (id == null) {
            throw new IllegalStateException("域客户创建失败");
        }
        return loadCustomerById(domainId, id);
    }

    private void ensureCustomerLoginNameAvailable(String loginName) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM customer_account WHERE login_name = ?",
                Integer.class,
                loginName);
        if (count != null && count > 0) {
            throw new IllegalArgumentException("登录名已存在");
        }
    }

    private long createIdentitySubject(String phone) {
        jdbcTemplate.update("""
                        INSERT INTO identity_subject (subject_type, phone, status, created_at, updated_at)
                        VALUES ('person', ?, 'active', CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3))
                        """,
                phone);
        Long id = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        if (id == null) {
            throw new IllegalStateException("身份主体创建失败");
        }
        return id;
    }

    private long createCustomerAccount(
            long subjectId,
            String loginName,
            String displayName,
            String phone,
            String email) {
        String tempPassword = UUID.randomUUID().toString().replace("-", "");
        jdbcTemplate.update("""
                        INSERT INTO customer_account (
                            subject_id, login_name, display_name, avatar_url, phone, email, password_hash,
                            must_change_password, status, source, auth_version, password_changed_at,
                            created_at, updated_at
                        )
                        VALUES (?, ?, ?, NULL, ?, ?, ?, 1, 'active', 'local', 1, CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3))
                        """,
                subjectId,
                loginName,
                displayName,
                phone,
                email,
                passwordEncoder.encode(tempPassword));
        Long id = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        if (id == null) {
            throw new IllegalStateException("客户账号创建失败");
        }
        return id;
    }

    private StaffAccountRow loadStaffAccountForDomain(long domainId, long staffAccountId) {
        try {
            return jdbcTemplate.queryForObject("""
                            SELECT sa.id, sa.login_name, sa.phone, sa.email
                            FROM domain_member dm
                            JOIN staff_account sa ON sa.id = dm.staff_account_id
                            WHERE dm.business_domain_id = ?
                              AND dm.staff_account_id = ?
                              AND dm.deleted_at IS NULL
                            LIMIT 1
                            """,
                    (rs, rowNum) -> new StaffAccountRow(
                            rs.getLong("id"),
                            rs.getString("login_name"),
                            rs.getString("phone"),
                            rs.getString("email")),
                    domainId,
                    staffAccountId);
        } catch (EmptyResultDataAccessException ex) {
            throw new IllegalArgumentException("员工不在本业务域");
        }
    }

    private long resolveOrCreateCustomerAccountFromStaff(StaffAccountRow staff) {
        Long existingId = jdbcTemplate.query("""
                        SELECT id FROM customer_account
                        WHERE login_name = ? OR phone = ?
                        LIMIT 1
                        """,
                rs -> rs.next() ? rs.getLong("id") : null,
                staff.loginName(),
                staff.phone());
        if (existingId != null) {
            return existingId;
        }
        long subjectId = createIdentitySubject(staff.phone());
        return createCustomerAccount(
                subjectId,
                staff.loginName(),
                staff.loginName(),
                staff.phone(),
                staff.email());
    }

    private DomainDtos.DomainView loadDomain(long domainId) {
        return domainService.getDomain(domainId);
    }

    private String normalizeStatus(String status) {
        if (!StringUtils.hasText(status)) {
            throw new IllegalArgumentException("status is required");
        }
        String normalized = status.trim().toLowerCase();
        if (!List.of("pending", "active", "disabled").contains(normalized)) {
            throw new IllegalArgumentException("unsupported customer status");
        }
        return normalized;
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    private LocalDateTime toLocalDateTime(java.sql.Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private record CustomerQuery(String whereClause, List<Object> args) {
    }

    private record CustomerAccountRow(
            long id,
            long subjectId,
            String loginName,
            String displayName,
            String phone,
            String email) {
    }

    private record StaffAccountRow(
            long id,
            String loginName,
            String phone,
            String email) {
    }
}
