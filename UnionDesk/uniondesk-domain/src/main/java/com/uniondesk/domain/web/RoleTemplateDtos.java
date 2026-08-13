package com.uniondesk.domain.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

public final class RoleTemplateDtos {

    private RoleTemplateDtos() {
    }

    public record RoleTemplateView(
            long id,
            String code,
            String name,
            String description,
            String sync_strategy,
            List<String> locked_fields,
            boolean preset,
            int version,
            Long created_by,
            LocalDateTime created_at,
            LocalDateTime updated_at,
            int applied_domain_count) {
    }

    public record AppliedDomainView(
            long domain_id,
            long instance_domain_role_id,
            String sync_mode,
            Integer instance_version,
            LocalDateTime applied_at) {
    }

    public record RoleTemplateDetailView(
            RoleTemplateView template,
            List<AppliedDomainView> applied_domains,
            List<PermissionItemView> permission_items) {
    }

    public record PermissionItemView(
            long id,
            String code,
            String name,
            String module,
            String type) {
    }

    public record CreateRoleTemplateRequest(
            @NotBlank String code,
            @NotBlank String name,
            String description,
            List<String> locked_fields,
            String sync_strategy,
            @NotNull List<Long> permission_item_ids) {
    }

    public record UpdateRoleTemplateRequest(
            String name,
            String description,
            List<String> locked_fields,
            String sync_strategy,
            List<Long> permission_item_ids) {
    }

    public record ApplyRequest(
            @NotNull List<Long> domain_ids,
            String sync_mode) {
    }

    public record SyncRequest(
            List<Long> domain_ids) {
    }

    public record UnapplyRequest(
            @NotNull List<Long> domain_ids) {
    }

    public record BindMembersRequest(
            @NotNull List<Long> staff_ids,
            @NotNull List<Long> domain_ids) {
    }

    public record DomainResult(
            long domain_id,
            String reason) {
    }

    public record BatchResult(
            List<Long> success,
            List<DomainResult> skipped,
            List<DomainResult> failed) {
    }

    public record RoleTemplateListView(long total, List<RoleTemplateView> items) {}

    public record PermissionItemListView(long total, List<PermissionItemView> items) {}
}
