package com.uniondesk.ticket.entity;

import java.time.LocalDateTime;

public class TicketHistoryPo {

    private long id;
    private long ticketId;
    private long businessDomainId;
    private String action;
    private String fromValue;
    private String toValue;
    private Long operatorSubjectId;
    private String operatorActorType;
    private String payload;
    private LocalDateTime createdAt;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getTicketId() {
        return ticketId;
    }

    public void setTicketId(long ticketId) {
        this.ticketId = ticketId;
    }

    public long getBusinessDomainId() {
        return businessDomainId;
    }

    public void setBusinessDomainId(long businessDomainId) {
        this.businessDomainId = businessDomainId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getFromValue() {
        return fromValue;
    }

    public void setFromValue(String fromValue) {
        this.fromValue = fromValue;
    }

    public String getToValue() {
        return toValue;
    }

    public void setToValue(String toValue) {
        this.toValue = toValue;
    }

    public Long getOperatorSubjectId() {
        return operatorSubjectId;
    }

    public void setOperatorSubjectId(Long operatorSubjectId) {
        this.operatorSubjectId = operatorSubjectId;
    }

    public String getOperatorActorType() {
        return operatorActorType;
    }

    public void setOperatorActorType(String operatorActorType) {
        this.operatorActorType = operatorActorType;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
