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
4. **默认首页三元规则**：仅 `platform.*` actions → 平台控制台；仅非 `platform.*` → 统一业务域后台；两者都有 → **仍进统一业务域后台**。`platformAccess` 仅表平台控制台能力，不单独决定首页。

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
