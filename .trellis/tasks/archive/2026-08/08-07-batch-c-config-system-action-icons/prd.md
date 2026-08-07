# 批次C：配置家族与系统（ticket-config / system/）操作列图标化

## Goal

将工单配置家族各面板与系统管理（system/）下操作列改造为统一纯图标语言（图标词典与规范见父任务 `08-07-admin-table-action-icons` PRD）。本批次含代码副本，需同步修改。

## 改造清单

| 文件 | 现状 | 目标 |
|---|---|---|
| `platform/ticket-config/types/components/ticket-type-sortable-table.tsx` | P2+P4：编辑/属性/工作流+More 下拉（复制/删除） | More→Ellipsis + Tooltip；菜单项补图标 |
| `platform/ticket-config/attributes/components/attribute-sortable-table.tsx` | P2：编辑/删除 图标+文字 | 纯图标 + Tooltip |
| `platform/ticket-config/statuses/ticket-statuses-panel.tsx` | P1 文字：编辑/删除 | 图标：编辑 Edit/删除 Delete danger + Tooltip |
| `platform/ticket-config/templates/team-templates-panel.tsx` | P1 文字：配置/删除 | 图标：配置(SettingOutlined)/删除 Delete danger + Tooltip |
| `platform/ticket-config/templates/config/collaboration-panel.tsx` | P2+P4：属性/工作流 + More 下拉（移除） | More→Ellipsis + Tooltip；菜单项补图标 |
| `platform/domains/ticket-type-attributes/index.tsx` | P1 文字：拔出 | 图标：拔出（`RollbackOutlined` 或确认用词后定）+ Tooltip，ConfirmPopover 保留 |
| `platform/domains/ticket-type-config/components/attribute-slot-table.tsx` | P2：编辑/删除 图标+文字 | 纯图标 + Tooltip |
| `platform/domains/ticket-type-config/components/workflow/rule-config-modal.tsx` | P1 文字：编辑（Modal 内小表） | 图标：Edit + Tooltip |
| `platform/domains/ticket-type-config/components/workflow/step-rule-attribute-update-editor.tsx` | P1 文字：删除（Modal 内小表） | 图标：Delete danger + Tooltip |
| `system/menu/index.tsx`（含副本 `platform/system/menu/index.tsx`） | P1 树表：新增目录/新增按钮/编辑/删除 | 图标：Plus/Edit/Delete danger + Tooltip，ConfirmPopover 保留；两处同步 |
| `system/role/index.tsx` | P1 文字：编辑/删除 | 图标：Edit/Delete danger + Tooltip，ConfirmPopover 保留 |
| `common/form-design/components/form-schema-version-drawer.tsx` | P2：预览/回退 图标+文字 | 纯图标 + Tooltip |
| `platform/domains/detail/components/domain-ticket-types-panel.tsx` | P2+P4（types-panel 副本） | 与 `domain/ticket-config/types-panel.tsx` 同步（批次 A 已改则此处对齐） |

## Requirements

- R1 全部图标按钮加中文 Tooltip
- R2 「更多」统一 `EllipsisOutlined`（4 处 MoreOutlined 迁移）
- R3 菜单项补图标；删除项 danger 保留
- R4 代码副本必须同步修改（system/menu 双副本、types-panel 双副本）
- R5 业务逻辑/确认机制/权限判断原样保留；Modal 内小表格同样图标化（Tooltip 可省，空间受限时以主会话裁决为准）

## Acceptance Criteria

- [ ] AC1 清单内文件操作列均为纯图标，无文字按钮残留（Modal 内小表除外，Tooltip 可省略）
- [ ] AC2 全仓无 `MoreOutlined` 残留（grep 验证）
- [ ] AC3 副本文件同步一致（system/menu、types-panel 两对）
- [ ] AC4 typecheck:admin、check:utf8 通过
- [ ] AC5 浏览器冒烟：system/menu 树表操作、ticket-type-sortable 更多下拉正常

## Out of Scope

- 日志只读表、Modal 内选择表
- 删除动作确认机制的强制统一（现状保留）
