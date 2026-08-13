package com.uniondesk.ticket.web;

import com.uniondesk.auth.core.UserContext;
import com.uniondesk.auth.core.UserContextHolder;
import com.uniondesk.iam.core.PermissionCodes;
import com.uniondesk.iam.core.RequirePermission;
import com.uniondesk.ticket.core.TicketSatisfactionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class SatisfactionController {

    private final TicketSatisfactionService satisfactionService;

    public SatisfactionController(TicketSatisfactionService satisfactionService) {
        this.satisfactionService = satisfactionService;
    }

    @GetMapping("/domains/{domain_id}/tickets/my/{ticket_id}/satisfaction")
    @RequirePermission(value = PermissionCodes.TICKET_VIEW_SELF, domainIdParam = "domain_id")
    public TicketSatisfactionService.SatisfactionView getSatisfaction(
            @PathVariable("domain_id") long domainId,
            @PathVariable("ticket_id") long ticketId) {
        return satisfactionService.getByTicket(domainId, ticketId, requireCurrent().userId());
    }

    @PostMapping("/domains/{domain_id}/tickets/my/{ticket_id}/satisfaction")
    @RequirePermission(value = PermissionCodes.TICKET_VIEW_SELF, domainIdParam = "domain_id")
    public TicketSatisfactionService.SatisfactionSubmissionResult submitSatisfaction(
            @PathVariable("domain_id") long domainId,
            @PathVariable("ticket_id") long ticketId,
            @RequestBody TicketSatisfactionService.SubmitSatisfactionCommand command) {
        return satisfactionService.submit(domainId, ticketId, requireCurrent().userId(), command.rating(), command.comment());
    }

    private UserContext requireCurrent() {
        return UserContextHolder.current()
                .orElseThrow(() -> new IllegalStateException("user context is not available"));
    }
}
