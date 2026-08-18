# Database Guidelines

> MyBatis + Flyway + MySQL 约定（UnionDesk 后端）。

---

## Overview

- **ORM**：MyBatis（非 JPA）；PO 为纯 JavaBean，无注解映射。
- **迁移**：Flyway，脚本集中在 `uniondesk-app`。
- **外键**：**新表不使用数据库外键**；引用关系由业务逻辑保证（见 `AGENTS.md` §3）。
- **命名**：表/列 `snake_case`；Java PO `camelCase`；MyBatis 开启 `map-underscore-to-camel-case`。

---

## Flyway

| 项 | 约定 |
|----|------|
| 活跃目录 | `uniondesk-app/src/main/resources/db/migration/current/` |
| 统一基线 | `V20260816150000__unified_final_baseline.sql` |
| 历史版本 | 已合并并从仓库移除，可通过 Git 历史追溯 |
| 配置 | `application.yml` → `spring.flyway.locations: classpath:db/migration/current` |
| 文件命名 | 后续增量沿用 `V{YYYYMMDDHHMM}__{snake_case_description}.sql`；当前基线固定为统一最终版本 |

**当前基线**：

- `V20260816150000__unified_final_baseline.sql`

后续数据库变更从该基线继续追加新的版本脚本；历史版本通过 Git 历史追溯。

测试环境：`TestFlywayConfiguration.java` 在 `@Profile({"test","demo"})` 下执行 `repair()` + `migrate()`。

---

## 表设计惯例（近期迁移）

```sql
-- 主键
id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT

-- 时间戳
created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3)

-- 软引用（无 FK）
domain_id BIGINT UNSIGNED NOT NULL
ticket_type_id BIGINT UNSIGNED NOT NULL

-- 索引
UNIQUE KEY uk_... (...)
KEY idx_... (...)

-- 配置类字段
config_json JSON
type_config JSON
form_schema JSON
```

**平台/域作用域**：常见 `scope` + `scope_domain_key` 生成列实现唯一约束（见 `V202606220001__ticket_type_platform_scope.sql`）。

**外键移除**：新迁移主动 `DROP FOREIGN KEY`（同文件示例）。

---

## MyBatis

**配置**（`application.yml`）：

```yaml
mybatis:
  mapper-locations: classpath*:mapper/**/*.xml
  configuration:
    map-underscore-to-camel-case: true
```

**Mapper 接口**：`@Mapper`，方法参数用 `@Param`，XML `namespace` 为接口全限定名。

**Repository 层**：在 mapper 之上封装；缺失记录时抛 `IllegalArgumentException`（带中文消息），由全局 handler 转为 API 错误。

```java
// TicketTypeRepository.java
throw new IllegalArgumentException("事项类型不存在");
```

---

## Entity（PO）命名

| 类型 | 命名 | 示例 |
|------|------|------|
| 表映射 | `{Entity}Po` | `TicketTypePo`, `TicketAttributePo` |
| 查询视图 | `{Entity}ViewPo` | `AuditLogViewPo` |
| 写入模型 | `{Entity}WritePo` | 审计模块 |
| 状态常量 | PO 内 `public static final String` | `TicketTypePo.SCOPE_PLATFORM` |

PO **不使用** JPA `@Entity` / MyBatis `@Table` 注解。

---

## 权限与种子数据

- 新平台能力常伴随 Flyway 插入 `iam_permission`、菜单种子（见 `V202606210002__platform_ticket_config_attribute_permissions.sql`）。
- 登记版本到 `docs/product/increment-plan`（产品流程，非代码强制）。

---

## 禁止 / 反模式

- ❌ 新表添加 `FOREIGN KEY` 约束
- ❌ 在 feature 模块放 Flyway 脚本（必须在 `uniondesk-app`）
- ❌ 在业务代码使用 `JdbcTemplate`（Demo 模块 `@Profile("demo")` 除外）
- ❌ 绕过 Repository 在 Controller 直接调 Mapper

---

## 参考文件

- 迁移示例：`uniondesk-app/src/main/resources/db/migration/current/V202607070001__ticket_status_table.sql`
- Mapper XML：`uniondesk-ticket/src/main/resources/mapper/ticket/TicketTypeMapper.xml`
- PO 示例：`uniondesk-ticket/.../entity/TicketTypePo.java`
- Repository：`uniondesk-ticket/.../repository/TicketTypeRepository.java`
