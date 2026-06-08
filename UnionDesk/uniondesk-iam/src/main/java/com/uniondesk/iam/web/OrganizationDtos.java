package com.uniondesk.iam.web;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

public final class OrganizationDtos {

    private OrganizationDtos() {
    }

    public record OrganizationUnitView(
            long id,
            String code,
            String name,
            Long parentId,
            String parentName,
            Long leaderUserId,
            String leaderName,
            int orderNo,
            int status,
            String remark,
            LocalDateTime createdAt) {
    }

    public record CreateOrganizationRequest(
            @NotBlank(message = "请输入组织编码")
            String code,
            @NotBlank(message = "请输入组织名称")
            String name,
            Long parentId,
            Long leaderUserId,
            Integer orderNo,
            Integer status,
            String remark) {
    }

    public record UpdateOrganizationRequest(
            String code,
            String name,
            Long parentId,
            Long leaderUserId,
            Integer orderNo,
            Integer status,
            String remark) {
    }
}
