## Why

**US-S3-01** 要求域管理员在平台业务域详情「工单」Tab 配置工单类型与模板，对齐 **PRD F3.1 终局**：低代码表单设计器 + 状态流程 DAG。当前 `detail-tickets.tsx` 为 Empty；后端 API 存在但 DTO 语义混乱（`dynamic_fields` 实为状态流）；IAM 控制台绑定后须 `platform.domain.control.*` 权限。

## What Changes

- **Flyway**：`ticket_type.status`、`ticket_type.form_schema`；`platform.domain.control.ticket_type.{read,create,update,delete}`；预置类型 + §6.4 默认状态流种子。
- **后端**：
  - API 契约修正：`status_flow` + `form_schema`（废弃 `dynamic_fields`）。
  - `StatusFlowValidator`（TR-01 + 流转图完整性）；`FormSchemaValidator`（Formily schema）。
  - 启用/停用、删除引用保护；Controller 改绑 platform 权限。
- **前端**：
  - `detail-tickets.tsx` 列表 + `TicketTypeDesignerDrawer`（基本信息 / Formily 设计器 / React Flow DAG）。
  - 模板 CRUD；`detail-sider` Tab 门控；shared API。
- **依赖**：Formily 2.x、`@xyflow/react`（ADR + 设计文档 §6.1）。

## Capabilities

### New Capabilities

- `ticket-type-config` — 平台域详情工单类型与模板 **F3.1 全量**配置

## Impact

- `TicketConfigService` / DTO 破坏性调整
- Flyway + IAM catalog
- AdminWeb 新设计器模块 + 依赖
- `packages/shared` API
- Sprint/backlog 范围说明（原 MVP AC5 作废）

## Non-Goals

- 快捷回复 / 优先级等级 UI
- CustomerWeb 提单页（US-S3-03，仅消费 `form_schema`）
- business 端独立配置路由
- `TicketService` 运行时流转逻辑变更
