# Implement — P2

## Checklist

- [x] 1. domain-permissions 补 audit/login/role 常量
- [x] 2. 日志后端并列 domain.* 读码（若缺）
- [x] 3. shared 补 domain role 写 API（若缺）
- [x] 4. `/domain/audit-logs`、`/domain/login-logs`
- [x] 5. `/domain/roles`（读 + 写）
- [x] 6. typecheck
  - 建议人工冒烟：日志筛选、角色 CRUD、平台 detail 无回归

## Validation

```powershell
pnpm -C UnionDeskWeb/apps/UnionDeskAdminWeb exec tsc -b --pretty false
```

## Order

logs → roles（roles 依赖 shared API）。
