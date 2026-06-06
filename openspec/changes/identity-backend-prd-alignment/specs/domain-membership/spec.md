## ADDED Requirements

### Requirement: 域成员关系与域内展示同表

domain_member MUST 承载员工与业务域的成员关系（status、source 等）及可选域内展示列：domain_nickname、domain_avatar_url、domain_contact_phone、domain_contact_email；MUST NOT 使用独立的 domain_member_profile 表。

#### Scenario: 域内展示列可空

- **WHEN** 新添加域成员且未配置域内展示
- **THEN** domain_member 展示列为 NULL，对外 nickname 经 IdentityPresentationService 回退 staff_account

#### Scenario: 设置域内 nickname

- **WHEN** 管理员更新成员域内 domain_nickname
- **THEN** 仅更新 domain_member 对应列；API 字段名为 domain_nickname

### Requirement: 域成员绑定与角色单轨

新建域成员 MUST 写入 domain_member 与 domain_member_role；MUST NOT 仅写入 user_domain_role。

#### Scenario: 域创建者授权

- **WHEN** 创建新业务域
- **THEN** 仅为创建者写入 domain_member 与 super_admin 的 domain_member_role

### Requirement: 域角色解析

员工域内权限 MUST 从 domain_member_role 加载。

#### Scenario: 域内 agent

- **WHEN** 员工在某域 domain_member 含 agent 角色
- **THEN** 登录上下文包含该域及 agent 角色码

#### Scenario: 软删成员同步清理角色

- **WHEN** 域成员被软删（deleted_at 设置）
- **THEN** 应用层 MUST 同步移除该成员的 domain_member_role 行（无 FK CASCADE）
