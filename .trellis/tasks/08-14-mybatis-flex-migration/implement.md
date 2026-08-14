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

## 阶段 1：按模块分批（每批：实体注解 + 接口转换 + Repository/Service 分页切 Page + 删 XML + 验证 + commit）

- [ ] 1.1 `iam` 批次（auth/ + iam/）
- [ ] 1.2 `support` 批次（blockedword 已完成试点；含 audit/attachment/notification/sla/dashboard/config/common）
- [ ] 1.3 `domain` 批次
- [ ] 1.4 `ticket` 批次
- [ ] 每批验证：`./mvnw -B -ntp compile` + 对应模块单测；复杂联表/UNION（如 `selectEffectiveGrants`）保留 XML 或 `@Select`
- [ ] 每批 commit 前自查：无超出本批范围的改动（AGENTS.md 精准修改约束）

## 阶段 2：主键生成器切换（R4，独立 commit 可回滚）

- [ ] 2.1 用户指定「部分模型」清单 → 实体 `@Id` 改为 `@Id(keyType = KeyType.Generator, value = KeyGenerators.snowFlakeId)`
- [ ] 2.2 验证：插入后 id 由生成器回填（19 位大数）、既有数据不受影响、外键引用正常（AC4）

## 阶段 3：全量验证与收尾

- [ ] 3.1 停 dev 服务 → `./mvnw -B -ntp test` 全量 → 与 master 基线对照：**无新增失败**、Flex 绑定错误 = 0（AC3）
- [ ] 3.2 红线自查：grep 无新增 `prepareAuth`/`DataScope`/分页拦截器；数据权限过滤均在 service 显式（AC5）
- [ ] 3.3 汇总报告；`task.py` 收尾（归档/提交）

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
