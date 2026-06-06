## ADDED Requirements

### Requirement: 平台控制台可管理域内员工成员

平台管理员在业务域详情「员工管理」Tab MUST 能够：从平台员工添加成员并分配角色、通过域内流程新建员工并分配角色、修改成员角色、软删移除成员、禁用/启用成员（行内与批量）；MUST 遵守最后 `domain_admin` 与 `super_admin` 保护规则。

#### Scenario: 添加未入域员工

- **WHEN** 管理员选择活跃 `staff_account` 且该员工在本域无未软删 `domain_member`
- **THEN** 创建 `domain_member`（status=active）并写入所选 `domain_member_role`

#### Scenario: 域内新建员工并入域

- **WHEN** 管理员在控制台提交新建员工表单（含手机号、登录账号、初始密码、角色）
- **THEN** 在同一事务内创建或关联 `identity_subject`、`staff_account` 与当前域 `domain_member`，并写入角色；不要求额外 `platform.user.create` 权限

#### Scenario: 重复添加拒绝

- **WHEN** 管理员尝试添加已在域内的员工
- **THEN** 返回中文错误，提示成员已存在

#### Scenario: 移除最后管理员拒绝

- **WHEN** 管理员移除或降权域内唯一 `domain_admin` 或 `super_admin`
- **THEN** 操作失败并返回中文保护提示

#### Scenario: 禁用最后管理员拒绝

- **WHEN** 管理员禁用域内唯一 `domain_admin` 或 `super_admin`（行内或批量）
- **THEN** 操作失败并返回中文保护提示

### Requirement: 成员列表筛选

系统 MUST 支持按关键字（昵称、真实姓名、手机号、邮箱、登录账号）、成员状态、加入时间区间（`domain_member.created_at`）分页查询成员列表。

#### Scenario: 关键字与时间筛选

- **WHEN** 管理员传入 keyword、status、created_from、created_to
- **THEN** 返回符合条件的未软删成员分页结果

### Requirement: 员工候选、启停与新建 API

系统 MUST 提供：

- `GET .../members/staff-candidates`
- `PUT .../members/{memberId}/status`（`active`/`disabled`）
- `POST .../members/with-staff`（域内新建员工并入域）

shared 包 MUST 封装成员相关写操作与扩展列表 API。

#### Scenario: 候选列表排除已入域员工

- **WHEN** 查询 staff-candidates
- **THEN** 仅返回 status=active 且不在当前域未软删成员表中的 `staff_account`

#### Scenario: 启停成员

- **WHEN** 管理员 PUT status 为 disabled 或 active
- **THEN** 更新 `domain_member.status` 与 `disabled_at`（启用时清空 disabled_at）

#### Scenario: 批量启停与批量删除

- **WHEN** 管理员在前端对多条记录执行批量启停或批量删除
- **THEN** 客户端逐条调用单成员 API；任一保护规则或校验失败时中断并展示错误

### Requirement: 成员管理权限与菜单

Flyway MUST 幂等补齐 `domain.member.read|create|update_roles|delete|update_status` 及控制台 `PLATFORM-DOMAIN-MEMBERS` 按钮；前端按钮显隐与接口权限一致。

#### Scenario: 无创建权限隐藏添加

- **WHEN** 用户无 `domain.member.create`
- **THEN** 「添加员工」按钮不可见且 POST 接口 403
