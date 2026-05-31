# 管理端实现盘点 — `implementation-inventory.md`

| 文档版本 | 日期 | 说明 |
|:---|:---|:---|
| 1.2 | 2026-05-26 | S2 Story 交叉引用（§3 平台域详情、§7 E2-00） |

> 状态说明：**Done** = 前后端均可正常使用（含基本校验与权限）；**Partial** = 核心路径可用但缺边界 case / 依赖 demo 数据 / 缺独立页面；**Todo** = 未实现或为占位。

**路由分端**（与 [`backlog-epics.md`](./backlog-epics.md) §1.2 一致）：

| 端 | 判定 |
|:---|:---|
| **平台端** | `route_path` 以 **`/platform/`** 为前缀；页面多在 `pages/platform/*` |
| **业务域端** | **非** `/platform/` 前缀的根级模块；`iam_admin_menu.scope=business`（如 `/home`、`/system/*` 等，以后台菜单为准） |

**范围**：本章 §1～§5 仅盘点 **平台端**（PRD §3.4）。业务域端见 **§7**（S2 / Epic E2 前补全）。

---

## 1. 首页

| 项 | 前端 | 后端 | 状态 | 备注 |
|:---|:---|:---|:---|:---|
| 平台首页仪表盘 | `pages/platform/home/index.tsx` | `DashboardController` → `DemoDataService` | **Partial** | 前端 UI 完整（Statistic 卡片 + 快捷入口 + 最近审计列表），但后端数据完全来自 `DemoDataService` 模拟，非真实聚合查询 |
| 平台概览 API | — | `GET /api/v1/dashboard` | **Partial** | 仅返回 mock 数据（domainCount / activeUserCount / loginLogs 等均为硬编码）；无 `DashboardService` 真实数据层 |

**S1 待办**：DashboardController 接入真实 `count(*)` 聚合查询，至少覆盖域数、在职/停用/离职工人数、近 N 天审计量。

---

## 2. 组织

| 项 | 前端 | 后端 | 状态 | 备注 |
|:---|:---|:---|:---|:---|
| 组织/部门树（列表 + CRUD） | `pages/platform/dept/index.tsx` | `OrganizationController` + `OrganizationService` | **Done** | 树形展示、搜索过滤、展开/折叠、新建/编辑弹窗（含父部门选择 + 负责人选择）、删除（含子部门校验）、循环引用检测 |
| 组织 API | — | `GET/POST /api/v1/iam/organizations`、`PUT/DELETE /api/v1/iam/organizations/{id}` | **Done** | 权限：`PLATFORM_ORGANIZATION_READ/CREATE/UPDATE/DELETE` |
| 组织架构配置页 | `pages/platform/org-config/index.tsx` | 无对应 API | **Todo** | 占位页，显示"组织配置功能开发中"（Empty 组件） |

**Flyway**：组织表（`iam_organization`）在 `V202605200002` rebaseline 中已建立。

---

## 3. 业务域

| 项 | 前端 | 后端 | 状态 | 备注 |
|:---|:---|:---|:---|:---|
| 业务域列表 | `pages/platform/domains/index.tsx` | `DomainController` | **Done** | 卡片展示 + 分页、关键词搜索、创建日期筛选；新建向导 Modal；删除需 step-up 二次认证 |
| 业务域创建 | `pages/platform/domains/components/domains-modal.tsx` | `POST /api/v1/admin/domains` | **Done** | 多步向导；Step3 含 `registration_enabled` / `invitation_enabled` 双开关（US-S1-03） |
| 业务域编辑 | `pages/platform/domains/detail/components/detail-baseinfo.tsx`、`detail-onboarding.tsx` | `PUT /api/v1/admin/domains/{id}` | **Partial** | 基础信息 Tab 可更新；**S2 US-S2-01** 完善回显与删除 UX（输入 code + Step-up） |
| 业务域详情 | `pages/platform/domains/detail/index.tsx` | `GET /api/v1/admin/domains/{id}` | **Done** | Meta 头 + 左侧 10 Tab（概览/基础/配置/客户入域/工单/角色/屏蔽词/通知/日志/成员）+ 右侧内容区 |
| 业务域配置 | `pages/platform/domain-config/config-panel.tsx` | `DomainConfigController` | **Done** | KV 键值对配置表单（key/value/valueType/description） |
| 业务域删除 | `detail-header.tsx`、列表 | `DELETE /api/v1/admin/domains/{id}` | **Partial** | **S2 US-S2-01**：`deleted_at`+`updated_*`；权限 `platform.domain.control.deleted`；无 `deleted` 列 |
| 客户入域（邀请码） | `pages/platform/domain-onboarding/` | `InvitationCodeController` | **Partial** | 前端存在 onboarding 面板，后端提供邀请码 CRUD；CustomerWeb 接真实 API 见 **US-S1-05（S1 暂缓）** |
| 域成员管理 | `detail-members.tsx` | `DomainMemberController` | **Partial** | 列表只读；**S2 US-S2-03** 添加/改角色/启停（需 status API） |
| 域客户管理 | `detail-customers.tsx` | `DomainCustomerController` | **Partial** | S1 主路径 Done；**S2 US-S2-04** 体验与单条编辑 |
| 域角色管理 | `detail-roles.tsx` | `DomainRoleController` | **Partial** | **S2 US-S2-02**「角色管理」只读；`platform.domain.roles.*` |
| 域屏蔽词（域内） | `detail-blockwords.tsx` | `BlockedWordService` | **Partial** | **S2 US-S2-05**：`platform.domain.blocked_word.*`；全局 `platform.blocked_word.*` |
| 域业务日志 | `detail-logs.tsx` | 域级 audit/login API | **Partial** | **S2 US-S2-06**：双 Tab；`platform.audit-logs.read` |
| 入域双配置迁移 | — | `V202605250001` | **Done** | 脚本已入库；HEAD 后端 DTO/Service 与 AdminWeb 双字段 UI 已对齐（US-S1-03） |

**Flyway 相关**：`V202605200002`（base 建表）、`V202605200003`（audit 字段）、`V202605240001`（description）、`V202605250001`（access policy，双字段已对齐）。

---

## 4. IAM（身份与访问管理）

### 4.1 用户管理

| 项 | 前端 | 后端 | 状态 | 备注 |
|:---|:---|:---|:---|:---|
| 平台用户列表 | `pages/platform/user/index.tsx` | `IamController` + `StaffController` | **Done** | 表格展示 + 搜索（用户名/账号/手机/邮箱） + 部门树侧栏筛选 + 状态 Tag（在职/停用/离职） |
| 用户创建 | `pages/platform/user/components/detail.tsx` | `POST /api/v1/iam/users` | **Done** | 含账号、姓名、手机、邮箱、组织归属、角色分配 |
| 用户编辑 | `pages/platform/user/components/detail.tsx` | `PUT /api/v1/iam/users/{userId}` | **Done** | 同创建字段 |
| 用户离职 | `platform/user/index.tsx`（离职按钮） | `POST /api/v1/iam/users/{userId}/offboard` | **Done** | 需二次确认 |
| 离职池 | `pages/platform/offboard-pool/index.tsx` | `GET /api/v1/iam/users/offboard-pool` + `restore` | **Done** | 列表含域名/角色/离职时间/操作人；支持一键恢复 |
| 重置密码 | `platform/user/components/reset-password-modal.tsx` | `IamController`（间接） | **Done** | 随机生成 16 位密码，Modal 展示后复制 |
| 用户删除（永久） | — | `DELETE /api/v1/iam/users/{userId}` | **Partial** | 后端 API 就绪，前端无对应操作入口（仅离职/恢复） |
| 用户导入导出 | `pages/platform/import-export/index.tsx` | — | **Todo** | 页面存在，API 待查 |

### 4.2 角色管理

| 项 | 前端 | 后端 | 状态 | 备注 |
|:---|:---|:---|:---|:---|
| 角色列表 | `pages/system/role/index.tsx` | `GET /api/v1/iam/roles` | **Done** | 表格展示，支持 scope 筛选（platform/domain） |
| 角色创建/编辑 | `pages/system/role/components/detail.tsx` | `POST/PUT /api/v1/iam/roles` | **Done** | 含 code/name/scope/description；权限树分配（菜单+按钮 checkbox tree） |
| 角色删除 | `pages/system/role/index.tsx`（删除按钮） | `DELETE /api/v1/iam/roles/{roleId}` | **Done** | 含安全校验（禁止删除预置角色） |
| 角色权限查看 | — | `GET /api/v1/iam/roles/{roleId}/permissions` | **Done** | 后端 API 就绪 |
| 角色权限替换 | — | `PUT /api/v1/iam/roles/{roleId}/permissions` | **Done** | 后端 API 就绪 |

### 4.3 权限管理

| 项 | 前端 | 后端 | 状态 | 备注 |
|:---|:---|:---|:---|:---|
| 权限管理页面 | `pages/platform/permission/index.tsx` | — | **Partial** | 页面仅为 `<Navigate to="/platform/role" replace />`，无独立权限管理界面 |
| 权限码列表 | — | `GET /api/v1/iam/admin-permission-codes` | **Done** | 后端返回全部权限码定义，供前端 `AuthGuarded` 组件使用 |
| 资源管理 | — | `GET/POST /api/v1/iam/resources`、`PUT /api/v1/iam/resources/{id}` | **Done** | 后端 API 就绪（按 type/scope 筛选），前端无对应管理页面 |
| 角色-资源绑定 | — | `GET/PUT /api/v1/iam/roles/{roleId}/resources` | **Done** | 后端 API 就绪 |

**说明**：当前权限管理主要通过 **角色编辑页中的权限树** 完成（菜单节点 + 按钮节点），无需独立权限页面。资源（Resources）管理后端就绪但前端未暴露，S1 可评估是否需要。

### 4.4 菜单管理

| 项 | 前端 | 后端 | 状态 | 备注 |
|:---|:---|:---|:---|:---|
| 菜单树 | `pages/platform/system/menu/index.tsx` | `GET /api/v1/iam/menus/tree` | **Done** | 树形 Table 展示 + scope 筛选（platform/business 切换）；含图标选择器 + 菜单/按钮节点类型标签 |
| 菜单创建 | `platform/system/menu/components/detail.tsx` | `POST /api/v1/iam/menus` | **Done** | jsxKey/name/path/icon/parentId/nodeType/scope/visible |
| 菜单编辑 | `platform/system/menu/components/detail.tsx` | `PUT /api/v1/iam/menus/{menuId}` | **Done** | 同创建字段 |
| 菜单删除 | `platform/system/menu/index.tsx`（删除按钮） | `DELETE /api/v1/iam/menus/{menuId}` | **Done** | 二次确认后删除；自动级联删除子菜单 |

### 4.5 IAM 其他

| 项 | 前端 | 后端 | 状态 | 备注 |
|:---|:---|:---|:---|:---|
| 当前用户权限快照 | — | `GET /api/v1/iam/me/permission-snapshot` | **Done** | 返回 user + roles + domains + menus + actions |
| 当前用户菜单资源 | — | `GET /api/v1/iam/me/menu-resources` | **Done** | 前端 `isSendRoutingRequest = true` 启用后端动态路由 |
| 动态路由注册 | `router/routes/config.ts` | `IamController` / `StaffController` | **Done** | 后端菜单 → 前端动态路由 → `AuthGuarded` 按钮级权限 |
| 跨域访问拒绝 | — | `US-S1-08`（Todo，**S1 暂缓**） | **Todo** | A 域身份访问 B 域数据暂未全面拦截；2026-05-26 不纳入 S1 Committed |

**Flyway 相关**：菜单迁移 `V202605200004`～`006`（域菜单结构）、`V202605210001`（日志审计菜单）、`V202605220001`（五模块菜单精简）、`V202605220002`（菜单图标与按钮权限回填）。

---

## 5. 日志审计

### 5.1 操作日志（审计日志）

| 项 | 前端 | 后端 | 状态 | 备注 |
|:---|:---|:---|:---|:---|
| 操作日志列表 | `pages/platform/log/operation-log/index.tsx` | `GET /api/v1/admin/audit-logs` | **Done** | 可分页、支持模块/操作者/关键词/时间范围筛选 |
| 审计日志统一页 | `pages/platform/audit-logs/index.tsx`（"平台审计" Tab） | 同上 | **Done** | 含业务域/操作者/动作/结果/目标/明细/时间列；Tabs 切换审计/登录 |
| 域级审计日志 | — | `GET /api/v1/admin/domains/{domainId}/audit-logs` | **Done** | 后端 API 就绪，前端未直接暴露此入口 |

**备注**：操作日志存在两套前端页面——`platform/log/operation-log/`（独立页）与 `platform/audit-logs/`（Tabs 统一页），功能重叠。S1 建议收敛为统一页，移除 `platform/log/operation-log/`。

### 5.2 登录日志

| 项 | 前端 | 后端 | 状态 | 备注 |
|:---|:---|:---|:---|:---|
| 登录日志列表 | `pages/platform/log/login-log/index.tsx` | `GET /api/v1/admin/login-logs` | **Done** | 可分页，支持主体ID/门户类型/结果/客户端/事件类型/时间范围筛选 |
| 登录日志统一页 | `pages/platform/audit-logs/index.tsx`（"登录日志" Tab） | 同上 | **Done** | 含业务域/账号/门户/结果/IP/失败原因/登录时间列 |
| 域级登录日志 | — | `GET /api/v1/admin/domains/{domainId}/login-logs` | **Done** | 后端 API 就绪，前端未直接暴露此入口 |
| 登录日志表统一 | — | `V202605230001` + `V202605230002` | **Done** | `login_log` 表含 `client_code`/`sid`/`event_type` 统一字段 |

**备注**：同上，`platform/log/login-log/` 与 `platform/audit-logs/` 存在功能重叠，建议 S1 收敛。

### 5.3 审计能力

| 项 | 前端 | 后端 | 状态 | 备注 |
|:---|:---|:---|:---|:---|
| 操作写入 `audit_log` | — | `AuditLogService`（写入端） | **Done** | 管理操作、状态变更、权限修改均写入审计日志 |
| 审计日志不可删除 | — | `audit_log` 表结构 | **Done** | 表无 `deleted_at` 等软删除标记字段 |
| 登录日志统一写入 | — | `LoginAuditService` | **Done** | 登录事件统一写入 `login_log` |
| 审计日志导出 | — | — | **Todo** | 无导出功能 |
| 安全告警中心 | — | — | **Todo** | PRD §3.4.3 提及，未实现 |

---

## 6. 汇总与缺口

### 6.1 状态汇总

| 模块 | Done | Partial | Todo | 完成度 |
|:---|:---|:---|:---|:---|
| 首页 | 0 | 2 | 0 | 路由可达，数据 Partial（纯 demo） |
| 组织 | 1 | 0 | 1 | 50%（org-config 占位） |
| 业务域 | 9 | 4 | 0 | ~65%（双配置 CRUD Done；成员/客户/角色 Tab 仍为 Partial） |
| IAM | 17 | 3 | 2 | ~80% |
| 日志审计 | 11 | 0 | 2 | ~85% |
| **合计** | **36** | **11** | **5** | **~70%** |

### 6.2 关键缺口（Top 5）

| 排名 | 缺口 | 模块 | 状态 | S1 建议 |
|:---|:---|:---|:---|:---|
| 1 | 首页仪表盘数据为 demo 模拟 | 首页 | Partial | 接入真实聚合查询 |
| 2 | 跨域访问拒绝未全面覆盖 | IAM | Todo（S1 暂缓） | US-S1-08 |
| 3 | 域成员/客户/角色/日志/屏蔽词 Tab 未成品化 | 业务域 | Partial | **S2 US-S2-02～06** |
| 4 | 操作日志/登录日志两套前端页面冗余 | 日志审计 | Done（重复） | 收敛为统一入口 |
| 5 | 审计日志无导出功能 | 日志审计 | Todo | 评估是否需要 |
| 6 | 登录后默认跳转误落 `/system/menu`（双 scope 平台管理员） | 认证 / 登录页 | **Done** | `resolve-home-path.ts` + `platformAccess` 优先 → `/platform/home`（S1） |
| 7 | 登录滑块验证交互（按住反馈、终点松手顿挫） | 认证 / 登录页 | Partial | **US-S2-UX-01**（S2 Committed，sprint-2-plan §4） |

### 6.3 交叉引用

| 引用文档 | 路径 |
|:---|:---|
| Epic 地图 | [`backlog-epics.md`](./backlog-epics.md) |
| User Stories | [`backlog-stories.md`](./backlog-stories.md) |
| Sprint 0 计划 | [`sprint-0-plan.md`](./sprint-0-plan.md) |
| 数据库增量计划 | [`../architecture/database-increment-plan.md`](../architecture/database-increment-plan.md) |
| 数据模型 | [`../architecture/data-model.md`](../architecture/data-model.md) |

---

## 7. 业务域端（根级非 `/platform/`，Epic E2）

> **S2 Committed**：**US-S2-E2-00** 最小可达；余量 **US-S2-E2-01（Stretch）**。  
> 域内成员/客户/角色 **平台侧** 管理见 §3 + **US-S2-02～04**（非 E2 签 off 必要条件）。

| 项 | 前端线索 | 后端 | 状态 | 备注 |
|:---|:---|:---|:---|:---|
| 业务域首页/入口 | `/home` 等 | — | **Todo** | **US-S2-E2-00**；与 `/platform/home` 分离 |
| 系统用户/角色/菜单/部门 | `pages/system/user` 等 | IAM 域级 API | **Partial** | E2-00 至少 1 页非 Empty |
| 域内成员/客户/角色（business 端） | 根级模块（待扩） | 域 API | **Partial** | 平台侧见 §3 **US-S2-02～04** |
| 工单类型设计 | — | `ticket_type` | **Todo** | **US-S2-E2-01 Stretch** |
| 域 SLA / 通知模板 | — | — | **Todo** | PRD §3.3.1，S3+ |
