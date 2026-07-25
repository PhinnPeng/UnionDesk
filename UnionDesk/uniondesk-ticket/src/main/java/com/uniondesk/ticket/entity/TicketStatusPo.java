package com.uniondesk.ticket.entity;

import java.time.LocalDateTime;

public class TicketStatusPo {

    public static final String SCOPE_PLATFORM = "platform";
    public static final String SCOPE_DOMAIN = "domain";
    public static final String STATUS_ACTIVE = "active";
    public static final String STATUS_DISABLED = "disabled";
    public static final String CATEGORY_NOT_STARTED = "not_started";
    public static final String CATEGORY_IN_PROGRESS = "in_progress";
    public static final String CATEGORY_COMPLETED = "completed";
    public static final String STATE_TYPE_IN_PROGRESS = "in_progress";
    public static final String STATE_TYPE_PAUSED = "paused";
    public static final String STATE_TYPE_TERMINAL = "terminal";

    private long id;
    private String scope;
    private Long businessDomainId;
    private String code;
    private String name;
    private String description;
    private String category;
    private String stateType;
    private String configJson;
    private String status;
    private int sortOrder;
    private boolean system;
    private Long sourceStatusId;
    private Long createdBy;
    private Long updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public Long getBusinessDomainId() {
        return businessDomainId;
    }

    public void setBusinessDomainId(Long businessDomainId) {
        this.businessDomainId = businessDomainId;
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

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getStateType() {
        return stateType;
    }

    public void setStateType(String stateType) {
        this.stateType = stateType;
    }

    public String getConfigJson() {
        return configJson;
    }

    public void setConfigJson(String configJson) {
        this.configJson = configJson;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public boolean isSystem() {
        return system;
    }

    public void setSystem(boolean system) {
        this.system = system;
    }

    public Long getSourceStatusId() {
        return sourceStatusId;
    }

    public void setSourceStatusId(Long sourceStatusId) {
        this.sourceStatusId = sourceStatusId;
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
