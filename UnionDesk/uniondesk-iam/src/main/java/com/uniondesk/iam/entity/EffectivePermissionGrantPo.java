package com.uniondesk.iam.entity;

/**
 * 有效权限授权投影（iam_role_binding + role + iam_role_permission + iam_permission 联合查询）
 */
public class EffectivePermissionGrantPo {

    private String roleLevel;
    private String bindingScope;
    private Long businessDomainId;
    private String permissionScope;

    public String getRoleLevel() { return roleLevel; }
    public void setRoleLevel(String roleLevel) { this.roleLevel = roleLevel; }
    public String getBindingScope() { return bindingScope; }
    public void setBindingScope(String bindingScope) { this.bindingScope = bindingScope; }
    public Long getBusinessDomainId() { return businessDomainId; }
    public void setBusinessDomainId(Long businessDomainId) { this.businessDomainId = businessDomainId; }
    public String getPermissionScope() { return permissionScope; }
    public void setPermissionScope(String permissionScope) { this.permissionScope = permissionScope; }
}
