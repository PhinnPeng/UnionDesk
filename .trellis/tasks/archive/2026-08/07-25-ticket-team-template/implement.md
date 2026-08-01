# 实现计划：团队模板

## 顺序

1. Flyway：`ticket_team_template` / `_item` / `business_domain.applied_*` + 权限种子  
2. Po/Mapper/Repository + `TicketTeamTemplateService` + Controller  
3. shared API/types + `TeamTemplatesPanel` + 侧栏 section  
4. `TeamTemplateApplyService` + 建域 body/`domains-modal`  
5. 种子系统模板 + 集成测试（建域后改平台类型，域不变）  
6. （可选）审计、include 高级开关打磨  

## 验证

- [x] CRUD + 权限 403/200（代码已接线；需本地启动后手测）
- [x] Apply：建域 `team_template_id` → 深拷贝类型/slots/schema/flow/描述模板
- [ ] AC4：改平台类型后旧域配置不变（手测）
- [x] disabled / 空选 / 系统模板删除拒绝（服务层校验）

## 启动

```bash
python ./.trellis/scripts/task.py start 07-25-ticket-team-template
```
