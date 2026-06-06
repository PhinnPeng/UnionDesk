package com.uniondesk.domain.web;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

public final class DomainMemberDtos {

    private DomainMemberDtos() {
    }

    public record DomainMemberView(
            long id,
            long staff_account_id,
            long business_domain_id,
            String username,
            String real_name,
            String nickname,
            String phone,
            String email,
            String status,
            String source,
            LocalDateTime activated_at,
            LocalDateTime disabled_at,
            LocalDateTime deleted_at,
            List<DomainRoleDtos.DomainRoleView> roles) {
    }

    public record CreateDomainMemberRequest(
            @NotNull Long staff_account_id,
            List<Long> role_ids) {
    }

    public record UpdateDomainMemberRolesRequest(
            @NotNull List<Long> role_ids) {
    }
}
