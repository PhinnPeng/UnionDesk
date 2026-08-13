package com.uniondesk.domain.entity;

import java.time.LocalDateTime;

/**
 * 角色模板下发域记录（模板 → 业务域 → domain_role 实例）。
 */
public class RoleTemplateDomainPo {

    private Long id;
    private Long templateId;
    private Long businessDomainId;
    private Long instanceDomainRoleId;
    private String syncMode;
    private LocalDateTime appliedAt;
    private LocalDateTime updatedAt;

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

    public Long getBusinessDomainId() {
        return businessDomainId;
    }

    public void setBusinessDomainId(Long businessDomainId) {
        this.businessDomainId = businessDomainId;
    }

    public Long getInstanceDomainRoleId() {
        return instanceDomainRoleId;
    }

    public void setInstanceDomainRoleId(Long instanceDomainRoleId) {
        this.instanceDomainRoleId = instanceDomainRoleId;
    }

    public String getSyncMode() {
        return syncMode;
    }

    public void setSyncMode(String syncMode) {
        this.syncMode = syncMode;
    }

    public LocalDateTime getAppliedAt() {
        return appliedAt;
    }

    public void setAppliedAt(LocalDateTime appliedAt) {
        this.appliedAt = appliedAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
