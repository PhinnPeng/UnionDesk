## 1. 文档

- [ ] 1.1 `docs/architecture/iam-rbac-dual-table-architecture.md`
- [ ] 1.2 `docs/architecture/permission-registration-checklist.md`
- [ ] 1.3 Design Doc 定稿（双表 + 前端审计）

## 2. 后端 — 开发态清理

- [ ] 2.1 删除 `PermissionCodes` 全部 `@Deprecated` 别名
- [ ] 2.2 修正 Java 引用为正式常量
- [ ] 2.3 `AdminPermissionCatalog` 删除 `DOMAIN_ADMIN_READ`（`domain.admin.read`）等旧项
- [ ] 2.4 `AdminPermissionCatalogTests`、`AdminMenuServiceTest` 通过

## 3. 前端 — 基于审计调整

- [ ] 3.1 `platform-domain-permissions.ts` 删除 3 条 `@deprecated` re-export
- [ ] 3.2 `detail-shared.ts` 删除全部 `DOMAIN_*_PERMISSION` 别名 export
- [ ] 3.3 迁移引用（10 文件）：`detail-sider`、`detail/index`、`detail-overview`、`detail-customers`、`detail-blockwords`、`detail-roles`、`detail-audit-logs`、`detail-login-logs`、`detail-baseinfo`、`detail-onboarding`
- [ ] 3.4 删除 `detail-logs.tsx`
- [ ] 3.5 `permission-code-labels.ts` 删除旧码（audit_log、login_log、domain.customer 等）
- [ ] 3.6 `pnpm exec tsc --noEmit` 通过

## 4. 验收

- [ ] 4.1 全仓无 `DOMAIN_.*_PERMISSION` import
- [ ] 4.2 全仓无 `@deprecated` 权限 re-export
- [ ] 4.3 手工：super_admin 重新登录后域详情各 tab 与清理前一致

## 5. 明确不做

- [ ] 5.1 双表合并 — 不做
- [ ] 5.2 DomainControlFeatureRegistry — 不做
