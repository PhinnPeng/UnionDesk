# 角色与菜单 PRD 对齐修复清单

## 目标
- 以《产品需求文档 v1.0》和《权限矩阵》为准，收口角色管理与菜单管理的接口边界、敏感操作控制、审计与保活规则。
- 仅记录当前实现与 PRD 的差异、修复项、验收项，作为统一追踪入口。
- 与现有的角色、菜单子模块清单并行，不互相覆盖。

## 当前已确认的差异

### 角色管理
- 平台角色绑定、业务域 `super_admin` 授予 / 回收、域角色权限分配、角色删除等高影响操作，当前仍需补齐 step-up 与审计日志闭环。
- 平台管理入口当前偏向 `super_admin`，需要与 PRD 中 `platform_admin` / `super_admin` / `domain_admin` 的操作边界再次对齐。
- 最后一名 `platform_admin` / `super_admin` 的保活保护需要显式保留并可验收。
- 域成员角色变更同样属于高影响操作，需要纳入统一校验与审计。

### 菜单管理
- 菜单树与 `permission-snapshot` 的口径需要统一，确保菜单级导航与按钮级权限分层清晰。
- `required` 菜单 / 按钮需要保持不可删、不可改，并在角色权限分配时自动携带。
- 菜单 CRUD 需要补齐审计日志，避免只做结构对齐、缺少合规闭环。

## 待修复项

### 角色管理
- [ ] 平台角色绑定补齐 step-up 校验与审计日志
- [ ] 业务域 `super_admin` 授予 / 回收补齐 step-up 校验与审计日志
- [ ] 域角色权限分配补齐 step-up 校验与审计日志
- [ ] 角色删除补齐 step-up 校验与审计日志
- [ ] 域成员角色变更补齐 step-up 校验与审计日志
- [ ] 明确 `platform_admin`、`super_admin`、`domain_admin` 的操作边界并与 PRD 对齐
- [ ] 保留并验收最后一名 `platform_admin` / `super_admin` 的保活保护

### 菜单管理
- [ ] 统一 `/api/v1/iam/me/permission-snapshot` 与 `/api/v1/iam/menus/tree` 的返回口径
- [ ] 菜单管理树保持菜单级与按钮级分层，避免将按钮权限混入导航树
- [ ] `required` 菜单 / 按钮不可删除、不可修改
- [ ] 角色权限分配时自动携带 `required` 菜单 / 按钮
- [ ] 菜单新增、编辑、删除补齐审计日志
- [ ] 菜单树与当前用户菜单全量配置保持一致，接口返回需可直接支撑前端路由与管理页

### 统一验收
- [ ] 高影响操作必须有 step-up 验证
- [ ] 高影响操作必须写入审计日志
- [ ] 保活规则必须有负向验收用例
- [ ] 角色与菜单的接口边界必须与 PRD 和权限矩阵一致

## 2026-05-10 角色管理页收口与系统角色编辑
- [x] `IamService.updateRole` 放开系统角色编辑，删除保护保持不变
- [x] 角色管理页按应用域收口：平台页仅显示 `global` 角色，业务域页仅显示 `domain` 角色
- [x] 操作列编辑始终显示，系统角色仅隐藏删除按钮
- [x] 角色页授权树按当前应用域切换菜单树 scope
- [x] 验证通过：`mvnw.cmd -Dtest=IamServiceTests test`
- [x] 验证通过：`pnpm test --run src/pages/system/role/index.test.tsx`
- [x] 验证通过：`pnpm typecheck`

## 验收方式
- 大改动优先使用 BDD / 集成测试覆盖。
- 小改动维持最小测试增量，不扩散无关测试面。
- 每一项必须落到具体接口、具体行为和具体验收结果。

## 备注
- 先不新增数据库 schema，优先复用现有 `audit_log` / `operation_log` / step-up 体系。
- 平台管理入口是否放宽到 `platform_admin`，作为单独决策点处理，不在本清单里预设结论。

## 2026-05-10 角色编辑页菜单回写与保存后关闭
- [x] 编辑抽屉在 detailData 后到达时重新回写菜单选择
- [x] 保存成功后主动关闭抽屉并刷新角色列表
- [x] 验证通过：`pnpm vitest run src/pages/system/role/components/detail.test.tsx`
