## Context

- **产品终局**：PRD §5.3.1 F3.1 — 低代码表单设计器 + 状态流程配置；ADR Formily 2.x。
- **后端基础**：`TicketConfigController` CRUD 已有；`ticket_type.status_flow_config` 列已有；`dynamic_field_config` 表存在但本 Story 以 `form_schema` JSON 为设计器事实源。
- **前端占位**：`detail-tickets.tsx` Empty。
- **权限**：须 `platform.domain.control.ticket_type.*`（US-S3-00 后 platform_admin 无 `domain.*`）。

## Goals

- 一步交付 Formily 动态字段设计器 + React Flow 状态流 DAG，无中间 MVP 层。
- API 语义清晰：`status_flow` / `form_schema` / `status`。
- TR-01 及流转图完整性校验；预置 feedback/suggestion 可启停。
- 模板在同一 Tab 可维护。

## Decisions

### 1. 不保留 `dynamic_fields` 别名

尚无生产 UI，本 change 一次性修正 DTO，避免长期技术债。

### 2. 存储

| 数据 | 列 |
|:---|:---|
| 状态流 | `status_flow_config` |
| Formily schema | `form_schema`（新增列） |
| 启停 | `status`（新增列） |

### 3. 设计器 UI

`TicketTypeDesignerDrawer` 三 Tab：

1. 基本信息 — code / name / status
2. 动态字段 — `@formily/designable`
3. 状态流程 — `@xyflow/react` + 节点属性面板 +「导入默认模板」

列表页仅负责入口；不做简化 Modal-only 编辑。

### 4. 权限

平台域详情：`platform.domain.control.ticket_type.*`；保留 `domain.ticket_type.*` 种子。

### 5. POC Gate

合并主路径前，独立 POC 验证 Formily + antd 6 共存。

## Risks

| 风险 | 缓解 |
|:---|:---|
| 范围大于原 5 SP | 同步 sprint/backlog |
| Formily 集成成本 | POC gate |
| DAG 误配 | 后端图校验 |

## Verification

- Validator / Service 单测
- `pnpm run typecheck`
- 手工：设计器保存、默认模板导入、启停、删除保护
