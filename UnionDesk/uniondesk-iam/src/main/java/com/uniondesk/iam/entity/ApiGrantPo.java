package com.uniondesk.iam.entity;

/**
 * API 授权投影（role + iam_role_resource + iam_resource 联合查询）
 */
public class ApiGrantPo {

    private String httpMethod;
    private String pathPattern;

    public String getHttpMethod() { return httpMethod; }
    public void setHttpMethod(String httpMethod) { this.httpMethod = httpMethod; }
    public String getPathPattern() { return pathPattern; }
    public void setPathPattern(String pathPattern) { this.pathPattern = pathPattern; }
}
