package com.uniondesk.auth.entity;

import com.mybatisflex.annotation.Table;

@Table("auth_client")
public class AuthClientPo {

    private String clientCode;
    private String allowedAccountType;
    private int status;

    public String getClientCode() { return clientCode; }
    public void setClientCode(String clientCode) { this.clientCode = clientCode; }
    public String getAllowedAccountType() { return allowedAccountType; }
    public void setAllowedAccountType(String allowedAccountType) { this.allowedAccountType = allowedAccountType; }
    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }
}
