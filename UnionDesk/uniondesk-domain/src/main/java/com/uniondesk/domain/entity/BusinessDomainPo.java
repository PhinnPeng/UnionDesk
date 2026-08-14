package com.uniondesk.domain.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import java.time.LocalDateTime;

@Table("business_domain")
public class BusinessDomainPo {

    @Id(keyType = KeyType.Auto)
    private Long id;
    private String code;
    private String name;
    private String description;
    private String logo;
    private String visibilityPolicy;
    private String visibilityPolicyCodes;
    private String registrationEnabled;
    private String invitationEnabled;

    @Column(onInsertValue = "1")
    private Integer status;

    @Column(onInsertValue = "CURRENT_TIMESTAMP(3)")
    private LocalDateTime createdAt;

    @Column(onInsertValue = "CURRENT_TIMESTAMP(3)", onUpdateValue = "CURRENT_TIMESTAMP(3)")
    private LocalDateTime updatedAt;

    private LocalDateTime deletedAt;
    private Long createdBy;
    private Long updatedBy;

    @Column(ignore = true)
    private String creatorName;

    @Column(ignore = true)
    private String updaterName;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLogo() {
        return logo;
    }

    public void setLogo(String logo) {
        this.logo = logo;
    }

    public String getVisibilityPolicy() {
        return visibilityPolicy;
    }

    public void setVisibilityPolicy(String visibilityPolicy) {
        this.visibilityPolicy = visibilityPolicy;
    }

    public String getVisibilityPolicyCodes() {
        return visibilityPolicyCodes;
    }

    public void setVisibilityPolicyCodes(String visibilityPolicyCodes) {
        this.visibilityPolicyCodes = visibilityPolicyCodes;
    }

    public String getRegistrationEnabled() {
        return registrationEnabled;
    }

    public void setRegistrationEnabled(String registrationEnabled) {
        this.registrationEnabled = registrationEnabled;
    }

    public String getInvitationEnabled() {
        return invitationEnabled;
    }

    public void setInvitationEnabled(String invitationEnabled) {
        this.invitationEnabled = invitationEnabled;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public Long getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(Long updatedBy) {
        this.updatedBy = updatedBy;
    }

    public String getCreatorName() {
        return creatorName;
    }

    public void setCreatorName(String creatorName) {
        this.creatorName = creatorName;
    }

    public String getUpdaterName() {
        return updaterName;
    }

    public void setUpdaterName(String updaterName) {
        this.updaterName = updaterName;
    }
}
