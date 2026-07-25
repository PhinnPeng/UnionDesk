# 实现计划：员工身份遗留表物理拆除

> 在用户审阅设计并批准 `task.py start` 之前，本文件为执行清单草稿。

## 前置

- [ ] 用户审阅并批准 `prd.md` + `design.md` + `docs/superpowers/specs/2026-07-18-staff-identity-legacy-demolition-design.md`
- [ ] `task.py start` 后方可改业务代码
- [ ] 联调前确认库可清空

## W1 — Staff 能力补齐

1. `staff_account` 离职字段 + `StaffAccountService.offboard` / 列表筛选 `offboarded`
2. Flyway：建 `staff_organization`；Staff API 支持组织读写与 `organizationId` 筛选
3. `POST .../offboard`（及 pool 约定）；区分 disable vs offboard
4. 更新 `AdminPermissionCatalog` path；Flyway 更新 `iam_permission.path_pattern`
5. 验证：Staff API 单测 / 手工创建与离职

**检查点**：仅 staff 路径可完成旧用户页核心操作（后端）

## W2 — 前端切换 + 删除旧 API

1. `UnionDeskAdminWeb` `api/platform/iam.ts` → `/admin/staff`
2. 用户页 / 离职池 / 创建编辑 DTO 适配
3. 删除 `IamController` `/users*` 与 `IamService` UserAccount CRUD、`user_domain_role` 写入
4. 更新相关测试（`iam.test.ts`、后端 Controller 测试）

**检查点**：AdminWeb 用户流走 staff；`/iam/users` 404

## W3 — JOIN 切源与引用扫清

1. 审计 / 登录日志 / 域 created_by / ticket 摘要等 mapper 改 `staff_account`
2. 全仓 grep：`user_account`、`user_domain_role`、`user_global_role`、`user_organization`（排除 archive）
3. 修掉 `IdentitySubjectMapper` 等残留

**检查点**：grep 门禁通过；相关查询不报错

## W4 — DROP + seed + 文档

1. Flyway：解 FK → DROP 四表（及触发器）→ seed 平台管理员
2. 空库 migrate + 冒烟（AC2–AC6）
3. 更新 `data-model.md` §3.3、`implementation-traceability.md`
4. `trellis-check` / 质量门

**检查点**：全部 AC 勾选

## 验证命令（实现时按仓库习惯微调）

```powershell
# 后端测试（模块级）
cd UnionDesk; .\mvnw.cmd -pl uniondesk-app -am test -Dtest=*Staff*,*Iam*

# 前端相关测试
cd UnionDeskWeb; pnpm -C apps/UnionDeskAdminWeb test -- iam

# 引用门禁
rg -n "user_account|user_domain_role|user_global_role|user_organization" UnionDesk --glob "!**/archive/**"
```

## 回滚点

- W1–W3 完成前：Git revert 即可
- W4 执行后：重建联调库；不要在已 DROP 库上尝试半恢复旧表

## 风险文件

- `IamService.java` / `IamController.java` / `UserAccountMapper*`
- `StaffController.java` / `StaffAccountService.java`
- `OrganizationMapper.xml`（`user_organization`）
- `LoginLogMapper.xml` / `AuditLogMapper.xml` / `BusinessDomainMapper.xml`
- `UnionDeskWeb/.../api/platform/iam.ts` + `pages/platform/user/*`
