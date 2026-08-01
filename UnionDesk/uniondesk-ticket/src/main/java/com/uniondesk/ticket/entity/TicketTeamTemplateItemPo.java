package com.uniondesk.ticket.entity;

import java.time.LocalDateTime;

public class TicketTeamTemplateItemPo {

    private long id;
    private long teamTemplateId;
    private long ticketTypeId;
    private int sortOrder;
    private boolean includeFormSchema;
    private boolean includeWorkflow;
    private boolean includeDescriptionTemplate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** 联表展示用，非持久化必填 */
    private String ticketTypeCode;
    private String ticketTypeName;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getTeamTemplateId() {
        return teamTemplateId;
    }

    public void setTeamTemplateId(long teamTemplateId) {
        this.teamTemplateId = teamTemplateId;
    }

    public long getTicketTypeId() {
        return ticketTypeId;
    }

    public void setTicketTypeId(long ticketTypeId) {
        this.ticketTypeId = ticketTypeId;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public boolean isIncludeFormSchema() {
        return includeFormSchema;
    }

    public void setIncludeFormSchema(boolean includeFormSchema) {
        this.includeFormSchema = includeFormSchema;
    }

    public boolean isIncludeWorkflow() {
        return includeWorkflow;
    }

    public void setIncludeWorkflow(boolean includeWorkflow) {
        this.includeWorkflow = includeWorkflow;
    }

    public boolean isIncludeDescriptionTemplate() {
        return includeDescriptionTemplate;
    }

    public void setIncludeDescriptionTemplate(boolean includeDescriptionTemplate) {
        this.includeDescriptionTemplate = includeDescriptionTemplate;
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

    public String getTicketTypeCode() {
        return ticketTypeCode;
    }

    public void setTicketTypeCode(String ticketTypeCode) {
        this.ticketTypeCode = ticketTypeCode;
    }

    public String getTicketTypeName() {
        return ticketTypeName;
    }

    public void setTicketTypeName(String ticketTypeName) {
        this.ticketTypeName = ticketTypeName;
    }
}
