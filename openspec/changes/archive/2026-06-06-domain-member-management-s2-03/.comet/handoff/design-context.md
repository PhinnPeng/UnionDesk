# Design Context Handoff

- **change**: domain-member-management-s2-03
- **phase**: design
- **mode**: compact
- **generated**: 2026-06-06

## Source: proposal.md

US-S2-03 员工管理 Tab 需从只读升级为全操作；补 staff-candidates、PUT status、shared API、Flyway domain.member.*。

## Source: backlog-stories.md US-S2-03

AC: 添加+角色、改角色+软删+保护规则、启停 API、shared 封装、Flyway 按钮。

## Source: DomainMemberController.java (existing)

已有 GET/POST members、PUT roles、DELETE member；缺 staff-candidates 与 status。
