# 客户列表页：批量添加员工整合进添加客户弹窗，移除批量禁用/启用

## Goal

业务控制台「客户管理」列表页（`apps/UnionDeskAdminWeb/src/pages/domain/customers/index.tsx`）工具栏优化：

- 移除独立的「批量添加员工」按钮，将批量能力整合进「添加客户」弹窗的「选择员工新增」模式（单选 → 多选），一次勾选多名员工批量创建客户。
- 移除「批量禁用」「批量启用」按钮及相关行选择（rowSelection）能力。

## Requirements

- R1 「添加客户」弹窗「选择员工新增」模式由单选（radio）改为多选（checkbox），提交时以选中员工 ID 数组调用现有 `createDomainCustomersFromStaff`（接口已支持数组）。
- R2 移除工具栏「批量添加员工」按钮（功能并入添加客户弹窗后不再需要独立入口）。
- R3 移除工具栏「批量禁用」「批量启用」按钮；`DomainCustomersPage` 中相关的 `selectedRowKeys` 状态、`applyStatusChange` 批量路径、`confirmEnable` 批量调用、Table `rowSelection` 一并移除。行内单条「禁用/启用」按钮保留（属操作列，不属批量）。
- R4 权限不新增：批量添加员工沿用现有 `DOMAIN_CUSTOMER_CREATE`；批量状态操作移除后 `DOMAIN_CUSTOMER_UPDATE_STATUS` 仍用于行内禁用/启用。

## Acceptance Criteria

- [ ] AC1 工具栏仅剩「添加客户」按钮（权限 `DOMAIN_CUSTOMER_CREATE`）
- [ ] AC2 「添加客户」弹窗「选择员工新增」可勾选多名员工，确认后批量创建客户并提示 `成功添加 N 名（跳过 M 名）`（沿用现有提示逻辑）
- [ ] AC3 页面无「批量禁用」「批量启用」按钮，表格无行选择复选框列
- [ ] AC4 行内「禁用/启用」按钮行为不变
- [ ] AC5 本次改动不涉及后端与 shared 包接口改动

## Out of Scope

- 后端接口改动（`createDomainCustomersFromStaff` 已支持数组）
- 平台域详情客户 Tab（detail-customers.tsx）
- 重置密码相关功能（现有任务 08-06-customer-reset-password 内容不受影响）
