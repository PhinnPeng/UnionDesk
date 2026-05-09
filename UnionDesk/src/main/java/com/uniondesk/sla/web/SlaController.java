package com.uniondesk.sla.web;

import com.uniondesk.common.web.PageResult;
import com.uniondesk.iam.core.PermissionCodes;
import com.uniondesk.iam.core.RequirePermission;
import com.uniondesk.sla.core.SlaService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/v1/admin/domains/{domainId}")
public class SlaController {

    private final SlaService slaService;

    public SlaController(SlaService slaService) {
        this.slaService = slaService;
    }

    @GetMapping("/sla-rules")
    @RequirePermission(PermissionCodes.DOMAIN_SLA_READ)
    public PageResult<SlaService.SlaRuleView> listSlaRules(
            @PathVariable long domainId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "20") int pageSize) {
        return slaService.listSlaRules(domainId, page, pageSize);
    }

    @PostMapping("/sla-rules")
    @RequirePermission(PermissionCodes.DOMAIN_SLA_CREATE)
    public SlaService.SlaRuleView createSlaRule(
            @PathVariable long domainId,
            @Valid @RequestBody SlaService.SlaRuleCommand request) {
        return slaService.createSlaRule(domainId, request);
    }

    @PutMapping("/sla-rules/{ruleId}")
    @RequirePermission(PermissionCodes.DOMAIN_SLA_UPDATE)
    public SlaService.SlaRuleView updateSlaRule(
            @PathVariable long domainId,
            @PathVariable long ruleId,
            @Valid @RequestBody SlaService.SlaRuleCommand request) {
        return slaService.updateSlaRule(domainId, ruleId, request);
    }

    @DeleteMapping("/sla-rules/{ruleId}")
    @RequirePermission(PermissionCodes.DOMAIN_SLA_UPDATE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSlaRule(@PathVariable long domainId, @PathVariable long ruleId) {
        slaService.deleteSlaRule(domainId, ruleId);
    }

    @GetMapping("/sla-calendars")
    @RequirePermission(PermissionCodes.DOMAIN_SLA_READ)
    public PageResult<SlaService.SlaCalendarView> listSlaCalendars(
            @PathVariable long domainId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "20") int pageSize) {
        return slaService.listSlaCalendars(domainId, page, pageSize);
    }

    @PostMapping("/sla-calendars")
    @RequirePermission(PermissionCodes.DOMAIN_SLA_CREATE)
    public SlaService.SlaCalendarView createSlaCalendar(
            @PathVariable long domainId,
            @Valid @RequestBody SlaService.SlaCalendarCommand request) {
        return slaService.createSlaCalendar(domainId, request);
    }

    @PutMapping("/sla-calendars/{calendarId}")
    @RequirePermission(PermissionCodes.DOMAIN_SLA_UPDATE)
    public SlaService.SlaCalendarView updateSlaCalendar(
            @PathVariable long domainId,
            @PathVariable long calendarId,
            @Valid @RequestBody SlaService.SlaCalendarCommand request) {
        return slaService.updateSlaCalendar(domainId, calendarId, request);
    }
}
