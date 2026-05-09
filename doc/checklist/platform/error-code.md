# 错误码统一进度

## 目标

统一后端错误响应为数字错误码，`message` 使用中文描述。

## 进度

- [x] 新增 `ErrorCodes` 枚举
- [x] `ApiExceptionHandler` 改为统一输出数字 code
- [x] `AuthController`、`StaffController`、`IamController`、`RequirePermissionInterceptor`、`JwtAuthenticationFilter`、`CorsSecurityConfig` 的英文错误文案已收口
- [x] `AuthControllerTests`、`AuthControllerIntegrationTest`、`StaffControllerTests`、`DomainRoleControllerTests` 已同步断言

## 验证

- `cmd /c "cd /d F:\WorkSpace\UnionDesk\UnionDesk && mvnw.cmd -q -Dtest=AuthControllerTests,AuthControllerIntegrationTest,StaffControllerTests,AuthServiceTests,PlatformRoleServiceTest,IamControllerTests,DomainRoleControllerTests,TicketLifecycleIntegrationTest test"`
- `cmd /c "cd /d F:\WorkSpace\UnionDesk\UnionDesk && mvnw.cmd -q -Dtest=!UnionDeskApplicationTests test"`

