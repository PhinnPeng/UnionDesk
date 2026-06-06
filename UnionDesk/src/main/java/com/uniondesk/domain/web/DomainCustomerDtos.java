package com.uniondesk.domain.web;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

public final class DomainCustomerDtos {

    private DomainCustomerDtos() {
    }

    public record DomainCustomerView(
            long id,
            long business_domain_id,
            long customer_account_id,
            long subject_id,
            String username,
            String nickname,
            String phone,
            String email,
            String status,
            String source,
            LocalDateTime activated_at,
            LocalDateTime disabled_at,
            LocalDateTime created_at,
            LocalDateTime updated_at) {
    }

    public record CreateDomainCustomerRequest(
            @JsonAlias({"customer_account_id"})
            @NotNull Long customerAccountId,
            String source) {
    }

    public record CreateDomainCustomerManualRequest(
            @JsonAlias({"display_name", "displayName", "nickname"})
            @NotBlank String nickname,
            @JsonAlias({"login_name", "loginName", "username"})
            @NotBlank String username,
            @NotBlank String phone,
            String email) {
    }

    public record CreateDomainCustomersFromStaffRequest(
            @JsonAlias({"staff_account_ids"})
            @NotEmpty List<Long> staffAccountIds) {
    }

    public record BatchCreateDomainCustomersResult(
            int added,
            int skipped,
            List<DomainCustomerView> items) {
    }

    public record UpdateDomainCustomerStatusRequest(
            @NotBlank String status) {
    }
}
