import { fetchDomainPriorityLevels, fetchDomainTicketTypes } from "@uniondesk/shared";

import { fetchSlaCalendars } from "#src/api/platform/sla";
import { MemberPicker } from "#src/pages/platform/components/member-picker";

import { Collapse, Form, Input, InputNumber, Select, Switch, Tooltip, Typography } from "antd";
import type { FormInstance } from "antd";
import { useCallback, useEffect, useMemo, useState } from "react";

import styles from "./index.module.less";

interface SlaRuleFormProps {
	form: FormInstance;
	/** 数据源业务域（事项类型/优先级/日历/成员）；为空时对应控件不可用 */
	domainId?: string;
	/** 全局默认规则模式：隐藏域级字段（事项类型/优先级/日历/紧急配置） */
	global?: boolean;
}

type SelectOption = {
	label: string
	value: string
};

function safeJson(value: string | null | undefined) {
	if (!value) {
		return {};
	}
	try {
		return JSON.parse(value) as Record<string, unknown>;
	}
	catch {
		return {};
	}
}

function prettyJson(value: Record<string, unknown>) {
	return JSON.stringify(value ?? {}, null, 2);
}

function toNumber(value: unknown): number | null {
	const num = typeof value === "number" ? value : Number(value);
	return Number.isFinite(num) ? num : null;
}

/**
 * SLA 规则编辑器（域端与平台端共用）：
 * - 事项类型/优先级/日历下拉选择（不再手填 ID），日历为预留能力 disabled
 * - 超时动作可视化配置，与「高级模式（JSON）」双向同步（JSON 文本为唯一数据源）
 */
export function SlaRuleForm({ form, domainId, global = false }: SlaRuleFormProps) {
	const [ticketTypeOptions, setTicketTypeOptions] = useState<SelectOption[]>([]);
	const [priorityOptions, setPriorityOptions] = useState<SelectOption[]>([]);
	const [calendarOptions, setCalendarOptions] = useState<SelectOption[]>([]);

	useEffect(() => {
		if (!domainId) {
			setTicketTypeOptions([]);
			setPriorityOptions([]);
			setCalendarOptions([]);
			return;
		}
		let cancelled = false;
		void Promise.all([
			fetchDomainTicketTypes(domainId).then(items => items.map(item => ({ label: item.name, value: String(item.id) }))),
			fetchDomainPriorityLevels(domainId).then(page => page.items.map(item => ({ label: item.name, value: String(item.id) }))),
			fetchSlaCalendars(domainId, { page: 1, page_size: 100 }).then(page => page.list.map(item => ({ label: item.name, value: String(item.id) }))),
		])
			.then(([types, priorities, calendars]) => {
				if (cancelled) {
					return;
				}
				setTicketTypeOptions(types);
				setPriorityOptions(priorities);
				setCalendarOptions(calendars);
			})
			.catch(() => {
				if (!cancelled) {
					setTicketTypeOptions([]);
					setPriorityOptions([]);
					setCalendarOptions([]);
				}
			});
		return () => {
			cancelled = true;
		};
	}, [domainId]);

	// 超时动作：breachActionText（JSON 文本）为唯一数据源，可视化控件由 JSON 派生，
	// 可视化改动经 patchBreachAction 序列化回 JSON；JSON 文本改动自动反解析回可视化控件
	const breachActionText = Form.useWatch("breachActionText", form);
	const breachAction = useMemo(() => safeJson(breachActionText), [breachActionText]);

	const patchBreachAction = useCallback((updater: (action: Record<string, unknown>) => Record<string, unknown>) => {
		form.setFieldValue("breachActionText", prettyJson(updater(safeJson(breachActionText))));
	}, [breachActionText, form]);

	const assignToStaffAccountId = toNumber(breachAction.assign_to_staff_account_id);
	const watcherStaffAccountIds = Array.isArray(breachAction.add_watcher_staff_account_ids)
		? breachAction.add_watcher_staff_account_ids
			.map(toNumber)
			.filter((id): id is number => id != null)
		: [];

	return (
		<>
			<Form.Item name="name" label="规则名称" rules={[{ required: true, message: "请输入规则名称" }]}>
				<Input placeholder="如 默认首响规则" />
			</Form.Item>
			<div className="grid gap-4 lg:grid-cols-2">
				{!global && (
					<>
						<Form.Item name="ticketTypeId" label="事项类型">
							<Select allowClear placeholder="全部类型（域默认）" options={ticketTypeOptions} />
						</Form.Item>
						<Form.Item name="priorityLevelId" label="优先级">
							<Select allowClear placeholder="全部优先级" options={priorityOptions} />
						</Form.Item>
						<Form.Item name="calendarId" label="工作日历">
							<Tooltip title="工作日历计算暂未启用（预留）">
								<span className="block w-full">
									<Select disabled allowClear placeholder="工作日历计算暂未启用（预留）" options={calendarOptions} />
								</span>
							</Tooltip>
						</Form.Item>
						<Form.Item name="isUrgentConfig" label="紧急配置" valuePropName="checked">
							{/* 遗留死字段：全链路未参与匹配与计算，仅保留兼容 */}
							<Switch />
						</Form.Item>
					</>
				)}
				<Form.Item name="firstResponseMinutes" label="首响分钟">
					<InputNumber className="w-full" min={0} />
				</Form.Item>
				<Form.Item name="resolutionMinutes" label="解决分钟">
					<InputNumber className="w-full" min={0} />
				</Form.Item>
			</div>
			<Form.Item label="超时动作">
				<div className="flex flex-col gap-3 rounded-md border border-colorBorder p-3">
					<div className="flex items-center justify-between">
						<Typography.Text>超时按序升级优先级</Typography.Text>
						<Switch
							checked={breachAction.escalate_priority === true}
							onChange={checked => patchBreachAction(action => {
								const next = { ...action };
								if (checked) {
									next.escalate_priority = true;
								}
								else {
									delete next.escalate_priority;
								}
								return next;
							})}
						/>
					</div>
					<div className="flex items-center gap-3">
						<Typography.Text className="w-32 shrink-0">更换处理人</Typography.Text>
						<MemberPicker
							domainId={domainId}
							value={assignToStaffAccountId}
							placeholder="超时后强制指派处理人（可选）"
							onChange={value => patchBreachAction(action => {
								const next = { ...action };
								if (value == null) {
									delete next.assign_to_staff_account_id;
								}
								else {
									next.assign_to_staff_account_id = Number(value);
								}
								return next;
							})}
						/>
					</div>
					<div className="flex items-center gap-3">
						<Typography.Text className="w-32 shrink-0">添加关注人</Typography.Text>
						<MemberPicker
							domainId={domainId}
							multiple
							value={watcherStaffAccountIds}
							placeholder="超时后追加关注人（不覆盖已有）"
							onChange={value => patchBreachAction(action => {
								const next = { ...action };
								const ids = Array.isArray(value) ? value.map(Number) : [];
								if (ids.length > 0) {
									next.add_watcher_staff_account_ids = ids;
								}
								else {
									delete next.add_watcher_staff_account_ids;
								}
								return next;
							})}
						/>
					</div>
					<Typography.Text type="secondary" className="text-xs">
						超时后自动执行；执行顺序固定：升级优先级 → 更换处理人 → 添加关注人，每工单仅执行一次。
					</Typography.Text>
				</div>
			</Form.Item>
			<Collapse
				ghost
				items={[{
					key: "advanced",
					label: "高级模式（JSON）",
					children: (
						<Form.Item name="breachActionText" label="超时动作 JSON">
							<Input.TextArea rows={5} className={styles.jsonEditor} placeholder='例如 {"raise_priority_to":"urgent"}' />
						</Form.Item>
					),
				}]}
			/>
		</>
	);
}
