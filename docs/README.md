# 文档总览

本目录用于维护 UnionDesk 的产品、架构、数据与运维文档。

## 仓库定位

- `UnionDesk`：后端服务
- `UnionDeskWeb`：前端应用，包含 `UnionDeskCustomerWeb` 和 `UnionDeskAdminWeb`

## 文档结构

```
docs/
├── README.md                         # 本文档总览
├── product/                          # 产品层 - 业务视角
│   └── prd.md                        # 产品需求文档，定义业务规则与全局约束
├── architecture/                     # 架构层 - 技术决策视角
│   └── data-model.md                 # 数据库设计文档，完整数据字典
└── operations/                       # 运维层 - 部署视角
    ├── deployment-guide.md           # 部署手册，含 Flyway 迁移与可观测性
    └── backup-restore.md             # 备份恢复手册
```

## 维护规则

- 需求边界变化时，优先同步 `product/prd.md`、`architecture/data-model.md`。
- 与代码实现出现偏差时，以当前代码实现和 Flyway 迁移事实为准，并在文档里说明差异。
