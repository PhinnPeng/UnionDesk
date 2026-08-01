# Implement — 业务域端完整镜像（E0 + M0 首批）

## Scope（本迭代）

按已审阅 `prd.md` / `design.md`，先落地：

1. **E0 进端契约**：域名单判定 `businessDomainAccess`；默认首页；顶栏切换；按当前域加载 business 权限包（可分步：先名单+首页，再 snapshot 按域）。
2. **M0 菜单种子**：缺口 `domain.*` 权限码 +「域治理」catalog/menu/button 写入 Flyway，并在业务端「菜单管理」可见。
3. **前端空壳**：注册 `/domain/*` 静态路由与占位页，菜单可导航（功能页后续子任务填实）。

完整 12 页业务实现不在本迭代一次做完；本迭代验收 AC0 + 菜单管理可见域治理树 + 路由可达。

## Checklist

- [x] 1. 写/补 `PermissionCodes` + `AdminPermissionCatalog` 缺口码
- [x] 2. Flyway：`iam_permission` + `iam_admin_menu`（域治理）+ `domain_admin` 角色绑定
- [x] 3. 前端 `permission-code-labels` 同步中文
- [x] 4. `hasBusinessDomainAccess` 改为基于 `accessibleDomains` / 成员名单
- [x] 5. `resolveHomePath*` 按 platformAccess × businessDomainAccess 二维表
- [x] 6. 登录/用户信息透传 `accessibleDomains`；顶栏按钮行为对齐 §0.5
- [x] 7. 按域 permission-snapshot（或切会话域再拉包）最小可用路径
- [x] 8. `/domain/*` 路由 + 占位页 + component_key 对齐菜单
- [x] 9. 菜单模块同步：`platform-com-registry` 注册 `domain/*` 组件；业务/平台菜单管理可选用
- [x] 10. 方案 A：`/home` 概览产品化 + Flyway 固化 BUSINESS-HOME；`/domain/overview` 为「运营概览」骨架
- [x] 11. 菜单扁平化：取消「域治理/工作台」catalog；子菜单上移一级；`/home` 口径改为「概览」
- [ ] 12. 手工验证：侧栏一级「概览」+ 原域治理叶子；菜单管理无「域治理」父节点

## Validation

- 登录三种账号路径符合 design §0.7
- `/system/menu`（business）树为一级扁平：概览、运营概览、通用设置…（无「域治理」「工作台」父节点）
- 点击侧栏菜单进入对应 `/domain/*` 或 `/home`（无 404）
- 菜单编辑「前端组件」级联可选 `home` / `domain/*`（无「域治理/」前缀）

## Rollback

- 回滚 Flyway 版本；还原 access / resolve-home-path / auth 相关前端文件

## Landed artifacts（本迭代）

| 区域 | 路径 |
|:---|:---|
| Flyway | `UnionDesk/uniondesk-app/.../V20260727183000__business_domain_govern_menus.sql` |
| 权限码 | `PermissionCodes.java` + `AdminPermissionCatalog.java` |
| 进端 | `resolve-home-path.ts` / `business-domain.ts` / `store/auth.ts` / `IamService.loadPermissionSnapshot` |
| 路由/占位 | `router/routes/modules/domain.ts` + `pages/domain/**` |
| 菜单模块 | `permission-code-labels.ts` + `platform-com-registry.ts` |
