# Design — MyBatis-Flex 替换（XML→纯Java + Auto/snowFlakeId 主键 + 统一分页）

> 依据：`prd.md`（R1-R5、AC1-AC6）+ 验证分支结论。目标形态：Flex 纯 Java 查询为主、残留 XML 过渡、**分页统一 `paginate(Page, qw)`**、部分模型 `snowFlakeId` 主键。红线沿用「码后缀 + 路径分工 + service 过滤」三层模型。

## 1. 分层批次策略

每批（按模块）执行同一节奏：**实体注解 → 接口 extends BaseMapper → 查询转 default 方法（QueryWrapper）→ 分页方法连同 Repository/Service 调用切 Page 形态 → 删该批 XML → 编译 + 单测 + 分页等价冒烟**。

- 批次顺序（依赖自底向上）：`iam` → `support` → `domain` → `ticket`（ticket 依赖 domain/support，最后）
- **试点先行**：`BlockedWordMapper`（uniondesk-support/blockedword，单表 CRUD + 全局/域分页，最干净）验证全链路后再批量
- 复杂 SQL（联表/UNION/树查询，如 `IamPermissionMapper.selectEffectiveGrants`）**暂留 XML 或 `@Select`**，不强行 QueryWrapper——Flex 完全兼容（验证分支已证 0 绑定错误）

## 2. 实体注解契约（R2）

- `@Table("实际表名")`：类名≠表名必须显式（`BlockedWordPo` → `blocked_word`；`OrganizationPo` → `platform_organization`）
- `@Id`：默认 `keyType = Auto`（自增，insert 省略 id 列 + 回填）；R4 模型改 `@Id(keyType = KeyType.Generator, value = KeyGenerators.snowFlakeId)`
- `@Column(onInsertValue = "CURRENT_TIMESTAMP(3)")`：`created_at` 等 DB 默认值列——**必须声明**，否则 Flex insert 会把 NULL 写进 NOT NULL 列（XML 原行为是省略该列）
- `@Column(ignore = true)`：JOIN 冗余展示字段（如 `OrganizationPo.parentName/leaderName`）不参与 insert/update/select 映射
- 列名映射：camelCase→snake_case 由 Flex 默认完成（`businessDomainId`→`business_domain_id`），`map-underscore-to-camel-case: true` 保留

## 3. Mapper / Repository / Service 契约（R1/R3）

**分页统一走 Flex `paginate`**（count 自动生成，同一 QueryWrapper，无 count/列表条件漂移风险）：

```java
// Mapper：default 方法，返回 Flex Page
@Mapper
public interface BlockedWordMapper extends BaseMapper<BlockedWordPo> {
    default Page<BlockedWordPo> selectPageByGlobal(Page<BlockedWordPo> page, String keywordLike) {
        return paginate(page, QueryWrapper.create()
                .from(BlockedWordPo.class)
                .where(BlockedWordPo::getBusinessDomainId).isNull()
                .and(BlockedWordPo::getWord).like(keywordLike, If::hasText)
                .orderBy(BlockedWordPo::getCreatedAt, false)
                .orderBy(BlockedWordPo::getId, false));
    }
    // 非分页方法同前：selectListByQuery / selectOneByQuery / selectCountByQuery / deleteByQuery
}

// Repository：透传 Page，删除独立 count 方法
public Page<BlockedWordPo> findPageByGlobal(Page<BlockedWordPo> page, String keywordLike) {
    return mapper.selectPageByGlobal(page, keywordLike);
}

// Service：Page.of(page, size) 构造，映射现有 PageResult(total, list)
Page<BlockedWordPo> result = repository.findPageByGlobal(Page.of(page, pageSize), keywordLike);
return new PageResult<>(result.getTotalRow(), result.getRecords().stream().map(...).toList());
```

- 统一规则：
  - Mapper 分页方法签名 `Page<T> selectPageByXxx(Page<T> page, ...)`，内部 `paginate(page, qw)`；**不再用 `limit(offset, rows)` 手动拼接**
  - Repository 返回 `Page<T>`，删除随之孤立的独立 count 方法（如 `countByGlobal/countByDomain`）
  - Service 用 `Page.of(pageNumber, pageSize)` 构造（pageSize 缺省可配 `FlexGlobalConfig.defaultPageSize`），映射现有 `PageResult<T>(totalRow, records)`
  - 保留 count 语义的校验方法（如 `existsGlobal` → `countByGlobalAndWord`）维持 `selectCountByQuery`
- 数据权限过滤写在 service 层 QueryWrapper（`.eq(TicketPo::getCustomerId, ctx.userId())`），不落 Mapper

## 4. 主键策略（R4）

| 生成器 | 结构 | 适用 |
|---|---|---|
| **Auto（默认）** | DB 自增 | 未指定模型（含试点 BlockedWordPo） |
| **snowFlakeId** | 位段式 41 位时间戳+10 位机器码(MAC+PID 自动推导)+12 位序列；400w/s、回拨>5ms 抛异常 | 「部分模型」（首批清单） |

- flexId 经评估弃用（用户决策：仅 Auto/snowFlakeId 二选一；flexId 有序性优势对既有自增模型的增量意义有限）
- 切换注意：新 id 为 19 位大数；id 列保持 BIGINT 不动；既有数据 id 不变；外键引用不受影响
- **首批「部分模型」清单待用户指定**（建议：高流量/需全局唯一且可排序主键的表，如工单 `ticket`、站内信 `inbox_message`）
- 时机：随所属批次一起切，**独立 commit**（可单独回滚）

## 5. 兼容性与回滚

- Flex 与残留 XML/注解 SQL 共存（验证分支 345 用例已证）
- 每批独立 commit；回滚 = revert 该批 commit（XML 从 git 历史恢复；Repository/Service 分页改动随批 revert）
- 主键策略切换独立 commit，可单独回滚（生成器换回 Auto 即可，不影响既有数据）

**风险与对策**

| 风险 | 对策 |
|---|---|
| insert 写 NULL 进 NOT NULL 默认值列 | `@Column(onInsertValue/onUpdateValue)` |
| JOIN 冗余字段被 insert/update 携带 | `@Column(ignore = true)` 或独立 VO 类 |
| Mockito mock 的 mapper/repository default 方法执行真实逻辑 | 单测用 `when(...).thenReturn(...)` 显式 stub；转换后跑单测确认（Page 形态签名同步改） |
| 分页越界/默认值行为变化 | AC2 冒烟：默认 page_size、越界页、count 与列表一致性 |
| dev 服务与测试争 DB 锁（Lock wait timeout 50s） | 全量验证前停 dev 服务（`mvn spring-boot:run` 进程） |
| snowFlakeId 切换后 id 长度变化影响前端/日志假设 | 切换前按模型确认（AC4） |

## 6. 红线落地（R5）

- 数据权限过滤一律写进 **service 层** QueryWrapper（显式、可审、可测）
- **不引入**：Flex `IDialect.prepareAuth/forSelectByQuery` 数据权限钩子、PageHelper/自写分页拦截器、DataScope 注解/拦截器
- 码后缀（`@RequirePermission` 逐端点）与路径分工（`/my/**` vs `/admin/**`）不动
