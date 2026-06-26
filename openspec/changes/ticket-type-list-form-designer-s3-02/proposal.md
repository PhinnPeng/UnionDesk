## Why

**S3-01** 已交付工单类型列表、Formily 设计器与状态流配置，但存在三类缺口：

1. **持久化误导**：设计器顶栏「保存/发布」仅更新前端内存并提示「保存成功」，未调用后端；用户以为已落库。
2. **无草稿/发布分离**：`form_schema` 单字段，无法「保存草稿」与「发布生效」区分；用户侧应始终读取已发布版本。
3. **列表与流程体验不足**：SubTab 仍名「工单类型」、Table 展示；缺描述/图标、域内编码名称唯一约束；创建后强制跳转全量三 Tab 配置页，与「表单设计 / 编辑基础信息」分流需求不符。

## What Changes

### 后端

- Flyway：`form_schema_draft`、`description`、`icon`；域内 `(business_domain_id, code)` / `(business_domain_id, name)` 唯一索引。
- API：扩展 TicketType DTO；新增 `PUT .../form-schema/draft`、`POST .../form-schema/publish`；`updateTicketType` 不再写 `form_schema`。
- 创建/更新时唯一性校验与中文错误信息。

### 前端

- 域详情 SubTab「工单类型」→ **「工单列表」**；列表 **卡片网格** 展示（居中布局）。
- 创建弹窗：编码、名称、描述、图标；创建成功后 **询问是否进入表单设计**。
- 卡片操作：**表单设计**（独立页 Tab）、**编辑**（基础信息弹窗）、启用/停用、删除。
- 独立表单设计页 `/platform/domains/ticket/form-design/:domainId/:typeId`（`src/pages/common/form-design/index.tsx`）；**保存=草稿 API，发布=发布 API**。
- 抽取 `IconPicker` 至 `src/components/icon-picker/` 供菜单与工单类型共用。
- 设计器 Actions 区业务化（移除 Playground 链接）。

## Capabilities

### New Capabilities

- `ticket-type-list-form-designer` — 工单列表 UX、元数据扩展、表单 schema 草稿/发布持久化

### Modified Capabilities

- `ticket-type-config`（S3-01）— 配置入口由全量三 Tab 页拆分为列表 + 表单设计页 + 编辑弹窗

## Impact

- `uniondesk-ticket`：`TicketTypePo`、Mapper、Service、Controller、迁移
- `UnionDeskWeb/packages/shared`：类型与 API
- `detail-tickets.tsx`、表单设计路由、`formily-form-designer` 组件
- 用户侧（S3-03 及后续）仅消费 `form_schema`（已发布）

## Non-Goals

- CustomerWeb 提单页改造（仅约定读取发布版 schema）
- 工单模板 SubTab 卡片化（本 change 可顺带表格居中，非主目标）
- 编辑弹窗内嵌状态流（状态流保留独立入口 `/flow`）
- 设计器深浅色主题适配（已回退固定浅色）
