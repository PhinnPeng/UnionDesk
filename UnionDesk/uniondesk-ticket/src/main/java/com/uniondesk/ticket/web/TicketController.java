package com.uniondesk.ticket.web;

import com.uniondesk.auth.core.UserContext;
import com.uniondesk.auth.core.UserContextHolder;
import com.uniondesk.common.web.PageResult;
import com.uniondesk.iam.core.PermissionCodes;
import com.uniondesk.iam.core.RequirePermission;
import com.uniondesk.ticket.core.TicketService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/v1")
public class TicketController {

    // --- ListView records for list endpoints ---

    public record TicketListView(long total, List<TicketService.TicketRow> items) {}

    public record TicketHistoryListView(long total, List<TicketService.TicketHistoryRow> items) {}

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @PostMapping("/domains/{domain_id}/tickets")
    @ResponseStatus(HttpStatus.CREATED)
    @RequirePermission(value = PermissionCodes.TICKET_CREATE, domainIdParam = "domain_id")
    public TicketService.TicketSubmissionResult createCustomerTicket(
            @PathVariable("domain_id") long domainId,
            @Valid @RequestBody TicketService.CreateTicketCommand request) {
        return ticketService.createCustomerTicket(requireCurrent(), domainId, request);
    }

    @GetMapping("/domains/{domain_id}/tickets/my")
    @RequirePermission(value = PermissionCodes.TICKET_VIEW_SELF, domainIdParam = "domain_id")
    public TicketListView listCustomerTickets(
            @PathVariable("domain_id") long domainId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "100") int limit) {
        List<TicketService.TicketRow> items = ticketService.listCustomerTickets(domainId, requireCurrent().userId(), status, limit);
        return new TicketListView(items.size(), items);
    }

    @GetMapping("/domains/{domain_id}/tickets/my/{ticket_id}")
    @RequirePermission(value = PermissionCodes.TICKET_VIEW_SELF, domainIdParam = "domain_id")
    public TicketService.TicketDetailResult getCustomerTicketDetail(
            @PathVariable("domain_id") long domainId,
            @PathVariable("ticket_id") long ticketId) {
        return ticketService.getCustomerTicketDetail(requireCurrent(), domainId, ticketId);
    }

    @PostMapping("/domains/{domain_id}/tickets/my/{ticket_id}/replies")
    @RequirePermission(value = PermissionCodes.TICKET_REPLY_SELF, domainIdParam = "domain_id")
    public TicketService.TicketActionResult replyCustomerTicket(
            @PathVariable("domain_id") long domainId,
            @PathVariable("ticket_id") long ticketId,
            @Valid @RequestBody TicketService.ReplyTicketCommand request) {
        return ticketService.replyCustomerTicket(requireCurrent(), domainId, ticketId, request);
    }

    @PostMapping("/domains/{domain_id}/tickets/my/{ticket_id}/withdraw")
    @RequirePermission(value = PermissionCodes.TICKET_WITHDRAW_SELF, domainIdParam = "domain_id")
    public TicketService.TicketActionResult withdrawCustomerTicket(
            @PathVariable("domain_id") long domainId,
            @PathVariable("ticket_id") long ticketId,
            @Valid @RequestBody TicketService.WithdrawTicketCommand request) {
        return ticketService.withdrawCustomerTicket(requireCurrent(), domainId, ticketId, request);
    }

    @GetMapping("/admin/domains/{domain_id}/tickets")
    @RequirePermission(value = PermissionCodes.TICKET_VIEW_DOMAIN_ALL, domainIdParam = "domain_id")
    public PageResult<TicketService.TicketRow> listAdminTickets(
            @PathVariable("domain_id") long domainId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "page_size", required = false) Integer pageSize,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long assignee,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "false") boolean assigned_to_me,
            @RequestParam(name = "sla_status", required = false) String sla_status,
            @RequestParam(defaultValue = "100") int limit) {
        // 兼容旧调用：未传 page_size 时回退到 limit（保持原有一次拉取行为）
        int effectivePageSize = pageSize != null ? pageSize : limit;
        return ticketService.listAdminTickets(
                requireCurrent(),
                domainId,
                page,
                effectivePageSize,
                status,
                assignee,
                priority,
                keyword,
                assigned_to_me,
                sla_status);
    }

    @GetMapping("/admin/domains/{domain_id}/tickets/{ticket_id}")
    @RequirePermission(value = PermissionCodes.TICKET_VIEW_DOMAIN_ALL, domainIdParam = "domain_id")
    public TicketService.TicketDetailResult getAdminTicketDetail(
            @PathVariable("domain_id") long domainId,
            @PathVariable("ticket_id") long ticketId) {
        return ticketService.getTicketDetail(domainId, ticketId);
    }

    @PostMapping("/admin/domains/{domain_id}/tickets/{ticket_id}/claim")
    @RequirePermission(value = PermissionCodes.TICKET_CLAIM, domainIdParam = "domain_id")
    public TicketService.TicketActionResult claimTicket(
            @PathVariable("domain_id") long domainId,
            @PathVariable("ticket_id") long ticketId,
            @Valid @RequestBody TicketService.ClaimTicketCommand request) {
        return ticketService.claimTicket(requireCurrent(), domainId, ticketId, request);
    }

    @PostMapping("/admin/domains/{domain_id}/tickets/{ticket_id}/assign")
    @RequirePermission(value = PermissionCodes.TICKET_ASSIGN, domainIdParam = "domain_id")
    public TicketService.TicketActionResult assignTicket(
            @PathVariable("domain_id") long domainId,
            @PathVariable("ticket_id") long ticketId,
            @Valid @RequestBody TicketService.AssignTicketCommand request) {
        return ticketService.assignTicket(requireCurrent(), domainId, ticketId, request);
    }

    @PostMapping("/admin/domains/{domain_id}/tickets/{ticket_id}/watchers")
    @RequirePermission(value = PermissionCodes.TICKET_ASSIGN, domainIdParam = "domain_id")
    public TicketService.TicketActionResult replaceTicketWatchers(
            @PathVariable("domain_id") long domainId,
            @PathVariable("ticket_id") long ticketId,
            @Valid @RequestBody TicketService.ReplaceWatchersCommand request) {
        return ticketService.replaceTicketWatchers(requireCurrent(), domainId, ticketId, request);
    }

    @PostMapping("/admin/domains/{domain_id}/tickets/{ticket_id}/replies")
    @RequirePermission(value = PermissionCodes.TICKET_REPLY, domainIdParam = "domain_id")
    public TicketService.TicketActionResult replyAdminTicket(
            @PathVariable("domain_id") long domainId,
            @PathVariable("ticket_id") long ticketId,
            @Valid @RequestBody TicketService.ReplyTicketCommand request) {
        return ticketService.replyTicket(requireCurrent(), domainId, ticketId, request);
    }

    @PatchMapping("/admin/domains/{domain_id}/tickets/{ticket_id}/status")
    @RequirePermission(value = PermissionCodes.TICKET_CLOSE, domainIdParam = "domain_id")
    public TicketService.TicketActionResult changeTicketStatus(
            @PathVariable("domain_id") long domainId,
            @PathVariable("ticket_id") long ticketId,
            @Valid @RequestBody TicketService.ChangeTicketStatusCommand request) {
        return ticketService.changeTicketStatus(requireCurrent(), domainId, ticketId, request);
    }

    @GetMapping("/admin/domains/{domain_id}/tickets/{ticket_id}/history")
    @RequirePermission(value = PermissionCodes.TICKET_VIEW_DOMAIN_ALL, domainIdParam = "domain_id")
    public TicketHistoryListView listTicketHistory(
            @PathVariable("domain_id") long domainId,
            @PathVariable("ticket_id") long ticketId) {
        List<TicketService.TicketHistoryRow> items = ticketService.listTicketHistory(domainId, ticketId);
        return new TicketHistoryListView(items.size(), items);
    }

    @PostMapping("/admin/domains/{domain_id}/tickets/{ticket_id}/merge")
    @RequirePermission(value = PermissionCodes.TICKET_MERGE, domainIdParam = "domain_id")
    public TicketService.TicketActionResult mergeTicket(
            @PathVariable("domain_id") long domainId,
            @PathVariable("ticket_id") long ticketId,
            @Valid @RequestBody TicketService.MergeTicketCommand request) {
        return ticketService.mergeTicket(requireCurrent(), domainId, ticketId, request);
    }

    private UserContext requireCurrent() {
        return UserContextHolder.current()
                .orElseThrow(() -> new IllegalStateException("user context is not available"));
    }
}
