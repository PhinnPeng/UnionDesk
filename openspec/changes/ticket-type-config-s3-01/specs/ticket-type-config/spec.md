## ADDED Requirements

### Requirement: 平台域详情工单类型配置

平台管理员 MUST 能在业务域详情「工单管理」Tab 查看、创建、编辑、删除本域工单类型，并对接 `TicketConfigController`。

#### Scenario: 类型列表

- **WHEN** 用户持有 `platform.domain.control.ticket_type.read` 并打开工单 Tab
- **THEN** 展示本域工单类型列表（编码、名称、状态、字段数、状态数等）

#### Scenario: 打开设计器

- **WHEN** 用户持有 update 权限并点击「设计」
- **THEN** 打开工单类型设计器（基本信息、动态字段、状态流程三 Tab）

#### Scenario: 删除

- **WHEN** 用户持有 delete 权限并确认删除
- **THEN** 类型被删除；若存在引用则拒绝 + 中文提示

### Requirement: Formily 动态字段设计器

管理员 MUST 能通过低代码表单设计器配置工单类型的 `form_schema`（在系统默认字段之上追加扩展字段），并支持预览。

#### Scenario: 保存表单 schema

- **WHEN** 用户在「动态字段」Tab 拖拽组件并保存
- **THEN** `form_schema` 持久化；再次打开设计器可还原画布

#### Scenario: 非法 schema

- **WHEN** 提交的 schema 结构不合法
- **THEN** 保存失败 + 中文错误提示

### Requirement: 系统默认字段 title / description

每个工单类型的 `form_schema` MUST 包含系统字段 `title`（标题）与 `description`（详细描述），且不可删除或改为非必填。

#### Scenario: 新建类型自动注入

- **WHEN** 管理员创建工单类型且未提交 form_schema
- **THEN** 系统写入含 title、description 的默认 schema

#### Scenario: 保存缺少系统字段

- **WHEN** 提交的 form_schema 不含 title 或 description
- **THEN** 保存失败 + 中文错误提示

#### Scenario: 提单字段映射

- **WHEN** 客户通过 US-S3-03 提交工单
- **THEN** form 中 title/description 写入 `ticket.title` / `ticket.description`；其余字段写入 `custom_fields`

### Requirement: 状态流 DAG 可视化

管理员 MUST 能通过 DAG 编辑器配置 `status_flow`（节点、连线、状态属性），并支持导入 foundation-rules §6.4 默认模板。

#### Scenario: DAG 编辑保存

- **WHEN** 用户添加状态节点、连线并保存
- **THEN** `status_flow` 持久化且含节点坐标

#### Scenario: 导入默认模板

- **WHEN** 用户点击「导入默认状态流」
- **THEN** 画布加载 §6.4 预置状态与流转

### Requirement: 状态流 TR-01 与图完整性校验

保存 `status_flow` 时，系统 MUST 满足 **TR-01**（至少一个终态）及流转图引用完整性。

#### Scenario: 无终态保存失败

- **WHEN** 提交的状态流不含任何终态
- **THEN** 保存失败并返回中文错误提示

#### Scenario: 非法流转

- **WHEN** transition 引用不存在的状态 code
- **THEN** 保存失败 + 中文错误提示

### Requirement: 预置反馈与建议类型

系统 MUST 支持预置「反馈」「建议」工单类型，且管理员可启用/停用。

#### Scenario: 启停预置类型

- **WHEN** 管理员将预置类型 status 设为 disabled
- **THEN** 客户提单类型列表不展示该类型（US-S3-03 联调验收）

### Requirement: 工单模板配置

管理员 MUST 能在同一 Tab 管理工单模板（列表/新建/编辑/删除），对接 `ticket-templates` API。

#### Scenario: 模板 CRUD

- **WHEN** 用户持有相应权限
- **THEN** 模板与工单类型关联可维护

### Requirement: 权限与 Tab 门控

#### Scenario: Tab 可见性

- **WHEN** 用户无 `platform.domain.control.ticket_type.read`
- **THEN** 侧栏不展示「工单管理」Tab

#### Scenario: 按钮与 API 一致

- **WHEN** 用户无按钮权限
- **THEN** 按钮不可见；若仍调用 API → 403 + 中文

### Requirement: API 契约

工单类型 API MUST 使用 `status_flow` 与 `form_schema` 字段；MUST NOT 再使用 `dynamic_fields` 表示状态流。

#### Scenario: 响应字段

- **WHEN** 客户端 GET 工单类型详情
- **THEN** 响应含 `status_flow`、`form_schema`、`status`，不含 `dynamic_fields`
