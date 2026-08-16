# Research: 批量端点先例 + 单点 claim/close 语义（R1）

- Query: 代码库中已有批量端点先例（batch-claim/batch-close 可参考的形态）；单点 claim/close 当前实现位置与语义（乐观锁、SLA 首响、幂等性）；后端是否已用 MyBatis-Flex
- Scope: internal
- Date: 2026-08-16

## Findings

### 1. uniondesk-ticket 内无批量端点（确认）

`grep -ri "batch" uniondesk-ticket/src/main/java` 仅在 `SlaScanJob.java`（定时扫描，`BATCH_LIMIT=100` 分批）与 `TicketTypeFlow*`（batch 出现在方法内名，非 HTTP 批量端点）出现。`TicketController` 全部为单点端点。

### 2. 单点 claim / close 当前实现

- 入口：`TicketController.java`
  - `POST /admin/domains/{domain_id}/tickets/{ticket_id}/claim` → `claimTicket`，行 130-137，权限 `ticket.claim`
  - `PATCH /admin/domains/{domain_id}/tickets/{ticket_id}/status` → `changeTicketStatus`，行 166-173，权限 `ticket.close`（关闭即 status=closed 走此端点）
- 服务层：`TicketService.java`
  - `claimTicket`（行 239-259）：
    1. `loadTicketRow` 读当前行，**乐观锁**：`current.version() != command.version()` → 抛「工单已被他人修改，请刷新」（行 243-245）
    2. `ticketRepository.updateClaim(ticketId, domainId, userId, version, now)`，返回 0 时抛「工单已被领取或状态不允许领取」（行 247-250）——**领取的 SQL 层幂等/互斥条件见下**
    3. `recordHistory(..., "claim", from=原受理人, to=userId, Map.of("version", ...))`（行 251-252）
    4. `recordAudit(..., "ticket.claim", ...)`（行 253-254）
    5. `slaService.recordFirstResponse(...)`（行 255）——**领取即触发 SLA 首响记录**
    6. `notificationCenterService.notifyTicketStatusChanged(...)` + `refreshTicketSla`（行 256-257）
  - `changeTicketStatus`（行 205-237）：
    1. 乐观锁校验（行 208-210）
    2. `isAllowedStatusTransition(current, command.status())`（行 211-213，状态机见行 822-832：open/new→processing/resolved/withdrawn/merged；processing→resolved/closed/merged；resolved→closed/merged；closed/withdrawn/merged→无）
    3. `ticketRepository.updateStatus(...)`（行 216-219，version+1）
    4. `recordHistory(..., "status_change", from, to, ...)`（行 221-223）
    5. resolved/closed → `slaService.recordResolution` + `TicketStatusChangedEvent`；processing → `recordFirstResponse`（行 224-234）
    6. `refreshTicketSla`（行 235）
- 乐观锁 SQL（`uniondesk-ticket/src/main/resources/mapper/ticket/TicketMapper.xml`）：
  - `updateClaim`（行 301-315）：`WHERE id=? AND business_domain_id=? AND version=? AND assigned_to IS NULL AND status IN ('open','new')`，置 `assigned_to`、status→processing、`version=version+1`、`sla_first_responded_at=COALESCE(..., now)`——**已领取的工单第二次 claim 必然 updated=0，天然幂等/互斥**
  - `updateStatus`（行 280-299）：`WHERE id=? AND version=?`，按目标状态联动 `sla_first_responded_at` / `sla_resolved_at` / `sla_status`（closed/resolved/withdrawn/merged → sla_status='stopped'）
  - `forceAssign`（行 367-384）：SLA 超时强制指派，绕开版本锁（注释明示）
- 命令体（`TicketService.java`）：`ClaimTicketCommand(long version)`（行 1102）、`ChangeTicketStatusCommand(String status, long version, Long quickReplyTemplateId, String content)`（行 1076-1081）

**批量语义推论**：单点 claim 的互斥来自 SQL `assigned_to IS NULL AND status IN ('open','new')`，不依赖版本号本身——批量端点若复用同一 SQL，无需前端版本号也能保证「同一工单只被领取一次」；`updated==0` 即可作为失败项原因（如「已被他人领取」）。批量 close 需注意：`updateStatus` 依赖 `version=?`，若批量时前端只传 ticketId，需先读行或用不带版本条件的新 SQL（或沿用单点语义由前端循环携带 version——但跨页多选时版本可能过期）。

### 3. 已有批量端点先例（可仿照）

| 先例 | 位置 | 形态 |
|---|---|---|
| 跨域批量停用 | `uniondesk-domain/.../web/StaffDomainMemberBatchController.java`（全文 133 行） | `POST /api/v1/admin/staff/{staffId}/domain-members/batch-status`；入参 `BatchStatusRequest(domain_ids, status)`；**逐项 try-catch，成功/失败分别收集**，返回 `BatchStatusResult(List<Long> success, List<BatchDomainFailure failed)`（failed 带中文 reason）；逐项写审计（`writeBatchAudit`，行 102-119）；注意其 `@RequirePermission(PLATFORM_USER_DOMAIN_BATCH_STATUS)` + step-up token |
| 批量建屏蔽词 | `uniondesk-support/.../web/BlockedWordController.java` 行 54-63（域）与 `PlatformBlockedWordController.java` 行 46-51（全局） | `POST /batch`；服务 `BlockedWordService.createBatch`（行 94-135）：单事务内循环，**去重跳过**（seenInBatch + 存在性检查），返回 `BatchCreateBlockedWordResult(int created_count, List<BatchCreateSkippedItem> skipped)`；上限校验「单次最多添加 200 个屏蔽词」（行 97-99） |
| 角色模板批量 | `uniondesk-domain/.../web/RoleTemplateController.java` 行 74-98 | `apply/sync/unapply/bindMembers` 均返回 `RoleTemplateDtos.BatchResult` |
| 客户批量新增 | `DomainCustomerController.java` 行 86 `addCustomersFromStaff` → `BatchCreateDomainCustomersResult` | 从员工列表批量建客户 |
| 批量 DTO 形状 | `DomainMemberDtos.BatchStatusResult(success, failed)`（行 78-82）、`BlockedWordDtos.BatchCreateBlockedWordResult(created_count, skipped)`（行 32-36） | 均为「成功数/列表 + 失败列表（含中文原因）」 |

- 批量端点命名惯例：`POST .../batch-status`、`POST .../batch`（放资源路径下），R1 倾向 `POST /admin/domains/{domain_id}/tickets/batch-claim` / `batch-close`（PRD 倾向后端批量幂等端点）。
- ticket 模块内批处理先例：`SlaScanJob.java`（行 20-40）——`BATCH_LIMIT=100` 分批扫描、**单条失败隔离不中断整批**（注释行 13-14）——与批量端点「部分成功」哲学一致。
- 审计动作码：`uniondesk-common/.../audit/AuditActionCodes.java` 有 `PLATFORM_USER_DOMAIN_BATCH_STATUS` 等批量码先例；ticket 域审计动作现用 `"ticket.claim"` / `"ticket.reply"` 等字符串（`TicketService.recordAudit`，行 623-640）。

### 4. MyBatis-Flex 状态（确认已启用）

- 根 `pom.xml` 行 69-71：`com.mybatis-flex:mybatis-flex-spring-boot3-starter:1.11.8`（dependencyManagement）；`uniondesk-ticket/pom.xml` 行 35-36 引用
- `TicketMapper extends BaseMapper<TicketPo>`（`TicketMapper.java` 行 14）——MyBatis-Flex 通用 CRUD 可用
- PO 用 Flex 注解：`TicketPo.java` `@Table("ticket")` + `@Id(keyType=KeyType.Generator, value=KeyGenerators.snowFlakeId)` + `@Column(onInsertValue=...)`
- **混合形态**：`TicketHistoryMapper` / `TicketReplyMapper` 是纯 `@Mapper` 接口 + XML（无 BaseMapper）；复杂查询走 XML（`mapper/ticket/TicketMapper.xml` 等）。新批量 SQL 可二选一：Flex `updateByQuery`/`updateBatch` 或 XML 手写（对齐现有风格优先 XML/Repository 封装）
- Repository 层惯例：`*Repository` 封装 mapper，缺记录抛中文 `IllegalArgumentException`（spec `database-guidelines.md`：绕过 Repository 直调 Mapper 是反模式）

## Caveats

- 单点 claim 在事务内依次做「领取 SQL → history → audit → SLA → 通知」，批量端点若逐项复用 `claimTicket`，每项都是独立事务（`@Transactional` 在 service 方法上），天然满足「部分成功」；但要注意批量端点整体不应包一个 @Transactional（否则单条失败回滚全部，违背 AC1 部分成功）。
- `updateClaim` 的 `WHERE status IN ('open','new')` 意味着已 processing 的工单不能 claim——批量领取只能领「待处理」工单。
- 前端跨页多选时行版本号可能过期：批量端点若按 PRD 倾向「后端保证幂等」，入参建议只含 ticket_id 列表（后端读行/用 SQL 条件），而不是复用 `ClaimTicketCommand(version)`。
- spec `backend/database-guidelines.md` 提到「PO 不使用 @Table 注解」已过时——现状新实体（TicketPo/TicketReplyPo/InboxMessagePo）均用 MyBatis-Flex 注解。
