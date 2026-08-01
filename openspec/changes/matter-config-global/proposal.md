# 本 Sprint — 实现方案

> **设计文档（仅事项属性）**：[`design-attribute.md`](design-attribute.md)  
> **本文件**：本 Sprint **全部交付范围**；除属性外其余项**不单独写设计**，沿用现网或简要说明。

---

## 方案概述

本 Sprint 在 **平台管理后台** 交付事项能力收口：以 **事项属性**（字典 + 插件 + 布局版本）为**唯一专项设计**；同批完成团队模板建域复制、事项类型列表 UX、工作流（维持现网 JSON）、权限菜单等，避免多份设计文档交叉重复。

**属性实现策略（v2）**：字典无 `code`，`name`+`description` 必填；MVP 四类 field_type（input/select/switch/date）；**插入类型后**才配 slot_config；编排用**拖拽列表**（一行一属性），**弃用 Formily 画布**；发布快照仍用物理表 `ticket_form_schema`（逻辑名 form_release）。  
**非属性策略**：团队模板/类型元数据按 [`design.md`](design.md) §4 执行但不扩写；工作流 **不** 拆 `ticket_status` 插件表，继续 `status_flow_config` + 现有 Flow 页。

---

## 1. 本 Sprint 交付清单

| # | 模块 | 设计深度 | 说明 |
|---|------|---------|------|
| **S1** | **事项属性** | **专项设计** | [`design-attribute.md`](design-attribute.md) |
| S2 | 团队模板 + 建域复制 | 执行项 | 建域可选模板；快照含属性+插件+layout；绑定后不可换 |
| S3 | 全局设置 → 事项配置壳 | 执行项 | 左菜单 + 全局属性/类型入口；团队模板页 |
| S4 | 域管控 · 事项类型列表 | 执行项 | 重构 `detail-tickets`；状态/显示 Switch；操作列 |
| S5 | 工作流 | 执行项 | **维持** ReactFlow + `status_flow_config`；不拆状态字典 |
| S6 | 权限 IAM | 执行项 | `platform.ticket_config.*` + `ticket_attribute.*` |
| — | 域成员后台 / CustomerWeb | 不做 | 后续 Sprint |

---

## 2. 功能结构（Sprint 全景）

```text
平台管理后台
├── 全局设置 → 事项配置                    [S3+S1 全局属性]
│   ├── 团队模板                           [S2]
│   ├── 事项类型（元数据 + 操作·属性）      [S3+S4 简版全局列表]
│   └── 事项属性（字典）                   [S1 ★]
│
├── 业务域 → 域管控 → 事项管理             [S4+S1]
│   ├── 事项类型列表
│   ├── 事项属性（域内字典）               [S1 ★]
│   └── 操作 · 属性 → form-design          [S1 ★]
│   └── 操作 · 工作流 → flow（现网）       [S5]
│
└── 创建业务域（可选团队模板）             [S2]
```

---

## 3. S1 事项属性 — 见专项设计

全部细节：**[`design-attribute.md`](design-attribute.md)**（数据表、API、UI、权限、MVP field_type、验收）。

---

## 4. S2 团队模板 + 建域（执行摘要）

- 表：`ticket_team_template`、`ticket_team_template_item`；`business_domain.applied_team_template_*`
- 建域 body 可选 `team_template_id`；`TeamTemplateApplyService` 深拷贝 **含 ticket_attribute + 插件 + published layout**
- 已确认：不强制模板；绑定后不可换；全局/域互不影响
- 参考：[`design.md`](design.md) §4（不重复展开）

---

## 5. S4 事项类型列表（执行摘要）

- 复用/新建 `ticket-type-list-table.tsx`（platform | domain）
- 列：名称、描述、状态 Switch、显示 Switch、操作（编辑·属性·工作流·删除）
- 属性 → 现有 `form-design`；工作流 → 现有 `flow`
- 隐藏「事项模板」SubTab

---

## 6. S5 工作流（执行摘要）

- **本 Sprint 不重构**为状态字典 + `ticket_flow_config` 版本表
- 继续 `ticket_type.status_flow_config` + `StatusFlowValidator`
- 多 terminal：**允许**（现网校验已支持）
- 客户撤回：**仅 `in_progress` 可配 `allow_customer_withdraw`**（现网规则不变）
- 状态 SLA / 流转条件插件化 → **下一 Sprint**

---

## 7. 实现步骤

| 步骤 | 内容 | 检查点 |
|------|------|--------|
| 1 | Flyway：`ticket_attribute`、`ticket_type_attribute` | 表可写 |
| 2 | 属性字典 + 插件 + 物化发布 API | Postman 通 |
| 3 | 域内属性 UI + form-design 插件面板 | 草稿/发布/未发布 Tag |
| 4 | 全局属性 + promote + IAM | 全局 CRUD + 上浮 |
| 5 | 团队模板 + 建域复制（含属性） | 新域有快照属性 |
| 6 | 类型列表 UX + 全局事项配置壳 | 列表与菜单可进 |
| 7 | 集成验收 | §design-attribute 验收 + 建域 + 列表 |

---

## 8. 文件清单（摘要）

### 属性核心（S1）

| 操作 | 路径 |
|------|------|
| 新增 | `V*__ticket_attribute.sql` |
| 新增 | `TicketAttributeService`、`TicketTypeAttributePluginService` |
| 修改 | `TicketFormSchemaService`（物化） |
| 修改 | `TicketConfigController` + `PlatformTicketConfigController`（属性部分） |
| 新增/改 | `ticket-config/attributes/`、`detail-tickets` 属性 Tab |
| 修改 | `form-design/`、`packages/shared/api.ts` |

### 同 Sprint 其他

| 操作 | 路径 |
|------|------|
| 新增 | 团队模板 Service/Flyway、`TeamTemplateApplyService` |
| 修改 | `DomainBootstrapService`、建域表单 |
| 修改 | `detail-tickets.tsx`、`ticket-type-list-table.tsx` |
| 新增 | IAM Flyway `platform.ticket_config.*` |

完整勾选见 [`tasks.md`](tasks.md)。

---

## 9. 约束

- 仅 **平台域**；不用 `domain.ticket_*`
- 属性 MVP：**6 种 field_type**（见 design-attribute §4）
- layout published **≤10** 版；trim 跳过 ticket 引用
- 工作流本 Sprint **不拆表**

---

## 10. 文档索引

| 文档 | 用途 |
|------|------|
| **design-attribute.md** | ★ 事项属性唯一专项设计 |
| **proposal.md**（本文件） | Sprint 总范围 + 非属性执行摘要 |
| **tasks.md** | 可勾选任务 |
| design.md | v3 全量背景（归档参考） |
