package com.uniondesk.ticket.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public final class TicketTeamTemplateDtos {

    private TicketTeamTemplateDtos() {
    }

    public record TeamTemplateItemView(
            String id,
            String ticket_type_id,
            String ticket_type_code,
            String ticket_type_name,
            int sort_order,
            boolean include_form_schema,
            boolean include_workflow,
            boolean include_description_template) {
    }

    public record TeamTemplateView(
            String id,
            String code,
            String name,
            String description,
            String icon,
            String status,
            boolean is_system,
            int sort_order,
            int version,
            List<TeamTemplateItemView> items,
            String created_at,
            String updated_at) {
    }

    public record TeamTemplateListView(
            long total,
            List<TeamTemplateView> items) {
    }

    public record TeamTemplateOptionView(
            String id,
            String code,
            String name,
            String description,
            String icon,
            int version,
            int item_count) {
    }

    public record TeamTemplateItemRequest(
            @NotNull Long ticket_type_id,
            Integer sort_order,
            Boolean include_form_schema,
            Boolean include_workflow,
            Boolean include_description_template) {
    }

    public record CreateTeamTemplateRequest(
            String code,
            @NotBlank String name,
            String description,
            String icon,
            String status,
            @Valid List<TeamTemplateItemRequest> items) {
    }

    public record UpdateTeamTemplateRequest(
            String name,
            String description,
            String icon,
            String status,
            @Valid List<TeamTemplateItemRequest> items) {
    }

    public record ReorderTeamTemplatesRequest(
            @NotEmpty List<Long> ordered_ids) {
    }
}
