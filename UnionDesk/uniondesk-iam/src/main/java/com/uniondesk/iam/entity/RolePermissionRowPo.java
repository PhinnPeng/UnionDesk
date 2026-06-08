package com.uniondesk.iam.entity;

/**
 * 角色权限行投影（iam_role_permission + role + iam_permission 联合查询）
 */
public class RolePermissionRowPo {

    private String code;
    private String name;
    private String httpMethod;
    private String pathPattern;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getHttpMethod() { return httpMethod; }
    public void setHttpMethod(String httpMethod) { this.httpMethod = httpMethod; }
    public String getPathPattern() { return pathPattern; }
    public void setPathPattern(String pathPattern) { this.pathPattern = pathPattern; }
}
