package com.uniondesk.iam.web;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
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
            String username,
            @JsonProperty("real_name") String realName,
            String nickname,
            String phone,
            @JsonProperty("mobile") String mobile,
            String email,
            int status,
            String employmentStatus,
            String accountType,
            List<String> roleCodes,
            List<Long> businessDomainIds,
            List<Long> organizationIds,
            List<String> platformRoles,
            String offboardedAt,
            Long offboardedBy,
            String offboardReason) {
    }

    public record CreateStaffRequest(
            @JsonAlias({"login_name", "loginName"})
            @NotBlank String username,
            @JsonAlias({"realName"})
            String real_name,
            String nickname,
            @JsonAlias({"mobile"})
            @NotBlank String phone,
            String email,
            @NotBlank String password,
            String accountType,
            @NotEmpty List<String> roleCodes,
            @NotNull List<Long> businessDomainIds,
            List<Long> organizationIds) {
    }

    public record UpdateStaffRequest(
            @JsonAlias({"login_name", "loginName"})
            String username,
            @JsonAlias({"realName"})
            String real_name,
            String nickname,
            @JsonAlias({"mobile"})
            String phone,
            String email,
            String password,
            String accountType,
            List<String> roleCodes,
            List<Long> businessDomainIds,
            List<Long> organizationIds,
            Integer status) {
    }

    public record UpdateStaffStatusRequest(
            @NotBlank String status) {
    }

    public record OffboardStaffRequest(
            String reason) {
    }
}
