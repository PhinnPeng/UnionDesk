package com.uniondesk.iam.entity;

public class DomainMemberPo {

    private Long id;
    private Long staffAccountId;
    private Long businessDomainId;
    private String domainNickname;
    private String domainAvatarUrl;
    private String domainContactPhone;
    private String domainContactEmail;
    private String status;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getStaffAccountId() { return staffAccountId; }
    public void setStaffAccountId(Long staffAccountId) { this.staffAccountId = staffAccountId; }
    public Long getBusinessDomainId() { return businessDomainId; }
    public void setBusinessDomainId(Long businessDomainId) { this.businessDomainId = businessDomainId; }
    public String getDomainNickname() { return domainNickname; }
    public void setDomainNickname(String domainNickname) { this.domainNickname = domainNickname; }
    public String getDomainAvatarUrl() { return domainAvatarUrl; }
    public void setDomainAvatarUrl(String domainAvatarUrl) { this.domainAvatarUrl = domainAvatarUrl; }
    public String getDomainContactPhone() { return domainContactPhone; }
    public void setDomainContactPhone(String domainContactPhone) { this.domainContactPhone = domainContactPhone; }
    public String getDomainContactEmail() { return domainContactEmail; }
    public void setDomainContactEmail(String domainContactEmail) { this.domainContactEmail = domainContactEmail; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
