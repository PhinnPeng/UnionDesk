package com.uniondesk.ticket.web;

import com.uniondesk.iam.core.PermissionCodes;
import com.uniondesk.iam.core.RequirePermission;
import com.uniondesk.ticket.core.TicketConfigService;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class TicketConfigController {

    private final TicketConfigService ticketConfigService;

    public TicketConfigController(TicketConfigService ticketConfigService) {
        this.ticketConfigService = ticketConfigService;
    }

    @GetMapping("/admin/domains/{domain_id}/ticket-types")
    @RequirePermission(PermissionCodes.DOMAIN_TICKET_TYPE_READ)
    public List<TicketConfigDtos.TicketTypeView> listTicketTypes(@PathVariable("domain_id") long domainId) {
        return ticketConfigService.listTicketTypes(domainId);
    }

    @PostMapping("/admin/domains/{domain_id}/ticket-types")
    @ResponseStatus(HttpStatus.CREATED)
    @RequirePermission(PermissionCodes.DOMAIN_TICKET_TYPE_CREATE)
    public TicketConfigDtos.TicketTypeView createTicketType(
            @PathVariable("domain_id") long domainId,
            @Valid @RequestBody TicketConfigDtos.CreateTicketTypeRequest request) {
        return ticketConfigService.createTicketType(domainId, request);
    }

    @PutMapping("/admin/domains/{domain_id}/ticket-types/{type_id}")
    @RequirePermission(PermissionCodes.DOMAIN_TICKET_TYPE_UPDATE)
    public TicketConfigDtos.TicketTypeView updateTicketType(
            @PathVariable("domain_id") long domainId,
            @PathVariable("type_id") long typeId,
            @Valid @RequestBody TicketConfigDtos.UpdateTicketTypeRequest request) {
        return ticketConfigService.updateTicketType(domainId, typeId, request);
    }

    @DeleteMapping("/admin/domains/{domain_id}/ticket-types/{type_id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequirePermission(PermissionCodes.DOMAIN_TICKET_TYPE_DELETE)
    public void deleteTicketType(@PathVariable("domain_id") long domainId, @PathVariable("type_id") long typeId) {
        ticketConfigService.deleteTicketType(domainId, typeId);
    }

    @GetMapping("/admin/domains/{domain_id}/ticket-templates")
    @RequirePermission(PermissionCodes.DOMAIN_TICKET_TEMPLATE_READ)
    public List<TicketConfigDtos.TicketTemplateView> listTicketTemplates(@PathVariable("domain_id") long domainId) {
        return ticketConfigService.listTicketTemplates(domainId);
    }

    @PostMapping("/admin/domains/{domain_id}/ticket-templates")
    @ResponseStatus(HttpStatus.CREATED)
    @RequirePermission(PermissionCodes.DOMAIN_TICKET_TEMPLATE_CREATE)
    public TicketConfigDtos.TicketTemplateView createTicketTemplate(
            @PathVariable("domain_id") long domainId,
            @Valid @RequestBody TicketConfigDtos.CreateTicketTemplateRequest request) {
        return ticketConfigService.createTicketTemplate(domainId, request);
    }

    @PutMapping("/admin/domains/{domain_id}/ticket-templates/{template_id}")
    @RequirePermission(PermissionCodes.DOMAIN_TICKET_TEMPLATE_UPDATE)
    public TicketConfigDtos.TicketTemplateView updateTicketTemplate(
            @PathVariable("domain_id") long domainId,
            @PathVariable("template_id") long templateId,
            @Valid @RequestBody TicketConfigDtos.UpdateTicketTemplateRequest request) {
        return ticketConfigService.updateTicketTemplate(domainId, templateId, request);
    }

    @DeleteMapping("/admin/domains/{domain_id}/ticket-templates/{template_id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequirePermission(PermissionCodes.DOMAIN_TICKET_TEMPLATE_DELETE)
    public void deleteTicketTemplate(@PathVariable("domain_id") long domainId, @PathVariable("template_id") long templateId) {
        ticketConfigService.deleteTicketTemplate(domainId, templateId);
    }

    @GetMapping({
            "/admin/domains/{domain_id}/quick-replies",
            "/admin/domains/{domain_id}/quick-reply-templates"
    })
    @RequirePermission(PermissionCodes.DOMAIN_QUICK_REPLY_READ)
    public List<TicketConfigDtos.QuickReplyView> listQuickReplies(@PathVariable("domain_id") long domainId) {
        return ticketConfigService.listQuickReplies(domainId);
    }

    @PostMapping({
            "/admin/domains/{domain_id}/quick-replies",
            "/admin/domains/{domain_id}/quick-reply-templates"
    })
    @ResponseStatus(HttpStatus.CREATED)
    @RequirePermission(PermissionCodes.DOMAIN_QUICK_REPLY_CREATE)
    public TicketConfigDtos.QuickReplyView createQuickReply(
            @PathVariable("domain_id") long domainId,
            @Valid @RequestBody TicketConfigDtos.CreateQuickReplyRequest request) {
        return ticketConfigService.createQuickReply(domainId, request);
    }

    @PutMapping({
            "/admin/domains/{domain_id}/quick-replies/{reply_id}",
            "/admin/domains/{domain_id}/quick-reply-templates/{reply_id}"
    })
    @RequirePermission(PermissionCodes.DOMAIN_QUICK_REPLY_UPDATE)
    public TicketConfigDtos.QuickReplyView updateQuickReply(
            @PathVariable("domain_id") long domainId,
            @PathVariable("reply_id") long replyId,
            @Valid @RequestBody TicketConfigDtos.UpdateQuickReplyRequest request) {
        return ticketConfigService.updateQuickReply(domainId, replyId, request);
    }

    @DeleteMapping({
            "/admin/domains/{domain_id}/quick-replies/{reply_id}",
            "/admin/domains/{domain_id}/quick-reply-templates/{reply_id}"
    })
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequirePermission(PermissionCodes.DOMAIN_QUICK_REPLY_DELETE)
    public void deleteQuickReply(@PathVariable("domain_id") long domainId, @PathVariable("reply_id") long replyId) {
        ticketConfigService.deleteQuickReply(domainId, replyId);
    }

    @GetMapping("/admin/domains/{domain_id}/priority-levels")
    @RequirePermission(PermissionCodes.DOMAIN_PRIORITY_LEVEL_READ)
    public List<TicketConfigDtos.PriorityLevelView> listPriorityLevels(@PathVariable("domain_id") long domainId) {
        return ticketConfigService.listPriorityLevels(domainId);
    }

    @PostMapping("/admin/domains/{domain_id}/priority-levels")
    @ResponseStatus(HttpStatus.CREATED)
    @RequirePermission(PermissionCodes.DOMAIN_PRIORITY_LEVEL_CREATE)
    public TicketConfigDtos.PriorityLevelView createPriorityLevel(
            @PathVariable("domain_id") long domainId,
            @Valid @RequestBody TicketConfigDtos.CreatePriorityLevelRequest request) {
        return ticketConfigService.createPriorityLevel(domainId, request);
    }

    @PutMapping("/admin/domains/{domain_id}/priority-levels/{level_id}")
    @RequirePermission(PermissionCodes.DOMAIN_PRIORITY_LEVEL_UPDATE)
    public TicketConfigDtos.PriorityLevelView updatePriorityLevel(
            @PathVariable("domain_id") long domainId,
            @PathVariable("level_id") long levelId,
            @Valid @RequestBody TicketConfigDtos.UpdatePriorityLevelRequest request) {
        return ticketConfigService.updatePriorityLevel(domainId, levelId, request);
    }

    @DeleteMapping("/admin/domains/{domain_id}/priority-levels/{level_id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequirePermission(PermissionCodes.DOMAIN_PRIORITY_LEVEL_DELETE)
    public void deletePriorityLevel(@PathVariable("domain_id") long domainId, @PathVariable("level_id") long levelId) {
        ticketConfigService.deletePriorityLevel(domainId, levelId);
    }
}
