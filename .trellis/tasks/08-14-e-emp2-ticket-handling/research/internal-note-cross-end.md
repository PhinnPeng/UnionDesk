# Research: 工单详情时间线跨端渲染与内部备注可见性（R2 跨端）

- Query: AdminWeb ticket-detail-drawer 如何渲染时间线；CustomerWeb detail.tsx 从哪些接口读历史、能否区分类型；回复 API 请求体字段；内部备注是否走 inbox_message；客户侧是否通过 ticket history 接口读到全部历史
- Scope: internal (mixed: 后端 + AdminWeb + CustomerWeb)
- Date: 2026-08-16

## Findings

### 1. AdminWeb 时间线渲染（ticket-detail-drawer.tsx，271 行）

- 数据源：`fetchTicketDetail(domainId, ticketId)`（`src/api/platform/ticket.ts` 行 159-161）→ `GET v1/admin/domains/{domain_id}/tickets/{ticket_id}`，返回 `TicketDetailResult { ticket, replies, history, watcherStaffAccountIds }`
- **drawer 只用 history，完全不用 replies**（`ticket-detail-drawer.tsx` 行 96-97 只 setHistory；行 203-219 Timeline 渲染 history items）——回复正文在 AdminWeb 时间线里不显示，只看「回复」动作标签
- 分组现状：`filterHistory`（行 49-57）按 `action !== "reply"` 切「全部/只看日志/只看评论」三个 Tabs（行 189-198）——**这就是现成的「分组」雏形**，R2 需扩展为「公开回复/内部备注/系统操作」三组
- 动作中文映射 `ACTION_LABELS`（行 16-24）：create/claim/assign/reply/status_change/merge/close；**无 withdraw**（drawer 不显示撤回动作）
- 实时刷新：订阅 `REALTIME_EVENT.TICKET_REPLIED / TICKET_UPDATED`（行 119-136）
- 旧详情页 `pages/domain/ticket-queue/detail.tsx`（590 行，路由遗留页）也用 `fetchTicketDetail` + Timeline，含回复表单（replyPresets 行 31-38）与 `sla-display`；新交互已迁往 drawer，此页仍存在（commit 89358ab 后点击编号不再跳转）

### 2. CustomerWeb 数据流与可见性（detail.tsx，347 行）

- 数据源：`getCustomerTicketLive(ticketId)`（`packages/shared/src/customer-portal-live.ts` 行 261-281）→ `getCustomerMyTicketDetail(domainId, ticketId)`（`packages/shared/src/api.ts` 行 2849-2862）→ `GET v1/domains/{domain_id}/tickets/my/{ticket_id}`
- **客户侧只渲染 `ticket.replies`**（detail.tsx 行 226-244「公开动态」ul-timeline，`[...ticket.replies].reverse()`，显示 authorName/authorType/content），**不渲染 history**
- `CustomerTicketDetail` 类型只有 `{ ticket, replies }`（api.ts 行 2823-2826）——前端层面客户看不到 history
- 回复提交：`replyCustomerTicketLive(ticket.id, content, version)`（detail.tsx 行 172）→ 后端 `replyCustomerTicket`（`TicketService` 行 422-431）→ 复用 `replyTicket`，权限 `ticket.reply.self`

### 3. 关键风险：客户接口原样返回 history（后端泄漏面）

`getCustomerTicketDetail`（`TicketService` 行 415-420）＝ `requireCustomer` + `requireTicketOwner` + **直接返回 `getTicketDetail`**（行 405-413）＝ `TicketDetailResult(ticket, replies, history, watchers)`。即：

- **客户 API 响应体实际包含完整 history**（含 claim/assign/status_change/merge 及 payload 明细），只是 CustomerWeb 前端未渲染
- R2 若把内部备注写入 `ticket_history`（新 action），**不经后端过滤就会出现在客户 API 响应中**（AC3 风险）
- 同理若内部备注写入 `ticket_reply`（新 reply_type），也会出现在客户 API 的 replies 里（`listTicketReplies` 行 518-523 不过滤类型）

**结论**：内部备注无论落在哪张表，都必须在 `getCustomerTicketDetail`（或 `getTicketDetail` 加客户视角参数）处过滤，前端渲染与否不能作为安全边界。

### 4. 回复 API 请求体

- Admin：`POST /admin/domains/{domain_id}/tickets/{ticket_id}/replies`（`TicketController` 行 157-164，权限 `ticket.reply`）体 `ReplyTicketCommand(long version, String content, Long quickReplyTemplateId, List<Long> attachmentIds)`（`TicketService` 行 1117-1122）；前端 `replyAdminTicket`（ticket.ts 行 163-168）
- Customer：`POST /domains/{domain_id}/tickets/my/{ticket_id}/replies`（行 65-72，权限 `ticket.reply.self`）同一命令体
- `replyType` 服务端推导：`quickReplyTemplateId == null ? "text" : "quick"`（行 305），写入 `ticket_reply.reply_type`；`senderType` 由角色推导 customer/staff/system（行 653-658）

### 5. 内部备注是否需走 inbox_message（站内信）——不需要

- `inbox_message`（`InboxMessagePo.java`）字段：notification_log_id / recipient_subject_id / portal_type / business_domain_id / title / content / jump_url / is_read / readAt —— 是**通知收件箱**（收件人为 subject，带已读状态），语义是「通知某人」，不是工单时间线的讨论内容
- 站内信由 `NotificationCenterService` 统一写（`notifyTicketCreated/StatusChanged/Reply/Merged/SatisfactionInvite`，行 55-126），对应客户/员工通知场景；内部备注是工单处理上下文，不应混入收件箱
- 结论：内部备注落 `ticket_reply`（扩展 reply_type）或 `ticket_history`（扩展 action）即可，**不引入 inbox**

## Caveats

- AdminWeb drawer 无回复输入框（旧 detail.tsx 有）——若 R2 在 drawer 加「内部备注」输入，需确认交互入口放在 drawer 还是旧详情页（drawer 是当前主交互，见 commit 89358ab）
- 客户 API 泄漏 history 是既有行为（非本次引入），但内部备注上线前必须过滤；建议同时在 `CustomerTicketDetail` 后端投影中彻底去掉 history 字段（客户前端本就不用）
- `filterHistory` 的「只看评论」= action==="reply"，若内部备注新增 action/类型，需同步扩展该过滤逻辑与 ACTION_LABELS
