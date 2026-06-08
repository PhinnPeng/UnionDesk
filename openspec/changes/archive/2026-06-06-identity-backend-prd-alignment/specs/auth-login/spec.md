## ADDED Requirements

### Requirement: 分端登录账号单表

员工端登录 MUST 查询 staff_account 单表；客户端登录 MUST 查询 customer_account 单表。

#### Scenario: 员工登录

- **WHEN** staff 端使用 username 与密码登录
- **THEN** 系统在 staff_account 按 username 校验 password_hash

#### Scenario: 禁止 user_account 回退

- **WHEN** user_account 存在但无 staff_account
- **THEN** 员工端登录失败

### Requirement: 会话 user_id 语义

auth_login_session.user_id MUST 为 staff_account.id 或 customer_account.id。

#### Scenario: 员工会话

- **WHEN** staff 登录成功
- **THEN** session.account_type=staff 且 user_id 等于 staff_account.id
