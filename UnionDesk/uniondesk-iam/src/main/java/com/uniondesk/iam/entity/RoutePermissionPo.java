package com.uniondesk.iam.entity;

/**
 * 路由权限投影（iam_permission 查询结果）
 */
public class RoutePermissionPo {

    private String code;
    private String pathPattern;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getPathPattern() { return pathPattern; }
    public void setPathPattern(String pathPattern) { this.pathPattern = pathPattern; }
}
