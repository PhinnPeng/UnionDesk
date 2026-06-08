package com.uniondesk.iam.entity;

/**
 * 域成员展示投影（domain_member 查询结果）
 */
public class DomainMemberPresentationPo {

    private String domainNickname;
    private String domainAvatarUrl;
    private String domainContactPhone;
    private String domainContactEmail;

    public String getDomainNickname() { return domainNickname; }
    public void setDomainNickname(String domainNickname) { this.domainNickname = domainNickname; }
    public String getDomainAvatarUrl() { return domainAvatarUrl; }
    public void setDomainAvatarUrl(String domainAvatarUrl) { this.domainAvatarUrl = domainAvatarUrl; }
    public String getDomainContactPhone() { return domainContactPhone; }
    public void setDomainContactPhone(String domainContactPhone) { this.domainContactPhone = domainContactPhone; }
    public String getDomainContactEmail() { return domainContactEmail; }
    public void setDomainContactEmail(String domainContactEmail) { this.domainContactEmail = domainContactEmail; }
}
