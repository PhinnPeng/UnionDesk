# 设计：CustomerWeb 多域登录与风险登录通知

## 1. Boundaries

| In | Out |
|----|-----|
| CustomerWeb `/login`、`/d/:domainCode/login` 固化 live | 注册 / 邀请码真实入域 |
| 账号级常用 IP + 新 IP 站内风险通知（不阻断） | 二次输码 / pendingToken |
| Inbox live（至少风险通知可见） | Admin 风险登录策略 |
| `switch-domain` 与专属入口衔接 | 子域 / 域品牌主题 |
| shared `customer-portal-live` / api 封装 | 平台控制台大改 |

## 2. Identity & domain model

```text
customer_account (统一登录主体)
       │ 1:N
       ▼
domain_customer (域客户关系 = 可访问业务域)
       │
       ▼
JWT / auth_login_session.business_domain_id = 当前作业域
```

- **登录按账号**：`POST /auth/login` + `X-UD-Client-Code: ud-customer-web`。
- **数据按域隔离**：工单、类型等 API 使用会话当前域；换域走 `POST /auth/switch-domain`。
- **「不同业务域客户不同」**：体现在关系表与域内数据，而非每域一套密码账号。

## 3. Entry modes

### 3.1 全局门户 `/login`

1. 拉 `login-config` → 可选滑块拿 `captchaToken`。
2. `loginCustomerLive` → hydrate 快照。
3. 路由：
   - `defaultBusinessDomainId > 0` 或已有 `activeDomain` → `/home`
   - 否则 → `/domains`
4. 支持 `?next=` 仅在已有当前域时优先回首页（保持现逻辑意图）。

### 3.2 专属入口 `/d/{domainCode}/login`

1. 同上登录（账号级，**请求体不强制带 domainId**）。
2. 登录成功后 `enterDedicatedDomain(domainCode)`：
   - 在 `accessibleDomains` 中按 `code` 匹配；
   - 已加入 → `switchDomain` → `/home`；
   - 未加入 / 无匹配 → 保持会话，`/domains` + 中文提示（不自动入域、不泄露其它域私有信息）。
3. 专属页可展示域名称：优先登录后匹配；未登录前若无公开域 API，则仅展示 code 或通用文案（本期可不新增公开 API）。

## 4. Auth sequence (C + B)

```text
密码 + captchaToken?
        │
        ▼
验密成功 ──► 签发 JWT / session（写入 clientIp, UA）
        │
        ├─ IP ∈ trusted_login_ip（同账号）
        │     → 更新 last_used_at
        │     → 不发风险站内信
        │
        └─ IP ∉ trusted（含首次登录）
              → 写 inbox 风险通知（B）
              → 审计 RISK_LOGIN_NEW_IP
              → upsert IP 到 trusted
              → 仍返回正式 token（不阻断）
              → 可选响应字段 riskLoginNotified=true
```

### 4.1 Captcha（C）

- 沿用现有 `AuthCaptchaService` + `login-config.captchaEnabled`。
- Customer 登录在 captcha 开启时必须带有效一次性 `captchaToken`（与现网 staff/customer 规则对齐，缺口则补齐 customer 分支校验）。

### 4.2 Trusted IP（账号级）

**表** `auth_customer_trusted_login_ip`（名称可微调，语义如下）：

| 列 | 说明 |
|----|------|
| id | PK |
| user_id | 客户账号 id |
| client_ip | 规范化字符串（IPv4/IPv6 文本） |
| last_used_at | 最近成功登录 |
| created_at | 首次信任时间 |

- 唯一键：`(user_id, client_ip)`。
- 建议上限：每账号 10 条；超出按 `last_used_at` LRU 删除最旧。
- 仅 `ud-customer-web` / customer 登录路径维护。

**首次登录**：无任何 trusted 行时，当前 IP 视为「新」→ 发一次风险通知并写入 trusted（用户能立刻在 inbox 看到，符合「有记录」；若产品希望首次静默，可改为「空表仅写入不通知」——**本期默认：首次也通知一次**，文案可为「首次在该环境登录」）。

### 4.3 Risk inbox message（B）

- 调用 `NotificationCenterService` 扩展方法，例如 `notifyCustomerRiskLogin(userId, domainIdOrNull, ip, uaSummary, at)`。
- 标题：`登录环境提醒`；正文含 IP、时间、UA 摘要；`kind/code`：`security.risk_login`。
- `business_domain_id`：优先会话默认域 / 首个已加入域；**0 域账号**：若 inbox 要求非空域则跳过站内信、仅写 `auth_login_log` 审计；否则允许 null（以表结构为准，实现时读 `inbox_message` 约束）。

### 4.4 Login response extension（可选）

在现有 `LoginResponse` 增加：

- `riskLoginNotified: boolean`（camelCase；后端可 snake_case + 别名）

前端 toast：新环境时提示已发站内提醒。

## 5. Frontend modules

| 模块 | 行为 |
|------|------|
| `pages/login` | 继续 `loginCustomerLive`；识别 `riskLoginNotified`；专属走 `enterDedicatedDomain` |
| `utils/domain-flow` | 保持「匹配 code → switch → home / domains」 |
| `pages/domains` | 切域 live；邀请码区保留但标明后续/演示（不扩大为真实入域） |
| `pages/inbox` | 改为 live：`fetchP0Inbox` / unread / mark read；映射为门户列表 UI |
| `App.tsx` | 维持 `RequireSession` / `RequireDomain` 分层 |
| `customer-portal-live.ts` | 登录 hydrate；inbox 拉取与同步辅助 |

Mock `customer-portal.ts` 可保留给本地演示，但登录页与 inbox **主路径以 live 为准**。

## 6. Backend touchpoints

| 组件 | 改动 |
|------|------|
| Flyway | 新表 `auth_customer_trusted_login_ip` |
| `AuthService.loginCustomer` | captcha 强制（若缺）；trusted IP 判定；风险通知；响应标志 |
| TrustedIp 小服务/仓库 | list / upsert / prune |
| `NotificationCenterService` | risk login 通知 |
| `AuthDtos.LoginResponse` | 可选 `riskLoginNotified` |
| Inbox API | 已有则只接前端；权限确保客户可 `inbox.read` |

## 7. API contracts (summary)

| Method | Path | Auth | Notes |
|--------|------|------|-------|
| GET | `/api/v1/auth/login-config` | public | captcha 开关 |
| POST | `/api/v1/auth/captcha/*` | public | 现有 |
| POST | `/api/v1/auth/login` | public + client code | 成功可带 `risk_login_notified` |
| POST | `/api/v1/auth/switch-domain` | Bearer | 域必须在可访问列表 |
| GET | `/api/v1/inbox` | Bearer | 风险通知列表 |
| PUT | `/api/v1/inbox/{id}/read` | Bearer | 标已读 |

不新增「环境验证码」类公开接口。

## 8. Security & UX rules

- 登录失败文案统一，防账号枚举。
- 专属链未加入域：不自动注册、不返回其它客户信息。
- B 不阻断：攻击者有密码仍可进；B 的价值是 **其它已登录设备/事后审计可见**；C 抑制机器人。
- IP 取自网关/servlet `remoteAddr`（与现 session 一致）；若前有反代，依赖既有转发头配置（本期不新造 IP 解析框架）。

## 9. Compatibility / Rollback

- 纯增量表 + Auth 钩子 + CustomerWeb/shared；回滚：还原迁移（或停用钩子）、还原 FE inbox/login 提示。
- 旧客户端忽略未知 JSON 字段仍可登录。

## 10. Rollout shape

1. 后端：表 + trusted IP + 登录钩子 + 通知  
2. shared：登录类型字段；inbox live helpers（若尚未完整）  
3. CustomerWeb：登录提示 + inbox 接 live + 回归专属/全局路由  

## 11. Open implementation notes（非产品分歧）

- Inbox 表 `business_domain_id` 是否可空：实现时按 schema 分支。  
- 首次登录是否通知：本期 **通知**（见 4.2）。  
- 同 IP 短时间重复登录：仅更新 `last_used_at`，不重复插站内信。
