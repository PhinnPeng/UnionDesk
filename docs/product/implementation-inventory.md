# 管理端实现盘点 — `implementation-inventory.md`

| 文档版本 | 日期 | 说明 |
|:---|:---|:---|
| 1.0 | 2026-05-24 | Sprint 0：覆盖首页、组织、业务域、IAM、日志审计 5 大模块 |

> 状态说明：**Done** = 前后端均可正常使用（含基本校验与权限）；**Partial** = 核心路径可用但缺边界 case / 依赖 demo 数据 / 缺独立页面；**Todo** = 未实现或为占位。

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
| 业务域列表 | `pages/platform/domains/index.tsx` | `DomainController` | **Done** | 卡片展示 + 分页、关键词搜索、创建日期筛选；新建/编辑抽屉；删除需 step-up 二次认证 |
| 业务域创建 | `pages/platform/domains/components/domain-create-drawer.tsx` | `POST /api/v1/admin/domains` | **Partial** | 含 code/name/description/visibility_policy；当前 UI/API 仍使用 `registration_policy` 单下拉，非 `registration_enabled` / `invitation_enabled` 双开关（见 US-S1-03） |
| 业务域编辑 | `pages/platform/domains/components/domain-edit-drawer.tsx` | `PUT /api/v1/admin/domains/{id}` | **Partial** | 同创建，`registration_policy` 单字段 |
| 业务域详情 | `pages/platform/domains/detail/index.tsx` | `GET /api/v1/admin/domains/{id}` | **Done** | 含 basic-info-tab + config-tab（KV 配置） + overview-tab 三个 Tab |
| 业务域配置 | `pages/platform/domain-config/config-panel.tsx` | `DomainConfigController` | **Done** | KV 键值对配置表单（key/value/valueType/description） |
| 业务域删除 | `pages/platform/domains/index.tsx`（删除按钮） | `DELETE /api/v1/admin/domains/{id}` | **Done** | 需 step-up 二次认证 |
| 客户入域（邀请码） | `pages/platform/domain-onboarding/` | `InvitationCodeController` | **Partial** | 前端存在 onboarding 面板，后端提供邀请码 CRUD，但完整入域流程（客户注册 → 入域 → 角色绑定）需客户端配合（见 US-S1-05） |
| 域成员管理 | — | `DomainMemberController` | **Partial** | 后端 API 就绪（CRUD），前端尚未在详情 Tab 中提供独立成员管理 UI |
| 域客户管理 | — | `DomainCustomerController` | **Partial** | 后端 API 就绪，前端尚未独立页面 |
| 域角色管理 | — | `DomainRoleController` | **Partial** | 后端 API 就绪，前端在域详情中无独立角色分配入口 |
| 入域双配置迁移 | — | `V202605250001` | **Partial** | 迁移脚本存在（ADD `registration_enabled`/`invitation_enabled` + DROP `registration_policy`），但迁移可能仅在联调库执行，HEAD 代码 `DomainDtos` 仍使用 `registration_policy` 字段 |

**Flyway 相关**：`V202605200002`（base 建表）、`V202605200003`（audit 字段）、`V202605240001`（description）、`V202605250001`（access policy，⚠️ HEAD 代码 `DomainDtos` 未对齐，见上）。

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
| 跨域访问拒绝 | — | `US-S1-08`（Todo） | **Todo** | A 域身份访问 B 域数据暂未全面拦截 |

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
| 业务域 | 7 | 6 | 0 | ~55%（创建/编辑/迁移仍为 `registration_policy`） |
| IAM | 17 | 3 | 2 | ~80% |
| 日志审计 | 11 | 0 | 2 | ~85% |
| **合计** | **36** | **11** | **5** | **~70%** |

### 6.2 关键缺口（Top 5）

| 排名 | 缺口 | 模块 | 状态 | S1 建议 |
|:---|:---|:---|:---|:---|
| 1 | 首页仪表盘数据为 demo 模拟 | 首页 | Partial | 接入真实聚合查询 |
| 2 | 跨域访问拒绝未全面覆盖 | IAM | Todo | US-S1-08 |
| 3 | 域成员/客户/角色管理前端缺独立 UI | 业务域 | Partial | 评估加入域详情 Tab |
| 4 | 操作日志/登录日志两套前端页面冗余 | 日志审计 | Done（重复） | 收敛为统一入口 |
| 5 | 审计日志无导出功能 | 日志审计 | Todo | 评估是否需要 |

### 6.3 交叉引用

| 引用文档 | 路径 |
|:---|:---|
| Epic 地图 | [`backlog-epics.md`](./backlog-epics.md) |
| User Stories | [`backlog-stories.md`](./backlog-stories.md) |
| Sprint 0 计划 | [`sprint-0-plan.md`](./sprint-0-plan.md) |
| 数据库增量计划 | [`../architecture/database-increment-plan.md`](../architecture/database-increment-plan.md) |
| 数据模型 | [`../architecture/data-model.md`](../architecture/data-model.md) |
