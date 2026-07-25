package com.uniondesk.ticket.web;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public final class TicketStatusDtos {

    private TicketStatusDtos() {
    }

    public record TicketStatusView(
            String id,
            String scope,
            String code,
            String name,
            String description,
            String category,
            String state_type,
            String status,
            int sort_order,
            boolean is_system,
            String created_at,
            String updated_at) {
    }

    public record TicketStatusListView(
            long total,
            List<TicketStatusView> items) {
    }

    public record CreateTicketStatusRequest(
            @NotBlank String name,
            String description,
            @NotBlank String category,
            String code) {
    }

    public record UpdateTicketStatusRequest(
            String name,
            String description,
            String category) {
    }
}
