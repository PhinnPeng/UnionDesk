# admin 角色配置进度

## 目标

确保 `admin` 登录后在 `GET /api/v1/auth/me` 中返回 `platformRoles`，且包含 `platform_admin`。

## 进度

- [x] 新增迁移 `V202605100001__admin_platform_role.sql`
- [x] `AuthService.currentUser()` 改为从 `PlatformRoleService` 读取平台角色
- [x] `AuthControllerIntegrationTest` 通过真实登录验证 `platformRoles`
- [x] `PlatformRoleServiceTest` 重新收口最后一个 `platform_admin` 保护场景

## 验证

- `cmd /c "cd /d F:\WorkSpace\UnionDesk\UnionDesk && mvnw.cmd -q -Dtest=AuthControllerTests,AuthControllerIntegrationTest,StaffControllerTests,AuthServiceTests,PlatformRoleServiceTest,IamControllerTests,DomainRoleControllerTests,TicketLifecycleIntegrationTest test"`
- `cmd /c "cd /d F:\WorkSpace\UnionDesk\UnionDesk && mvnw.cmd -q -Dtest=!UnionDeskApplicationTests test"`

