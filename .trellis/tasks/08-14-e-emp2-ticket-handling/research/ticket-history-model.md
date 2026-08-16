# Research: TicketHistory 实体字段 / 写入点 / 时间线查询（R2）

- Query: TicketHistoryPo/TicketHistory 实体字段（是否有 note/内部标志/分组字段）、listTicketHistory 实现、history 写入点（回复/状态变更/领取/关闭/合并各写什么）、时间线查询 SQL
- Scope: internal
- Date: 2026-08-16

## Findings

### 1. 实体字段（TicketHistoryPo）——无 note / 无内部标志 / 无分组字段

`uniondesk-ticket/src/main/java/com/uniondesk/ticket/entity/TicketHistoryPo.java`（11 字段）：

| 字段 | 类型 | 说明 |
|---|---|---|
| id | long | 自增 |
| ticketId | long | |
| businessDomainId | long | |
| action | String | 动作码（create/claim/assign/reply/status_change/merge/withdraw/watchers_replace） |
| fromValue | String | 旧值（状态/受理人） |
| toValue | String | 新值 |
| operatorSubjectId | Long | 操作人（identity_subject.id，可空=系统） |
| operatorActorType | String | customer/staff/system |
| payload | String(JSON) | 动作明细 |
| createdAt | LocalDateTime | |

**无 `note`/`visibility`/`category`/`group` 字段**。表结构（`uniondesk-app/src/main/resources/db/migration/current/V202605200002__rebaseline_current_schema.sql` 行 1040-1058）同字段。当前「分组」只能靠 `action` 推断（前端 drawer 已用 `action==="reply"` 区分评论/日志）。

### 2. listTicketHistory 实现

- `TicketService.listTicketHistory`（行 526-530）：`ticketHistoryRepository.findByTicketIdAndDomainId(ticketId, businessDomainId)` → `toTicketHistoryRow`（行 1016-1026，含 action/fromValue/toValue/operatorSubjectId/operatorActorType/payloadJson/createdAt）
- 查询 SQL（`mapper/ticket/TicketHistoryMapper.xml` 行 31-44）：`WHERE ticket_id=? AND business_domain_id=? ORDER BY created_at ASC, id ASC`，`CAST(payload AS CHAR) AS payload_json`
- Controller：`TicketController.listTicketHistory`（行 175-182），权限 `ticket.view.domain_all`；详情接口 `getTicketDetail`（`TicketService` 行 405-413）也内嵌 history

### 3. 全部 history 写入点（recordHistory 统一入口）

`recordHistory`（`TicketService` 行 834-845）：写 `action/fromValue/toValue/operatorSubjectId/operatorActorType/payload(JSON)`，`createdAt` 由 DB 默认 CURRENT_TIMESTAMP(3)（XML insert 不写 created_at，行 19-29）。

| 动作 | action 值 | fromValue | toValue | payload | 位置 |
|---|---|---|---|---|---|
| 创建 | `create` | null | `"open"` | ticket_no/priority/source | 行 184-187 |
| 领取 | `claim` | 原 assignedTo 或 null | userId 字符串 | version | 行 251-252 |
| 指派 | `assign` | 原 assignedTo 或 null | 新受理人 | version | 行 273-274 |
| 回复 | `reply` | 当前状态 | 当前状态（相同） | version/reply_id/reply_type(text\|quick)/sender_type | 行 318-322 |
| 状态变更（含关闭） | `status_change` | 旧状态 | 新状态 | version/new_status | 行 221-223 |
| 撤回 | `withdraw` | 旧状态 | `withdrawn` | version/reason | 行 361-363 |
| 合并 | `merge` | 旧状态 | `merged` | version/target_ticket_id/target_ticket_no/note | 行 394-398 |
| 关注人替换 | `watchers_replace` | null | null | watcher_staff_account_ids | 行 442-449 |
| SLA 超时强制指派 | `assign` | 原受理人 | 新受理人 | source=sla_breach | 行 564-568（forceAssign，context=null） |

**回复内容不进 history**：`reply` 的 payload 只有 reply_id/reply_type，正文存在 `ticket_reply.content`。时间线要显示回复正文需关联 `replies`（`TicketDetailResult` 已同时返回 replies 与 history，见 `TicketService` 行 1089-1100）。

### 4. R2 内部备注的落点分析（基于现有模型）

- 若内部备注走 **ticket_reply**：需新增 `reply_type` 值（如 `"note"`/`"internal_note"`，现仅有 `text`/`quick`，`TicketService` 行 305 写死），并在客户侧 `getCustomerTicketDetail`（行 415-420 → `getTicketDetail` 行 405-413）过滤——**该接口把完整 replies+history 原样返回客户**，见 internal-note-cross-end.md。
- 若内部备注走 **ticket_history**：`action` 新值（如 `"internal_note"`），正文放 payload（如 `{"content": "..."}` 或新列）；但 `TicketHistoryPo` 无内容字段，且 payload 是 JSON 明细——需要前端约定解析，或加列。历史记录规范（见下）倾向新 action 值 + payload。
- 无论走哪条路，「分组展示」当前都无现成字段，需前端按 action/类型推断或后端新增分组字段。

## Caveats

- `TicketHistoryPo` 无 `updated_at`、无逻辑删除——内部备注若写错无法编辑（只能追加）。
- history 的 `operatorSubjectId` 通过 `ensureIdentitySubject`（行 847-875）解析，customer 与 staff 账号分表查 subject；内部备注的操作人语义沿用即可。
- 时间线排序 `created_at ASC, id ASC`（行 43）——同秒写入靠 id 保序，批量场景写入顺序=插入顺序。
- spec 约定：历史记录规范未见独立 spec 文件；`.trellis/spec/backend/index.md` 质量检查要求「新迁移在 current/ 命名 V{datetime}__desc.sql」「feature 模块不放 Flyway 脚本」——若加表/加列，迁移脚本须放 uniondesk-app。
