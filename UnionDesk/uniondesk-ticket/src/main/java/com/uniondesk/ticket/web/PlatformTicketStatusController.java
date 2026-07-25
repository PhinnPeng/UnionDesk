package com.uniondesk.ticket.web;

import com.uniondesk.iam.core.PermissionCodes;
import com.uniondesk.iam.core.RequirePermission;
import com.uniondesk.ticket.core.TicketStatusService;
import com.uniondesk.ticket.web.TicketStatusDtos.TicketStatusListView;
import com.uniondesk.ticket.web.TicketStatusDtos.TicketStatusView;
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
@RequestMapping("/api/v1/platform")
public class PlatformTicketStatusController {

    private final TicketStatusService ticketStatusService;

    public PlatformTicketStatusController(TicketStatusService ticketStatusService) {
        this.ticketStatusService = ticketStatusService;
    }

    @GetMapping("/ticket-statuses")
    @RequirePermission(PermissionCodes.PLATFORM_TICKET_CONFIG_STATUS_READ)
    public TicketStatusListView listTicketStatuses(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer page,
            @RequestParam(name = "page_size", required = false) Integer pageSize) {
        return ticketStatusService.listPlatform(keyword, page, pageSize);
    }

    @PostMapping("/ticket-statuses")
    @ResponseStatus(HttpStatus.CREATED)
    @RequirePermission(PermissionCodes.PLATFORM_TICKET_CONFIG_STATUS_CREATE)
    public TicketStatusView createTicketStatus(
            @Valid @RequestBody TicketStatusDtos.CreateTicketStatusRequest request) {
        return ticketStatusService.createPlatform(request, null);
    }

    @PutMapping("/ticket-statuses/{status_id}")
    @RequirePermission(PermissionCodes.PLATFORM_TICKET_CONFIG_STATUS_UPDATE)
    public TicketStatusView updateTicketStatus(
            @PathVariable("status_id") long statusId,
            @Valid @RequestBody TicketStatusDtos.UpdateTicketStatusRequest request) {
        return ticketStatusService.updatePlatform(statusId, request, null);
    }

    @DeleteMapping("/ticket-statuses/{status_id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequirePermission(PermissionCodes.PLATFORM_TICKET_CONFIG_STATUS_DELETE)
    public void deleteTicketStatus(@PathVariable("status_id") long statusId) {
        ticketStatusService.deletePlatform(statusId);
    }
}
