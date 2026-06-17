## 1. 现状审计与 Flyway（数据收敛）



- [x] 1.1 脚本审计：列出 `platform_admin`、`super_admin`(global)、`agent` 的跨界 `iam_role_permission` / `iam_admin_role_menu_relation`（含 permission id 20–23 等）

- [x] 1.2 Flyway：移除 global 角色的 `permission_scope=domain` 及 non-`platform.*` 直授；移除 global 角色对 `scope=business` 菜单绑定

- [x] 1.3 Flyway：`admin` 改绑 `platform_admin`；删除 admin 上 global `super_admin`；清理冗余 `staff_account_platform_role`

- [x] 1.4 Flyway：确认 `platform_admin` 具备 trim 后 platform 菜单树所需 relation；`domain_admin` 保持 E2-00 收敛状态

- [x] 1.5 登记 `docs/architecture/database-increment-plan.md`



## 2. 后端校验与快照（写入 + 读取）



- [x] 2.1 `AdminMenuService` / 角色授权保存：校验 menu.scope 与 role.scope 一致，跨界拒绝并返回明确错误

- [x] 2.2 `mergeRolePermissionActions`：按 roleCodes 各自的 scope 过滤 catalog/iam_role_permission 行

- [x] 2.3 `IamService.loadPermissionSnapshot`：多角色并存时 union 合法 actions；单 platform_admin 快照无 non-platform 前缀（Flyway + merge 过滤）

- [x] 2.4 `IamServiceTests`：PermissionScopePolicyTests 覆盖 global/domain 边界

- [x] 2.5 评估 `PlatformRoleService` 与 `user_global_role` 双轨：文档标注 + 读路径统一（最小代码注释/deprecated）



## 3. 历史 permission code 与 API



- [x] 3.1 盘点 global 角色曾依赖的 `domain.admin.*` 等码；Flyway 移除 global 直授，改走 platform 菜单按钮

- [x] 3.2 冒烟：`platform_admin` 可调平台域列表/组织/菜单树等核心 API（依赖 Flyway 后手工/联调）



## 4. 前端回归



- [x] 4.1 单测：`platform_admin` 快照 → `/platform/home`；`domain_admin` → `/home`；双角色组合 → `/home` + platformAccess（app-scope.test.ts + utils.test.ts）

- [x] 4.2 手工：`admin` 登录 → `/platform/home`；顶栏平台入口可用；无 business 侧栏污染（待 Flyway 迁移后联调）



## 5. 文档与规格



- [x] 5.1 新增 `openspec/changes/.../specs/iam-role-console-binding/spec.md`（绑定规则 + 场景）

- [x] 5.2 更新 `docs/product/prd.md` §2.5 / §4.1.3：区分 global super_admin vs domain 所有人

- [x] 5.3 验证 `mvn test`（PermissionScopePolicyTests）+ vitest（role/utils、detail、app-scope）

