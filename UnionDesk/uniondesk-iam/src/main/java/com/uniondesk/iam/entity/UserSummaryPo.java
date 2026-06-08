package com.uniondesk.iam.entity;

/**
 * 用户摘要投影（user_account 查询结果）
 */
public class UserSummaryPo {

    private long id;
    private String username;
    private String mobile;
    private String email;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getMobile() { return mobile; }
    public void setMobile(String mobile) { this.mobile = mobile; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
