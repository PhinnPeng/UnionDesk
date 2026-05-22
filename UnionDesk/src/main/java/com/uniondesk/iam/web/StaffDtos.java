package com.uniondesk.iam.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public final class StaffDtos {

    private StaffDtos() {
    }

    public record UpdatePlatformRolesRequest(
            @NotNull @NotEmpty List<String> roleCodes) {
    }

    public record StaffPlatformRolesResponse(
            long staffAccountId,
            List<String> platformRoles) {
    }

    public record StaffAccountView(
            long id,
            String loginName,
            String nickname,
            String phone,
            String email,
            int status,
            String employmentStatus,
            String accountType,
            List<String> roleCodes,
            List<Long> businessDomainIds,
            List<String> platformRoles) {
    }

    public record CreateStaffRequest(
            @NotBlank String loginName,
            String nickname,
            @NotBlank String phone,
            String email,
            @NotBlank String password,
            @NotBlank String accountType,
            @NotEmpty List<String> roleCodes,
            @NotNull List<Long> businessDomainIds) {
    }

    public record UpdateStaffRequest(
            String loginName,
            String nickname,
            String phone,
            String email,
            String password,
            String accountType,
            List<String> roleCodes,
            List<Long> businessDomainIds,
            Integer status) {
    }

    public record UpdateStaffStatusRequest(
            @NotBlank String status) {
    }
}
