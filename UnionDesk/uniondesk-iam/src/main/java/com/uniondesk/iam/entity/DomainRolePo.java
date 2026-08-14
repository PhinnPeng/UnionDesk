package com.uniondesk.iam.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;

@Table("domain_role")
public class DomainRolePo {

    @Id(keyType = KeyType.Auto)
    private Long id;
    private String code;
    private Long businessDomainId;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public Long getBusinessDomainId() { return businessDomainId; }
    public void setBusinessDomainId(Long businessDomainId) { this.businessDomainId = businessDomainId; }
}
