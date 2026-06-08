package com.uniondesk.domain.entity;

import java.time.LocalDateTime;

public class AuditLogPo {

    private Long id;
    private Long businessDomainId;
    private Long operatorSubjectId;
    private String operatorActorType;
    private String target;
    private String action;
    private String detail;
    private String result;
    private String requestId;
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getBusinessDomainId() {
        return businessDomainId;
    }

    public void setBusinessDomainId(Long businessDomainId) {
        this.businessDomainId = businessDomainId;
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

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
