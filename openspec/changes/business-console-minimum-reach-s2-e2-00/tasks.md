## 1. Flyway 与权限绑定

- [x] 1.1 新增 `domain.menu.*` 四码（iam_permission + PermissionCodes + AdminPermissionCatalog）
- [x] 1.2 Flyway：business scope 按钮 `platform.menu.*` / `platform.role.*` → `domain.*`；移除 role 3 的 platform scope role_menu
- [x] 1.3 清理 role 3 孤儿 `iam_role_permission`（platform scope 直授）；登记 `database-increment-plan.md`

## 2. 后端快照与 API

- [x] 2.1 `IamController` 菜单 CRUD/tree：`@RequirePermission` 支持 platform + domain 双码
- [x] 2.2 `IamService.loadPermissionSnapshot`：actions 按 activeMenuScope + permissionScope 过滤
- [x] 2.3 `IamServiceTests`：domain 角色快照无 `platform.*`；global 角色仍含 platform.*

## 3. 前端首页与权限

- [x] 3.1 `resolveHomePathFromMenus` 改为 actions 三元规则（与 `platformAccess` 解耦）
- [x] 3.2 单测：仅 platform.* → 平台首页；仅 domain.* → 业务首页；两者都有 → 业务首页
- [x] 3.3 `hasPlatformAccess` 仍管平台入口；auth-guard root 重定向跟随新首页规则

## 4. 验收与文档

- [x] 4.1 手工：`domain_admin` 登录 → 业务首页；侧栏无 `/platform/`；打开 `/system/menu` 或 `/system/role`（Flyway 收敛 + 单测覆盖首页/快照过滤）
- [x] 4.2 `pnpm typecheck` + 后端 `mvn test`（相关模块）
- [x] 4.3 `backlog-stories.md` US-S2-E2-00 → Done；`.codex-tmp/S2-closure-tracker.md` 勾选
