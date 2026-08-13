# Research: 54 功能节点「明细 + 权限码」覆盖矩阵与缺口清单

- **Query**: 检查整体 PRD 是否已落实到细节——每个功能节点（F1.x–F4.x）是否都具备①具体功能明细（页面行为/操作细节/校验规则/边界）②权限代码；产出逐节点覆盖矩阵 + 缺口清单 + 孤儿项检查
- **Scope**: 内部（prd.md V2.2 / feature-list.md v1.0 / implementation-inventory.md v1.3 / PermissionCodes.java / DomainConfigController / foundation-rules.md 交叉取证）
- **Date**: 2026-08-11

## 结论速览

1. **明细覆盖**：54 条中 完整 37（69%）、部分 10（19%）、缺失 7（13%，其中 4 条模板遗留/占位为预期）。唯一的 **P0 明细缺口 = F1.5 满意度评价**（全站无入口、无 Story、无操作细节）。
2. **权限码覆盖**：54 条中 有码 36（67%，管理端 42 条中 36 条已实现项 100% 有码）、部分 1（F2.2 推断）、无 17（客户端 12 + 模板遗留 3 + E5 未排期 1 + 登录豁免 1，全部为**预期无码**）。经本次代码验证，上轮研究标注的 6 个「推断/待确认」码中 5 个已确认存在（F3.2/F4.2/F4.4/F4.6/F4.19）。
3. **孤儿项**：清单↔详细设计 54/54 一一对应，**无孤儿**；§4.3 页面结构对 F1.5/F2.2/F2.3 三个规划中功能无承载行（合理），但 §4.4 自洽检验勾选「每项功能均有页面承载」与事实不符（轻微过度声明）。

---

## 1. 完整覆盖矩阵（54/54）

> 依据：feature-list.md §3/§4 功能说明列 + §7 操作矩阵 + §8 权限码对照表；prd.md §4.2/§4.3/§5；PermissionCodes.java（本次复核）；DomainConfigController（本次复核）。
> 明细覆盖：完整=有操作枚举+校验/边界；部分=有操作但缺细节或规划中；缺失=仅占位说明无操作细节。
> 权限码：有=精确实现码（点分）；部分=推断/待确认；无=认证豁免/模板遗留/未排期（预期，非缺口）。

### 1.1 M1 客户端（F1.1–F1.12，12 条）

| 编号 | 功能 | 明细覆盖 | 权限码 | 缺口描述 | 证据 |
| :--- | :--- | :--- | :--- | :--- | :--- |
| F1.1 | 提交工单（动态表单） | 部分 | 无（预期） | 三步提单+API 完整；动态表单字段未渲染、attachmentIds 空数组（实现缺口，明细已写清） | feature-list.md:116；prd.md §5.1.1 |
| F1.2 | 在线咨询 | 部分 | 无（预期） | 规划中 E5；语义（排队/接入/聊天/转工单）有、无操作细节（未拆 Story） | feature-list.md:117 |
| F1.3 | 反馈/建议（工单类型路径） | 完整 | 无（预期） | 预置类型+启用/停用决策清晰 | feature-list.md:118；US-S3-01 AC4 |
| F1.4 | 我的工单/咨询历史 | 完整 | 无（预期） | 列表筛选/详情时间线/补充/撤回（TR-03 open 态乐观锁）均详；咨询历史无、附件展示无（实现缺口已标注） | feature-list.md:119；prd.md §5.1.2 |
| F1.5 | 满意度评价 | **缺失** | 无（预期） | **P0 功能全站无入口**（grep 0 命中）、无 Story、无操作细节（星级/文字评价仅一句话）；唯一 P0 明细缺口 | feature-list.md:120 |
| F1.6 | 登录 | 完整 | 无（预期） | 滑块 challenge 5s、记住账号、专属域入口、新环境提醒；忘记密码占位已标注 | feature-list.md:121；prd.md §5.1.3 |
| F1.7 | 注册与入域 | 完整 | 无（预期） | 表单字段/开放域下拉/邀请码预填；mock 状态与 US-S3-02 明确 | feature-list.md:122 |
| F1.8 | 业务域选择与切换 | 完整 | 无（预期） | 三组卡片+switch-domain API+FR-05 | feature-list.md:123 |
| F1.9 | 服务首页 | 完整 | 无（预期） | 统计卡/最近 5 条/未读 3 条/jumpUrl | feature-list.md:124 |
| F1.10 | 站内信/通知中心 | 完整 | 无（预期） | 分类/未读/已读/jumpUrl | feature-list.md:125 |
| F1.11 | 个人中心 | 完整 | 无（预期） | 账号信息/退出登录真实 API；通知偏好占位已标注 | feature-list.md:126 |
| F1.12 | 修改密码 | 完整 | 无（预期） | ≥6 位/两次一致/不同当前；mustChangePassword 强制 | feature-list.md:127 |

### 1.2 M2 员工端（F2.1–F2.4，4 条）

| 编号 | 功能 | 明细覆盖 | 权限码 | 缺口描述 | 证据 |
| :--- | :--- | :--- | :--- | :--- | :--- |
| F2.1 | 工单队列与详情处理 | 完整 | 有 | `ticket.*` 11 码（read/create/view.self/view.domain_all/claim/assign/reply.self/reply/close/withdraw.self/merge）；明细含筛选/时间线/回复/备注/转派/状态/批量；business 端未成品（US-S3-04）已标注 | feature-list.md:164、:266、:332 |
| F2.2 | SLA 感知与高亮 | 部分 | 部分（推断） | 规划中 E4；`domain.sla.read` 为推断（domain.sla.* 族存在，PermissionCodes.java:171 附近）；依赖 F4.15 引擎未接入 UI | feature-list.md:165、:333 |
| F2.3 | 在线咨询工作台 | 部分 | 无（预期） | 规划中 E5 未拆 Story；语义（接入/撤回 2 分钟/转工单）有、无操作细节 | feature-list.md:166 |
| F2.4 | 业务域端首页/工作台 | 完整 | 有 | `domain.home.read`（PermissionCodes.java:13 + Flyway V20260728170000）；三元规则+FR-03 | feature-list.md:167、:335 |

### 1.3 M3 域管理后台（F3.1–F3.16，16 条）

| 编号 | 功能 | 明细覆盖 | 权限码 | 缺口描述 | 证据 |
| :--- | :--- | :--- | :--- | :--- | :--- |
| F3.1 | 工单类型设计 | 完整 | 有 | `platform.domain.control.ticket_type.{r,c,u,d}`（PermissionCodes.java:126-133）+ `domain.ticket_type.*`（:175-178）；TR-01 终态校验、系统字段锁定均详 | feature-list.md:168、:270、:336 |
| F3.2 | SLA 规则与通知模板 | 部分 | 有 | SLA 规则明细完整（复用 F4.15）；**通知模板为占位无操作细节**；码 `domain.notification_template.{read,update}` 本次确认存在（PermissionCodes.java:173-174） | feature-list.md:169、:271 |
| F3.3 | 成员/客户/角色管理 | 完整 | 有 | `domain.member.*`/`domain.customer.*`/`domain.role.*`；最后 admin 保护规则、重复添加校验均详 | feature-list.md:170、:272 |
| F3.4 | 事项属性与状态配置（域内） | 完整 | 有 | `domain.ticket_attribute.*`（PermissionCodes.java:26-29）+ `domain.ticket_status.*`（:30-33）本次确认存在（上轮标注「inventory 缺项待确认」可升格）；三面板/插槽/系统属性锁定详 | feature-list.md:171、:273 |
| F3.5 | 域客户管理增强 | 完整 | 有 | `domain.customer.{update,update_status,reset_password}`；查询/添加/导入/启停/编辑/重置密码/只读详情全列 | feature-list.md:172、:274 |
| F3.6 | 入域配置（域端） | 完整 | 有 | `domain.invitation_code.*` + `domain.general.read`；双开关+DR-01/02 | feature-list.md:173、:275 |
| F3.7 | 域基础设置 | 完整 | 有 | `domain.general.{read,update,update_status}`；code 不可改已标注 | feature-list.md:174、:276 |
| F3.8 | 域参数配置（KV） | 完整 | 有 | `domain.config.{read,update}`（PermissionCodes.java:166-167） | feature-list.md:175、:277 |
| F3.9 | 域屏蔽词库 | 完整 | 有 | 增删查/重复提示/去空格禁空词全；码有**重命名漂移**：旧 `domain.blocked_word.*` 保留，以 Flyway V202606080001 的 `platform.domain.control.blocked_word.*` 为准（feature-list §8 头注③已声明） | feature-list.md:176、:278、:344 |
| F3.10 | 域运营概览 | 部分 | 有 | `domain.overview.read`；明细=4 Statistic+趋势但统计值「—」（部分实现已标注，无 Story） | feature-list.md:177、:279 |
| F3.11 | 域通知配置 | 缺失（占位） | 有 | 占位页（「菜单与权限已就绪」）；码 `domain.notification_template.*` 存在；无操作明细（规划中，预期） | feature-list.md:178、:280、:346 |
| F3.12 | 域级操作日志/登录日志 | 完整 | 有 | `domain.audit_log.read` + `domain.login_log.read`；分页/筛选/审计不可删除 | feature-list.md:179、:281 |
| F3.13 | 系统角色管理（域端） | 完整 | 有 | `domain.role.{r,c,u,d}` + `domain.role.permission.{r,u}`（domain-permissions.ts 实证）；预置禁删/scope 一致 | feature-list.md:180、:282 |
| F3.14 | 系统菜单管理（域端） | 完整 | 有 | `domain.menu.*`；树 CRUD+scope=business 筛选 | feature-list.md:181、:283 |
| F3.15 | 系统用户管理（域端） | 缺失（模板遗留） | 无（预期） | 模板残留单 Input，非业务功能（明确标注） | feature-list.md:182 |
| F3.16 | 系统部门管理（域端） | 缺失（模板遗留） | 无（预期） | 计数器 demo，非业务功能（明确标注） | feature-list.md:183 |

### 1.4 M4 平台管理后台（F4.1–F4.22，22 条）

| 编号 | 功能 | 明细覆盖 | 权限码 | 缺口描述 | 证据 |
| :--- | :--- | :--- | :--- | :--- | :--- |
| F4.1 | 业务域创建与管理 | 完整 | 有 | `platform.domain.list.read`/`platform.domain.create`/`platform.domain.control.*`；列表/向导 Step3/详情 10 Tab/软删 code+Step-up 全详 | feature-list.md:137、:286 |
| F4.2 | 模板中心 | 部分 | 有 | 团队模板已实现但菜单隐藏（V20260726092200）；「域模板提炼」语义与实现待对齐；码 `platform.ticket_config.template.*` 本次确认存在（PermissionCodes.java:146-149） | feature-list.md:138、:287 |
| F4.3 | 员工账号与离职池 | 完整 | 有 | `platform.user.*` + `platform.user.offboard_pool.{read,export,batch_restore}`；列表/创建/离职/池/恢复/重置密码全列；CSV 导入未实现、永久删除无入口均标注 | feature-list.md:139、:288 |
| F4.4 | 系统设置与安全告警 | 部分 | 有 | 系统设置 KV read/update 有明细；**安全告警中心未实现**（inventory §5.3 Todo）；码 `platform.system_config.{read,update}` 本次确认存在（PermissionCodes.java:168-169） | feature-list.md:140、:289 |
| F4.5 | 平台端登录与动态菜单 | 完整 | 无（预期） | 滑块/三元规则/动态菜单/FR-03 全详；登录认证豁免 | feature-list.md:141、:290 |
| F4.6 | 平台首页仪表盘 | 部分 | 有 | Statistic+快捷+最近审计；数据为 DemoDataService mock（已标注 S1 待办）；码 `platform.dashboard.read` 本次确认存在（PermissionCodes.java:212） | feature-list.md:142、:291 |
| F4.7 | 用户管理 | 完整 | 有 | `platform.user.{read,create,update,disable,reset_password,restore,delete}`；搜索/部门侧栏/状态 Tag/离职二次确认/重置密码 16 位/永久删除无入口全详 | feature-list.md:143、:292 |
| F4.8 | 组织/部门管理 | 完整 | 有 | `platform.organization.{r,c,u,d}`；树 CRUD/子部门校验/循环引用检测 | feature-list.md:144、:293 |
| F4.9 | 角色管理 | 完整 | 有 | `platform.role.*` + `platform.role_permission.*` + `platform.role.bind`；scope 不一致拒绝+预置禁删 | feature-list.md:145、:294 |
| F4.10 | 菜单管理 | 完整 | 有 | `platform.menu.*`；树 Table/图标选择器/级联删除 | feature-list.md:146、:295 |
| F4.11 | 权限管理入口 | 部分 | 有 | 仅重定向 `/platform/role`（inventory §4.3 Partial）；码复用 `platform.role.*`（门控） | feature-list.md:147、:296、:362 |
| F4.12 | 审计日志/登录日志（平台统一页） | 完整 | 有 | `platform.log.audit.read` + `platform.log.login.read`；Tabs+独立页双入口/筛选/审计不可删除/导出 Todo | feature-list.md:148、:297 |
| F4.13 | 全局屏蔽词 | 完整 | 有 | `platform.blocked_word.{read,create,delete}`；CRUD/跨域生效/去空格禁空词 | feature-list.md:149、:298 |
| F4.14 | 事项配置 | 完整 | 有 | `platform.ticket_config.{attr,type,status,template}.*` 16 码（PermissionCodes.java:134-149 实证）；Formily/React Flow/TR-01/版本历史全详 | feature-list.md:150、:299 |
| F4.15 | SLA 规则与工作日历 | 完整 | 有 | `domain.sla.{r,c,u,d}`（平台侧复用，PermissionCodes.java:171 附近 + SlaController）；规则/日历 CRUD+计时引擎+违约动作；「未挂验收 Story」已标注 | feature-list.md:151、:300 |
| F4.16 | 站内信（管理端） | 完整 | 有 | `inbox.read` + `inbox.mark_read`；列表/未读/已读 | feature-list.md:152、:301 |
| F4.17 | 附件上传（MinIO） | 完整 | 有 | `attachment.upload` + `attachment.download`；服务端代理→MinIO+外部依赖 ADR | feature-list.md:153、:302 |
| F4.18 | 用户导入导出 | 缺失（占位） | 有 | 占位页（API 待查）；码 `platform.user.import`（PermissionCodes.java:59）+ `platform.user.offboard_pool.export`（:65）本次确认存在；无操作明细（规划中，预期） | feature-list.md:154、:303、:369 |
| F4.19 | 域配置 KV（平台侧） | 完整 | 有（已确认复用） | KV CRUD 明细完整；**码=复用 `domain.config.{read,update}`**（本次 DomainConfigController 实证：`/api/v1/admin/domains/{domainId}/config` 注解 DOMAIN_CONFIG_READ/UPDATE），无 `platform.domain.control.config.*` 专属码；feature-list §8 的「待确认」可升格为「已确认复用」 | feature-list.md:155、:304、:370；DomainConfigController.java:25,31 |
| F4.20 | 客户入域邀请码面板（平台侧） | 完整 | 有 | `domain.invitation_code.{read,create,delete}`（平台侧复用）；CRUD+失效+DR-02+US-S3-02 关联 | feature-list.md:156、:305 |
| F4.21 | 组织配置（平台侧） | 缺失（占位） | 有 | Empty 占位；码 `platform.organization.*` 存在（占位但权限已就绪）；无操作明细（规划中，预期） | feature-list.md:157、:306、:372 |
| F4.22 | 模板遗留页面 | 缺失（模板遗留） | 无（预期） | 模板演示页清单完整（access/route-nest/about/outside/personal-center + dev POC 未注册）；非业务功能 | feature-list.md:158 |

---

## 2. 统计汇总

| 维度 | 完整/有 | 部分 | 缺失/无 | 备注 |
| :--- | :--- | :--- | :--- | :--- |
| 明细覆盖 | 37（69%） | 10（19%） | 7（13%） | 7 条缺失中 4 条模板遗留/占位为预期；**仅 F1.5 为 P0 非预期缺口** |
| 权限码 | 36（67%） | 1（2%） | 17（31%） | 有码 36 条 = 管理端已实现项全覆盖；无码 17 条全为预期（客户端 12 + 模板遗留 3 + E5 未排期 1 + 登录豁免 1） |
| 管理端口径 | 有码 36/42（86%） | 1（F2.2） | 5（预期） | 管理端已实现项 100% 有码 |

## 3. 缺口清单（按优先级）

### P0（MVP 必做，需主代理决策）

| # | 功能 | 缺口 | 现状证据 | 建议方向（供决策，非本 agent 职责） |
| :--- | :--- | :--- | :--- | :--- |
| 1 | **F1.5 满意度评价** | 明细缺失：全站无入口（grep 0 命中）、无 Story、无操作细节；P0 但完全未落地 | feature-list.md:120；prd.md §5.1.2 仅一句话 | 是否保留 P0？若保留需补 Story+AC+入口设计；若降级需同步 prd §4.2 优先级 |
| 2 | **F2.2 SLA 感知与高亮** | 明细部分：E4 Stretch 未排期；权限码 `domain.sla.read` 为推断（码族存在但 F2.2 未实现，绑定未验证） | feature-list.md:165、:333 | 依赖 F4.15 引擎（已实现），排期或明确延后 |

### P1（非阻塞，文档可立即修正）

| # | 功能 | 缺口 | 现状证据 |
| :--- | :--- | :--- | :--- |
| 3 | F4.19 域配置 KV（平台侧） | feature-list §8 标「待确认」——**本次已确认复用 `domain.config.*`**（DomainConfigController 注解实证），文档可升格 | DomainConfigController.java:25,31；PermissionCodes.java:166-167 |
| 4 | F3.2 通知模板 / F4.2 模板中心 / F4.4 安全告警 / F4.6 仪表盘 / F4.18 导入导出 | 上轮 6 个「推断」码中 5 个已确认存在（notification_template/template/system_config/dashboard/import-export），文档来源列可补 PermissionCodes.java 行号 | PermissionCodes.java:59,65,146-149,168-169,173-174,212 |
| 5 | F3.4 域内属性/状态码 | 上轮标「inventory 缺项，待确认精确迁移」——`domain.ticket_attribute.*`/`domain.ticket_status.*` 已确认存在（PermissionCodes.java:26-33），可升格 | PermissionCodes.java:26-33 |

### 非缺口但需知晓（漂移/一致性提示）

- **F3.9 重命名漂移**：`domain.blocked_word.*` 旧码保留 vs Flyway 新码 `platform.domain.control.blocked_word.*`（feature-list §8 头注③已声明，追踪即可）。
- **§4.4 自洽检验过度声明**：prd.md §4.4 勾选「功能清单中每项功能均有页面/模块承载」，但 F1.5/F2.2/F2.3 三个规划中功能在 §4.3 无承载行（合理存在，但勾选与事实不符，建议改为「规划中功能已在清单标注无页面」）。

## 4. 孤儿项检查（三方一致性）

### 4.1 清单 ↔ 详细设计（prd §4.2 ↔ §5）

- **结果：54/54 一一对应，无孤儿项**。§5.1.1–5.1.5（F1.1–F1.12）、§5.2.1–5.2.3（F2.1–F2.4）、§5.3.1–5.3.3（F3.1–F3.16）、§5.4.1–5.4.5（F4.1–F4.22）全部覆盖；无「有清单无设计」「有设计无清单」。
- prd §4.2 与 feature-list §2 的 54 条编号、名称、优先级、实现状态逐条比对**全部一致**。

### 4.2 页面结构（§4.3）↔ 功能编号

- **承载完整**：客户端 11 行覆盖 F1.1–F1.12；管理端-业务域端 12 行覆盖 F2.4 + F3.1–F3.16；管理端-平台端 17 行覆盖 F4.1–F4.22（含 F2.1 演示页 `/platform/ticket-pool`、F3.1/F3.4 域详情内双入口）。
- **无承载行（3 个，均为规划中功能，属合理）**：F1.5（满意度评价）、F2.2（SLA 感知）、F2.3（在线咨询工作台）——§4.3 未列，feature-list §7 矩阵对应行标注「—/规划中」。
- **双入口交叉均已标注**：F3.1/F3.4（域端 + 平台域详情）、F3.3/F3.5（域端 + 平台域详情 Tab）、F3.9/F3.12/F4.19（域端 + 平台域详情侧栏同源）、F2.1（端侧归属业务域端但演示页在平台端）。

### 4.3 一致性结论

三方（清单/详细设计/页面结构）**整体一致，无孤儿项**；唯一修正点：§4.4 自洽检验的 [x] 勾选应注明「3 个规划中功能（F1.5/F2.2/F2.3）无页面承载，已标注」。

---

## Caveats / Not Found

- 客户端后端 API 门控（是否纯 RequireSession）未逐接口核实，沿用上轮推断（CustomerWeb 前端 0 权限引用 + feature-list 备注）。
- `ticket.*` 11 码的 agent 角色绑定仍待 US-S3-04 联调确认（F2.1 权限码"有"但默认授权角色为推断）。
- F2.2 的 `domain.sla.read` 未逐行核对 PermissionCodes.java（domain.sla.* 族在 :171 附近已由上轮研究确认），本次未重复打开该文件核对 read 常量——如需精确行号可补查。
- backlog-stories.md / backlog-epics.md 未逐 Story 展开核对（story-inventory.md 已覆盖），本报告聚焦 feature-list/prd 快照层面。
