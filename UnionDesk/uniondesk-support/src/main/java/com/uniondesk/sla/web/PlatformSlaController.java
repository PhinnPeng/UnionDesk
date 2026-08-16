package com.uniondesk.sla.web;

import com.uniondesk.common.web.PageResult;
import com.uniondesk.iam.core.PermissionCodes;
import com.uniondesk.iam.core.RequirePermission;
import com.uniondesk.sla.core.SlaService;
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

/**
 * 平台全局 SLA 规则（business_domain_id IS NULL），跨业务域兜底。
 */
@RestController
@RequestMapping("/api/v1/admin/platform/sla-rules")
public class PlatformSlaController {

    private final SlaService slaService;

    public PlatformSlaController(SlaService slaService) {
        this.slaService = slaService;
    }

    @GetMapping
    @RequirePermission(PermissionCodes.PLATFORM_SLA_READ)
    public PageResult<SlaService.SlaRuleView> listGlobalSlaRules(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "20") int pageSize) {
        return slaService.listGlobalSlaRules(page, pageSize);
    }

    @PostMapping
    @RequirePermission(PermissionCodes.PLATFORM_SLA_CREATE)
    public SlaService.SlaRuleView createGlobalSlaRule(@Valid @RequestBody SlaService.SlaRuleCommand request) {
        return slaService.createGlobalSlaRule(request);
    }

    @PutMapping("/{ruleId}")
    @RequirePermission(PermissionCodes.PLATFORM_SLA_UPDATE)
    public SlaService.SlaRuleView updateGlobalSlaRule(
            @PathVariable long ruleId,
            @Valid @RequestBody SlaService.SlaRuleCommand request) {
        return slaService.updateGlobalSlaRule(ruleId, request);
    }

    @DeleteMapping("/{ruleId}")
    @RequirePermission(PermissionCodes.PLATFORM_SLA_DELETE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteGlobalSlaRule(@PathVariable long ruleId) {
        slaService.deleteGlobalSlaRule(ruleId);
    }
}
