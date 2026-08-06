# CustomerWeb 多域登录与风险登录通知

## Goal

将 CustomerWeb 门户登录固化为统一客户账号鉴权，并正确接入多业务域上下文（全局入口 + 专属域入口）；在滑动验证（C）基础上，对新登录 IP 写入站内风险通知（B，仅通知、不阻断登录），且用户能在门户通知中看到该提醒。

## Background

- 密码登录已走 live：`loginCustomerLive` → `POST /api/v1/auth/login`（`ud-customer-web`）。
- 滑块验证码链路已存在（`login-config` + captcha challenge/verify）。
- 客户账号跨域唯一；域隔离靠 `domain_customer` 可访问列表 + 会话 `businessDomainId`。
- 专属路径 `/d/:domainCode/login` 已有前端骨架；邀请入域 UI 仍为 mock（本期不做真实入域）。
- Inbox 页仍偏 mock；若不接 live，B 对用户不可见。
- 无「常用登录 IP」与「新环境风险通知」能力。

## Confirmed Decisions

| 项 | 结论 |
|:---|:---|
| 账号模型 | 统一客户账号登录；不做「每域独立登录账号」 |
| 域关系 | 登录只消费已加入域；未加入则进选域页/提示，不自动入域 |
| C | 跟随全局 `login-config.captchaEnabled` 的滑动验证 |
| B | 新 IP → 站内风险通知 + 审计；**不阻断、不二次输码** |
| 常用 IP 粒度 | 挂在客户账号（与域无关） |
| Inbox | 本期包含 live 接入，至少能展示风险登录通知 |
| 注册/邀请码真实入域 | 本期 Out |

## Requirements

### R1 全局与专属登录入口

- `/login`：验密（+ 可选滑块）成功后，有可用当前域则进 `/home`，否则 `/domains`。
- `/d/{domainCode}/login`：登录成功后若已加入该域则 `switch-domain` 进 `/home`；未加入则留在会话并导向 `/domains` 提示；域不存在时友好错误。

### R2 多域会话

- 登录响应 `accessibleDomains` / `defaultBusinessDomainId` 驱动门户快照。
- 选域页 `selectCustomerDomainLive` → `POST /auth/switch-domain`；后续工单等 API 以会话当前域为准。
- 0 域账号允许登录，困在 `/domains`（可进「我的」）。

### R3 滑动验证（C）

- 按 `GET /auth/login-config`；开启时无 `captchaToken` 不可登录；失败刷新滑块。

### R4 常用 IP 与风险通知（B）

- 维护账号级常用登录 IP；登录成功时比对当前 `clientIp`。
- 新 IP：异步/同步写入站内信风险提醒（IP、时间、UA 摘要）+ 审计事件；仍签发正式 JWT。
- Upsert 当前 IP 到常用列表（建议上限 + LRU）。
- 前端可提示「检测到新登录环境，已发送站内提醒」（可选 `riskLoginNotified`）。

### R5 Inbox live

- CustomerWeb `/inbox` 读取真实 inbox API，能展示本账号风险登录通知。

## Out of Scope

- 注册页 / 邀请码真实入域 API
- 新 IP 二次验证码或 pendingToken 拦登录
- Admin 端同等风险策略
- 域级品牌主题 / 子域托管
- 常用 IP 用户自助管理页（可作为后续）

## Acceptance Criteria

- [x] AC1：全局登录成功后按域数量正确进入 `/home` 或 `/domains`
- [x] AC2：专属 `/d/{code}/login` 已加入域可直达首页；未加入不踢登录并提示选域/联系开通
- [x] AC3：`login-config` 开启滑块时未完成不可登录；完成后方可成功
- [x] AC4：首次或新 IP 登录成功后站内信出现风险登录通知，且登录不被阻断
- [x] AC5：同一常用 IP 再次登录不重复刷「新环境」类风险通知（或仅刷新 last_used，不新增告警）
- [x] AC6：Inbox 页展示真实通知（含上述风险消息），不再仅依赖本地 mock 演示数据作为唯一来源
- [x] AC7：切域后会话域与门户当前域一致；无 CustomerWeb 外无关模块夹带大改

## Constraints

- 前端：CustomerWeb 现有壳与中文文案；shared live 封装优先，页面不裸拼 axios。
- 后端：不引入 DB 外键新依赖风格违背项目约定；Flyway 迁移增量。
- 外科手术：不顺手做注册/邀请真实化。
