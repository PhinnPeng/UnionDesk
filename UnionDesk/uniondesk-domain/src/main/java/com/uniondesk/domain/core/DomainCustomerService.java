package com.uniondesk.domain.core;

import com.uniondesk.common.web.PageResult;
import com.uniondesk.domain.entity.DomainCustomerPo;
import com.uniondesk.domain.entity.StaffAccountPo;
import com.uniondesk.domain.repository.DomainCustomerRepository;
import com.uniondesk.domain.web.DomainCustomerDtos;
import com.uniondesk.domain.web.DomainDtos;
import com.uniondesk.iam.core.CustomerAccountService;
import com.uniondesk.iam.core.IdentitySubjectService;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class DomainCustomerService {

    private final DomainCustomerRepository domainCustomerRepository;
    private final DomainService domainService;
    private final Clock clock;
    private final IdentitySubjectService identitySubjectService;
    private final CustomerAccountService customerAccountService;

    public DomainCustomerService(
            DomainCustomerRepository domainCustomerRepository,
            DomainService domainService,
            Clock clock,
            IdentitySubjectService identitySubjectService,
            CustomerAccountService customerAccountService) {
        this.domainCustomerRepository = domainCustomerRepository;
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
        String normalizedStatus = StringUtils.hasText(status) ? normalizeStatus(status) : null;
        String keywordLike = StringUtils.hasText(keyword) ? "%" + keyword.trim() + "%" : null;
        int normalizedPage = Math.max(page, 1);
        int normalizedPageSize = Math.max(pageSize, 1);
        long offset = (normalizedPage - 1L) * normalizedPageSize;

        long total = domainCustomerRepository.countCustomers(domainId, normalizedStatus, keywordLike);
        List<DomainCustomerPo> pos = domainCustomerRepository.findCustomers(
                domainId, normalizedStatus, keywordLike, normalizedPageSize, offset);
        return new PageResult<>(total, pos.stream().map(this::toCustomerView).toList());
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
            StaffAccountPo staff = loadStaffAccountForDomain(domainId, staffAccountId);
            long customerAccountId = resolveOrCreateCustomerAccountFromStaff(staff);
            if (isCustomerInDomain(domainId, customerAccountId)) {
                throw new IllegalArgumentException(staff.getUsername() + "：该员工已作为客户存在");
            }
            items.add(insertDomainCustomerLink(domainId, customerAccountId, "staff_import"));
            added += 1;
        }
        return new DomainCustomerDtos.BatchCreateDomainCustomersResult(added, skipped, items);
    }

    public DomainCustomerDtos.DomainCustomerView getCustomer(long domainId, long customerId) {
        loadDomain(domainId);
        return loadCustomerById(domainId, customerId);
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
        LocalDateTime activatedAt = "active".equals(status)
                ? now
                : (current.activated_at() == null ? now : current.activated_at());
        LocalDateTime disabledAt = "disabled".equals(status) ? now : null;
        domainCustomerRepository.updateStatus(status, activatedAt, disabledAt, customerId, domainId);
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

    private DomainCustomerDtos.DomainCustomerView loadCustomerById(long domainId, long customerId) {
        DomainCustomerPo po = domainCustomerRepository.findById(customerId, domainId);
        if (po == null) {
            throw new IllegalArgumentException("domain customer not found");
        }
        return toCustomerView(po);
    }

    private DomainCustomerDtos.DomainCustomerView toCustomerView(DomainCustomerPo po) {
        return new DomainCustomerDtos.DomainCustomerView(
                po.getId(),
                po.getBusinessDomainId(),
                po.getCustomerAccountId(),
                po.getSubjectId(),
                po.getUsername(),
                po.getNickname(),
                po.getPhone(),
                po.getEmail(),
                po.getStatus(),
                po.getSource(),
                po.getActivatedAt(),
                po.getDisabledAt(),
                po.getCreatedAt(),
                po.getUpdatedAt());
    }

    private CustomerAccountService.CustomerAccount loadCustomerAccount(long customerAccountId) {
        return customerAccountService.findById(customerAccountId)
                .orElseThrow(() -> new IllegalArgumentException("customer account not found"));
    }

    private boolean isCustomerInDomain(long domainId, long customerAccountId) {
        return domainCustomerRepository.countByDomainAndCustomer(domainId, customerAccountId) > 0;
    }

    private DomainCustomerDtos.DomainCustomerView insertDomainCustomerLink(
            long domainId,
            long customerAccountId,
            String source) {
        ensureCustomerNotInDomain(domainId, customerAccountId);
        LocalDateTime now = now();
        long id = domainCustomerRepository.insert(customerAccountId, domainId, source, now);
        return loadCustomerById(domainId, id);
    }

    private void ensureCustomerNotInDomain(long domainId, long customerAccountId) {
        if (isCustomerInDomain(domainId, customerAccountId)) {
            throw new IllegalArgumentException("该客户已在域内");
        }
    }

    private StaffAccountPo loadStaffAccountForDomain(long domainId, long staffAccountId) {
        StaffAccountPo staff = domainCustomerRepository.findStaffInDomain(domainId, staffAccountId);
        if (staff == null) {
            throw new IllegalArgumentException("员工不在本业务域");
        }
        return staff;
    }

    private long resolveOrCreateCustomerAccountFromStaff(StaffAccountPo staff) {
        return customerAccountService.findIdByUsernameOrPhone(staff.getUsername(), staff.getPhone())
                .orElseGet(() -> {
                    identitySubjectService.resolveSubjectIdByPhone(staff.getPhone());
                    return customerAccountService.create(new CustomerAccountService.CreateCustomerCommand(
                            staff.getUsername(),
                            staff.getUsername(),
                            staff.getPhone(),
                            staff.getEmail(),
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
}
