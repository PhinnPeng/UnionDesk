## Context

**权威设计：** [`docs/superpowers/specs/2026-06-06-identity-scheme-a-design.md`](../../../docs/superpowers/specs/2026-06-06-identity-scheme-a-design.md)  
**基准 DDL：** [`V202605200002__rebaseline_current_schema.sql`](../../../UnionDesk/src/main/resources/db/migration/current/V202605200002__rebaseline_current_schema.sql)

本文档仅记录 **架构决策索引**；**数据结构（§4）**、迁移、服务说明见 Superpowers Design Doc。

## Goals / Non-Goals

**Goals：** L0 归一；L1 staff/customer 合一；L2 `domain_member.domain_*`；停写 user_account；域角色单轨；身份域去 FK + 业务校验。

**Non-Goals：** profile 拆表；DROP user_account；客户域级昵称；主体合并 API；下游表 DROP FK。

## Decisions

| ID | 决策 |
|:---|:---|
| **D1** | 逻辑 L0/L1/L2 映射：`identity_subject` → `staff_account`/`customer_account` → `domain_member`/`domain_customer` |
| **D2** | 登录只查 L1 单表；会话 `user_id` = 账号表主键 |
| **D3** | 展示更新不 bump `auth_version` |
| **D4** | 停写 `user_account`；域 RBAC 只写 `domain_member_role` |
| **D5** | Flyway 340001：DROP 身份域 FK + ADD 列 + 回填 + 角色补录 |
| **D6** | 引用完整性：逻辑引用 + UK/IDX；写路径 Service 校验；不新增 DB FK |
| **D7** | `merged_into_id`：只读 `resolveEffectiveSubjectId`；禁止向已合并主体挂新账号；合并 API 后续 Story |
| **D8** | 数据结构说明集中在 Superpowers **§4**（按表 + 分组 + 340001 增量；§4.8 API 出参） |
| **D9** | 命名对齐：**real_name**（真实姓名）、**nickname**（昵称）；API JSON = DB 列名（snake_case） |
| **D10** | staff：`real_name` + `nickname`；domain：`domain_nickname`；customer：`display_name` RENAME → `nickname` |
| **D11** | 域内对外 nickname 链：`domain_nickname` → `staff.nickname` → `real_name` → `username`；`real_name` 不域覆盖 |
| **D12** | 登录账号：`login_name` RENAME → `username`（staff/customer）；API 与 DB 对齐；`login_log` 等审计表不在本次 rename |

## Migration Plan

340001：DROP FK → ADD 列 → 回填 → 角色补录 → Java 切换 → 冒烟

## Risks

| 风险 | 缓解 |
|:---|:---|
| staff 凭证+展示同表 | update 方法分离 |
| 去 user_account fallback | 迁移前 staff 覆盖率 100% |

## Open Questions

域内展示更新：并入 `DomainMemberService` 的 member update API（不单独 PUT profile）。
