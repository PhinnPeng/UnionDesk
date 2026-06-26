package com.uniondesk.ticket.entity;

import java.time.LocalDateTime;

public class TicketTypePo {

    private long id;
    private long businessDomainId;
    private String code;
    private String name;
    private String description;
    private String icon;
    private String statusFlowConfig;
    private String formSchema;
    private String formSchemaDraft;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getBusinessDomainId() {
        return businessDomainId;
    }

    public void setBusinessDomainId(long businessDomainId) {
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

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getStatusFlowConfig() {
        return statusFlowConfig;
    }

    public void setStatusFlowConfig(String statusFlowConfig) {
        this.statusFlowConfig = statusFlowConfig;
    }

    public String getFormSchema() {
        return formSchema;
    }

    public void setFormSchema(String formSchema) {
        this.formSchema = formSchema;
    }

    public String getFormSchemaDraft() {
        return formSchemaDraft;
    }

    public void setFormSchemaDraft(String formSchemaDraft) {
        this.formSchemaDraft = formSchemaDraft;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
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
}
