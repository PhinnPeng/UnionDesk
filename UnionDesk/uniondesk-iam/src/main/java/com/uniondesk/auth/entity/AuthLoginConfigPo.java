package com.uniondesk.auth.entity;

import com.mybatisflex.annotation.Table;

@Table("auth_login_config")
public class AuthLoginConfigPo {

    private String configKey;
    private String configValue;

    public String getConfigKey() { return configKey; }
    public void setConfigKey(String configKey) { this.configKey = configKey; }
    public String getConfigValue() { return configValue; }
    public void setConfigValue(String configValue) { this.configValue = configValue; }
}
