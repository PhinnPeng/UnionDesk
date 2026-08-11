# Implement — 项目最终 PRD 完善与双端功能清单

## 执行清单

1. **研究（trellis-research）** → 产出 task research/ 证据文件
   - CustomerWeb 页面勘察与功能状态（13 页：login/register/home/domains/tickets(new/list/detail)/chat/inbox/me/change-password）
   - AdminWeb 路由双端归属清单（平台端 `/platform/*` vs 业务域端根级，含 domain/* 与 system/*）
   - backlog Story 编号抽取（US-Sx-xx，映射到各功能）
   - prd.md V2.2 需要的现状事实核对（页面结构、功能清单扩展项）
   - 验证：research 输出覆盖 10 列所需全部证据，含页面/路由路径
2. **修订 `docs/product/prd.md` → V2.2**
   - 版本说明加行；§4.2 功能清单双端全量（端侧列）；§4.3 页面结构对齐；§5 详细设计编号对应；§4.4 自洽勾选；待确认项标「验收期回填」；§6.1 埋点命名占位
   - 验证：编号唯一；§4.2↔§4.3↔§5 自洽；无功能定义改动（git diff 复核）
3. **新建 `docs/product/feature-list.md`**
   - 总览图例 + 最终功能清单 + 客户端表 + 管理端表（平台/域两节）+ 功能清单说明 + 追踪索引
   - 验证：10 列齐全；编号与 prd.md 一致；状态术语与 inventory 对齐
4. **更新 `docs/README.md`** 结构登记
   - 验证：引用路径有效
5. **质量验证**
   - 自洽检查（编号↔页面↔Epic/Story）；`pnpm --dir UnionDeskWeb run check:utf8`
   - 验证：check:utf8 通过；git diff 仅 3 个文档文件
6. **评审门**：trellis-check（Agent 形式）复核 AC1–AC6 → 通过后交付

## 验证命令

```powershell
pnpm --dir UnionDeskWeb run check:utf8
git diff --stat   # 期望：docs/product/prd.md、docs/product/feature-list.md、docs/README.md
```

## 风险文件与回滚点

| 文件 | 风险 | 回滚 |
|:---|:---|:---|
| `docs/product/prd.md` | 中（权威文档，354 行） | `git restore docs/product/prd.md` |
| `docs/product/feature-list.md` | 低（新增） | 直接删除 |
| `docs/README.md` | 低 | `git restore` |

## 评审门（task.py start 前）

- [ ] prd.md / design.md / implement.md 经用户审阅
- [ ] implement.jsonl / check.jsonl 含真实条目
- [ ] 用户批准 start
