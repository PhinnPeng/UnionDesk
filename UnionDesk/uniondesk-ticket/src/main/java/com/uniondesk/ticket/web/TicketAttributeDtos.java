package com.uniondesk.ticket.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public final class TicketAttributeDtos {

    private TicketAttributeDtos() {
    }

    public record TicketAttributeView(
            String id,
            String scope,
            String business_domain_id,
            String name,
            String description,
            String field_type,
            Object type_config,
            String status,
            int sort_order,
            boolean is_system,
            String system_key,
            String source_attribute_id,
            String created_at,
            String updated_at) {
    }

    public record TicketAttributeListView(
            long total,
            List<TicketAttributeView> items) {
    }

    public record CreateTicketAttributeRequest(
            @NotBlank String name,
            String description,
            @NotBlank String field_type,
            Object type_config) {
    }

    public record UpdateTicketAttributeRequest(
            String name,
            String description,
            String field_type,
            Object type_config,
            String status) {
    }

    public record SortOrderItem(
            long id,
            int sort_order) {
    }

    public record ReorderTicketAttributesRequest(
            @NotEmpty List<@Valid SortOrderItem> orders) {
    }

    public record AttributeSlotConfig(
            Boolean required,
            String placeholder,
            Boolean visible_to_customer,
            String default_value,
            String display_name) {
    }

    public record AttributeSlotView(
            String id,
            String ticket_type_id,
            String attribute_id,
            TicketAttributeView attribute,
            int sort_order,
            AttributeSlotConfig slot_config,
            String status,
            boolean is_system,
            String system_field_key) {
    }

    public record AttributeSlotListView(
            long total,
            List<AttributeSlotView> items) {
    }

    public record InsertAttributeSlotRequest(
            @NotNull Long attribute_id,
            AttributeSlotConfig slot_config) {
    }

    public record UpdateAttributeSlotRequest(
            @NotNull AttributeSlotConfig slot_config) {
    }

    public record ReorderAttributeSlotsRequest(
            @NotEmpty List<@Valid SortOrderItem> orders) {
    }

    public record PromoteTicketAttributeRequest(
            @NotNull Long attribute_id) {
    }
}
