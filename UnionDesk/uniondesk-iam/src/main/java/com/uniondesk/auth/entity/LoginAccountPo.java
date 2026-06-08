package com.uniondesk.auth.entity;

/**
 * 登录账号查询结果（staff_account / customer_account 联合投影）
 */
public class LoginAccountPo {

    private long id;
    private String username;
    private String mobile;
    private String email;
    private String passwordHash;
    private int status;
    private String accountType;
    private String employmentStatus;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getMobile() { return mobile; }
    public void setMobile(String mobile) { this.mobile = mobile; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }
    public String getAccountType() { return accountType; }
    public void setAccountType(String accountType) { this.accountType = accountType; }
    public String getEmploymentStatus() { return employmentStatus; }
    public void setEmploymentStatus(String employmentStatus) { this.employmentStatus = employmentStatus; }
}
