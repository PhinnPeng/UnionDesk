package com.uniondesk.domain.core;

import com.uniondesk.common.web.PageResult;
import com.uniondesk.domain.web.DomainCustomerDtos;
import com.uniondesk.domain.web.DomainDtos;
import com.uniondesk.iam.core.CustomerAccountService;
import com.uniondesk.iam.core.IdentitySubjectService;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class DomainCustomerService {

    private final JdbcTemplate jdbcTemplate;
    private final DomainService domainService;
    private final Clock clock;
    private final IdentitySubjectService identitySubjectService;
    private final CustomerAccountService customerAccountService;

    public DomainCustomerService(
            JdbcTemplate jdbcTemplate,
            DomainService domainService,
            Clock clock,
            IdentitySubjectService identitySubjectService,
            CustomerAccountService customerAccountService) {
        this.jdbcTemplate = jdbcTemplate;
        this.domainService = domainService;
        this.clock = clock;
        this.identitySubjectService = identitySubjectService;
        this.customerAccountService = customerAccountService;
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
                                    ca.username,
                                    ca.nickname,
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
        String username = request.username().trim();
        String nickname = request.nickname().trim();
        String phone = request.phone().trim();
        String email = StringUtils.hasText(request.email()) ? request.email().trim() : null;
        customerAccountService.ensureUsernameAvailable(username);
        long customerAccountId = customerAccountService.create(new CustomerAccountService.CreateCustomerCommand(
                username,
                nickname,
                phone,
                email,
                java.util.UUID.randomUUID().toString().replace("-", ""),
                true));
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
                throw new IllegalArgumentException(staff.username() + "：该员工已作为客户存在");
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
        Timestamp activatedAt = "active".equals(status) ? Timestamp.valueOf(now) : Timestamp.valueOf(current.activated_at() == null ? now : current.activated_at());
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
                current.business_domain_id(),
                current.customer_account_id(),
                current.subject_id(),
                current.username(),
                current.nickname(),
                current.phone(),
                current.email(),
                status,
                current.source(),
                "active".equals(status) ? now : current.activated_at(),
                "disabled".equals(status) ? now : null,
                current.created_at(),
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
            conditions.add("(ca.username LIKE ? OR ca.nickname LIKE ? OR ca.phone LIKE ? OR ca.email LIKE ?)");
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
                rs.getString("username"),
                rs.getString("nickname"),
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
                                ca.username,
                                ca.nickname,
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

    private CustomerAccountService.CustomerAccount loadCustomerAccount(long customerAccountId) {
        return customerAccountService.findById(customerAccountId)
                .orElseThrow(() -> new IllegalArgumentException("customer account not found"));
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

    private void ensureCustomerNotInDomain(long domainId, long customerAccountId) {
        if (isCustomerInDomain(domainId, customerAccountId)) {
            throw new IllegalArgumentException("该客户已在域内");
        }
    }

    private StaffAccountRow loadStaffAccountForDomain(long domainId, long staffAccountId) {
        try {
            return jdbcTemplate.queryForObject("""
                            SELECT sa.id, sa.username, sa.phone, sa.email
                            FROM domain_member dm
                            JOIN staff_account sa ON sa.id = dm.staff_account_id
                            WHERE dm.business_domain_id = ?
                              AND dm.staff_account_id = ?
                              AND dm.deleted_at IS NULL
                            LIMIT 1
                            """,
                    (rs, rowNum) -> new StaffAccountRow(
                            rs.getLong("id"),
                            rs.getString("username"),
                            rs.getString("phone"),
                            rs.getString("email")),
                    domainId,
                    staffAccountId);
        } catch (EmptyResultDataAccessException ex) {
            throw new IllegalArgumentException("员工不在本业务域");
        }
    }

    private long resolveOrCreateCustomerAccountFromStaff(StaffAccountRow staff) {
        return customerAccountService.findIdByUsernameOrPhone(staff.username(), staff.phone())
                .orElseGet(() -> {
                    long subjectId = identitySubjectService.resolveSubjectIdByPhone(staff.phone());
                    return customerAccountService.create(new CustomerAccountService.CreateCustomerCommand(
                            staff.username(),
                            staff.username(),
                            staff.phone(),
                            staff.email(),
                            java.util.UUID.randomUUID().toString().replace("-", ""),
                            true));
                });
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

    private record StaffAccountRow(
            long id,
            String username,
            String phone,
            String email) {
    }
}
