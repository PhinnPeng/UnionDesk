# IAM 角色—控制台绑定对齐 — 高层设计

## Context

```
PRD 意图                         当前实现缺口
────────────────────────────────────────────────────────────
platform_admin → 平台控制台      platform_admin 含 domain.user.*
domain_admin   → 业务域控制台    E2-00 已收敛 ✓
super_admin    → 域「所有人」     另有 global super_admin 全量包
admin 账号     → 平台管理员       绑 global super_admin
```

**已有能力（不重复建设）：**

- `role.scope`：`global` | `domain`（`IamService.replaceUserRoleBindings` 已分写 `user_global_role` / `user_domain_role`）
- `iam_admin_menu.scope`：`platform` | `business`
- `iam_permission.permission_scope`：`platform` | `domain`
- E2-00：`domain_admin` 移除 platform 菜单/直授；快照 actions 按 `activeMenuScope` 过滤；前端 actions 三元首页规则

**本 change 补齐：** 让 **platform 侧角色** 与 **bootstrap 数据** 同样满足边界；并在 **写入路径** 防止再次跨界。

---

## Goals

1. 任一 `global` 角色快照 actions **不含** non-platform 前缀码（除非产品明确的双栖组合来自 **两个角色** 而非一个角色包）。
2. 任一 `domain` 角色快照 actions **不含** `platform.*`。
3. `platform_admin` 登录默认 `/platform/home`；`domain_admin` 默认 `/home`。
4. 角色保存/授权 UI 无法把 platform 菜单勾到 domain 角色（反之亦然）。

---

## 角色模型（定稿）

### 标准系统角色

| code | scope | 绑定表 | 菜单 scope | 权限包 | 默认首页 | 产品名 |
|:---|:---|:---|:---|:---|:---|:---|
| `platform_admin` | global | `user_global_role` | platform | 仅 `platform.*` + `permission_scope=platform` | `/platform/home` | 平台管理员 |
| `security_auditor` | global | `user_global_role` | platform（子集） | 审计/日志类 platform 码 | `/platform/home` | 安全审计员 |
| `super_admin` | **global** | `user_global_role` | platform（全量或 break-glass 子集） | **仅 platform 包**；不再含 ticket/domain.user 等业务直授 | `/platform/home` | **系统超级管理员**（break-glass） |
| `domain_admin` | domain | `user_domain_role` + domainId | business | `domain.*` / 业务 ticket 等 | `/home` | 业务域管理员 |
| `agent` | domain | 同上 | business | 工单/咨询子集 | `/home` | 客服专员 |
| `super_admin` | **domain** (`domain_role`) | `domain_member_role` | N/A（域内 RBAC） | 域内 permission_item 全量 | `/home` | **业务域所有人** |

> **命名策略（本 change）**：不改表字段 code；通过 **UI 展示名 + 文档** 区分两个 super_admin。中长期可 rename global → `system_root`（Non-Goal 后续 change）。

### 双控制台用户（显式组合）

```
user_global_role:    platform_admin
user_domain_role:    domain_admin @ domain-A
```

| 维度 | 行为 |
|:---|:---|
| 快照 actions | platform.* + domain.*（来自两角色合并） |
| platformAccess | true |
| 默认首页 | `/home`（维持 E2-00 三元规则） |
| 平台工作 | 顶栏平台入口 |

**禁止**：单个 `global super_admin` 角色内嵌全部 business 直授来模拟双控制台。

---

## 架构决策

### D1. 绑定规则 = 三层一致

保存/种子/快照三处统一：

```
role.scope  ↔  iam_admin_menu.scope  ↔  permission.permission_scope + code 前缀
─────────────────────────────────────────────────────────────────────────────
global      ↔  platform              ↔  platform + platform.*
domain      ↔  business              ↔  domain + 非 platform.* 业务码
```

**校验点：**

- `AdminMenuService.replaceRolePermissionRows` / 角色授权保存：拒绝跨界 menu relation
- `mergeRolePermissionActions(roleCodes, …)`：按 **各 role.scope** 过滤 catalog 行，不仅依赖 JWT 里的 activeMenuScope
- Flyway 数据修复：DELETE 跨界 `iam_role_permission` / `iam_admin_role_menu_relation`

### D2. bootstrap `admin` → `platform_admin`

```sql
-- 意图（具体 id 以 Flyway 幂等写法为准）
DELETE ugr FROM user_global_role ugr
  JOIN user_account ua ON ua.id = ugr.user_id
  JOIN role r ON r.id = ugr.role_id
WHERE ua.username = 'admin' AND r.code = 'super_admin';

INSERT IGNORE INTO user_global_role (user_id, role_id)
SELECT ua.id, r.id FROM user_account ua, role r
WHERE ua.username = 'admin' AND r.code = 'platform_admin';
```

同时清理 `staff_account_platform_role` 重复绑定（若与 global 角色冗余）。

### D3. 收敛 `platform_admin` 权限包

从 `iam_role_permission` 移除 role_id=5 的 permission_id ∈ {20,21,22,23}（`domain.user.*`）及任何 `permission_scope=domain` 行。

保留 platform 菜单树（`V202605220001` trim 后 platform scope 全量给 super_admin；**platform_admin 应绑定 platform 子集或同等 platform 包，但不含 domain 直授**）。

### D4. 收敛 global `super_admin` 权限包

**方向 A（推荐，本 change）：** global super_admin 与 platform_admin 同为 **纯 platform 包**；break-glass 通过「全 platform 菜单 + 全 platform 直授」体现，不再携带 ticket/consultation 等业务 API 码。

- DELETE super_admin 的 business scope `iam_admin_role_menu_relation`
- DELETE super_admin 的 `iam_role_permission` where `permission_scope = 'domain'` OR code NOT LIKE 'platform.%'（保留 `platform.*`；`domain.admin.*` 等历史 platform-scope 但 domain 前缀码单独评估，见 D5）

**方向 B（备选，不默认）：** 保留 super_admin 全量包 → 必须改首页规则（违反 Non-Goal）。

### D5. 历史 permission code 前缀例外

存在 `domain.admin.read` 等 **`permission_scope=platform`** 但 code 以 `domain.` 开头 的码。

| 处理 | 说明 |
|:---|:---|
| 短期 | global 角色 **不再直授** 这些码；改由 platform 菜单按钮挂载的 `platform.domain.*` 新码替代（若已有则切换绑定） |
| 首页判定 | 收敛后 global 角色快照不含这些码，不影响 `/platform/home` |
| 长期 follow-up | 统一 rename 为 `platform.domain.*` |

### D6. 快照合并算法（增强）

当前：`loadPermissionSnapshot` 按 **JWT role（单 effectiveRole）** 的 activeMenuScope 过滤 actions。

增强：

1. `listUserRoleCodesByClient` 取用户 **全部** 绑定角色（已有）。
2. `mergeRolePermissionActions(normalizedRoleCodes, …)` 内对每个 roleCode 查 scope，只 merge 与该 scope 匹配的 permission。
3. `loadPermissionSnapshot` 过滤 actions 时：若用户仅有 global 角色 → 仅 platform 前缀 + platform scope；若仅有 domain 角色 → 排除 platform.*。
4. **多角色并存**：合并各角色允许集（union），自然支持双控制台。

```java
// 伪代码
boolean allowPermission(RoleDefinition role, PermissionDefinition perm) {
  if ("global".equals(role.scope())) {
    return "platform".equals(perm.permissionScope())
        && perm.code().startsWith("platform.");
  }
  return !perm.code().startsWith("platform.");
}
```

（`domain.admin.*` 例外在 D5 中通过「不直授、仅 platform.* 菜单按钮」消化。）

### D7. 前端（最小变更）

权限包收敛后：

- `platform_admin` actions 仅 `platform.*` → `resolveHomePathFromActions` → `/platform/home`（无需改三元规则）
- 回归单测：platform_admin 快照、admin 登录路径
- `hasPlatformAccess` 不变

若 Flyway 后仍有漏网 non-platform 码，增加 **开发期断言日志**（可选），不在本 change 改首页规则。

---

## 数据流（收敛后）

```
admin 登录 (platform_admin)
    │
    ▼
user_global_role → platform_admin (global)
    │
    ▼
loadPermissionSnapshot
    ├── menuTree: platform scope only
    └── actions: platform.* only
            │
            ▼
resolveHomePathFromActions → /platform/home ✓
platformAccess → true ✓
```

```
domain_admin @ D1 登录
    │
    ▼
user_domain_role → domain_admin (domain)
    │
    ▼
loadPermissionSnapshot
    ├── menuTree: business only
    └── actions: domain.* / ticket.* (no platform.*)
            │
            ▼
resolveHomePathFromActions → /home ✓
platformAccess → false ✓
```

---

## Flyway 清单（草案）

| 序号 | 内容 |
|:---|:---|
| V202606…001 | 清理 platform_admin / super_admin(global) 跨界 `iam_role_permission` |
| V202606…002 | 清理 super_admin(global) 对 business scope 菜单的 role_menu_relation |
| V202606…003 | admin 账号改绑 platform_admin；清理冗余 staff_account_platform_role |
| V202606…004 | 按需为 platform_admin 补全 platform 菜单绑定（与 trim 后树一致） |

登记 `docs/architecture/database-increment-plan.md`。

---

## Risks

| 风险 | 缓解 |
|:---|:---|
| 移除 super_admin 业务直授后，某 API 仅认 super_admin 角色码 | 审计 `@RequirePermission` 与测试；平台能力应走 platform.* 码 |
| 双轨 `staff_account_platform_role` 与 IAM 不一致 | 本 change 文档标注 deprecated；读路径以 `user_global_role` 为准 |
| 历史 `domain.admin.*` platform 码 | D5 不直授 + 菜单按钮改用 `platform.domain.*` |

---

## Open Questions（design 阶段可细化）

1. global `super_admin` 是否 UI 禁止分配给普通用户（仅 seed break-glass）？
2. `platform_admin` 菜单范围 = trim 后全 platform 树，还是再裁子集？
3. `agent` 是否同步做 business-only 权限包审计（与 domain_admin 同模式）？
