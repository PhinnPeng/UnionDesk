# Comet Design Handoff

- Change: business-console-minimum-reach-s2-e2-00
- Phase: design
- Mode: compact
- Context hash: dd01d56a2ae29e0cad20f614b90dfeca06e1b7d31f04a3e5e90f60551d72da84

Generated-by: comet-handoff.sh

OpenSpec remains the canonical capability spec. This handoff is a deterministic, source-traceable context pack, not an agent-authored summary.

## openspec/changes/business-console-minimum-reach-s2-e2-00/proposal.md

- Source: openspec/changes/business-console-minimum-reach-s2-e2-00/proposal.md
- Lines: 1-31
- SHA256: 2e4b815879dc8bb67f9ff604c60fc4065c4050b7b8ff7ddf2631a18e18d227e1

```md
## Why

US-S2-E2-00 要求**仅具备 business scope**、无平台权限的域内员工登录后进入业务域端首页，并能通过动态菜单访问至少一个 `pages/system/*` 真实页面。当前 `domain_admin` 等域角色在种子数据中仍绑定 platform scope 菜单与 `platform.*` 按钮权限；前端 `hasPlatformAccess()` 将快照中任意 `platform.*` action 视为平台权限，导致登录后跳转 `/platform/home` 而非 `/system/menu`（或 `VITE_BASE_HOME_PATH`），与 S2 主路径 AC 冲突。

## What Changes

- **IAM / Flyway / 快照**：business scope 按钮改用 **`domain.*`** 权限码；收敛 `domain_admin` 菜单绑定；快照 **actions 按 active scope 过滤**（与 menuTree 一致）。
- **Flyway**：收敛 `domain_admin`（及纯 business 测试账号）的 `iam_admin_role_menu_relation` / `iam_role_permission`，移除 platform scope 菜单绑定；登记 `database-increment-plan.md`。
- **前端**：确认 `hasPlatformAccess` + `resolveHomePathFromMenus` 在纯 business 快照下默认业务首页；补充单测。
- **验收**：`domain_admin` 登录 → 业务首页；侧栏仅 business 树（无 `/platform/`）；至少 `/system/menu` 或 `/system/role` 可打开（二者已实现，非 Empty 占位）。
- **文档**：`backlog-stories.md` US-S2-E2-00 → Done；`.codex-tmp/S2-closure-tracker.md` 勾选。

## Capabilities

### New Capabilities

- `business-console-minimum-reach` — 业务域端最小可达（E2 主路径）

## Impact

- `UnionDesk/uniondesk-iam/.../IamService.java`（快照 actions 按 scope 过滤）
- Flyway 增量 + `AdminPermissionCatalog`（按需）
- `UnionDeskWeb/.../api/user/utils.ts`、`resolve-home-path.ts`、auth-guard 相关单测
- `docs/architecture/database-increment-plan.md`、`docs/product/backlog-stories.md`

## Non-Goals

- 全量 `business.*` 新命名空间（本 Story 仅用既有 **`domain.*`** 对齐 business 树按钮）
- 实现 `/system/user`、`/system/dept` 完整 CRUD（Stretch；本 Story 仅需 ≥1 个非占位 system 页）
- BusinessWeb 独立应用、工单类型（US-S2-E2-01 Stretch）
- 平台端 `/platform/*` 菜单或权限模型重构
```

## openspec/changes/business-console-minimum-reach-s2-e2-00/design.md

- Source: openspec/changes/business-console-minimum-reach-s2-e2-00/design.md
- Lines: 1-65
- SHA256: 7cb2c19e47b5d5a59f5dcd6f45d37ce691369d8906b0404a78aa6ef956b00fc1

```md
## Context

- **双控制台边界**：平台端 `scope=platform`（根路径 `/platform/`）；业务域端 `scope=business`（根路径 `/system/` 等）。见 `backlog-epics` §8.0。
- **后端已有**：`IamService.resolveAdminMenuScope(role)` — `global` 角色 → `platform`，`domain` 角色 → `business`；快照 `menuTree` 已按 active scope 过滤。
- **缺口 1**：快照 `actions` **未**按 scope 过滤 — business scope 菜单按钮仍挂载 `platform.menu.read` 等码，前端 `hasPlatformAccess()` 见 `platform.*` 即 `platformAccess=true`。
- **缺口 2**：rebaseline 中 `domain_admin`（`role.id=3`）的 `iam_admin_role_menu_relation` 含大量 platform scope 菜单（38、48–54 等），与「纯 business 员工」语义不符（后端虽过滤展示，但权限绑定混乱，不利于验收与后续 RBAC）。
- **前端已有**：`resolveHomePathFromMenus`、`hasPlatformAccess`（`api/user/utils.ts`）；`system/menu`、`system/role` 为真实页面；`system/user` 仍为占位。

```
登录 (domain_admin)
    │
    ▼
loadPermissionSnapshot
    ├── menuTree: business only ✓ (已有)
    └── actions: 含 platform.menu.* ✗
            │
            ▼
hasPlatformAccess → true → /platform/home ✗
```

## Goals

1. 无平台权限的域内员工登录后进入 **业务域首页**（默认 `/system/menu` 或 env `VITE_BASE_HOME_PATH`）。
2. 动态菜单与 `iam_admin_menu.scope=business` 一致；侧栏树无 `/platform/` 模块。
3. 至少 1 个 `pages/system/*` 页面可打开（复用现有 menu/role 页）。
4. 快照含 **platform scope 菜单或 platform 角色** 时，`platformAccess=true`，首页仍优先 `/platform/home`（AC #4）。

## Decisions

### 1. `domain.*` 业务码 + 快照 scope 过滤（后端 + Flyway）

- **Flyway**：business scope 按钮 `platform.menu.*` / `platform.role.*` → `domain.menu.*` / `domain.role.*`；新增 `domain.menu.*` catalog 项；`IamController` 菜单 API 双码 `@RequirePermission`。
- **快照**：`IamService.loadPermissionSnapshot` 按 `activeMenuScope` 过滤 button actions；`mergeRolePermissionActions` 按 catalog `permissionScope` 过滤（domain 角色不合并 platform 直授）。
- 平台角色仍为 platform scope，actions 含 `platform.*` → `hasPlatformAccess` 保持 true。

**理由**：与前端 `domain.menu.*` 已一致；满足 AC#4（有 platform.* 才 platformAccess）与 AC#1（纯 business 无 platform.*）。

### 2. Flyway 收敛 domain_admin 菜单绑定

新增 Flyway（如 `V202606140001__business_console_domain_admin_menus.sql`）：

- 删除 `role_id=3`（`domain_admin`）对 **platform scope** `iam_admin_menu` 的 `iam_admin_role_menu_relation` 行。
- 保留 business scope 系统管理菜单（catalog 1、menu 2/3/4/5 及对应 button 子节点）。
- 按需清理 `iam_role_permission` 中仅服务于已移除 platform 菜单、且 business 树不再引用的孤儿权限（保持迁移幂等）。
- 登记 `database-increment-plan.md` US-S2-E2-00 行。

### 3. 前端 platformAccess 判定（维持 AC #4 规则）

- **规则不变**：`actions` 中任意 `platform.*` → `platformAccess=true`；叠加 platform scope 菜单、平台角色 hint。
- **依赖后端过滤**：域角色快照不再携带 spurious `platform.*` actions 后，`hasPlatformAccess` 自然为 false。
- 补充 `utils.ts` / `resolve-home-path` 单测：纯 business 快照 → business home；含 platform 菜单或 platform.* → platform home。

### 4. 验收账号与页面

| 检查项 | 预期 |
|:---|:---|
| `domain_admin` 登录 | 跳转 `/system/menu`（或配置首页） |
| 侧栏 | 仅「系统管理」等 business 项，无「业务域管理」等平台项 |
| 可打开页 | `/system/menu` 或 `/system/role` 树表/列表正常 |
| `super_admin` 登录 | 仍进 `/platform/home` |

## Risks

- 若其他域角色（如 `agent`）仍绑定 platform 菜单，需同样收敛或确认产品预期。
- `@RequirePermission("platform.menu.read")` 等后端 API 仍接受 business 树按钮授权 — 需确认权限校验走 menu 授权链而非仅 action 前缀（现有 AdminMenu 授权应仍有效）。
```

## openspec/changes/business-console-minimum-reach-s2-e2-00/tasks.md

- Source: openspec/changes/business-console-minimum-reach-s2-e2-00/tasks.md
- Lines: 1-23
- SHA256: 6e563d25b7899c3884754be25a17f82872ed4687c191acb3b2842c561c1737dd

```md
## 1. Flyway 与权限绑定

- [ ] 1.1 新增 `domain.menu.*` 四码（iam_permission + PermissionCodes + AdminPermissionCatalog）
- [ ] 1.2 Flyway：business scope 按钮 `platform.menu.*` / `platform.role.*` → `domain.*`；移除 role 3 的 platform scope role_menu
- [ ] 1.3 清理 role 3 孤儿 `iam_role_permission`（platform scope 直授）；登记 `database-increment-plan.md`

## 2. 后端快照与 API

- [ ] 2.1 `IamController` 菜单 CRUD/tree：`@RequirePermission` 支持 platform + domain 双码
- [ ] 2.2 `IamService.loadPermissionSnapshot`：actions 按 activeMenuScope + permissionScope 过滤
- [ ] 2.3 `IamServiceTests`：domain 角色快照无 `platform.*`；global 角色仍含 platform.*

## 3. 前端首页与权限

- [ ] 3.1 验证 `hasPlatformAccess` + `resolveHomePathFromMenus` 在过滤后快照下行为正确
- [ ] 3.2 补充/更新 `api/user/utils` 与 `resolve-home-path` 单测
- [ ] 3.3 确认 auth-guard 对 business 路由无 platform 误拦截

## 4. 验收与文档

- [ ] 4.1 手工：`domain_admin` 登录 → 业务首页；侧栏无 `/platform/`；打开 `/system/menu` 或 `/system/role`
- [ ] 4.2 `pnpm typecheck` + 后端 `mvn test`（相关模块）
- [ ] 4.3 `backlog-stories.md` US-S2-E2-00 → Done；`.codex-tmp/S2-closure-tracker.md` 勾选
```

## openspec/changes/business-console-minimum-reach-s2-e2-00/specs/business-console-minimum-reach/spec.md

- Source: openspec/changes/business-console-minimum-reach-s2-e2-00/specs/business-console-minimum-reach/spec.md
- Lines: 1-49
- SHA256: 4f190c0c0f4a6baf50915ae094363b28f55b66ebd2747378235d48c29d060a38

```md
## ADDED Requirements

### Requirement: 纯 business 员工默认进入业务域首页

系统 MUST 使仅具备 business scope 菜单、且无平台角色（`super_admin` / `platform_admin`）的域内员工，登录 AdminWeb 后默认进入业务域首页，而非 `/platform/home`。

#### Scenario: domain_admin 登录首页

- **WHEN** 用户以 `domain_admin` 登录且权限快照 menuTree 仅含 `scope=business` 菜单
- **THEN** 登录成功跳转至 `VITE_BASE_HOME_PATH` 或菜单首项（如 `/system/menu`）
- **AND** 不跳转 `/platform/home`

### Requirement: 动态菜单与 business scope 一致

AdminWeb 侧栏动态菜单 MUST 与 `iam_admin_menu.scope=business` 授权一致，且不包含 `/platform/` 路径模块。

#### Scenario: 侧栏无平台模块

- **WHEN** 纯 business 员工加载菜单快照
- **THEN** 侧栏树中不出现 `/platform/domains` 等平台路由
- **AND** 至少包含一条可导航的 business 系统管理菜单

### Requirement: 至少一个 system 页面可达

业务域端 MUST 提供至少一个 `pages/system/*` 非 Empty 占位页面可通过菜单打开。

#### Scenario: 打开菜单管理或角色管理

- **WHEN** 用户点击「菜单管理」或「角色管理」
- **THEN** 页面渲染真实列表/树表内容（非占位 Empty）

### Requirement: 平台权限判定

权限快照 `actions` 中若存在任意以 `platform.` 开头的权限码，或快照含 platform scope 菜单，或用户具备平台角色，系统 MUST 视为 `platformAccess=true`，首页优先 `/platform/home`。

#### Scenario: 平台管理员仍进平台首页

- **WHEN** 用户为 `super_admin` 或快照含 platform scope 菜单及对应 `platform.*` actions
- **THEN** 默认首页为 `/platform/home`

### Requirement: 业务域按钮使用 domain 权限码

business scope 的 `iam_admin_menu` 按钮 MUST 使用 `domain.*` 权限码（如 `domain.menu.read`），不得使用 `platform.*` 前缀，以确保纯 business 快照不含 platform 权限判定噪声。

#### Scenario: domain_admin 快照 actions

- **WHEN** `domain_admin` 加载权限快照且 active scope 为 business
- **THEN** `actions` 含 `domain.menu.read` 等业务码
- **AND** `actions` 不含 `platform.menu.read` 等 platform 前缀码
```

