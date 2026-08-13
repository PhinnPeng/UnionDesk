# 平台端首页概览上移为顶层菜单并更名概览

## Goal

平台端菜单结构调整：将「首页概览」从「平台首页」目录（catalog）的子菜单**上移一层**为顶层菜单，更名「概览」；删除「平台首页」目录；**角色权限（菜单关联）联动更新**，保证各角色对首页的可见性不变。

## 现状（证据）

- 运行时结构（V202605220001 创建）：`PLATFORM-HOME-CATALOG`（catalog「平台首页」）→ 子菜单「首页概览」（route `/platform/home`，scope=platform，原名 平台首页 的种子 id=38）
- catalog 下仅此一个子菜单（单子节点目录冗余）；`platform.home.query` 按钮挂在 `/platform/home` 菜单下（V202605220002），**不在 catalog 下**——删 catalog 不影响按钮
- 角色关联：`iam_admin_role_menu_relation` 中 super_admin 关联全部平台菜单（含 catalog 与 首页菜单，V202605220001 后半 INSERT IGNORE）；首页菜单 `permission_code=NULL`，访问控制靠菜单关联
- 前端无需改动：菜单名由 `iam_admin_menu` 动态渲染；`platform-com-registry.ts` 组件 label 已是「概览」（value=home）；`permission-code-labels.ts` 为权限码标签与菜单名无关

## Requirements

- R1 上移：「首页概览」menu（route `/platform/home`）→ `parent_id=NULL`（顶层）、更名「概览」、`order_no=1`（顶层首位）
- R2 删除目录：`PLATFORM-HOME-CATALOG`（catalog）删除；目录树遗留子节点（如有）一并清理
- R3 角色权限联动：删除 `iam_admin_role_menu_relation` 中指向目录树（catalog 及其遗留子节点）的关联；「概览」菜单的既有关联**保留**（各角色可见性不变）；`iam_role_permission`（platform.home.query 授权）不动
- R4 幂等与顺序：先脱离（R1）→ 再删关联（R3）→ 删子节点 → 删目录（R2），避免 FK/孤儿

## Acceptance Criteria

- [ ] AC1 迁移执行后：平台端菜单树顶层出现「概览」（route `/platform/home`，order 1，非目录子节点）；「平台首页」目录消失
- [ ] AC2 `iam_admin_role_menu_relation` 无指向 `PLATFORM-HOME-CATALOG` 的孤儿关联；「概览」菜单的关联保留（super_admin 等角色仍可见）
- [ ] AC3 `platform.home.query` 按钮与授权不变；业务域端「工作台/首页概览」（/home）不受影响
- [ ] AC4 迁移可重复语义正确（Flyway 单次执行）；无其他迁移引用冲突
- [ ] AC5 冒烟：平台端登录后导航显示「概览」一级菜单，进入 /platform/home 正常渲染（验证需 Docker/DB 可用环境）

## Out of Scope

- 业务域端「工作台」目录（Q1 仅平台端）
- 菜单名国际化/前端文案（前端零改动）
- 权限码增删

## 参考证据

- `UnionDesk/uniondesk-app/src/main/resources/db/migration/current/V202605220001__trim_platform_admin_menus.sql`（catalog 创建 + 角色重关联模式）
- `V202605220002__platform_menu_icons_and_buttons.sql`（按钮挂载 + platform.home.query 授权）
- `V202605200002__rebaseline_current_schema.sql`（菜单/关联表结构）
- `UnionDeskWeb/apps/UnionDeskAdminWeb/src/pages/platform/system/menu/components/platform-com-registry.ts:11`（组件 label 已为「概览」）
