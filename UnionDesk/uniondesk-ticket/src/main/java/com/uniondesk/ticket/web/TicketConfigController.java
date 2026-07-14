package com.uniondesk.ticket.web;

import com.uniondesk.iam.core.PermissionCodes;
import com.uniondesk.iam.core.RequirePermission;
import com.uniondesk.ticket.core.TicketAttributeService;
import com.uniondesk.ticket.core.TicketConfigService;
import com.uniondesk.ticket.core.TicketTypeAttributeSlotService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class TicketConfigController {

    private final TicketConfigService ticketConfigService;
    private final TicketAttributeService ticketAttributeService;
    private final TicketTypeAttributeSlotService ticketTypeAttributeSlotService;

    public TicketConfigController(
            TicketConfigService ticketConfigService,
            TicketAttributeService ticketAttributeService,
            TicketTypeAttributeSlotService ticketTypeAttributeSlotService) {
        this.ticketConfigService = ticketConfigService;
        this.ticketAttributeService = ticketAttributeService;
        this.ticketTypeAttributeSlotService = ticketTypeAttributeSlotService;
    }

    @GetMapping("/admin/domains/{domain_id}/ticket-types")
    @RequirePermission(PermissionCodes.PLATFORM_DOMAIN_CONTROL_TICKET_TYPE_READ)
    public TicketConfigDtos.TicketTypeListView listTicketTypes(@PathVariable("domain_id") long domainId) {
        List<TicketConfigDtos.TicketTypeView> items = ticketConfigService.listTicketTypes(domainId);
        return new TicketConfigDtos.TicketTypeListView(items.size(), items);
    }

    @PostMapping("/admin/domains/{domain_id}/ticket-types")
    @ResponseStatus(HttpStatus.CREATED)
    @RequirePermission(PermissionCodes.PLATFORM_DOMAIN_CONTROL_TICKET_TYPE_CREATE)
    public TicketConfigDtos.TicketTypeView createTicketType(
            @PathVariable("domain_id") long domainId,
            @Valid @RequestBody TicketConfigDtos.CreateTicketTypeRequest request) {
        return ticketConfigService.createTicketType(domainId, request);
    }

    @PutMapping("/admin/domains/{domain_id}/ticket-types/{type_id}")
    @RequirePermission(PermissionCodes.PLATFORM_DOMAIN_CONTROL_TICKET_TYPE_UPDATE)
    public TicketConfigDtos.TicketTypeView updateTicketType(
            @PathVariable("domain_id") long domainId,
            @PathVariable("type_id") long typeId,
            @Valid @RequestBody TicketConfigDtos.UpdateTicketTypeRequest request) {
        return ticketConfigService.updateTicketType(domainId, typeId, request);
    }

    @PutMapping("/admin/domains/{domain_id}/ticket-types/{type_id}/form-schema/draft")
    @RequirePermission(PermissionCodes.PLATFORM_DOMAIN_CONTROL_TICKET_TYPE_UPDATE)
    public TicketConfigDtos.TicketTypeView saveFormSchemaDraft(
            @PathVariable("domain_id") long domainId,
            @PathVariable("type_id") long typeId,
            @Valid @RequestBody TicketConfigDtos.SaveFormSchemaDraftRequest request) {
        return ticketConfigService.saveFormSchemaDraft(domainId, typeId, request.form_schema());
    }

    @PostMapping("/admin/domains/{domain_id}/ticket-types/{type_id}/form-schema/publish")
    @RequirePermission(PermissionCodes.PLATFORM_DOMAIN_CONTROL_TICKET_TYPE_UPDATE)
    public TicketConfigDtos.TicketTypeView publishFormSchema(
            @PathVariable("domain_id") long domainId,
            @PathVariable("type_id") long typeId,
            @Valid @RequestBody TicketConfigDtos.PublishFormSchemaRequest request) {
        return ticketConfigService.publishFormSchema(domainId, typeId, request.form_schema());
    }

    @PostMapping("/admin/domains/{domain_id}/ticket-types/{type_id}/form-release/draft")
    @RequirePermission(PermissionCodes.PLATFORM_DOMAIN_CONTROL_TICKET_TYPE_UPDATE)
    public TicketConfigDtos.TicketTypeView saveFormReleaseDraft(
            @PathVariable("domain_id") long domainId,
            @PathVariable("type_id") long typeId) {
        return ticketTypeAttributeSlotService.saveFormReleaseDraft(domainId, typeId, null);
    }

    @PostMapping("/admin/domains/{domain_id}/ticket-types/{type_id}/form-release/publish")
    @RequirePermission(PermissionCodes.PLATFORM_DOMAIN_CONTROL_TICKET_TYPE_UPDATE)
    public TicketConfigDtos.TicketTypeView publishFormRelease(
            @PathVariable("domain_id") long domainId,
            @PathVariable("type_id") long typeId) {
        return ticketTypeAttributeSlotService.publishFormRelease(domainId, typeId, null);
    }

    @GetMapping({
            "/admin/domains/{domain_id}/ticket-types/{type_id}/form-release/versions",
            "/admin/domains/{domain_id}/ticket-types/{type_id}/form-schema/versions"
    })
    @RequirePermission(PermissionCodes.PLATFORM_DOMAIN_CONTROL_TICKET_TYPE_READ)
    public TicketConfigDtos.FormSchemaVersionsView listFormReleaseVersions(
            @PathVariable("domain_id") long domainId,
            @PathVariable("type_id") long typeId) {
        return ticketConfigService.listFormSchemaVersions(domainId, typeId);
    }

    @GetMapping("/admin/domains/{domain_id}/ticket-types/{type_id}/form-schema/versions/{version_no}")
    @RequirePermission(PermissionCodes.PLATFORM_DOMAIN_CONTROL_TICKET_TYPE_READ)
    public TicketConfigDtos.FormSchemaVersionDetailView getFormSchemaVersion(
            @PathVariable("domain_id") long domainId,
            @PathVariable("type_id") long typeId,
            @PathVariable("version_no") int versionNo) {
        return ticketConfigService.getFormSchemaVersion(domainId, typeId, versionNo);
    }

    @PostMapping("/admin/domains/{domain_id}/ticket-types/{type_id}/form-schema/versions/{version_no}/rollback")
    @RequirePermission(PermissionCodes.PLATFORM_DOMAIN_CONTROL_TICKET_TYPE_UPDATE)
    public TicketConfigDtos.TicketTypeView rollbackFormSchemaVersion(
            @PathVariable("domain_id") long domainId,
            @PathVariable("type_id") long typeId,
            @PathVariable("version_no") int versionNo) {
        return ticketConfigService.rollbackFormSchemaVersion(domainId, typeId, versionNo);
    }

    @GetMapping("/admin/domains/{domain_id}/ticket-attributes")
    @RequirePermission(PermissionCodes.PLATFORM_DOMAIN_CONTROL_TICKET_ATTRIBUTE_READ)
    public TicketAttributeDtos.TicketAttributeListView listDomainTicketAttributes(
            @PathVariable("domain_id") long domainId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer page,
            @RequestParam(name = "page_size", required = false) Integer pageSize) {
        return ticketAttributeService.listDomain(domainId, keyword, page, pageSize);
    }

    @PostMapping("/admin/domains/{domain_id}/ticket-attributes")
    @ResponseStatus(HttpStatus.CREATED)
    @RequirePermission(PermissionCodes.PLATFORM_DOMAIN_CONTROL_TICKET_ATTRIBUTE_CREATE)
    public TicketAttributeDtos.TicketAttributeView createDomainTicketAttribute(
            @PathVariable("domain_id") long domainId,
            @Valid @RequestBody TicketAttributeDtos.CreateTicketAttributeRequest request) {
        return ticketAttributeService.createDomain(domainId, request, null);
    }

    @PutMapping("/admin/domains/{domain_id}/ticket-attributes/{attribute_id}")
    @RequirePermission(PermissionCodes.PLATFORM_DOMAIN_CONTROL_TICKET_ATTRIBUTE_UPDATE)
    public TicketAttributeDtos.TicketAttributeView updateDomainTicketAttribute(
            @PathVariable("domain_id") long domainId,
            @PathVariable("attribute_id") long attributeId,
            @Valid @RequestBody TicketAttributeDtos.UpdateTicketAttributeRequest request) {
        return ticketAttributeService.updateDomain(domainId, attributeId, request, null);
    }

    @DeleteMapping("/admin/domains/{domain_id}/ticket-attributes/{attribute_id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequirePermission(PermissionCodes.PLATFORM_DOMAIN_CONTROL_TICKET_ATTRIBUTE_DELETE)
    public void deleteDomainTicketAttribute(
            @PathVariable("domain_id") long domainId,
            @PathVariable("attribute_id") long attributeId) {
        ticketAttributeService.deleteDomain(domainId, attributeId);
    }

    @PutMapping("/admin/domains/{domain_id}/ticket-attributes/reorder")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequirePermission(PermissionCodes.PLATFORM_DOMAIN_CONTROL_TICKET_ATTRIBUTE_UPDATE)
    public void reorderDomainTicketAttributes(
            @PathVariable("domain_id") long domainId,
            @Valid @RequestBody TicketAttributeDtos.ReorderTicketAttributesRequest request) {
        ticketAttributeService.reorderDomain(domainId, request, null);
    }

    @GetMapping("/admin/domains/{domain_id}/ticket-types/{type_id}/attribute-slots")
    @RequirePermission(PermissionCodes.PLATFORM_DOMAIN_CONTROL_TICKET_TYPE_READ)
    public TicketAttributeDtos.AttributeSlotListView listAttributeSlots(
            @PathVariable("domain_id") long domainId,
            @PathVariable("type_id") long typeId) {
        List<TicketAttributeDtos.AttributeSlotView> slots = ticketTypeAttributeSlotService.listSlots(domainId, typeId);
        return new TicketAttributeDtos.AttributeSlotListView(slots.size(), slots);
    }

    @PostMapping("/admin/domains/{domain_id}/ticket-types/{type_id}/attribute-slots")
    @ResponseStatus(HttpStatus.CREATED)
    @RequirePermission(PermissionCodes.PLATFORM_DOMAIN_CONTROL_TICKET_TYPE_UPDATE)
    public TicketAttributeDtos.AttributeSlotView insertAttributeSlot(
            @PathVariable("domain_id") long domainId,
            @PathVariable("type_id") long typeId,
            @Valid @RequestBody TicketAttributeDtos.InsertAttributeSlotRequest request) {
        return ticketTypeAttributeSlotService.insertSlot(domainId, typeId, request, null);
    }

    @PutMapping("/admin/domains/{domain_id}/ticket-types/{type_id}/attribute-slots/{slot_id}")
    @RequirePermission(PermissionCodes.PLATFORM_DOMAIN_CONTROL_TICKET_TYPE_UPDATE)
    public TicketAttributeDtos.AttributeSlotView updateAttributeSlot(
            @PathVariable("domain_id") long domainId,
            @PathVariable("type_id") long typeId,
            @PathVariable("slot_id") long slotId,
            @Valid @RequestBody TicketAttributeDtos.UpdateAttributeSlotRequest request) {
        return ticketTypeAttributeSlotService.updateSlot(domainId, typeId, slotId, request, null);
    }

    @DeleteMapping("/admin/domains/{domain_id}/ticket-types/{type_id}/attribute-slots/{slot_id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequirePermission(PermissionCodes.PLATFORM_DOMAIN_CONTROL_TICKET_TYPE_UPDATE)
    public void removeAttributeSlot(
            @PathVariable("domain_id") long domainId,
            @PathVariable("type_id") long typeId,
            @PathVariable("slot_id") long slotId) {
        ticketTypeAttributeSlotService.removeSlot(domainId, typeId, slotId);
    }

    @PutMapping("/admin/domains/{domain_id}/ticket-types/{type_id}/attribute-slots/reorder")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequirePermission(PermissionCodes.PLATFORM_DOMAIN_CONTROL_TICKET_TYPE_UPDATE)
    public void reorderAttributeSlots(
            @PathVariable("domain_id") long domainId,
            @PathVariable("type_id") long typeId,
            @Valid @RequestBody TicketAttributeDtos.ReorderAttributeSlotsRequest request) {
        ticketTypeAttributeSlotService.reorderSlots(domainId, typeId, request, null);
    }

    @DeleteMapping("/admin/domains/{domain_id}/ticket-types/{type_id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequirePermission(PermissionCodes.PLATFORM_DOMAIN_CONTROL_TICKET_TYPE_DELETE)
    public void deleteTicketType(@PathVariable("domain_id") long domainId, @PathVariable("type_id") long typeId) {
        ticketConfigService.deleteTicketType(domainId, typeId);
    }

    @GetMapping("/admin/domains/{domain_id}/ticket-templates")
    @RequirePermission(PermissionCodes.PLATFORM_DOMAIN_CONTROL_TICKET_TYPE_READ)
    public TicketConfigDtos.TicketTemplateListView listTicketTemplates(@PathVariable("domain_id") long domainId) {
        List<TicketConfigDtos.TicketTemplateView> items = ticketConfigService.listTicketTemplates(domainId);
        return new TicketConfigDtos.TicketTemplateListView(items.size(), items);
    }

    @PostMapping("/admin/domains/{domain_id}/ticket-templates")
    @ResponseStatus(HttpStatus.CREATED)
    @RequirePermission(PermissionCodes.PLATFORM_DOMAIN_CONTROL_TICKET_TYPE_CREATE)
    public TicketConfigDtos.TicketTemplateView createTicketTemplate(
            @PathVariable("domain_id") long domainId,
            @Valid @RequestBody TicketConfigDtos.CreateTicketTemplateRequest request) {
        return ticketConfigService.createTicketTemplate(domainId, request);
    }

    @PutMapping("/admin/domains/{domain_id}/ticket-templates/{template_id}")
    @RequirePermission(PermissionCodes.PLATFORM_DOMAIN_CONTROL_TICKET_TYPE_UPDATE)
    public TicketConfigDtos.TicketTemplateView updateTicketTemplate(
            @PathVariable("domain_id") long domainId,
            @PathVariable("template_id") long templateId,
            @Valid @RequestBody TicketConfigDtos.UpdateTicketTemplateRequest request) {
        return ticketConfigService.updateTicketTemplate(domainId, templateId, request);
    }

    @DeleteMapping("/admin/domains/{domain_id}/ticket-templates/{template_id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequirePermission(PermissionCodes.PLATFORM_DOMAIN_CONTROL_TICKET_TYPE_DELETE)
    public void deleteTicketTemplate(@PathVariable("domain_id") long domainId, @PathVariable("template_id") long templateId) {
        ticketConfigService.deleteTicketTemplate(domainId, templateId);
    }

    @GetMapping({
            "/admin/domains/{domain_id}/quick-replies",
            "/admin/domains/{domain_id}/quick-reply-templates"
    })
    @RequirePermission(PermissionCodes.DOMAIN_QUICK_REPLY_READ)
    public TicketConfigDtos.QuickReplyListView listQuickReplies(@PathVariable("domain_id") long domainId) {
        List<TicketConfigDtos.QuickReplyView> items = ticketConfigService.listQuickReplies(domainId);
        return new TicketConfigDtos.QuickReplyListView(items.size(), items);
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
    public TicketConfigDtos.PriorityLevelListView listPriorityLevels(@PathVariable("domain_id") long domainId) {
        List<TicketConfigDtos.PriorityLevelView> items = ticketConfigService.listPriorityLevels(domainId);
        return new TicketConfigDtos.PriorityLevelListView(items.size(), items);
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
