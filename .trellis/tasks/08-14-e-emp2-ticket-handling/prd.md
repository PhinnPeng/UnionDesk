# E-EMP2 工单处理中心增强

> 2026-08-14 立项，`08-14-employee-handling-chain` 子任务（P1）。对应 PRD F2.1。
> 2026-08-16 勘察（`research/` 5 篇）+ grill-with-docs 拍板 4 项决策（见「已确认决策」），方案细化到实现级。

## Goal

补齐工单处理中心的两项增强：**批量领取/批量关闭** + **内部备注与时间线分组**。当前工单队列/详情单行动作已通（领取/指派/回复/关闭/合并），但缺批量端点和内部备注/时间线分组完善。

## 现状（勘察核实，research/ 各篇有 file:line 依据）

- 单行动作端点齐全：`TicketController` claim:117 / assign:126 / replies:144 / status:153 / merge:171；**无批量端点**
- 单点 claim 天然幂等：`updateClaim` SQL（TicketMapper.xml:301-315）`WHERE assigned_to IS NULL AND status IN ('open','new')`，已领取/非待处理必然 updated=0；SLA 首响随领取记录（`TicketService` 行 255）
- 单点 close 依赖 `version=?` 乐观锁（`updateStatus`，TicketMapper.xml:280-299），状态机 `isAllowedStatusTransition`（行 822-832）禁止终态（closed/withdrawn/merged）再流转
- 代码库批量先例：`StaffDomainMemberBatchController`（逐项 try-catch 部分成功 + 失败中文原因 + 逐项审计）、`BlockedWordService.createBatch`（单事务去重跳过 + 上限 200）
- `TicketHistoryPo` 仅 11 字段，**无 note/分组字段**；回复正文不进 history（payload 只有 reply_id/reply_type）；`ticket_reply` 有 content 列、`reply_type` 仅 text/quick（行 305 写死）
- **客户详情接口泄漏**：`getCustomerTicketDetail`（行 415-420）原样返回 `TicketDetailResult(ticket, replies, history, watchers)`——客户 API 实际含完整 history，仅前端未渲染
- 交互现状：抽屉（主交互）只有领取/指派按钮，无回复表单；公开回复入口在旧详情页 `detail.tsx`（路由 `/domain/ticket-queue/:ticketId` 仍注册）；抽屉时间线已有「全部/日志/评论」Tabs 雏形（`filterHistory` 按 `action==="reply"` 切分）
- 权限：`ticket.claim`/`ticket.close` 已作为工单队列菜单按钮码绑定 domain_admin/agent（V20260813160000），**批量按钮直接复用，无需新增权限**
- `replyTicket` 状态拦截仅 merged/withdrawn（行 288-291）→ **已关闭工单可追加回复/备注**（合理：关闭后补内部备注）
- 测试在 uniondesk-app（86 文件，ticket 13 个；TicketServiceTests 等 3 个 @Disabled 不阻塞）；`mvnw -o test-compile -pl uniondesk-app -am` exit 0

## 已确认决策（2026-08-16 用户拍板）

| # | 决策点 | 结论 |
|---|---|---|
| D1 | 批量端点形态 | **后端批量端点**（非前端循环）：`POST .../tickets/batch-claim` + `batch-close`，部分成功语义 |
| D2 | 内部备注落点 | **ticket_reply 扩展 `reply_type="internal_note"`**（非 ticket_history）——正文有 content 列、作者/时间/附件语义完备，与公开回复同表同 API |
| D3 | 客户侧收口 | **客户详情接口仅返回公开 replies**：过滤 internal_note 且移除 history 字段（顺带修复既有泄漏） |
| D4 | 交互入口 | **抽屉新增「内部备注」输入 + 时间线三组**（公开回复/内部备注/系统操作）；公开回复发送入口仍在旧详情页（不迁移，控制范围） |

## Requirements

### R1 批量领取/批量关闭

**后端（uniondesk-ticket）**

- 新端点（`TicketController`）：
  - `POST /admin/domains/{domain_id}/tickets/batch-claim`，权限 `@RequirePermission(ticket.claim)`
  - `POST /admin/domains/{domain_id}/tickets/batch-close`，权限 `@RequirePermission(ticket.close)`
  - 入参 `BatchTicketIdsRequest(List<Long> ticket_ids)`——**不带 version**（跨页多选版本过期；幂等由 SQL 条件/后端读行保证）
  - 返回 `BatchTicketActionResult { List<Long> success, List<BatchTicketFailure failed }`，`failed` 含 `ticketId + 中文 reason`（仿 `DomainMemberDtos.BatchStatusResult`）
- 实现：整体**不包 @Transactional**（否则单条失败回滚全部，违背部分成功）；逐项复用单点 service（各自事务）
  - 批量领取 → 复用 `claimTicket` 语义（SQL 互斥天然幂等）；updated=0 记失败「已被他人领取或状态不允许领取」
  - 批量关闭 → 后端读行 + `isAllowedStatusTransition → closed` 校验（仅 processing/resolved 可关；open/new/终态失败并返回原因）
  - 每项照走 history + audit + SLA + 通知链路（复用单点路径，无特殊分支）
- 上限：单次最多 100 个（对齐 `SlaScanJob.BATCH_LIMIT`，防超长事务链）

**前端（ticket-queue/index.tsx）**

- Table 加 `rowSelection`（`selectedRowKeys` 受控，仿 `platform/user/index.tsx` 行 116/478-484）
- Card `extra` 加两个批量按钮（仿 platform/user 行 458-467）：「批量领取」`auth="ticket.claim"`、「批量关闭」`auth="ticket.close"`，均 `disabled={selectedRowKeys.length === 0}`
- 不做按操作切换的行禁用（两个按钮允许勾选的状态集不同，引入操作模式切换反而复杂）；失败项由后端原因在结果提示中展示，符合 AC1「失败项提示、其余成功」
- 结果反馈：全部成功 → `message.success`；有失败 → 弹窗/Message 列出失败项与原因（仿 `BatchDisableModal` 失败展示形态）

### R2 内部备注 + 时间线三组

**后端（uniondesk-ticket）**

- `ReplyTicketCommand` 扩展 `Boolean internal`（默认 false）；`replyTicket` 内：
  - `internal == true` → `reply_type = "internal_note"`；history `action` 仍记 `"reply"`（payload 含 `reply_type=internal_note`，与公开回复可区分）
  - 内部备注副作用裁剪：**不记 SLA 首响**（`slaService.recordFirstResponse` 跳过）、**不通知客户**（`notificationCenterService.notifyTicketReply` 跳过）、**不发 `TicketRepliedEvent`**（客户实时订阅 ticket.replied，不能收到；`RealtimeEventListener` 也仅 `publishToUser(ACTOR_CUSTOMER)`，员工端域级广播属 P3 未做——内部备注不新增任何推送，与其他员工端动作同等对待，抽屉提交后经 `handleActionDone` 自刷新）
  - customer 路径（`replyCustomerTicket`）强制 `internal=false`（服务端兜底，防客户传 internal 探内部通道）
- 客户侧收口（D3）：`getCustomerMyTicketDetail` 不再复用 `getTicketDetail` 全量返回，改为构建客户投影：`ticket + 仅公开 replies（reply_type != 'internal_note'）`；响应去掉 history/watchers（`CustomerTicketDetail` 前端类型本无 history 字段，无感知）
- 无 DB 迁移（reply_type 是开放字符串，history 结构不变）

**前端（ticket-detail-drawer.tsx）**

- 时间线 Tabs 由「全部/日志/评论」改为三组：**公开回复 / 内部备注 / 系统操作**
  - 公开回复：replies 中 `reply_type ∈ (text, quick)`，显示正文/作者/时间
  - 内部备注：replies 中 `reply_type === "internal_note"`
  - 系统操作：history（排除 `action === "reply"`，避免与公开回复重复渲染）；`ACTION_LABELS` 补 `withdraw`（现状缺失，三组化后系统操作需完整动作文案）
  - 时间线数据源由纯 history 改为 `replies + history` 合并（`TicketDetailResult` 已同时返回两者，drawer 现仅用 history）
- 抽屉底部新增「内部备注」输入框（TextArea + 发送，`auth="ticket.reply"` 包裹）→ `replyAdminTicket(..., { internal: true })`；发送后刷新详情与父列表
- 公开回复发送入口不动（旧详情页 `detail.tsx` 保持现状）

## Acceptance Criteria

- [ ] AC1 多选一键批量领取：全成功；混有「已被他人领取/非待处理」时失败项中文原因提示、其余成功，无部分状态错乱
- [ ] AC2 多选一键批量关闭：processing/resolved 成功；open/new 与终态（closed/withdrawn/merged）失败并提示（TR-02 不回归）
- [ ] AC3 内部备注写入 ticket_reply（reply_type=internal_note），**客户详情接口看不到**（过滤 + 不推送 + 不记 SLA 首响）
- [ ] AC4 抽屉时间线按公开回复/内部备注/系统操作三组展示，内容与动作文案正确
- [ ] AC5 权限不足时批量按钮按 `AuthGuarded` 隐藏；uniondesk-app 现有 12 个启用 ticket 测试不回归
- [ ] AC6（新增）uniondesk-app 集成测试：批量部分成功语义（仿 `StaffDomainMemberBatchIntegrationTest`）+ 客户详情不含内部备注/history（仿 `CustomerTicketPermissionIntegrationTest`）

## Out of Scope

- 状态机接入（归 E-EMP3）
- SLA 高亮（归 E-EMP3）
- 公开回复表单迁移进抽屉（保持旧详情页入口）
- 内部备注的编辑/删除（无此能力，只能追加）

## 参考

- 勘察：`research/batch-endpoints.md`、`research/ticket-history-model.md`、`research/internal-note-cross-end.md`、`research/permissions-frontend.md`、`research/test-infra.md`
