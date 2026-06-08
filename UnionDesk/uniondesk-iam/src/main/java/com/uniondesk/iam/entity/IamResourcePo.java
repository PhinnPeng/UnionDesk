package com.uniondesk.iam.entity;

public class IamResourcePo {

    private Long id;
    private String resourceType;
    private String resourceCode;
    private String resourceName;
    private String clientScope;
    private String httpMethod;
    private String pathPattern;
    private Long parentId;
    private Integer orderNo;
    private String icon;
    private String component;
    private Integer hidden;
    private Integer status;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getResourceType() { return resourceType; }
    public void setResourceType(String resourceType) { this.resourceType = resourceType; }
    public String getResourceCode() { return resourceCode; }
    public void setResourceCode(String resourceCode) { this.resourceCode = resourceCode; }
    public String getResourceName() { return resourceName; }
    public void setResourceName(String resourceName) { this.resourceName = resourceName; }
    public String getClientScope() { return clientScope; }
    public void setClientScope(String clientScope) { this.clientScope = clientScope; }
    public String getHttpMethod() { return httpMethod; }
    public void setHttpMethod(String httpMethod) { this.httpMethod = httpMethod; }
    public String getPathPattern() { return pathPattern; }
    public void setPathPattern(String pathPattern) { this.pathPattern = pathPattern; }
    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }
    public Integer getOrderNo() { return orderNo; }
    public void setOrderNo(Integer orderNo) { this.orderNo = orderNo; }
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
    public String getComponent() { return component; }
    public void setComponent(String component) { this.component = component; }
    public Integer getHidden() { return hidden; }
    public void setHidden(Integer hidden) { this.hidden = hidden; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
}
