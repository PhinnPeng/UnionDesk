package com.uniondesk.domain.web;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public final class DomainCustomerDtos {

    private DomainCustomerDtos() {
    }

    public record DomainCustomerView(
            long id,
            long businessDomainId,
            long customerAccountId,
            long subjectId,
            String loginName,
            String displayName,
            String phone,
            String email,
            String status,
            String source,
            LocalDateTime activatedAt,
            LocalDateTime disabledAt,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
    }

    public record CreateDomainCustomerRequest(
            @JsonAlias({"customer_account_id"})
            @NotNull Long customerAccountId,
            String source) {
    }

    public record UpdateDomainCustomerStatusRequest(
            @NotBlank String status) {
    }
}
