package com.uniondesk.domain.entity;

import java.time.LocalDateTime;

/**
 * 角色模板权限项（模板与 permission_item 的关联）。
 */
public class RoleTemplatePermissionPo {

    private Long id;
    private Long templateId;
    private Long permissionItemId;
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTemplateId() {
        return templateId;
    }

    public void setTemplateId(Long templateId) {
        this.templateId = templateId;
    }

    public Long getPermissionItemId() {
        return permissionItemId;
    }

    public void setPermissionItemId(Long permissionItemId) {
        this.permissionItemId = permissionItemId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
