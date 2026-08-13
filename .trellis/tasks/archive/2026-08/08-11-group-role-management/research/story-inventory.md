# Research: Backlog Story 编号抽取（US-Sx-xx）

- **Query**: 从 `docs/product/backlog-stories.md` 抽取所有 US-Sx-xx 编号及标题，按 Epic/功能域归类，供 feature-list 的「关联 Epic/Story」列引用
- **Scope**: internal（文档抽取）
- **Date**: 2026-08-11

## Findings

### Sprint 0 — E0 项目奠基（全部 Done）

| Story | 标题 | 功能域 |
|:---|:---|:---|
| US-S0-01 | 管理端实现盘点 | 文档/盘点 |
| US-S0-02 | Backlog 骨架 | 文档 |
| US-S0-03 | 文档权威链 | 文档 |
| US-S0-04 | 联调环境说明 | 工程 |
| US-S0-05 | increment-plan 与口径对齐 | 文档/DB |
| US-S0-06 | 外部依赖 ADR | 工程/依赖 |
| US-S0-07 | 数据库基线快照与迁移备份 | 工程/DB |

### Sprint 1 — E1 平台端（S1-04/05/08 暂缓）

| Story | 标题 | 状态 | 功能域 |
|:---|:---|:---|:---|
| US-S1-00a | JRebel Maven 热更新 | Done | 工程 |
| US-S1-00b | 消除 Security 默认密码日志 | Done | 工程/安全 |
| US-S1-01 | 平台登录与动态菜单 | Done | 认证/IAM |
| US-S1-02 | 创建业务域与 bootstrap | Done | 业务域 |
| US-S1-03 | 入域双配置 CRUD | Done | 业务域/入域 |
| US-S1-04 | 客户注册 API | Todo（并入 US-S3-02） | 客户端入域 |
| US-S1-05 | CustomerWeb 接真实注册/入域 API | Todo（并入 US-S3-02） | 客户端入域 |
| US-S1-06 | 域内客户手动添加 | Done | 域客户 |
| US-S1-07 | IAM 角色/权限/按钮 | Done | IAM |
| US-S1-08 | 跨域访问拒绝 | Todo（S1 暂缓 → S4+） | 鉴权 |
| US-S1-09 | 登录日志与操作日志（管理端） | Done | 审计 |

### Sprint 2 — E2 业务域端 + 平台域详情超额 + UX（Committed 全部 Done）

| Story | 标题 | 状态 | 功能域 |
|:---|:---|:---|:---|
| US-S2-UX-01 | 登录滑块验证体验优化 | Done | 认证 UX（E6） |
| US-S2-01 | 业务域基础信息与安全删除 | Done | 业务域（平台侧 E1 超额） |
| US-S2-02 | 角色管理（只读） | Done | 域角色（平台侧） |
| US-S2-03 | 域内员工管理 | Done | 域成员（平台侧） |
| US-S2-04 | 域内客户管理完善 | Done | 域客户（平台侧） |
| US-S2-05 | 双层屏蔽词库（平台全局+域内） | Done | 屏蔽词 |
| US-S2-06 | 域内业务日志 | Done | 审计 |
| US-S2-E2-00 | 业务域端最小可达 | Done | 业务域端（E2 主路径） |
| US-S2-E2-01 | 工单类型设计（Stretch） | Done（US-S3-01 承接） | 工单类型 |

### Sprint 3 — E3 工单最小闭环 + E6 治理（S3-02~04 Todo）

| Story | 标题 | 状态 | 功能域 |
|:---|:---|:---|:---|
| US-S3-00 | IAM 角色—控制台绑定对齐 | Done | IAM 治理（E6） |
| US-S3-01 | 工单类型与模板配置（F3.1） | Done | 工单类型/表单设计 |
| US-S3-01a | 平台事项类型属性插槽增强 | Done | 工单属性 |
| US-S3-02 | 客户注册与 CustomerWeb 入域 | Todo | 客户端入域（E3） |
| US-S3-03 | CustomerWeb 提单与我的工单 | Todo（代码已基本实现，联调/验收待办） | 客户端工单（E3） |
| US-S3-04 | 员工端工单队列与处理 | Todo | 员工工作台（E3） |

### Stretch / 占位（不纳入当前 Sprint 签 off）

| Story | 标题 | 功能域 |
|:---|:---|:---|
| US-S3-E4-01/02 | SLA UI | SLA（E4） |
| US-S3-UX-02/03 | 审计语义/日志收敛 | 审计 |
| US-S1-08 | 跨域访问拒绝 | 鉴权 |
| US-S2-01 AC4 | 已删域直链行为 | 业务域 |
| E5 | 在线咨询运行时 | 咨询 |

### Epic 维度归类（供 feature-list「关联 Epic/Story」列）

| 功能域 | 关联 Epic | 代表 Story |
|:---|:---|:---|
| 客户端-登录/会话 | E1/E3 | US-S1-01、S2-UX-01、S3-02 |
| 客户端-注册/入域 | E3 | US-S3-02（并入 S1-04/05） |
| 客户端-提单/我的工单 | E3 | US-S3-03 |
| 客户端-在线咨询 | E5 | 未拆 Story |
| 员工端-工单队列/处理 | E3 | US-S3-04 |
| 员工端-咨询工作台 | E5 | 未拆 Story |
| 域后台-工单类型/表单 | E2/E3 | US-S3-01、S3-01a（承接 S2-E2-01） |
| 域后台-SLA/通知模板 | E2/E4 | US-S3-E4-01/02（Stretch） |
| 域后台-成员/客户/角色 | E2 | US-S2-03/04/02（平台侧）、E2 域端 |
| 平台-业务域管理 | E1 | US-S1-02/03、S2-01 |
| 平台-IAM（用户/角色/菜单/部门/离职池） | E1 | US-S1-07、S1-01 |
| 平台-审计/登录日志 | E1/E6 | US-S1-09、S2-06 |
| 平台-屏蔽词 | E1/E6 | US-S2-05 |
| 平台-导入导出/附件 | E1/E6 | 未拆 Story（inventory §4.1/附件 P0） |
| 治理横切（step-up/审计不可删/登录日志统一） | E6 | US-S1-09、S3-00、S2-01 AC2 |

## Caveats / Not Found

- 编号体系：`US-S0-xx`（E0）、`US-S1-xx`（E1，含 S1-00a/00b 子项）、`US-S2-xx` + `US-S2-E2-00/01`、`US-S3-xx` + `US-S3-01a` + `US-S3-E4-xx` 占位、`US-S2-UX-01`（E6）
- 文档版本 v2.1（2026-06-17）后代码新增（SLA 管理、domain/ticket-config、客户编辑/重置密码等）无对应 Story，feature-list 引用时备注「无 Story」
- backlog-stories 未登记 S3 之后的 Sprint 计划（Sprint 4 及以后为占位）
