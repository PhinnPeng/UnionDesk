package com.uniondesk.domain.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import java.time.LocalDateTime;

/**
 * 角色模板（集团统一角色）。
 */
@Table("role_template")
public class RoleTemplatePo {

    public static final String SYNC_IMMEDIATE = "immediate";
    public static final String SYNC_MANUAL = "manual";
    public static final String SYNC_NONE = "none";

    public static final String LOCK_FIELD_PERMISSIONS = "permissions";

    @Id(keyType = KeyType.Auto)
    private Long id;
    private String code;
    private String name;
    private String description;
    private String syncStrategy;
    private String lockedFields;
    private Integer preset;
    private Integer version;
    private Long createdBy;

    @Column(onInsertValue = "CURRENT_TIMESTAMP(3)")
    private LocalDateTime createdAt;

    @Column(onInsertValue = "CURRENT_TIMESTAMP(3)", onUpdateValue = "CURRENT_TIMESTAMP(3)")
    private LocalDateTime updatedAt;

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

    public String getSyncStrategy() {
        return syncStrategy;
    }

    public void setSyncStrategy(String syncStrategy) {
        this.syncStrategy = syncStrategy;
    }

    public String getLockedFields() {
        return lockedFields;
    }

    public void setLockedFields(String lockedFields) {
        this.lockedFields = lockedFields;
    }

    public Integer getPreset() {
        return preset;
    }

    public void setPreset(Integer preset) {
        this.preset = preset;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
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
}
