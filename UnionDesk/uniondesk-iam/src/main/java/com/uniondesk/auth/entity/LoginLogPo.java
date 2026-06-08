package com.uniondesk.auth.entity;

import java.time.LocalDateTime;

public class LoginLogPo {

    private Long id;
    private String sid;
    private Long subjectId;
    private String loginName;
    private String loginIdentifierMasked;
    private String loginIdentifierType;
    private String eventType;
    private String portalType;
    private String clientCode;
    private Long businessDomainId;
    private String result;
    private String failReason;
    private String ip;
    private String userAgent;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSid() { return sid; }
    public void setSid(String sid) { this.sid = sid; }
    public Long getSubjectId() { return subjectId; }
    public void setSubjectId(Long subjectId) { this.subjectId = subjectId; }
    public String getLoginName() { return loginName; }
    public void setLoginName(String loginName) { this.loginName = loginName; }
    public String getLoginIdentifierMasked() { return loginIdentifierMasked; }
    public void setLoginIdentifierMasked(String loginIdentifierMasked) { this.loginIdentifierMasked = loginIdentifierMasked; }
    public String getLoginIdentifierType() { return loginIdentifierType; }
    public void setLoginIdentifierType(String loginIdentifierType) { this.loginIdentifierType = loginIdentifierType; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public String getPortalType() { return portalType; }
    public void setPortalType(String portalType) { this.portalType = portalType; }
    public String getClientCode() { return clientCode; }
    public void setClientCode(String clientCode) { this.clientCode = clientCode; }
    public Long getBusinessDomainId() { return businessDomainId; }
    public void setBusinessDomainId(Long businessDomainId) { this.businessDomainId = businessDomainId; }
    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }
    public String getFailReason() { return failReason; }
    public void setFailReason(String failReason) { this.failReason = failReason; }
    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }
    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
