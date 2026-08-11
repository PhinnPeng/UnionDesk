# 功能清单 — UnionDesk 客户端 / 管理端

| 文档版本 | 日期 | 说明 |
| :--- | :--- | :--- |
| 1.0 | 2026-08-11 | 双端全量功能清单（客户端 CustomerWeb + 管理端 AdminWeb），编号与 [`prd.md`](./prd.md) V2.2 §4.2 一致 |

> **状态基准**：2026-08-11（S3 编码中：US-S3-00/01/01a 已 Done，US-S3-02/03/04 Todo）。
> 状态随迭代变化，以 [`implementation-inventory.md`](./implementation-inventory.md) 与代码为准；本清单为快照。

---

## 1. 总览与图例

### 1.1 端侧划分（与 backlog-epics.md §1.2 一致）

| 端侧 | 应用 | 路由判定 | 模块 |
| :--- | :--- | :--- | :--- |
| **客户端** | `UnionDeskCustomerWeb` | `/`、`/d/:domainCode/*` 等 | M1 |
| **管理端-平台端** | `UnionDeskAdminWeb` | `/platform/*`，`iam_admin_menu.scope=platform` | M4 |
| **管理端-业务域端** | `UnionDeskAdminWeb` | 根级非 `/platform/`，`scope=business` | M2（员工端作业台）、M3（域管理后台） |

### 1.2 状态术语（与 implementation-inventory.md 对齐）

| 状态 | 英文 | 含义 |
| :--- | :--- | :--- |
| 已实现 | Done | 前后端均可正常使用（含基本校验与权限） |
| 部分 | Partial | 核心路径可用但缺边界 case / 依赖 demo 数据 / 部分入口为占位；「待联调确认」为联调状态未知项 |
| 规划中 | Todo | 未实现或未排期（备注注明 Epic，如 E4/E5/P1） |
| 占位 | — | 页面存在但为模板遗留 / 「功能开发中」空态，非业务功能（备注标「模板遗留/占位」） |

### 1.3 优先级

- **P0**：MVP 必做（与 prd.md 一致）
- **P1**：重要，后续 Sprint 排期
- **P2**：低优 / 模板遗留

### 1.4 统计摘要

| 模块 | 端侧 | 条目数 | 已实现 | 部分 | 规划中/占位 |
| :--- | :--- | :--- | :--- | :--- | :--- |
| M1 客户端 | 客户端 | 12 | 6 | 4 | 2 |
| M2 员工端 | 管理端-业务域端 | 4 | 1 | 1 | 2 |
| M3 域管理后台 | 管理端-业务域端 | 16 | 11 | 2 | 3 |
| M4 平台管理后台 | 管理端-平台端 | 22 | 13 | 6 | 3 |
| **合计** | — | **54** | **31** | **13** | **10** |

---

## 2. 最终功能清单（全量一表）

> 完整 10 列明细见 §3（客户端）、§4（管理端：4.1 平台端 / 4.2 业务域端）。

| 编号 | 模块 | 功能 | 端侧 | 优先级 | 实现状态 |
| :--- | :--- | :--- | :--- | :--- | :--- |
| F1.1 | 客户端 | 提交工单（动态表单） | 客户端 | P0 | 部分 |
| F1.2 | 客户端 | 在线咨询 | 客户端 | P0 | 规划中（E5） |
| F1.3 | 客户端 | 反馈/建议（工单类型路径） | 客户端 | P0 | 已实现 |
| F1.4 | 客户端 | 我的工单 / 咨询历史 | 客户端 | P0 | 部分 |
| F1.5 | 客户端 | 满意度评价 | 客户端 | P0 | 规划中（P1） |
| F1.6 | 客户端 | 登录（滑块验证、专属域入口） | 客户端 | P0 | 已实现 |
| F1.7 | 客户端 | 注册与入域（注册表单、邀请码） | 客户端 | P0 | 部分（本地 mock） |
| F1.8 | 客户端 | 业务域选择与切换 | 客户端 | P0 | 已实现 |
| F1.9 | 客户端 | 服务首页 | 客户端 | P0 | 已实现 |
| F1.10 | 客户端 | 站内信 / 通知中心 | 客户端 | P0 | 已实现 |
| F1.11 | 客户端 | 个人中心（账号信息、退出登录） | 客户端 | P0 | 部分 |
| F1.12 | 客户端 | 修改密码 | 客户端 | P0 | 已实现 |
| F2.1 | 员工端 | 工单队列与详情处理 | 管理端-业务域端 | P0 | 部分 |
| F2.2 | 员工端 | SLA 感知与高亮 | 管理端-业务域端 | P0 | 规划中（E4） |
| F2.3 | 员工端 | 在线咨询工作台 | 管理端-业务域端 | P0 | 规划中（E5） |
| F2.4 | 员工端 | 业务域端首页 / 工作台 | 管理端-业务域端 | P0 | 已实现 |
| F3.1 | 域管理后台 | 工单类型设计 | 管理端-业务域端 | P0 | 已实现 |
| F3.2 | 域管理后台 | SLA 规则与通知模板 | 管理端-业务域端 | P0 | 部分 |
| F3.3 | 域管理后台 | 成员 / 客户 / 角色管理 | 管理端-业务域端 | P0 | 已实现 |
| F3.4 | 域管理后台 | 事项属性与状态配置（域内） | 管理端-业务域端 | P0 | 已实现 |
| F3.5 | 域管理后台 | 域客户管理增强（编辑/重置密码/导入） | 管理端-业务域端 | P0 | 已实现 |
| F3.6 | 域管理后台 | 入域配置（域端） | 管理端-业务域端 | P0 | 已实现 |
| F3.7 | 域管理后台 | 域基础设置 | 管理端-业务域端 | P0 | 已实现 |
| F3.8 | 域管理后台 | 域参数配置（KV） | 管理端-业务域端 | P0 | 已实现 |
| F3.9 | 域管理后台 | 域屏蔽词库 | 管理端-业务域端 | P0 | 已实现 |
| F3.10 | 域管理后台 | 域运营概览 | 管理端-业务域端 | P1 | 部分 |
| F3.11 | 域管理后台 | 域通知配置 | 管理端-业务域端 | P1 | 规划中（占位） |
| F3.12 | 域管理后台 | 域级操作日志 / 登录日志 | 管理端-业务域端 | P0 | 已实现 |
| F3.13 | 域管理后台 | 系统角色管理（域端） | 管理端-业务域端 | P0 | 已实现 |
| F3.14 | 域管理后台 | 系统菜单管理（域端） | 管理端-业务域端 | P0 | 已实现 |
| F3.15 | 域管理后台 | 系统用户管理（域端） | 管理端-业务域端 | P1 | 占位（模板遗留） |
| F3.16 | 域管理后台 | 系统部门管理（域端） | 管理端-业务域端 | P1 | 占位（模板遗留） |
| F4.1 | 平台管理后台 | 业务域创建与管理 | 管理端-平台端 | P0 | 已实现 |
| F4.2 | 平台管理后台 | 模板中心 | 管理端-平台端 | P0 | 部分 |
| F4.3 | 平台管理后台 | 员工账号与离职池 | 管理端-平台端 | P0 | 部分 |
| F4.4 | 平台管理后台 | 系统设置与安全告警 | 管理端-平台端 | P0 | 部分 |
| F4.5 | 平台管理后台 | 平台端登录与动态菜单 | 管理端-平台端 | P0 | 已实现 |
| F4.6 | 平台管理后台 | 平台首页仪表盘 | 管理端-平台端 | P0 | 部分 |
| F4.7 | 平台管理后台 | 用户管理 | 管理端-平台端 | P0 | 已实现 |
| F4.8 | 平台管理后台 | 组织 / 部门管理 | 管理端-平台端 | P0 | 已实现 |
| F4.9 | 平台管理后台 | 角色管理 | 管理端-平台端 | P0 | 已实现 |
| F4.10 | 平台管理后台 | 菜单管理 | 管理端-平台端 | P0 | 已实现 |
| F4.11 | 平台管理后台 | 权限管理入口 | 管理端-平台端 | P1 | 部分 |
| F4.12 | 平台管理后台 | 审计日志 / 登录日志（平台统一页） | 管理端-平台端 | P0 | 已实现 |
| F4.13 | 平台管理后台 | 全局屏蔽词 | 管理端-平台端 | P0 | 已实现 |
| F4.14 | 平台管理后台 | 事项配置（类型/属性/状态/模板） | 管理端-平台端 | P0 | 已实现 |
| F4.15 | 平台管理后台 | SLA 规则与工作日历 | 管理端-平台端 | P0 | 已实现（未挂验收 Story） |
| F4.16 | 平台管理后台 | 站内信（管理端） | 管理端-平台端 | P0 | 已实现 |
| F4.17 | 平台管理后台 | 附件上传（MinIO） | 管理端-平台端 | P0 | 已实现 |
| F4.18 | 平台管理后台 | 用户导入导出 | 管理端-平台端 | P1 | 规划中（占位） |
| F4.19 | 平台管理后台 | 域配置 KV（平台侧） | 管理端-平台端 | P0 | 已实现 |
| F4.20 | 平台管理后台 | 客户入域邀请码面板（平台侧） | 管理端-平台端 | P0 | 部分 |
| F4.21 | 平台管理后台 | 组织配置（平台侧） | 管理端-平台端 | P1 | 规划中（占位） |
| F4.22 | 平台管理后台 | 模板遗留页面 | 管理端-平台端 | P2 | 占位（模板遗留） |

---

## 3. 客户端功能清单表（CustomerWeb，10 列）

| 功能编号 | 端侧 | 模块 | 功能 | 功能说明 | 优先级 | 实现状态 | 对应页面/路由 | 关联 Epic/Story | 备注 |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| F1.1 | 客户端 | 客户端 | 提交工单（动态表单） | 三步提单：选择启用类型 → 填标题/描述 → 提交成功展示工单号；调 `POST /api/v1/domains/{domain_id}/tickets` 真实 API | P0 | 部分 | `/tickets/new` | E3（US-S3-03） | 动态表单字段未渲染（仅系统字段 title/description）；attachmentIds 恒为空数组 |
| F1.2 | 客户端 | 客户端 | 在线咨询 | 排队 → 接入客服 → 实时聊天（文字/图片/附件）；会话可转工单 | P0 | 规划中（E5 未排期） | `/chat`（占位页「即将开放」） | E5（未拆 Story） | 纯静态占位页，唯一动作「去提交工单」 |
| F1.3 | 客户端 | 客户端 | 反馈/建议（工单类型路径） | 「反馈」「建议」作为预置工单类型在提单时可选，由管理员启用/停用 | P0 | 已实现 | `/tickets/new`（类型选择） | E3（US-S3-01 AC4） | MVP 以工单类型路径交付，无独立入口 |
| F1.4 | 客户端 | 客户端 | 我的工单 / 咨询历史 | 我的工单列表（类型侧栏计数、生命周期筛选 pending/active/done、关键词搜索）+ 详情（公开动态时间线、补充说明、撤回 open 态 version 乐观锁） | P0 | 部分 | `/tickets`、`/tickets/:ticketId` | E3（US-S3-03） | 咨询历史无；详情关联咨询为占位卡；附件展示无 |
| F1.5 | 客户端 | 客户端 | 满意度评价 | 工单关闭/咨询结束后，通知中心评价入口，星级 + 文字评价 | P0 | 规划中（P1） | 无（全站无入口） | P1（未拆 Story） | 全站无评价功能（grep 0 命中） |
| F1.6 | 客户端 | 客户端 | 登录 | 账号/密码登录（真实 JWT API）；滑块验证（后端 challenge，5s 过期缓冲）；记住账号；专属域入口 `/d/:domainCode/login`；新环境登录站内提醒 | P0 | 已实现 | `/login`、`/d/:domainCode/login` | E1/E3（US-S1-01、S2-UX-01） | 忘记密码为占位提示（「功能开发中」toast） |
| F1.7 | 客户端 | 客户端 | 注册与入域 | 注册表单（显示名/登录名/手机号/密码/邮箱可选）+ 开放域下拉 + 邀请码；专属域注册预填邀请码 | P0 | 部分（本地 mock） | `/register`、`/d/:domainCode/register` | E3（US-S3-02，并入 S1-04/05） | 本地状态机 mock（`cust-at` 伪 token），未调 `/api/v1/auth/register`；域下拉仅展示 `registration_enabled=allowed` 待接入 |
| F1.8 | 客户端 | 客户端 | 业务域选择与切换 | 「已加入/可加入/需管理员开通」三组卡片；切换当前域（真实 switch-domain API） | P0 | 已实现 | `/domains` | E3（US-S3-02） | 邀请码加入为本地 mock（归 F1.7） |
| F1.9 | 客户端 | 客户端 | 服务首页 | 问候语、待处理提醒、生命周期统计卡（待处理/进行中/已完成）、最近 5 条工单、未读通知前 3 条（jumpUrl 跳转） | P0 | 已实现 | `/home` | E3（US-S3-03） | 首页本身无独立 Story |
| F1.10 | 客户端 | 客户端 | 站内信 / 通知中心 | 站内信列表（kind 分类 system/ticket/domain）、未读数、标为已读、按 jumpUrl 跳转查看 | P0 | 已实现 | `/inbox` | P0 契约（未拆 Story） | 底部导航「通知」Tab 带未读 badge |
| F1.11 | 客户端 | 客户端 | 个人中心 | 账号信息（显示名/登录名/手机号）、业务域/通知入口、退出登录（真实 API） | P0 | 部分 | `/me` | 无 Story（S3+ 新增） | 通知偏好为占位（toast「UI 占位」） |
| F1.12 | 客户端 | 客户端 | 修改密码 | 当前/新密码/确认；前端校验（新密码 ≥6 位、两次一致、与当前不同）；`mustChangePassword` 强制改密；成功后回登录页 | P0 | 已实现 | `/change-password` | 无 Story（S3+ 新增） | RequireSession 守卫，强制改密时不可跳过 |

---

## 4. 管理端功能清单表（AdminWeb，10 列）

### 4.1 平台端（/platform/*，scope=platform）— M4 平台管理后台

| 功能编号 | 端侧 | 模块 | 功能 | 功能说明 | 优先级 | 实现状态 | 对应页面/路由 | 关联 Epic/Story | 备注 |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| F4.1 | 管理端-平台端 | 平台管理后台 | 业务域创建与管理 | 卡片列表 + 分页/关键词搜索/创建日期筛选；新建向导 Modal（Step3 入域双开关）；详情 10 Tab（概览/基础/配置/客户入域/工单/角色/屏蔽词/通知/日志/成员）；软删除（code 确认 + Step-up 二次认证） | P0 | 已实现 | `/platform/domains`、`/platform/domains/detail/:domainId` | E1/E6（US-S1-02、S1-03、S2-01） | 已删域不在列表展示（`deleted_at IS NOT NULL`）；已删域直链行为（US-S2-01 AC4）延后；目标态=集团统一管理（任务 08-11-group-role-management）：域详情成员/客户/角色 Tab 与域端同源双入口，平台侧角色走模板下发 |
| F4.2 | 管理端-平台端 | 平台管理后台 | 模板中心 | 将域完整配置提炼为模板，供快速创建新域 | P0 | 部分 | `/platform/ticket-config/templates` | E1（US-S1-02） | 现状=团队模板已实现但菜单已隐藏（V20260726092200）；「域模板提炼」语义与实现待对齐 |
| F4.3 | 管理端-平台端 | 平台管理后台 | 员工账号与离职池 | 全局员工列表/创建/离职；离职池列表 + 一键恢复；重置密码 | P0 | 部分 | `/platform/user`、`/platform/offboard-pool` | E1（US-S1-07） | CSV 批量导入未实现（见 F4.18）；永久删除无前端入口；离职池含域名/角色/离职时间/操作人 |
| F4.4 | 管理端-平台端 | 平台管理后台 | 系统设置与安全告警 | 系统设置 KV（fetch/updateSystemConfig） | P0 | 部分 | `/platform/system-settings` | E1（US-S1-07 关联） | 安全告警中心未实现（inventory §5.3 Todo）；密码强度/登录锁定/IP 白名单未成品 |
| F4.5 | 管理端-平台端 | 平台管理后台 | 平台端登录与动态菜单 | 登录（滑块验证 US-S2-UX-01）；按权限快照三元规则跳转双轨首页；后端菜单动态路由；无权限菜单不可见 | P0 | 已实现 | `/login` | E1/E6（US-S1-01、S2-UX-01、S3-00） | 平台/业务域共用同一登录页；忘记密码为占位 stub；`admin` 默认绑定 `platform_admin`（US-S3-00） |
| F4.6 | 管理端-平台端 | 平台管理后台 | 平台首页仪表盘 | Statistic 卡片 + 快捷入口 + 最近审计列表 | P0 | 部分 | `/platform/home` | E1（S1 待办） | 数据来自 `DemoDataService` mock，非真实聚合（S1 待办接 count(*)） |
| F4.7 | 管理端-平台端 | 平台管理后台 | 用户管理 | 列表（搜索 + 部门树侧栏 + 状态 Tag 在职/停用/离职）；创建/编辑（账号/姓名/手机/邮箱/组织/角色）；离职（二次确认）；重置密码（随机 16 位 Modal 复制） | P0 | 已实现 | `/platform/user` | E1（US-S1-07） | 永久删除 `DELETE /api/v1/iam/users/{userId}` 后端就绪、前端无入口；目标态（任务 08-11-group-role-management）：跨域批量停用（多域成员，step-up 二次认证，TR-04 部分成功语义） |
| F4.8 | 管理端-平台端 | 平台管理后台 | 组织 / 部门管理 | 树形 CRUD + 搜索 + 展开/折叠 + 新建/编辑弹窗（父部门/负责人）+ 删除子部门校验 + 循环引用检测 | P0 | 已实现 | `/platform/dept` | E1（inventory §2） | 无独立 Story；组织配置页（F4.21）为占位 |
| F4.9 | 管理端-平台端 | 平台管理后台 | 角色管理 | 角色列表（scope 筛选 platform/domain）；创建/编辑（code/name/scope/description + 菜单按钮权限树）；删除（禁止预置角色） | P0 | 已实现 | `/platform/role` | E1/E6（US-S1-07、S3-00） | scope 不一致拒绝 + 中文提示（US-S3-00 AC3）；目标态（任务 08-11-group-role-management）：平台建角色模板（role_template）一次下发多域（本页新增「模板」Tab）；domain scope 角色双轨冻结（新增角色走模板/域端，旧角色只读保留） |
| F4.10 | 管理端-平台端 | 平台管理后台 | 菜单管理 | 菜单树 Table（scope 筛选 + 图标选择器 + 菜单/按钮节点标签）；创建/编辑/删除（级联子菜单） | P0 | 已实现 | `/platform/system/menu` | E1（US-S1-07） | 后端菜单 → 动态路由 → `AuthGuarded` 按钮级权限 |
| F4.11 | 管理端-平台端 | 平台管理后台 | 权限管理入口 | 权限管理页重定向至角色管理页 | P1 | 部分 | `/platform/permission` | E1（US-S1-07 关联） | 仅 `<Navigate to="/platform/role">`，无独立权限界面 |
| F4.12 | 管理端-平台端 | 平台管理后台 | 审计日志 / 登录日志（平台统一页） | 审计/登录 Tabs 统一页 + 独立页双入口；分页 + 模块/操作者/关键词/时间范围筛选；登录日志含主体/门户/结果/客户端/IP | P0 | 已实现 | `/platform/audit-logs`、`/platform/log/operation-log`、`/platform/log/login-log` | E1/E6（US-S1-09） | 独立页与统一页功能重叠，建议收敛；审计不可删除；导出 Todo |
| F4.13 | 管理端-平台端 | 平台管理后台 | 全局屏蔽词 | 平台全局词库 CRUD，跨域生效（`business_domain_id` 为空） | P0 | 已实现 | `/platform/blockwords` | E2（US-S2-05） | 权限 `platform.blocked_word.*`；词条去首尾空格、禁空词 |
| F4.14 | 管理端-平台端 | 平台管理后台 | 事项配置（类型/属性/状态/模板） | 事项类型/属性/状态/团队模板配置；含 Formily 表单设计器（草稿/发布/版本历史）+ React Flow 状态流 DAG（TR-01 至少一个终态）+ 属性插槽（拖拽排序/默认值 JSON `{mode,value}`） | P0 | 已实现 | `/platform/ticket-config/*` | E2/E3（US-S3-01、S3-01a） | templates 菜单已隐藏；inventory v1.3 未覆盖（S3 新增） |
| F4.15 | 管理端-平台端 | 平台管理后台 | SLA 规则与工作日历 | SLA 规则（首响/解决时限、违约动作）+ 工作日历 CRUD；计时引擎（SlaTimingEngine） | P0 | 已实现（未挂验收 Story） | `/platform/sla-management` | E4（US-S3-E4-01/02 Stretch） | backlog E4 标「占位」与代码不符；前端调 `#src/api/platform/sla`，后端 `uniondesk-support` sla 包 |
| F4.16 | 管理端-平台端 | 平台管理后台 | 站内信（管理端） | 管理端站内信列表/未读/已读（P0 inbox 契约） | P0 | 已实现 | `/platform/inbox` | P0 契约（未拆 Story） | — |
| F4.17 | 管理端-平台端 | 平台管理后台 | 附件上传（MinIO） | 附件上传（服务端代理 → MinIO，P0 契约） | P0 | 已实现 | `/platform/attachments` | P0 契约（未拆 Story） | 依赖 MinIO 服务（外部依赖 ADR） |
| F4.18 | 管理端-平台端 | 平台管理后台 | 用户导入导出 | 用户批量导入导出 | P1 | 规划中（占位） | `/platform/import-export` | E1（inventory §4.1） | 页面存在，API 待查 |
| F4.19 | 管理端-平台端 | 平台管理后台 | 域配置 KV（平台侧） | 业务域 KV 键值对配置（key/value/valueType/description） | P0 | 已实现 | `/platform/domain-config` | E1（inventory §3） | 域详情「配置」Tab 同源 |
| F4.20 | 管理端-平台端 | 平台管理后台 | 客户入域邀请码面板（平台侧） | 平台侧邀请码 CRUD（列表/创建/删除/失效） | P0 | 部分 | `/platform/domain-onboarding` | E1（US-S1-03、S1-06） | CustomerWeb 接真实入域 API 见 US-S3-02（未完成） |
| F4.21 | 管理端-平台端 | 平台管理后台 | 组织配置（平台侧） | 组织架构配置 | P1 | 规划中（占位） | `/platform/org-config` | E1（inventory §2） | Empty 占位「组织配置功能开发中」 |
| F4.22 | 管理端-平台端 | 平台管理后台 | 模板遗留页面 | access/route-nest/about/outside/personal-center 等模板演示页与异常页；`dev/ticket-type-designer-poc` | P2 | 占位（模板遗留） | `/access/*`、`/route-nest/*`、`/about`、`/outside/*` 等 | 无（react-antd-admin 遗留） | 非业务功能；dev POC 路由未注册；另含隐私政策/服务条款等公开页 |

### 4.2 业务域端（根级非 /platform/，scope=business）— M2 员工端 + M3 域管理后台

| 功能编号 | 端侧 | 模块 | 功能 | 功能说明 | 优先级 | 实现状态 | 对应页面/路由 | 关联 Epic/Story | 备注 |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| F2.1 | 管理端-业务域端 | 员工端 | 工单队列与详情处理 | 按域/状态/优先级/SLA 筛选的工单队列；详情时间线 + 属性卡；公开回复/内部备注/转派/变更状态/批量领取/批量关闭 | P0 | 部分 | `/platform/ticket-pool`、`/platform/ticket-detail`（P0 演示） | E3（US-S3-04） | 当前仅平台端 P0 演示页；business 端工单处理页未成品（US-S3-04 Todo） |
| F2.2 | 管理端-业务域端 | 员工端 | SLA 感知与高亮 | 列表/详情高亮 SLA 即将超时或已超时的工单 | P0 | 规划中（E4） | — | E4（US-S3-E4-01/02） | SLA 规则引擎已有（F4.15），未接入工单 UI |
| F2.3 | 管理端-业务域端 | 员工端 | 在线咨询工作台 | 手动接入排队客户；实时聊天 + 撤回 2 分钟内消息；一键转工单（带客户信息 + 会话摘要） | P0 | 规划中（E5） | — | E5（未拆 Story） | 未排期 |
| F2.4 | 管理端-业务域端 | 员工端 | 业务域端首页 / 工作台 | 当前域 + 按权限过滤的快捷入口 + 概览说明；business 菜单入口 | P0 | 已实现 | `/home` | E2（US-S2-E2-00） | V20260728170000 工作台菜单（`BUSINESS-HOME-MENU`）；权限 `domain.home.read` |
| F3.1 | 管理端-业务域端 | 域管理后台 | 工单类型设计 | 类型列表/新建/编辑/删除；Drawer 三 Tab（基础信息、Formily 表单设计 title/description 锁定、React Flow 状态流 DAG）；预置「反馈」「建议」启用/停用；`general` 补默认 form_schema | P0 | 已实现 | `/domain/ticket-config`（域内）、`/platform/domains/ticket-type-config/*`、`/platform/domains/ticket/form-design/*`（域详情内） | E2/E3（US-S3-01，承接 S2-E2-01） | 保存状态流满足 TR-01（至少一个终态）；权限 `platform.domain.control.ticket_type.*` / `domain.ticket_type.*` |
| F3.2 | 管理端-业务域端 | 域管理后台 | SLA 规则与通知模板 | 本域默认 SLA + 按类型特殊 SLA（首响/解决时限、违约动作）；事件通知模板（站内信/邮件） | P0 | 部分 | `/platform/sla-management`（SLA）、`/domain/settings/notifications`（通知占位） | E2/E4（US-S3-E4-01/02） | SLA 规则已实现（F4.15）；通知模板为占位（菜单与权限已就绪） |
| F3.3 | 管理端-业务域端 | 域管理后台 | 成员 / 客户 / 角色管理 | 域成员（平台员工添加/改角色/启停/移除）；域客户（列表/手动添加/启停）；域角色（预置 + 自定义） | P0 | 已实现 | `/domain/settings/members`、`/domain/customers/list`、`/domain/settings/roles` | E2（US-S2-03、S2-04、S2-02 域端版） | 最后 `domain_admin` / `super_admin` 保护规则；同一员工同一域不可重复添加；与平台域详情 Tab 同源双入口（API 复用、权限码 OR）；目标态=集团统一管理（任务 08-11-group-role-management）：角色实例受模板锁定字段约束 |
| F3.4 | 管理端-业务域端 | 域管理后台 | 事项属性与状态配置（域内） | 域内事项配置三面板（类型/属性/状态，复用平台面板）；属性插槽拖拽排序、显示名/占位符/默认值、系统属性锁定不可删 | P0 | 已实现 | `/domain/ticket-config` | E2/E3（US-S3-01a） | 权限 `domain.ticket_type/attribute/status.*`；inventory 缺项 |
| F3.5 | 管理端-业务域端 | 域管理后台 | 域客户管理增强 | 查询/手动添加/从员工导入/启停/编辑/重置密码/只读详情 | P0 | 已实现 | `/domain/customers/list` | E2（US-S2-04） | V20260806120000/130000 增重置密码与编辑按钮菜单；无独立 Story（S3 后新增）；与平台域详情「客户」Tab 同源双入口（US-S2-04；资料编辑/重置密码仅域端可写）；目标态=集团统一管理（任务 08-11-group-role-management）：平台跨域批量停用与域端日常运营协同 |
| F3.6 | 管理端-业务域端 | 域管理后台 | 入域配置（域端） | 邀请码 + 注册策略双开关维护 | P0 | 已实现 | `/domain/onboarding`、`/domain/settings/onboarding` | E2（US-S1-03） | — |
| F3.7 | 管理端-业务域端 | 域管理后台 | 域基础设置 | 域名称/LOGO/描述 | P0 | 已实现 | `/domain/settings/basic` | E2（US-S2-01 域端版） | — |
| F3.8 | 管理端-业务域端 | 域管理后台 | 域参数配置（KV） | KV 键值对（key/value/valueType/description） | P0 | 已实现 | `/domain/settings/config` | E1（inventory §3 域端版） | — |
| F3.9 | 管理端-业务域端 | 域管理后台 | 域屏蔽词库 | 域内词条增删查、重复提示、空态；去首尾空格、禁空词 | P0 | 已实现 | `/domain/settings/blockwords` | E2（US-S2-05） | 域端权限 `domain.*`；平台侧域详情 Tab 同源 |
| F3.10 | 管理端-业务域端 | 域管理后台 | 域运营概览 | 4 项 Statistic + 趋势图 | P1 | 部分 | `/domain/overview` | 无 Story（S3+ 新增） | 统计值「—」、趋势「数据接入中」 |
| F3.11 | 管理端-业务域端 | 域管理后台 | 域通知配置 | 通知配置（站内信/邮件模板等） | P1 | 规划中（占位） | `/domain/settings/notifications` | E2/E4 | Empty「功能开发中，菜单与权限已就绪」 |
| F3.12 | 管理端-业务域端 | 域管理后台 | 域级操作日志 / 登录日志 | 域级 audit/login 列表，分页 + 时间/结果/关键词筛选 | P0 | 已实现 | `/domain/settings/audit-logs`、`/domain/settings/login-logs` | E2（US-S2-06） | 审计不可删除；平台侧域详情侧栏同源 |
| F3.13 | 管理端-业务域端 | 域管理后台 | 系统角色管理（域端） | 角色列表/创建/编辑（权限树）/删除，scope=domain | P0 | 已实现 | `/system/role` | E2（US-S2-02） | 域端版角色管理（可写）；平台侧域详情内为只读；目标态（任务 08-11-group-role-management）：模板下发角色实例在本页展示「模板来源 + 锁定字段」，锁定字段只读、非锁定字段可微调 |
| F3.14 | 管理端-业务域端 | 域管理后台 | 系统菜单管理（域端） | 菜单树 CRUD，scope=business 筛选 | P0 | 已实现 | `/system/menu` | E2（US-S2-E2-00 关联） | — |
| F3.15 | 管理端-业务域端 | 域管理后台 | 系统用户管理（域端） | 域端系统用户管理 | P1 | 占位（模板遗留） | `/system/user` | E2（inventory §7） | 模板残留（单 Input）；非业务功能 |
| F3.16 | 管理端-业务域端 | 域管理后台 | 系统部门管理（域端） | 域端系统部门管理 | P1 | 占位（模板遗留） | `/system/dept` | E2（inventory §7） | 模板残留（计数器 demo）；非业务功能 |

---

## 5. 功能清单说明（按模块叙述）

### 5.1 M1 客户端（CustomerWeb）

- **目的**：客户寻求帮助的统一入口（提单 / 查单 / 通知 / 账号）。
- **主要能力**：登录 / 注册 / 域选择（F1.6–F1.8）→ 提单（类型 + 表单，F1.1/F1.3）→ 我的工单（时间线/补充/撤回，F1.4）→ 通知中心（F1.10）→ 个人中心 / 改密（F1.11/F1.12）；在线咨询（F1.2）与满意度评价（F1.5）规划中。
- **关键规则**：FR-05（未入域拒绝）、TR-03（撤回约束，open 态 version 乐观锁）；注册 / 入域 mock 见 US-S3-02；动态表单字段渲染依赖 F3.1 的 `form_schema`（当前未渲染）。
- **现状要点**：登录与工单链路接真实 API（`customer-portal-live.ts` + `api.ts`）；注册与邀请码入域为本地 mock；chat 为占位页。

### 5.2 M2 员工端（客服作业台，管理端-业务域端）

- **目的**：客服处理本域工单与在线咨询。
- **主要能力**：工单队列 / 详情处理（F2.1）；SLA 感知（F2.2）；咨询工作台（F2.3）；域端首页工作台（F2.4）。
- **关键规则**：FR-01（未授权 403 + 中文）、FR-03（按钮不可见）、TR-02（终态锁定）、TR-04；权限 `ticket.*`。
- **现状要点**：工单队列 / 处理（US-S3-04）未成品，现为平台端 P0 演示页（`/platform/ticket-pool`）；在线咨询 E5 未排期；工作台（F2.4）已上线。

### 5.3 M3 域管理后台（管理端-业务域端）

- **目的**：域管理员配置本域规则与人员（PRD §5.3）。
- **主要能力**：工单类型 / 属性 / 状态配置（F3.1/F3.4）、SLA 与通知（F3.2/F3.11）、入域 / 基础 / 参数 / 屏蔽词（F3.6–F3.9）、客户 / 成员 / 角色（F3.3/F3.5/F3.13–F3.16）、日志与概览（F3.10/F3.12）。
- **关键规则**：TR-01（状态流至少一个终态）、TR-02（配置层约束）；US-S2-02/03/04/05/06 权限码体系（`domain.*` / `platform.domain.control.*`）。
- **现状要点**：域内配置基本 Done（S3 期新增 domain/ticket-config、customers 增强）；域通知配置为占位；system/user、system/dept 为模板遗留占位；集团目标态（任务 08-11-group-role-management）：域角色实例受模板锁定字段约束（默认锁权限包），模板来源/锁定字段在角色页展示（F3.13）。

### 5.4 M4 平台管理后台（管理端-平台端）

- **目的**：平台管理员跨域治理（PRD §5.4）。
- **主要能力**：业务域管理（F4.1/F4.2/F4.19/F4.20）、IAM（用户/部门/角色/菜单/权限/离职池/导入导出，F4.3/F4.7–F4.11/F4.18/F4.21）、安全与审计（F4.4/F4.12/F4.13）、工单与消息（F4.14–F4.17）、平台入口（F4.5/F4.6/F4.22）。
- **关键规则**：FR-01/FR-03（权限）、US-S3-00（角色—控制台绑定，`role.scope` 一致校验）、step-up 二次认证（域删除）、审计不可删除；集团目标态（任务 08-11-group-role-management）：平台统一角色模板一次下发多域 + 跨域批量停用（step-up），依赖 US-S1-08（目标域校验，P0 前置安全债）。
- **现状要点**：平台端 Done 主导；Partial 为首页仪表盘（demo 数据）、模板中心（菜单隐藏）、导入导出（占位）、安全告警中心（未实现）等；SLA 规则与工作日历已实现但 backlog 未挂验收 Story。

---

## 6. 追踪索引

### 6.1 编号体系

- **唯一来源**：[`prd.md`](./prd.md) §4.2（模块 M1–M4 + 功能 F1.x–F4.x）；本文档每行复用同一编号，双向可追踪。
- **端侧归属**：M1 = 客户端；M2/M3 = 管理端-业务域端；M4 = 管理端-平台端。

### 6.2 反查映射

| 功能编号段 | 详细设计章节（prd.md） | 关联 Epic | 代表 Story | 主要页面/路由 |
| :--- | :--- | :--- | :--- | :--- |
| F1.1–F1.5 | §5.1.1–5.1.2 | E3（E5 咨询、P1 评价） | US-S3-01/03 | `/tickets/*`、`/chat` |
| F1.6–F1.12 | §5.1.3–5.1.5 | E3（E1 登录共用） | US-S1-01、S2-UX-01、S3-02 | `/login`、`/register`、`/domains`、`/home`、`/inbox`、`/me`、`/change-password` |
| F2.1–F2.4 | §5.2.1–5.2.3 | E3（E4 SLA、E5 咨询） | US-S3-04、S2-E2-00 | `/home`、`/platform/ticket-pool` |
| F3.1–F3.16 | §5.3.1–5.3.3 | E2/E3（E4 SLA） | US-S3-01/01a、S2-02/03/04/05/06 | `/domain/*`、`/system/*` |
| F4.1–F4.22 | §5.4.1–5.4.5 | E1/E6（E4 SLA） | US-S1-01/02/03/07/09、S2-01/05、S3-00 | `/platform/*` |

### 6.3 关联文档

| 文档 | 用途 |
| :--- | :--- |
| [`prd.md`](./prd.md) | 功能定义与详细设计（L2） |
| [`implementation-inventory.md`](./implementation-inventory.md) | 管理端实现盘点（平台端为主，v1.3） |
| [`backlog-epics.md`](./backlog-epics.md) | Epic 地图（E0–E6） |
| [`backlog-stories.md`](./backlog-stories.md) | User Story 与 AC（L6 迭代任务源） |
| [`foundation-rules.md`](./foundation-rules.md) | 身份/域/工单状态机规则（FR/DR/TR，L3） |

---

## 7. 操作矩阵（按功能编号）

> 粒度说明：本矩阵以**页面功能级**为行（与 §2 全量 54 条一一对应），操作列枚举该功能的关键操作（列表 / 详情 / 新增 / 编辑 / 删除）；「其他操作」承载非 CRUD 能力（导入/导出/启停/重置密码/撤回/转派/批量等）。单元格取值：`✔` = 已实现；`—` = 不适用；`部分` + 简注 = 实现不全；`规划中（E4/E5）` = 未排期。按钮级校验与边界引用规则编号（FR/TR/DR）与 Story AC（如 TR-01 终态校验、US-S2-01 code 确认 + Step-up），无引用写 `—`；证据不足写 `待确认`（以代码为准）。

| 功能编号 | 列表 | 详情 | 新增 | 编辑 | 删除 | 其他操作 | 校验/边界 |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| F1.1 | —（提单页无列表语义） | —（提交成功展示工单号） | ✔（提单提交：`POST /api/v1/domains/{domain_id}/tickets`） | —（提单后不可编辑） | —（无取消/删除入口） | —（attachmentIds 恒为空数组） | FR-05（未入域拒绝，US-S3-03 AC4）；动态表单字段未渲染（仅 title/description） |
| F1.2 | — | — | — | — | — | 规划中（E5：排队/接入/实时聊天/转工单） | —（E5 未拆 Story） |
| F1.3 | —（无独立列表） | — | ✔（提单可选「反馈」「建议」预置类型提交） | — | — | — | 预置类型由管理员启用/停用（US-S3-01 AC4） |
| F1.4 | ✔（我的工单：类型侧栏计数/生命周期筛选/关键词搜索） | ✔（公开动态时间线、属性） | —（提交归 F1.1） | — | —（无取消/删除工单入口） | 部分（补充说明 ✔、撤回 ✔ open 态；咨询历史无、附件展示无） | TR-03（撤回：open 态 + version 乐观锁） |
| F1.5 | — | — | — | — | — | 规划中（P1：星级 + 文字评价提交，全站无入口） | —（P1 未拆 Story） |
| F1.6 | — | — | — | — | — | ✔（滑块验证、记住账号、专属域入口、新环境登录站内提醒） | 滑块验证后端 challenge 5s 过期缓冲（US-S2-UX-01）；忘记密码为占位提示 |
| F1.7 | — | — | 部分（注册提交：本地 mock，未调 `/api/v1/auth/register`） | — | — | 部分（开放域下拉仅展示 `registration_enabled=allowed` 待接入；专属域注册预填邀请码） | DR-01/DR-02（注册/邀请策略校验，US-S3-02 AC1） |
| F1.8 | ✔（已加入/可加入/需管理员开通三组卡片） | — | — | — | — | ✔（切换当前域 switch-domain 真实 API；邀请码加入为本地 mock 归 F1.7） | FR-05（未入域客户不可访问域能力） |
| F1.9 | ✔（最近 5 条工单、未读通知前 3 条） | — | — | — | — | ✔（jumpUrl 跳转） | — |
| F1.10 | ✔（站内信列表，kind 分类 system/ticket/domain） | ✔（按 jumpUrl 跳转查看） | — | — | — | ✔（未读数 badge、标为已读） | — |
| F1.11 | — | ✔（账号信息：显示名/登录名/手机号） | — | — | — | 部分（退出登录 ✔ 真实 API；通知偏好为占位 toast） | — |
| F1.12 | — | — | — | — | — | ✔（修改密码：当前/新密码/确认） | 前端校验（新密码 ≥6 位、两次一致、与当前不同）；`mustChangePassword` 强制改密不可跳过 |
| F2.1 | 部分（仅平台端 P0 演示页 `/platform/ticket-pool`；business 端队列未成品 US-S3-04） | 部分（P0 演示：时间线 + 属性卡） | —（工单由客户创建） | — | — | 部分（公开回复/内部备注/转派/变更状态/批量领取/批量关闭，P0 演示） | TR-02（终态后不可再变更）；FR-01/FR-03（`ticket.*` 权限，US-S3-04 AC4） |
| F2.2 | 规划中（E4） | 规划中（E4） | — | — | — | —（依赖 F4.15 计时引擎） | —（US-S3-E4-01/02 Stretch 未排期） |
| F2.3 | — | — | — | — | — | 规划中（E5：手动接入、实时聊天、撤回 2 分钟内消息、一键转工单） | —（E5 未拆 Story） |
| F2.4 | — | — | — | — | — | ✔（快捷入口按权限过滤；business 菜单入口） | FR-03（无权限入口不可见）；US-S2-E2-00 |
| F3.1 | ✔（类型列表） | —（Drawer 三 Tab 承载查看/编辑） | ✔（类型新建 + Drawer） | ✔（基础信息/Formily 表单设计/状态流 DAG；title/description 锁定） | ✔（类型删除） | ✔（预置「反馈」「建议」启用/停用） | TR-01（状态流至少一个终态，违反保存失败 + 中文）；US-S3-01 AC2（系统字段锁定） |
| F3.2 | 部分（SLA 规则列表 ✔；通知模板占位） | — | 部分（SLA 规则新建复用 F4.15；通知模板无） | 部分（SLA 规则编辑复用 F4.15；通知模板无） | 部分（SLA 规则删除复用 F4.15；通知模板无） | — | —（E4 US-S3-E4-01/02 Stretch） |
| F3.3 | ✔（成员/客户/角色三列表） | —（客户只读详情归 F3.5） | ✔（添加平台员工成员、手动添加客户、创建自定义角色） | ✔（改成员角色、客户启停、角色权限组合） | 部分（成员移除 ✔ 软删；角色删除受预置保护；客户无删除仅启停） | ✔（成员/客户启停） | 最后 `domain_admin`/`super_admin` 保护规则；同一员工同一域不可重复添加（US-S2-03 AC2/AC4） |
| F3.4 | ✔（类型/属性/状态三面板） | — | ✔（属性/状态新增） | ✔（显示名/占位符/默认值、拖拽排序） | 部分（非 `is_system` 可删；系统属性锁定） | ✔（属性插槽拖拽排序、默认值 JSON `{mode,value}` literal） | TR-02（配置层约束）；US-S3-01a AC1/AC5（系统属性锁定、默认值格式） |
| F3.5 | ✔（客户查询） | ✔（只读详情 Modal） | ✔（手动添加、从员工导入） | ✔（客户编辑） | —（无删除，启停代替） | ✔（启停、重置密码） | —（S3+ 新增，无独立 Story；V20260806120000/130000） |
| F3.6 | ✔（邀请码列表） | — | ✔（邀请码生成） | —（无编辑，失效代替） | ✔（删除/失效） | ✔（注册策略/邀请开关双开关） | DR-01/DR-02（注册/邀请策略） |
| F3.7 | — | — | — | ✔（域名称/LOGO/描述） | — | — | —（code 创建后不可改） |
| F3.8 | ✔（KV 列表） | — | ✔（KV 新增） | ✔（KV 编辑） | ✔（KV 删除） | — | — |
| F3.9 | ✔（词条列表） | — | ✔（词条新增，重复提示） | —（无编辑，增删查） | ✔（词条删除） | —（空态） | 词条去首尾空格、禁空词（US-S2-05） |
| F3.10 | — | — | — | — | — | 部分（4 项 Statistic + 趋势图；统计值「—」、趋势「数据接入中」） | —（S3+ 新增，无 Story） |
| F3.11 | — | — | — | — | — | 规划中（占位：通知模板配置；菜单与权限已就绪） | — |
| F3.12 | ✔（audit/login 列表，分页 + 筛选） | —（日志行内查看） | — | — | —（审计不可删除） | ✔（时间/结果/关键词筛选） | 审计日志不可删除（US-S2-06 AC4） |
| F3.13 | ✔（角色列表，scope=domain） | —（权限树在编辑内） | ✔（角色创建） | ✔（角色编辑：权限树） | ✔（角色删除，预置角色禁删） | — | 禁止删除预置角色（US-S3-00）；scope=domain 一致 |
| F3.14 | ✔（菜单树，scope=business 筛选） | — | ✔（菜单创建） | ✔（菜单编辑） | ✔（级联删除子菜单） | — | —（US-S2-E2-00 关联） |
| F3.15 | —（模板遗留占位） | — | — | — | — | — | — |
| F3.16 | —（模板遗留占位） | — | — | — | — | — | — |
| F4.1 | ✔（卡片列表 + 分页/关键词搜索/创建日期筛选） | ✔（详情 10 Tab） | ✔（新建向导 Modal，Step3 入域双开关） | ✔（基础信息 PUT） | ✔（软删除：code 确认 + Step-up） | ✔（启用/禁用、详情 10 Tab 子配置入口） | US-S2-01 AC2（code 完全一致确认 + Step-up 二次认证）、AC3（已删域不在列表） |
| F4.2 | ✔（团队模板列表，菜单已隐藏 V20260726092200） | — | 部分（团队模板创建，菜单隐藏；「域模板提炼」语义待对齐） | 部分（团队模板编辑，菜单隐藏；「域模板提炼」语义待对齐） | 部分（团队模板删除，菜单隐藏；「域模板提炼」语义待对齐） | — | —（US-S1-02 关联） |
| F4.3 | ✔（全局员工 + 离职池列表，离职池含域名/角色/离职时间/操作人） | —（弹窗承载） | 部分（手动创建 ✔；CSV 批量导入未实现） | —（归 F4.7 用户编辑） | —（无永久删除前端入口；删除后进离职池） | ✔（离职二次确认、离职池一键恢复、重置密码） | 永久删除无前端入口（inventory §4.1） |
| F4.4 | — | — | — | — | — | 部分（系统设置 KV read/update ✔；安全告警中心未实现；密码强度/登录锁定/IP 白名单未成品） | —（US-S1-07 关联；inventory §5.3） |
| F4.5 | — | — | — | — | — | ✔（滑块验证、动态菜单、三元规则跳转、忘记密码 stub） | US-S2-UX-01（滑块）；US-S3-00（三元规则）；FR-03（无权限菜单不可见） |
| F4.6 | ✔（最近审计列表） | — | — | — | — | 部分（Statistic 卡片 + 快捷入口；`DemoDataService` mock 非真实聚合） | —（S1 待办接 count(*) 聚合） |
| F4.7 | ✔（搜索 + 部门树侧栏 + 状态 Tag） | —（弹窗承载） | ✔（创建） | ✔（账号/姓名/手机/邮箱/组织/角色） | —（永久删除后端就绪、前端无入口） | ✔（离职二次确认、重置密码随机 16 位 Modal 复制、启停） | —（US-S1-07）；永久删除无入口（inventory §4.1） |
| F4.8 | ✔（树形展示 + 搜索 + 展开/折叠） | — | ✔（新建：父部门/负责人） | ✔（编辑） | ✔（删除：子部门校验） | ✔（循环引用检测） | 删除子部门校验、循环引用检测（inventory §2） |
| F4.9 | ✔（scope 筛选 platform/domain） | — | ✔（code/name/scope/description + 权限树） | ✔（权限树分配） | ✔（预置角色禁删） | ✔（菜单+按钮权限树分配） | scope 不一致拒绝 + 中文提示（US-S3-00 AC3）；禁止删除预置角色 |
| F4.10 | ✔（菜单树 Table + scope 筛选） | — | ✔（菜单/按钮节点） | ✔（编辑） | ✔（级联子菜单） | ✔（图标选择器） | —（US-S1-07） |
| F4.11 | — | — | — | — | — | 部分（仅重定向至 `/platform/role`，无独立界面） | — |
| F4.12 | ✔（审计/登录 Tabs + 分页 + 筛选） | —（日志行内查看） | — | — | —（审计不可删除） | ✔（筛选：模块/操作者/关键词/时间；登录按主体/门户/结果/客户端/IP） | 审计不可删除（inventory §5.3）；导出 Todo |
| F4.13 | ✔（全局词库） | — | ✔（词条新增） | ✔（词条编辑） | ✔（词条删除） | —（跨域生效：`business_domain_id` 为空） | 词条去首尾空格、禁空词（US-S2-05） |
| F4.14 | ✔（类型/属性/状态/模板列表） | — | ✔（类型/属性/状态/模板新增） | ✔（Formily 表单设计器、属性插槽） | 部分（非系统属性可删；系统属性锁定） | ✔（草稿/发布/版本历史、React Flow 状态流 DAG、拖拽排序、默认值 JSON） | TR-01（至少一个终态）；US-S3-01a（系统属性锁定不可删） |
| F4.15 | ✔（SLA 规则 + 工作日历列表） | — | ✔（SLA 规则/工作日历新增） | ✔（SLA 规则/工作日历编辑） | ✔（SLA 规则/工作日历删除） | ✔（SlaTimingEngine 计时引擎、违约动作配置） | —（E4 US-S3-E4-01/02 Stretch；未挂验收 Story） |
| F4.16 | ✔（站内信列表） | ✔（查看） | — | — | — | ✔（未读/已读标记） | —（P0 inbox 契约） |
| F4.17 | ✔（附件列表/下载） | — | — | — | — | ✔（上传：服务端代理 → MinIO） | —（P0 契约；依赖 MinIO 服务） |
| F4.18 | — | — | — | — | — | 规划中（占位：批量导入导出；API 待查） | — |
| F4.19 | ✔（KV 列表） | — | ✔（KV 新增） | ✔（KV 编辑） | ✔（KV 删除） | — | —（域详情「配置」Tab 同源） |
| F4.20 | ✔（邀请码列表） | — | ✔（邀请码创建） | —（无编辑，失效代替） | ✔（邀请码删除） | ✔（失效） | DR-02（邀请码）；US-S3-02（CustomerWeb 接真实入域 API 未完成） |
| F4.21 | — | — | — | — | — | 规划中（占位：组织架构配置） | — |
| F4.22 | —（模板遗留） | — | — | — | — | — | — |

---

## 8. 权限码对照表

> ① 权限码以 `PermissionCodes.java` 与 Flyway 迁移为准，本表为**参考快照（2026-08-11）**，仅用于追踪，不替代代码/迁移权威。
> ② 格式映射：PRD §4.1.3 的 `模块:操作`（如 `ticket:delete`）对应实现**点分码** `ticket.delete`；数据范围后缀 `:self` / `:domain_all` 对应 `ticket.view.self` / `ticket.view.domain_all`。
> ③ 重命名漂移：`domain.blocked_word.*` 已由 Flyway V202606080001 迁为 `platform.domain.control.blocked_word.*`（旧码在 `PermissionCodes.java` 与前端 `domain-permissions.ts` 仍保留），以最新 Flyway 为准。
> ④ **双轨说明**（目标态，任务 08-11-group-role-management）：`domain_role`（含 `domain_role_permission` / `domain_member_role`）= 各域运行时**业务角色**实例（轨 A，成员绑定唯一路径）；IAM `role(scope=domain)` + `iam_role_permission` = **控制台权限角色**（轨 B，定义层）。目标态**冻结双轨**：新增角色走模板/域端，旧角色只读保留。

| 功能编号 | 权限码 | 默认授权角色 | 来源 |
| :--- | :--- | :--- | :--- |
| F1.1 | —（认证豁免/公开，客户会话） | — | 无权限码（客户身份走 RequireSession；`POST /api/v1/domains/{domain_id}/tickets` 无管理岗门控） |
| F1.2 | —（认证豁免/公开） | — | 无权限码（E5 未拆 Story，页面占位） |
| F1.3 | —（认证豁免/公开） | — | 无权限码（工单类型路径，客户提单） |
| F1.4 | —（认证豁免/公开） | — | 无权限码 |
| F1.5 | —（认证豁免/公开） | — | 无权限码（P1 未拆 Story，全站无入口） |
| F1.6 | —（认证豁免/公开） | — | 无权限码（登录页公开；滑块 challenge 后端校验） |
| F1.7 | —（认证豁免/公开） | — | 无权限码（注册公开；入域策略由 DR-01/DR-02 控制） |
| F1.8 | —（认证豁免/公开） | — | 无权限码（切换 switch-domain 真实 API） |
| F1.9 | —（认证豁免/公开） | — | 无权限码 |
| F1.10 | —（认证豁免/公开） | — | 无权限码（客户站内信 RequireSession） |
| F1.11 | —（认证豁免/公开） | — | 无权限码 |
| F1.12 | —（认证豁免/公开） | — | 无权限码 |
| F2.1 | `ticket.{read,create,view.self,view.domain_all,claim,assign,reply.self,reply,close,withdraw.self,merge}` | agent / domain_admin / super_admin（域内置角色） | permission-code-labels.ts（11 码）+ US-S3-04（agent 绑定待联调确认） |
| F2.2 | `domain.sla.read`（推断） | agent / domain_admin | 推断：依赖 F4.15 引擎（SlaController.java:40）；待确认，以代码为准 |
| F2.3 | —（E5 未排期） | — | 无权限码（E5 未拆 Story） |
| F2.4 | `domain.home.read` | domain_admin / agent | Flyway V20260728170000（BUSINESS-HOME-MENU） |
| F3.1 | `platform.domain.control.ticket_type.{read,create,update,delete}`；域端 `domain.ticket_type.*` | platform_admin / super_admin（平台域详情）；domain_admin（域端） | Flyway V202606170001 + PermissionCodes.java:126 |
| F3.2 | `domain.sla.*`；`domain.notification_template.*`（通知占位，推断） | domain_admin / super_admin | SLA：PermissionCodes.java:171 + SlaController.java:40；通知：占位「菜单与权限已就绪」；待确认，以代码为准 |
| F3.3 | `domain.member.{read,create,update_roles,update_status,delete}`；`domain.customer.{read,create,update_status}`；`domain.role.*` | domain_admin / super_admin（域内置） | Flyway V202606060001 + US-S2-03/S2-04 |
| F3.4 | `domain.ticket_type.*`、`domain.ticket_attribute.*`、`domain.ticket_status.*` | domain_admin / super_admin | PermissionCodes.java（domain.ticket_* 族；inventory 缺项，待确认精确迁移） |
| F3.5 | `domain.customer.{update,update_status,reset_password}` | domain_admin / super_admin | Flyway V20260806120000 + PermissionCodes.java:22 |
| F3.6 | `domain.invitation_code.*`；`domain.general.read` | domain_admin / super_admin | 代码（onboarding 页 auth 数组）+ US-S1-03 |
| F3.7 | `domain.general.{read,update,update_status}` | domain_admin / super_admin | 代码（域设置控制器）+ US-S2-01 域端版 |
| F3.8 | `domain.config.{read,update}` | domain_admin / super_admin | PermissionCodes.java:166 + domain/config/index.tsx:39 |
| F3.9 | 域端 `domain.blocked_word.*`（旧码保留）；平台域详情 `platform.domain.control.blocked_word.*`（以最新为准） | domain_admin（域端）；platform_admin（平台域详情） | Flyway V202606080001（重命名）+ US-S2-05 |
| F3.10 | `domain.overview.read` | domain_admin / super_admin | AdminPermissionCatalog.java:27（S3+ 新增，无 Story） |
| F3.11 | `domain.notification_template.*` | domain_admin | 占位「菜单与权限已就绪」（E2/E4） |
| F3.12 | `domain.audit_log.read`、`domain.login_log.read` | domain_admin / super_admin | Flyway V202606090001 + US-S2-06 |
| F3.13 | `domain.role.{read,create,update,delete}` | domain_admin / super_admin | 代码（IAM 域级 API）+ US-S2-02 |
| F3.14 | `domain.menu.{read,create,update,delete}` | domain_admin / super_admin | 代码（IAM 域级 API）+ US-S2-E2-00 关联 |
| F3.15 | —（模板遗留） | — | 无权限码（模板遗留占位页） |
| F3.16 | —（模板遗留） | — | 无权限码（模板遗留占位页） |
| F4.1 | `platform.domain.list.read`、`platform.domain.create`、`platform.domain.control.{entry,overview,read,general.update,general.delete}` 等 | platform_admin / super_admin | Flyway V202605330004/V202605330005 + US-S1-02/S2-01 |
| F4.2 | `platform.ticket_config.template.*`（推断） | platform_admin | 推断：权限已注册、菜单已隐藏（V20260726092200）；待确认，以代码为准 |
| F4.3 | `platform.user.{create,disable,offboard,restore,reset_password}`；`platform.user.offboard_pool.{read,export,batch_restore}` | platform_admin / super_admin | PermissionCodes.java:64 + AdminPermissionCatalog.java:107 + US-S1-07 |
| F4.4 | `platform.system_config.{read,update}`（推断） | platform_admin | PermissionCodes.java:168-169；待确认，以代码为准 |
| F4.5 | —（认证豁免/公开） | — | 无权限码（登录公开；动态菜单/权限快照为登录后行为） |
| F4.6 | `platform.dashboard.read`（推断） | platform_admin | PermissionCodes.java:212；待确认，以代码为准 |
| F4.7 | `platform.user.{read,create,update,disable,reset_password,restore,delete}` | platform_admin / super_admin | PermissionCodes.java + US-S1-07 |
| F4.8 | `platform.organization.{read,create,update,delete}` | platform_admin / super_admin | PermissionCodes.java + US-S1-07（inventory §2） |
| F4.9 | `platform.role.{read,create,update,delete}`；`platform.role_permission.*`；`platform.role.bind` | platform_admin / super_admin | PermissionCodes.java:50 + US-S3-00 |
| F4.10 | `platform.menu.{read,create,update,delete}` | platform_admin / super_admin | PermissionCodes.java + US-S1-07 |
| F4.11 | —（重定向页，门控复用 `platform.role.*`） | platform_admin | 代码（pages/platform/permission/index.tsx） |
| F4.12 | `platform.log.audit.read`、`platform.log.login.read` | platform_admin / super_admin | Flyway V202605210001 + US-S1-09 |
| F4.13 | `platform.blocked_word.{read,create,delete}` | platform_admin / super_admin | Flyway V202606080001 + US-S2-05 |
| F4.14 | `platform.ticket_config.{attr,type,status,template}.*`（16 码） | platform_admin / super_admin | Flyway V202607070003 等 + US-S3-01/01a |
| F4.15 | `domain.sla.{read,create,update,delete}`（平台侧复用） | platform_admin / domain_admin | PermissionCodes.java:171 + SlaController.java:40（未挂验收 Story） |
| F4.16 | `inbox.read`、`inbox.mark_read` | platform_admin / domain_admin | 代码（InboxController）+ P0 inbox 契约 |
| F4.17 | `attachment.upload`、`attachment.download` | platform_admin / domain_admin / agent（员工会话） | 代码（AttachmentController）+ P0 契约 |
| F4.18 | `platform.user.import`、`platform.user.offboard_pool.export` | platform_admin | 占位但权限已就绪（inventory §4.1） |
| F4.19 | 待确认（`platform.domain.control.config.*` 未在 PermissionCodes.java 命中；疑复用 `domain.config.*`） | platform_admin / domain_admin | 未命中 PermissionCodes.java；待确认，以代码为准（域详情「配置」Tab 同源） |
| F4.20 | `domain.invitation_code.{read,create,delete}`（平台侧复用） | platform_admin / super_admin | 代码（InvitationCodeController）+ US-S1-03/S1-06 |
| F4.21 | `platform.organization.*` | platform_admin | 占位但权限已就绪（inventory §2） |
| F4.22 | —（模板遗留） | — | 无权限码（模板遗留页，demo 路由不受控） |
