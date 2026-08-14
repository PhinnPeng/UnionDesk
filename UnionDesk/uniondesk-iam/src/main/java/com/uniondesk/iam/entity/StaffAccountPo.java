package com.uniondesk.iam.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import java.time.LocalDateTime;

@Table("staff_account")
public class StaffAccountPo {

    @Id(keyType = KeyType.Auto)
    private Long id;
    private Long subjectId;
    private String username;
    private String realName;
    private String nickname;
    private String avatarUrl;
    private String phone;
    private String email;
    private String status;
    private String employmentStatus;
    private LocalDateTime offboardedAt;
    private Long offboardedBy;
    private String offboardReason;
    private String source;
    private Integer authVersion;
    private String passwordHash;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getSubjectId() { return subjectId; }
    public void setSubjectId(Long subjectId) { this.subjectId = subjectId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getRealName() { return realName; }
    public void setRealName(String realName) { this.realName = realName; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getEmploymentStatus() { return employmentStatus; }
    public void setEmploymentStatus(String employmentStatus) { this.employmentStatus = employmentStatus; }
    public LocalDateTime getOffboardedAt() { return offboardedAt; }
    public void setOffboardedAt(LocalDateTime offboardedAt) { this.offboardedAt = offboardedAt; }
    public Long getOffboardedBy() { return offboardedBy; }
    public void setOffboardedBy(Long offboardedBy) { this.offboardedBy = offboardedBy; }
    public String getOffboardReason() { return offboardReason; }
    public void setOffboardReason(String offboardReason) { this.offboardReason = offboardReason; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public Integer getAuthVersion() { return authVersion; }
    public void setAuthVersion(Integer authVersion) { this.authVersion = authVersion; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
}
