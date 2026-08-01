# Backend Development Guidelines

> UnionDesk 后端开发规范索引。

---

## Overview

本目录约定来自 `AGENTS.md`、`UnionDesk/README.md` 与代码库实际模式（2026-07-10 Bootstrap）。

---

## Guidelines Index

| Guide | Description | Status |
|-------|-------------|--------|
| [Directory Structure](./directory-structure.md) | Maven 模块与五层包结构 | ✅ Filled |
| [Database Guidelines](./database-guidelines.md) | Flyway、MyBatis、无 FK | ✅ Filled |
| [Error Handling](./error-handling.md) | ApiResponse、异常抛出 | ✅ Filled |
| [Quality Guidelines](./quality-guidelines.md) | 测试、审查清单 | ✅ Filled |
| [Logging Guidelines](./logging-guidelines.md) | Logback、审计 vs 日志 | ✅ Filled |

---

## Pre-Development Checklist

开始写后端代码前：

- [ ] 确认功能归属模块（common/iam/support/domain/ticket/app）
- [ ] 阅读 [Directory Structure](./directory-structure.md) 确定文件落位
- [ ] 若涉及表结构，阅读 [Database Guidelines](./database-guidelines.md)（无 FK、Flyway 命名）
- [ ] 若涉及 API 错误，阅读 [Error Handling](./error-handling.md)
- [ ] 对照 `AGENTS.md` §3（MyBatis、无外键）

---

## Quality Check

完成实现后：

- [ ] `.\mvnw.cmd test` 通过（测试在 `uniondesk-app`）
- [ ] 新迁移在 `db/migration/current/`，命名 `V{datetime}__desc.sql`
- [ ] Controller 有 `@RequirePermission`（如适用）
- [ ] 业务错误消息为中文
- [ ] 未在 feature 模块添加 test 源集

---

## 权威参考

- `UnionDesk/README.md` — 模块与授权模型
- `AGENTS.md` §3 — 后端简要规范
- `docs/` — 产品与架构文档（非 `doc/` 历史目录）
