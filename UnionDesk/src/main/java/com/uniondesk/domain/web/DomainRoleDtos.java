package com.uniondesk.domain.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public final class DomainRoleDtos {

    private DomainRoleDtos() {
    }

    public record DomainRoleView(
            long id,
            long business_domain_id,
            String code,
            String name,
            boolean preset) {
    }

    public record DomainRolePermissionView(
            long role_id,
            String code,
            String name,
            List<PermissionItemView> permission_items) {
    }

    public record PermissionItemView(
            long id,
            String code,
            String name,
            String module,
            String type) {
    }

    public record CreateDomainRoleRequest(
            @NotBlank String code,
            @NotBlank String name) {
    }

    public record UpdateDomainRoleRequest(
            String code,
            String name) {
    }

    public record UpdateDomainRolePermissionRequest(
            @NotNull List<Long> permission_item_ids) {
    }
}
