package com.uniondesk.domain.web;

import com.uniondesk.common.web.PageResult;
import com.uniondesk.domain.core.DomainService;
import com.uniondesk.iam.core.PermissionCodes;
import com.uniondesk.iam.core.RequirePermission;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class DomainController {

    private final DomainService domainService;

    public DomainController(DomainService domainService) {
        this.domainService = domainService;
    }

    @GetMapping("/domains")
    @RequirePermission(PermissionCodes.DOMAIN_READ)
    public PageResult<DomainDtos.DomainBriefView> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "20") int pageSize,
            @RequestParam(required = false) String keyword) {
        return domainService.listCustomerDomains(page, pageSize, keyword);
    }

    @GetMapping("/domains/{id}")
    @RequirePermission(PermissionCodes.DOMAIN_READ)
    public DomainDtos.DomainView get(@PathVariable long id) {
        return domainService.getDomain(id);
    }

    @GetMapping("/admin/domains")
    @RequirePermission(PermissionCodes.DOMAIN_ADMIN_READ)
    public PageResult<DomainDtos.DomainView> listAdmin(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "20") int pageSize,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        return domainService.listAdminDomains(page, pageSize, status, keyword);
    }

    @GetMapping("/admin/domains/{id}")
    @RequirePermission(PermissionCodes.DOMAIN_ADMIN_READ)
    public DomainDtos.DomainView getAdmin(@PathVariable long id) {
        return domainService.getDomain(id);
    }

    @PostMapping("/admin/domains")
    @ResponseStatus(HttpStatus.CREATED)
    @RequirePermission(PermissionCodes.DOMAIN_ADMIN_CREATE)
    public DomainDtos.DomainCreateResponse createAdmin(@Valid @RequestBody DomainDtos.CreateDomainRequest request) {
        return domainService.createDomain(request);
    }

    @PutMapping("/admin/domains/{id}")
    @RequirePermission(PermissionCodes.DOMAIN_ADMIN_UPDATE)
    public DomainDtos.DomainView updateAdmin(@PathVariable long id, @Valid @RequestBody DomainDtos.UpdateDomainRequest request) {
        return domainService.updateDomain(id, request);
    }

    @DeleteMapping("/admin/domains/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequirePermission(PermissionCodes.DOMAIN_ADMIN_DELETE)
    public void deleteAdmin(@PathVariable long id) {
        domainService.deleteDomain(id);
    }
}
