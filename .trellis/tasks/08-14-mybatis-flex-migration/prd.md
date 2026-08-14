# MyBatis-Flex 替换方案（XML→纯Java + Auto/snowFlakeId 主键）

> 2026-08-14 立项（P1）。依据：`feature/verify-mybatis-flex-replacement` 分支已完成的全量替换验证（提交 1877987）——编译 7 模块全绿、345 用例与 master 基线对照零回归、Flex 执行层异常（绑定/SQL 语法/Mapper 缺失）= 0。

## Goal

把持久层从「原生 MyBatis + 手写 XML」平稳迁移到 **MyBatis-Flex 1.11.8 纯 Java 操作**：

1. 76 个 XML Mapper 分批替换为 `BaseMapper`/`QueryWrapper` 纯 Java（存量 XML 与 Java 共存直至全量完成）
2. 主键：默认 `Auto`；「部分模型」改用 `snowFlakeId`（id 列 BIGINT 不变）
3. 分页**统一使用 MyBatis-Flex 官方分页操作**（`BaseMapper.paginate(Page, qw)`，count 自动生成），不引入分页拦截器（不回归 SQL 解析）

## 现状（证据已闭合）

- 持久层：Spring Boot 3.4.4 + `mybatis-spring-boot-starter` 3.0.4（原生 MyBatis）；**76 接口 + 76 XML** 全手写；**99 个纯 POJO 实体**（无 `@Table`/`@Id`）
- 分页：手写 `LIMIT #{limit} OFFSET #{offset}` + 独立 COUNT 查询（如 `countAdminDomains`），Service 经 Repository 分别取列表与总数
- 主键：全部 `bigint AUTO_INCREMENT`（`useGeneratedKeys` 回填）
- 验证分支已证：Flex 1.11.8 下 76 XML + 14 注解式 Mapper 全部正常绑定执行；345 用例 295 通过，39 失败全部复现于 master 基线或为 dev 服务锁争用（环境问题）

## Requirements

- **R1（XML→纯 Java）** 76 个 XML Mapper 分批替换为 `BaseMapper<T>` + QueryWrapper lambda；查询方法保留原语义（default 方法），非分页方法 Service 层零改动
- **R2（实体注解）** 涉及实体补 `@Table`/`@Id`/`@Column`：表名、主键策略、`created_at`/`updated_at` 的 `onInsertValue`/`onUpdateValue`、JOIN 冗余字段 `ignore`
- **R3（分页·统一 Flex 操作）** 分页一律 `BaseMapper.paginate(Page<T>, qw)`（`Page.of(page, size)` 构造，count 自动生成）；Repository/Service 分页调用同步切 Page 形态，映射现有 `PageResult<T>(total, list)`；**不引入分页拦截器/PageHelper**
- **R4（主键）** 默认 `@Id(keyType=Auto)`；「部分模型」（清单见 design §4）改用 `@Id(keyType=KeyType.Generator, value=KeyGenerators.snowFlakeId)`；不改变既有数据 id
- **R5（红线约束）** 数据范围仍按「码后缀 + 路径分工 + service 过滤」表达；**不引入** Flex `IDialect` 数据权限钩子 / DataScope 类机制

## Acceptance Criteria

- [ ] **AC1** 试点（BlockedWordMapper）完成：接口 `extends BaseMapper`、实体注解齐全、XML 删除；Repository/Service 分页调用已切 Page 形态（含单测同步）；`./mvnw compile` 全绿；`BlockedWordServiceTests`/`PlatformBlockedWordControllerTests` 通过
- [ ] **AC2** 分页等价：`paginate` 返回的 records/totalRow 与迁移前 `LIMIT/OFFSET + count` 语义一致（逐项比对，含默认 page_size=20 与越界页）
- [ ] **AC3** 全量替换后全量测试与 master 基线**无新增失败**（验证前停 dev 服务避免锁争用）；Flex 绑定错误 = 0
- [ ] **AC4** 主键切换模型：插入后 id 由 `snowFlakeId` 生成器回填（非自增小整数）；既有数据不受影响；外键引用正常
- [ ] **AC5** 红线：无新增「按身份动态过滤」代码；数据权限过滤仍走 service 显式 QueryWrapper 条件
- [ ] **AC6** 残留 XML 与 Java 共存期正常（未迁移 Mapper 绑定无回归）

## Out of Scope

- 分页拦截器 / PageHelper / 自动分页（已决策：统一 Flex `paginate` 官方操作）
- Flex `IDialect` 数据权限钩子（红线禁止）
- 全部实体一次性注解化（随批推进，不单列）
- 库表 DDL 变更（id 列保持 BIGINT，不做列级改动）

## 参考证据

- 验证分支：`feature/verify-mybatis-flex-replacement`（提交 `1877987`，6 文件：5 pom + application.yml）
- Flex 坐标：`com.mybatis-flex:mybatis-flex-spring-boot3-starter:1.11.8`；配置前缀 `mybatis-flex:`（mapper-locations/configuration 子项结构不变）
- 分页：`BaseMapper.paginate(Page, QueryWrapper)` / `paginateAs` / `xmlPaginate`（源码核实）；`Page.of(pageNumber, pageSize)`；全局默认 pageSize 来自 `FlexGlobalConfig.defaultPageSize`（默认 10，可调）
- 主键生成器（源码核实）：`KeyGenerators.snowFlakeId / flexId / uuid / ulid`；`@Id(keyType=Generator, value=...)`
- 红线：`.trellis/tasks/archive/2026-08/08-13-customer-permission-chain/design.md` §1
