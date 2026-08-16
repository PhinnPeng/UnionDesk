# 员工端处理链路打通 — 下迭代任务点方案

> 2026-08-13 立项（勘察稿，供确认后分批实现）。聚焦 **PRD §5.2 员工端处理链路**（feature-list F2.1/F2.2/F2.3）。
> 勘察依据：`docs/product/prd.md`（§5.2）、`docs/product/feature-list.md`（F2.1-F2.3）、后端 `uniondesk-ticket`/`uniondesk-support`、前端 `UnionDeskAdminWeb/src/pages/domain/*`。

## 现状结论（勘察已核实）

**已就绪**：
- 工单队列：列表/分页/筛选（status/assignee/priority/keyword/assigned_to_me）后端+前端全通
  - `TicketController:83` `GET /admin/domains/{id}/tickets` · `TicketService.listAdminTickets:437` · 前端 `pages/domain/ticket-queue/index.tsx`
- 工单详情：回复/附件/领取/指派/关注人/状态/关闭/合并 全通
  - `TicketController:109-178` · `pages/domain/ticket-queue/detail.tsx` · `src/api/platform/ticket.ts`
- 工单权限码齐（`PermissionCodes.java:199-209`）+ 目录（`AdminPermissionCatalog` `:435-445`）+ 菜单种子（`V20260813160000`）
- 咨询：列表/查看/回复/转工单 全通（`ConsultationRuntimeController` · `pages/domain/consultations/index.tsx`）

**缺口（按优先级）**：
1. **[最高] 咨询"接入/领取会话"缺失**（F2.3 接待）：后端无 `claimSession`（仅 `ConsultationService.replyAdmin:157` 首次回复隐式认领），无主动抢单/`assigned_to_me` 会话队列；前端无"接入/领取"按钮。PRD F2.3「客服手动从队列中接入」。
2. **[最高] 咨询"结束咨询"缺失**：后端无 end-session 端点（`closeSession` 仅在 `convertToTicket:202` 内部调用），未转工单的会话无法终结。
3. **[高] 工单状态机两套割裂**：运行时用硬编码 `isAllowedStatusTransition`（`TicketService:688`，open/processing/resolved...），可配置工作流 `StatusFlowValidator`/`TicketTypeFlowService` 未接入动作路径；DB 内置码 `not_started/in_progress/completed/cancelled`（`V202607070002`）与运行时码不一致 → 下拉可选状态与运行时合法状态不匹配。
4. **[高] F2.1 批量动作缺失**：PRD F2.1「批量领取、批量关闭」前端未实现（仅单行 action）。
5. **[中] 聊天撤回缺失**：PRD F2.3「撤回 2 分钟内消息」两端均无。
6. **[中] `consultation.view/convert/customer` 未入 `AdminPermissionCatalog`**（仅 DB 种子里，目录模型缺，导致 IAM 配置/审计不一致）；agent 的 `ticket.view.domain_all/claim/close` 授权补齐情况待核对。
7. **[中] F2.2 SLA 感知未接入工单 UI**（SlaTimingEngine 已实现，feature-list F2.2 标注 📅 未排期）。

---

## 任务点拆分

### Epic E-EMP1 在线咨询工作台接待闭环（对应 F2.3，最高优先）
- **S-EMP1-01 咨询会话接入（claim）**：后端 `ConsultationService.claimSession(context, domainId, sessionNo)` + `ConsultationRuntimeController` 管理端点 `POST /admin/domains/{id}/consultations/{session_no}/claim`（权限 `consultation.claim`）；`listAdminSessions` 支持 `assigned_to_me` 筛选（会话队列"我的会话"）。前端咨询列表加"接入/领取"按钮 + 会话队列可按"仅看我的"过滤。
  - 验收：未分配会话可被客服接入并写入 `assigned_to`；已分配他人会话不可再接入；`assigned_to_me` 列表正确。
- **S-EMP1-02 咨询会话结束（end）**：后端 `ConsultationService.endSession(context, domainId, sessionNo, reason?)` + 管理端点 `POST /admin/domains/{id}/consultations/{session_no}/end`（权限 `consultation.view` 或新增 `consultation.close`）；会话 `open→closed` 终态（未转工单也允许关闭）。前端工作台加"结束咨询"按钮 + 二次确认。
  - 验收：未转工单会话可手动关闭并落终态；已关闭/已转单会话不可重复结束；转工单后自动关闭与手动结束互不冲突。
- **S-EMP1-03 聊天消息撤回**：后端 `ConsultationService.retractMessage(context, domainId, sessionNo, messageId)`（仅 2 分钟内、仅本人/本人会话消息可撤）；`listMessages` 返回已撤状态。前端消息气泡加"撤回"入口。
  - 验收：2 分钟内消息可撤、超时拒绝、非本人不可撤、撤回后展示"已撤回"。
- **S-EMP1-04 咨询权限目录补全**：`AdminPermissionCatalog` 增补 `consultation.view/convert/customer`（+新增的 claim/close 若采用），与 `V20260813200000` 种子对齐；核对 agent/domain_admin 的 `ticket.*`、`consultation.*` 授权是否真正覆盖（防"菜单有按钮但权限被拒"）。
  - 验收：`GET /iam/admin-permission-codes` 列出全部咨询码；agent 角色可正确处理咨询（权限实测）。

### Epic E-EMP2 工单处理中心增强（对应 F2.1，次优先）
- **S-EMP2-01 批量领取/批量关闭**：前端工单队列加行选择 + 工具栏"批量领取""批量关闭"（复用 `claim`/`close` 端点循环，后端可选 `POST .../batch-claim` 幂等批量）。权限 `ticket.claim`/`ticket.close`。
  - 验收：多选后一键领取/关闭，失败项提示、其余成功；权限不足时按钮按 `AuthGuarded` 隐藏。
- **S-EMP2-02 内部备注 + 时间线完善**：PRD F2.1 工单详情「内部备注」与「左侧时间线（公开回复/内部备注/系统操作）」——现状是否有内部备注类型与时间线分组呈现，未达标则补（后端 history 类型 + 前端 tab 分组）。
  - 验证后定范围：内部备注≠公开回复写入，不向客户展示。

### Epic E-EMP3 状态机与 SLA（治理层，建议与 EMP2 并行立项）
- **S-EMP3-01 运行时状态机接可配置工作流**：`changeTicketStatus` 的 `isAllowedStatusTransition`（`TicketService:688`）改走 `TicketTypeFlowService`/`StatusFlowValidator`，让域配置的状态流转真正约束运行时；统一 DB 内置状态码与运行时码口径，消除下拉可选状态与合法状态不一致。
  - 验收：配置的状态流转在 `PATCH /status` 生效；非法流转返回业务文案；既有 `FrRulesAcceptanceTest`（FR-01/FR-03/TR-02）全绿。
- **S-EMP3-02 SLA 感知高亮（F2.2）**：工单列表/详情接入 `SlaTimingEngine` 算出 SLA 状态（正常/即将超时/已超时），前端 Tag/Tooltip 高亮。
  - 验收：即将超时/已超时工单在列表与详情有高亮标识；与 SLA 规则数据一致。

> 说明：E-EMP1 直接承接 `customer-permission-chain` 打通的客户咨询权限（`consultation.customer` 已授权），员工端补"接入→结束"即形成完整闭环；本迭代建议 **先做 E-EMP1（含目录治理），再做 E-EMP2/E-EMP3**。

---

## 需要补充确认的点
1. **咨询"接入"是否走 `assigned_to` 抢单语义**，还是需要"排队分配（先来先服务/当前可接数上限）"？本方案默认**手动抢单（claim）**。
2. **工单状态机**是"直接接现有可配置工作流"，还是先只做"消除 DB 内置码与运行时码不一致"的最小治理？影响 E-EMP3-01 的拆分。
3. **F2.2 SLA 高亮**是否纳入本次迭代（feature-list 标 📅 未排期），还是延后？
