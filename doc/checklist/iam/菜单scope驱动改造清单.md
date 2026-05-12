# 菜单 scope 后端驱动改造清单

## 目标
将 AdminWeb 菜单展示切换为后端 `permission-snapshot` 驱动，并通过 `scope` 字段区分平台/业务菜单域。

## 任务
1. 后端 `iam_admin_menu` 增加 `scope` 列、约束、索引，并补平台菜单种子数据
2. 后端 `AdminMenuService` / `IamService` / `IamController` / `IamDtos` 贯通 `scope`
3. 前端 shared 类型、AdminWeb 路由转换与菜单生成支持 `scope`
4. 菜单管理页补 `scope` 字段与列表展示，删除旧 `permission` 路由入口
5. 补充回归测试并完成 `typecheck` / `test` / 浏览器验证

## 状态
- [x] 后端迁移与 DTO
- [x] 前端 shared / router / 菜单页
- [x] 测试与验证

## 结果
- 已将菜单树切换为后端 `permission-snapshot` 驱动，并通过 `scope` 区分平台/业务菜单域。
- 已在真实浏览器中验证平台侧边栏显示为 `权限管理 -> 平台角色 / 平台菜单`。

## 4. 2026-05-08 平台级路由对齐
- [x] 后端平台菜单 `route_path` 已迁移为 `/platform/*`，保留 `component_key` 不变。
- [x] `PermissionSnapshot` 仅返回当前角色可见的菜单视图，平台管理员只看平台菜单，域管理员只看业务菜单。
- [x] 前端删除静态平台路由模块，AdminWeb 完全依赖后端动态路由生成。
- [x] 前端路径标准化同时兼容 `/platform/*` 与旧的 `/system/*` 输入。
- [x] 已在浏览器中验证 `/platform/menu` 可正常加载，按钮与图标列渲染正常。

## 5. 2026-05-08 缺陷修正
- [x] `resolveAdminMenuScope` 增加空角色和缺失角色回退，避免 `role not found` 直接打断菜单快照生成。
- [x] 路由路径迁移脚本补充幂等说明，避免重复执行时产生歧义。

## 6. 2026-05-08 菜单系统问题修复
- [x] 菜单与角色表格序号列改回数字序号，移除 `indexBorder` 圆圈样式。
- [x] 菜单图标列改为真实 Ant Design 图标渲染，并对图标名做了 `trim` 兼容。
- [x] `fetchUserInfo` / `fetchAsyncRoutes` 合并为一次 `permission-snapshot` 数据装载，避免重复请求。
- [x] 登录后 `/system/menu` 可正常进入，`permission-snapshot` 在浏览器登录链路中只请求一次。
- [x] 纯平台首页跳转逻辑已由 `auth-guard` 单测覆盖，平台/业务菜单分流保持一致。

## 7. 2026-05-08 轻微缺陷收口
- [x] `permission-snapshot` 增加 5 秒短缓存与进行中请求复用，避免 `fetchUserInfo()` / `fetchAsyncRoutes()` 分开调用时重复请求。
- [x] 菜单图标缺失时改为紧凑警告图标回退，不再输出长文本撑宽列。

## 8. 2026-05-08 菜单域切换
- [x] 菜单管理页新增平台端 / 业务端 / 全部菜单的域切换入口。
- [x] `scopeFilter = all` 时显示 `scope` 列，其余场景隐藏该列。
- [x] 新增 / 编辑菜单时按当前域自动填充默认 `scope`。
- [x] 菜单树请求支持 `scope` 参数，并补充对应单测。
- [x] 菜单域切换测试已完成，`typecheck` 通过。

## 9. 2026-05-08 菜单域切换二态化
- [x] 菜单域切换收敛为平台端 / 业务端两态，不再提供“全部菜单”入口。
- [x] 菜单列表固定展示 `scope` 列，并用标签区分平台端与业务端。
- [x] 菜单域切换组件改为双行说明样式，平台端与业务端各自显示路径提示。
- [x] `fetchMenuTree` / `fetchMenuList` 参数类型收窄为平台端与业务端。
- [x] 相关测试已同步更新并通过验证。

## 2026-05-09 菜单树 scope 收口修复
- [x] `GET /api/v1/iam/menus/tree` 已透传 `scope`，并按当前用户授权菜单树返回。
- [x] `AdminMenuService` 已收口授权树查询与 required 按钮映射，避免平台 / 业务菜单混流。
- [x] `AdminMenuRequiredMenuIntegrationTest` 已覆盖 `/permission-snapshot` 与 `/menus/tree?scope=platform` 路由集合一致性回归。

## 2026-05-12 Flyway 回溯排查与无损修复
- [ ] 在隔离 MySQL 容器或临时 schema 中导出 `iam_admin_menu`、`iam_admin_role_menu_relation` 与 `flyway_schema_history` 快照，只做对比，不碰当前 `uniondesk` 库。
- [ ] 复核 `V202605110001__fix_platform_permission_tree_and_buttons.sql` 与 `V202605110002__remove_platform_catalog_wrapper.sql`，确认当前菜单变化都是前向幂等迁移，不存在回滚脚本误执行。
- [ ] 如果发现菜单节点或按钮授权缺失，只新增一条只增不删的正向迁移恢复数据，不改写已执行的历史迁移。
- [ ] 后续验证统一切到隔离环境，禁止在当前共享库上直接做 `flyway:clean`、`flyway:repair` 或会删除数据的临时 SQL。
- [ ] 更新启动说明，明确 `spring-boot:run` 会触发 Flyway，日常联调先用隔离库或显式关闭迁移后再起后端。
