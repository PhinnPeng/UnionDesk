# 备份恢复手册（UnionDesk）

---

## 1. 备份策略

### 1.1 数据库备份

- 使用 MySQL 官方备份工具（mysqldump / XtraBackup）进行定期全量备份。
- P0 阶段提供备份脚本，支持定时执行。
- 备份文件需异地存储，与生产环境物理隔离。

#### 1.1.1 开发 / 联调：Flyway 或 DDL 试跑前备份（US-S0-07）

在**联调库**（`127.0.0.1:30306/uniondesk`）上执行下列操作 **之前**，须先全库备份：

- `flyway:migrate` / `flyway:validate` 试跑
- 手工 DDL 或数据修复
- 导出基线参考快照（若与 migrate 同日，**先备份再导出**）

```powershell
New-Item -ItemType Directory -Force -Path UnionDesk/backups
$ts = Get-Date -Format "yyyyMMdd_HHmmss"
# 密码勿写入仓库；可用环境变量 UNIONDESK_DB_PASSWORD 或交互 -p
mysqldump -h 127.0.0.1 -P 30306 -u uniondesk_app -p `
  --single-transaction --routines --triggers `
  uniondesk > "UnionDesk/backups/uniondesk_$ts.sql"
```

恢复（仅开发/联调，会覆盖当前库数据）：

```powershell
mysql -h 127.0.0.1 -P 30306 -u uniondesk_app -p uniondesk `
  < UnionDesk/backups/uniondesk_YYYYMMDD_HHmmss.sql
```

> **说明**：自动化脚本见 `UnionDesk/scripts/backup-db.ps1`（Windows）、`backup-db.sh`（Unix）；无 `mysqldump` 时使用 JDBC `DbBackup.java`。

详见 [`backlog-stories.md`](../product/backlog-stories.md) US-S0-07、[`database-increment-plan.md`](../architecture/database-increment-plan.md) §1.2。

### 1.2 文件备份

- 本地存储根目录 `storage.local_root` 必须纳入备份范围。
- 对象存储模式（MinIO/S3）依赖云存储自身冗余机制，本地仅保留配置。

### 1.3 配置备份

- 环境配置、密钥等通过配置中心 / Vault 管理，随版本控制。

---

## 2. 恢复流程

### 2.1 数据库恢复

1. 停止应用服务，确保无新写入。
2. 使用全量备份文件恢复数据库。
3. 按 Flyway 迁移脚本顺序执行增量恢复。
4. 验证数据完整性（核心表行数、关键记录校验）。

### 2.2 文件恢复

1. 从备份存储恢复本地文件目录。
2. 验证附件与业务记录的引用完整性。

### 2.3 全量恢复验证

- P0 交付前完成一次完整的恢复流程验证。
- 记录实际 RTO（恢复时间目标）与 RPO（恢复点目标）。

---

## 3. 运维 Gate

- 数据库主从部署作为生产上线建议项，不阻塞 P0。
- 季度恢复演练作为生产上线后的运维 Gate，需定期执行。
- 演练结果记录至审计日志。
