## ADDED Requirements

### Requirement: 身份主体不可作为登录入口

`identity_subject` MUST 仅表示自然人锚点；系统 MUST NOT 在 identity_subject 上存储 password_hash 或使用 subject_id 直接完成登录校验。

#### Scenario: 员工登录校验

- **WHEN** 员工端提交用户名与密码
- **THEN** 系统在 staff_account 凭证表校验密码，并通过 staff_account.subject_id 关联 identity_subject

### Requirement: 手机号唯一身份主体

系统 MUST 以 `identity_subject.phone` 作为自然人主体的唯一归并键；同一有效手机号 MUST NOT 对应多个未合并的 `person` 类型主体。

#### Scenario: 按手机号解析已存在主体

- **WHEN** 业务调用 `IdentitySubjectService.resolveSubjectIdByPhone` 且手机号已存在
- **THEN** 返回已有主体的 `id`，不插入新行

#### Scenario: 禁止业务层裸插入主体

- **WHEN** 审查 domain、auth、iam 模块写路径
- **THEN** 除 `IdentitySubjectService` 外 MUST NOT 直接 `INSERT INTO identity_subject`

### Requirement: 合并主体解析（只读，本 change 不提供合并 API）

当 `identity_subject.merged_into_id` 非空时，系统 MUST 通过 `resolveEffectiveSubjectId` 解析至最终有效主体 id（只读，防环）。

#### Scenario: 读取已合并主体

- **WHEN** 传入的 subject_id 对应行 merged_into_id 已设置
- **THEN** `resolveEffectiveSubjectId` 返回链末端有效主体 id

#### Scenario: 禁止向已合并主体挂载新账号

- **WHEN** StaffAccountService 或 CustomerAccountService 创建账号且 subject 的 merged_into_id 非空
- **THEN** 拒绝创建并返回「身份主体不可用」类错误

### Requirement: 身份域引用完整性由业务规则保证

身份核心表（identity_subject、staff_account、customer_account、domain_member、domain_member_role、domain_customer、staff_account_platform_role）之间的引用 MUST 为逻辑引用（列 + 索引），MUST NOT 使用数据库 FOREIGN KEY 约束。写路径 MUST 在持久化前校验被引用实体存在且状态有效；校验失败 MUST 返回中文业务错误。

#### Scenario: 创建员工账号时校验主体

- **WHEN** StaffAccountService 创建员工且 subject_id 不存在或主体非 active
- **THEN** 拒绝创建并返回「身份主体不可用」类错误

#### Scenario: 添加域成员时校验账号与业务域

- **WHEN** DomainMemberService 添加成员且 staff_account_id 或 business_domain_id 无效
- **THEN** 拒绝创建并返回对应中文错误，不依赖 DB FK 拦截
