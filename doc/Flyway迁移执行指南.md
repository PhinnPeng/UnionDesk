# Flyway 数据库迁移执行指南

## 前置条件

### 1. MySQL 全局参数配置

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
-- 使用 root 或具有 SUPER 权限的用户登录 MySQL
mysql -u root -p

-- 设置全局参数
SET GLOBAL log_bin_trust_function_creators = 1;

-- 验证设置
SELECT @@GLOBAL.log_bin_trust_function_creators;
```

**方式三：修改 MySQL 配置文件（永久生效）**

在 `my.cnf` 或 `my.ini` 文件的 `[mysqld]` 部分添加：

```ini
[mysqld]
log_bin_trust_function_creators = 1
```

然后重启 MySQL 服务。

### 2. 数据库和用户准备

确保已创建数据库和用户：

```sql
CREATE DATABASE IF NOT EXISTS uniondesk CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'uniondesk'@'localhost' IDENTIFIED BY 'uniondesk';
GRANT ALL PRIVILEGES ON uniondesk.* TO 'uniondesk'@'localhost';
FLUSH PRIVILEGES;
```

## 执行 Flyway 迁移

### 方式一：使用 Maven 命令（推荐）

```bash
# 进入后端项目目录
cd UnionDesk

# 清空数据库（仅开发环境，生产环境禁用）
.\mvnw.cmd flyway:clean

# 执行迁移
.\mvnw.cmd flyway:migrate

# 查看迁移历史
.\mvnw.cmd flyway:info
```

### 方式二：应用启动时自动迁移

应用启动时会自动执行 Flyway 迁移（`application.yml` 中 `spring.flyway.enabled=true`）。

## 验证迁移结果

### 1. 检查 Flyway 历史表

```sql
USE uniondesk;
SELECT * FROM flyway_schema_history ORDER BY installed_rank;
```

应该看到 15 条迁移记录，状态均为 `Success`。

### 2. 检查核心表是否存在

```sql
-- P0 核心表检查
SHOW TABLES LIKE 'identity_subject';
SHOW TABLES LIKE 'staff_account';
SHOW TABLES LIKE 'customer_account';
SHOW TABLES LIKE 'business_domain';
SHOW TABLES LIKE 'ticket';
SHOW TABLES LIKE 'ticket_type';
SHOW TABLES LIKE 'ticket_reply';
SHOW TABLES LIKE 'ticket_history';
SHOW TABLES LIKE 'sla_rule';
SHOW TABLES LIKE 'sla_calendar';
SHOW TABLES LIKE 'file_attachment';
SHOW TABLES LIKE 'audit_log';
SHOW TABLES LIKE 'login_log';
SHOW TABLES LIKE 'inbox_message';
SHOW TABLES LIKE 'notification_log';
```

### 3. 检查表结构完整性

```sql
-- 检查 identity_subject 表结构
DESC identity_subject;

-- 检查 ticket 表的 SLA 字段
DESC ticket;

-- 检查外键约束
SELECT 
    TABLE_NAME,
    CONSTRAINT_NAME,
    REFERENCED_TABLE_NAME
FROM information_schema.KEY_COLUMN_USAGE
WHERE TABLE_SCHEMA = 'uniondesk'
  AND REFERENCED_TABLE_NAME IS NOT NULL
ORDER BY TABLE_NAME, CONSTRAINT_NAME;
```

## 常见问题

### Q1: 执行迁移时报错 "You do not have the SUPER privilege"

**原因**：用户没有 SUPER 权限，无法设置全局参数或创建触发器。

**解决方案**：
1. 使用 root 用户执行 `src/main/resources/db/init-mysql-config.sql` 脚本
2. 或者在 MySQL 配置文件中永久设置 `log_bin_trust_function_creators = 1`

### Q2: 迁移失败后如何重试？

**解决方案**：
```bash
# 1. 清空数据库（开发环境）
.\mvnw.cmd flyway:clean

# 2. 重新执行迁移
.\mvnw.cmd flyway:migrate
```

**注意**：生产环境不要使用 `flyway:clean`，应该使用 `flyway:repair` 修复失败的迁移。

### Q3: 如何查看迁移脚本的执行顺序？

**解决方案**：
```bash
.\mvnw.cmd flyway:info
```

### Q4: VALUES function deprecated 警告

**原因**：MySQL 8.0.20+ 版本中 `VALUES()` 函数已废弃。

**影响**：仅警告，不影响迁移执行。后续版本会使用 `AS alias` 语法替代。

## P0 数据库 Schema 完整性清单

### 核心表（必须存在）

- [x] `business_domain` - 业务域
- [x] `identity_subject` - 统一身份主体
- [x] `staff_account` - 员工账号
- [x] `customer_account` - 客户账号
- [x] `domain_member` - 域成员（员工）
- [x] `domain_customer` - 客户入域关系
- [x] `invitation_code` - 邀请码
- [x] `platform_role` - 平台角色
- [x] `domain_role` - 业务域内角色
- [x] `permission_item` - 权限项
- [x] `domain_role_permission` - 域角色与权限项
- [x] `ticket` - 工单
- [x] `ticket_type` - 工单类型
- [x] `ticket_reply` - 工单回复
- [x] `ticket_history` - 工单操作流
- [x] `ticket_relation` - 工单关联/合并
- [x] `ticket_priority_level` - 工单优先级
- [x] `sla_rule` - SLA 规则
- [x] `sla_calendar` - SLA 工作日历
- [x] `file_attachment` - 统一附件主表
- [x] `attachment_ref` - 附件业务关联
- [x] `attachment_policy` - 附件策略
- [x] `audit_log` - 审计日志
- [x] `login_log` - 登录日志
- [x] `inbox_message` - 站内信
- [x] `notification_log` - 通知发送日志
- [x] `notification_template` - 通知模板
- [x] `system_config` - 平台系统配置
- [x] `domain_config` - 业务域配置
- [x] `blocked_word` - 屏蔽词

### 关键字段检查

#### business_domain
- [x] `registration_policy` - 注册策略
- [x] `visibility_policy_codes` - 可见策略（JSON）
- [x] `deleted_at` - 软删除标记

#### ticket
- [x] `version` - 乐观锁
- [x] `result` - 反馈/建议内部结论
- [x] `sla_first_response_deadline` - SLA 首响截止时间
- [x] `sla_resolution_deadline` - SLA 解决截止时间
- [x] `sla_first_responded_at` - SLA 首响时间
- [x] `sla_resolved_at` - SLA 解决时间
- [x] `sla_status` - SLA 状态
- [x] `sla_paused_duration` - SLA 暂停时长
- [x] `assignee_staff_account_id` - 指派员工
- [x] `customer_account_id` - 客户账号

#### ticket_reply
- [x] `sender_type` - 发送者类型
- [x] `staff_account_id` - 员工账号
- [x] `customer_account_id` - 客户账号

### 索引检查

```sql
-- 检查关键索引
SELECT 
    TABLE_NAME,
    INDEX_NAME,
    GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) AS COLUMNS
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = 'uniondesk'
  AND TABLE_NAME IN ('ticket', 'ticket_reply', 'ticket_history', 'audit_log', 'login_log')
GROUP BY TABLE_NAME, INDEX_NAME
ORDER BY TABLE_NAME, INDEX_NAME;
```

## 迁移脚本清单

| 版本 | 文件名 | 说明 |
|------|--------|------|
| V1 | V1__init_schema.sql | 初始 schema（MVP 基础表） |
| V2 | V2__seed_demo_data.sql | 演示数据种子 |
| V202604190001 | V202604190001__init.sql | 健康检查标记表 |
| V202604200001 | V202604200001__seed_demo_consultation.sql | 演示咨询数据 |
| V202604210001 | V202604210001__auth_login_foundation.sql | 认证登录基础 |
| V202604210002 | V202604210002__auth_login_config_extensions.sql | 认证登录配置扩展 |
| V202604220001 | V202604220001__single_account_type_auth_client_iam.sql | 单账号类型认证客户端 IAM |
| V202604250001 | V202604250001__unified_identity_permission_snapshot.sql | 统一身份权限快照 |
| V202604250002 | V202604250002__iam_menu_tree_user_offboard_and_actions.sql | IAM 菜单树用户离职和操作 |
| V202604271000 | V202604271000__migrate_admin_menu_runtime.sql | 迁移管理菜单运行时 |
| V202604281000 | V202604281000__iam_permission_scope_refactor.sql | IAM 权限范围重构 |
| V202604291600 | V202604291600__enable_auth_slider_captcha.sql | 启用认证滑块验证码 |
| V202604302130 | V202604302130__platform_organization_foundation.sql | 平台组织基础 |
| V202605031200 | V202605031200__p0_prd_schema_extensions.sql | **P0 PRD schema 扩展（核心）** |
| V202605031201 | V202605031201__p0_ticket_notification_attachment_permissions.sql | **P0 工单通知附件权限** |

## 相关文档

- [数据库设计](./数据库设计.md)
- [P0 验收用例表](./P0验收用例表.md)
- [数据模型迁移策略](./数据模型迁移策略.md)
