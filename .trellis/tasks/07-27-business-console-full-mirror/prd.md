# 业务域端完整镜像平台域控制台

## Goal

在 AdminWeb **业务域端**（根级非 `/platform/`、`iam_admin_menu.scope=business`）落地与 **平台内业务域控制台**（`/platform/domains/detail/:domainId`）功能完整对齐的域治理能力，使 `domain_admin` 无需进入平台端即可完成本域配置与人员管理。

## Background

- 平台侧域控制台（US-S2-01～06 + 事项配置）已成熟，含约 12 个 Tab 模块（`/platform/domains/detail/:domainId?tab=`）。
- 不新建独立 DomainWeb；继续复用 `UnionDeskAdminWeb` 双控制台模型。
- **2026-07-31 现状复核（本会话）**：
  - **已落地**：E0 进端契约、M0 菜单/路由/`domain.*` 种子、`/home`、事项配置主链路（含类型配置子页）。
  - **仍为 Empty 占位**：`/domain/basic|members|roles|customers|onboarding|blockwords|notifications|config|audit-logs|login-logs`（10 页）；`/domain/overview` 仅为骨架。
  - **事项配置残余缺口**：业务端表单设计器路由未独立落地（平台有 `/platform/domains/ticket/form-design/...`）。
  - 相关并行任务：`07-28-sidebar-domain-switcher`（多域切换 UI）、`07-29-business-sidebar-align`（侧栏对齐）。

## Confirmed Decisions

| 决策 | 结论 |
|:---|:---|
| 范围策略 | **C：完整镜像** — 对齐平台域控制台全部模块（含通知占位） |
| 前端落地 | **独立页面** — 各模块独立 page 文件与路由，便于后续个性化；**不**抽共享 Panel 强制双端复用 |
| 导航形态 | **侧栏多菜单项** — 纳入 `scope=business` 菜单管理（`/system/menu`）维护 |
| 进端契约 | **平台能力** → 进平台端；**域访问名单非空** → 进业务域端；进入/切域时再拉**当前域**权限（见 `design.md` §0） |
| 默认首页 | 仅平台 → `/platform/home`；仅名单或双端 → 业务域首页 |
| 执行方式 | Trellis 父任务 + 可独立验收的子任务 |
| 应用边界 | 仍在 `UnionDeskAdminWeb` 内；不做独立 DomainWeb |
| P0 批次 | **人员管理 + 通用设置 + 入域管理**（其余模块顺延） |
| P0 域上下文 | 绑定会话活跃域；**允许切域**（侧栏切换，见 `07-28`） |
| 切域后页签 | 执行切域时 MUST **清空当前标签页内容**（`resetTabs`，回退可达路由/首页） |
| 侧栏可切域列表 | 仅展示当前用户**有成员权限**且业务域**已启用**的域；禁用域不得出现在切换列表 |
| 域治理菜单可见性 | **按权限码细粒度**：有对应 `domain.*.read`（或模块约定读码）才展示该菜单项；无码则不可见。坐席默认不绑治理写码，但若被授予读码则可进只读视图 |
| 通用设置写范围 | 业务端仅 `domain.general.read` + `domain.general.update`（基础信息）；**不**种子/暴露 `domain.general.update_status` / delete；启停与删除仅平台端 |
| P0 人员写能力 | **完整写**对齐平台：`domain.member.create` / `update_roles` / `update_status` / `delete`（外加 read） |
| P0 入域能力 | **只读**：仅 `domain.invitation_code.read`（+ 策略展示只读）；create/delete/改策略后续子任务 |

## Requirements

### R0 — 双端进端与当前域权限

- 系统 MUST 用「是否具备平台能力」判定 `platformAccess`（平台角色 / 平台菜单 / `platform.*`），决定能否进入平台端。
- 系统 MUST 用「员工是否在至少一业务域成员名单中」（`accessibleDomains` 非空）判定 `businessDomainAccess`，**不得**仅因存在 business 菜单或非 `platform.*` 权限码而视为可进业务域端。
- 进入业务域端或切换当前业务域时，系统 MUST 按当前 `businessDomainId` 加载该域的 business 菜单与 `domain.*`（及约定共享码）权限包。
- 默认首页 MUST 遵循：仅平台 → 平台首页；仅域名单或双端 → 业务域首页。

### R1 — 功能镜像清单

业务域端 MUST 提供与平台域控制台对应的下列模块（独立页面）：

| 模块 | 平台对照 | 业务端差异约束 |
|:---|:---|:---|
| 概览 | `overview` | 仅本域数据；无跨域治理入口 |
| 通用设置 | `basic` | **禁止**删除业务域；**禁止**启停业务域（启停仅平台端） |
| 人员管理 | `members` | 权限码使用 `domain.member.*`；**首版完整写**：添加 / 改角色 / 启停成员 / 移除（对齐平台） |
| 角色管理 | `roles` | 权限码使用 `domain.role.*`；能力至少不低于平台只读；写能力按 PRD F3.3 另开子要求 |
| 客户管理 | `customers` | `domain.customer.*` |
| 入域管理 | `onboarding` | **P0 仅只读**：查看邀请码与注册/邀请策略；创建/删除邀请码与改策略 **不在 P0** |
| 事项配置 | `ticket_config` | 类型/属性/状态；表单/流程设计器入口可达 |
| 屏蔽词库 | `blockwords` | 本域词库 |
| 通知配置 | `notifications` | 允许与平台一致的占位页，但须有独立路由与菜单 |
| 参数配置 | `config` | 本域 KV |
| 操作日志 | `audit_logs` | 本域只读 |
| 登录日志 | `login_logs` | 本域只读 |

### R2 — 独立页面与路由

- 每个模块 MUST 有独立页面文件（如 `pages/domain/...`），不得与平台 `detail-*.tsx` 共用同一实现文件。
- 允许参考/复制平台实现作为起点，但业务端页面后续可独立演进。
- 路由 MUST 为根级非 `/platform/` 前缀；菜单 `scope=business`。

### R3 — 域上下文

- 业务端页面 MUST 绑定当前员工会话的活跃业务域（`UserContext.businessDomainId` / 登录返回的 `defaultBusinessDomainId`），不得要求用户从平台跨域列表进入。
- 多域成员 MUST 能切换当前域；切换后 MUST 重新加载该域权限包，并 MUST **清空当前打开的标签页**（避免跨域残留页签/脏状态）。
- 左侧业务域切换组件的可选列表 MUST 仅包含：当前用户有权限访问、且状态为**启用**的业务域（禁用域不可选、不展示）。
- 切域 UI 由 `07-28-sidebar-domain-switcher` 落地；P0 三页消费会话活跃域，不自建页内切域控件。

### R4 — 权限隔离

- 业务端按钮/菜单 MUST 使用 `domain.*` 权限码，不得污染纯 business 快照为 `platform.*`。
- 平台端现有 `platform.domain.control.*` 路径与权限 MUST 保持可用、行为不回归。
- 进端判定 MUST 遵循 R0，废止「用非 platform 权限码反推业务端可达」。
- 域治理侧栏菜单 MUST **按权限码细粒度显隐**：仅当用户具备该模块约定读权限（如 `domain.member.read` / `domain.general.read` / 入域对应读码）时展示对应菜单项；无读码 MUST NOT 展示入口。
- 页内写操作按钮 MUST 另绑写码（create/update/delete）；仅有读码时页面以只读形态呈现（无破坏性操作入口）。

### R5 — 非目标（Out of Scope）

- 新建 `UnionDeskDomainWeb` 或其它独立前端应用
- 跨域业务域列表/创建/删除（属平台治理）
- 全局事项字典、全局屏蔽词、离职池、平台组织 IAM
- 工单作业台 / 咨询工作台（Epic E3/E5）
- 强制抽取双端共享 Panel 组件库（本任务明确不做）
- 双端用户「记住上次所在端」偏好（可后续单独立项）

## Acceptance Criteria

- [ ] AC0：进端符合 R0（仅平台 / 仅名单 / 双端 三种路径与顶栏按钮行为正确；切域后权限随域变化、标签页清空；侧栏切域列表仅含有权限且启用的域）
- [ ] AC1：`domain_admin`（在域名单内）登录后侧栏可见业务域治理菜单组，且不含 `/platform/domains` 等平台模块
- [ ] AC2：R1 清单中每个模块均有独立路由页面可打开（通知可为占位 Empty，但不可缺路由）
- [ ] AC3：人员/客户/入域/事项配置/屏蔽词/参数/日志的主流程与平台域控制台行为一致（本域数据、中文提示、权限显隐）
- [ ] AC4：通用设置可编辑本域基础信息，但无删除域入口、无启停域入口（启停仅平台）
- [ ] AC5：业务域端权限快照 actions 使用 `domain.*`，不含因本功能引入的 `platform.domain.control.*` / `platform.*` 噪声
- [ ] AC6：平台 `/platform/domains/detail/:domainId` 既有功能无回归
- [ ] AC7：父任务下各子任务均可独立验收；父任务最终做一次端到端集成核对

## Open Questions

（无阻塞 P0 的开放问题。）

| 延后项 | 默认倾向 | 何时定 |
|:---|:---|:---|
| 域角色页写能力 | 设计默认开放 create/update | `/domain/roles` 子任务开干前 |
| 入域写能力 | 对齐平台邀请码 CRUD + 策略更新 | P0 只读验收后的入域增强子任务 |

> 已确认：导航/进端/首页、P0 三页范围、切域清页签、侧栏仅启用域、菜单按读码细粒度、通用设置仅基础信息、人员完整写、入域 P0 只读（方案 B）。
> 切域交互以 `07-28` 为准。

## Next Child Task

| 子任务 | 状态 | 交付 |
|:---|:---|:---|
| `07-31-business-console-p0-members-basic-onboarding` | in_progress / 已实现 | 人员完整写 + 通用仅基础信息 + 入域只读 |
| `07-31-business-console-p1-customers-blockwords-config` | in_progress / 已实现 | 客户完整写 + 屏蔽词 CRUD + 参数配置 |
| `07-31-business-console-p2-logs-roles` | in_progress | 操作/登录日志只读 + 域角色读写 |

## Technical Notes

- 后端优先复用 `/api/v1/admin/domains/{domainId}/…` 既有 Controller/Service；鉴权补充/对齐 `domain.*`。
- 前端复制平台面板为独立实现时，须替换权限码与 `domainId` 来源（会话活跃域，非 path 参数为主）。
- 进端改造涉及：`hasBusinessDomainAccess`、`resolveHomePath*`、`PlatformEntryButton`、permission-snapshot 按域加载；登录已返回 `accessibleDomains` 可复用。
- 复杂任务：本父任务需 `design.md` + `implement.md`；实现落在子任务。

## Child Task Map（规划中）

| 建议子任务 | 交付 | 对应 AC |
|:---|:---|:---|
| 进端契约：名单判定 + 按域 snapshot + 默认首页 | 改造 access / auth-guard / 顶栏 | AC0 |
| 基建：路由/菜单/`domain.*` 权限种子 | 可导航空壳 + 菜单管理可见 | AC1, AC5 |
| 概览 + 通用 + 入域 + 参数 | 4 页 | AC2–4 |
| 人员 + 客户 + 角色 | 3 页 | AC2–3 |
| 事项配置（含设计器入口） | 1 页 + 子路由 | AC2–3 |
| 屏蔽词 + 双日志 | 3 页 | AC2–3 |
| 通知占位 | 1 页 | AC2 |
| 父任务集成验收 | 回归平台 + E2E | AC6–7 |
