# Research: AdminWeb 路由双端归属清单

- **Query**: 列出 AdminWeb 所有页面及路由归属（平台端 `/platform/*` vs 业务域端根级），每页一行：路径 → 功能摘要 → 实现状态
- **Scope**: internal（路由 + 页面代码勘察）
- **Date**: 2026-08-11

## Findings

### 判定规则（与 backlog-epics.md §1.2 / inventory 一致）

- **平台端**：路由前缀 `/platform/`（`app-scope-core.ts isPlatformRoutePath`），菜单 `scope=platform`；部分页面在 `router/routes/core/platform-pages.ts` 内置（hideInMenu），其余由后端菜单动态注册（`generate-routes-from-backend.ts`，`component` 映射 `pages/<path>`）
- **业务域端**：根级非 `/platform/`，菜单 `scope=business`；前端静态模块 `routes/modules/{home,system,domain}.ts` + 后端菜单
- **员工工作台（工单处理）归属结论**：业务域端。证据：`V20260728170000__business_home_workbench_menu.sql` 建 `BUSINESS-HOME-MENU` 菜单 `route_path='/home'`、`scope='business'`、权限 `domain.home.read`，授予 `domain_admin` + `agent`；首页即 `pages/home/index.tsx`（业务域端概览，非平台）。**但工单队列/处理（US-S3-04）尚未成品**：现仅有 `/platform/ticket-pool`（P0 演示页）与 `/platform/ticket-detail`（P0 演示面板），business 端无工单处理页面（grep 工单队列/ticket-queue 0 命中）

### 平台端（`/platform/*`，scope=platform）— 约 27 个页面文件

| 路由 | 页面文件 | 功能摘要 | 实现状态 |
|:---|:---|:---|:---|
| `/platform/home` | `platform/home` | 仪表盘（Statistic 卡 + 快捷入口 + 最近审计） | **Partial**（后端 DemoDataService mock，inventory §1） |
| `/platform/user` | `platform/user` | 用户列表/创建/编辑/离职/重置密码 | **Done**（inventory §4.1；永久删除无前端入口 Partial） |
| `/platform/dept` | `platform/dept` | 组织/部门树 CRUD | **Done**（inventory §2） |
| `/platform/offboard-pool` | `platform/offboard-pool` | 离职池（列表 + 一键恢复） | **Done** |
| `/platform/org-config` | `platform/org-config` | 组织配置 | **Todo**（Empty 占位「组织配置功能开发中」） |
| `/platform/domains` | `platform/domains` | 业务域卡片列表/新建向导/删除 step-up | **Done** |
| `/platform/domains/detail/:domainId?` | `platform/domains/detail` | 业务域控制台：Meta + 10 Tab（概览/基础/配置/客户入域/工单/角色/屏蔽词/通知/日志/成员） | **Done**（US-S2-01~06；`detail-tickets.tsx` 工单类型配置 US-S3-01 Done） |
| `/platform/domains/ticket/form-design/:domainId/:typeId` | `common/form-design` | Formily 低代码表单设计器（草稿/发布/版本历史） | **Done**（US-S3-01） |
| `/platform/domains/ticket-type-config/:domainId/:typeId(/flow)` | `platform/domains/ticket-type-config` | 事项类型配置 + React Flow 状态流 DAG | **Done**（US-S3-01） |
| `/platform/domains/ticket-type-attributes/:domainId/:typeId` | `platform/domains/ticket-type-attributes` | 属性插槽编排（拖拽排序/默认值） | **Done**（US-S3-01a） |
| `/platform/permission` | `platform/permission` | 权限管理（重定向 `/platform/role`） | **Partial**（仅 Navigate 重定向） |
| `/platform/role` | `platform/role` | 角色列表/创建/编辑（权限树）/删除 | **Done** |
| `/platform/menu` | `platform/system/menu` | 菜单树 CRUD（scope 筛选、图标选择） | **Done**（前端注册于 `pages/platform/system/menu`） |
| `/platform/audit-logs` | `platform/audit-logs` | 审计统一页（审计/登录 Tabs） | **Done** |
| `/platform/log/operation-log` | `platform/log/operation-log` | 操作日志独立页 | **Done**（与 audit-logs 功能重叠，inventory §5.1 备注建议收敛） |
| `/platform/log/login-log` | `platform/log/login-log` | 登录日志独立页 | **Done**（同上重叠） |
| `/platform/blockwords` | `platform/blockwords` | 平台全局屏蔽词 | **Done**（US-S2-05） |
| `/platform/ticket-config`(+`/types` `/attributes` `/statuses` `/templates`…) | `platform/ticket-config/*` | 事项配置全家桶：类型/属性/状态/团队模板（含子配置页） | **Done**（S3 期新增；templates 菜单已隐藏 `V20260726092200__hide_ticket_team_template_menu`） |
| `/platform/ticket-pool` | `platform/ticket-pool` | 工单池列表 + 领取（P0 演示页，标注「路径对齐」） | **Partial**（演示口径，US-S3-04 要求升级为 business 工作台） |
| `/platform/ticket-detail` | `platform/ticket-detail` | 管理端工单处理面板（P0 演示） | **Partial**（演示口径） |
| `/platform/sla-management` | `platform/sla-management` | SLA 规则 + 工作日历 CRUD | **已实现**（前端调 `#src/api/platform/sla`；后端 `uniondesk-support` `sla` 包：SlaRuleController/SlaService/SlaTimingEngine 等 — **backlog E4 标「占位」与代码不符**） |
| `/platform/inbox` | `platform/inbox` | 管理端站内信（P0 inbox API） | **已实现**（P0 契约） |
| `/platform/system-settings` | `platform/system-settings` | 系统设置 KV（fetchSystemConfig/updateSystemConfig） | **已实现** |
| `/platform/attachments` | `platform/attachments` | 附件上传（服务端代理→MinIO，P0 契约对齐） | **已实现**（依赖 minio 服务） |
| `/platform/import-export` | `platform/import-export` | 用户导入导出 | **Todo**（页面存在，API 待查，inventory §4.1） |
| `/platform/domain-config` | `platform/domain-config` | 域配置 KV 表单 | **Done** |
| `/platform/domain-onboarding` | `platform/domain-onboarding` | 客户入域邀请码面板 | **Partial**（inventory §3；CustomerWeb 接真实 API 未完成） |

### 业务域端（根级非 `/platform/`，scope=business）

| 路由 | 页面文件 | 功能摘要 | 实现状态 |
|:---|:---|:---|:---|
| `/home` | `home` | 业务域端首页/工作台：当前域 + 快捷入口（按权限过滤）+ 说明 | **Done**（US-S2-E2-00；V20260728170000 工作台菜单；V20260728194500 扁平化后置顶「概览」） |
| `/system/user` | `system/user` | 系统用户 | **占位**（模板残留：单 Input，inventory §7 一致） |
| `/system/role` | `system/role` | 系统角色 | **Done**（inventory §4.2；domain scope 角色管理） |
| `/system/menu` | `system/menu` | 系统菜单 | **Done**（inventory §4.4；business 菜单管理） |
| `/system/dept` | `system/dept` | 系统部门 | **占位**（模板残留：计数器 demo） |
| `/domain/overview` | `domain/overview` | 运营概览：4 项 Statistic（值「—」）+ 趋势 Empty | **Partial**（统计值硬编码「—」，趋势「数据接入中」） |
| `/domain/ticket-config`(+`/types/:typeId`) | `domain/ticket-config` | 事项配置：类型/属性/状态三面板（复用 platform 面板） | **Done**（S3 期新增，domain.ticket_type/attribute/status 权限） |
| `/domain/customers/list` | `domain/customers` | 客户列表：查询/手动添加/从员工导入/启停/编辑/重置密码/只读详情 | **Done**（真实 API；V20260806120000/130000 增重置密码与编辑按钮菜单） |
| `/domain/customers/onboarding` | `domain/onboarding` | 入域配置（邀请码/注册策略） | **Done** |
| `/domain/settings/basic` | `domain/basic` | 通用设置（域名称/LOGO/描述） | **Done** |
| `/domain/settings/members` | `domain/members` | 员工管理：添加/改角色/启停/移除 | **Done**（真实 API，域内版 US-S2-03） |
| `/domain/settings/roles` | `domain/roles` | 角色管理（列表 + 只读权限项） | **Done**（域内版 US-S2-02） |
| `/domain/settings/onboarding` | `domain/onboarding`（复用） | 入域管理 | **Done** |
| `/domain/settings/config` | `domain/config` | 参数配置 KV | **Done** |
| `/domain/settings/blockwords` | `domain/blockwords` | 屏蔽词库（域内 CRUD） | **Done** |
| `/domain/settings/notifications` | `domain/notifications` | 通知配置 | **Todo**（Empty「功能开发中，菜单与权限已就绪」） |
| `/domain/settings/audit-logs` | `domain/audit-logs` | 操作日志（域级） | **Done** |
| `/domain/settings/login-logs` | `domain/login-logs` | 登录日志（域级） | **Done** |
| 旧路径 `/domain/customers`、`/domain/basic` 等 | `domain/legacy-redirect` | 旧路由重定向到新 `/domain/*` 路径 | Done（11 条 redirectRoute） |

### 其他（核心/演示/未注册）

| 路由 | 说明 |
|:---|:---|
| `/login` | 登录页（含滑块验证 + forgotPassword 模板组件——**无真实 API，纯倒计时 stub**） |
| `/personal-center/my-profile`、`/personal-center/settings` | 个人中心（模板页） |
| `/access/*`、`/route-nest/*`、`/about`、`/outside/*` | 模板 demo 路由（react-antd-admin 遗留，权限演示用） |
| `/privacy-policy`、`/terms-of-service` | 外部公开路由 |
| `/exception/403/404/500`、`unknown-component` | 异常页 |
| `dev/ticket-type-designer-poc` | **路由未注册**（源码注释自证：「Dev-only POC…路由未注册」） |
| `/workspace` | CustomerWeb 路由（AdminWeb 无） |

### 计数

- **平台端**：约 27 个页面文件（含子路径），Done 主导；Partial 4（home/ticket-pool/ticket-detail/permission/domain-onboarding）；Todo 2（org-config、import-export）
- **业务域端**：约 17 个页面文件，Done 主导；Partial 1（overview）；Todo 2（notifications、system/user、system/dept 占位）
- 平台/域双端同源页面（customer/member/role/blockword/audit-log/login-log 域详情 Tab 与域端页面并存）

## Caveats / Not Found

- 路由注册以「前端静态模块 + 后端菜单动态路由」双轨：本表路径以后端菜单种子 + 前端模块为准；个别页面（如 `platform/ticket-pool`、`sla-management`）菜单注册需以联调库 `iam_admin_menu` 实际数据复核（代码存在，是否挂菜单未逐一核对种子 SQL）
- inventory v1.3（2026-06-17）未覆盖 S3 期大量新页面（ticket-config 全家桶、domain/ticket-config、SLA、domain/customers 增强等），状态以本次代码勘察为准，备注中应注明「inventory 缺项」
