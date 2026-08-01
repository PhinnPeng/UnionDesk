# 业务域端菜单 / 功能 / 按钮权限设计

> 父任务：`07-27-business-console-full-mirror`  
> 依据：平台域控制台 US-S2-01～06、E2-00、PRD §5.3、PermissionCodes / AdminPermissionCatalog、拍板 C（完整镜像）+ 独立页面  
> 进端契约：平台能力 + 域访问名单（2026-07-27 确认）

---

## 0. 双端进端契约（已确认）

### 0.1 目标模型

进端**不再**用「是否有某类权限码 / 是否有 business 菜单」互推；改为两个独立开关：

```text
platformCapable?     ──是──► 可进入平台端（加载平台全局权限 / platform 菜单）
        │
domainMembership?    ──是──► 可进入业务域端
        │                    └─ 选定或默认当前业务域
        │                         └─ 按当前域拉取 domain.* 权限 + business 菜单
        └─ 否 ──► 无业务域端入口（不可「返回业务端」）
```

| 开关 | 判定依据 | 进入后权限来源 |
|:---|:---|:---|
| **平台能力** `platformAccess` | 具备平台角色（`super_admin` / `platform_admin`），或平台菜单，或任意 `platform.*` 权限 | 平台全局 snapshot（`scope=platform` 菜单 + `platform.*` actions） |
| **域访问名单** `businessDomainAccess` | 员工在至少一个人业务域的**成员名单**中（`accessibleDomains.length > 0`，后端已有登录字段） | **进入业务域端时 / 切换当前域时**，按 `businessDomainId` 拉取该域权限包 |

### 0.2 与现状差异（改造点）

| 项 | 现状 | 目标 |
|:---|:---|:---|
| `platformAccess` | 角色 / platform 菜单 / `platform.*` 任一 | **保持**该语义（「是否具备平台能力」） |
| `businessDomainAccess` | 快照中是否存在 `scope=business` 菜单 | 改为 **域访问名单非空**（成员关系），**不**用菜单推断 |
| 默认首页 | actions 三元规则（仅 platform.* → 平台；否则业务） | 见 §0.3 |
| 业务权限加载 | 登录一次合并 snapshot | **进业务域端或切域时**再取当前域权限；平台端不混入当前域 `domain.*` 噪声（或分 scope 持有两套 snapshot） |

### 0.3 默认首页规则（确认）

在「平台能力 / 域名单」二维下：

| platformAccess | businessDomainAccess | 默认首页 |
|:---|:---|:---|
| true | false | `/platform/home` |
| false | true | 业务域首页（如 `/home`） |
| true | true | **业务域首页**（维持 E2-00「双端优先进业务」） |
| false | false | 无可用控制台 → 友好无权限页 / 登出提示（不应正常登录成功） |

> 废止「仅靠 actions 里是否有非 `platform.*` 码」作为进业务端门槛；业务端门槛以**名单**为准。  
> 若双端用户希望默认进平台，可作为后续偏好项，本任务不实现。

### 0.4 进入业务域端时的权限加载

1. 解析当前域：`session.businessDomainId` / `defaultBusinessDomainId`；多域时允许切换（UI 在子任务实现，契约预留）。  
2. 请求权限快照须绑定域上下文，例如：  
   - `GET /api/v1/iam/me/permission-snapshot?domainId={id}`，或  
   - 先 `POST/PUT` 切换会话活跃域，再拉无参 snapshot。  
3. 返回内容：  
   - `menuTree`：仅 `scope=business`（按该域角色授权）  
   - `actions`：该域 `domain.*` / 共享作业码（如 `ticket.*`），**不得**因进业务端而塞入 `platform.*`  
4. 前端 `userStore`：  
   - `platformAccess` / `businessDomainAccess` 按 §0.1  
   - `actions` / `menus` 在**业务端上下文**下为当前域包；切到平台端时使用平台包（可内存分持 `platformSnapshot` / `businessSnapshot`）。

### 0.5 顶栏切换

| 所在端 | 按钮 | 条件 |
|:---|:---|:---|
| 业务域端 | 「平台管理」 | `platformAccess === true` |
| 平台端 | 「返回业务端」 | `businessDomainAccess === true`（名单非空） |

纯平台用户不显示「返回业务端」；纯域成员不显示「平台管理」。

### 0.6 与菜单 / 按钮权限设计的关系

- §2～§4 的 business 菜单与 `domain.*` 按钮，仍是**进入业务域端之后**的授权内容。  
- 没有域名单 → 根本进不了业务端，与是否种子了「域治理」菜单无关。  
- 有名单但某域角色未授某按钮 → 进得了端，侧栏/按钮按当前域权限显隐。

### 0.7 验收要点（进端）

- [ ] 仅平台能力、无域成员：登录进 `/platform/home`；无「返回业务端」  
- [ ] 仅域成员、无平台能力：登录进业务首页；无「平台管理」  
- [ ] 双具备：登录进业务首页；两端入口均可切换  
- [ ] 切域后菜单/按钮随新域权限变化；请求携带新 `businessDomainId`  
- [ ] 业务端 snapshot actions 无 `platform.*` 噪声

---

## 1. 现状盘点

### 1.1 业务域端已有（E2-00 + 手工脚本）

| 菜单/能力 | 路由 | scope | 按钮权限 | 状态 |
|:---|:---|:---|:---|:---|
| 工作台 / 首页概览 | `/home` | business | `domain.home.read` | 手工脚本 `business-home-menu.sql`；Flyway 未必全量 |
| 菜单管理 | `/system/menu`（历史 `/system/menus`） | business | `domain.menu.read/create/update/delete` | Done（E2-00） |
| 角色管理（系统） | `/system/role` | business | `domain.role.*` + `domain.role.permission.*` | Done（E2-00） |
| 用户 / 部门 | `/system/user`、`/system/dept` | business | 占位 | Partial |

约束（OpenSpec `business-console-minimum-reach`）：

- business 按钮 MUST 用 `domain.*`，不得引入 `platform.*`
- `domain_admin` 不得绑定 platform scope 菜单

### 1.2 平台内业务域控制台（对照源）

路由：`/platform/domains/detail/:domainId?tab=…`  
权限前缀：`platform.domain.control.*` / `platform.domain.roles.*`

| Tab | 读权限（侧栏） | 写/按钮（摘要） |
|:---|:---|:---|
| 概览 | `…overview` | — |
| 通用 | entry + general.update/update-status | **含 delete（业务端禁止）** |
| 人员 | `…member.read` | create / update_roles / update_status / delete |
| 角色 | `platform.domain.roles.read` | permissions.read（平台只读） |
| 客户 | `…customer.read` | create / update-status |
| 入域 | （域更新 + invitation） | `domain.invitation_code.*` 已存在 |
| 事项配置 | ticket_type/attribute/status.read | 对应 CRUD |
| 屏蔽词 | `…blocked_word.read` | create / delete |
| 通知 | 无门控（占位） | 预留 `domain.notification_template.*` |
| 参数 | `domain.config.read` | `domain.config.update` |
| 操作/登录日志 | `…audit_log/login_log.read` | 只读 |

### 1.3 后端已具备、业务端尚未挂菜单的 `domain.*`

已在 `PermissionCodes` / Catalog 中、可直接挂 business 按钮：

- `domain.member.*`、`domain.role.*`、`domain.role.permission.*`
- `domain.invitation_code.*`、`domain.config.*`
- `domain.ticket_type.*`、`domain.ticket_template.*`
- `domain.sla.*`、`domain.notification_template.*`
- `domain.blocked_word.*`（labels 有；需确认 Catalog/Flyway 是否齐全）

**缺口（需新增 Catalog + iam_permission + 菜单按钮）**：

| 缺口码 | 用途 | 对应平台码 |
|:---|:---|:---|
| `domain.overview.read` | 概览页 | `platform.domain.control.overview` |
| `domain.general.read` / `domain.general.update` | 通用设置基础信息（**无 update_status、无 delete**；启停/删除仅平台） | `…general.read/update` |
| `domain.customer.read/create/update_status` | 客户管理 | `…customer.*` |
| `domain.ticket_attribute.*` CRUD | 事项属性 | `…ticket_attribute.*` |
| `domain.ticket_status.*` CRUD | 事项状态 | `…ticket_status.*` |
| `domain.audit_log.read` | 本域操作日志 | `…audit_log.read` |
| `domain.login_log.read` | 本域登录日志 | `…login_log.read` |

API 层：多数 Controller 已同时支持或可扩展为 `domain.*` / `platform.domain.control.*` 双码鉴权；新增码时在 `@RequirePermission` 中与平台码并列或统一走 Service 内鉴权。

---

## 2. 目标信息架构（侧栏菜单，纳入菜单管理）

导航形态定为 **侧栏多菜单项 + 独立路由**（便于在「业务域端 → 菜单管理」中维护，呼应独立页面个性化）。

```text
业务域端侧栏（scope=business）
├── 工作台                          catalog  BUSINESS-HOME-CATALOG
│   └── 首页概览                    /home
├── 域治理                          catalog  BUSINESS-DOMAIN-GOVERN-CATALOG   ★ 新增
│   ├── 概览                        /domain/overview
│   ├── 通用设置                    /domain/basic
│   ├── 人员管理                    /domain/members
│   ├── 角色管理                    /domain/roles          （域角色，非 /system/role）
│   ├── 客户管理                    /domain/customers
│   ├── 入域管理                    /domain/onboarding
│   ├── 事项配置                    /domain/ticket-config
│   ├── 屏蔽词库                    /domain/blockwords
│   ├── 通知配置                    /domain/notifications  （可先占位）
│   ├── 参数配置                    /domain/config
│   ├── 操作日志                    /domain/audit-logs
│   └── 登录日志                    /domain/login-logs
└── 系统管理                        catalog  （现有）
    ├── 菜单管理                    /system/menu
    ├── 角色管理                    /system/role           （IAM 系统角色配置）
    ├── 用户管理                    /system/user           （后续成品化）
    └── 部门管理                    /system/dept           （后续成品化）
```

说明：

- **域治理 / 角色管理**（`/domain/roles`）= 本域业务角色与权限勾选（PRD F3.3）。  
- **系统管理 / 角色管理**（`/system/role`）= 现有 E2-00 IAM 角色页，保留不删。  
- 业务端 **不做**：删域、**启停域**、跨域列表、全局字典、平台 IAM。（启停域仅平台 `/platform/domains/detail` 通用设置）

---

## 3. 菜单节点与按钮权限矩阵（可导入菜单管理）

约定：

- `menu` 节点：`permission_code` 可空；访问靠子按钮 `*.read` + 路由守卫。  
- `button` 节点：`permission_code` 必填，供 `AuthGuarded` / 角色勾选。  
- `code` 种子前缀：`BUSINESS-DOMAIN-*`。

### 3.1 域治理 Catalog

| code | 类型 | 名称 | route / 按钮码 |
|:---|:---|:---|:---|
| `BUSINESS-DOMAIN-GOVERN-CATALOG` | catalog | 域治理 | — |

### 3.2 各功能页

#### 概览 `/domain/overview`

| code | 类型 | 名称 | permission_code |
|:---|:---|:---|:---|
| `BUSINESS-DOMAIN-OVERVIEW` | menu | 概览 | — |
| `BUSINESS-DOMAIN-OVERVIEW-READ` | button | 查看概览 | `domain.overview.read` |

#### 通用设置 `/domain/basic`

| code | 类型 | 名称 | permission_code |
|:---|:---|:---|:---|
| `BUSINESS-DOMAIN-BASIC` | menu | 通用设置 | — |
| `BUSINESS-DOMAIN-BASIC-READ` | button | 查看通用设置 | `domain.general.read` |
| `BUSINESS-DOMAIN-BASIC-UPDATE` | button | 更新基础信息 | `domain.general.update` |

> **禁止**业务端种子/暴露：`domain.general.delete`、`domain.general.update_status` 及对应删除/启停按钮（启停与删除仅平台域控制台）。

#### 人员管理 `/domain/members`

| code | 类型 | 名称 | permission_code |
|:---|:---|:---|:---|
| `BUSINESS-DOMAIN-MEMBERS` | menu | 人员管理 | — |
| `…-READ` | button | 查看成员 | `domain.member.read` |
| `…-CREATE` | button | 添加成员 | `domain.member.create` |
| `…-UPDATE-ROLES` | button | 编辑成员角色 | `domain.member.update_roles` |
| `…-UPDATE-STATUS` | button | 启停成员 | `domain.member.update_status` |
| `…-DELETE` | button | 移除成员 | `domain.member.delete` |

#### 角色管理 `/domain/roles`

| code | 类型 | 名称 | permission_code |
|:---|:---|:---|:---|
| `BUSINESS-DOMAIN-ROLES` | menu | 角色管理 | — |
| `…-READ` | button | 查看域角色 | `domain.role.read` |
| `…-CREATE` | button | 创建域角色 | `domain.role.create` |
| `…-UPDATE` | button | 编辑域角色 | `domain.role.update` |
| `…-DELETE` | button | 删除域角色 | `domain.role.delete` |
| `…-PERM-READ` | button | 查看角色权限 | `domain.role.permission.read` |
| `…-PERM-UPDATE` | button | 编辑角色权限 | `domain.role.permission.update` |

> 镜像阶段默认开放写能力（对齐 PRD F3.3）；首版可只绑 read 给非 admin 角色。

#### 客户管理 `/domain/customers`

| code | 类型 | 名称 | permission_code |
|:---|:---|:---|:---|
| `BUSINESS-DOMAIN-CUSTOMERS` | menu | 客户管理 | — |
| `…-READ` | button | 查看客户 | `domain.customer.read` |
| `…-CREATE` | button | 添加客户 | `domain.customer.create` |
| `…-UPDATE-STATUS` | button | 启停客户 | `domain.customer.update_status` |

#### 入域管理 `/domain/onboarding`

| code | 类型 | 名称 | permission_code |
|:---|:---|:---|:---|
| `BUSINESS-DOMAIN-ONBOARDING` | menu | 入域管理 | — |
| `…-READ` | button | 查看入域配置 | `domain.invitation_code.read` |
| `…-CREATE` | button | 创建邀请码 | `domain.invitation_code.create` |
| `…-DELETE` | button | 删除邀请码 | `domain.invitation_code.delete` |
| `…-POLICY` | button | 更新注册/邀请策略 | `domain.general.update` |

> **P0 批次**：页面只实现只读视图（依赖 `…-READ`）；CREATE/DELETE/POLICY 按钮可保留在菜单种子供角色勾选，但 **P0 UI 不暴露写入口**；写能力另开增强子任务。

#### 事项配置 `/domain/ticket-config`

| code | 类型 | 名称 | permission_code |
|:---|:---|:---|:---|
| `BUSINESS-DOMAIN-TICKET-CONFIG` | menu | 事项配置 | — |
| 类型 CRUD 四按钮 | button | … | `domain.ticket_type.read/create/update/delete` |
| 属性 CRUD 四按钮 | button | … | `domain.ticket_attribute.*`（新增） |
| 状态 CRUD 四按钮 | button | … | `domain.ticket_status.*`（新增） |

表单/流程设计器入口走同一套 type update 权限。

#### 屏蔽词 `/domain/blockwords`

| 按钮 | permission_code |
|:---|:---|
| 读/增/删 | `domain.blocked_word.read/create/delete` |

#### 通知配置 `/domain/notifications`

| 按钮 | permission_code |
|:---|:---|
| 读/改 | `domain.notification_template.read/update` |

首版页面可 Empty 占位，菜单与按钮仍入库，便于菜单管理可见。

#### 参数配置 `/domain/config`

| 按钮 | permission_code |
|:---|:---|
| 读/改 | `domain.config.read/update` |

#### 操作日志 / 登录日志

| 菜单 | 按钮码 |
|:---|:---|
| `/domain/audit-logs` | `domain.audit_log.read` |
| `/domain/login-logs` | `domain.login_log.read` |

---

## 4. 角色默认授权建议

| 角色 | 域治理 | 系统管理 |
|:---|:---|:---|
| `domain_admin` | 域治理全部 menu+button（无删域） | 菜单/角色 CRUD |
| `agent` | 仅概览只读（可选）+ 作业台相关（E3，本任务不绑） | 无菜单管理写权限 |
| 自定义域角色 | 由 domain_admin 在「域角色」中勾选 `domain.*` 按钮 | 一般不授菜单管理 |

绑定方式：

1. `iam_admin_role_menu_relation`：菜单树可见性  
2. `iam_role_permission`：API 鉴权兜底（与 E2-00 对 `domain.menu.*` 的做法一致）

**禁止**：向 `platform_admin` / global 角色绑定 `domain.*` 业务按钮（进端已改名单判定，仍避免平台角色快照混入业务码，见 `admin-platform-home-data-fix.sql`）。

---

## 5. 与「业务域端菜单管理」的关系

`/system/menu`（business scope）已可维护 `scope=business` 的 catalog/menu/button。

本方案落地后：

1. **Flyway 种子**写入上表节点 → 菜单管理树中立即可见、可改名/调序/启停。  
2. **权限码选择器**（`permission-code-labels.ts` + `/iam/admin-permission-codes`）需包含全部新增 `domain.*`。  
3. 后续个性化（隐藏某页、改图标）优先走菜单管理，而不是改代码路由表（静态路由仍需注册 component）。

前端静态路由仍须注册 `/domain/*`（与现有 `/system/*` 一样：菜单驱动可见性，路由表保证懒加载组件）。

---

## 6. 实现分期（挂到菜单管理的交付顺序）

| 阶段 | 交付 | 依赖 |
|:---|:---|:---|
| **E0** | 进端契约：名单判定、按域 snapshot、默认首页与顶栏 | 复用登录 `accessibleDomains` |
| **M0** | Catalog「域治理」+ 全部 menu/button（可先 hidden=1）+ 缺口权限码 | 可与 E0 并行 |
| M1 | 人员/客户/角色 解 hidden，绑 domain_admin | 独立页面落地 |
| M2 | 概览/通用/入域/参数 | 同上 |
| M3 | 事项配置 + 设计器入口 | 复用域 API |
| M4 | 屏蔽词 + 双日志 + 通知占位 | 日志 API 双码鉴权 |

推荐 **E0 + M0** 优先：先进端正确，再让「菜单管理」出现完整「域治理」树。

---

## 7. 兼容与风险

| 风险 | 缓解 |
|:---|:---|
| 双码并存（platform vs domain） | Service 层 `hasAny(platformCode, domainCode)`；业务页只认 domain |
| global 角色误绑 domain.* | Flyway 断言 + 文档禁止；进端以名单为准后仍避免 snapshot 噪声 |
| 进端改造与旧三元规则并存期 | 一次性改 `hasBusinessDomainAccess` + `resolveHomePath*`；加 AC0 回归用例 |
| `/domain/roles` vs `/system/role` 混淆 | 菜单命名区分「域角色」/「系统角色」；PRD 说明 |
| 缺码导致 403 | M0 同步补 Catalog、iam_permission、Controller `@RequirePermission` |
| 按域 snapshot 接口缺失 | 扩展 permission-snapshot 支持 `domainId`，或先切会话域再拉包 |

---

## 8. 待产品确认（不阻塞 E0 / M0）

1. 多域切换 UI 形态（顶栏切换器等；**须可切换**已写入契约）。  
2. `agent` 是否可见「域治理」只读子集。  
3. 通知/SLA 是否在本 Epic 解隐（默认通知占位、SLA 仍 Todo）。
