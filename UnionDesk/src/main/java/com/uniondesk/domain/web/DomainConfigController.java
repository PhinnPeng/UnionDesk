package com.uniondesk.domain.web;

import com.uniondesk.domain.core.DomainConfigService;
import com.uniondesk.iam.core.PermissionCodes;
import com.uniondesk.iam.core.RequirePermission;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/domains/{domainId}/config")
public class DomainConfigController {

    private final DomainConfigService domainConfigService;

    public DomainConfigController(DomainConfigService domainConfigService) {
        this.domainConfigService = domainConfigService;
    }

    @GetMapping
    @RequirePermission(PermissionCodes.DOMAIN_CONFIG_READ)
    public DomainConfigService.DomainConfigView getDomainConfig(@PathVariable long domainId) {
        return domainConfigService.load(domainId);
    }

    @PutMapping
    @RequirePermission(PermissionCodes.DOMAIN_CONFIG_UPDATE)
    public DomainConfigService.DomainConfigView updateDomainConfig(
            @PathVariable long domainId,
            @Valid @RequestBody DomainConfigService.DomainConfigUpdateCommand request) {
        return domainConfigService.update(domainId, request);
    }
}
