# Directory Structure

> UnionDesk 后端代码组织方式（基于 `UnionDesk/` Maven 多模块工程）。

---

## Overview

后端为 **Maven 多模块 + 分层包结构**。业务逻辑在各自 feature 模块的 `core/`；HTTP 入口在 `web/`；持久化在 `entity` / `mapper` / `repository`。启动、全局配置、异常处理、**全部测试** 集中在 `uniondesk-app`。

权威说明见 `UnionDesk/README.md`。

---

## Maven 模块

```text
UnionDesk/
├── uniondesk-common/     # ApiResponse、ErrorCodes、领域事件（无 DB）
├── uniondesk-iam/        # 认证 + IAM（auth.* 与 iam.* 子包）
├── uniondesk-support/    # 审计、附件、敏感词、SLA、通知等横切能力
├── uniondesk-domain/     # 业务域、成员、客户、邀请码
├── uniondesk-ticket/     # 工单、类型、属性、表单 schema
└── uniondesk-app/        # @SpringBootApplication、Flyway、ApiExceptionHandler、集成测试
```

依赖方向：`common` ← `iam` ← `support` ← `domain` ← `ticket` ← `app`。

---

## 单模块包分层

每个业务模块在 `com.uniondesk.{feature}` 下使用固定五层：

```text
entity/       # *Po.java 持久化对象
mapper/       # MyBatis @Mapper 接口
repository/   # @Repository，封装 mapper，提供 findRequired*
core/         # @Service 业务逻辑
web/          # @RestController + *Dtos.java
```

**示例（ticket 模块）**：

| 层 | 路径 |
|----|------|
| entity | `uniondesk-ticket/.../entity/TicketTypePo.java` |
| mapper | `uniondesk-ticket/.../mapper/TicketTypeMapper.java` |
| repository | `uniondesk-ticket/.../repository/TicketTypeRepository.java` |
| core | `uniondesk-ticket/.../core/TicketConfigService.java` |
| web | `uniondesk-ticket/.../web/PlatformTicketConfigController.java` |

**Mapper XML**：`{module}/src/main/resources/mapper/{subdomain}/{Name}Mapper.xml`  
例：`uniondesk-ticket/src/main/resources/mapper/ticket/TicketTypeMapper.xml`

---

## 特殊位置

| 内容 | 位置 |
|------|------|
| 启动类 + `@MapperScan` | `uniondesk-app/.../UnionDeskApplication.java` |
| 全局异常处理 | `uniondesk-app/.../common/web/ApiExceptionHandler.java` |
| 登录 Controller | `uniondesk-app/.../auth/web/AuthController.java`（非 iam 模块） |
| JWT / Session | `uniondesk-iam/.../auth/core/` |
| 权限拦截 | `uniondesk-iam/.../iam/web/RequirePermissionInterceptor.java` |
| Flyway 迁移 | `uniondesk-app/src/main/resources/db/migration/current/` |
| 全部测试 | `uniondesk-app/src/test/java/com/uniondesk/` |

---

## API 路径约定

- 前缀：`/api/v1/...`
- 平台管理：`/api/v1/admin/platform/...`
- 域内管理：`/api/v1/admin/domains/{domain_id}/...`
- 查询参数：**snake_case**（如 `page_size`）

---

## DTO 约定

- 每个 Controller 配套一个 `{Feature}Dtos.java`，内含 nested `record`。
- 例：`ticket/web/TicketStatusDtos.java`、`domain/web/DomainDtos.java`

---

## 新增功能时如何落位

1. 判断归属模块（ticket / domain / support / iam）。
2. 按 **entity → mapper (+ XML) → repository → core → web** 顺序添加。
3. 需要新表时，在 `uniondesk-app/.../db/migration/current/` 增加 Flyway 脚本。
4. 测试写在 `uniondesk-app/src/test/java/com/uniondesk/{feature}/`，**不要**在 feature 模块内建 test 目录。

---

## 参考实现

- 完整分层栈：`uniondesk-ticket/src/main/java/com/uniondesk/ticket/`
- 域业务 + 错误码：`uniondesk-domain/src/main/java/com/uniondesk/domain/core/`
- 模块说明：`UnionDesk/README.md`
