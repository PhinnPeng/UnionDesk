# domain-blockword-management-s2-05 — Design Handoff

> mode: compact | generated: 2026-06-07 | phase: design

## Sources

| artifact | path |
|:---|:---|
| proposal | openspec/changes/domain-blockword-management-s2-05/proposal.md |
| design | openspec/changes/domain-blockword-management-s2-05/design.md |
| tasks | openspec/changes/domain-blockword-management-s2-05/tasks.md |
| delta spec | openspec/changes/domain-blockword-management-s2-05/specs/domain-blockword-management/spec.md |
| design doc | docs/superpowers/specs/2026-06-07-domain-blockword-management-s2-05-design.md |

## Scope excerpt

- Dual-layer blocked words: global (NULL domain_id) + domain-scoped
- List: keyword fuzzy search + pagination (P0PageResult)
- Create: single + batch (dedupe within batch and skip DB duplicates)
- Permissions: platform.blocked_word.* + platform.domain.control.blocked_word.*
- UI: 平台页与域 Tab **各自独立 TSX**（TableSearchForm + Table，AGENTS.md §2.7）
- New page /platform/blockwords; upgrade detail-blockwords.tsx
