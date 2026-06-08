package com.uniondesk.attachment.entity;

public class AttachmentPolicyPo {

    private String scopeType;
    private Long scopeId;
    private String allowedTypesJson;
    private Integer maxSingleSizeMb;
    private Integer maxTotalSizeMb;

    public String getScopeType() {
        return scopeType;
    }

    public void setScopeType(String scopeType) {
        this.scopeType = scopeType;
    }

    public Long getScopeId() {
        return scopeId;
    }

    public void setScopeId(Long scopeId) {
        this.scopeId = scopeId;
    }

    public String getAllowedTypesJson() {
        return allowedTypesJson;
    }

    public void setAllowedTypesJson(String allowedTypesJson) {
        this.allowedTypesJson = allowedTypesJson;
    }

    public Integer getMaxSingleSizeMb() {
        return maxSingleSizeMb;
    }

    public void setMaxSingleSizeMb(Integer maxSingleSizeMb) {
        this.maxSingleSizeMb = maxSingleSizeMb;
    }

    public Integer getMaxTotalSizeMb() {
        return maxTotalSizeMb;
    }

    public void setMaxTotalSizeMb(Integer maxTotalSizeMb) {
        this.maxTotalSizeMb = maxTotalSizeMb;
    }
}
