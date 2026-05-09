# Sprint 1-2: 身份认证 + 工单核心闭环

> 目标：实现客户身份注册/登录，完成工单基础 CRUD 和核心处理流程。

## 后端开发

### 身份认证（验收 #1-#4, #17, #19, #27）

- [ ] 客户注册 API `/api/v1/auth/register`
- [ ] 客户/员工登录 API `/api/v1/auth/login`
- [ ] Token 刷新/轮换机制 `/api/v1/auth/refresh`
- [ ] 找回密码流程（邮箱通道/管理员兜底）
- [ ] 验证码/滑块校验集成
- [ ] 邀请码管理 API（域管理员创建/列表/失效）
- [ ] 域客户管理 API（手动添加/列表/状态变更）

### 工单核心（验收 #5a, #33-#36）

- [ ] 工单类型配置表 + 基础类型初始化
- [ ] 工单 CRUD API
  - [ ] 客户提交工单 `POST /domains/{id}/tickets`
  - [ ] 工单列表（管理端）`GET /admin/domains/{id}/tickets`
  - [ ] 工单详情 `GET /admin/domains/{id}/tickets/{id}`
- [ ] 工单核心操作
  - [ ] 领取工单 `POST .../claim`
  - [ ] 指派工单 `PUT .../assign`
  - [ ] 回复工单 `POST .../replies`
  - [ ] 变更状态 `PUT .../status`
  - [ ] 客户撤回 `POST .../withdraw`
- [ ] 工单合并 API（验收 #33）
- [ ] 工单模板表 + 模板应用 API（验收 #35）
- [ ] 快捷回复模板 API（验收 #34, #72）
- [ ] 域级优先级管理 API（验收 #36）
- [ ] 乐观锁版本控制（防止并发冲突）

## 前端开发（UnionDeskCustomerWeb）

- [x] 项目初始化 + 路由框架
- [x] 登录/注册页面
- [x] 业务域选择页面
- [x] 提交工单页面（选类型/填标题/描述/附件）
- [x] 我的工单列表页
- [x] 工单详情页（状态/回复/撤回）

## 前端开发（UnionDeskAdminWeb）

- [ ] 工单队列页面（列表/筛选/领取）
- [ ] 工单详情页面（回复/转派/关闭/合并）
- [ ] 域配置：工单类型管理
- [ ] 域配置：快捷回复模板管理

## 数据模型

- [ ] Flyway 迁移：工单相关表
  - [ ] `ticket_type`, `ticket`, `ticket_reply`, `ticket_history`
  - [ ] `ticket_template`, `ticket_relation`, `ticket_priority_level`
  - [ ] `quick_reply_template`
- [ ] Flyway 迁移：邀请码/域客户表
  - [ ] `invitation_code`, `domain_customer`

## 验收标准

- [ ] TC-001~TC-004：业务域/身份认证用例通过
- [ ] TC-013~TC-017：工单核心流程用例通过
- [ ] TC-035~TC-037：附件/API/交付文档用例通过
- [ ] TC-038~TC-041：工单增强用例通过
- [ ] 客户可完成：注册 → 提单 → 查看 → 撤回
- [ ] 客服可完成：领取 → 回复 → 关闭
