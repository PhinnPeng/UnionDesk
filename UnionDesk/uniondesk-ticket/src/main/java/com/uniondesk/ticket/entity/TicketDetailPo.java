package com.uniondesk.ticket.entity;

import java.time.LocalDateTime;

public class TicketDetailPo {

    private long id;
    private String ticketNo;
    private long businessDomainId;
    private String businessDomainCode;
    private String businessDomainName;
    private long ticketTypeId;
    private String ticketTypeName;
    private long customerId;
    private Long assignedTo;
    private String title;
    private String description;
    private String status;
    private String priority;
    private String source;
    private String result;
    private int version;
    private String customFieldsJson;
    private LocalDateTime slaFirstResponseDeadline;
    private LocalDateTime slaResolutionDeadline;
    private LocalDateTime slaFirstRespondedAt;
    private LocalDateTime slaResolvedAt;
    private String slaStatus;
    private int slaPausedDuration;
    private LocalDateTime slaPauseStartedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastReplyAt;
    private long replyCount;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTicketNo() {
        return ticketNo;
    }

    public void setTicketNo(String ticketNo) {
        this.ticketNo = ticketNo;
    }

    public long getBusinessDomainId() {
        return businessDomainId;
    }

    public void setBusinessDomainId(long businessDomainId) {
        this.businessDomainId = businessDomainId;
    }

    public String getBusinessDomainCode() {
        return businessDomainCode;
    }

    public void setBusinessDomainCode(String businessDomainCode) {
        this.businessDomainCode = businessDomainCode;
    }

    public String getBusinessDomainName() {
        return businessDomainName;
    }

    public void setBusinessDomainName(String businessDomainName) {
        this.businessDomainName = businessDomainName;
    }

    public long getTicketTypeId() {
        return ticketTypeId;
    }

    public void setTicketTypeId(long ticketTypeId) {
        this.ticketTypeId = ticketTypeId;
    }

    public String getTicketTypeName() {
        return ticketTypeName;
    }

    public void setTicketTypeName(String ticketTypeName) {
        this.ticketTypeName = ticketTypeName;
    }

    public long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(long customerId) {
        this.customerId = customerId;
    }

    public Long getAssignedTo() {
        return assignedTo;
    }

    public void setAssignedTo(Long assignedTo) {
        this.assignedTo = assignedTo;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public String getCustomFieldsJson() {
        return customFieldsJson;
    }

    public void setCustomFieldsJson(String customFieldsJson) {
        this.customFieldsJson = customFieldsJson;
    }

    public LocalDateTime getSlaFirstResponseDeadline() {
        return slaFirstResponseDeadline;
    }

    public void setSlaFirstResponseDeadline(LocalDateTime slaFirstResponseDeadline) {
        this.slaFirstResponseDeadline = slaFirstResponseDeadline;
    }

    public LocalDateTime getSlaResolutionDeadline() {
        return slaResolutionDeadline;
    }

    public void setSlaResolutionDeadline(LocalDateTime slaResolutionDeadline) {
        this.slaResolutionDeadline = slaResolutionDeadline;
    }

    public LocalDateTime getSlaFirstRespondedAt() {
        return slaFirstRespondedAt;
    }

    public void setSlaFirstRespondedAt(LocalDateTime slaFirstRespondedAt) {
        this.slaFirstRespondedAt = slaFirstRespondedAt;
    }

    public LocalDateTime getSlaResolvedAt() {
        return slaResolvedAt;
    }

    public void setSlaResolvedAt(LocalDateTime slaResolvedAt) {
        this.slaResolvedAt = slaResolvedAt;
    }

    public String getSlaStatus() {
        return slaStatus;
    }

    public void setSlaStatus(String slaStatus) {
        this.slaStatus = slaStatus;
    }

    public int getSlaPausedDuration() {
        return slaPausedDuration;
    }

    public void setSlaPausedDuration(int slaPausedDuration) {
        this.slaPausedDuration = slaPausedDuration;
    }

    public LocalDateTime getSlaPauseStartedAt() {
        return slaPauseStartedAt;
    }

    public void setSlaPauseStartedAt(LocalDateTime slaPauseStartedAt) {
        this.slaPauseStartedAt = slaPauseStartedAt;
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

    public LocalDateTime getLastReplyAt() {
        return lastReplyAt;
    }

    public void setLastReplyAt(LocalDateTime lastReplyAt) {
        this.lastReplyAt = lastReplyAt;
    }

    public long getReplyCount() {
        return replyCount;
    }

    public void setReplyCount(long replyCount) {
        this.replyCount = replyCount;
    }
}
