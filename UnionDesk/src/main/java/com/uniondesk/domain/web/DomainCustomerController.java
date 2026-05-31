package com.uniondesk.domain.web;

import com.uniondesk.common.web.PageResult;
import com.uniondesk.domain.core.DomainCustomerService;
import com.uniondesk.iam.core.PermissionCodes;
import com.uniondesk.iam.core.RequirePermission;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/domains/{domainId}")
public class DomainCustomerController {

    private final DomainCustomerService domainCustomerService;

    public DomainCustomerController(DomainCustomerService domainCustomerService) {
        this.domainCustomerService = domainCustomerService;
    }

    @GetMapping("/customers")
    @RequirePermission(PermissionCodes.PLATFORM_DOMAIN_CUSTOMER_READ)
    public PageResult<DomainCustomerDtos.DomainCustomerView> listCustomers(
            @PathVariable long domainId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "20") int pageSize,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        return domainCustomerService.listCustomers(domainId, page, pageSize, status, keyword);
    }

    @PostMapping("/customers")
    @ResponseStatus(HttpStatus.CREATED)
    @RequirePermission(PermissionCodes.PLATFORM_DOMAIN_CUSTOMER_CREATE)
    public DomainCustomerDtos.DomainCustomerView addCustomer(
            @PathVariable long domainId,
            @Valid @RequestBody DomainCustomerDtos.CreateDomainCustomerRequest request) {
        return domainCustomerService.addCustomer(domainId, request);
    }

    @PostMapping("/customers/manual")
    @ResponseStatus(HttpStatus.CREATED)
    @RequirePermission(PermissionCodes.PLATFORM_DOMAIN_CUSTOMER_CREATE)
    public DomainCustomerDtos.DomainCustomerView addCustomerManual(
            @PathVariable long domainId,
            @Valid @RequestBody DomainCustomerDtos.CreateDomainCustomerManualRequest request) {
        return domainCustomerService.addCustomerManual(domainId, request);
    }

    @PostMapping("/customers/from-staff")
    @ResponseStatus(HttpStatus.CREATED)
    @RequirePermission(PermissionCodes.PLATFORM_DOMAIN_CUSTOMER_CREATE)
    public DomainCustomerDtos.BatchCreateDomainCustomersResult addCustomersFromStaff(
            @PathVariable long domainId,
            @Valid @RequestBody DomainCustomerDtos.CreateDomainCustomersFromStaffRequest request) {
        return domainCustomerService.addCustomersFromStaff(domainId, request);
    }

    @RequestMapping(path = "/customers/{customerId}/status", method = {RequestMethod.PUT, RequestMethod.PATCH})
    @RequirePermission(PermissionCodes.PLATFORM_DOMAIN_CUSTOMER_UPDATE)
    public DomainCustomerDtos.DomainCustomerView updateCustomerStatus(
            @PathVariable long domainId,
            @PathVariable long customerId,
            @Valid @RequestBody DomainCustomerDtos.UpdateDomainCustomerStatusRequest request) {
        return domainCustomerService.updateCustomerStatus(domainId, customerId, request);
    }
}
