package com.uniondesk.ticket.web;

import com.uniondesk.iam.core.PermissionCodes;
import com.uniondesk.iam.core.RequirePermission;
import com.uniondesk.ticket.core.TicketTeamTemplateService;
import com.uniondesk.ticket.web.TicketTeamTemplateDtos.TeamTemplateListView;
import com.uniondesk.ticket.web.TicketTeamTemplateDtos.TeamTemplateOptionView;
import com.uniondesk.ticket.web.TicketTeamTemplateDtos.TeamTemplateView;
import jakarta.validation.Valid;
import java.util.List;
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
@RequestMapping("/api/v1/platform/ticket-team-templates")
public class PlatformTicketTeamTemplateController {

    private final TicketTeamTemplateService ticketTeamTemplateService;

    public PlatformTicketTeamTemplateController(TicketTeamTemplateService ticketTeamTemplateService) {
        this.ticketTeamTemplateService = ticketTeamTemplateService;
    }

    @GetMapping
    @RequirePermission(PermissionCodes.PLATFORM_TICKET_CONFIG_TEMPLATE_READ)
    public TeamTemplateListView list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer page,
            @RequestParam(name = "page_size", required = false) Integer pageSize) {
        return ticketTeamTemplateService.list(keyword, page, pageSize);
    }

    @GetMapping("/options")
    @RequirePermission({
            PermissionCodes.PLATFORM_TICKET_CONFIG_TEMPLATE_READ,
            PermissionCodes.DOMAIN_ADMIN_CREATE
    })
    public List<TeamTemplateOptionView> options() {
        return ticketTeamTemplateService.listActiveOptions();
    }

    @GetMapping("/{template_id}")
    @RequirePermission(PermissionCodes.PLATFORM_TICKET_CONFIG_TEMPLATE_READ)
    public TeamTemplateView get(@PathVariable("template_id") long templateId) {
        return ticketTeamTemplateService.get(templateId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @RequirePermission(PermissionCodes.PLATFORM_TICKET_CONFIG_TEMPLATE_CREATE)
    public TeamTemplateView create(@Valid @RequestBody TicketTeamTemplateDtos.CreateTeamTemplateRequest request) {
        return ticketTeamTemplateService.create(request, null);
    }

    @PutMapping("/reorder")
    @RequirePermission(PermissionCodes.PLATFORM_TICKET_CONFIG_TEMPLATE_UPDATE)
    public void reorder(@Valid @RequestBody TicketTeamTemplateDtos.ReorderTeamTemplatesRequest request) {
        ticketTeamTemplateService.reorder(request, null);
    }

    @PutMapping("/{template_id}")
    @RequirePermission(PermissionCodes.PLATFORM_TICKET_CONFIG_TEMPLATE_UPDATE)
    public TeamTemplateView update(
            @PathVariable("template_id") long templateId,
            @Valid @RequestBody TicketTeamTemplateDtos.UpdateTeamTemplateRequest request) {
        return ticketTeamTemplateService.update(templateId, request, null);
    }

    @DeleteMapping("/{template_id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequirePermission(PermissionCodes.PLATFORM_TICKET_CONFIG_TEMPLATE_DELETE)
    public void delete(@PathVariable("template_id") long templateId) {
        ticketTeamTemplateService.delete(templateId);
    }
}
