# UnionDesk 后端

## 项目定位

`UnionDesk` 是 UnionDesk 的后端服务工程，提供客户咨询、工单处理、业务域隔离与管理端统计等能力。

## 技术栈

- Java 21
- Spring Boot 3.4
- Spring Security（JWT Filter 仅负责认证）
- MyBatis（Entity + Mapper + Repository）
- Flyway
- MySQL 8.0

## Maven 多模块结构

```text
UnionDesk/
├── pom.xml                      # 父 POM
├── uniondesk-common/            # 公共 Web 响应、领域事件
├── uniondesk-iam/               # 认证、IAM、权限
├── uniondesk-domain/            # 业务域、成员、客户、角色
├── uniondesk-ticket/            # 工单
├── uniondesk-support/           # 审计、附件、敏感词、SLA、通知
└── uniondesk-app/               # Boot 启动入口、Flyway、集成测试
```

各业务模块分层：`entity` / `mapper` / `repository` / `core` / `web`。

## 授权模型

- **JwtAuthenticationFilter**：解析 JWT、校验 Session、写入 `UserContext`（不做 API 路径授权）
- **RequirePermissionInterceptor**：通过 `@RequirePermission` 注解做接口授权

## 领域事件

`uniondesk-common/event/` 提供轻量事件总线（Spring `ApplicationEventPublisher`）。工单状态变更、成员状态变更在事务提交后（`AFTER_COMMIT`）异步触发审计/通知。

## Demo 模块

`DemoDataService`、Dashboard、Consultation 接口在 `demo` Profile 下启用（默认 `spring.profiles.include: demo`）。生产环境可通过 `SPRING_PROFILES_ACTIVE=prod` 并排除 `demo` 关闭。

## 启动方式

### 启动数据库（按需）

```powershell
docker compose up -d
```

### 编译

```powershell
.\mvnw.cmd clean compile
```

### 启动应用

```powershell
.\mvnw.cmd -pl uniondesk-app spring-boot:run
```

### 运行测试

```powershell
.\mvnw.cmd test
```

## 验收检查

```powershell
# 业务代码中无 JdbcTemplate（DemoDataService 为 @Profile("demo") 隔离）
rg JdbcTemplate uniondesk-*/src/main --glob "*.java"
```
