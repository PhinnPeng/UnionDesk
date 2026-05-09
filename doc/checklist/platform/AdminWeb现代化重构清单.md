# AdminWeb 现代化重构清单

> 适用范围：`UnionDeskWeb/apps/UnionDeskAdminWeb`
>
> 目标：使用 UI UX Pro Max 设计系统，将管理端从功能可用的 Ant Design 页面升级为企业级商务管理端，并补齐 PRD 要求的缺失页面。

## 前置条件

- [x] P0 命名重构已完成，源码层无 `P0` 残留
- [x] UI UX Pro Max 设计系统 `MASTER.md` 已生成

## 开发任务

- [x] C-0. 设计系统初始化
  - [x] 运行 `--design-system --persist` 生成 AdminWeb 设计系统
  - [x] 运行 `--stack react` 生成 React 技术栈指南
  - [x] 运行 `--domain ux` 生成 UX 最佳实践

- [x] C-1. 登录页重构
  - [x] 运行 `--page "login"` 生成登录页覆盖
  - [x] 重构为现代化登录界面
  - [x] 保留 SliderCaptcha 与 `fetchLoginConfig` 逻辑
  - [x] 检查点：登录与验证码滑块功能正常

- [x] C-2. 管理首页重构
  - [x] 运行 `--page "dashboard"` 生成仪表盘覆盖
  - [x] 重构为统计卡片 + 快捷入口 + 审计概览
  - [x] 数据源保持 `fetchPlatformOverview`
  - [x] 检查点：API 数据加载展示正常

- [x] C-3. 业务域管理页重构
  - [x] 运行 `--page "domains"` 生成域管理页覆盖
  - [x] 重构为现代化表格 + 搜索/筛选 + 优化 Modal + step-up 确认
  - [x] 保持 `fetchAdminDomainsPage` / `createAdminDomain` / `updateAdminDomain` / `deleteAdminDomain`
  - [x] 检查点：列表、新建、编辑、删除功能正常

- [x] C-4. 工单池页重构
  - [x] 运行 `--page "ticket-pool"` 生成工单池页覆盖
  - [x] 重构为现代化 Table + 状态标签 + 领取操作
  - [x] 检查点：工单列表加载与领取操作正常

- [x] C-5. 站内信管理页重构
  - [x] 运行 `--page "inbox"` 生成站内信页覆盖
  - [x] 重构为现代化通知列表 + 已读/未读 + 筛选
  - [x] 检查点：列表加载与标记已读正常

- [x] C-6. 客户入域 / 附件页面统一风格
  - [x] `platform/domain-onboarding/` 已应用设计系统
  - [x] `platform/attachments/` 已应用设计系统
  - [x] 检查点：相关功能正常

- [x] C-7. 员工管理页重构
  - [x] 运行 `--page "staff"` 生成员工页覆盖
  - [x] 改接 `fetchAdminStaffPage` / `updateStaffStatus`
  - [x] 检查点：员工列表与状态变更正常

- [x] C-8. 离职池页面重构
  - [x] 运行 `--page "offboard"` 生成离职池页覆盖
  - [x] 应用设计系统并完成功能验证
  - [x] 检查点：离职池列表与恢复操作正常

- [x] C-9. 审计日志页（新增，对应 TC-020 / TC-021）
  - [x] 运行 `--page "audit-logs"` 生成审计日志页覆盖
  - [x] 新建 `platform/audit-logs/index.tsx`
  - [x] 调用 `fetchPlatformAuditLogs` + `fetchLoginLogsPage`
  - [x] 检查点：API 调用正常，数据展示正确

- [x] C-10. 域配置页（新增，对应 TC-045）
  - [x] 运行 `--page "domain-config"` 生成域配置页覆盖
  - [x] 新建域配置入口
  - [x] 调用 `fetchDomainConfig` / `updateDomainConfig`
  - [x] 检查点：配置加载与更新正常

- [x] C-11. 系统设置页（新增，对应 TC-044 / TC-045）
  - [x] 运行 `--page "system-settings"` 生成系统设置页覆盖
  - [x] 新建 `platform/system-settings/index.tsx`
  - [x] 调用 `fetchSystemConfig` / `updateSystemConfig`
  - [x] 检查点：配置加载与更新正常

- [x] C-12. SLA 管理页（新增，对应 TC-018 / TC-041）
  - [x] 运行 `--page "sla-management"` 生成 SLA 页面覆盖
  - [x] 新建域级 SLA 管理入口
  - [x] 调用 `fetchSlaRules` / `createSlaRule` / `updateSlaRule`
  - [x] 检查点：SLA 规则 CRUD 正常

## 最终验证

- [x] `pnpm --dir UnionDeskWeb/apps/UnionDeskAdminWeb typecheck` 通过
- [x] `pnpm --dir UnionDeskWeb/apps/UnionDeskAdminWeb test --run` 通过（40/40）
- [x] 源码层无 `P0[A-Z_]` 残留
- [x] UI UX Pro Max Pre-Delivery 代码级检查通过
  - 说明：已完成源码扫描、对比度与断点检查；当前环境未启用可用的浏览器后端，后续可补跑可视化验收。

## 完成定义

- [x] 所有已有页面按 UUPM 设计系统完成重构
- [x] 4 个缺失页面已补齐
- [x] 员工管理改接 Shared API
- [x] typecheck + test 全绿
- [x] 平台用户菜单已调整为 用户列表 / 客户列表 / 离职池
- [x] 公告为空状态时，右列高度与左列第二层一致
