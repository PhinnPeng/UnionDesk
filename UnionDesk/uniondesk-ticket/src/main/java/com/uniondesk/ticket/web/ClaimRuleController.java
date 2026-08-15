package com.uniondesk.ticket.web;

import com.uniondesk.common.web.PageResult;
import com.uniondesk.iam.core.PermissionCodes;
import com.uniondesk.iam.core.RequirePermission;
import com.uniondesk.ticket.core.ClaimRuleService;
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
@RequestMapping("/api/v1/admin/domains/{domainId}/ticket-claim-rules")
public class ClaimRuleController {

    private final ClaimRuleService claimRuleService;

    public ClaimRuleController(ClaimRuleService claimRuleService) {
        this.claimRuleService = claimRuleService;
    }

    @GetMapping
    @RequirePermission(value = PermissionCodes.DOMAIN_TICKET_CLAIM_RULE_READ, domainIdParam = "domainId")
    public PageResult<ClaimRuleService.ClaimRuleView> listClaimRules(
            @PathVariable long domainId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "20") int pageSize) {
        return claimRuleService.listClaimRules(domainId, page, pageSize);
    }

    @PostMapping
    @RequirePermission(value = PermissionCodes.DOMAIN_TICKET_CLAIM_RULE_CREATE, domainIdParam = "domainId")
    public ClaimRuleService.ClaimRuleView createClaimRule(
            @PathVariable long domainId,
            @Valid @RequestBody ClaimRuleService.ClaimRuleCommand request) {
        return claimRuleService.createClaimRule(domainId, request);
    }

    @PutMapping("/{ruleId}")
    @RequirePermission(value = PermissionCodes.DOMAIN_TICKET_CLAIM_RULE_UPDATE, domainIdParam = "domainId")
    public ClaimRuleService.ClaimRuleView updateClaimRule(
            @PathVariable long domainId,
            @PathVariable long ruleId,
            @Valid @RequestBody ClaimRuleService.ClaimRuleCommand request) {
        return claimRuleService.updateClaimRule(domainId, ruleId, request);
    }

    @DeleteMapping("/{ruleId}")
    @RequirePermission(value = PermissionCodes.DOMAIN_TICKET_CLAIM_RULE_DELETE, domainIdParam = "domainId")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteClaimRule(@PathVariable long domainId, @PathVariable long ruleId) {
        claimRuleService.deleteClaimRule(domainId, ruleId);
    }
}
