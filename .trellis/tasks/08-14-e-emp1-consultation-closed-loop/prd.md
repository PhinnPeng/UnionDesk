# E-EMP1 在线咨询工作台接待闭环

> 2026-08-14 立项，`08-14-employee-handling-chain` 子任务（P0）。对应 PRD F2.3、feature-list F2.3。
> 依赖：S-00 冒烟（若暴露咨询相关阻塞项，先并入本任务处理）。

## Goal

补齐在线咨询"接待闭环"：**接入(claim) → 聊天 → 结束(end)/ 转工单**，并补权限目录一致性。当前 `ConsultationService`/`ConsultationRuntimeController` **缺** `claimSession`(接入) / `endSession`(结束) / `retractMessage`(撤回)；`closeSession` 仅 `convertToTicket` 内部调用。

## 现状（勘察核对）
- 咨询权限码 `consultation.view/reply/convert/customer` 已在 `PermissionCodes:217-220`
- 咨询运行时端点：create/list-my/my-messages/my-send（客户）；list-admin/admin-messages/admin-reply/convert-to-ticket（员工）— **无 claim/end**
- `ConsultationService.replyAdmin:157` 首次回复隐式认领（`updateAssignedToIfNull:161`），无主动接入语义
- `AdminPermissionCatalog` 仅列了 `CONSULTATION_REPLY`（:450），view/convert/customer 未入目录 → IAM 配置/审计与 DB 种子不一致

## Requirements
- R1 **接入(claim)**：`ConsultationService.claimSession(context, domainId, sessionNo)` + `POST /admin/domains/{id}/consultations/{session_no}/claim`（新增权限 `consultation.claim`）；`listAdminSessions` 支持 `assigned_to_me` 筛选（"我的会话"队列）。前端咨询列表加"接入/领取"按钮 + 队列"仅看我的"过滤。
- R2 **结束(end)**：`ConsultationService.endSession(context, domainId, sessionNo, reason?)` + `POST .../consultations/{session_no}/end`（权限 `consultation.close` 或复用 `consultation.view` 新增 `consultation.close`）；会话 open→closed 终态，未转工单也允许关闭。前端"结束咨询"按钮 + 二次确认。
- R3 **撤回**：`ConsultationService.retractMessage(context, domainId, sessionNo, messageId)`——仅 2 分钟内、仅可撤本人会话中自己的消息；列表返回已撤状态。前端消息气泡"撤回"入口 + 已撤展示。
- R4 **权限目录**：`AdminPermissionCatalog` 补 `consultation.view/convert/customer` + 新增 claim/close，与 `V20260813200000` 种子对齐；核对 agent 的 `ticket.*`/`consultation.*` 授权实际覆盖（防"菜单有按钮但权限被拒"）。

> 决策默认：接入采用**手动抢单 claim**（PRD F2.3「客服手动从队列中接入」）。

## Acceptance Criteria
- [ ] AC1 未分配会话可被客服接入并写入 `assigned_to`；已分配他人会话不可再接入；`assigned_to_me` 列表正确
- [ ] AC2 未转工单会话可手动结束落 closed 终态；已关闭/已转单会话不可重复结束；与转工单自动关闭互不冲突
- [ ] AC3 2 分钟内消息可撤回、超时拒绝、非本人/他人会话不可撤、撤回后展示"已撤回"
- [ ] AC4 `GET /iam/admin-permission-codes` 列出全部咨询码（view/convert/customer + 新增 claim/close）；agent 可正确处理咨询（权限实测）
- [ ] AC5 后端单测 + `AuthServiceTests`/咨询相关集成测试全绿；`FrRulesAcceptanceTest` 不回归

## Out of Scope
- 客户端消息撤回成品交互（若需另议）
- 排队分配（先来先服务/可接上限），本轮默认手动抢单
