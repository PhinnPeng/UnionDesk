# 批次A：业务域（domain/）操作列图标化

## Goal

将业务控制台 domain/ 目录下 5 处操作列改造为统一纯图标语言（图标词典与规范见父任务 `08-07-admin-table-action-icons` PRD）。

## 改造清单

| 文件 | 现状 | 目标 |
|---|---|---|
| `domain/members/index.tsx` | P1 文字：编辑角色/禁用/启用/删除 | 图标：编辑角色(EditOutlined) + 禁用(Stop)/启用(PlayCircle) + 更多(Ellipsis)下拉→删除(DeleteOutlined, danger) |
| `domain/roles/index.tsx` | P1 文字：配置权限/编辑/删除 | 图标：编辑(EditOutlined) + 更多(Ellipsis)下拉→配置权限/删除 |
| `domain/blockwords/index.tsx` | P1 文字：删除 | 图标：删除(DeleteOutlined, danger)，保留 ConfirmPopover |
| `domain/onboarding/index.tsx` | P1 文字：删除 | 图标：删除(DeleteOutlined, danger)，保留 ConfirmPopover 与整列条件渲染 |
| `domain/ticket-config/types-panel.tsx` | P2+P4：图标+文字，MoreOutlined 无 Tooltip | 统一：More→EllipsisOutlined + Tooltip；编辑/属性/工作流按钮补 Tooltip；菜单项补图标 |

## Requirements

- R1 全部图标按钮加中文 Tooltip
- R2 「更多」统一 `EllipsisOutlined`（types-panel 由 MoreOutlined 迁移）
- R3 破坏性操作保留 ConfirmPopover；禁用/启用、编辑、权限判断逻辑原样保留
- R4 操作列宽度按需收缩（成员表 220 → ~130）；fixed right 保留
- R5 只改操作列相关代码，不动查询/表单/弹窗内容

## Acceptance Criteria

- [ ] AC1 5 个文件操作列均为纯图标按钮（≤3 主图标 + 更多下拉），无文字按钮残留
- [ ] AC2 全部图标带 Tooltip；更多 trigger 为 EllipsisOutlined
- [ ] AC3 删除类动作保留二次确认；行内业务逻辑（禁用/启用/编辑/权限）不变
- [ ] AC4 typecheck:admin、check:utf8 通过
- [ ] AC5 浏览器冒烟：members 编辑角色/禁用确认、roles 更多下拉、types-panel 更多下拉正常

## Out of Scope

- domain/customers（已达标，不动）
- 删除动作确认机制变更（现状 ConfirmPopover 保留）
