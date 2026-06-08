package com.uniondesk.domain.entity;

public class MemberRolePo {

    private Long domainMemberId;
    private Long id;
    private Long businessDomainId;
    private String code;
    private String name;
    private Integer preset;

    public Long getDomainMemberId() {
        return domainMemberId;
    }

    public void setDomainMemberId(Long domainMemberId) {
        this.domainMemberId = domainMemberId;
    }

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

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getPreset() {
        return preset;
    }

    public void setPreset(Integer preset) {
        this.preset = preset;
    }
}
