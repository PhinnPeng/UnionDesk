package com.uniondesk.ticket.web;

import jakarta.validation.constraints.NotBlank;

public final class TicketConfigDtos {

    private TicketConfigDtos() {
    }

    public record TicketTypeView(
            String id,
            String domain_id,
            String code,
            String name,
            Object dynamic_fields,
            String status) {
    }

    public record CreateTicketTypeRequest(
            @NotBlank String code,
            @NotBlank String name,
            Object dynamic_fields) {
    }

    public record UpdateTicketTypeRequest(
            String name,
            Object dynamic_fields,
            String status) {
    }

    public record TicketTemplateView(
            String id,
            String domain_id,
            String name,
            String type,
            String type_id,
            Object fields_snapshot,
            String content,
            Integer sort_order) {
    }

    public record CreateTicketTemplateRequest(
            @NotBlank String name,
            @NotBlank String type,
            String type_id,
            Object fields_snapshot,
            String content,
            Integer sort_order) {
    }

    public record UpdateTicketTemplateRequest(
            String name,
            String type,
            String type_id,
            Object fields_snapshot,
            String content,
            Integer sort_order) {
    }

    public record QuickReplyView(
            String id,
            String domain_id,
            String title,
            String content,
            String scope,
            Integer sort_order,
            String created_at) {
    }

    public record CreateQuickReplyRequest(
            @NotBlank String title,
            @NotBlank String content,
            @NotBlank String scope,
            Integer sort_order) {
    }

    public record UpdateQuickReplyRequest(
            String title,
            String content,
            String scope,
            Integer sort_order) {
    }

    public record PriorityLevelView(
            String id,
            String domain_id,
            String name,
            String display_label,
            String color,
            Integer sort_order,
            Boolean is_default) {
    }

    public record CreatePriorityLevelRequest(
            @NotBlank String name,
            @NotBlank String display_label,
            String color,
            Integer sort_order,
            Boolean is_default) {
    }

    public record UpdatePriorityLevelRequest(
            String name,
            String display_label,
            String color,
            Integer sort_order,
            Boolean is_default) {
    }
}
