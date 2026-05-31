# 文档总览

本目录用于维护 UnionDesk 的产品、架构、数据与运维文档。

## 仓库定位

- `UnionDesk`：后端服务
- `UnionDeskWeb`：前端应用，包含 `UnionDeskCustomerWeb` 和 `UnionDeskAdminWeb`

## 文档权威与阅读顺序

> Solo + Agent 协作：权威链写在本节；战略变更时再扩展索引文档。

**冲突时按下列层级裁决（编号越小优先级越高）：**

| 层级 | 文档 | 回答的问题 |
|:---|:---|:---|
| L1 | [`product/vision.md`](product/vision.md) | 方向、北极星、MVP 成功定义、非目标 |
| L2 | [`product/prd.md`](product/prd.md) | 做什么、角色、功能叙述、项目边界 |
| L3 | [`product/foundation-rules.md`](product/foundation-rules.md) | 身份、域、工单状态机；可测试 FR/DR/TR |
| L4 | [`architecture/data-model.md`](architecture/data-model.md) | 逻辑实体、命名与隔离约定 |
| L5 | [`architecture/database-increment-plan.md`](architecture/database-increment-plan.md) | 按 Sprint 新增/变更哪些表（不写 SQL） |
| L6 | [`product/backlog-stories.md`](product/backlog-stories.md) | User Story、AC、Sprint 承诺 |
| L7 | 代码与 Flyway | 实现层；落后于 L1–L6 时须登记偏差 |

**协作约定：**

- **迭代任务源**：[`product/backlog-stories.md`](product/backlog-stories.md)（当前承诺：**S2**，Committed 见 [`sprint-2-plan.md`](product/sprint-2-plan.md) §2，约 22 SP；S1 已签 off 2026-05-26）。
- **数据库**：按 Sprint 增量；Flyway/DDL 为 L7，不作为 L3 规则来源。
- **Agent 读文档顺序**：vision → prd → foundation-rules → backlog → 架构子文档。

## 文档结构

```
docs/
├── README.md                         # 本文档（含权威链）
├── product/
│   ├── vision.md
│   ├── prd.md
│   ├── foundation-rules.md
│   ├── backlog-epics.md
│   ├── backlog-stories.md            # L6 迭代任务源
│   ├── sprint-0-plan.md
│   ├── sprint-1-plan.md              # S1 执行计划（已签 off，2026-05-26）
│   ├── sprint-2-plan.md              # S2 执行计划（E2 + 平台域超额 + UX-01）
│   └── implementation-inventory.md
├── architecture/
│   ├── data-model.md
│   ├── database-increment-plan.md
│   └── adr-external-dependencies.md
└── operations/
    ├── deployment-guide.md
    └── backup-restore.md
```

## 维护规则

- 需求边界变化：L1 vision（仅战略级）→ L2 prd → L3 foundation → L6 backlog。
- 逻辑字段变更：L3 → L4 同步 → L5 登记 → L7 Flyway。
- 与代码出现偏差：登记 [`qa/implementation-traceability.md`](qa/implementation-traceability.md)，**不以 Flyway 脚本反写需求**。
- 历史文档：仓库根目录 `doc/` 为只读参考，**以 `docs/` 为准**。
- **迭代任务源**：L6 为 [`product/backlog-stories.md`](product/backlog-stories.md)；[`checklist/`](checklist/) 仅为 PRD/模块修订清单，**不**替代 Backlog。
