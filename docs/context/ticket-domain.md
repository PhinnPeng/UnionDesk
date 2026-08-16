# 工单域（Ticket Domain）Context

工单处理域：工单从创建、SLA 计时保障、客服处理到满意度评价的领域语言，供员工端与域管理后台使用。

## Language

**SLA 配置（SLA Config）**：
业务域内唯一一份的服务等级配置，含响应时限（首次响应/解决响应）与超时动作；作用于该域所有工单。
_Avoid_: SLA 规则、SLA 策略

**首次响应（First Response）**：
工单创建后客服首次回复的时限（分钟）。超时后触发超时动作。
_Avoid_: 首响、第一响应

**解决响应（Resolution）**：
工单从创建到解决的时限（分钟）。超时后触发超时动作。
_Avoid_: 解决时限、处理时限

**超时动作（Breach Action）**：
SLA 时限违约后自动执行的动作：升级优先级（固定块）、更换处理人（动态块）、添加关注人（动态块）；每工单仅执行一次。
_Avoid_: 违约动作、SLA 惩罚

**工作日历（Working Calendar）**：
SLA 计时依据：工作日（周几）、周末是否工作、节假日列表；非工作日不计入 SLA 工时。
_Avoid_: 日历配置、排班

**紧急配置（Urgent Config）**：
为紧急优先级工单预留的独立时限配置（本轮后置，模型已预留）。
_Avoid_: 紧急 SLA、加急配置

**SLA 状态（SLA Status）**：
工单 SLA 计时状态：tracking（正常计时）/ breached（已超时）/ stopped（已结束，工单到终态）。
_Avoid_: SLA 生命周期、违约状态
