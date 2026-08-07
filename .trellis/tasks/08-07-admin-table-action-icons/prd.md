# AdminWeb 表格操作列统一图标操作语言

## Goal

统一 UnionDeskAdminWeb 全部数据表格的操作列视觉语言：以 domain/customers 已落地的纯图标模式（P3+P4）为基准，分三批改造其余 30 处操作列，并固化「图标词典 + 交互规范」到 spec，作为后续页面开发的操作列标准。

## 统一图标词典（本任务核心产物）

| 动作 | 图标 | 说明 |
|---|---|---|
| 编辑 | `EditOutlined` | |
| 查看/详情 | `EyeOutlined` | |
| 删除/移除/拔出 | `DeleteOutlined` | danger 色 |
| 禁用 | `StopOutlined` | |
| 启用 | `PlayCircleOutlined` | |
| 重置密码 | `KeyOutlined` | 更多菜单项内 |
| 新增下级/添加 | `PlusOutlined` | 树表场景 |
| 更多（下拉 trigger） | `EllipsisOutlined` | 全仓统一，废弃 `MoreOutlined` |
| 属性/设置 | `SettingOutlined` | 工单配置家族保留 |
| 工作流 | `NodeIndexOutlined` | 工单配置家族保留 |
| 离职 | `UserDeleteOutlined` | platform/user 保留 |

## 统一交互规范

- 所有操作列图标按钮必须包裹 `Tooltip`（中文提示）
- 主操作区 ≤3 个图标；低频/危险/次要动作收进「更多」Dropdown（`trigger=["click"]`，菜单项带图标）
- 破坏性操作（删除/禁用/离职等）必须 ConfirmPopover 二次确认（既有 Modal.confirm 的可保留或统一为 ConfirmPopover，以最小侵入为准）
- 行内操作权限沿用现状（AuthGuarded 或条件渲染均可，不强改）
- 操作列宽度按图标数量收缩（3 图标 ~120）

## Requirements

- R1 固化上述词典与规范：更新 `.trellis/spec/frontend/` 下相关指南（component-guidelines.md 或新增操作列规范小节）
- R2 三批改造（子任务 A/B/C），目标模式：`P3 纯图标 + P4 更多下拉`（参照 domain/customers 实现）
- R3 代码副本同步：system/menu ↔ platform/system/menu、types-panel ↔ domain-ticket-types-panel 同步修改
- R4 统一「更多」trigger 为 `EllipsisOutlined`，废弃 `MoreOutlined`
- R5 不改变业务行为：禁用/启用/删除/权限判断逻辑原样保留

## Acceptance Criteria

- [ ] AC1 spec 中固化图标词典与交互规范
- [ ] AC2 子任务 A/B/C 全部完成：31 处操作列中仅剩无操作列页面维持现状，其余全部 P3+P4 纯图标
- [ ] AC3 无 `MoreOutlined` 残留；全部图标按钮带 Tooltip
- [ ] AC4 typecheck:admin / check:utf8 全绿
- [ ] AC5 浏览器冒烟抽查各批次代表页（编辑/更多下拉/禁用确认正常）

## Out of Scope

- 删除动作确认机制的强制统一（sla/templates/statuses 三处直接删除补确认属建议项，各批次视成本决定，不得扩大改动面）
- 权限包裹方式统一（AuthGuarded vs canXxx）
- 日志类只读表、Modal 内选择表（无操作列）
- platform/user 的 offboard 状态行、offboard-pool 全 disabled 占位列（保留现状）
