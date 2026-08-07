# 客户列表操作列纯图标现代化改造

## Goal

业务控制台「客户管理」列表页（`apps/UnionDeskAdminWeb/src/pages/domain/customers/index.tsx`）操作列改为纯图标按钮，呈现更现代的界面：编辑、禁用/启用、更多（下拉）。

## Requirements

- R1 操作列改为 3 个纯图标按钮，全部带 `Tooltip` 悬浮提示：
  - 「编辑」：`EditOutlined`，点击打开客户详情弹窗（弹窗内容保持只读展示，无编辑接口；标题由「查看客户」改为「客户详情」或保持，见实现时与主会话确认，默认改为「客户详情」）
  - 「禁用/启用」：启用态显示 `StopOutlined`（禁用），禁用态显示 `PlayCircleOutlined`（启用）；保留 `ConfirmPopover` 二次确认
  - 「更多」：`EllipsisOutlined`，点击展开 `Dropdown` 菜单，菜单项：重置密码（保留 AuthGuarded 权限控制）、查看详情
- R2 「查看」按钮更名为「编辑」（纯图标后无文字）；原「重置密码」按钮从操作列主区移入「更多」下拉菜单，仍用 `AuthGuarded` 控制可见性（无权限时不渲染该菜单项）
- R3 操作列宽度由 190 收缩至 ~120（纯图标占位小），`scroll={{ x }}` 相应调整（1280 → ~1210）
- R4 保留权限体系：编辑/详情沿用 `DOMAIN_CUSTOMER_READ` 包裹可见性？——现状「查看」无权限包裹（页面整体 AuthGuarded READ），保持现状即可；禁用/启用沿用 `DOMAIN_CUSTOMER_UPDATE_STATUS`；重置密码沿用 `DOMAIN_CUSTOMER_RESET_PASSWORD`
- R5 全列居中保持；列表其余列不动

## Acceptance Criteria

- [ ] AC1 操作列显示 3 个图标按钮（编辑/禁用或启用/更多），无文字按钮；悬浮显示中文 Tooltip
- [ ] AC2 点击编辑图标打开详情弹窗；弹窗展示只读详情
- [ ] AC3 点击更多图标展开下拉菜单，含「重置密码」「查看详情」；无 `DOMAIN_CUSTOMER_RESET_PASSWORD` 权限时菜单不含重置密码项
- [ ] AC4 禁用/启用走图标 + ConfirmPopover 二次确认，行为与现状一致
- [ ] AC5 操作列宽度收缩且表格不溢出错位；其余列（居中/宽度/格式化）保持不变
- [ ] AC6 typecheck:admin 与 check:utf8 通过

## Out of Scope

- 真正的编辑能力（后端无更新客户信息接口，弹窗保持只读）
- 平台域详情客户 Tab（detail-customers.tsx）
- 工具栏按钮（添加客户）与其他列改动
