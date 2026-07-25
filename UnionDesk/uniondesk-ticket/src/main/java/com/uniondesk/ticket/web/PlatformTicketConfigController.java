package com.uniondesk.ticket.web;

import com.uniondesk.iam.core.PermissionCodes;
import com.uniondesk.iam.core.RequirePermission;
import com.uniondesk.ticket.core.TicketAttributeService;
import com.uniondesk.ticket.core.TicketTypeAttributeSlotService;
import com.uniondesk.ticket.core.TicketTypeService;
import com.uniondesk.ticket.web.TicketAttributeDtos.TicketAttributeListView;
import com.uniondesk.ticket.web.TicketAttributeDtos.TicketAttributeView;
import com.uniondesk.ticket.web.TicketConfigDtos.PlatformTicketTypeDetailView;
import com.uniondesk.ticket.web.TicketConfigDtos.PlatformTicketTypeListView;
import com.uniondesk.ticket.web.TicketConfigDtos.PlatformTicketTypeView;
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
@RequestMapping("/api/v1/admin/platform")
public class PlatformTicketConfigController {

    private final TicketAttributeService ticketAttributeService;
    private final TicketTypeService ticketTypeService;
    private final TicketTypeAttributeSlotService ticketTypeAttributeSlotService;

    public PlatformTicketConfigController(
            TicketAttributeService ticketAttributeService,
            TicketTypeService ticketTypeService,
            TicketTypeAttributeSlotService ticketTypeAttributeSlotService) {
        this.ticketAttributeService = ticketAttributeService;
        this.ticketTypeService = ticketTypeService;
        this.ticketTypeAttributeSlotService = ticketTypeAttributeSlotService;
    }

    @GetMapping("/ticket-attributes")
    @RequirePermission(PermissionCodes.PLATFORM_TICKET_CONFIG_ATTR_READ)
    public TicketAttributeListView listTicketAttributes(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer page,
            @RequestParam(name = "page_size", required = false) Integer pageSize) {
        return ticketAttributeService.listPlatform(keyword, page, pageSize);
    }

    @PostMapping("/ticket-attributes")
    @ResponseStatus(HttpStatus.CREATED)
    @RequirePermission(PermissionCodes.PLATFORM_TICKET_CONFIG_ATTR_CREATE)
    public TicketAttributeView createTicketAttribute(
            @Valid @RequestBody TicketAttributeDtos.CreateTicketAttributeRequest request) {
        return ticketAttributeService.createPlatform(request, null);
    }

    @PutMapping("/ticket-attributes/{attribute_id}")
    @RequirePermission(PermissionCodes.PLATFORM_TICKET_CONFIG_ATTR_UPDATE)
    public TicketAttributeView updateTicketAttribute(
            @PathVariable("attribute_id") long attributeId,
            @Valid @RequestBody TicketAttributeDtos.UpdateTicketAttributeRequest request) {
        return ticketAttributeService.updatePlatform(attributeId, request, null);
    }

    @DeleteMapping("/ticket-attributes/{attribute_id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequirePermission(PermissionCodes.PLATFORM_TICKET_CONFIG_ATTR_DELETE)
    public void deleteTicketAttribute(@PathVariable("attribute_id") long attributeId) {
        ticketAttributeService.deletePlatform(attributeId);
    }

    @PutMapping("/ticket-attributes/reorder")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequirePermission(PermissionCodes.PLATFORM_TICKET_CONFIG_ATTR_UPDATE)
    public void reorderTicketAttributes(@Valid @RequestBody TicketAttributeDtos.ReorderTicketAttributesRequest request) {
        ticketAttributeService.reorderPlatform(request, null);
    }

    @PostMapping("/ticket-attributes/promote-from-domain/{domain_id}")
    @ResponseStatus(HttpStatus.CREATED)
    @RequirePermission(PermissionCodes.PLATFORM_TICKET_CONFIG_ATTR_CREATE)
    public TicketAttributeView promoteFromDomain(
            @PathVariable("domain_id") long domainId,
            @Valid @RequestBody TicketAttributeDtos.PromoteTicketAttributeRequest request) {
        return ticketAttributeService.promoteFromDomain(domainId, request.attribute_id(), null);
    }

    @GetMapping("/ticket-types")
    @RequirePermission(PermissionCodes.PLATFORM_TICKET_CONFIG_TYPE_READ)
    public PlatformTicketTypeListView listTicketTypes(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer page,
            @RequestParam(name = "page_size", required = false) Integer pageSize) {
        return ticketTypeService.listPlatform(keyword, page, pageSize);
    }

    @GetMapping("/ticket-types/{type_id}")
    @RequirePermission(PermissionCodes.PLATFORM_TICKET_CONFIG_TYPE_READ)
    public PlatformTicketTypeDetailView getTicketType(@PathVariable("type_id") long typeId) {
        return ticketTypeService.getPlatformDetail(typeId);
    }

    @PostMapping("/ticket-types")
    @ResponseStatus(HttpStatus.CREATED)
    @RequirePermission(PermissionCodes.PLATFORM_TICKET_CONFIG_TYPE_CREATE)
    public PlatformTicketTypeView createTicketType(
            @Valid @RequestBody TicketConfigDtos.CreatePlatformTicketTypeRequest request) {
        return ticketTypeService.createPlatform(request);
    }

    @PutMapping("/ticket-types/{type_id}")
    @RequirePermission(PermissionCodes.PLATFORM_TICKET_CONFIG_TYPE_UPDATE)
    public PlatformTicketTypeView updateTicketType(
            @PathVariable("type_id") long typeId,
            @Valid @RequestBody TicketConfigDtos.UpdatePlatformTicketTypeRequest request) {
        return ticketTypeService.updatePlatform(typeId, request);
    }

    @DeleteMapping("/ticket-types/{type_id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequirePermission(PermissionCodes.PLATFORM_TICKET_CONFIG_TYPE_DELETE)
    public void deleteTicketType(@PathVariable("type_id") long typeId) {
        ticketTypeService.deletePlatform(typeId);
    }

    @PutMapping("/ticket-types/reorder")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequirePermission(PermissionCodes.PLATFORM_TICKET_CONFIG_TYPE_UPDATE)
    public void reorderTicketTypes(@Valid @RequestBody TicketConfigDtos.ReorderPlatformTicketTypesRequest request) {
        ticketTypeService.reorderPlatform(request);
    }

    @GetMapping("/ticket-types/{type_id}/attribute-slots")
    @RequirePermission(PermissionCodes.PLATFORM_TICKET_CONFIG_TYPE_READ)
    public TicketAttributeDtos.AttributeSlotListView listAttributeSlots(
            @PathVariable("type_id") long typeId) {
        java.util.List<TicketAttributeDtos.AttributeSlotView> slots = ticketTypeAttributeSlotService.listPlatformSlots(typeId);
        return new TicketAttributeDtos.AttributeSlotListView(slots.size(), slots);
    }

    @PostMapping("/ticket-types/{type_id}/attribute-slots")
    @ResponseStatus(HttpStatus.CREATED)
    @RequirePermission(PermissionCodes.PLATFORM_TICKET_CONFIG_TYPE_UPDATE)
    public TicketAttributeDtos.AttributeSlotView insertAttributeSlot(
            @PathVariable("type_id") long typeId,
            @Valid @RequestBody TicketAttributeDtos.InsertAttributeSlotRequest request) {
        return ticketTypeAttributeSlotService.insertPlatformSlot(typeId, request, null);
    }

    @PutMapping("/ticket-types/{type_id}/attribute-slots/{slot_id}")
    @RequirePermission(PermissionCodes.PLATFORM_TICKET_CONFIG_TYPE_UPDATE)
    public TicketAttributeDtos.AttributeSlotView updateAttributeSlot(
            @PathVariable("type_id") long typeId,
            @PathVariable("slot_id") long slotId,
            @Valid @RequestBody TicketAttributeDtos.UpdateAttributeSlotRequest request) {
        return ticketTypeAttributeSlotService.updatePlatformSlot(typeId, slotId, request, null);
    }

    @DeleteMapping("/ticket-types/{type_id}/attribute-slots/{slot_id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequirePermission(PermissionCodes.PLATFORM_TICKET_CONFIG_TYPE_UPDATE)
    public void removeAttributeSlot(
            @PathVariable("type_id") long typeId,
            @PathVariable("slot_id") long slotId) {
        ticketTypeAttributeSlotService.removePlatformSlot(typeId, slotId);
    }

    @PutMapping("/ticket-types/{type_id}/attribute-slots/reorder")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequirePermission(PermissionCodes.PLATFORM_TICKET_CONFIG_TYPE_UPDATE)
    public void reorderAttributeSlots(
            @PathVariable("type_id") long typeId,
            @Valid @RequestBody TicketAttributeDtos.ReorderAttributeSlotsRequest request) {
        ticketTypeAttributeSlotService.reorderPlatformSlots(typeId, request, null);
    }

    @PostMapping("/ticket-types/{type_id}/form-release/draft")
    @RequirePermission(PermissionCodes.PLATFORM_TICKET_CONFIG_TYPE_UPDATE)
    public PlatformTicketTypeDetailView saveFormReleaseDraft(@PathVariable("type_id") long typeId) {
        ticketTypeAttributeSlotService.savePlatformFormReleaseDraft(typeId, null);
        return ticketTypeService.getPlatformDetail(typeId);
    }

    @PostMapping("/ticket-types/{type_id}/form-release/publish")
    @RequirePermission(PermissionCodes.PLATFORM_TICKET_CONFIG_TYPE_UPDATE)
    public PlatformTicketTypeDetailView publishFormRelease(@PathVariable("type_id") long typeId) {
        ticketTypeAttributeSlotService.publishPlatformFormRelease(typeId, null);
        return ticketTypeService.getPlatformDetail(typeId);
    }
}
