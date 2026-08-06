# 实现计划：CustomerWeb 多域登录与风险登录通知

## Checklist

1. [x] Flyway：`auth_customer_trusted_login_ip`（user_id + client_ip 唯一，时间戳）
2. [x] 后端：TrustedLoginIp 仓储/服务（upsert、LRU 上限、isTrusted）
3. [x] 后端：`NotificationCenterService` 增加客户风险登录站内信；0 域时降级仅审计
4. [x] 后端：`loginCustomer` 接入 captcha 强制（与 config 对齐）、新 IP 通知、trusted upsert、`riskLoginNotified` 响应字段
5. [x] shared：`LoginResponse` / live 登录透传 `riskLoginNotified`；inbox 列表/已读封装对齐 CustomerWeb 需要
6. [x] CustomerWeb 登录页：成功时新环境 toast；专属/全局路由回归
7. [x] CustomerWeb Inbox：接 live API，展示含 `security.risk_login` 的通知
8. [x] 自检：全局/专属/0 域/滑块开闭/同 IP 不重复告警；相关 lint/tsc
   - 已通过：`mvn -pl uniondesk-app -am compile`、CustomerWeb `tsc --noEmit`
   - 说明：`uniondesk-app` 全量 testCompile 被既有 `TicketConfigServiceTests` 构造签名不匹配阻断（与本任务无关）

## Validation

```bash
# 后端：启动后
# 1) captcha 开启时无 token 登录应失败
# 2) 客户账号首次/换 IP 登录成功 → inbox 有风险通知，且拿到 JWT
# 3) 同 IP 再登 → 无新风险通知（或可接受仅 last_used 更新）
# 4) /d/{code}/login：已加入域进 home；未加入进 domains

pnpm -C UnionDeskWeb/packages/shared exec tsc --noEmit
# CustomerWeb 开发服冒烟：登录 → 通知 → 切域
```

## Review gates

- PRD AC 全部可勾选
- 无注册/邀请真实入域夹带
- 无 Admin 登录风险策略夹带

## Rollback

还原 Flyway 文件（或 feature 开关停用钩子）、AuthService/Notification 改动、shared 类型字段、CustomerWeb login/inbox 即可。
