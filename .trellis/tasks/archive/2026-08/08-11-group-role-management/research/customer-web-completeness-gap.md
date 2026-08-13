# Research: 客户端（CustomerWeb）功能完备性与缺口分析

- **Query**: 客户端（客户服务中心 `UnionDeskCustomerWeb`）在多业务域架构下的功能完备性：现有功能是否完备？相对 PRD 承诺、多业务域特性衍生需求、行业惯例，还有哪些功能未加入？
- **Scope**: mixed（内部文档 + 代码勘察）
- **Date**: 2026-08-11

## 0. 研究方法与证据基础

- 文档链：`docs/product/prd.md`（V2.2）、`feature-list.md`（1.0，2026-08-11 状态基准）、`backlog-epics.md`、`backlog-stories.md`、`vision.md`、`implementation-inventory.md`。
- 代码：`UnionDeskWeb/apps/UnionDeskCustomerWeb/src/**`（客户端全部页面）、`UnionDeskWeb/packages/shared/src/customer-portal.ts`（本地 mock 状态机）、`customer-portal-live.ts`（真实 API 封装）、`api.ts`（类型与端点）；后端 `UnionDesk/**`（Java，控制器/服务/迁移）。
- **事实性标注**：
  - 任务描述中提及的已有研究文件（`customer-web-pages.md`、`prd-detail-permission-gap.md`、`story-inventory.md`、`prd-alignment-notes.md`）在 `.trellis/tasks/08-11-project-final-prd-feature-list/research/` **均不存在**（目录原为空，本次新建）。本报告为独立研究产出。
  - `implementation-inventory.md` 确认**无客户端（CustomerWeb）盘点章节**（§1–§5 平台端、§7 业务域端待补），客户端实现状态以 feature-list.md 快照 + 代码为准。
  - 满意度评价验证：前端 `CustomerWeb/src` grep `评价|rating|satisfaction` 0 命中；后端 `UnionDesk/` grep `satisfaction|rating|评价` 0 命中（含 DemoDataService）。

---

## 1. 客户端功能全景表（F1.x，状态以代码为准交叉验证）

> 状态基准 2026-08-11（S3 编码中：US-S3-02/03 Todo）。「实现证据」为本次代码勘察确认点。

| 功能编号 | 功能/页面 | 状态 | PRD 依据 | 实现证据（代码） |
| :--- | :--- | :--- | :--- | :--- |
| F1.1 | 提交工单（动态表单）`/tickets/new` | **部分** | prd §5.1.1「填写动态表单」 | `new.tsx` 仅渲染系统字段 title/description（:145-166），无 form_schema 字段渲染；`attachmentIds: []` 恒空（:54）；后端 `TicketController` `POST /domains/{domain_id}/tickets` 已具备（TicketController.java:37） |
| F1.2 | 在线咨询 `/chat` | **规划中（E5）** | prd §5.1.1「排队→接入→实时聊天（文字/图片/附件）」 | `chat/index.tsx` 纯静态占位（15 行，「即将开放」）；后端 `ConsultationController` 为 `@Profile("demo")` 演示实现，全部走 `DemoDataService`（ConsultationController.java:26,29-33），无真实会话运行时 |
| F1.3 | 反馈/建议（工单类型路径） | **已实现** | prd §5.1.1「MVP 以工单类型路径交付」 | `new.tsx` 类型选择卡展示预置「反馈」「建议」（:105-115）；类型来自 `listCustomerDomainTicketTypes` 真实 API |
| F1.4 | 我的工单/咨询历史 `/tickets`、`/tickets/:id` | **部分** | prd §5.1.2「我的工单…补充信息…咨询历史…」 | 列表：类型侧栏计数/生命周期筛选/关键词搜索 ✔（`index.tsx`）；详情：公开时间线、补充说明、撤回 open 态 + version 乐观锁 ✔（`detail.tsx`）；**咨询历史无**；详情「关联咨询」为占位卡（detail.tsx:187-190）；附件展示无（`mapTicketRow` attachments: []，customer-portal-live.ts:70） |
| F1.5 | 满意度评价 | **缺失（全站无入口）** | prd §3.1/§5.1.2「工单关闭/咨询结束后，通知中心评价入口，星级+文字评价」 | 前端 grep 0 命中；后端 grep 0 命中；feature-list.md F1.5 标注「P1（未拆 Story），全站无评价功能」 |
| F1.6 | 登录（滑块验证、专属域入口）`/login`、`/d/:domainCode/login` | **已实现** | prd §5.1.3 | `login/index.tsx`：真实 `loginCustomerLive` JWT API、滑块 `LoginCaptcha`、记住账号、专属域预判（:92-102）、新环境登录提醒（:89-91）；**忘记密码为占位 toast**「功能开发中」（:68-71） |
| F1.7 | 注册与入域 `/register`、`/d/:domainCode/register` | **部分（本地 mock）** | prd §5.1.3「注册表单+开放域下拉+邀请码」 | `register/index.tsx` 调 `portal.register(...)`（本地状态机，toast「注册成功（本地演示）」:67）；`customer-portal-live.ts` **无 register 导入/调用**；**后端 `POST /api/v1/auth/register` 已实现**（AuthController.java:63-67；AuthService.java:362-394 含 DR-01/DR-02 校验、invitationCode `validateAndUse` 有效期/次数）——前端未接入（US-S3-02 Todo） |
| F1.8 | 业务域选择与切换 `/domains` | **已实现** | prd §5.1.3「已加入/可加入/需管理员开通三组卡片」 | `domains/index.tsx`：三组卡片 ✔；切换真实 `selectCustomerDomainLive`（switch-domain API）（:21）；**邀请码加入为本地 mock**（`portal.joinDomainByInvitation`，toast「已加入业务域（本地演示）」:36，页面标注「P1 将接真实入域 API」:63） |
| F1.9 | 服务首页 `/home` | **已实现** | prd §5.1.4 | `home/index.tsx`：问候语、待处理提醒、生命周期统计卡、最近 5 条工单、未读通知前 3 条 + jumpUrl 跳转 ✔ |
| F1.10 | 站内信/通知中心 `/inbox` | **已实现** | prd §5.1.4「system/ticket/domain 分类」 | `inbox/index.tsx`：列表/未读 badge/标为已读/jumpUrl ✔；kind 映射 system/ticket/domain（:78）；**无域筛选/分组展示**（见 §3 B-1） |
| F1.11 | 个人中心 `/me` | **部分** | prd §5.1.5「账号信息、业务域与通知入口、退出登录」 | `me/index.tsx`：账号信息/业务域/通知入口/退出真实 API ✔；**通知偏好为占位 toast**「UI 占位」（:62-72，PRD 已注明「即将上线」） |
| F1.12 | 修改密码 `/change-password` | **已实现** | prd §5.1.5 | `change-password/index.tsx`：当前/新密码/确认、前端校验（≥6 位/两次一致/不与当前相同）、`mustChangePassword` 强制改密不可跳过（App.tsx:82-84）✔ |

**汇总**：12 项 P0 功能中——已实现 6（F1.3/1.6/1.8/1.9/1.10/1.12）、部分 4（F1.1/1.4/1.7/1.11）、规划中/缺失 2（F1.2 规划中、F1.5 缺失）。与 feature-list.md §1.4 统计（6 已实现 / 4 部分 / 2 规划中）一致。

---

## 2. 缺口清单

### A 类：PRD 承诺未落地（缺陷）

| # | 缺口 | 为什么重要 | 优先级 | 依据 |
| :--- | :--- | :--- | :--- | :--- |
| A-1 | **满意度评价全链路缺失**（F1.5）：无评价入口、无评价 API、无评价数据模型 | PRD §3.1 场景 3、§3.3 客户端主路径末端（「确认解决 → 满意度评价」）、§5.1.2 明确承诺；vision §5 客户诉求「评价」；prd §6.1 埋点 `satisfaction_submit` 对应漏斗「评价参与率」——**服务闭环的收尾环节缺失，直接影响「完整服务闭环」核心价值（prd §2.3）** | **P0** | prd.md §3.1/§3.3/§5.1.2/§6.1；feature-list.md F1.5（全站 grep 0 命中）；代码：前后端 grep 0 命中 |
| A-2 | **提单动态表单字段未渲染**（F1.1 核心缺口）：客户端只渲染系统字段 title/description | PRD §5.1.1「填写动态表单」、§2.3「动态工单表单：各业务域可自定义工单字段」是产品核心差异化；管理端 Formily 表单设计器（F3.1/F4.14）已可产出 form_schema 并发布，**配置能力已具备但客户端消费端未接**——配置了也无法让客户看到 | **P0** | prd.md §2.3/§5.1.1；feature-list.md F1.1 备注；代码：new.tsx:145-166（无 form_schema 渲染）；TicketConfigController form-schema/publish 端点已存在 |
| A-3 | **注册与邀请码入域仍为本地 mock**（F1.7）：`/api/v1/auth/register` 后端已实现（含 DR-01/DR-02、邀请码校验/使用），客户端未接入 | PRD §5.1.3 承诺「注册与邀请码入域接真实 API（US-S3-02）」；注册/入域是**客户进入多业务域的第一步**，mock 意味着「开放注册/仅邀请」策略（prd §4.1.2 注册策略）实际不可用；F4.20 管理端邀请码面板（已实现）生成的邀请码客户端无法真实消费 | **P0** | prd.md §5.1.3；feature-list.md F1.7/F4.20；backlog-stories.md US-S3-02（AC1-4）；代码：AuthController.java:63、AuthService.java:362-394（后端已实现）；register/index.tsx:40-52（portal.register mock）；customer-portal-live.ts（无 register 调用） |
| A-4 | **在线咨询（F1.2）与咨询历史（F1.4 子项）未实现** | PRD §3.1 场景 2、§5.1.1「排队→接入→聊天→转工单」、§5.1.2「咨询历史：回看历史聊天记录和关联工单」；工单详情「关联咨询」为占位卡；咨询是「完整服务闭环」的一半（prd §2.3 闭环链路：咨询→转工单→SLA→评价） | P1（E5 未排期，下期） | prd.md §5.1.1/§5.1.2；feature-list.md F1.2/F1.4；代码：chat/index.tsx 占位、detail.tsx:187-190 占位卡；ConsultationController 为 @Profile("demo") |
| A-5 | **附件能力客户端不可用**：提单 `attachmentIds` 恒为空数组、详情无附件展示、回复无附件 | PRD §5.1.1 在线咨询承诺「可发送文字、图片、附件」；管理端 F4.17 附件上传（MinIO）已实现但 `AttachmentController` 全部端点挂 `ATTACHMENT_UPLOAD/ATTACHMENT_DOWNLOAD` 管理权限码（AttachmentController.java:32,64,76,83）——**客户身份无权限码体系，客户端无法调用**（结构性障碍，非仅前端缺失）；工单/回复附件数据层存在（attachments 字段）但客户侧不呈现 | P1 | prd.md §5.1.1；feature-list.md F1.1 备注（attachmentIds 恒空）、F4.17；代码：new.tsx:54、mapTicketRow attachments:[]（customer-portal-live.ts:70）、AttachmentController.java:32（@RequirePermission） |
| A-6 | **通知偏好占位**（F1.11 子项）：邮件/站内信开关为 toast 占位 | PRD §5.1.5 已注明「通知偏好当前为占位（邮件/站内信开关即将上线）」——**PRD 已声明未承诺**，但 SMTP 依赖（prd §6.4 外部依赖「SMTP 服务，Sprint 0 必确认」）与后端通知中心（NotificationCenterService，`smtp-enabled` 默认 false 降级站内信）已存在，偏好层缺失使客户无法控制通知渠道 | P2（PRD 已注明占位，弱缺陷） | prd.md §5.1.5/§6.4；代码：me/index.tsx:62-72；NotificationCenterService.java:29,191（smtpEnabled）；后端无 preference 表/端点（grep preference 0 业务命中） |

### B 类：多业务域场景衍生缺失（建议）

| # | 缺口 | 为什么重要（多域/客户体验角度） | 优先级 | 依据 |
| :--- | :--- | :--- | :--- | :--- |
| B-1 | **通知中心无业务域维度**：`inbox_message` 表有 `business_domain_id`（可空）与索引 `fk_inbox_domain`，但 `InboxController.listInbox` 仅按 `recipient_subject_id` 查询、无 domain 过滤参数；客户端 `mapInboxMessage` 硬编码 `domainId: 0`，列表无域分组/筛选/标签 | 多业务域核心场景：**一个客户账号同时是 A 品牌与 B 品牌的客户**（prd §2.1「一个账号主体」），A 域工单通知与 B 域通知在通知中心混杂，无法按品牌区分；域隔离价值（§2.2）在客户侧通知体验上未体现 | **P1** | prd.md §2.2/§4.1.2；代码：V202605200002__rebaseline_current_schema.sql:450,462（business_domain_id 字段+索引）；InboxController.java:40（仅 userId）；customer-portal-live.ts:266-270（domainId: 0） |
| B-2 | **无跨域聚合视角**：客户端工单数据仅 `currentDomainTickets`（当前域），首页统计/列表随域切换整体变化，无「全部域」视图或按域筛选 | 多域客户（加入多个品牌）无法总览各品牌服务请求的全局状态；需逐域切换查看——多域运营的客户侧体验断裂；与 FR-05 域隔离不冲突（聚合视图按已入域过滤） | **P1** | prd.md §2.2（一套系统支撑多品牌独立运营）；代码：home/index.tsx（currentDomainTickets）、customer-portal.ts（snapshot 仅 activeDomain 工单）、customer-portal-live.ts:158-170（按单域刷新） |
| B-3 | **域品牌化缺失**：域 LOGO/名称/描述（F3.7 域基础设置已可配置）未在客户端呈现——登录页、专属域入口、AppShell 均为 UnionDesk 统一外壳，仅文本提示域名称 | 多域定位下每个业务域是**独立品牌/业务线**（prd §2.1）；客户通过品牌入口进入（如 `/d/:domainCode/login` 专属链接）看到的仍是 UnionDesk 品牌外壳，无域 LOGO/主题/服务条款可见性——多品牌独立运营的核心体验价值未兑现 | P1 | prd.md §2.1/§2.2；代码：F3.7（域端可配置 LOGO）；login/index.tsx:141-147（仅文本「专属入口 · {name}」）、AppShell.tsx（uniondesk 品牌 logo :118-120） |
| B-4 | **SLA 承诺不向客户呈现**：客户端工单数据（`CustomerTicketRow`）无任何 sla_* 字段；后端 ticket 表有 sla_* 字段（prd 附录 A）、SLA 引擎已实现（F4.15，SlaTimingEngine），但 my 端点 DTO 未暴露 SLA | 客户服务体验惯例：客户应能看到「预计响应/解决时间」承诺；SLA 是产品核心能力（prd §2.3 闭环：SLA 保障），配置了 SLA 规则的域（F3.2/F4.15 已实现）在客户侧完全不可见 | P1（依赖 E4 SLA UI 排期） | prd.md §2.3/附录 A；代码：api.ts:2489-2503（CustomerTicketRow 无 sla 字段）；TicketController.java:46-56（my 端点）；F4.15 引擎已实现（feature-list.md） |
| B-5 | **客户侧「域停用/客户被停用/被移出域」反馈缺失**：F4.1 域可启用/禁用、F3.3 客户可启停、F3.3 成员可移除——客户端仅校验 `joined`（domain-flow.ts:16-22），对域 status 变化、客户被停用场景无专门提示 | 多域隔离下客户与域的关系是动态的；被停用/域停用后客户仍看到入口但访问失败/数据异常（或继续看到缓存数据），无「该域服务暂不可用/请联系管理员」的明确反馈；FR-05（未入域拒绝）只覆盖「未加入」，不覆盖「被移出/被停用」 | P2 | prd.md §4.1.2（可见/注册策略）、foundation-rules.md DR（入域规则）；代码：domain-flow.ts:16-22（仅 joined 判断）、App.tsx RequireDomain（仅 activeDomain 判断） |
| B-6 | **「请求补充信息」的客户侧交互无专门化**：PRD F2.1 员工端承诺「请求客户补充信息」动作，后端无专门端点（waiting_customer 状态 + 公开回复承担语义），客户端无「客服请求补充信息」的醒目提示（仅普通公开回复流） | 工单协作的关键环节：客户在 waiting_customer 状态下的响应动线不明确，影响解决效率；该缺口同时是 A 类弱项（PRD F2.1 承诺未完全落地）与多域工单质量相关 | P2 | prd.md §5.2.1（F2.1 处理动作）；代码：TicketController.java（无 request-info 端点，仅 status/replies）；customer-portal-live.ts:35-49（waiting_customer 状态存在） |
| B-7 | **多域切换后的状态一致性提示**：切换域（switch-domain）后工单列表/首页整体替换（正确域隔离），但无「当前域」上下文在页面级提醒（如工单页标题无域标识），详情页 URL 无域参数（`/tickets/:id` 不带 domainId），**跨域工单 ID 冲突时无防护**（前端仅按 session 的 businessDomainId 拉取，若深层链接无域上下文则依赖单域 session） | 多域场景下的数据正确性风险：客户端无显式域上下文路由，切换域后共享 URL/书签可能指向错误域语义（实际 API 层有 domain 校验兜底，但客户侧无感知/无引导） | P2 | prd.md §4.1.2；代码：App.tsx 路由（/tickets/:ticketId 无域参数）、customer-portal-live.ts:194-201（域来自 session.businessDomainId） |

### C 类：行业惯例可选（参考）

| # | 缺口 | 为什么重要 | 优先级 | 依据 |
| :--- | :--- | :--- | :--- | :--- |
| C-1 | **手机号验证码登录/绑定**：登录仅账号/密码；注册有手机号字段但无短信验证 | 工单 SaaS 客户门户惯例；手机号已是注册必填项，验证码登录/绑定是自然延伸；后端无短信通道（grep sms 0 业务命中） | P2 | prd.md §5.1.3；代码：register/index.tsx（phone 必填 :116-124）、login/index.tsx（仅密码） |
| C-2 | **忘记密码**：登录页占位 toast「功能开发中」；后端无 forgot-password/reset 端点 | 客户自助能力的基本项；PRD §5.1.3 已注明「忘记密码当前为占位提示」（声明项，非隐藏缺陷）；无找回路径时只能依赖管理员重置密码（F3.5 已实现，域端可重置） | P2 | prd.md §5.1.3（已注明占位）；代码：login/index.tsx:68-71；后端 grep forgot-password 0 命中 |
| C-3 | **客户端会话/设备管理**：后端 `revokeSession/revokeSessionsByUser` 已实现（AuthController.java:153-163，admin 端点），客户端无登录设备列表/踢除入口 | prd §4.1.4 全局安全策略承诺「JWT + Refresh Token 轮换，支持单账号多会话踢除」——**安全承诺的技术能力已存在，但客户端无消费入口**（介于 A/B 类的弱项，归 C 类参考） | P2 | prd.md §4.1.4；代码：AuthController.java:153-163、AuthService.java:660-682；客户端无会话管理页面 |
| C-4 | **邮件通知通道客户可见性**：通知中心仅站内信；`smtp-enabled` 默认 false（降级站内信），SMTP 为外部依赖待确认 | 行业惯例（工单进展邮件）；受外部依赖（prd §6.4 SMTP 服务）与通知偏好（A-6）双重约束，通道能力已有骨架（NotificationCenterService 邮件模板/日志字段） | P2 | prd.md §6.4；代码：NotificationCenterService.java:29,38,191-207 |
| C-5 | **账号注销/删除**：前后端均无客户自助注销 | 行业惯例与合规（数据主体权利）；vision 非目标未提及；域端客户管理有启停（F3.3）但客户无自助退出 | P2 | 代码：后端 grep 注销/deleteAccount 0 命中；客户端无入口 |
| C-6 | **无障碍/深色模式/响应式细节**：`ud-rail` 桌面侧栏 + `ud-dock` 移动底部导航已实现（响应式基础有）；无深色模式、无系统级无障碍审计 | vision §4 明确 i18n 非目标（多语言不列）；深色模式/无障碍为增强项，prd §6.2 终端要求已部分满足（响应式） | P2 | vision.md §4；prd.md §6.2；代码：AppShell.tsx（rail+dock 双形态） |

---

## 3. 「明确不做」边界核对（缺口不与 vision 非目标冲突）

| vision/prd 明确不做 | 本报告处理 | 核对结果 |
| :--- | :--- | :--- |
| 知识库 / 帮助中心 / VitePress 文档站（vision §4；prd §2.6） | 未列为缺口；提单页/空态引导文案属现有功能 | ✅ 无冲突 |
| AI 自动回复 / 智能客服（vision §4） | 未列入 | ✅ 无冲突 |
| 高级可视化 BI 报表（vision §4） | 未列入（F1.9 首页统计卡为现有功能，非 BI） | ✅ 无冲突 |
| 外部供应商协作门户（vision §4） | 未列入 | ✅ 无冲突 |
| 国际化 i18n（vision §4） | 未列入（C-6 明确排除多语言） | ✅ 无冲突 |
| 历史工单/客户数据批量迁移（vision §4） | 未列入 | ✅ 无冲突 |
| 反馈/建议独立轻量入口（三态展示）——下期规划（prd §2.6/§5.1.1） | 未列入本期缺口（标注下期形态） | ✅ 无冲突 |
| 在线咨询（E5）与 SLA 完整 UI（E4）——backlog 排期项 | A-4/B-4 以「未排期/依赖 E4/E5」标注，未越界要求 | ✅ 为 backlog 既有承诺 |

---

## 4. 关键证据索引（代码路径）

| 证据点 | 位置 |
| :--- | :--- |
| 提单仅系统字段、附件恒空 | `UnionDeskWeb/apps/UnionDeskCustomerWeb/src/pages/tickets/new.tsx:54,145-166` |
| 在线咨询占位页 | `UnionDeskWeb/apps/UnionDeskCustomerWeb/src/pages/chat/index.tsx`（15 行静态） |
| 关联咨询占位卡 | `UnionDeskWeb/apps/UnionDeskCustomerWeb/src/pages/tickets/detail.tsx:187-190` |
| 工单附件/回复附件映射为空 | `UnionDeskWeb/packages/shared/src/customer-portal-live.ts:70,233` |
| 注册 mock（本地状态机） | `UnionDeskWeb/apps/UnionDeskCustomerWeb/src/pages/register/index.tsx:40-52`；`UnionDeskWeb/packages/shared/src/customer-portal.ts:374,620` |
| live 层无注册/邀请 API | `UnionDeskWeb/packages/shared/src/customer-portal-live.ts`（imports 无 register/join） |
| 后端注册已实现（DR-01/DR-02） | `UnionDesk/uniondesk-app/src/main/java/com/uniondesk/auth/web/AuthController.java:63-67`；`uniondesk-app/.../auth/core/AuthService.java:362-394` |
| 邀请码校验（有效期/次数） | `UnionDesk/uniondesk-domain/src/main/java/com/uniondesk/domain/core/InvitationCodeService.java:68-78` |
| 咨询仅 demo 实现 | `UnionDesk/uniondesk-support/src/main/java/com/uniondesk/consultation/web/ConsultationController.java:26`（@Profile("demo")） |
| 附件端点挂管理权限码 | `UnionDesk/uniondesk-support/src/main/java/com/uniondesk/attachment/web/AttachmentController.java:32,64,76,83` |
| 通知中心 SMTP 开关（默认 false） | `UnionDesk/uniondesk-support/src/main/java/com/uniondesk/notification/core/NotificationCenterService.java:29,191-207` |
| 站内信表含 business_domain_id | `UnionDesk/uniondesk-app/src/main/resources/db/migration/current/V202605200002__rebaseline_current_schema.sql:445-466` |
| inbox 查询无域过滤 | `UnionDesk/uniondesk-support/src/main/java/com/uniondesk/notification/web/InboxController.java:35-42` |
| 客户端 inbox 硬编码 domainId:0 | `UnionDeskWeb/packages/shared/src/customer-portal-live.ts:266-270` |
| 客户端工单 DTO 无 SLA 字段 | `UnionDeskWeb/packages/shared/src/api.ts:2489-2503` |
| 会话踢除后端已实现（客户端无 UI） | `UnionDesk/uniondesk-app/src/main/java/com/uniondesk/auth/web/AuthController.java:153-163` |
| 忘记密码占位 toast | `UnionDeskWeb/apps/UnionDeskCustomerWeb/src/pages/login/index.tsx:68-71` |
| 通知偏好占位 toast | `UnionDeskWeb/apps/UnionDeskCustomerWeb/src/pages/pages/me/index.tsx`（实为 `src/pages/me/index.tsx:62-72`） |
| 专属域入口仅文本品牌 | `UnionDeskWeb/apps/UnionDeskCustomerWeb/src/pages/login/index.tsx:141-147`；AppShell.tsx:118-120 |
| 未入域拒绝（FR-05 前端） | `UnionDeskWeb/apps/UnionDeskCustomerWeb/src/App.tsx:88-94`（RequireDomain）；domain-flow.ts:16-22 |

---

## 5. Caveats / 未找到

- **任务描述引用的 4 个既有研究文件不存在**：`customer-web-pages.md`、`prd-detail-permission-gap.md`、`story-inventory.md`、`prd-alignment-notes.md` 在任务 research/ 目录中未找到（目录初始为空）。若主代理预期引用其内容，需另行确认来源（可能在其他任务目录或尚未生成）。
- **implementation-inventory.md 无客户端章节**：客户端实现状态以 feature-list.md（2026-08-11 快照）+ 本次代码勘察为准，未做后端全量回归。
- 后端搜索范围：`UnionDesk/` 下 Java 源码与迁移文件；`consultation_message`、`inbox_message` 等表结构来自当前 rebaseline 迁移；未逐表核对历史归档迁移差异。
- `NotificationTemplateController` 存在（域通知模板 CRUD 后端），但客户端无通知模板消费场景（通知偏好 A-6 与其相关），域端 F3.11 前端仍为占位。
- 状态「已实现」的判定基于代码路径可用性（真实 API 调用 + 页面渲染），未进行运行时联调验证。
