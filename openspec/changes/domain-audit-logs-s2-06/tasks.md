## 1. Flyway 与权限

- [x] 1.1 平台码迁移：`platform.audit_log.read` → `platform.log.audit.read`；`platform.login_log.read` → `platform.log.login.read`
- [x] 1.2 域码迁移：`domain.audit_log.read` → `platform.domain.control.audit_log.read`；`domain.login_log.read` → `platform.domain.control.login_log.read`
- [x] 1.3 `PLATFORM-DOMAIN-DETAIL` 下「操作日志」「登录日志」两个 catalog + 按钮；角色自动授权
- [x] 1.4 `database-increment-plan.md` 登记

## 2. 后端

- [x] 2.1 `PermissionCodes` / `AdminPermissionCatalog` 新码；Controller `@RequirePermission` 更新
- [x] 2.2 域级 login API 扩展 `result`/`portal_type`/`keyword`；audit 扩展 `keyword`
- [x] 2.3 ControllerTests 更新

## 3. 前端 API 与权限常量

- [x] 3.1 `fetchDomainAuditLogs` / `fetchDomainLoginLogs`
- [x] 3.2 `platform-domain-permissions.ts` + `permission-code-labels.ts`

## 4. 前端域详情（两个独立页）

- [x] 4.1 新增 `detail-audit-logs.tsx`、`detail-login-logs.tsx`；删除 `detail-logs.tsx`
- [x] 4.2 `detailTabs`：`audit_logs` / `login_logs`；`detail-sider` 两项独立门控
- [x] 4.3 `detail/index.tsx` switch + 无效 tab 回退

## 5. 验收

- [x] 5.1 typecheck + 后端单测；手工：两页权限隔离、筛选分页、平台页仍可用
- [x] 5.2 backlog / S2-closure-tracker 收口
