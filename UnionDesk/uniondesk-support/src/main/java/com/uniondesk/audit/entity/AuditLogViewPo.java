package com.uniondesk.audit.entity;

import java.time.LocalDateTime;

public class AuditLogViewPo {

    private Long id;
    private Long businessDomainId;
    private Long operatorSubjectId;
    private String operatorName;
    private String operatorActorType;
    private String target;
    private String action;
    private String detail;
    private String result;
    private LocalDateTime occurredAt;
    private String requestId;
    private String ip;

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

    public String getOperatorName() {
        return operatorName;
    }

    public void setOperatorName(String operatorName) {
        this.operatorName = operatorName;
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

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(LocalDateTime occurredAt) {
        this.occurredAt = occurredAt;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }
}
