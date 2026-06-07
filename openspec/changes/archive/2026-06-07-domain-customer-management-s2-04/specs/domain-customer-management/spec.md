## ADDED Requirements

### Requirement: 控制台客户权限命名 platform.domain.control.customer.*

业务域详情「客户管理」Tab 的权限 MUST 使用 `platform.domain.control.customer.*` 命名空间，与 `platform.domain.control.general.*` 同级；MUST 自 `platform.domain.customer.*` 迁移。

#### Scenario: 权限码结构

- **WHEN** 查看 Flyway 与 PermissionCodes
- **THEN** 存在 `platform.domain.control.customer.read`、`create`、`update-status`

#### Scenario: 迁移映射

- **WHEN** 自 `platform.domain.customer.read/create/update` 升级
- **THEN** 分别映射至 control.customer 三码且角色绑定不丢失

#### Scenario: 菜单 catalog

- **WHEN** 迁移完成
- **THEN** 按钮挂于 `PLATFORM-DOMAIN-CONTROL-CUSTOMER`（或等价 hidden catalog）

### Requirement: 只读查看域内客户

#### Scenario: GET 单条

- **WHEN** GET `.../customers/{id}` 且持有 `platform.domain.control.customer.read`
- **THEN** 返回完整客户资料

#### Scenario: 只读 Modal

- **WHEN** 点击「查看」
- **THEN** 只读 Modal；无编辑与保存

### Requirement: 禁用与启用 update-status

#### Scenario: status API

- **WHEN** PUT/PATCH status 且持有 `platform.domain.control.customer.update-status`
- **THEN** 更新 active/disabled

#### Scenario: 禁用二次确认

- **WHEN** 点击禁用（行内或批量）
- **THEN** 二次确认后调用 API

#### Scenario: 无权限

- **WHEN** 无对应 control.customer 码
- **THEN** 按钮不可见且 API 403

### Requirement: 列表 UI 与 Shared normalize

#### Scenario: 菜单式布局与字段归一化

- **WHEN** 进入客户 Tab 或加载列表
- **THEN** Card 筛选 + Table 工具栏 + link 操作列；display_name/login_name 正确
