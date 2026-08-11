# Research: CustomerWeb 页面勘察

- **Query**: 对 `UnionDeskWeb/apps/UnionDeskCustomerWeb/src/pages/` 13 个页面逐一勘察（功能摘要 / 实现状态 / 路由 / 缺口）
- **Scope**: internal（代码勘察）
- **Date**: 2026-08-11

## Findings

### 路由骨架（`src/App.tsx` + `src/routes/index.ts`）

| 路由 | 守卫 |
|:---|:---|
| `/login`、`/register`、`/d/:domainCode/login`、`/d/:domainCode/register` | 公开 |
| `/change-password` | RequireSession（未登录→`/login`；`mustChangePassword` 强制） |
| `/domains`、`/me` | RequireSession + AppShell |
| `/home`、`/workspace`(→重定向 `/home`)、`/tickets`、`/tickets/new`、`/tickets/:ticketId`、`/chat`、`/inbox` | RequireSession + AppShell + RequireDomain（无 activeDomain→`/domains`） |
| `/` | LandingRedirect；`*` → `/` |

- 底部导航 5 Tab（`AppShell.tsx`）：首页 / 工单 / 咨询（rail 上标「二期」soon）/ 通知（未读 badge）/ 我的；顶栏搜索 → `/tickets?q=`
- 会话恢复：`SessionBootstrap` → `restoreCustomerPortalLive()`（`packages/shared/src/customer-portal-live.ts`），本地 demo token（`cust-at` 前缀）跳过恢复

### 页面清单

| # | 页面文件 | 路由 | 状态 | 功能摘要 | 关键证据 |
|:---|:---|:---|:---|:---|:---|
| 1 | `pages/login/index.tsx` | `/login`、`/d/:domainCode/login` | **已实现（真实 API）** | 账号/密码登录、滑块验证（`fetchLoginConfig` 开关 + `createCaptchaChallenge`/`verifyCaptcha`）、记住账号（localStorage）、专属域入口（`enterDedicatedDomain`→`selectCustomerDomainLive`）、新环境登录站内提醒 | `loginCustomerLive` → `api.login`；**缺口：忘记密码 = toast「忘记密码功能开发中」占位**；页脚演示提示 customer/customer123 |
| 2 | `pages/login/LoginCaptcha.tsx` | 组件（非页面） | 已实现 | 滑块验证封装（shared `SliderCaptcha` + 后端 challenge 缓存） | challenge 5s 过期缓冲 |
| 3 | `pages/register/index.tsx` | `/register`、`/d/:domainCode/register` | **部分（本地 mock）** | 注册表单：显示名/登录名/手机号/密码/邮箱（可选）/开放域下拉/邀请码；专属域注册预填邀请码 | `portal.register` = `registerCustomer`（`customer-portal.ts:620`）**本地状态机 mock，无真实 API**；成功提示「注册成功（本地演示）」；US-S3-02 Todo；shared `api.ts` 无 register 端点 |
| 4 | `pages/home/index.tsx` | `/home` | **已实现（真实 API）** | 问候语、待处理提醒、生命周期统计卡（待处理/进行中/已完成）、我的工单最近 5 条、未读通知前 3 条（jumpUrl 跳转，`/workspace`→`/home` 兜底） | `refreshCustomerTicketsLive` → `listCustomerMyTickets` |
| 5 | `pages/domains/index.tsx` | `/domains` | **部分** | 已加入 / 可加入 / 需管理员开通三组卡片；切换域；邀请码加入 | 切换 = `selectCustomerDomainLive`（真实 switch-domain API）；**邀请码加入 = `joinDomainByInvitation` 本地 mock**（文案自注「P1 将接真实入域 API」） |
| 6 | `pages/tickets/new.tsx` | `/tickets/new` | **已实现（真实 API）** | 三步：选类型（启用类型卡片）→ 填标题/描述 → 成功展示工单号 | `refreshCustomerTicketTypesLive` + `createCustomerTicketLive` → `createCustomerMyTicket`（POST）；**缺口：动态表单字段未渲染（PRD F1.1 动态表单仅 title/description 静态字段）**；attachmentIds 恒空数组 |
| 7 | `pages/tickets/index.tsx` | `/tickets` | **已实现（真实 API）** | 类型侧栏（带计数）、生命周期筛选（pending/active/done）、关键词搜索（标题/工单号/类型，同步 URL `q`/`life` 参数） | `refreshCustomerTicketsLive`；无分页（前端全量 slice） |
| 8 | `pages/tickets/detail.tsx` | `/tickets/:ticketId` | **已实现（真实 API）** | 工单信息、公开动态时间线（作者/类型/时间）、补充说明、撤回（仅 open 态，version 乐观锁） | `getCustomerTicketLive` / `replyCustomerTicketLive` / `withdrawCustomerTicketLive`；**缺口：满意度评价入口无（PRD F1.5 全站无）；关联咨询 = 占位卡「二期可从会话转工单」；附件展示无** |
| 9 | `pages/chat/index.tsx` | `/chat` | **占位** | 「在线咨询即将开放」二期能力页，唯一动作「去提交工单」 | 纯静态 JSX；E5 未排期；导航标「二期」 |
| 10 | `pages/inbox/index.tsx` | `/inbox` | **已实现（真实 API）** | 站内信列表（kind 分类 system/ticket/domain）、未读数、标为已读、查看跳转（jumpUrl） | `fetchCustomerInboxLive` / `markCustomerInboxReadLive` → P0 inbox API（`fetchP0InboxPage` 等） |
| 11 | `pages/me/index.tsx` | `/me` | **混合** | 账号信息（显示名/登录名/手机）、业务域/通知入口、退出登录；专属入口示例 | 退出 = `logoutCustomerLive`（真实）；**缺口：通知偏好 = toast「UI 占位」（邮件/站内信即将上线）** |
| 12 | `pages/change-password/index.tsx` | `/change-password` | **已实现（真实 API）** | 当前密码/新密码/确认，前端校验（≥6 位、一致、不同），成功后回登录页 | `changePassword`（shared API）+ `updateStoredMustChangePassword(false)` |

### 联调状态结论

- 登录、工单链路（列表/新建/详情/回复/撤回）、通知、改密、切域：**均接真实 API**（`@uniondesk/shared` `customer-portal-live.ts` + `api.ts`），联调状态为「已实现」而非待确认
- **注册（`portal.register`）与邀请码入域（`joinDomainByInvitation`）仍是本地 mock**：in-memory 账号 + 伪 token（`cust-at` 前缀），不调 `/api/v1/auth/register` —— 与 US-S3-02（Todo）一致
- chat 纯占位（E5）；忘记密码占位；通知偏好占位；满意度评价全站无（grep `满意度|satisfaction|evaluation|评价` 0 命中）

## Caveats / Not Found

- 「13 个页面」口径：12 个页面文件 + `LoginCaptcha` 组件 + routes 配置；`pages/` 下另有 `login.css`、`assets/`（图片）非页面
- `tickets/new` 动态表单：管理端 Formily 设计器已产出 `form_schema`，但 CustomerWeb 提单页未按 schema 渲染动态字段（仅系统字段 title/description）
