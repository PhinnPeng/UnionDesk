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
