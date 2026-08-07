# 批次B：平台主列表（platform/）操作列图标化

## Goal

将平台端 platform/ 主列表及域详情 Tab 下 9 处操作列改造为统一纯图标语言（图标词典与规范见父任务 `08-07-admin-table-action-icons` PRD）。

## 改造清单

| 文件 | 现状 | 目标 |
|---|---|---|
| `platform/user/index.tsx` | P2+P4：编辑/离职/更多（文字「更多」+Ellipsis），菜单项无图标，无 Tooltip | 补 Tooltip；「更多」按钮去文字化；菜单项补图标（重置密码=KeyOutlined） |
| `platform/dept/index.tsx` | P2 树表：Plus/Edit/Delete+文字 | 纯图标（新增下级 Plus/编辑 Edit/删除 Delete danger），Tooltip，Modal.confirm 保留 |
| `platform/blockwords/index.tsx` | P1 文字：删除 | 图标：DeleteOutlined danger，ConfirmPopover 保留 |
| `platform/sla-management/index.tsx` | P1 文字：编辑/删除（规则表）、编辑（日历表） | 图标：编辑 Edit / 删除 Delete danger + Tooltip |
| `platform/ticket-pool/index.tsx` | P1 文字：领取 | 图标：领取（可选用 `PlayCircleOutlined` 或保留文字按钮——唯一动作且语义特殊，主会话裁决：保留文字+图标混排或纯图标） |
| `platform/inbox/index.tsx` | P1 文字：标记已读 | 图标：已读（`CheckOutlined`），disabled 状态保留 |
| `platform/offboard-pool/index.tsx` | P1 占位：恢复/删除（均 disabled） | 图标化占位（RollbackOutlined/DeleteOutlined，保留 disabled） |
| `platform/domains/detail/components/detail-customers.tsx` | P1 文字：查看/禁用/启用 | 图标：查看 Eye/禁用 Stop/启用 PlayCircle + Tooltip，ConfirmPopover 保留 |
| `platform/domains/detail/components/detail-members.tsx` | P1 文字：编辑角色/禁用/启用/删除 | 图标：编辑角色 Edit + 禁用/启用 + 更多→删除 |
| `platform/domains/detail/components/detail-roles.tsx` | P1 文字：查看权限 | 图标：EyeOutlined + Tooltip |
| `platform/domains/detail/components/detail-blockwords.tsx` | P1 文字：删除 | 图标：DeleteOutlined danger，ConfirmPopover 保留 |

## Requirements

- R1 全部图标按钮加中文 Tooltip
- R2 「更多」统一 `EllipsisOutlined`；platform/user 的「更多」文字按钮去文字
- R3 破坏性操作保留原确认机制；AuthGuarded 权限包裹原样保留
- R4 操作列宽度按需收缩；fixed right 保留
- R5 ticket-pool「领取」动作由实现时与主会话确认（默认保留文字+图标，因其为单一主操作）

## Acceptance Criteria

- [ ] AC1 清单内文件操作列均为纯图标（唯一例外：ticket-pool 领取按钮经主会话确认后处理）
- [ ] AC2 全部图标带 Tooltip；无文字按钮残留（除 ticket-pool）
- [ ] AC3 业务逻辑与确认机制不变；权限包裹不变
- [ ] AC4 typecheck:admin、check:utf8 通过
- [ ] AC5 浏览器冒烟：user 更多下拉、dept 树表操作、detail-customers 禁用确认正常

## Out of Scope

- 平台工单配置家族（types/statuses/templates/attributes/collaboration 面板）——批次 C
- system/ 目录——批次 C
- offboard-pool 按钮启用（占位保留 disabled）
