# SLA 配置优化 执行计划（implement）

> 配套：`prd.md`（需求/AC）、`design.md`（技术设计）、`docs/architecture/adr-sla-two-tier-config.md`（决策 v1.3）。
> 执行方式：多子代理分批并行实现（只改不提交），主代理统一验证/提交/归档。

## 变更边界

### 后端（uniondesk-support/sla + uniondesk-ticket + iam + app）

| 文件 | 改动 |
|:---|:---|
| `sla/core/SlaService.java` | `loadPolicy` 两段式（域内→全局兜底）；`evaluateTicket` 幂等重构：终态 stopped 跳过、违约消除自愈 tracking、违约时按 `sla_breach_actioned` 一次性执行动作（按序升级/换处理人/加关注人）→ `breached`（旧键 `sla_status` 兼容覆盖） |
| `sla/repository/SlaRepository.java` + `sla/mapper/SlaRuleMapper.xml` | 新增 `findGlobalPolicy`（`business_domain_id IS NULL ORDER BY id DESC LIMIT 1`）、`updateSlaStatus`、`markBreachActioned`；`selectPolicy` 不动 |
| `sla/core/SlaScanJob.java`（新） | `@Scheduled(cron="0 * * * * *")` + `Clock`；候选=`tracking` 且任一时限已过未完成，LIMIT 100 分批调 `evaluateTicket`；单条失败隔离 |
| `sla/web/PlatformSlaController.java`（新） | `/api/v1/admin/platform/sla-rules` GET/POST/PUT/DELETE；`@RequirePermission(PLATFORM_SLA_*)`；创建/更新校验类型/优先级/日历必空 |
| `iam/core/PermissionCodes.java` | 注册 `platform.sla.{read,create,update,delete}` |
| `uniondesk-ticket/.../mapper/ticket/TicketMapper.xml` | 终态统一：`resolved/closed/withdrawn/merged → 'stopped'`（移除 `→ 'resolved'` 分支） |
| `uniondesk-support/.../mapper/sla/SlaTicketMapper.xml` | `updateResolution` 置 `'stopped'`（原 `'resolved'`） |
| `uniondesk-app/.../db/migration/current/V2026081612xxxx__sla_config_optimization.sql`（新） | `sla_rule` DROP FK + `business_domain_id` 可空；`ticket.sla_breach_actioned` 新列；`ticket.sla_status='resolved' → 'stopped'`；平台权限码菜单/角色种子（幂等，code 为键） |
| 测试 `uniondesk-app/src/test/java/com/uniondesk/sla/...` | 匹配链（全局兜底/事项优先）、动作（按序升级/换人/加关注）、幂等/状态机（违约→breached、首响后自愈 tracking、终态统一 stopped、再超时不重复动作）、扫描任务（Clock）、平台端点校验 |

### 前端（UnionDeskAdminWeb）

| 文件 | 改动 |
|:---|:---|
| `packages/shared/src/api.ts` | 新增 `fetchGlobalSlaRules/createGlobalSlaRule/updateGlobalSlaRule/deleteGlobalSlaRule`（`{total, items}` 解包约定） |
| `src/pages/domain/sla/index.tsx` | 规则编辑器易用化：类型/优先级 Select（allowClear）、日历 Select disabled+提示、动作可视化块（按序升级 Switch、换处理人单选、加关注人多选 member-picker）、JSON 高级模式折叠双向同步；抽离 `SlaRuleForm` 私有子组件 |
| `src/pages/platform/sla-management/index.tsx` | Segmented「全局默认规则/业务域代管」；全局区列表+编辑器（无类型/优先级/日历）；代管区复用 `SlaRuleForm` |
| `src/pages/domain/ticket-queue/detail.tsx` | 移除「关闭」按钮与关闭确认 Modal 入口（保留「变更状态」） |
| `src/pages/domain/ticket-queue/index.tsx` | 移除「关闭」行操作/批量关闭入口 |
| `src/pages/domain/ticket-queue/sla-display.ts` | `slaStatusMeta` 三态：移除 resolved，stopped 展示「已结束」 |

## 执行顺序

1. 后端（B 子代理）→ 校验：`mvnw.cmd -pl uniondesk-app test`（SLA 相关）
2. 前端表单（F1 子代理）与关闭移除（F2 子代理）并行 → 校验：`pnpm run typecheck:admin` + lint
3. 主代理联检：全量相关测试 + 冒烟（数据库迁移执行、SLA 规则 CRUD、超时动作）
4. 提交（仅本任务文件）+ 归档

## 明确不做（边界外）

- 工作日历计算、可配置工作流运行时接入（E-EMP3）、「即将超时」阈值、通知模板、SLA 暂停/恢复、`ticket.close` 权限清理、动作编排顺序自定义
- 不重构无关代码；临时文件仅放 `agent-work/`

## 验证计划

- 后端：`.\mvnw.cmd test`（`uniondesk-app` 全量，重点 `com.uniondesk.sla.*` 与 `TicketWorkflowTests`/FR 测试）
- 前端：`pnpm run typecheck:admin`、`pnpm run lint:admin`（无新增 error）
- 数据库：本地 30306 执行迁移，验证全局规则行（business_domain_id NULL）与 `sla_breach_actioned` 列
