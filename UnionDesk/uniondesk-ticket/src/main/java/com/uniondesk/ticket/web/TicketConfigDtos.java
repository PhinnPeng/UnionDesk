package com.uniondesk.ticket.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

public final class TicketConfigDtos {

    private TicketConfigDtos() {
    }

    public record TicketTypeView(
            String id,
            String domain_id,
            String code,
            String name,
            String description,
            String icon,
            Object status_flow,
            Object form_schema,
            Object form_schema_draft,
            Integer form_schema_current_version_no,
            Boolean form_schema_has_unpublished,
            String status,
            java.util.List<TransitionRuleView> transition_rules) {
    }

    public record CreateTicketTypeRequest(
            @NotBlank String code,
            @NotBlank String name,
            String description,
            String icon,
            Object status_flow,
            Object form_schema) {
    }

    public record UpdateTicketTypeRequest(
            String name,
            String description,
            String icon,
            Object status_flow,
            String status,
            java.util.List<SaveTransitionRuleRequest> transition_rules) {
    }

    public record SaveFormSchemaDraftRequest(Object form_schema) {
    }

    public record PublishFormSchemaRequest(Object form_schema) {
    }

    public record FormSchemaVersionsView(
            Integer current_version_no,
            java.util.List<FormSchemaVersionSummaryView> items) {
    }

    public record FormSchemaVersionSummaryView(
            int version_no,
            boolean is_current,
            String published_at,
            String published_by) {
    }

    public record FormSchemaVersionDetailView(
            int version_no,
            Object form_schema,
            String published_at,
            String published_by) {
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

    public record PlatformTicketTypeView(
            String id,
            String scope,
            String code,
            String name,
            String description,
            String icon,
            String category,
            String status,
            int sort_order,
            boolean is_system,
            long linked_domain_count,
            String created_at,
            String updated_at) {
    }

    public record PlatformTicketTypeDetailView(
            String id,
            String scope,
            String code,
            String name,
            String description,
            String icon,
            String category,
            String status,
            int sort_order,
            boolean is_system,
            long linked_domain_count,
            Object status_flow,
            Object form_schema,
            Object form_schema_draft,
            Integer form_schema_current_version_no,
            Boolean form_schema_has_unpublished,
            String created_at,
            String updated_at) {
    }

    public record PlatformTicketTypeListView(
            long total,
            java.util.List<PlatformTicketTypeView> items) {
    }

    public record CreatePlatformTicketTypeRequest(
            String code,
            @NotBlank String name,
            String description,
            @NotBlank String icon,
            String category,
            String template_key) {
    }

    public record UpdatePlatformTicketTypeRequest(
            String name,
            String description,
            String icon,
            Object status_flow,
            String status) {
    }

    public record ReorderPlatformTicketTypesRequest(
            @NotEmpty java.util.List<@Valid PlatformTicketTypeSortOrderItem> orders) {
    }

    public record PlatformTicketTypeSortOrderItem(
            long id,
            int sort_order) {
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

    // --- ListView records for list endpoints ---

    public record TicketTypeListView(
            long total,
            java.util.List<TicketTypeView> items) {
    }

    public record TicketTemplateListView(
            long total,
            java.util.List<TicketTemplateView> items) {
    }

    public record QuickReplyListView(
            long total,
            java.util.List<QuickReplyView> items) {
    }

    public record PriorityLevelListView(
            long total,
            java.util.List<PriorityLevelView> items) {
    }

    // --- Workflow Transition Rule records ---

    public record TransitionRuleView(
            String id,
            String from_state_code,
            String to_state_code,
            String step_name,
            String permission_mode,
            java.util.List<Long> member_ids,
            java.util.List<Long> role_ids,
            java.util.List<String> required_slot_ids,
            java.util.List<AttributeUpdateItemView> attribute_updates) {
    }

    public record AttributeUpdateItemView(
            String slot_id,
            Object value,
            String value_type) {
    }

    public record SaveTransitionRuleRequest(
            String from_state_code,
            String to_state_code,
            String step_name,
            String permission_mode,
            java.util.List<Long> member_ids,
            java.util.List<Long> role_ids,
            java.util.List<String> required_slot_ids,
            java.util.List<AttributeUpdateItemRequest> attribute_updates) {
    }

    public record AttributeUpdateItemRequest(
            String slot_id,
            Object value,
            String value_type) {
    }

    public record WorkflowConfigView(
            Object status_flow,
            java.util.List<TransitionRuleView> transition_rules) {
    }
}
