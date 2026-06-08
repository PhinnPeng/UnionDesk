package com.uniondesk.iam.entity;

public class IdentitySubjectPo {

    private Long id;
    private String subjectType;
    private String phone;
    private String status;
    private Long mergedIntoId;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSubjectType() { return subjectType; }
    public void setSubjectType(String subjectType) { this.subjectType = subjectType; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getMergedIntoId() { return mergedIntoId; }
    public void setMergedIntoId(Long mergedIntoId) { this.mergedIntoId = mergedIntoId; }
}
