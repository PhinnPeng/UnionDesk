# 部署手册（UnionDesk）

---

## 1. 环境规划

### 1.1 环境分层

- `dev`、`test`、`staging`、`prod`

### 1.2 部署方式

- 后端容器化部署。
- 本期前端静态资源与附件优先通过源站 / 对象存储直链访问，预留 CDN 接入能力。
- 全链路 HTTPS。

### 1.3 配置与密钥

- 配置按环境隔离。
- 密钥统一托管（Vault/KMS 类服务）。
- 禁止本地文件存生产密钥。

---

## 2. Flyway 数据库迁移

### 2.1 前置条件

由于部分 migration 脚本包含触发器和函数定义，需要设置 MySQL 全局参数 `log_bin_trust_function_creators`。

**方式一：使用 root 用户执行配置脚本（推荐）**

```bash
# 进入项目目录
cd UnionDesk

# 使用 root 用户执行配置脚本
mysql -u root -p < src/main/resources/db/init-mysql-config.sql
```

**方式二：在 MySQL 命令行中手动设置**

```sql
SET GLOBAL log_bin_trust_function_creators = 1;
SELECT @@GLOBAL.log_bin_trust_function_creators;
```

**方式三：修改 MySQL 配置文件（永久生效）**

在 `my.cnf` 或 `my.ini` 文件的 `[mysqld]` 部分添加：

```ini
[mysqld]
log_bin_trust_function_creators = 1
```

### 2.2 数据库和用户准备

```sql
CREATE DATABASE IF NOT EXISTS uniondesk CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'uniondesk'@'localhost' IDENTIFIED BY 'uniondesk';
GRANT ALL PRIVILEGES ON uniondesk.* TO 'uniondesk'@'localhost';
FLUSH PRIVILEGES;
```

### 2.3 迁移目录

- 活跃迁移目录：`UnionDesk/src/main/resources/db/migration/current/`
- 历史脚本归档：`UnionDesk/src/main/resources/db/migration/archive/`
- 当前重基线文件：`V202605200002__rebaseline_current_schema.sql`

### 2.4 执行迁移

**方式一：使用 Maven 命令（推荐）**

```bash
# 进入后端项目目录
cd UnionDesk

# 清空数据库（仅开发环境，生产环境禁用）
.\mvnw.cmd flyway:clean

# 执行迁移（只扫描 current 目录）
.\mvnw.cmd flyway:migrate

# 查看迁移历史
.\mvnw.cmd flyway:info
```

**方式二：应用启动时自动迁移**

应用启动时会自动执行 Flyway 迁移（`application.yml` 中 `spring.flyway.enabled=true`）。

### 2.5 验证迁移结果

```sql
USE uniondesk;
SELECT * FROM flyway_schema_history ORDER BY installed_rank;

-- P0 核心表检查
SHOW TABLES LIKE 'identity_subject';
SHOW TABLES LIKE 'staff_account';
SHOW TABLES LIKE 'customer_account';
SHOW TABLES LIKE 'business_domain';
SHOW TABLES LIKE 'ticket';
SHOW TABLES LIKE 'sla_rule';
SHOW TABLES LIKE 'audit_log';
SHOW TABLES LIKE 'login_log';
SHOW TABLES LIKE 'inbox_message';
```

### 2.6 常见问题

**Q1: 执行迁移时报错 "You do not have the SUPER privilege"**

使用 root 用户执行 `src/main/resources/db/init-mysql-config.sql` 脚本，或在 MySQL 配置文件中永久设置 `log_bin_trust_function_creators = 1`。

**Q2: 迁移失败后如何重试？**

开发环境：`flyway:clean` + `flyway:migrate`
生产环境：使用 `flyway:repair` 修复失败的迁移，禁止使用 `clean`。

---

## 3. 可观测性

### 3.1 指标

时延、错误率、SLA 达成率、队列积压、通知成功率。

### 3.2 日志

- 优先接入 ELK（应用日志、业务日志、审计日志分层）。
- 若环境暂未接入 ELK：使用 Spring 日志（Logback）输出本地滚动文件。

### 3.3 告警

接口异常、慢查询、通知失败、队列堆积。

### 3.4 备份

P0 提供备份脚本、恢复手册与一次恢复流程验证；定期恢复演练作为生产上线后的运维 Gate，明确 RPO/RTO。

---

## 4. 演进路线

- 阶段 1（MVP）：模块化单体 + 单库，快速验证与上线。
- 阶段 2（增长期）：按压力拆分通知、搜索、报表子系统。
- 阶段 3（规模化）：按领域拆分服务，建设事件总线与数据治理体系。
