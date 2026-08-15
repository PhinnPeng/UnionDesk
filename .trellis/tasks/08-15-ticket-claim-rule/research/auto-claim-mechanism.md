# 研究：工单自动领取机制（auto-claim-mechanism）

> 研究时间：2026-08-15｜执行：@trellis-research 子代理｜结论已回填 PRD（.trellis/tasks/08-15-ticket-claim-rule/prd.md），用户确认更名「领取规则」

## 结论摘要

1. **触发方案**：MVP = 客户提单事务内同步触发（try-catch 包裹，失败仅日志不阻断提单）；阶段二 = 定时扫描兜底（@EnableScheduling，未领取超时重试，条件 UPDATE 幂等）。项目无 MQ/定时基建，域成员规模小，SLA 首响要求即时。
2. **领取人策略**：MVP 主推 least_loaded（候选池内受理未完结工单最少，并列取最近分配久者）+ fixed（指定人，失效则跳过记日志）；增强 online_first（auth_login_session 活跃信号）/ round_robin。
3. **规则模型**：新表 `ticket_claim_rule`（原 auto_claim_rule，更名）：business_domain_id + name + enabled + match_ticket_type_id(NULL=全部) + match_priority_level_id(NULL=全部) + strategy + assignee_staff_account_id + grace_minutes。匹配完全对齐 SlaRuleMapper.selectPolicy（具体度优先、同度取 id 大）。
4. **配置入口**：事项配置页 sider 第四段（与类型/属性/状态并列），改动最小、不新增菜单；备选独立菜单挂功能配置。
5. **权限**：新增 domain.ticket_claim_rule.read/create/update/delete 4 码（照 domain.sla.* 先例），domain_admin 全量 + agent 只读（V20260813140000 幂等补全模式）。
6. **执行**：复用 updateClaim（乐观锁）+ recordHistory(claim, context=null, payload auto:true) + SLA 首响落库（受理即首响语义）。
7. **边界**：无候选/全部禁用→未领取+日志；指定人失效→跳过；并发靠乐观锁；同步失败降级。
8. **风险**：① 自动领取=SLA 已受理（需业务拍板，建议计入）；② claim 现状无「未指派/终态」前置校验，已指派工单可被再领取覆盖（建议修复，影响 EMP2 批量领取需联动回归）；③ 优先级两套口径（ticket.priority 存 code vs sla_rule 存 level_id）——新表与 selectPolicy 同构避免第三套；④ 无定时基建（阶段二新增）；⑤ 多规则命中歧义需固化排序并文档化。

## 关键文件索引

- TicketService.claimTicket:227-246 / createTicketForCustomer:130-191 / recordHistory:732-743
- TicketMapper.xml updateClaim:265-278
- SlaRuleMapper.xml selectPolicy:92-104（匹配模板）
- SlaController.java:20-65（CRUD 先例）
- V20260813140000__domain_sla_menu.sql（权限/角色种子先例）
- pages/domain/ticket-config/index.tsx（sider 三段结构:30-40、entryAuth:83-87）
- pages/domain/sla/index.tsx（规则页 UI 先例）
- api/platform/sla.ts:50-68（CRUD 客户端先例）
