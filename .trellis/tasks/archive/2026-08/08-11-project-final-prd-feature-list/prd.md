# 项目最终 PRD 完善与双端功能清单

## Goal

1. 完善项目最终 PRD：`docs/product/prd.md` 原地升级 V2.2（结构定稿、消除与实现的脱节、待确认项标注回填时点）
2. 完善项目最终功能清单：全量口径（已实现 S0–S3 + 规划中 E4/E5/P1/P2），每项标注实现状态
3. 产出客户端、管理端双端功能清单表及功能清单说明：新增 `docs/product/feature-list.md`

## Background（已确认事实）

- `docs/` 为权威链（L1–L7，见 `docs/README.md`）；`prd.md` 现为 V2.1（2026-06-04），结构符合 PRD 模板，但存在：待确认项（§2.4 指标 ×4、§3.4 漏斗 ×5、§6.1 埋点、§6.4 筹备）、功能清单按四端划分（M1–M4）、与实现事实脱节
- 实现事实：双应用 —— `UnionDeskCustomerWeb`（客户端，13 个页面）+ `UnionDeskAdminWeb`（管理端，内部分「平台端 `/platform/*`」与「业务域端 根级」两轨，见 `backlog-epics.md` §1.2）；`implementation-inventory.md` v1.3 盘点平台端为主（36 Done / 11 Partial / 5 Todo），域端 §7 待补，客户端无盘点文档
- `backlog-epics.md`：E0–E6，S3（E3 工单闭环）编码中；`vision.md` 定义 MVP 与非目标
- `docs/checklist/product/prd.md`：PRD 审查结论为 `Return for Fixes`（R1–R5 流程任务未启动）——本任务不承接该流程清单，但 V2.2 修订方向与其建议一致（结构补齐、验收可追踪）

## Requirements

- R1 `prd.md` 升级 V2.2：版本说明新增一行（保留 V1.0/V2.1 历史）；§4.2 功能清单改双端口径并扩展全量功能（每项带「端侧」列）；§4.3 页面结构与实现一致；§5 详细设计与功能编号一一对应；§4.4 自洽检验全部勾选；待确认项标注「验收期回填」；§6.1 埋点给命名规范占位（如 `ticket_submit`）
- R2 新建 `feature-list.md`：最终功能清单（全量+状态）+ 客户端表 + 管理端表（平台端/业务域端分轨）+ 功能清单说明；表列固定 10 列：`功能编号 | 端侧 | 模块 | 功能 | 功能说明 | 优先级 | 实现状态 | 对应页面/路由 | 关联 Epic/Story | 备注`
- R3 编号体系唯一：沿用 PRD M1–M4 模块 + F1.x–F4.x 编号并扩展新增条目；`feature-list.md` 复用同一编号（两文档双向可追踪）
- R4 实现状态判定证据链：AdminWeb 以 `implementation-inventory.md` 为基准 + pages 目录交叉验证；CustomerWeb 无盘点 → 代码勘察（login/register/home/domains/tickets/chat/inbox/me/change-password），联调状态未知项标「部分，待联调确认」；规划项以 `backlog-epics.md` E4/E5 与 `prd.md` §2.6 为准；状态术语与 inventory 对齐（已实现=Done、部分=Partial、规划中=Todo/未排期）
- R5 `docs/README.md` 文档结构登记 `feature-list.md`，引用有效
- R6 不改变任何功能定义、业务规则、验收标准实质（prd-review-optimizer 边界）；指标数值不虚构

## Decisions（Brainstorm 2026-08-11 确认）

- D1 功能清单范围口径：**全量口径**，每项带「实现状态」列（已实现/部分/规划中）
- D2 双端划分：**客户端 = CustomerWeb；管理端 = AdminWeb**（管理端表内按「平台端 / 业务域端」分轨）
- D3 落盘：`prd.md` 原地升级 V2.2 + 新增 `docs/product/feature-list.md`；新 Trellis 任务 `08-11-project-final-prd-feature-list` 承载（独立于 login 任务）
- D4 待确认项处理：保留并标注「验收期回填」；埋点 Code 给命名规范占位，具体值后续统一
- D5 表结构：完整 10 列 + 页面功能级粒度（按钮级细节并入功能说明）
- D6 单任务承载：三产出共享编号体系与证据链，拆分无独立价值，不建父/子任务
- D7 集团管理目标态（2026-08-11 追加，替代方案 D「平台只读」）：职责矩阵更新为「平台统一管理（用户/角色模板/跨域批量停用）+ 域内实例微调（锁定字段约束）」；详见任务 `08-11-group-role-management`（研究：research/group-role-management-model.md）

## Acceptance Criteria

- [ ] AC1 `prd.md` V2.2 定稿：版本行新增；§4.2 功能清单双端全量（带端侧列）；§4.3 与实现一致；§5 与编号一一对应；自洽检验全勾选；待确认项已标注回填时点
- [ ] AC2 `feature-list.md` 产出：最终功能清单 + 客户端表 + 管理端表（平台/域分组）+ 功能清单说明；每行 10 列齐全
- [ ] AC3 追踪闭环：feature-list 编号与 prd.md 编号一致；页面/路由可反查；Epic/Story 引用可溯源
- [ ] AC4 `docs/README.md` 已登记新文档；文档内部引用路径有效
- [ ] AC5 git diff 证明未改变功能定义/业务规则/验收标准实质（改动文件：`docs/product/prd.md`、`docs/product/feature-list.md`、`docs/product/foundation-rules.md`、`docs/product/backlog-epics.md`、`docs/product/backlog-stories.md`、`docs/architecture/data-model.md`；`docs/README.md` 已登记完成）
- [ ] AC6 文件 UTF-8（`pnpm --dir UnionDeskWeb run check:utf8` 通过）
- [ ] AC7 集团管理目标态文档对齐：foundation-rules 新增 FR-07（统一角色模板+域实例+锁定字段）；prd §4.1.3 增补职责矩阵；feature-list F3.3/F3.5/F3.13/F4.1/F4.7 备注与 §8 双轨说明更新；backlog-epics §8.0 补重叠条件 + US-S1-08 提级 P0；data-model 双轨关系文档化；backlog-stories US-S1-08 标注提级

## Out of Scope

- 指标数值回填（验收期进行）
- `docs/checklist/product/prd.md` R1–R5 流程任务
- 任何实现编码任务（本任务仅文档）
- 双端之外的端（如移动端独立 App）

## Technical Notes

- 改动文件：`docs/product/prd.md`、`docs/product/feature-list.md`（新建）、`docs/README.md`
- 详细设计见 design.md；执行计划见 implement.md
