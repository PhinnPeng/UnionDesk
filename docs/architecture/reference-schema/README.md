# 联调库参考快照（非 Flyway）

> **US-S0-07**：只读参考，Flyway **不**加载此目录。权威迁移链见 `UnionDesk/src/main/resources/db/migration/current/`。

## 用途

- 对照联调库与 Git 中 Flyway 脚本链是否 drift
- 新成员快速理解「当前联调库长什么样」
- **不是** rebaseline 产物；不替代 `V202605200002` 及后续增量

## 当前快照

| 日期 | 文件 | Flyway 最大 version |
|:---|:---|:---|
| 2026-05-25 | [`uniondesk_baseline_20260525.sql`](./uniondesk_baseline_20260525.sql) | `202605250001` |

## 生成

迁移前须先全库备份（见 [`backup-restore.md`](../../operations/backup-restore.md) §1.1.1）：

```powershell
# Windows
cd UnionDesk
.\scripts\backup-db.ps1
```

```bash
# macOS / Linux / Git Bash
cd UnionDesk
./scripts/backup-db.sh
```

参考快照：

```powershell
# Windows
cd UnionDesk
.\scripts\export-baseline-reference.ps1
```

```bash
# macOS / Linux
cd UnionDesk
./scripts/export-baseline-reference.sh
```

产出路径：`uniondesk_baseline_YYYYMMDD.sql`（可 commit，无凭据）。实现：[`GenerateBaselineReference.java`](../../../UnionDesk/scripts/GenerateBaselineReference.java)，密码读 `UNIONDESK_DB_PASSWORD`。

## volatile 表（仅 schema，不导出数据）

与 `GenerateBaselineReference.EXCLUDE_DATA` 一致，含：`auth_login_log`、`auth_login_session`、`login_log`、`audit_log`、`operation_log`、`notification_log`、`ticket_event_log`、`ticket_history`、`inbox_message`、`file_attachment` 等；**不含** `flyway_schema_history` 表定义。

## 登记

每次导出后更新 [`database-increment-plan.md`](../database-increment-plan.md) §2.1（日期、Flyway 最大 version、drift 结论）。
