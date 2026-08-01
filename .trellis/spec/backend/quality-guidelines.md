# Quality Guidelines

> 后端代码质量、测试与审查标准。

---

## Overview

- Java 21 + Spring Boot 3.4
- 测试 **全部** 在 `uniondesk-app`；feature 模块无独立 test 源集
- 框架：JUnit 5、Mockito、AssertJ、Spring Test（MockMvc / `@SpringBootTest`）

---

## 验证命令

```powershell
cd UnionDesk
.\mvnw.cmd test                    # 全量
.\mvnw.cmd test -Dtest=FooTests    # 单类（加 -Dsurefire.failIfNoSpecifiedTests=false -pl uniondesk-app -am）
.\mvnw.cmd clean compile           # 编译检查
```

---

## 测试放置与命名

| 模式 | 用途 | 示例 |
|------|------|------|
| `{Name}Tests` | 单元 / Controller 测试（主流） | `TicketStatusServiceTests.java` |
| `{Name}Test` | 混合 | 部分集成测试 |
| `{Name}IntegrationTest` | 全栈 + DB | `AuthControllerIntegrationTest.java` |

路径镜像生产包：`uniondesk-app/src/test/java/com/uniondesk/ticket/core/`。

---

## 两种测试风格

### A. 隔离单元 / Controller（无 Spring 上下文）

```java
MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new TicketConfigController(...)).build();
when(ticketConfigService.listTicketTypes(1L)).thenReturn(...);
mockMvc.perform(get("/api/v1/admin/domains/1/ticket-types"))
    .andExpect(status().isOk());
```

适用：Service 纯逻辑、Controller 契约。

### B. 集成测试（Spring 上下文 + Flyway）

```java
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthControllerIntegrationTest { ... }
```

辅助类：`IntegrationTestSupport.java`、`IntegrationAuthSupport.java`。

断言优先 **AssertJ**：`assertThat(...)`、`assertThatThrownBy(...)`。

---

## 代码审查清单

- [ ] 新 API 有 `@RequirePermission` 且权限码已 Flyway 种子
- [ ] 业务异常有稳定中文消息
- [ ] 新表迁移无 FK；命名符合 `V{datetime}__desc.sql`
- [ ] 分层正确：Controller 薄、逻辑在 core
- [ ] 测试覆盖核心分支（至少 Service 或 Controller 一层）
- [ ] 未在 feature 模块添加 test 目录
- [ ] 未使用 `JdbcTemplate`（demo profile 除外）

---

## AGENTS.md 对齐（后端相关）

- 不使用数据库外键
- 新增模块前先查阅现有 Maven 模块与分层
- 数据库访问走 MyBatis Repository，不用 JPA

---

## 反模式

- ❌ 只测 happy path、不断言错误码/中文 message
- ❌ Controller 测试启动完整 `@SpringBootTest` 当单元测试用
- ❌ 在 ticket/iam 等子模块 `pom.xml` 加 test 依赖
- ❌ 跳过 `mvn test` 声称完成

---

## 参考文件

- 单元测试：`uniondesk-app/src/test/java/com/uniondesk/ticket/core/TicketStatusServiceTests.java`
- Controller 测试：`uniondesk-app/src/test/java/com/uniondesk/ticket/web/TicketConfigControllerTests.java`
- 集成测试：`uniondesk-app/src/test/java/com/uniondesk/auth/web/AuthControllerIntegrationTest.java`
- 模块说明：`UnionDesk/README.md`
