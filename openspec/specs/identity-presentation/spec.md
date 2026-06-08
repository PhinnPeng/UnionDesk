## ADDED Requirements

### Requirement: 域内员工展示统一解析

系统 MUST 通过 `IdentityPresentationService` 解析员工在业务域内的对外展示；域内 API MUST NOT 在 Controller 或零散 SQL 中重复实现 fallback。

#### Scenario: 域内 nickname 回退

- **WHEN** 查询某域某员工的对外 nickname 且 domain_member.domain_nickname 为空
- **THEN** 依次回退 staff_account.nickname、staff_account.real_name、staff_account.username

#### Scenario: 真实姓名不做域级覆盖

- **WHEN** 查询员工 real_name
- **THEN** 仅返回 staff_account.real_name，不读取 domain_member 展示列

#### Scenario: 客户 nickname 列对齐

- **WHEN** 查询客户展示信息
- **THEN** 使用 customer_account.nickname（340001 自 display_name 重命名）与 avatar_url

### Requirement: 展示更新不触发凭证失效

更新 staff_account 或 domain_member 上的展示字段 MUST NOT 递增 auth_version。

#### Scenario: 修改域内 nickname

- **WHEN** 仅更新 domain_member.domain_nickname
- **THEN** staff_account.auth_version 保持不变
