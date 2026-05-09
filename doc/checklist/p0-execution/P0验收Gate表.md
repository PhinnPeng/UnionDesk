# P0 验收 Gate 表

> 角色：P0 验收与发布 Gate 负责人。  
> 范围：仅覆盖 `doc/P0验收用例表.md` 中的 53 条 P0 用例；`TC-019` 仅作为 P1 参考，不计入 P0 Gate。
> 当前判断：**暂不允许进入 P0 联调/验收**。原因是 G0-G4 相关前后端、数据库与部署验证均未清零，且本次未完成实测验证。

## 状态说明

- `未联调`：对应后端 / 前端 / 数据库能力尚未完成或未完成联调。
- `待验证`：已有改动或任务拆解，但尚未通过命令验证。
- `P1参考`：仅用于后续版本，不阻塞 P0。

## Gate 表

| 用例编号 | 负责人 | 依赖接口 | 验证命令 | 当前状态 | 阻塞项 |
|---|---|---|---|---|---|
| TC-001 | 后端主责，前端/DB 协同 | `/admin/domains`，`business_domain`，`visibility_policy_codes` | `.\mvnw.cmd -q test` | 后端：未联调；前端：未联调；数据库：未联调 | 域 CRUD、可见策略、注册策略未完成 |
| TC-002 | 后端主责，前端协同 | 域直链入口，`/auth/me`，`business_domain` | `.\mvnw.cmd -q test && pnpm --dir UnionDeskWeb/apps/UnionDeskCustomerWeb test:run` | 后端：未联调；前端：未联调；数据库：未联调 | 直链可见性与未授权提示未完成 |
| TC-003 | 后端主责，前端/DB 协同 | `/admin/domains/{id}/customers`，`invitation_code`，`domain_customer` | `.\mvnw.cmd -q test` | 后端：未联调；前端：未联调；数据库：未联调 | 入域、邀请码、状态流转未完成 |
| TC-004 | 后端主责，前端协同 | `/api/v1/auth/login`，`/auth/me`，`identity_subject` | `.\mvnw.cmd -q test && pnpm --dir UnionDeskWeb/apps/UnionDeskCustomerWeb test:run` | 后端：未联调；前端：未联调；数据库：未联调 | 双身份登录态与无客户权限提示未完成 |
| TC-005 | 后端主责，前端协同 | `/api/v1/auth/login`，`/password/reset`，`login_log` | `.\mvnw.cmd -q test && pnpm --dir UnionDeskWeb/apps/UnionDeskCustomerWeb test:run` | 后端：未联调；前端：未联调；数据库：未联调 | 锁定、找回密码、错误提示未完成 |
| TC-006 | 后端主责 | `/auth/refresh`，会话表 | `.\mvnw.cmd -q test` | 后端：未联调；前端：未联调；数据库：未联调 | Refresh 轮换与并发会话踢除未完成 |
| TC-007 | 后端主责，前端协同 | `/api/v1/auth/step-up`，`X-UD-Step-Up-Token` | `.\mvnw.cmd -q test` | 后端：未联调；前端：未联调；数据库：未联调 | step-up 二次验证链路未完成 |
| TC-008 | 后端主责，前端/DB 协同 | 角色/权限接口，`domain_member_role`，`domain_role_permission` | `.\mvnw.cmd -q test && pnpm --dir UnionDeskWeb/apps/UnionDeskAdminWeb test` | 后端：未联调；前端：未联调；数据库：未联调 | RBAC 勾选、数据范围、菜单控制未完成 |
| TC-009 | 后端主责 | 角色上限校验，`domain_role` | `.\mvnw.cmd -q test` | 后端：未联调；前端：未联调；数据库：未联调 | 20 个自定义角色上限未完成 |
| TC-010 | 后端主责 | `auth_version`，权限变更失效链路 | `.\mvnw.cmd -q test` | 后端：未联调；前端：未联调；数据库：未联调 | 权限变更后 Token 立即失效未完成 |
| TC-011 | 后端主责 | `platform_admin` 保护规则 | `.\mvnw.cmd -q test` | 后端：未联调；前端：未联调；数据库：未联调 | 最后一名业务域超级管理员保护未完成 |
| TC-012 | 后端主责 | `domain_admin` 保护规则 | `.\mvnw.cmd -q test` | 后端：未联调；前端：未联调；数据库：未联调 | 最后一名域管理员保护未完成 |
| TC-053 | 后端主责，前端协同 | `/api/v1/admin/staff/{staff_id}/platform-roles`，`platform_role` | `.\mvnw.cmd -q test && pnpm --dir UnionDeskWeb/apps/UnionDeskAdminWeb test` | 后端：未联调；前端：未联调；数据库：未联调 | 最后一名平台管理员保护未完成 |
| TC-013 | 后端主责，前端/DB 协同 | `POST /domains/{id}/tickets`，`ticket`，`ticket_type`，`ticket_history` | `.\mvnw.cmd -q test && pnpm --dir UnionDeskWeb/apps/UnionDeskCustomerWeb test:run` | 后端：未联调；前端：未联调；数据库：未联调 | 客户提单、编号生成、落历史未完成 |
| TC-014 | 后端主责，前端协同 | `claim`，`assign`，`reply`，`status`，`ticket_reply` | `.\mvnw.cmd -q test && pnpm --dir UnionDeskWeb/apps/UnionDeskAdminWeb test` | 后端：未联调；前端：未联调；数据库：未联调 | shared 层 ticket aliases 已对齐，前端 ticket 详情页仍待切换到新 API |
| TC-015 | 后端主责，前端协同 | `withdraw`，`ticket.status` | `.\mvnw.cmd -q test && pnpm --dir UnionDeskWeb/apps/UnionDeskCustomerWeb test:run` | 后端：未联调；前端：未联调；数据库：未联调 | 客户撤回与状态限制未完成 |
| TC-016 | 后端主责 | `version`，乐观锁校验 | `.\mvnw.cmd -q test` | 后端：未联调；前端：未联调；数据库：未联调 | 并发冲突提示与版本控制未完成 |
| TC-017 | 后端主责 | 工单状态机 | `.\mvnw.cmd -q test` | 后端：未联调；前端：未联调；数据库：未联调 | 非法状态流转拒绝未完成 |
| TC-018 | 后端主责，DB 协同 | `sla_rule`，`sla_calendar`，ticket SLA 字段 | `.\mvnw.cmd -q test` | 后端：未联调；前端：未联调；数据库：未联调 | 基础 SLA 计时与预警未完成 |
| TC-019 | QA 参考 | `waiting_customer`，SLA 暂停/恢复 | `.\mvnw.cmd -q test` | P1参考 | 不阻塞 P0 |
| TC-020 | 后端主责，前端协同 | `audit_log`，审计查询接口 | `.\mvnw.cmd -q test && pnpm --dir UnionDeskWeb/apps/UnionDeskAdminWeb test` | 后端：未联调；前端：未联调；数据库：未联调 | 审计落库与筛选未完成 |
| TC-021 | 后端主责，前端协同 | `login_log`，登录日志接口 | `.\mvnw.cmd -q test && pnpm --dir UnionDeskWeb/apps/UnionDeskAdminWeb test` | 后端：未联调；前端：未联调；数据库：未联调 | 登录/失败日志未完成 |
| TC-022 | 后端主责，前端协同 | `inbox_message`，`notification_log`，`notification_template` | `.\mvnw.cmd -q test && pnpm --dir UnionDeskWeb/apps/UnionDeskCustomerWeb test:run` | 后端：未联调；前端：未联调；数据库：未联调 | 站内信必达与 SMTP 降级未完成 |
| TC-023 | 后端主责，前端协同 | 站内信未读数接口 | `.\mvnw.cmd -q test && pnpm --dir UnionDeskWeb/apps/UnionDeskCustomerWeb test:run` | 后端：未联调；前端：未联调；数据库：未联调 | 已读/未读状态未完成 |
| TC-024 | 后端主责 | XSS 净化链路 | `.\mvnw.cmd -q test` | 后端：未联调；前端：未联调；数据库：未联调 | 输入净化与安全展示未完成 |
| TC-025 | 后端主责，DB 协同 | `blocked_word`，命中审计 | `.\mvnw.cmd -q test` | 后端：未联调；前端：未联调；数据库：未联调 | 关键字屏蔽与审计未完成 |
| TC-026 | 后端主责 | IP 白名单校验 | `.\mvnw.cmd -q test` | 后端：未联调；前端：未联调；数据库：未联调 | 员工端 IP 白名单未完成 |
| TC-027 | 后端/运维主责 | `/api/v1/health`，`/api/v1/readiness` | `docker compose up -d && .\mvnw.cmd -q test` | 后端：未联调；前端：未联调；数据库：未联调 | 健康检查与就绪检查未完成 |
| TC-028 | 后端/DB 主责 | Flyway 迁移链路 | `.\mvnw.cmd -q test` | 后端：未联调；前端：不适用；数据库：未联调 | 迁移脚本与 schema 一致性未完成 |
| TC-029 | 后端/运维主责 | CI/CD 流水线 | `.\mvnw.cmd -q test` | 后端：待验证；前端：待验证；数据库：待验证 | CI/CD 基线未落地 |
| TC-030 | 后端/运维主责 | Docker Compose 单机部署 | `docker compose up -d && .\mvnw.cmd -q test` | 后端：未联调；前端：未联调；数据库：未联调 | 单机部署链路未完成 |
| TC-031 | 前端主责 | 响应式布局页面 | `pnpm --dir UnionDeskWeb/apps/UnionDeskCustomerWeb test:run && pnpm --dir UnionDeskWeb/apps/UnionDeskAdminWeb test` | 前端：未联调；后端：接口待联调；数据库：不适用 | 手机/平板/桌面适配未完成 |
| TC-032 | 前端主责 | 浏览器兼容性 | `pnpm --dir UnionDeskWeb/apps/UnionDeskCustomerWeb test:run` | 前端：未联调；后端：接口待联调；数据库：不适用 | 多浏览器兼容未完成 |
| TC-033 | 前端主责 | 中文文案资源 | `pnpm --dir UnionDeskWeb/apps/UnionDeskCustomerWeb test:run && pnpm --dir UnionDeskWeb/apps/UnionDeskAdminWeb test` | 前端：未联调；后端：不适用；数据库：不适用 | 简体中文文案未完成 |
| TC-034 | 后端主责，前端协同 | `staff_account`，离职池接口 | `.\mvnw.cmd -q test && pnpm --dir UnionDeskWeb/apps/UnionDeskAdminWeb test` | 后端：未联调；前端：未联调；数据库：未联调 | 离职池释放权限与回退未完成 |
| TC-035 | 后端主责，前端/DB 协同 | `/attachments/presign`，`/confirm`，`attachment_policy` | `.\mvnw.cmd -q test && pnpm --dir UnionDeskWeb/apps/UnionDeskCustomerWeb test:run` | 后端：未联调；前端：未联调；数据库：未联调 | 统一附件管理与白名单未完成 |
| TC-054 | 后端主责，前端协同 | `/api/v1/attachments/upload`，`/download`，`file_attachment` | `.\mvnw.cmd -q test && pnpm --dir UnionDeskWeb/apps/UnionDeskCustomerWeb test:run` | 后端：未联调；前端：未联调；数据库：未联调 | 本地附件降级链路未完成 |
| TC-036 | 后端主责，前端协同 | 统一错误码表，`request_id` | `.\mvnw.cmd -q test && pnpm --dir UnionDeskWeb/apps/UnionDeskAdminWeb test` | 后端：未联调；前端：未联调；数据库：不适用 | 统一错误码和排障串未完成 |
| TC-037 | QA/文档主责 | Swagger/OpenAPI、错误码表、部署手册 | `.\mvnw.cmd -q test && pnpm --dir UnionDeskWeb/apps/UnionDeskAdminWeb test && pnpm --dir UnionDeskWeb/apps/UnionDeskCustomerWeb test:run` | 待验证 | 交付文档未齐套 |
| TC-038 | 后端主责，前端协同 | `ticket_relation`，合并接口 | `.\mvnw.cmd -q test && pnpm --dir UnionDeskWeb/apps/UnionDeskAdminWeb test` | 后端：未联调；前端：未联调；数据库：未联调 | shared 层 mergeTicket alias 已补，工单合并页仍待接线 |
| TC-039 | 后端主责，前端协同 | `quick_reply_template`，关闭工单回复 | `.\mvnw.cmd -q test && pnpm --dir UnionDeskWeb/apps/UnionDeskAdminWeb test` | 后端：未联调；前端：未联调；数据库：未联调 | shared 层 reply/close aliases 已补，快速回复结束语前端仍待接线 |
| TC-040 | 后端主责，前端/DB 协同 | `ticket_template`，`dynamic_field_config` | `.\mvnw.cmd -q test && pnpm --dir UnionDeskWeb/apps/UnionDeskAdminWeb test && pnpm --dir UnionDeskWeb/apps/UnionDeskCustomerWeb test:run` | 后端：未联调；前端：未联调；数据库：未联调 | 工单模板分层未完成 |
| TC-041 | 后端主责，DB 协同 | `ticket_priority_level`，SLA 违约动作 | `.\mvnw.cmd -q test` | 后端：未联调；前端：未联调；数据库：未联调 | 域级优先级与违约动作未完成 |
| TC-042 | 后端主责，前端协同 | `ticket:view:self`，数据范围权限后缀 | `.\mvnw.cmd -q test && pnpm --dir UnionDeskWeb/apps/UnionDeskAdminWeb test` | 后端：未联调；前端：未联调；数据库：未联调 | 数据范围权限未完成 |
| TC-043 | 后端主责，前端协同 | 单用户角色调整接口 | `.\mvnw.cmd -q test && pnpm --dir UnionDeskWeb/apps/UnionDeskAdminWeb test` | 后端：未联调；前端：未联调；数据库：未联调 | 单用户逐一调整约束未完成 |
| TC-044 | 后端主责，DB 协同 | `system_config`，`domain_config`，密码策略 | `.\mvnw.cmd -q test` | 后端：未联调；前端：未联调；数据库：未联调 | 密码复杂度策略未完成 |
| TC-045 | 后端主责，前端/DB 协同 | `system_config`，`domain_config` | `.\mvnw.cmd -q test && pnpm --dir UnionDeskWeb/apps/UnionDeskAdminWeb test` | 后端：未联调；前端：未联调；数据库：未联调 | 系统配置与域配置未完成 |
| TC-046 | 后端主责，前端协同 | `quick_reply_template` | `.\mvnw.cmd -q test && pnpm --dir UnionDeskWeb/apps/UnionDeskAdminWeb test` | 后端：未联调；前端：未联调；数据库：未联调 | 快捷回复模板管理未完成 |
| TC-047 | 后端主责，DB 协同 | `ticket_history` | `.\mvnw.cmd -q test` | 后端：未联调；前端：未联调；数据库：未联调 | shared 层 fetchTicketHistory alias 已补，工单操作历史仍待端到端验证 |
| TC-048 | 后端主责，前端协同 | 工单时间线接口，`ticket_reply` | `.\mvnw.cmd -q test && pnpm --dir UnionDeskWeb/apps/UnionDeskCustomerWeb test:run && pnpm --dir UnionDeskWeb/apps/UnionDeskAdminWeb test` | 后端：未联调；前端：未联调；数据库：未联调 | shared 层 ticket detail / reply aliases 已补，回复时间线展示仍待前端切换 |
| TC-049 | 后端主责，前端协同 | `audit_log`，域筛选查询 | `.\mvnw.cmd -q test && pnpm --dir UnionDeskWeb/apps/UnionDeskAdminWeb test` | 后端：未联调；前端：未联调；数据库：未联调 | 审计日志域筛选未完成 |
| TC-050 | 后端/运维主责 | 环境隔离配置 | `docker compose up -d && .\mvnw.cmd -q test` | 后端：待验证；前端：待验证；数据库：待验证 | 多环境隔离未完成 |
| TC-051 | 后端主责 | Redis 使用点审查 | `.\mvnw.cmd -q test` | 后端：待验证；前端：不适用；数据库：待验证 | Redis 用途限定未完成 |
| TC-052 | 后端/运维主责 | 备份脚本、恢复手册、健康检查 | `docker compose up -d && .\mvnw.cmd -q test` | 后端：待验证；前端：不适用；数据库：待验证 | 可用性与恢复演练未完成 |

## Gate 结论

- P0 目前 **不允许进入联调/验收**。
- 原因不是单一用例失败，而是 `G0 工作区 Gate`、`G1 启动 Gate`、`G2 契约 Gate`、`G3 测试 Gate` 都没有可被当前工作区证明的完成状态。
- `TC-019` 已单独标记为 `P1参考`，不阻塞 P0。
