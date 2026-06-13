## ADDED Requirements

### Requirement: 业务域详情操作日志页

平台管理员 MUST 在业务域详情侧栏独立入口「操作日志」查看当前域的审计记录；数据范围固定为路径 `domainId`，不可切换业务域；**不使用**页内 Tabs 嵌套。

#### Scenario: 域内操作日志分页列表

- **WHEN** GET `/api/v1/admin/domains/{domainId}/audit-logs` 且持有 `platform.domain.control.audit_log.read`
- **THEN** 返回 `PageResult`，支持 `page`/`page_size` 分页；支持 `operator`、`action`、`keyword`、`startTime`、`endTime` 筛选（与平台操作日志字段子集一致，无业务域列）

#### Scenario: 操作日志页无权限

- **WHEN** 用户无 `platform.domain.control.audit_log.read`
- **THEN** 侧栏不显示「操作日志」；直链 `?tab=audit_logs` 时展示 `Empty`「无权限查看操作日志」

### Requirement: 业务域详情登录日志页

平台管理员 MUST 在业务域详情侧栏独立入口「登录日志」查看当前域的登录记录；**不使用**页内 Tabs 嵌套。

#### Scenario: 域内登录日志分页列表

- **WHEN** GET `/api/v1/admin/domains/{domainId}/login-logs` 且持有 `platform.domain.control.login_log.read`
- **THEN** 返回 `PageResult`；支持 `page`/`page_size`；支持时间范围、`result`、`portal_type`、账号关键词筛选（与平台登录日志对齐，无业务域列）

#### Scenario: 登录日志页无权限

- **WHEN** 用户无 `platform.domain.control.login_log.read`
- **THEN** 侧栏不显示「登录日志」；直链 `?tab=login_logs` 时展示 `Empty`「无权限查看登录日志」

### Requirement: 平台日志审计权限

平台统一页 `/platform/audit-logs` MUST 使用 `platform.log.*` 权限前缀，与域详情 `platform.domain.control.*` 分离。

#### Scenario: 平台操作日志权限

- **WHEN** GET `/api/v1/admin/audit-logs`
- **THEN** 要求 `platform.log.audit.read`（自 `platform.audit_log.read` 迁移）

#### Scenario: 平台登录日志权限

- **WHEN** GET `/api/v1/admin/login-logs`
- **THEN** 要求 `platform.log.login.read`（自 `platform.login_log.read` 迁移）

### Requirement: 域详情菜单与权限迁移

#### Scenario: Flyway 登记域详情 catalog

- **WHEN** 执行本 Story Flyway
- **THEN** `PLATFORM-DOMAIN-DETAIL` 下存在 hidden catalog「操作日志」「登录日志」及对应 `platform.domain.control.audit_log.read` / `platform.domain.control.login_log.read` 查询按钮；已有域详情入口的角色自动获得菜单与权限绑定

#### Scenario: 域级旧码迁移

- **WHEN** Flyway 执行完成
- **THEN** `domain.audit_log.read` / `domain.login_log.read` 映射至 `platform.domain.control.audit_log.read` / `platform.domain.control.login_log.read`，角色绑定不丢失
