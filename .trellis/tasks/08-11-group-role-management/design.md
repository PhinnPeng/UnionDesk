# Design — 集团统一角色与跨域批量权限管理（模板 + 实例 + 锁定字段 + 同步策略）

> 依据：prd.md（R-A~R-D、D1–D9）+ research/group-role-management-model.md（证据链）。方案 D「平台只读」目标态已作废。

## 1. 目标架构

**模型 3：统一模板 + 域实例 + 域级覆盖 + 同步策略**。平台创建 `role_template`（含权限包）→ apply 到选定多个业务域 → 各域生成 `domain_role` 实例；实例继承模板，域内可微调**锁定字段之外**的字段；模板变更按同步策略传播。

职责矩阵（D9）：平台 = 统一管人 + 模板创建/下发/同步 + 跨域批量停用（step-up）+ 审计监管；域 = 成员日常运营 + 实例微调（锁定字段除外）+ 域内业务自治。

## 2. 边界与落点

- **实例落点 = 轨 A（domain_role 族）**：成员绑定（`domain_member_role`）与权限生效均走现有路径，模板模型与现状同构（research §3.1）
- **轨 B（IAM role(scope=domain)）冻结**（D5）：新角色走模板/域端；旧角色只读保留
- **跨域写一律逐域鉴权**（`hasPermissionForDomains`），P0 安全债完成前模板/批量 API 不开放（D8）

## 3. 数据模型（P1 建表，Flyway 新版本号）

| 变更 | 表 | 关键列 | 约束 |
|:---|:---|:---|:---|
| 新增 | `role_template` | `id, code, name, description, sync_strategy('immediate'/'manual'/'none'), locked_fields(json 默认 ["permissions"]), preset, created_by, created_at, updated_at` | `code` 唯一 |
| 新增 | `role_template_permission` | `id, template_id, permission_item_id` | 唯一 `(template_id, permission_item_id)`；复用 `permission_item` 目录（与 `domain_role_permission` 同构） |
| 新增 | `role_template_domain` | `id, template_id, business_domain_id, instance_domain_role_id, sync_mode, applied_at, updated_at` | 唯一 `(template_id, business_domain_id)` |
| 扩展 | `domain_role` | 增 `template_id`（可空 FK）、`template_version`(int)、`locked_fields`(json) | 非模板角色三列均为空，行为不变 |
| 不变 | `domain_role_permission` / `domain_member_role` / `staff_account` / `domain_member` | — | apply 时批量写实例权限；成员绑定复用 `StaffAccountService.bindDomainMemberships` |

**事务边界**：apply/批量停用按**逐域事务**（TR-04 部分成功），不做整批回滚。

## 4. 权限模型

- 模板权限码 = `permission_item`（轨 A 目录）；实例权限 = 继承模板权限包
- **覆盖规则**：`locked_fields` 内字段域端修改 → 403 + 中文（默认锁定「权限包」；「名称/成员」域端可微调）
- **同步传播**：
  - `immediate`：模板权限变更自动下发各实例；被域覆盖项跳过并记录（审计可查）
  - `manual`：平台手动 `sync`；域端展示「落后 N 版本」漂移提示（`template_version` 对比）
  - `none`：一次性模板，创建后与实例解耦（承认漂移）
- **满额域**（每域自定义角色 ≤20，foundation-rules §3.2）：apply 前校验余量，满额域跳过 + 中文提示（部分成功）
- **新权限码**：`platform.role_template.{read,create,update,delete,apply,sync}`、`platform.user.domain_batch_status`（2026-08-12 决策：原 `platform.staff.*` 改名，全仓员工码统一 `platform.user.*`）

## 5. API 契约

```text
# 模板
POST /api/v1/iam/role-templates                      # {code,name,permission_item_ids,locked_fields,sync_strategy}
POST /api/v1/iam/role-templates/{templateId}/apply   # {domain_ids:[...], sync_mode:'immediate'}
POST /api/v1/iam/role-templates/{templateId}/sync    # {domain_ids?:[...]}（manual 触发）
POST /api/v1/iam/role-templates/{templateId}/unapply # {domain_ids:[...]}（解绑实例 → 转独立角色）
POST /api/v1/iam/role-templates/{templateId}/bind-members  # {staff_ids:[...], domain_ids:[...]}（复用 bindDomainMemberships）
# 跨域批量停用（step-up 二次认证，高危；令牌经 validateStepUpToken 真实校验，含存量 updatePlatformRoles 同规修复）
POST /api/v1/admin/staff/{staffId}/domain-members/batch-status
     # {domain_ids:[...], status:'disabled'} → {success:[domainId...], failed:[{domainId,reason}...]}
```

- 鉴权：全部经 `hasPermissionForDomains(operator, code, domainIds)` 逐域校验（P0-① 落地后启用）
- 错误语义：锁定字段 403+中文；满额域跳过；批量部分成功返回逐域结果；审计逐域写 N 行（`business_domain_id` + `detail` 记录请求域集与逐域结果）

## 6. 审计设计（P0-②）

- 扩展 `AuditLogSemanticsListener` + `AuditActionCodes`：域成员增删/改角色、域角色 CRUD、模板 apply/sync/unapply、跨域批量停用
- 批量操作逐域写 `audit_log` 行；**操作入口端点列 `point`**（值域 platform/domain，未来可扩展 customer/api；可空默认 null 兼容存量 `business_domain_id=0L` 写入；2026-08-12 Q3 决策：用户提议定名，语义=操作入口端点，区别于接口 endpoint）随审计补齐一并落地

## 7. UI 落点

| 页面 | 改动 |
|:---|:---|
| F4.9 平台角色页 | 新增「模板」Tab：模板列表/创建/推送选域 Modal/同步按钮/漂移状态列 |
| F3.13 域端角色页 + F3.3 | 实例行「模板来源 + 版本」徽标；锁定字段只读禁用；非锁定可编辑（后端校验锁定） |
| F4.7 用户管理 / F4.3 离职池 | 行内「跨域批量停用」入口：选员工 → 选域集 → step-up 确认 → 部分成功摘要（复用 `detail-members.tsx` rowSelection 模式） |

## 8. 实施阶段（与 prd.md 迁移路径一致）

| 阶段 | 内容 | 依赖 | 验收要点 |
|:---|:---|:---|:---|
| **P0-① 安全债** | US-S1-08 目标域校验：`RequirePermissionInterceptor` 透传 `targetBusinessDomainId`，service 层 `requireDomain` 校验操作人授权域集 | — | A 域管理员操作 B 域 → 403 + 中文；平台管理员批量跨域通过 |
| **P0-② 审计补齐** | listener + action codes + 操作入口端点列 `point`；**step-up 真实校验接通**（`validateStepUpToken` → 存量 `updatePlatformRoles` + 新 batch-status） | — | 成员/角色/批量操作逐域落审计，可区分操作端；step-up 令牌真实校验生效 |
| **P1-1 模板层** | 三表建表（Flyway）；模板 CRUD + apply/sync/unapply/bind-members 服务（复用 `DomainRoleService.createRole` + `bindDomainMemberships`）；F4.9「模板」Tab | P0-① | 一次 apply 多域生成实例；锁定字段生效；满额域跳过 |
| **P1-2 批量停用** | `batch-status` API + F4.7 跨域入口（step-up） | P0-① | 多域部分成功语义；逐域审计 |
| **P2 域端展示** | F3.13/F3.3 模板徽标、锁定只读、漂移提示 | P1-1 | 域端不可改锁定字段；manual 漂移可见 |
| **P3 后置** | 双轨治理落地、`immediate` 异步队列、模板版本历史 | — | 不阻塞 MVP |

## 9. 兼容性与回滚

- 新增表/列均可空、非破坏；既有 API 与页面行为不变（模板功能叠加）
- 回滚：Flyway 反向迁移 + 权限码回授；P0-① 独立可回滚（拦截器开关）
- 漂移缓解：`template_version` + 漂移状态列 + manual 冲突摘要

## 10. 风险与对策

| 风险 | 对策 |
|:---|:---|
| R1 跨域越权（放大） | P0-① 硬前置，未完成不开放模板/批量 API |
| 模板漂移 / 覆盖冲突 | 锁定字段拒绝；immediate 被覆盖项跳过并审计；manual 冲突摘要 |
| 满额域冲突 | apply 前余量校验，跳过 + 提示（上限放宽另议） |
| ≤20 上限当前无实现校验 | 模板 apply 与域端创建双入口统一补数量校验（P1 内） |
| `iam_role_binding` 遗留表 | 本设计不依赖（research §7 确认） |

## 11. 待确认（已按默认值锁定，实施时如有偏差再议）

- 同步策略默认 `immediate`（D2）；锁定默认仅「权限包」（D3）；满额跳过（D4）；双轨冻结（D5）；先手选域（D6）；批量停用 step-up（D7）
