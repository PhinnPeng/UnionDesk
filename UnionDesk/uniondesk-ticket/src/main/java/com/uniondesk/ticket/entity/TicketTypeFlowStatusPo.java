package com.uniondesk.ticket.entity;

import java.time.LocalDateTime;

public class TicketTypeFlowStatusPo {

    private long id;
    private long domainId;
    private long ticketTypeId;
    private String stateCode;
    private String name;
    private String stateType;
    private boolean allowCustomerWithdraw;
    private boolean resolved;
    private int sortOrder;
    private Long sourceStatusId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getDomainId() {
        return domainId;
    }

    public void setDomainId(long domainId) {
        this.domainId = domainId;
    }

    public long getTicketTypeId() {
        return ticketTypeId;
    }

    public void setTicketTypeId(long ticketTypeId) {
        this.ticketTypeId = ticketTypeId;
    }

    public String getStateCode() {
        return stateCode;
    }

    public void setStateCode(String stateCode) {
        this.stateCode = stateCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStateType() {
        return stateType;
    }

    public void setStateType(String stateType) {
        this.stateType = stateType;
    }

    public boolean isAllowCustomerWithdraw() {
        return allowCustomerWithdraw;
    }

    public void setAllowCustomerWithdraw(boolean allowCustomerWithdraw) {
        this.allowCustomerWithdraw = allowCustomerWithdraw;
    }

    public boolean isResolved() {
        return resolved;
    }

    public void setResolved(boolean resolved) {
        this.resolved = resolved;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Long getSourceStatusId() {
        return sourceStatusId;
    }

    public void setSourceStatusId(Long sourceStatusId) {
        this.sourceStatusId = sourceStatusId;
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
