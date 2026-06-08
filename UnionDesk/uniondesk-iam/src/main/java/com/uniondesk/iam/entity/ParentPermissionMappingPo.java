package com.uniondesk.iam.entity;

public class ParentPermissionMappingPo {

    private Long parentId;
    private String permissionCode;

    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }
    public String getPermissionCode() { return permissionCode; }
    public void setPermissionCode(String permissionCode) { this.permissionCode = permissionCode; }
}
