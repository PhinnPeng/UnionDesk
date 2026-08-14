package com.uniondesk.domain.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import java.time.LocalDateTime;

/**
 * 角色模板下发域记录（模板 → 业务域 → domain_role 实例）。
 */
@Table("role_template_domain")
public class RoleTemplateDomainPo {

    @Id(keyType = KeyType.Auto)
    private Long id;
    private Long templateId;
    private Long businessDomainId;
    private Long instanceDomainRoleId;
    private String syncMode;

    @Column(onInsertValue = "CURRENT_TIMESTAMP(3)")
    private LocalDateTime appliedAt;

    @Column(onInsertValue = "CURRENT_TIMESTAMP(3)", onUpdateValue = "CURRENT_TIMESTAMP(3)")
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
