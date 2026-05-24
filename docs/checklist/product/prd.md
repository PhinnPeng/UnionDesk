# PRD 修订清单

## 背景
`docs/product/prd.md` 审查结论为 `Return for Fixes`，需要先补齐边界、策略、验收和引用再进入下一轮评审。

## 目标
将 PRD 修订为可进入复审和开发拆分的状态。

## 任务

| ID | 负责人 | 事项 | 输入 | 输出 | 完成标准 | 状态 |
|----|--------|------|------|------|----------|------|
| R1 | 交互设计师 | 明确低代码表单设计器能力边界，并补 UI/UX 引用清单 | 审查意见、现有 PRD、现有 UX 资料 | `workspace/ux/prd-fix-notes.md` | 边界可执行，可支持后续设计 | Not Started |
| R2 | 后端工程师 | 补在线咨询排队策略与后端约束说明 | 审查意见、PRD 在线咨询章节 | `workspace/backend/prd-fix-notes.md` | 策略可落地 | Not Started |
| R3 | 质量工程师 | 拆分并补齐按特性的详细验收标准 | 审查意见、PRD 全文 | `workspace/qa/prd-acceptance-matrix.md` | 每个核心功能点可验证 | Not Started |
| R4 | 前端工程师 | 梳理 UX 启动所需设计依赖与实现前置条件 | 审查意见、PRD、UX 引用清单 | `workspace/frontend/prd-kickoff-deps.md` | 可明确前端启动所需输入 | Not Started |
| R5 | 代码审查师 | 对修订后的 PRD 进行复审 | R1-R4 输出 | 审查结论 | 无阻塞问题后进入下一步 | Pending Confirmation |

## 验收标准
- 5 个审查问题全部关闭。
- PRD 的引用路径与内容边界可追踪、可复审。
- 复审结论从 `Return for Fixes` 变更为可推进状态。
