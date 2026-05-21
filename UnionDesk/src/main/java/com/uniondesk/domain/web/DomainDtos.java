package com.uniondesk.domain.web;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.List;

public final class DomainDtos {

    private DomainDtos() {
    }

    public record DomainView(
            long id,
            String code,
            String name,
            String logo,
            List<String> visibility_policy_codes,
            String registration_policy,
            int status,
            LocalDateTime created_at,
            LocalDateTime updated_at,
            LocalDateTime deleted_at,
            Long created_by,
            Long updated_by,
            String creator_name,
            String updater_name) {
    }

    public record DomainBriefView(
            long id,
            String code,
            String name,
            String logo) {
    }

    public record CreateDomainRequest(
            @NotBlank String code,
            @NotBlank String name,
            String logo,
            List<String> visibility_policy_codes,
            String registration_policy) {
    }

    public record UpdateDomainRequest(
            String code,
            String name,
            String logo,
            List<String> visibility_policy_codes,
            String registration_policy,
            Integer status) {
    }

    public record DomainCreateResponse(long id, String code) {
    }
}
