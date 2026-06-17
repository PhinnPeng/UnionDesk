# Comet Design Handoff

- Change: iam-role-console-binding-alignment
- Phase: design
- Mode: compact
- Context hash: 5f56a33d64e3325d8650f2aeaa931a7723fd1b1854a18a347ec816642ed52964

Generated-by: comet-handoff.sh

OpenSpec remains the canonical capability spec. This handoff is a deterministic, source-traceable context pack, not an agent-authored summary.

## openspec/changes/iam-role-console-binding-alignment/proposal.md

- Source: openspec/changes/iam-role-console-binding-alignment/proposal.md
- Lines: 1-44
- SHA256: e66f14455ceb6f7f46dae54e884d2651ab30fb7f70ae6cb4a18c1ed61fbf5df3

```md
## Why

PRD §4.1.3 定义 **「平台角色 + 业务域角色」双层权限**，且平台管理员与域管理员分别在不同控制台工作。当前实现存在三类偏差：

1. **权限包跨界**：全局 `super_admin`、`platform_admin` 的 `iam_role_permission` 含 `domain.user.*`、`ticket.*` 等非 platform 前缀码；全局 `super_admin` 还绑定 business scope 菜单。导致权限快照 actions 同时含 `platform.*` 与非 `platform.*`，按 E2-00 三元规则默认进 `/home` 而非 `/platform/home`。
2. **绑定语义混乱**：`super_admin` 一名两用（`role.scope=global` vs `domain_role.preset`「业务域所有人」）；bootstrap `admin` 绑全局 super_admin 而非 `platform_admin`；遗留 `staff_account_platform_role` 与 `user_global_role` 双轨。
3. **E2-00 只收敛了 `domain_admin`**：platform 侧角色与种子账号未按「控制台边界」对齐，产品规则「管理平台角色绑定管理平台、业务域角色绑定业务域」尚未在数据与校验层落地。

## What Changes

- **角色—控制台绑定规则（硬约束）**
  - `role.scope = global`（平台角色）：仅允许 `iam_admin_menu.scope = platform` 菜单绑定 + `permission_scope = platform` 的 `iam_role_permission` 直授（permission code 统一 `platform.*` 前缀）。
  - `role.scope = domain`（业务域角色）：仅允许 `scope = business` 菜单 + 非 platform 前缀业务码（`domain.*`、`ticket.*` 等）；不得含 `platform.*` 直授或 platform 菜单绑定。
- **种子与 bootstrap 账号**
  - `admin` 默认绑定 **`platform_admin`**（非全局 super_admin）。
  - 清理 `platform_admin` / `platform_admin` 类角色的 non-platform 直授；全局 `super_admin` 降级为 break-glass：仅保留 platform 控制台能力或 UI 不可分配。
- **运行时校验**
  - 保存角色菜单/权限时校验 scope 一致；`mergeRolePermissionActions` / 快照过滤按 **角色 scope + permission_scope** 双重过滤，不仅靠 activeMenuScope。
- **首页与入口**
  - 在权限包收敛后，`platform_admin` 快照仅含 `platform.*` → 默认 `/platform/home`；`domain_admin` 仅 business → `/home`。
  - 双控制台用户通过 **显式组合绑定**（`platform_admin` + `domain_admin@domainId`）实现，默认仍进业务首页（维持 E2-00 spec）。
- **命名与文档**
  - UI/文档区分「系统超级管理员（global）」与「业务域所有人（domain_role）」；登记 `database-increment-plan.md`。

## Capabilities

### New Capabilities

- `iam-role-console-binding` — 平台/业务域角色与控制台、权限包、绑定表一致

## Impact

- Flyway 增量（`iam_role_permission`、`iam_admin_role_menu_relation`、`user_global_role` bootstrap）
- `UnionDesk/uniondesk-iam/.../IamService.java`、`AdminMenuService.java`（快照合并与保存校验）
- `UnionDesk/uniondesk-iam/.../PlatformRoleService.java`（与 `user_global_role` 收敛评估）
- `UnionDeskWeb/.../resolve-home-path.ts`、相关单测（收敛后回归）
- `docs/product/prd.md` 角色表、`docs/architecture/database-increment-plan.md`

## Non-Goals

- 全量删除 `staff_account_platform_role` / `domain_member_role` 表（本 change 仅对齐 IAM 主路径；域成员表收敛另开 change）
- 重命名全部 permission code（如 `domain.admin.read` → `platform.domain.admin.read`）— 仅在新码场景遵循前缀规则，历史码列入 follow-up
- BusinessWeb 独立应用、自定义域角色 UI 完整重构
- 修改 E2-00「双具备 → 业务首页」三元规则（除非用户后续单独立项）
```

## openspec/changes/iam-role-console-binding-alignment/design.md

- Source: openspec/changes/iam-role-console-binding-alignment/design.md
- Lines: 1-226
- SHA256: 718c9b7f959f6c7ecfd73b814949da0595a948b1cb47bc9c160a1b88b7f30540

[TRUNCATED]

```md
# IAM 角色—控制台绑定对齐 — 高层设计

## Context

```
PRD 意图                         当前实现缺口
────────────────────────────────────────────────────────────
platform_admin → 平台控制台      platform_admin 含 domain.user.*
domain_admin   → 业务域控制台    E2-00 已收敛 ✓
super_admin    → 域「所有人」     另有 global super_admin 全量包
admin 账号     → 平台管理员       绑 global super_admin
```

**已有能力（不重复建设）：**

- `role.scope`：`global` | `domain`（`IamService.replaceUserRoleBindings` 已分写 `user_global_role` / `user_domain_role`）
- `iam_admin_menu.scope`：`platform` | `business`
- `iam_permission.permission_scope`：`platform` | `domain`
- E2-00：`domain_admin` 移除 platform 菜单/直授；快照 actions 按 `activeMenuScope` 过滤；前端 actions 三元首页规则

**本 change 补齐：** 让 **platform 侧角色** 与 **bootstrap 数据** 同样满足边界；并在 **写入路径** 防止再次跨界。

---

## Goals

1. 任一 `global` 角色快照 actions **不含** non-platform 前缀码（除非产品明确的双栖组合来自 **两个角色** 而非一个角色包）。
2. 任一 `domain` 角色快照 actions **不含** `platform.*`。
3. `platform_admin` 登录默认 `/platform/home`；`domain_admin` 默认 `/home`。
4. 角色保存/授权 UI 无法把 platform 菜单勾到 domain 角色（反之亦然）。

---

## 角色模型（定稿）

### 标准系统角色

| code | scope | 绑定表 | 菜单 scope | 权限包 | 默认首页 | 产品名 |
|:---|:---|:---|:---|:---|:---|:---|
| `platform_admin` | global | `user_global_role` | platform | 仅 `platform.*` + `permission_scope=platform` | `/platform/home` | 平台管理员 |
| `security_auditor` | global | `user_global_role` | platform（子集） | 审计/日志类 platform 码 | `/platform/home` | 安全审计员 |
| `super_admin` | **global** | `user_global_role` | platform（全量或 break-glass 子集） | **仅 platform 包**；不再含 ticket/domain.user 等业务直授 | `/platform/home` | **系统超级管理员**（break-glass） |
| `domain_admin` | domain | `user_domain_role` + domainId | business | `domain.*` / 业务 ticket 等 | `/home` | 业务域管理员 |
| `agent` | domain | 同上 | business | 工单/咨询子集 | `/home` | 客服专员 |
| `super_admin` | **domain** (`domain_role`) | `domain_member_role` | N/A（域内 RBAC） | 域内 permission_item 全量 | `/home` | **业务域所有人** |

> **命名策略（本 change）**：不改表字段 code；通过 **UI 展示名 + 文档** 区分两个 super_admin。中长期可 rename global → `system_root`（Non-Goal 后续 change）。

### 双控制台用户（显式组合）

```
user_global_role:    platform_admin
user_domain_role:    domain_admin @ domain-A
```

| 维度 | 行为 |
|:---|:---|
| 快照 actions | platform.* + domain.*（来自两角色合并） |
| platformAccess | true |
| 默认首页 | `/home`（维持 E2-00 三元规则） |
| 平台工作 | 顶栏平台入口 |

**禁止**：单个 `global super_admin` 角色内嵌全部 business 直授来模拟双控制台。

---

## 架构决策

### D1. 绑定规则 = 三层一致

保存/种子/快照三处统一：

```
role.scope  ↔  iam_admin_menu.scope  ↔  permission.permission_scope + code 前缀
─────────────────────────────────────────────────────────────────────────────
global      ↔  platform              ↔  platform + platform.*
domain      ↔  business              ↔  domain + 非 platform.* 业务码
```

**校验点：**
```

Full source: openspec/changes/iam-role-console-binding-alignment/design.md

## openspec/changes/iam-role-console-binding-alignment/tasks.md

- Source: openspec/changes/iam-role-console-binding-alignment/tasks.md
- Lines: 1-31
- SHA256: f9b3e382df7635647fb420b7b88a8d867d3fc6c32620fce971723f6e1b28b661

```md
## 1. 现状审计与 Flyway（数据收敛）

- [ ] 1.1 脚本审计：列出 `platform_admin`、`super_admin`(global)、`agent` 的跨界 `iam_role_permission` / `iam_admin_role_menu_relation`（含 permission id 20–23 等）
- [ ] 1.2 Flyway：移除 global 角色的 `permission_scope=domain` 及 non-`platform.*` 直授；移除 global 角色对 `scope=business` 菜单绑定
- [ ] 1.3 Flyway：`admin` 改绑 `platform_admin`；删除 admin 上 global `super_admin`；清理冗余 `staff_account_platform_role`
- [ ] 1.4 Flyway：确认 `platform_admin` 具备 trim 后 platform 菜单树所需 relation；`domain_admin` 保持 E2-00 收敛状态
- [ ] 1.5 登记 `docs/architecture/database-increment-plan.md`

## 2. 后端校验与快照（写入 + 读取）

- [ ] 2.1 `AdminMenuService` / 角色授权保存：校验 menu.scope 与 role.scope 一致，跨界拒绝并返回明确错误
- [ ] 2.2 `mergeRolePermissionActions`：按 roleCodes 各自的 scope 过滤 catalog/iam_role_permission 行
- [ ] 2.3 `IamService.loadPermissionSnapshot`：多角色并存时 union 合法 actions；单 platform_admin 快照无 non-platform 前缀
- [ ] 2.4 `IamServiceTests`：platform_admin 仅 platform.*；super_admin(global) 无 domain 直授；domain_admin 无 platform.*；admin bootstrap 绑定断言
- [ ] 2.5 评估 `PlatformRoleService` 与 `user_global_role` 双轨：文档标注 + 读路径统一（最小代码注释/deprecated）

## 3. 历史 permission code 与 API

- [ ] 3.1 盘点 global 角色曾依赖的 `domain.admin.*` 等码；改绑到已有 `platform.domain.*` 菜单按钮（或补 Flyway 新 platform 码）
- [ ] 3.2 冒烟：`platform_admin` 可调平台域列表/组织/菜单树等核心 API

## 4. 前端回归

- [ ] 4.1 单测：`platform_admin` 快照 → `/platform/home`；`domain_admin` → `/home`；双角色组合 → `/home` + platformAccess
- [ ] 4.2 手工：`admin` 登录 → `/platform/home`；顶栏平台入口可用；无 business 侧栏污染

## 5. 文档与规格

- [ ] 5.1 新增 `openspec/changes/.../specs/iam-role-console-binding/spec.md`（绑定规则 + 场景）
- [ ] 5.2 更新 `docs/product/prd.md` §2.5 / §4.1.3：区分 global super_admin vs domain 所有人
- [ ] 5.3 验证 `mvn test`（iam 模块）+ `pnpm typecheck`（AdminWeb 相关单测）
```

## openspec/changes/iam-role-console-binding-alignment/specs/iam-role-console-binding/spec.md

- Source: openspec/changes/iam-role-console-binding-alignment/specs/iam-role-console-binding/spec.md
- Lines: 1-91
- SHA256: 11cc17a747b67cf2771d7d79f378e7e1fda2eca8700edd6d4801337f23de1516

[TRUNCATED]

```md
## ADDED Requirements

### Requirement: 平台角色仅绑定平台控制台

系统 MUST 使 `role.scope = global` 的平台角色（含 `platform_admin`、`super_admin`、`security_auditor`）仅拥有：

- `iam_admin_menu.scope = platform` 的菜单/按钮绑定；
- `iam_permission.permission_scope = platform` 且 permission code 以 `platform.` 开头的直授或按钮授权。

平台角色的权限快照 `actions` MUST NOT 含不以 `platform.` 开头的 permission code。

#### Scenario: platform_admin 快照仅含 platform 前缀

- **WHEN** 用户仅绑定 `platform_admin`（global）并加载 AdminWeb 权限快照
- **THEN** `actions` 中每条 code 均以 `platform.` 开头
- **AND** `menuTree` 仅含 `scope=platform` 节点

#### Scenario: admin bootstrap 绑定 platform_admin

- **WHEN** 种子账号 `admin` 登录 AdminWeb
- **THEN** 其 IAM 绑定为 `platform_admin`（`user_global_role`）
- **AND** 不绑定 global `super_admin`

#### Scenario: platform_admin 默认首页

- **WHEN** `platform_admin` 登录且快照 actions 仅含 `platform.*`
- **THEN** 访问 `/` 重定向至 `/platform/home`

### Requirement: 业务域角色仅绑定业务域控制台

系统 MUST 使 `role.scope = domain` 的业务域角色（含 `domain_admin`、`agent`）仅拥有：

- `iam_admin_menu.scope = business` 的菜单/按钮绑定；
- 非 `platform.*` 前缀的业务 permission code（如 `domain.*`、`ticket.*`）。

业务域角色的权限快照 MUST NOT 含 `platform.*` 前缀的 actions，且 `menuTree` MUST NOT 含 `/platform/` 路由模块。

#### Scenario: domain_admin 快照无 platform 前缀

- **WHEN** 用户仅绑定 `domain_admin` 并加载权限快照
- **THEN** `actions` 不含以 `platform.` 开头的 code
- **AND** 侧栏 `menuTree` 不含 platform scope 菜单

#### Scenario: domain_admin 默认首页

- **WHEN** `domain_admin` 登录
- **THEN** 访问 `/` 重定向至业务域首页（`/home` 或 `VITE_BASE_HOME_PATH`）

#### Scenario: agent 权限包审计

- **WHEN** 种子数据 Flyway 收敛完成
- **THEN** `agent` 角色不存在 platform scope 菜单绑定
- **AND** 不存在 `platform.*` 直授

### Requirement: 双控制台显式组合

用户同时具备平台角色与业务域角色时，系统 MUST 通过 **分离绑定**（`user_global_role` + `user_domain_role`）实现，不得通过单个 global 角色内嵌 business 直授模拟。

#### Scenario: 双角色组合默认首页

- **WHEN** 用户同时绑定 `platform_admin` 与 `domain_admin@domain-A`
- **THEN** 快照 actions 同时含 `platform.*` 与非 `platform.*`
- **AND** `platformAccess=true`
- **AND** 访问 `/` 重定向至业务域首页（维持 E2-00 三元规则）

### Requirement: 角色授权保存 scope 校验

保存角色菜单/按钮授权（`replaceRolePermissions`）时，系统 MUST 拒绝跨界绑定并返回明确错误：

- global 角色不得绑定 business scope 菜单或 non-`platform.*` 按钮权限；
- domain 角色不得绑定 platform scope 菜单或 `platform.*` 按钮权限。

#### Scenario: domain 角色勾选 platform 按钮被拒绝

- **WHEN** 管理员为 `domain_admin` 保存含 platform scope 菜单按钮的授权
- **THEN** 请求失败并提示 scope 不一致

#### Scenario: global 角色勾选 business 菜单被拒绝

- **WHEN** 管理员为 `platform_admin` 保存含 business scope 菜单的授权
```

Full source: openspec/changes/iam-role-console-binding-alignment/specs/iam-role-console-binding/spec.md

