package com.uniondesk.iam.entity;

import java.time.LocalDateTime;

public class UserAccountPo {

    private Long id;
    private String username;
    private String nickname;
    private String mobile;
    private String email;
    private String remark;
    private String accountType;
    private Integer status;
    private String employmentStatus;
    private LocalDateTime offboardedAt;
    private Long offboardedBy;
    private String offboardReason;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getMobile() { return mobile; }
    public void setMobile(String mobile) { this.mobile = mobile; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public String getAccountType() { return accountType; }
    public void setAccountType(String accountType) { this.accountType = accountType; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public String getEmploymentStatus() { return employmentStatus; }
    public void setEmploymentStatus(String employmentStatus) { this.employmentStatus = employmentStatus; }
    public LocalDateTime getOffboardedAt() { return offboardedAt; }
    public void setOffboardedAt(LocalDateTime offboardedAt) { this.offboardedAt = offboardedAt; }
    public Long getOffboardedBy() { return offboardedBy; }
    public void setOffboardedBy(Long offboardedBy) { this.offboardedBy = offboardedBy; }
    public String getOffboardReason() { return offboardReason; }
    public void setOffboardReason(String offboardReason) { this.offboardReason = offboardReason; }
}
