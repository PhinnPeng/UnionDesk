# UnionDesk 运维与联调脚本

本目录仅保留 **文档引用** 或 **Sprint 计划约定** 的脚本；一次性联调/修复脚本不入库。

## 保留清单

| 脚本 | 用途 | 权威文档 |
|:---|:---|:---|
| `backup-db.ps1` / `backup-db.sh` | 联调库全库备份（mysqldump 或 JDBC 回退） | [`docs/operations/backup-restore.md`](../../docs/operations/backup-restore.md) |
| `export-baseline-reference.ps1` / `.sh` | 导出参考 schema 快照至 `docs/architecture/reference-schema/` | [`reference-schema/README.md`](../../docs/architecture/reference-schema/README.md) |
| `check-flyway.ps1` | Flyway history 与 `current/` 脚本一致性检查 | US-S0-07 / [`database-increment-plan.md`](../../docs/architecture/database-increment-plan.md) |
| `run-with-jrebel.ps1` | JRebel Agent 启动 Spring Boot | [`UnionDesk/README.md`](../README.md) §JRebel |
| `DbBackup.java` | JDBC 全库备份实现 | 由 `backup-db.*` 调用 |
| `GenerateBaselineReference.java` | JDBC 参考快照实现 | 由 `export-baseline-reference.*` 调用 |
| `DbFlywayCheck.java` | Flyway 核查实现 | 由 `check-flyway.ps1` 调用 |

## 环境变量

- `UNIONDESK_DB_PASSWORD`：数据库密码（未设置时从 `application.yml` 读取，勿写入仓库）
- `JREBEL_HOME`：JRebel Agent 安装目录（仅 `run-with-jrebel.ps1`）

## 常用命令

```powershell
cd UnionDesk

# 迁移前备份
.\scripts\backup-db.ps1

# Flyway 与 current/ 对齐检查
.\scripts\check-flyway.ps1

# JRebel 热更新启动
$env:JREBEL_HOME = "C:\jrebel"
.\scripts\run-with-jrebel.ps1
```

## 不入库

- `scripts/*.class`：Java 辅助类编译产物
- `target/backup-classes/`、`target/cp.txt`：脚本运行时临时目录
