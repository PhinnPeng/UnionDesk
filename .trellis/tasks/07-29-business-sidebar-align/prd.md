# 业务域侧栏与事项配置壳对齐

## Goal

消除业务域端「侧栏收起/展开」时顶区分割线与页签/内容区的视觉错位，并让事项配置等满宽壳页与顶栏/页签左缘对齐，收起展开仍保持美观。

## Background

- 用户在业务域「事项配置」页标注：侧栏顶区分割线与主区页签底边不在同一水平线；内容区相对页签呈台阶缩进。
- 先前用 Cursor Canvas 做了示意，用户要求清理该示例，改用浏览器可交互演示。

## Confirmed Facts（仓库可证）

| 事实 | 锚点 |
|:---|:---|
| 顶栏高度 48 | `layout/constants.ts` `headerHeight` |
| 页签高度 35 | `layout/constants.ts` `tabbarHeight` |
| 折叠按钮高度 40 | `layout/constants.ts` `siderTriggerHeight` |
| 主区随侧栏宽 `paddingLeft` + `transition-all` | `layout/container-layout/index.tsx` |
| 收起时品牌区 = Logo 行 48 + Trigger 40，底部分割线 | `layout/widgets/logo/index.tsx` + `index.less` |
| 展开时品牌区高度 = 48，与顶栏底边对齐 | 同上 |
| `BasicContent` 固定 `p-4` | `components/basic-content/index.tsx` |
| 事项配置壳在 BasicContent 内 | `pages/domain/ticket-config/index.tsx` |
| 收起顶区≈90px vs 顶栏+页签=83px → Δ≈7px | 由上列常量推导 |

## Requirements

- R1: 侧栏**收起**时，顶区底部分割线 MUST 与「顶栏+页签」底边处于同一水平参考线（允许 0–1px 渲染误差）。
- R2: 事项配置页（及同类满宽壳）内容壳左缘 MUST 与页签内容区左缘对齐（取消相对页签的额外外框缩进台阶）。
- R3: 侧栏收起/展开过渡 MUST 保持现有 `paddingLeft` 动画机制，不得引入内容二次跳动。
- R4: 平台端布局与业务端共用同一套侧栏顶区高度策略时 MUST 不破坏展开态「品牌行对齐顶栏」的既有观感。

## Decisions

| 决策 | 结论 | 日期 |
|:---|:---|:---|
| 方案组合 | **A+B**：收起顶区高度对齐 `header+tabbar`，事项配置等目标页满宽壳 | 2026-07-30 |
| 满宽壳范围 | **仅** `/domain/ticket-config` 列表壳；类型设计器与其它域页保持默认 `p-4` | 2026-07-30 |

## Proposed Options（已决策）

| 方案 | 内容 | 结论 |
|:---|:---|:---|
| A | 收起态 `sidebar-brand` 总高 = `headerHeight + tabbarHeight`（无页签时仅为 header） | 采用（全局侧栏） |
| B | `/domain/ticket-config` 的 `BasicContent` 满宽（`p-0`） | 采用（仅列表壳） |
| A+B | 组合 | **已选** |

## Out of Scope

- 业务页内「事项类型」小节侧栏与全局菜单项对齐（两套导航，不强行齐）
- 重做双列导航 / mixed nav 布局
- Cursor Canvas 示意（已删除）
- 业务域其它占位/Card 页去 `p-4`
- 类型设计器满宽（可后续单独立项）

## Acceptance Criteria

- [x] AC1: 侧栏收起时，顶区底边与页签底边可视对齐（Δ≤1px；无页签时对齐顶栏底边）— 代码：`Logo` 收起高度 = header(+tabbar)
- [x] AC2: `/domain/ticket-config` 内容壳左缘与页签内容左缘对齐 — `BasicContent !p-0` + 去圆角
- [x] AC3: 侧栏展开/收起过渡无额外横向跳动 — 未改 `paddingLeft` 机制
- [x] AC4: 展开态品牌行仍对齐顶栏底边 — 展开态未改高度策略

## Implementation Notes

- A: `layout/widgets/logo` + `sider-trigger`（`fillCollapsedHeight`）
- B: `pages/domain/ticket-config/index.tsx` + `index.module.less`
- 演示页：`.trellis/tasks/07-29-business-sidebar-align/demo/align-demo.html`

## Open Questions

（无）

## Demo

- 交互示意：`.trellis/tasks/07-29-business-sidebar-align/demo/align-demo.html`（浏览器打开）
