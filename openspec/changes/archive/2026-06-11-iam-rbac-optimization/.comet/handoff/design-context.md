# Comet Design Handoff

- Change: iam-rbac-optimization
- Phase: design
- Mode: compact
- Context hash: pending-manual

Generated-by: agent-manual (Git Bash unavailable for comet-handoff.sh)

OpenSpec remains the canonical capability spec.

## Source: openspec/changes/iam-rbac-optimization/proposal.md

S2 权限登记漂移：码表分裂、域详情功能不可见、域级鉴权重复。目标：Catalog SSOT + 三层命名 + registry + Interceptor domainId。

## Source: openspec/changes/iam-rbac-optimization/design.md

Phase A 治理 + Phase B 鉴权/前端；DomainControlFeatureRegistry；@RequirePermission(domainId)。

## Source: openspec/changes/iam-rbac-optimization/specs/iam-rbac-governance/spec.md

L1/L2/L3 命名；Catalog 校验；registry 驱动侧栏；域级 API 鉴权；Flyway 模板。
