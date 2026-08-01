# Logging Guidelines

> 当前项目的日志实践（偏精简）。

---

## Overview

- **框架**：SLF4J + Logback
- **配置**：`uniondesk-app/src/main/resources/logback-spring.xml`
- **默认格式**：纯文本 `%d %-5level [%thread] %logger{36} - %msg%n`
- **默认级别**：root `INFO`
- **结构化日志**：仅 `elk` profile 启用 Logstash JSON（非默认）

业务代码中 **几乎不使用** 应用日志；审计走数据库 `audit_log` 表。

---

## 实际使用模式

全项目生产代码仅少数类使用 `LoggerFactory.getLogger`：

| 文件 | 场景 |
|------|------|
| `ApiExceptionHandler.java` | `log.error` 记录 DB 异常与未处理异常 |
| `AuditLogWriter.java` | `log.warn` 审计写入失败（含 action/target） |

```java
private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);
log.error("未处理的服务器异常", ex);
```

- **无** `@Slf4j` Lombok 惯例
- **无** MDC / traceId 约定
- **无** 业务 Service 中的 info/debug 刷屏

---

## 何时该打日志

| 应该 | 不应该 |
|------|--------|
| 未预期异常（handler 兜底） | 每个 CRUD 成功路径 |
| 基础设施失败（审计落库失败） | 用日志替代审计表 |
| 排查中的临时 debug（提交前删除） | 记录密码、token、完整 PII |

---

## 审计 vs 日志

用户操作审计通过 `AuditLogWriter` 写入 **`audit_log` 表**，不是 application log。新增需审计的操作应走审计模块，而非 `log.info`。

---

## 配置扩展

需要 ELK 时激活 Spring profile `elk`（Logstash TCP appender）。默认开发/生产不启用。

---

## 反模式

- ❌ 在 Service 层大量 `log.info` 替代单元测试或审计
- ❌ 日志中输出 JWT、密码、验证码
- ❌ 引入新日志框架或 `@Slf4j` 与现有风格混用（除非团队统一迁移）

---

## 参考文件

- `uniondesk-app/src/main/resources/logback-spring.xml`
- `uniondesk-app/.../common/web/ApiExceptionHandler.java`
- `uniondesk-support/.../audit/core/AuditLogWriter.java`
