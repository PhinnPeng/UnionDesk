# Implement — MyBatis-Flex 替换执行计划

> 分支：`feature/verify-mybatis-flex-replacement`（依赖替换与零回归验证已完成，提交 `1877987`；本任务在其之上推进 XML→纯Java 与主键切换）
> 前置：全量测试前需**停止 dev 服务**（`mvn spring-boot:run` 进程），否则 Lock wait timeout 干扰基线对照

## 阶段 0：试点（BlockedWordMapper，验证全链路）✅

- [x] 0.1 `BlockedWordPo`：`@Table("blocked_word")` + `@Id(keyType = KeyType.Auto)` + `createdAt` 加 `@Column(onInsertValue = "CURRENT_TIMESTAMP(3)")`
- [x] 0.2 `BlockedWordMapper extends BaseMapper<BlockedWordPo>`：全部方法转 default（QueryWrapper lambda；分页统一 `paginate(Page.of(...), qw)`，count 自动生成）
- [x] 0.3 `BlockedWordRepository`：分页方法签名改 `Page<BlockedWordPo>` 形态；删除孤立的 `countByGlobal/countByDomain`；保留 `existsGlobal/existsInDomain`（`selectCountByQuery`）
- [x] 0.4 `BlockedWordService.listPage`：改 `Page.of(page, pageSize)` 构造 → 映射 `PageResult(totalRow, records)`
- [x] 0.5 同步单测 mock 签名（`BlockedWordServiceTests`/`PlatformBlockedWordControllerTests`）
- [x] 0.6 删除 `uniondesk-support/src/main/resources/mapper/blockedword/BlockedWordMapper.xml`
- [x] 0.7 验证：`./mvnw -B -ntp compile`；单测 10/10 通过
- [x] 0.8 分页等价冒烟（AC2）：`BlockedWordRepositoryIntegrationTest` 真库通过（插入回填 + paginate count + 越界页）；commit `13fe061`
  - 注意：删除 XML 后必须 `clean`（target/classes 残留旧 XML 会导致 SqlSessionFactory 解析失败）

## 阶段 1：按模块分批 ✅（四批并行子代理完成 + 主代理统一验证）

- [x] 1.1 `iam` 批次（auth/ + iam/）— commit `c64193c`（21 Mapper，3 全转+15 半转+3 保留；10 个 Mapper 冲突 statement 重命名 insertRow/updateRow/deleteRowById/selectAllRows）
- [x] 1.2 `support` 批次 — commit `315f1c8`（6 全转 + 9 保留，NotificationTemplate/SlaCalendar 分页切 Page）
- [x] 1.3 `domain` 批次 — commit `1355962`（12 全转 + 3 保留，DomainRepository/Service 分页切 Page）
- [x] 1.4 `ticket` 批次 — commit `7d1fc3e`（20 全转 + 4 保留，7 Repository/4 Service 分页切 Page；修复 6 Mapper QueryWrapper 误用）
- [x] 每批验证：`./mvnw -B -ntp compile` 全绿 + 对应单测；复杂联表/UNION 保留 XML 或 `@Select`
- [x] 每批 commit 前自查：改动范围仅限本批模块（git status 逐批核对）

## 阶段 2：主键生成器切换（R4，独立 commit 可回滚）⏳ 待用户清单

- [ ] 2.1 用户指定「部分模型」清单 → 实体 `@Id` 改为 `@Id(keyType = KeyType.Generator, value = KeyGenerators.snowFlakeId)`
- [ ] 2.2 验证：插入后 id 由生成器回填（19 位大数）、既有数据不受影响、外键引用正常（AC4）

## 阶段 3：全量验证与收尾（AC3/AC5 已验证 ✅）

- [x] 3.1 停 dev 服务 → 全量 346 用例：失败类集合与 master 基线**完全一致**（19 类），Flex 绑定错误=0、SQL 语法/列错误=0；3 个新增失败证实为 DB 污染（恢复种子 `staff_account_platform_role (2,1)` 后转绿）
- [x] 3.2 红线自查：`prepareAuth`/`DataScope`/PageHelper/分页拦截器 = 0 命中（AC5）
- [ ] 3.3 汇总报告；`task.py` 收尾（归档/提交）——待用户确认清单与归档

## 验证命令

```bash
# 编译
./mvnw -B -ntp compile
# 单测（指定类）
./mvnw -B -ntp test -pl uniondesk-app -am -Dtest='BlockedWordServiceTests,PlatformBlockedWordControllerTests' -Dsurefire.failIfNoSpecifiedTests=false
# 全量（先停 dev 服务）
./mvnw -B -ntp test
# 基线对照：git stash 本批改动后同环境复跑失败类
```

## 评审门 / 回滚点

- 每批 commit 前：编译 + 单测全绿 + 分页等价冒烟（试点）；AC1/AC2 为试点硬门
- 回滚：revert 单批 commit（XML 从 git 历史恢复；Repository/Service 分页改动随批 revert）；主键切换 revert 独立 commit
