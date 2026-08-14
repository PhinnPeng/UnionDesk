package com.uniondesk.iam.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;

@Table("iam_permission")
public class IamPermissionPo {

    @Id(keyType = KeyType.Auto)
    private Long id;
    private String code;
    private String name;
    private String httpMethod;
    private String pathPattern;
    private Integer status;
    private String permissionScope;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getHttpMethod() { return httpMethod; }
    public void setHttpMethod(String httpMethod) { this.httpMethod = httpMethod; }
    public String getPathPattern() { return pathPattern; }
    public void setPathPattern(String pathPattern) { this.pathPattern = pathPattern; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public String getPermissionScope() { return permissionScope; }
    public void setPermissionScope(String permissionScope) { this.permissionScope = permissionScope; }
}
