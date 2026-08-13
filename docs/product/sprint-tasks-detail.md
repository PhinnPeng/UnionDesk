# Sprint 任务细节细化 — 可执行方案（S3.5–S8）

> 配套 [`sprint-4-8-roadmap.md`](./sprint-4-8-roadmap.md)（排期总览）。本文档为每任务的可执行细节：现状锚点 → 改动落点（文件级）→ 验收标准。
> 勘察日期：2026-08-13。锚点以当时仓库状态为准。

---

## S3.5 · 任务 2：`test-identity-fix`（P1）

**目标**：预存测试身份缺陷清零 + 断言信封收敛 + backlog 状态回写。

**现状锚点**：
- `SlaRuleCrudIntegrationTest`（3 用例）与 `TicketLifecycleIntegrationTest` 员工流（4 用例）用 `admin`（platform_admin，global 角色）访问 `domain.sla.*`/`ticket.claim` 等域码端点 → `PermissionScopePolicy` 要求域角色 → 恒 403
- 种子仅 1 个员工账号 `admin`（rebaseline:1968，`{noop}admin123`）；无 agent/domain_admin 种子
- 断言旧格式：`$.id`/`$.name` 等根节点断言（`ApiResponseWrapper` 已包 `data.*`）

**可执行落点**（方案已确认：A+B+C + SQL建号+真实登录）：
1. 测试基建 `IntegrationAuthSupport`/`IntegrationTestSupport` 新增 `insertDomainStaff(domainId, loginName, roleCode)` 助手：插 `staff_account`（{noop} 密码）+ `domain_member`(active) + `domain_member_role` 绑定 + 角色授权码（参数化 SQL，无凭据字面量——测试密码用 `{noop}` 占位约定）
2. `SlaRuleCrudIntegrationTest`：`adminAccessToken` → 新建 domain_admin 账号真实登录
3. `TicketLifecycleIntegrationTest` 员工流 4 用例：新建 agent 账号真实登录（claim/assign/reply/close 需 agent 码；merge 用例待 S5 补码或先用 domain_admin）
4. 断言收敛：全部 `$.id` → `$.data.id`（含 `SlaRuleCrud`/`DomainAdminIntegrationTest`）
5. `backlog-stories.md` 回写：US-S1-08 → Done；US-S3-02/03 → 承接中；`feature-list.md` 状态列对齐

**验收**：`mvnw.cmd test -pl uniondesk-app -am` 全量通过（预存失败清零，除 S3.5 任务 3 影响项）。

---

## S3.5 · 任务 3：`flyway-chain-consistency`（P0）

**目标**：current/ 迁移链 fresh-install 自洽。

**现状锚点**：
- `V202606060001__domain_member_permissions_and_menu.sql`：5 处 `INSERT iam_admin_menu` 用 `scope='domain'`（:33/:39/:47/:55/:63）→ 违反 rebaseline `chk_iam_admin_menu_scope`（仅 platform/business）→ fresh-install INSERT 失败
- `V20260719100446__drop_legacy_identity_tables.sql`：卸 16 个 FK 后 `DROP TABLE user_account`，**漏卸 `auth_login_log.fk_auth_login_log_user`**（rebaseline:815）→ DROP 被 FK 引用失败

**可执行落点**（方案已确认）：
1. 修订 `V202606060001`：`scope='domain'` → `'business'`（5 处；与业务域端菜单 rebaseline 语义一致）
2. 修订 `V20260719100446`：在 DROP user_account 前补 `ALTER TABLE auth_login_log DROP FOREIGN KEY fk_auth_login_log_user;`
3. dev 库 `flyway_schema_history` 同步：CRC32 重算两个修订文件 checksum 并更新对应 2 条记录（防应用启动 validate 失败）
4. 验证：空库全量 Flyway（需可用 Docker/CI 环境；不可用则语法核对 + 交付，空库验证列为 Sprint 验收）

**验收**：fresh-install 空库 Flyway 全量通过 + validate；dev 库应用启动正常。

---

## S4 · 任务 4：`customer-register-api`（P0，F1.7 + F4.20）

**目标**：注册与邀请码入域走真实 API（去 mock `cust-at`）。

**现状锚点**：
- 后端 **100% 就绪**：`POST /api/v1/auth/register`（AuthController:62）——`RegisterRequest(loginName/password/displayName/phone/email/domainId/invitationCode/captchaToken)`，`RegisterResponse(accessToken/refreshToken/accountId)` **注册即登录即入域**；DR-01(41101 自助注册禁用)/DR-02(41102 邀请码禁用)在 `AuthService.register`(:389-440)；邀请码 `InvitationCodeService.validateAndUse`(:64-83)；管理端邀请码面板已接真实 API
- 前端 mock：`shared/src/customer-portal.ts` `registerCustomer`(:620-682)、`joinCustomerDomainByInvitation`(:713)、`createSession`(:371 伪 token)；`customer-portal-live.ts` **缺 registerLive/joinDomainLive**
- 页面：`pages/register/index.tsx`（提交 `portal.register` :40，toast「注册成功（本地演示）」）；`pages/domains/index.tsx` joinDomain 仍 mock（:36）

**可执行落点**：
1. `packages/shared/src/api.ts`：新增 `register(payload, options)` → POST /auth/register（参考 login :446）
2. `customer-portal-live.ts`：新增 `registerCustomerLive`（调 register → hydrate 会话 → 刷新 domains/tickets/types）；`customer-portal.ts` 的 `useCustomerPortal.register` 在 live 模式路由到 live 实现
3. `pages/register/index.tsx`：接 `registerCustomerLive`；删「本地演示」文案；错误透传后端中文（41101/41102/账号已存在）；成功后 `enterDedicatedDomain`/跳 `/domains` 保留
4. `pages/domains/index.tsx`：**决策点**——若 US-S3-02 限定「注册即入域」，删除 mock 入域入口或改引导注册；若需「已登录客户凭邀请码入域」，新增后端端点 `POST /api/v1/domains/{domainId}/join`（复用 validateAndUse+addCustomer）+ live 适配
5. 验证码：后端宽松（captchaEnabled && token 非空才校验）——保持现状不强制

**验收**：注册（含邀请码/自助注册/禁用域拒绝）真实 API 全通过；登录后域选择/专属域跳转正确；无 `cust-at` 残留路径（除 mock 兼容层）

---

## S4 · 任务 5：`ticket-dynamic-form`（P0，F1.1）—— **已取消（2026-08-13 用户决策）**

> 决策：管理端事项配置已实现；客户端渲染不在当前范围。提单保持 title/description；F1.1 动态字段渲染待后续另行立项（缺口保留：CustomerWeb 无渲染代码、客户侧 DTO 无 form_schema）。

**目标**：客户端提单按管理端 Formily schema 渲染动态字段 + 附件真实上传。

**现状锚点**：
- 后端 **95% 就绪**：`ticket_form_schema` 表 + `TicketFormSchemaService`（draft/publish/versions/rollback）；schema 为 Formily JSON（`DefaultFormSchemaProvider` 示例：title/description 系统字段 + 自定义 properties）；提交链路 `CreateTicketCommand.dynamicData` 已支持（`TicketService.java:882-901`）；shared `createCustomerMyTicket` 已发 `dynamicData: {}`（api.ts:2570）
- **缺口**：客户侧 `GET /domains/{id}/ticket-types` 返回 `CustomerTicketTypeView(id/name/description)` **不含 form_schema**；客户上传权限未验证
- 前端：`new.tsx` 仅 title/description（:145-166），`attachmentIds: []` 硬编码（:54）；CustomerWeb 无 formily 依赖（仅 AdminWeb 有 `@formily/*`）

**可执行落点**：
1. 后端小改：`CustomerTicketTypeView` 加 `form_schema`（published，`loadAggregate().publishedSchema()`）或新增 `GET .../ticket-types/{type_id}/form-schema`；mapper 补字段
2. shared：`CustomerPortalTypeOption` 补 schema；`refreshCustomerTicketTypesLive` 透传
3. 新增渲染器（**选型决策**：a. 引 @formily（与 admin 同栈但视觉/体积冲突）；b. 自写轻量映射器——推荐 b，30-60 行映射 `x-component` → 原生控件，与 `ud-*` 风格一致）：Input/Input.TextArea/Select/NumberPicker/Checkbox/Radio/DatePicker/Upload，值收集为 `dynamicData`
4. `new.tsx`：选类型后按 schema 渲染（系统字段恒渲染）；提交传 `dynamicData`；`attachmentIds` 接 `uploadP0AttachmentLocal`+`confirmP0Attachment`
5. 客户上传权限：验证 `ATTACHMENT_UPLOAD` 对客户 clientCode 生效（若 403 → 后端为客户放行或新增客户上传端点）

**验收**：管理端配置的类型表单在客户端按 schema 渲染（含必填/类型校验）；提交后 `custom_fields` 正确落库；附件上传/确认/展示可用

---

## S4 · 任务 6：`satisfaction-survey`（P0，F1.5）

**目标**：满意度评价全链路（模型/API + 客户端入口 + 通知中心入口 + 埋点）。

**现状锚点**：后端 0 落地（全仓 grep 无 satisfaction）；工单关闭事件 `TicketStatusChangedEvent`（TicketService:207-214）→ `NotificationEventListener.onTicketStatusChanged`（:19-31）已存在；`NotificationCenterService.notify*` 系列 + `inbox_message`（无 kind 列，靠 jumpUrl 跳转）；`pages/tickets/detail.tsx` 无评价入口；埋点基建 0

**可执行落点**：
1. 后端新增（uniondesk-ticket）：表 `ticket_satisfaction`（id/domain_id/ticket_id 唯一/customer_id/rating 1-5/comment/status/created_at）+ `POST/GET /domains/{domain_id}/tickets/my/{ticket_id}/satisfaction`（权限 ticket.view.self 级；仅 closed 可评、仅本人）
2. 通知入口：`NotificationCenterService.notifyTicketSatisfactionInvite`（templateCode `ticket.satisfaction_invite`，jumpUrl 指向工单详情）——关闭时触发；**顺带修复 inbox jumpUrl 跳 API 路径的隐患**（现有跳转直接 navigate API URL）
3. 客户端 `detail.tsx`：closed/resolved 态渲染星级+文字评价区（提交后防重复）
4. 埋点：新建最小基建 `shared/track.ts`（`trackEvent(code, payload)` 占位 + 本地日志），事件码约定 `satisfaction.submit/view` 写入 PRD 附录

**验收**：工单关闭后可评（仅本人/仅一次）；通知中心收评价邀请并可跳转；埋点事件码落位

---

## S5 · 任务 7：`staff-ticket-queue`（P0，F2.1 / US-S3-04）

**目标**：业务域端工单队列与处理闭环（agent 全链路），E3 签 off。

**现状锚点**：
- 演示页：`pages/platform/ticket-pool`（无筛选/分页）、`pages/platform/ticket-detail`（手输 ID；状态硬编码 :497-505；SLA Tag+deadline 已展示）
- 后端端点 10 个全就绪（claim/assign/watchers/reply/close/merge/history + 列表/详情），权限码已注册；**缺口：`listAdminTickets` 仅 status+limit，无分页/筛选/我的待办**（TicketService.listTickets :432）
- agent 授权：缺 `ticket.merge`(37)；菜单绑定需新迁移；业务域菜单体系（工作台/域治理 catalog，V20260727183000/V20260728173000 模式）
- 前端复用：`member-picker.tsx`、`priority-badge.tsx`、AGENTS.md §2.7 骨架（detail-members 为模板）；`src/api/platform/ticket.ts` 已 1:1 覆盖全部端点

**可执行落点**：
1. 后端：`TicketController.listAdminTickets` + `TicketService.listTickets` 扩展分页/筛选（page/page_size/status/assignee/priority/keyword/assigned_to_me）→ `PageResult`（对齐 SlaController 模式）
2. 新 Flyway：agent 补 `ticket.merge` 授权 + `/ticket-queue` business 菜单行（含按钮码）+ domain_admin/agent 菜单绑定（仿 V20260728173000）
3. 前端新增 `pages/domain/ticket-queue/index.tsx`（+less）：域上下文（authStore）→ TableSearchForm + Table（分页 20/showSizeChanger）+ extra 工具栏（AuthGuarded）；行操作领取/指派（MemberPicker）/关闭（ConfirmPopover）
4. 详情改造：`pages/domain/ticket-queue/detail.tsx`（或改造演示页）：路由参数化、状态下拉改动态 ticket-statuses、优先级改 priority-levels
5. 前端路由 `modules/domain.ts` 加 `/ticket-queue`（business scope，auth `ticket.view.domain_all`）

**验收**：agent 登录业务域端可见「工单队列」；筛选/分页/我的待办可用；领取→指派→回复→关闭全链路；US-S3-04 签 off

---

## S5 · 任务 8：`domain-sla-page`（P1，F3.2/F3.11）

**目标**：SLA 域端独立页 + 通知配置实现。

**现状锚点**：后端 SLA 端点全就绪（`SlaController`，缺日历删除端点）；平台页 `pages/platform/sla-management` 完整 CRUD 可复用；`/domain/settings/notifications` 为 Empty 占位（权限已注册）

**可执行落点**：
1. 域端页 `pages/domain/sla/index.tsx`（复用平台页改造：域上下文替换域下拉）；后端补 `DELETE /sla-calendars/{id}`（对齐 rules）
2. Flyway：`/domain/sla` business 菜单行 + domain_admin/agent 绑定（仿 V20260727183000）
3. 通知配置实现（F3.11）：与 SLA 语义澄清后，基于 `NotificationTemplateController`（域端模板 CRUD 端点已存在）建配置 UI
4. 分页处理：平台页现只拉 20 条未翻页——域端页实现分页

**验收**：域管理员可在业务域端维护 SLA 规则/日历；通知模板可配置

---

## S5 · 任务 9：`security-alerts-center`（P1，F4.4）

**目标**：安全告警中心 + 密码强度/登录锁定/IP 白名单。

**现状锚点**：`/platform/system-settings` 为通用 KV 编辑器（无专用安全表单）；`LoginConfigService` 字段无 lockout/ip 白名单/密码强度；全仓无 security alert 后端——**纯新建**

**可执行落点**：
1. 后端：登录策略扩展（密码强度规则/连续失败锁定/IP 白名单——表或 KV 扩展 + 校验逻辑挂 `AuthService.login`）；告警数据源（安全事件表或审计聚合）
2. 新权限码 `platform.security_policy.*` + Flyway 菜单行 `/platform/security-alerts`
3. 前端：独立页或系统设置 Tab（告警列表 + 策略表单）

**验收**：密码强度/锁定/IP 白名单策略生效；告警中心可查（数据源就绪）

---

## S6 · 任务 10-13：清收四件套

### 10 `user-import-export`（P1，F4.18 + F4.3）
- 现状：`/platform/import-export` 纯静态 mock；后端无 import/export 端点（权限 `platform.user.import` 已注册）
- 落点：后端 `ImportExportController`（Excel 解析/异步任务表，或最小 CSV 同步实现）；前端用户页工具栏接真实上传/下载交互；F4.3 永久删除入口（后端 `DELETE staff` 已就绪？——勘察未确认，实现时核对）

### 11 `org-config-merge`（P3，F4.21）
- 现状：`/platform/org-config` Empty 占位（菜单 ADM0000000071）；组织管理 = `/platform/dept`（OrganizationController 全 CRUD 就绪）
- 落点：Flyway 置 org-config 菜单 hidden=1 + 删除占位页文件；确认组织架构页覆盖配置项

### 12 `template-pages-cleanup`（P3，F3.15/F3.16/F4.22）
- 现状：`/access/*`、`/route-nest/*`、`/about`、`/outside/*`（modules 文件 access.ts/routeNest.ts/about.ts/outside.ts）+ `/system/user`、`/system/dept`（system.ts :14-29/:47-62）+ CustomerWeb settings 死目录
- 落点：删 4 个 modules 文件 + 对应 pages 目录 + system.ts 的 user/dept 两条（**保留 role/menu**）；保留异常页（exception/403/404/500）与公开页（privacy/terms）；注意测试引用清理（layout-menu/utils.test.ts）

### 13 `dashboard-real-aggregation`（P2，F4.6）
- 现状：前端 `/platform/home` 已前端聚合（overview.ts 并行拉取 count）；`DashboardController` 是 `@Profile("demo")`（DemoDataService）
- 落点：新建生产 `OverviewController`（domain/user/ticket/consultation 各 count(*)，权限 `platform.dashboard.read` 已注册）；前端切真实端点；补 ticket/consultation count 与空态

---

## S6 · 任务 14 + S7 · 任务 17：`group-role-management` P1-1/P1-2/P2

> 规划产物已归档（archive/2026-08/08-11-group-role-management/）：prd.md（AC1–AC7）/design.md（§3 数据模型/§5 API/§8 阶段）完整，**直接按 design.md 执行**，此处不重复。
- P1-1 模板层：role_template 三表 Flyway + CRUD/apply/sync/unapply/bind-members + F4.9「模板」Tab
- P1-2 批量停用：`POST /admin/staff/{id}/domain-members/batch-status` + step-up UI（F4.7 入口）
- P2 域端展示：F3.13 模板徽标/锁定只读/漂移提示
- 前置：P0 安全债已完成 ✅；注意 `bindDomainMemberships` 为 private 需暴露；≤20 上限双入口新建校验

---

## S7 · 任务 15：`sla-ui-epic`（P2，E4/F2.2）

**现状**：SLA 引擎已实现（`SlaService`：breached 判定 :143-156；`SlaTicketPo` 字段全）；ticket-detail 已展示 slaStatus Tag + deadlines；ticket-pool 列表未展示
**落点**（依赖 S5 任务 7 队列页）：队列列表加 SLA 列（状态 Tag + 到期/超时高亮）；详情加濒临 breach 倒计时 + breachActionJson 提示；纯前端

---

## S7 · 任务 16：`online-consultation`（P2，E5/F1.2+F2.3）

**现状**：`ConsultationController` 是 `@Profile("demo")`；表结构齐全（consultation_session/message/ticket_link）；`consultation.reply` 已授 agent/domain_admin；客户端 `/chat` 占位页
**落点**（建议拆 2 子任务）：
- 客户端：真实会话页（列表/消息/发送）
- 工作台：会话列表 + 转工单（带客户信息+会话摘要，复用 consultation_ticket_link）
- 后端：去 demo profile、补权限注解（当前无——demo 门控下风险可控，生产化必须加）

---

## S8 · 任务 18：`prd-doc-fixes`（P1）—— **已取消（2026-08-13 用户决策）**

> 决策：PRD 文档修正不再单独立项；§4.4 联动与 feature-list/backlog 状态同步并入各 Sprint 执行时顺手维护。

**现状**：prd.md §4.4 自洽检验过度声明（宣称无残留，但 flyway 链仍有 fresh-install 问题——与 S3.5 任务 3 联动修正）；feature-list 权限码待确认 3 行（F4.2/F4.14/F4.20）+ F2.1 agent 绑定待联调
**落点**：§4.4 勾选与 flyway-chain-consistency 结果联动；权限码升格落位；feature-list/backlog 状态同步（贯穿各 Sprint）

---

## S8 · 任务 19：双端验收固化（P2）

**现状**：`IntegrationAuthSupport`（登录/验证码绕过/域准备）就绪；既有集成测试可作模板（CustomerTicketPermissionIntegrationTest 等）
**落点**：FR-01~06 场景化端到端用例（登录→入域→提单→认领→回复→关闭→评价）；前置 = S3.5 任务 2（测试身份修复）

---

## 风险总表

| 风险 | 涉及任务 | 对策 |
|:---|:---|:---|
| 工单列表无分页/筛选 | S5-7 | 后端扩展查询参数（P0 项） |
| agent 缺 merge 码 + 菜单绑定 | S5-7 | 新迁移（S5 内） |
| 安全告警纯新建 | S5-9 | 拆小步：策略先行、告警数据源后置 |
| 导入导出后端空白 | S6-10 | 最小 CSV 同步实现，异步/Excel 后置 |
| 客户端 form_schema 缺口 | S4-5 | 先扩 DTO（后端小改） |
| 评价触发与 inbox 跳转隐患 | S4-6 | 事件驱动 + jumpUrl 路由解析修复 |
| Flyway 链自洽 | 全部 | S3.5 任务 3 先行（P0） |
