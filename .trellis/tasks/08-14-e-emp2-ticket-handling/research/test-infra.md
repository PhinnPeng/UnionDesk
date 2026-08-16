# Research: uniondesk-ticket 测试基建（测试文件清单 / 覆盖点 / mvn test 状态 / MyBatis-Flex 下测试写法）

- Query: uniondesk-ticket 模块测试文件清单（src/test 下）、现有 TicketService 单测覆盖点、mvn test 是否可运行（预存损坏测试文件状态）、MyBatis-Flex 下测试如何写
- Scope: internal
- Date: 2026-08-16

## Findings

### 1. 测试位置：不在 uniondesk-ticket，而在 uniondesk-app

- `uniondesk-ticket/src/test` **不存在**（find 无结果）。全部测试集中在 `uniondesk-app/src/test/java/com/uniondesk/`，共 86 个 Java 文件
- spec 确认：`backend/index.md` Quality Check 明示「测试在 uniondesk-app」「未在 feature 模块添加 test 源集」——**新增工单测试必须放 uniondesk-app/src/test/java/com/uniondesk/ticket/**

### 2. ticket 相关测试清单（uniondesk-app/src/test/java/com/uniondesk/ticket/）

| 文件 | 类型 | 说明 |
|---|---|---|
| `core/TicketServiceTests.java`（316 行） | 单元（Mockito） | **@Disabled("待 TicketRepository mock 重写")**（行 51）——现有覆盖：createCustomerTicket 编号生成+history 持久化（行 131）、模板字段合并（行 188）、changeTicketStatus 非法流转拒绝（行 263） |
| `core/TicketWorkflowTests.java` | 单元 | @Disabled("待 TicketRepository mock 重写")（行 51） |
| `core/FormSchemaValidatorTests.java` / `FormSnapshotBuilderTests.java` / `StatusFlowValidatorTests.java` / `TicketConfigServiceTests.java` / `TicketFormSchemaServiceTests.java` / `TicketStatusServiceTests.java` | 单元 | 配置/表单校验类，正常启用 |
| `core/SlaScanJobTests.java` | 单元 | SLA 扫描任务 |
| `web/TicketLifecycleIntegrationTest.java`（约 300+ 行） | **集成（SpringBootTest+MockMvc）** | 全流程：登录态（IntegrationAuthSupport.mockCaptchaBypass）→ 建单 → 领取 → 回复 → 状态变更 → 关闭 → 历史断言；`@ActiveProfiles("test")` + `@Transactional` + `@Import(FixedClockTestConfiguration.class)` |
| `web/TicketConfigControllerTests.java` | 集成/控制器 | 配置控制器 |
| `web/CustomerTicketPermissionIntegrationTest.java` | 集成 | 客户侧权限（owner 校验等） |
| `web/TicketSatisfactionIntegrationTest.java` | 集成 | 满意度 |

### 3. mvn test 可运行状态（已实测）

- 本机执行 `./mvnw -o -q test-compile -pl uniondesk-app -am -DskipTests` **exit 0，测试源码编译通过**
- 「预存损坏测试文件」状态：3 个被禁用测试类（`TicketServiceTests`、`TicketWorkflowTests`、`AdminMenuServiceTest`）均为 `@Disabled("待 ... mock 重写")`，**不参与运行、不阻塞构建**；最近 commit `2c70700 fix(test): 对齐主代码签名修复测试编译` 已修复测试编译签名（TicketService 构造器补 ClaimRuleService、TicketRow 补 ticketTypeIcon 等）
- 结论：`mvn test` 当前可跑（单测+集成测试均启用，集成测试走 test profile 的 TestFlywayConfiguration，需要测试 DB 配置——`application-test.yml`/`IntegrationTestSupport` 在 uniondesk-app test 源集）

### 4. MyBatis-Flex 下测试写法（现状模式）

- **单元测试**：Mockito mock 全部 Repository + 依赖 Service，直接 `new TicketService(...)`（`TicketServiceTests` 行 106-127 展示了 21 个构造参数的注入清单）——TicketService 构造器参数顺序就是依赖清单（`TicketService.java` 行 88-131）；注意该类被 Disabled，新单测若启用需重写 TicketRepository mock（现状 TicketRepository 内部是 mapper 调用，mock 时用 `when(...).thenReturn(...)`）
- **集成测试**：`@SpringBootTest + @AutoConfigureMockMvc + @ActiveProfiles("test") + @Transactional`，走真实 MyBatis-Flex 映射 + Flyway 测试迁移；`IntegrationTestSupport`（uniondesk-app test 源集 `com.uniondesk.support`）提供公共脚手架；`@SpyBean` 用于绕过登录审计（`LoginAuditService`）、`@MockBean AuthCaptchaService` 绕过验证码
- MyBatis-Flex 本身无特殊测试设施需求：Flex 的 `BaseMapper` 在集成测试中走真实 DB；XML mapper 同路径加载（`mapper-locations: classpath*:mapper/**/*.xml`）
- 批量端点新测试建议：集成测试仿 `StaffDomainMemberBatchIntegrationTest.java`（domain 模块批量端点集成测试先例）+ 单测仿 BlockedWordServiceTests

## Caveats

- 现有 TicketService 单测整体 Disabled，AC5「现有工单单测不回归」的实际含义是：**新增功能不要破坏 12 个启用的 ticket 测试**（尤其 TicketLifecycleIntegrationTest 等集成测试）；是否顺手重写 Disabled 单测属于越界（AGENTS.md §3 精准修改）
- 集成测试依赖 test profile DB（MySQL test 库），离线环境跑 `mvn test` 若缺 DB 会失败于集成测试——test-compile 已验证通过，完整 `mvn test` 需测试库可用
- 前端另有 `ticket-type-flow-utils.test.ts`、`description-template-markdown.test.ts`（AdminWeb vitest）与 `platform/user/index.test.tsx`——批量按钮若加前端测试可参考这些
