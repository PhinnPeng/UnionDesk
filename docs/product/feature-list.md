# 功能清单（以功能为核心 · 端侧分区）

> **文档版本**：2.0（2026-08-13 重构，替代 1.0 多表结构）
> **更新日期**：2026-08-13
> **说明**：本清单以**功能**为唯一核心实体，按端侧分区；每行汇聚该功能的所有关键属性（页面/菜单/路由/状态/权限/边界），为单一事实来源。状态变更仅需修改主表对应行。目标态决策（2026-08-12）已内嵌各行备注；集团统一角色目标态见任务 `08-11-group-role-management`。

---

## 状态图例

| 图标 | 含义 |
| :--- | :--- |
| ✅ | 已实现（前后端均可正常使用） |
| 🚧 | 部分（核心路径可用，缺边界/依赖Mock/部分入口占位） |
| 📅 | 规划中（未排期或待后续Sprint） |
| 🗑 | 占位（模板遗留/非业务功能，或待移除） |

---

## 列填写规范

| 列名 | 填写说明 |
| :--- | :--- |
| **编号** | 唯一标识（如 F1.1），用于跨文档追踪（与 prd.md §4.2 一致） |
| **功能名称** | 业务功能名 |
| **页面** | 界面标题/模块名（如“业务域管理页”），即用户看到的页面名称 |
| **菜单路径** | 导航层级，用 `>` 分隔（如“平台管理 > 业务域管理”）；若无独立菜单，写“（通过XX按钮/入口进入）” |
| **路由** | URL路径（含参数占位符，如 `/tickets/:ticketId`） |
| **功能简述** | 一句话说明核心能力（不超过30字） |
| **关键操作** | 动词枚举（如“增、删、改、查、导出、批量关闭、转派”） |
| **状态** | 使用上方图例 |
| **权限码** | 入口典型权限码；公开/认证豁免写“公开”或“客户会话”；多码写典型或“见附录” |
| **备注/边界** | 关键约束、已知缺陷、依赖项、目标态说明 |

---

## 1. 客户端（CustomerWeb）

> 权限列：`公开` = 无需登录；`客户会话` = 需客户身份登录（RequireSession）。

| 编号 | 功能名称 | 页面 | 菜单路径 | 路由 | 功能简述 | 关键操作 | 状态 | 权限码 | 备注/边界 |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| F1.1 | 提交工单（动态表单） | 提单页 | 无（底部导航「工单」Tab → 提单按钮） | `/tickets/new` | 三步提单：选类型→填标题/描述→提交展示工单号，调真实 API | 提单提交、类型选择 | 🚧 | 客户会话 | 动态表单字段未渲染（仅系统字段 title/description）；attachmentIds 恒空数组 |
| F1.2 | 在线咨询 | 在线咨询窗口 | 无（底部导航「咨询」Tab） | `/chat` | 排队→接入客服→实时聊天，会话可转工单 | 排队/接入/聊天/转工单（规划中） | 📅 | 客户会话 | 纯静态占位页「即将开放」；E5 未排期；目标态成品页 |
| F1.3 | 反馈/建议（工单类型路径） | 提单页（类型选择） | 无（同 F1.1 入口） | `/tickets/new` | 「反馈」「建议」为预置工单类型，提单时可选，管理员启停 | 提单可选类型提交 | ✅ | 客户会话 | MVP 以工单类型路径交付，无独立入口；预置类型由管理员启用/停用（US-S3-01 AC4） |
| F1.4 | 我的工单 / 咨询历史 | 我的工单列表/工单详情 | 无（底部导航「工单」Tab） | `/tickets`、`/tickets/:ticketId` | 工单列表（类型侧栏/生命周期筛选/关键词）+详情时间线与补充、撤回 | 查、补充说明、撤回（open 态 version 乐观锁） | 🚧 | 客户会话 | 咨询历史无、详情关联咨询占位卡、附件展示无；目标态补「咨询历史」入口 |
| F1.5 | 满意度评价 | 通知中心（目标态主入口） | 无（目标态：通知中心评价入口） | 无（全站无入口） | 工单关闭/咨询结束后，星级+文字评价 | 评价提交（规划中） | 📅 | 客户会话 | 全站 grep 0 命中；目标态主入口=通知中心，工单详情「确认解决后评价」为辅入口（P1 未拆 Story） |
| F1.6 | 登录 | 登录页 | 无（公开页） | `/login`、`/d/:domainCode/login` | 账号密码登录+滑块验证+记住账号+专属域入口 | 登录、滑块验证、记住账号 | ✅ | 公开 | 滑块 challenge 5s 过期缓冲；忘记密码为占位 toast；新环境登录站内提醒 |
| F1.7 | 注册与入域 | 注册页 | 无（公开页） | `/register`、`/d/:domainCode/register` | 注册表单+开放域下拉+邀请码，专属域预填邀请码 | 注册提交（本地 mock） | 🚧 | 公开 | 本地状态机 mock（`cust-at` 伪 token）未调 `/api/v1/auth/register`；域下拉仅展示 `registration_enabled=allowed` 待接入（US-S3-02） |
| F1.8 | 业务域选择与切换 | 业务域选择页 | 无（登录后路由守卫引导） | `/domains` | 「已加入/可加入/需管理员开通」三组卡片，切换当前域 | 切换当前域（真实 switch-domain API） | ✅ | 客户会话 | 邀请码加入为本地 mock（归 F1.7）；FR-05 未入域拒绝 |
| F1.9 | 服务首页 | 服务首页 | 无（底部导航「首页」Tab） | `/home` | 问候语、待处理提醒、生命周期统计、最近 5 条工单、未读通知前 3 条 | 查看统计、jumpUrl 跳转 | ✅ | 客户会话 | 首页本身无独立 Story |
| F1.10 | 站内信 / 通知中心 | 通知中心 | 无（底部导航「通知」Tab，带未读 badge） | `/inbox` | 站内信列表 kind 分类、未读数、标已读、jumpUrl 跳转 | 查、标为已读、跳转查看 | ✅ | 客户会话 | P0 契约未拆 Story |
| F1.11 | 个人中心 | 个人中心 | 无（底部导航「我的」Tab） | `/me` | 账号信息、业务域/通知入口、退出登录 | 查看账号信息、退出登录 | 🚧 | 客户会话 | 通知偏好为占位 toast；S3+ 新增无 Story |
| F1.12 | 修改密码 | 修改密码页 | 无（个人中心入口/强制改密） | `/change-password` | 当前/新密码/确认，前端校验，强制改密 | 修改密码 | ✅ | 客户会话 | 新密码≥6 位、两次一致、与当前不同；`mustChangePassword` 强制改密不可跳过；RequireSession 守卫 |

---

## 2. 管理端-业务域端（AdminWeb / Business）

> 权限列：写典型权限码，完整列表见附录。

### 2.1 员工端（M2）

| 编号 | 功能名称 | 页面 | 菜单路径 | 路由 | 功能简述 | 关键操作 | 状态 | 权限码 | 备注/边界 |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| F2.1 | 工单队列与详情处理 | 工单队列/工单详情（目标态新增·页面未建，待迁移） | 无菜单（现状为平台端 P0 演示页 `/platform/ticket-pool`，business 端队列未成品 US-S3-04） | `/ticket-queue`、`/ticket-queue/:ticketId`（建议）；现状 `/platform/ticket-pool`、`/platform/ticket-detail` | 按域/状态/优先级/SLA 筛选队列，详情时间线+属性卡处理 | 查、公开回复、内部备注、转派、变更状态、批量领取、批量关闭 | 🚧 | `ticket.*`（11 码，见附录） | TR-02 终态后不可变更；FR-01/FR-03；P0 演示页为平台端内建路由（hideInMenu） |
| F2.2 | SLA 感知与高亮 | 工单队列/详情（页内增强，无独立页） | 无（随 F2.1 页面） | — | 列表/详情高亮 SLA 即将超时或已超时工单 | —（规划中） | 📅 | `domain.sla.{read,create,update}` | 依赖 F4.15 SlaTimingEngine（已实现），未接入工单 UI；E4 Stretch 未排期 |
| F2.3 | 在线咨询工作台 | 在线咨询工作台（目标态新增·页面未建） | 无 | `/consult-workbench`（建议） | 手动接入排队客户，实时聊天，一键转工单 | 接入、聊天、撤回（2 分钟内消息）、转工单（规划中） | 📅 | 无码（E5 未拆 Story） | E5 未排期；转工单带客户信息+会话摘要 |
| F2.4 | 业务域端首页 / 工作台 | 域端首页/工作台 | 概览（业务端一级菜单 `BUSINESS-HOME-MENU`，V20260728170000；V20260728194500 更名「概览」） | `/home` | 当前域+按权限过滤快捷入口+概览说明 | 快捷入口跳转（按权限过滤） | ✅ | `domain.home.read` | 权限/菜单来自 V20260728170000（BUSINESS-HOME-MENU）；FR-03 无权限入口不可见 |

### 2.2 域管理后台（M3）

| 编号 | 功能名称 | 页面 | 菜单路径 | 路由 | 功能简述 | 关键操作 | 状态 | 权限码 | 备注/边界 |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| F3.1 | 工单类型设计 | 域事项配置（Drawer 三 Tab） | 事项配置（业务端一级 `BUSINESS-DOMAIN-TICKET-CONFIG`，V20260727183000；V20260801180000 提至 order 30） | `/domain/ticket-config`；平台域详情内 `/platform/domains/ticket-type-config/*`、`/platform/domains/ticket/form-design/*` | 类型 CRUD + Formily 表单设计（title/description 锁定）+ React Flow 状态流 DAG | 增、删、改、查、预置「反馈」「建议」启停 | ✅ | 域端 `domain.ticket_type.*`；平台域详情 `platform.domain.control.ticket_type.*` | TR-01 至少一个终态（违反保存失败+中文）；US-S3-01 AC2 系统字段锁定；域详情内同源（见附录B） |
| F3.2 | SLA 规则与通知模板 | 域 SLA 规则（目标态新增·页面未建，SLA 部分主承载）+ 域通知配置（占位） | SLA：无（现状复用平台 `/platform/sla-management`）；通知：系统设置 > 功能配置 > 通知配置（`BUSINESS-DOMAIN-NOTIFICATIONS`，V20260801191000） | `/domain/settings/sla`（建议）；现状 `/platform/sla-management`、`/domain/settings/notifications` | 本域默认+按类型 SLA（首响/解决时限/违约动作），事件通知模板 | SLA 增删改查（复用 F4.15）、通知模板配置（占位） | 🚧 | `domain.sla.*`；`domain.notification_template.{read,update}` | 通知模板为占位（菜单与权限已就绪）；2026-08-12 决策：SLA 域端新建独立页，平台级 SLA 保留；与 F3.11 语义重叠待澄清 |
| F3.3 | 成员 / 客户 / 角色管理 | 域成员管理/域客户管理/域角色管理 | 组织成员 > 员工管理（`BUSINESS-DOMAIN-SETTINGS-ORG` order 35，V20260801192000）；客户管理 > 客户列表；组织成员 > 角色管理 | `/domain/settings/members`、`/domain/customers/list`、`/domain/settings/roles` | 域成员（平台员工添加/改角色/启停/移除）、域客户（列表/手动添加/启停）、域角色（预置+自定义） | 成员增、移除、改角色、启停；客户手动添加、启停；角色增删改 | ✅ | `domain.member.*`、`domain.customer.{read,create,update_status}`、`domain.role.*` | 最后 `domain_admin`/`super_admin` 保护；同一员工同域不可重复添加；与平台域详情 Tab 同源双入口（权限码 OR，见附录B）；目标态：角色实例受模板锁定字段约束 |
| F3.4 | 事项属性与状态配置（域内） | 域事项配置（三面板：类型/属性/状态） | 事项配置（同 F3.1） | `/domain/ticket-config` | 域内属性/状态三面板复用平台面板，插槽拖拽排序 | 属性/状态增删改、拖拽排序、默认值 JSON | ✅ | `domain.ticket_attribute.*`、`domain.ticket_status.*` | 系统属性锁定不可删；默认值 JSON `{mode,value}`（US-S3-01a）；TR-02 配置层约束 |
| F3.5 | 域客户管理增强 | 域客户管理 | 客户管理 > 客户列表（`BUSINESS-DOMAIN-CUSTOMERS` order 40，V20260801191000；编辑/重置密码按钮 V20260807130000） | `/domain/customers/list` | 查询/手动添加/从员工导入/启停/编辑/重置密码/只读详情 | 查、手动添加、从员工导入、启停、编辑、重置密码 | ✅ | `domain.customer.{update,update_status,reset_password}` | 无删除（启停代替）；与平台域详情「客户」Tab 同源双入口（见附录B），资料编辑/重置密码仅域端可写；目标态：平台跨域批量停用与域端日常运营协同 |
| F3.6 | 入域配置（域端） | 入域配置 | 客户管理 > 入域配置（`BUSINESS-DOMAIN-CUSTOMERS-ONBOARDING`，V20260801191000；旧入口 `/domain/onboarding` legacy 重定向） | `/domain/customers/onboarding`、`/domain/settings/onboarding` | 邀请码 + 注册策略双开关维护 | 邀请码生成、删除/失效、双开关 | ✅ | `domain.invitation_code.*`、`domain.general.read` | DR-01/DR-02 注册/邀请策略；三入口同页（见附录B） |
| F3.7 | 域基础设置 | 域基础设置 | 系统设置 > 通用设置（`BUSINESS-DOMAIN-SETTINGS` order 50，V20260801180000；`BUSINESS-DOMAIN-BASIC`，V20260801191000） | `/domain/settings/basic` | 域名称/LOGO/描述维护 | 编辑 | ✅ | `domain.general.{read,update,update_status}` | code 创建后不可改；启停走 `update_status` |
| F3.8 | 域参数配置（KV） | 域参数配置 | 系统设置 > 功能配置 > 参数配置（`BUSINESS-DOMAIN-CONFIG`，V20260801191000） | `/domain/settings/config` | KV 键值对（key/value/valueType/description） | 增、删、改、查 | ✅ | `domain.config.{read,update}` | — |
| F3.9 | 域屏蔽词库 | 域屏蔽词库 | 系统设置 > 功能配置 > 屏蔽词库（`BUSINESS-DOMAIN-BLOCKWORDS`，V20260801191000） | `/domain/settings/blockwords` | 域内词条增删查，重复提示、空态 | 增、删、查 | ✅ | 域端 `domain.blocked_word.*`（旧码保留）；平台域详情 `platform.domain.control.blocked_word.*` | 词条去首尾空格、禁空词（US-S2-05）；平台域详情「屏蔽词」Tab 同源（见附录B） |
| F3.10 | 域运营概览 | 域运营概览 | 运营概览（业务端一级 `BUSINESS-DOMAIN-OVERVIEW`，V20260727183000 建「概览」、V20260728194500 更名「运营概览」order 20） | `/domain/overview` | 4 项 Statistic + 趋势图 | 查看统计/趋势（部分） | 🚧 | `domain.overview.read` | 统计值「—」、趋势「数据接入中」；S3+ 新增无 Story |
| F3.11 | 域通知配置 | 域通知配置 | 系统设置 > 功能配置 > 通知配置（`BUSINESS-DOMAIN-NOTIFICATIONS`，V20260801191000） | `/domain/settings/notifications` | 站内信/邮件模板配置 | —（规划中） | 📅 | `domain.notification_template.*` | Empty「功能开发中，菜单与权限已就绪」；与 F3.2 通知模板部分语义重叠待澄清 |
| F3.12 | 域级操作日志 / 登录日志 | 域级操作日志/登录日志 | 系统设置 > 安全与审计 > 操作日志/登录日志（`BUSINESS-DOMAIN-AUDIT-LOGS`/`LOGIN-LOGS`，V20260801191000） | `/domain/settings/audit-logs`、`/domain/settings/login-logs` | 域级 audit/login 列表，分页+时间/结果/关键词筛选 | 查、筛选 | ✅ | `domain.audit_log.read`、`domain.login_log.read` | 审计不可删除（US-S2-06 AC4）；平台域详情侧栏同源（见附录B） |
| F3.13 | 系统角色管理（域端） | 系统角色管理 | 无后端菜单（`/system/*` 菜单在 V202605220001 trim 中删除未重建；前端静态路由 `roles:["admin"]` scope=business） | `/system/role` | 角色 CRUD + 权限树分配，scope=domain | 增、删、改、查、权限树分配 | ✅ | `domain.role.*` | 预置角色禁删；平台域详情「角色」Tab 为只读（可写仅域端）；目标态：模板下发角色实例展示「模板来源+锁定字段」，锁定字段只读、非锁定可微调 |
| F3.14 | 系统菜单管理（域端） | 系统菜单管理 | 无后端菜单（同 F3.13；前端静态路由 `auth: "domain.menu.read"`） | `/system/menu` | 菜单树 CRUD，scope=business 筛选 | 增、删、改、查 | ✅ | `domain.menu.*` | US-S2-E2-00 关联；权限由 V202606140001 注册 |
| F3.15 | 系统用户管理（域端） | 系统用户管理（模板遗留） | 无后端菜单（前端静态路由） | `/system/user` | 域端系统用户管理（模板占位） | — | 🗑 | 无码 | 模板残留（单 Input）非业务功能；**目标态移除**（2026-08-12 决策，业务由 F3.3 成员管理覆盖） |
| F3.16 | 系统部门管理（域端） | 系统部门管理（模板遗留） | 无后端菜单（前端静态路由） | `/system/dept` | 域端系统部门管理（模板占位） | — | 🗑 | 无码 | 模板残留（计数器 demo）非业务功能；**目标态移除**（2026-08-12 决策） |

---

## 3. 管理端-平台端（AdminWeb / Platform）

| 编号 | 功能名称 | 页面 | 菜单路径 | 路由 | 功能简述 | 关键操作 | 状态 | 权限码 | 备注/边界 |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| F4.1 | 业务域创建与管理 | 业务域管理（列表/新建向导/详情 10 Tab） | 业务域管理 > 业务域列表（`PLATFORM-DOMAIN-CATALOG` V202605200004 + 列表 V202605200005；详情 `PLATFORM-DOMAIN-DETAIL`，V202605330005 更名「业务域控制台」） | `/platform/domains`、`/platform/domains/detail/:domainId` | 域卡片列表+分页/搜索/日期筛选；新建向导 Modal；详情 10 Tab；软删除 | 查、建、改（PUT）、软删除（code 确认+Step-up）、启停 | ✅ | `platform.domain.list.read`、`platform.domain.create`、`platform.domain.control.*` | 已删域不展示（deleted_at）；已删域直链 AC4 延后；目标态：集团统一管理，详情成员/客户/角色 Tab 与域端同源双入口，平台侧角色走模板下发 |
| F4.2 | 模板中心 | 事项配置 > 团队模板子页 | 无独立一级菜单（templates 菜单隐藏 V20260726092200；主菜单 `/platform/ticket-config` 无迁移创建记录，待确认） | `/platform/ticket-config/templates` | 将域完整配置提炼为模板，供快速创建新域 | 模板增、删、改、查（菜单隐藏） | 🚧 | `platform.ticket_config.template.*` | **2026-08-12 决策：目标态沿用现状页**（语义「域模板提炼」与实现待对齐）；权限已注册、菜单已隐藏 |
| F4.3 | 员工账号与离职池 | 员工列表/离职池 | 组织管理 > 用户管理（`ADM0000000070` 组织管理 catalog）；组织管理 > 离职池（`ADM0000000041`） | `/platform/user`、`/platform/offboard-pool` | 全局员工列表/创建/离职；离职池列表+一键恢复；重置密码 | 建、离职二次确认、离职池恢复、重置密码 | 🚧 | `platform.user.{create,disable,offboard,restore,reset_password}`、`platform.user.offboard_pool.{read,export,batch_restore}` | CSV 批量导入未实现（见 F4.18）；永久删除无前端入口；离职池含域名/角色/离职时间/操作人；目标态并入用户管理页 |
| F4.4 | 系统设置与安全告警 | 系统设置 | 无菜单入口（`/platform/system-settings` 菜单 trim 删除未重建；前端内建路由） | `/platform/system-settings` | 系统设置 KV 读写；安全告警中心（规划中） | 系统设置 KV 读/写 | 🚧 | `platform.system_config.{read,update}` | 安全告警中心未实现（inventory §5.3 Todo）；密码强度/登录锁定/IP 白名单未成品；**2026-08-12 决策：安全告警中心独立页**（建议 `/platform/security-alerts`） |
| F4.5 | 平台端登录与动态菜单 | 登录页 | 无（公开页；平台/业务域共用） | `/login` | 滑块验证登录，权限快照三元规则跳转双轨首页，后端动态菜单 | 登录、滑块验证、动态菜单跳转 | ✅ | 公开 | 忘记密码为占位 stub；`admin` 默认绑定 `platform_admin`（US-S3-00）；FR-03 无权限菜单不可见 |
| F4.6 | 平台首页仪表盘 | 平台首页仪表盘 | 平台首页 > 首页概览（`PLATFORM-HOME-CATALOG`，V202605220001） | `/platform/home` | Statistic 卡片+快捷入口+最近审计列表 | 查看统计/快捷入口/审计列表（mock） | 🚧 | `platform.dashboard.read` | 数据来自 `DemoDataService` mock 非真实聚合（S1 待办接 count(*)） |
| F4.7 | 用户管理 | 用户管理 | 组织管理 > 用户管理（`ADM0000000039`；按钮 reset_password `ADM00000000xx`） | `/platform/user` | 列表（搜索+部门树+状态 Tag）；创建/编辑/离职/重置密码 | 增、改、查、离职二次确认、重置密码（随机 16 位 Modal 复制）、启停 | ✅ | `platform.user.{read,create,update,disable,reset_password,restore,delete}` | 永久删除后端就绪、前端无入口；目标态：**跨域批量停用**（选员工→域集→step-up 二次认证→部分成功摘要，TR-04） |
| F4.8 | 组织 / 部门管理 | 组织架构 | 组织管理 > 组织架构（`ADM0000000040`） | `/platform/dept` | 树形 CRUD+搜索+展开折叠+循环引用检测 | 增、删、改、查、展开/折叠 | ✅ | `platform.organization.{read,create,update,delete}` | 删除子部门校验、循环引用检测；**2026-08-12 决策：目标态并入 F4.21 组织配置** |
| F4.9 | 角色管理 | 角色管理 | 权限管理 > 角色管理（`ADM0000000048` 权限管理 catalog + `ADM0000000049` 角色管理） | `/platform/role` | 角色 CRUD，scope 筛选 platform/domain，菜单按钮权限树 | 增、删、改、查、菜单+按钮权限树分配 | ✅ | `platform.role.{read,create,update,delete}`、`platform.role_permission.*`、`platform.role.bind` | 预置角色禁删；scope 不一致拒绝+中文提示（US-S3-00 AC3）；**目标态：本页新增「模板」Tab**（role_template 一次下发多域/同步/漂移状态列），domain scope 角色双轨冻结（新角色走模板/域端，旧角色只读保留） |
| F4.10 | 菜单管理 | 菜单管理 | 权限管理 > 菜单管理（`ADM0000000050`，component `./platform/system/menu`） | `/platform/menu`（组件映射 `/platform/system/menu`） | 菜单树 Table，scope 筛选+图标选择器+菜单/按钮节点标签 | 增、删、改、查、图标选择 | ✅ | `platform.menu.{read,create,update,delete}` | 后端菜单→动态路由→`AuthGuarded` 按钮级权限（US-S1-07） |
| F4.11 | 权限管理入口 | 权限管理（重定向） | 权限管理（catalog，无独立路由；`ADM0000000048` 已 catalog 化） | `/platform/permission` | 权限管理页重定向至角色管理页 | 重定向 | 🚧 | 复用 `platform.role.*` | 仅 `<Navigate to="/platform/role">`，无独立权限界面 |
| F4.12 | 审计日志 / 登录日志（平台统一页） | 审计/登录日志统一页 | 日志审计 > 操作日志/登录日志（`PLATFORM-AUDIT-CATALOG` + `PLATFORM-OP-LOG-MENU`/`PLATFORM-LOGIN-LOG-MENU`，V202605210001）；统一页无菜单行 | `/platform/audit-logs`（统一页）、`/platform/log/operation-log`、`/platform/log/login-log`（独立页） | 审计/登录 Tabs 统一页+独立页双入口，分页+模块/操作者/关键词/时间筛选 | 查、筛选（登录按主体/门户/结果/客户端/IP） | ✅ | `platform.log.audit.read`、`platform.log.login.read` | 独立页与统一页功能重叠，目标态收敛至统一页；审计不可删除；导出 Todo |
| F4.13 | 全局屏蔽词 | 全局屏蔽词 | 屏蔽词库（`PLATFORM-BLOCKWORDS-MENU` `/platform/blockwords`，V202606080001，order 8 根级） | `/platform/blockwords` | 平台全局词库 CRUD，跨域生效 | 增、删、改、查 | ✅ | `platform.blocked_word.{read,create,delete}` | 跨域生效（`business_domain_id` 为空）；词条去首尾空格、禁空词（US-S2-05） |
| F4.14 | 事项配置（类型/属性/状态/模板） | 事项配置 | 无后端菜单行（主菜单 `/platform/ticket-config` 无迁移创建记录，疑为运行时菜单管理创建，待确认；子菜单 types/statuses/templates 依赖父行，V202606220002 等；templates 隐藏 V20260726092200） | `/platform/ticket-config/*` | 类型/属性/状态/模板配置，Formily 表单设计器+React Flow 状态流 DAG | 增删改查、草稿/发布/版本历史、属性插槽拖拽排序、默认值 JSON | ✅ | `platform.ticket_config.{attr,type,status,template}.*`（16 码） | TR-01 至少一个终态；系统属性锁定不可删（US-S3-01a）；前端内建路由（hideInMenu） |
| F4.15 | SLA 规则与工作日历 | SLA 管理 | 无菜单入口（`/platform/sla-management` 菜单 trim 删除未重建；前端内建路由） | `/platform/sla-management` | SLA 规则（首响/解决时限/违约动作）+工作日历 CRUD+计时引擎 | 增、删、改、查、违约动作配置 | ✅ | `domain.sla.{read,create,update,delete}`（平台侧复用） | SlaTimingEngine 已实现；未挂验收 Story（backlog E4 标占位与代码不符）；目标态：域级 SLA 迁 `/domain/settings/sla`，平台级保留 |
| F4.16 | 站内信（管理端） | 站内信 | 无菜单入口（trim 删除未重建；前端内建路由） | `/platform/inbox` | 管理端站内信列表/未读/已读 | 查、标为已读 | ✅ | `inbox.read`、`inbox.mark_read` | P0 inbox 契约（未拆 Story） |
| F4.17 | 附件上传（MinIO） | 附件上传 | 无菜单入口（trim 删除未重建；前端内建路由） | `/platform/attachments` | 附件上传（服务端代理→MinIO）、下载 | 上传、下载 | ✅ | `attachment.upload`、`attachment.download` | 依赖 MinIO 服务（外部依赖 ADR） |
| F4.18 | 用户导入导出 | 用户导入导出（占位） | 无菜单入口（`/platform/import-export` 菜单 trim 删除未重建） | `/platform/import-export` | 用户批量导入导出 | —（规划中） | 📅 | `platform.user.import`、`platform.user.offboard_pool.export` | 页面存在、API 待查；**2026-08-12 决策：目标态并入用户管理页工具栏操作** |
| F4.19 | 域配置 KV（平台侧） | 域配置 KV | 无菜单入口（trim 删除未重建；域详情「配置」Tab 同源） | `/platform/domain-config` | 业务域 KV 键值对配置（key/value/valueType/description） | 增、删、改、查 | ✅ | `domain.config.{read,update}`（平台侧复用，`platform.domain.control.config.*` 不存在） | 域详情「配置」Tab 同源双入口（见附录B） |
| F4.20 | 客户入域邀请码面板（平台侧） | 客户入域邀请码面板 | 无菜单入口（trim 删除未重建；域详情「客户入域」Tab 同源） | `/platform/domain-onboarding` | 平台侧邀请码 CRUD（列表/创建/删除/失效） | 增、删、失效 | 🚧 | `domain.invitation_code.{read,create,delete}`（平台侧复用） | CustomerWeb 接真实入域 API 见 US-S3-02（未完成）；域详情「客户入域」Tab 同源待确认 |
| F4.21 | 组织配置（平台侧） | 组织配置（占位） | 组织管理 > 组织配置（`ADM0000000071`） | `/platform/org-config` | 组织架构配置（占位） | —（规划中） | 📅 | `platform.organization.*` | Empty 占位「组织配置功能开发中」；**2026-08-12 决策：目标态与 F4.8 合并** |
| F4.22 | 模板遗留页面 | 模板演示页/异常页 | 无（demo 路由不受控） | `/access/*`、`/route-nest/*`、`/about`、`/outside/*` 等 | react-antd-admin 模板遗留演示页与异常页 | — | 🗑 | 无码 | 非业务功能；dev POC 路由未注册；目标态：异常页/公开页（隐私政策/服务条款）保留，demo 页移除 |

---

## 附录A：权限码速查（按功能编号索引）

> 本附录供开发查阅；主表已列典型码，完整定义以 `PermissionCodes.java` 与 Flyway 迁移为准。

| 编号 | 权限码 | 默认授权角色 | 来源 |
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
| F2.2 | `domain.sla.{read,create,update}` | agent / domain_admin | PermissionCodes.java:170-172（SLA 引擎 SlaController.java:40）；前端门控待功能任务补齐 |
| F2.3 | —（E5 未排期） | — | 无权限码（E5 未拆 Story） |
| F2.4 | `domain.home.read` | domain_admin / agent | Flyway V20260728170000（BUSINESS-HOME-MENU） |
| F3.1 | `platform.domain.control.ticket_type.{read,create,update,delete}`；域端 `domain.ticket_type.*` | platform_admin / super_admin（平台域详情）；domain_admin（域端） | Flyway V202606170001 + PermissionCodes.java:126 |
| F3.2 | `domain.sla.*`；`domain.notification_template.{read,update}` | domain_admin / super_admin | SLA：PermissionCodes.java:171 + SlaController.java:40；通知：PermissionCodes.java 注册，页面占位 |
| F3.3 | `domain.member.{read,create,update_roles,update_status,delete}`；`domain.customer.{read,create,update_status}`；`domain.role.*` | domain_admin / super_admin（域内置） | Flyway V202606060001 + US-S2-03/S2-04 |
| F3.4 | `domain.ticket_type.*`、`domain.ticket_attribute.*`、`domain.ticket_status.*` | domain_admin / super_admin | PermissionCodes.java:26-33（domain.ticket_* 族） |
| F3.5 | `domain.customer.{update,update_status,reset_password}` | domain_admin / super_admin | Flyway V20260806120000 + PermissionCodes.java:22 |
| F3.6 | `domain.invitation_code.*`；`domain.general.read` | domain_admin / super_admin | 代码（onboarding 页 auth 数组）+ US-S1-03 |
| F3.7 | `domain.general.{read,update,update_status}` | domain_admin / super_admin | 代码（域设置控制器）+ US-S2-01 域端版 |
| F3.8 | `domain.config.{read,update}` | domain_admin / super_admin | PermissionCodes.java:166 + domain/config/index.tsx:39 |
| F3.9 | 域端 `domain.blocked_word.*`（旧码保留）；平台域详情 `platform.domain.control.blocked_word.*` | domain_admin（域端）；platform_admin（平台域详情） | Flyway V202606080001（重命名）+ US-S2-05 |
| F3.10 | `domain.overview.read` | domain_admin / super_admin | AdminPermissionCatalog.java:27（S3+ 新增，无 Story） |
| F3.11 | `domain.notification_template.*` | domain_admin | 占位「菜单与权限已就绪」（E2/E4） |
| F3.12 | `domain.audit_log.read`、`domain.login_log.read` | domain_admin / super_admin | Flyway V202606090001 + US-S2-06 |
| F3.13 | `domain.role.{read,create,update,delete}` | domain_admin / super_admin | 代码（IAM 域级 API）+ US-S2-02 |
| F3.14 | `domain.menu.{read,create,update,delete}` | domain_admin / super_admin | 代码（IAM 域级 API）+ US-S2-E2-00 关联 |
| F3.15 | —（模板遗留） | — | 无权限码（模板遗留占位页） |
| F3.16 | —（模板遗留） | — | 无权限码（模板遗留占位页） |
| F4.1 | `platform.domain.list.read`、`platform.domain.create`、`platform.domain.control.{entry,overview,read,general.update,general.delete}` 等 | platform_admin / super_admin | Flyway V202605330004/V202605330005 + US-S1-02/S2-01 |
| F4.2 | `platform.ticket_config.template.*` | platform_admin | PermissionCodes.java + Flyway V20260726092200（权限已注册、菜单已隐藏） |
| F4.3 | `platform.user.{create,disable,offboard,restore,reset_password}`；`platform.user.offboard_pool.{read,export,batch_restore}` | platform_admin / super_admin | PermissionCodes.java:64 + AdminPermissionCatalog.java:107 + US-S1-07 |
| F4.4 | `platform.system_config.{read,update}` | platform_admin | PermissionCodes.java:168-169；前端门控待功能任务补齐 |
| F4.5 | —（认证豁免/公开） | — | 无权限码（登录公开；动态菜单/权限快照为登录后行为） |
| F4.6 | `platform.dashboard.read` | platform_admin | PermissionCodes.java:212；前端门控待功能任务补齐 |
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
| F4.18 | `platform.user.import`、`platform.user.offboard_pool.export` | platform_admin | 占位但权限已就绪（gap-research:89 确认） |
| F4.19 | `domain.config.{read,update}`（平台侧复用，`platform.domain.control.config.*` 不存在） | platform_admin / domain_admin | 复用 `domain.config.*`（DomainConfigController + PermissionCodes.java:166）；域详情「配置」Tab 同源 |
| F4.20 | `domain.invitation_code.{read,create,delete}`（平台侧复用） | platform_admin / super_admin | 代码（InvitationCodeController）+ US-S1-03/S1-06 |
| F4.21 | `platform.organization.*` | platform_admin | 占位但权限已就绪（inventory §2） |
| F4.22 | —（模板遗留） | — | 无权限码（模板遗留页，demo 路由不受控） |

---

## 附录B：多菜单入口 / 同源页面说明

| 功能编号 | 主承载页 | 其他入口/同源页面 | 说明 |
| :--- | :--- | :--- | :--- |
| F4.3 | 用户管理 `/platform/user` | 离职池 `/platform/offboard-pool` | 同一功能两个页面：员工管理主承载于用户管理页，离职池子功能独立页 |
| F4.19 | 业务域管理详情「配置」Tab | `/platform/domain-config` 独立页 | 同源双入口，API/权限复用 `domain.config.*` |
| F4.20 | 业务域管理详情「客户入域」Tab（待确认） | `/platform/domain-onboarding` 独立页 | 同源双入口 |
| F3.1 | 域事项配置 `/domain/ticket-config`（域端） | 平台域详情内 `/platform/domains/ticket-type-config/*`、`/platform/domains/ticket/form-design/*` | 同源承载，权限码双轨（域端 domain.ticket_type.* / 平台 platform.domain.control.ticket_type.*） |
| F3.4 | 域事项配置 `/domain/ticket-config` | 平台域详情内 `/platform/domains/ticket-type-attributes/*` | 属性/状态面板同源 |
| F3.2 | 域 SLA 规则页（目标态 `/domain/settings/sla`，SLA 部分主承载） | 平台 `/platform/sla-management`（平台级保留）、`/domain/settings/notifications`（通知模板部分） | 现状 SLA 复用平台页；2026-08-12 决策域端独立页；通知模板与 F3.11 语义重叠待澄清 |
| F4.12 | 审计/登录日志统一页 `/platform/audit-logs` | `/platform/log/operation-log`、`/platform/log/login-log` 独立页 | 双入口功能重叠，目标态收敛至统一页 |
| F1.5 | 通知中心 `/inbox`（目标态主入口） | 工单详情「确认解决后评价」辅入口（目标态） | 现状全站无入口 |
| F1.7 | 注册页 `/register`、`/d/:domainCode/register` | 业务域选择页 `/domains`（邀请码加入入口） | 邀请码加入为本地 mock 归 F1.7 |
| F2.1 | 工单队列/详情（目标态 `/ticket-queue`，页面未建） | `/platform/ticket-pool`、`/platform/ticket-detail`（P0 演示页临时承载） | US-S3-04 落地后迁移 |
| F3.3 | 域成员管理 `/domain/settings/members`、域客户管理 `/domain/customers/list`、域角色管理 `/domain/settings/roles` | 平台域详情「成员/客户/角色」Tab（同源双入口，API 复用、权限码 OR） | 目标态（08-11）集团统一管理，平台侧角色走模板下发 |
| F3.5 | 域客户管理 `/domain/customers/list` | 平台域详情「客户」Tab | 同源双入口（US-S2-04）；资料编辑/重置密码仅域端可写 |
| F3.9 | 域屏蔽词库 `/domain/settings/blockwords` | 平台域详情「屏蔽词」Tab | 同源 |
| F3.12 | 域级日志 `/domain/settings/audit-logs`、`/domain/settings/login-logs` | 平台域详情侧栏同源 | 同源 |
| F3.13 | 系统角色管理（域端）`/system/role` | 平台域详情「角色」Tab | 平台侧只读、域端可写 |
| F3.6 | 入域配置 `/domain/settings/onboarding` | 客户管理 > 入域配置 `/domain/customers/onboarding`；旧路由 `/domain/onboarding`（legacy 重定向） | 三入口同页 |
| F4.2 | 事项配置 templates 子页 `/platform/ticket-config/templates` | 事项配置页内 sider 进入（主侧栏菜单隐藏 V20260726092200） | 2026-08-12 决策沿用现状页 |

---

## 维护说明

1. **状态变更**：直接修改主表中对应行的“状态”列。
2. **新增功能**：在对应端侧分区追加一行，编号按规则递增（编号唯一来源 = prd.md §4.2）。
3. **权限/菜单变更**：修改该功能行的对应列，保持横向一致性（权限码权威 = `PermissionCodes.java` 与 Flyway 迁移，本表与附录A 为速查索引）。
4. **删除功能**：标记为 `🗑 已废弃` 并备注原因，保留编号用于历史追溯。
