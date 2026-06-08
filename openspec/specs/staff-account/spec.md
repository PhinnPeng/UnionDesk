## ADDED Requirements

### Requirement: 员工账号凭证与平台展示同表

staff_account MUST 同时承载登录凭证（username、password_hash、status、auth_version）与平台级展示（real_name、nickname、avatar_url、phone、email）；MUST NOT 使用独立的 staff_platform_profile 表。

#### Scenario: 创建员工

- **WHEN** 平台创建员工
- **THEN** 同一事务写入 identity_subject（经 Service 解析）与一行 staff_account（含 username、real_name、nickname 等）

#### Scenario: 平台员工列表

- **WHEN** 调用员工列表 API
- **THEN** JSON 与 DB 对齐：返回 username、real_name、nickname；MUST NOT 使用 login_name 字段名

#### Scenario: 员工字段命名对齐

- **WHEN** 平台创建或更新员工
- **THEN** 登录账号读写 staff_account.username；真实姓名读写 real_name；昵称读写 nickname

#### Scenario: Legacy 姓名回填

- **WHEN** 执行 340001 迁移且存在 user_account.nickname
- **THEN** 回填至 staff_account.real_name，而非 nickname

### Requirement: 平台员工库不依赖 user_account

新创建员工 MUST NOT 写入 user_account；IAM 读写 MUST 以 staff_account 为权威数据源。

#### Scenario: 新建员工无 legacy 行

- **WHEN** 创建新员工成功
- **THEN** 存在 staff_account 行且不存在对应 user_account 新行
