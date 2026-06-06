# 数据库 Sprint 增量计划

| 文档版本 | 日期 | 说明 |
|:---|:---|:---|
| 1.1 | 2026-05-24 | 增加 §1.1/§1.2 基线快照与迁移备份（US-S0-07） |

> **L5 定位**：按 Sprint / Story 登记**将变更哪些表/列**；**不写 SQL**。实现见 Flyway（L7）。逻辑实体见 [`data-model.md`](./data-model.md)（L4）与 [`foundation-rules.md`](../product/foundation-rules.md)（L3）。

---

## 0. 文档分工（L3 / L4 / L5 / L7）

| 层级 | 文档/产物 | 回答的问题 | 写什么 | 不写什么 |
|:---|:---|:---|:---|:---|
| **L3** | foundation-rules | 规则与逻辑实体职责 | FR/DR/TR；逻辑字段名 | DDL、Flyway |
| **L4** | data-model | 逻辑实体关系与属性 | ER、命名、逻辑类型 | Flyway 快照 |
| **L5** | **本文档** | 本 Sprint 动哪些持久化对象 | 表/列增量条目 | SQL |
| **L7** | Flyway | 怎么落库 | `V*.sql` | 业务规则长文 |

**冲突裁决**：规则 → L3；叫什么 → L3+L4；本迭代动哪些列 → L5+L6 Story；库里实际有什么 → L7。

---

## 1. Flyway 策略（骨架期）

| 原则 | 说明 |
|:---|:---|
| 阶段内 | 只追加 `db/migration/current/V20260526xxx__*.sql`；菜单/权限 SQL **幂等** |
| 阶段边界 | 管理端骨架稳定后（**S1 末**建议）做一次 **rebaseline**，旧脚本移 `archive/` |
| 非每 Story rebaseline | 按 Epic/Sprint 阶段，避免迁移链过长 |
| volatile 数据 | 域 bootstrap 等用 **Bootstrap 服务**；Flyway 只放稳定菜单基线 |

**S0 不执行 squash rebaseline**（整链压缩进单文件并移 archive）。

### 1.1 基线参考快照（US-S0-07，非 Flyway）

| 项 | 说明 |
|:---|:---|
| 权威来源 | 联调库 `uniondesk` @ `127.0.0.1:30306`（与 [`application.yml`](../../UnionDesk/src/main/resources/application.yml) 一致） |
| 产出路径 | `docs/architecture/reference-schema/uniondesk_baseline_YYYYMMDD.sql` |
| 性质 | **只读参考**；Flyway **不**加载此路径 |
| 内容 | 全库 **schema** + **稳定种子**（排除 volatile 表数据；排除列表见 US-S0-07 / `.codex-tmp/GenerateBaseline.java` 思路） |
| 与 L7 关系 | `db/migration/current/` 版本链 **保留不动**；快照用于 drift 对照与新成员理解现库 |

### 1.2 迁移前备份（US-S0-07）

| 项 | 说明 |
|:---|:---|
| 时机 | 联调库上执行 Flyway migrate/validate、手工 DDL、或 US-S0-07 导出 **之前** |
| 工具 | `mysqldump` 全库（计划脚本落点 `UnionDesk/scripts/backup-db.ps1`，**待实现**） |
| 产出 | `UnionDesk/backups/uniondesk_YYYYMMDD_HHmmss.sql`（**不入 Git**） |
| 恢复 | 仅开发/联调；见 [`backup-restore.md`](../operations/backup-restore.md) §1.1.1 |

---

## 2. 已执行增量（登记）

| Flyway 版本 | Story/来源 | 主要变更 | 状态 |
|:---|:---|:---|:---|
| V202605200002 | rebaseline | 全库 schema + 菜单种子 | Done |
| V202605200003 | 业务域审计字段 | business_domain 审计列 | Done |
| V202605200004~006 | 平台域菜单 | iam_admin_menu 业务域目录/列表/详情 | Done |
| V202605210001 | 日志审计菜单 | 登录/操作日志菜单 | Done |
| V202605220001~002 | 菜单精简与图标 | 平台 5 大模块、按钮权限 | Done |
| V202605230001~002 | 登录日志统一 | login_log 统一 | Done |
| V202605240001 | 业务域描述 | business_domain.description | Done |
| V202605250001 | 入域双字段 | registration_enabled / invitation_enabled；DROP registration_policy | Done |
| V202605330001 | 域 admin 权限重命名 | platform.domain.list.read / create / control.read | Done |
| V202605330002 | US-S1-06 客户管理 | platform.domain.customer.* 三码 + 客户管理菜单 catalog/buttons | Done |
| V202605330003 | US-S1-06 客户管理 | 已有业务域详情菜单授权的角色补齐客户管理菜单与 API 权限 | Done |
| V202605340001 | identity-backend-prd-alignment | 身份核心表 DROP FK；staff/customer `login_name`→`username`；customer `display_name`→`nickname`；staff 增 `real_name`/`nickname`/`avatar_url`；domain_member 增 `domain_nickname` 等；回填与 domain_member_role 补录 | Done |

### 2.1 基线参考快照（US-S0-07）

| 日期 | 来源 | Flyway 最大 version | 文件 | drift |
|:---|:---|:---|:---|:---|
| 2026-05-25 | 联调库 uniondesk @ 127.0.0.1:30306 | 202605250001 | [`reference-schema/uniondesk_baseline_20260525.sql`](./reference-schema/uniondesk_baseline_20260525.sql) | 无：`current/` 12 个版本与 `flyway_schema_history` 一致，无 missing/orphan |

---

## 3. Sprint 增量计划（待填）

### Sprint 1（E1 管理端 Walking Skeleton）

| Story ID | 表/列变更（计划） | Flyway 版本 | 状态 |
|:---|:---|:---|:---|
| US-S1-05 | （无表变更，前端接 API） | — | Todo（S1 暂缓，2026-05-26） |
| US-S1-07 | iam 资源/权限码目录 JWT 路径 + 域用户 PUT/DELETE 路径 | 202605310001 | Done |
| US-S1-07 | 权限管理菜单改为 catalog（移除 /platform/permission） | 202605310002 | Done |
| US-S1-02 | super_admin 所有人语义；历史域 permission_item / iam_role_binding 补齐 | 202605320001 | Done |
| US-S1-08 | （无表变更，API 层） | — | Todo（S1 暂缓，2026-05-26） |
| **S1 末** | **rebaseline 评估** | TBD | 待定 |

### Sprint 2（E2 + 平台域详情 + UX）

| Story ID | 表/列变更（计划） | Flyway 版本 | 状态 |
|:---|:---|:---|:---|
| US-S2-UX-01 | 无 | — | Todo |
| US-S2-01 | 无表变更；权限 `platform.domain.control.entry` / `overview` / `general.update` / `general.delete`（`202605330004`～`202605330005`）；删除写 `updated_at`/`updated_by`/`deleted_at`（**不改 status**；无 `deleted` 列） | 202605330004、202605330005 | Done |
| US-S2-02 | 权限 `platform.domain.roles.*`（只读）；菜单「角色管理」 | TBD | Todo |
| US-S2-03 | 无表；`domain.member.status` API；成员按钮权限码 | TBD | Todo |
| US-S2-04 | 无（除非客户编辑扩列） | — | Todo |
| US-S2-05 | `blocked_word` 扩展；权限 `platform.blocked_word.*`、`platform.domain.blocked_word.*` | TBD | Todo |
| US-S2-06 | 权限 `platform.audit-logs.read`（若缺则 Flyway） | TBD | Todo |
| US-S2-E2-00 | `iam_admin_menu` business 菜单（按需） | TBD | Todo |
| US-S2-E2-01 | `ticket_type` 等（Stretch） | TBD | 延后 |

---

## 4. 维护规则

1. Story **开发前**在 §3 登记一行；**合并后**填 Flyway 版本号。
2. 逻辑字段先在 L3/L4 更新，再登记 L5，最后写 L7。
3. 不以 Flyway 脚本反写 L3 规则。
