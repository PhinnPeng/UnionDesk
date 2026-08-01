# Design — P2 双日志 + 域角色

## Boundaries

| 层 | 范围 |
|:---|:---|
| 前端 | `pages/domain/{audit-logs,login-logs,roles}`；permissions 常量 |
| shared | 若缺 create/update/delete domain role API，在 `packages/shared` 最小补齐 |
| 后端 | 日志接口并列 `domain.audit_log.read` / `domain.login_log.read`；`DomainRoleController` 已是 `domain.role.*` |

## 对照

| 页 | 参考 | 差异 |
|:---|:---|:---|
| audit-logs | `detail-audit-logs.tsx` | `domain.audit_log.read` + 会话域 |
| login-logs | `detail-login-logs.tsx` | `domain.login_log.read` + 会话域 |
| roles | `detail-roles.tsx` + DomainRoleController 写接口 | 用 `fetchDomainRoles` / `fetchDomainRolePermissions`（非 platform-*）；补写 UI |

## 角色写

后端已具备 POST/PUT/DELETE roles 与 PUT permissions。前端需补 shared client 与业务页 UI（Modal/抽屉勾选）。系统角色 `super_admin` 等内置角色禁止删改（对齐 Service 校验）。
