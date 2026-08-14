package com.uniondesk.domain.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.Table;

/**
 * identity_subject.id 为业务主键（人员/客户主体 id，由调用方显式指定），非自增。
 */
@Table("identity_subject")
public class IdentitySubjectPo {

    @Id
    private Long id;
    private String subjectType;
    private String phone;
    private String status;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSubjectType() {
        return subjectType;
    }

    public void setSubjectType(String subjectType) {
        this.subjectType = subjectType;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
