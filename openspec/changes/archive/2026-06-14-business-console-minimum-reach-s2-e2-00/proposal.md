## Why

US-S2-E2-00 要求**仅具备 business scope**、无平台权限的域内员工登录后进入业务域端首页，并能通过动态菜单访问至少一个 `pages/system/*` 真实页面。当前 `domain_admin` 等域角色在种子数据中仍绑定 platform scope 菜单与 `platform.*` 按钮权限；前端 `hasPlatformAccess()` 将快照中任意 `platform.*` action 视为平台权限，导致登录后跳转 `/platform/home` 而非 `/system/menu`（或 `VITE_BASE_HOME_PATH`），与 S2 主路径 AC 冲突。

## What Changes

- **IAM / Flyway / 快照**：business scope 按钮改用 **`domain.*`** 权限码；收敛 `domain_admin` 菜单绑定；快照 **actions 按 active scope 过滤**（与 menuTree 一致）。
- **Flyway**：收敛 `domain_admin`（及纯 business 测试账号）的 `iam_admin_role_menu_relation` / `iam_role_permission`，移除 platform scope 菜单绑定；登记 `database-increment-plan.md`。
- **前端**：`platformAccess` 仍表平台控制台能力；**默认首页**改为 actions 三元规则（仅 platform.*→平台；仅非 platform.*→业务域；两者都有→业务域）。
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
