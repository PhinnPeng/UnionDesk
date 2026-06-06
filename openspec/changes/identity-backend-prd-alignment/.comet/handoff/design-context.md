# Design Handoff — identity-backend-prd-alignment

> **mode:** compact (manual fallback)  
> **design_doc:** `docs/superpowers/specs/2026-06-06-identity-scheme-a-design.md`  
> **canonical_spec:** openspec

## 决策摘要（2026-06-06 精简）

- L0/L1/L2 合一表映射；停写 user_account；域 RBAC 单轨
- 340001：DROP 11 条身份域 FK + ADD 列 + 回填 + 角色补录
- 引用完整性：写路径 Service 校验（无独立 Guard 类）
- `merged_into_id`：只读 resolve；禁止向已合并主体挂新账号；合并 API 后续 Story

## Source paths

| 文件 | 角色 |
|:---|:---|
| `openspec/changes/identity-backend-prd-alignment/proposal.md` | Why / What |
| `openspec/changes/identity-backend-prd-alignment/design.md` | 决策索引 D1–D7 |
| `openspec/changes/identity-backend-prd-alignment/tasks.md` | 实现任务 |
| `openspec/changes/identity-backend-prd-alignment/specs/*/spec.md` | Delta 验收 |

完整技术设计见 Superpowers Design Doc（含 §4 数据结构说明）。
