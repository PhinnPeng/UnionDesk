import { fetchDomainPriorityLevels } from "@uniondesk/shared";

import { fetchSlaConfig, updateSlaConfig, type SlaConfigView } from "#src/api/platform/sla";
import { AuthGuarded } from "#src/components/auth-guarded";
import { BasicContent } from "#src/components/basic-content";
import { DOMAIN_SLA_READ, DOMAIN_SLA_UPDATE } from "#src/pages/domain/domain-permissions";
import { MemberPicker } from "#src/pages/platform/components/member-picker";
import { useAuthStore } from "#src/store/auth";

import { DeleteOutlined } from "@ant-design/icons";
import {
	App,
	Button,
	Card,
	Checkbox,
	Collapse,
	DatePicker,
	Empty,
	Form,
	Input,
	InputNumber,
	Select,
	Switch,
	Tag,
	Typography,
} from "antd";
import type { Dayjs } from "dayjs";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";

import styles from "./index.module.less";

type ActionBlockKind = "assign" | "watchers";

type ActionBlock = {
	key: string
	kind: ActionBlockKind
	staffId?: number | number[]
};

type PriorityOption = {
	label: string
	value: number
};

const WEEKDAY_OPTIONS = [
	{ label: "周一", value: 1 },
	{ label: "周二", value: 2 },
	{ label: "周三", value: 3 },
	{ label: "周四", value: 4 },
	{ label: "周五", value: 5 },
	{ label: "周六", value: 6 },
	{ label: "周日", value: 7 },
];

const DEFAULT_WORKING_DAYS = [1, 2, 3, 4, 5];

const ACTION_TYPE_OPTIONS = [
	{ label: "更换处理人", value: "assign" },
	{ label: "添加关注人", value: "watchers" },
];

function resolveBusinessDomainId(
	defaultBusinessDomainId: string,
	accessibleDomains: Array<{ id: string }>,
): string {
	if (defaultBusinessDomainId) {
		return defaultBusinessDomainId;
	}
	const first = accessibleDomains[0];
	return first ? first.id : "";
}

function toNumber(value: unknown): number | null {
	const num = typeof value === "number" ? value : Number(value);
	return Number.isFinite(num) ? num : null;
}

function toHolidayList(value: unknown): string[] {
	return Array.isArray(value) ? value.filter((item): item is string => typeof item === "string") : [];
}

/** 域内单份 SLA 配置面板：响应时限 + 超时动作 + 工作日历 */
export default function DomainSlaPage() {
	const { message } = App.useApp();
	const defaultBusinessDomainId = useAuthStore(state => state.defaultBusinessDomainId);
	const accessibleDomains = useAuthStore(state => state.accessibleDomains);

	const domainId = useMemo(
		() => resolveBusinessDomainId(defaultBusinessDomainId, accessibleDomains ?? []),
		[accessibleDomains, defaultBusinessDomainId],
	);

	const [form] = Form.useForm();
	const blockSeqRef = useRef(0);
	const [loading, setLoading] = useState(false);
	const [saving, setSaving] = useState(false);
	const [blocks, setBlocks] = useState<ActionBlock[]>([]);
	const [holidays, setHolidays] = useState<string[]>([]);
	const [holidayPickerValue, setHolidayPickerValue] = useState<Dayjs | null>(null);
	const [actionPickerValue, setActionPickerValue] = useState<ActionBlockKind | undefined>(undefined);
	const [priorityOptions, setPriorityOptions] = useState<PriorityOption[]>([]);

	const escalateEnabled = Form.useWatch("escalateEnabled", form);

	// 升级目标档数据源（域优先级档）
	useEffect(() => {
		if (!domainId) {
			setPriorityOptions([]);
			return;
		}
		let cancelled = false;
		void fetchDomainPriorityLevels(domainId)
			.then(page => {
				if (!cancelled) {
					setPriorityOptions(page.items.map(item => ({ label: item.name, value: Number(item.id) })));
				}
			})
			.catch(() => {
				if (!cancelled) {
					setPriorityOptions([]);
				}
			});
		return () => {
			cancelled = true;
		};
	}, [domainId]);

	/** 配置 JSON → 表单字段 / 动态块 / 节假日 */
	const applyConfig = useCallback((config: SlaConfigView | null) => {
		const breachAction = config?.breachAction ?? {};
		const calendar = config?.calendar ?? {};

		const escalate = breachAction.escalate_priority;
		const escalateEnabledValue = typeof escalate === "object" && escalate !== null
			? (escalate as Record<string, unknown>).enabled === true
			: escalate === true;
		const escalateTarget = typeof escalate === "object" && escalate !== null
			? toNumber((escalate as Record<string, unknown>).to_priority_level_id)
			: null;

		const nextBlocks: ActionBlock[] = [];
		const assignId = toNumber(breachAction.assign_to_staff_account_id);
		if (assignId != null) {
			blockSeqRef.current += 1;
			nextBlocks.push({ key: `block-${blockSeqRef.current}`, kind: "assign", staffId: assignId });
		}
		const watcherIds = Array.isArray(breachAction.add_watcher_staff_account_ids)
			? breachAction.add_watcher_staff_account_ids
				.map(toNumber)
				.filter((id): id is number => id != null)
			: [];
		if (watcherIds.length > 0) {
			blockSeqRef.current += 1;
			nextBlocks.push({ key: `block-${blockSeqRef.current}`, kind: "watchers", staffId: watcherIds });
		}

		const workingDays = Array.isArray(calendar.working_days)
			? calendar.working_days
				.map(toNumber)
				.filter((day): day is number => day != null && day >= 1 && day <= 7)
			: [];

		form.setFieldsValue({
			firstResponseMinutes: config?.firstResponseMinutes ?? undefined,
			resolutionMinutes: config?.resolutionMinutes ?? undefined,
			escalateEnabled: escalateEnabledValue,
			escalateToPriorityLevelId: escalateTarget ?? undefined,
			workingDays: workingDays.length > 0 ? workingDays : DEFAULT_WORKING_DAYS,
			weekendWork: calendar.weekend_work === true,
		});
		setBlocks(nextBlocks);
		setHolidays(toHolidayList(calendar.holidays));
		setHolidayPickerValue(null);
		setActionPickerValue(undefined);
	}, [form]);

	const loadConfig = useCallback(async () => {
		if (!domainId) {
			applyConfig(null);
			return;
		}
		setLoading(true);
		try {
			const config = await fetchSlaConfig(domainId);
			applyConfig(config);
		}
		catch (error) {
			message.error(error instanceof Error ? error.message : "加载 SLA 配置失败");
			applyConfig(null);
		}
		finally {
			setLoading(false);
		}
	}, [applyConfig, domainId, message]);

	useEffect(() => {
		void loadConfig();
	}, [loadConfig]);

	// 动态块
	const addBlock = (kind: ActionBlockKind) => {
		blockSeqRef.current += 1;
		setBlocks(prev => [...prev, { key: `block-${blockSeqRef.current}`, kind }]);
	};

	const removeBlock = (key: string) => {
		setBlocks(prev => prev.filter(block => block.key !== key));
	};

	const patchBlock = (key: string, staffId: number | number[] | null) => {
		setBlocks(prev => prev.map(block => (
			block.key === key ? { ...block, staffId: staffId ?? undefined } : block
		)));
	};

	// 节假日
	const handleAddHoliday = (date: Dayjs | null) => {
		setHolidayPickerValue(null);
		if (!date) {
			return;
		}
		const text = date.format("YYYY-MM-DD");
		setHolidays(prev => (prev.includes(text) ? prev : [...prev, text].sort()));
	};

	const handleRemoveHoliday = (date: string) => {
		setHolidays(prev => prev.filter(item => item !== date));
	};

	// 可视化配置 → breachAction JSON
	const serializeBreachAction = useCallback((values: Record<string, unknown>): Record<string, unknown> => {
		const breachAction: Record<string, unknown> = {
			escalate_priority: {
				enabled: values.escalateEnabled === true,
				...(values.escalateToPriorityLevelId != null
					? { to_priority_level_id: Number(values.escalateToPriorityLevelId) }
					: {}),
			},
		};
		// 更换处理人块可加多个，JSON 仅一个槽位：最后一个有值的生效
		for (const block of blocks) {
			if (block.kind === "assign" && block.staffId != null) {
				breachAction.assign_to_staff_account_id = Number(block.staffId);
			}
		}
		// 添加关注人块：合并所有块内的关注人
		const watcherIds = blocks.flatMap(block => (
			block.kind === "watchers" && Array.isArray(block.staffId) ? block.staffId : []
		));
		if (watcherIds.length > 0) {
			breachAction.add_watcher_staff_account_ids = watcherIds;
		}
		return breachAction;
	}, [blocks]);

	// 可视化配置 → 日历 JSON
	const serializeCalendar = useCallback((values: Record<string, unknown>): Record<string, unknown> => ({
		working_days: Array.isArray(values.workingDays) ? values.workingDays.map(Number) : [],
		weekend_work: values.weekendWork === true,
		holidays,
	}), [holidays]);

	const handleSave = async () => {
		if (!domainId) {
			return;
		}
		const values = await form.validateFields().catch(() => null);
		if (!values) {
			return;
		}
		setSaving(true);
		try {
			await updateSlaConfig(domainId, {
				firstResponseMinutes: values.firstResponseMinutes ?? null,
				resolutionMinutes: values.resolutionMinutes ?? null,
				breachAction: serializeBreachAction(values),
				calendar: serializeCalendar(values),
			});
			message.success("SLA 配置已保存");
		}
		catch (error) {
			message.error(error instanceof Error ? error.message : "保存失败");
		}
		finally {
			setSaving(false);
		}
	};

	// 高级模式（JSON）：只读预览，与可视化配置实时同步
	const formAllValues = Form.useWatch([], form);
	const previewJson = useMemo(() => {
		if (!formAllValues) {
			return "";
		}
		return JSON.stringify({
			breachAction: serializeBreachAction(formAllValues),
			calendar: serializeCalendar(formAllValues),
		}, null, 2);
	}, [formAllValues, serializeBreachAction, serializeCalendar]);

	return (
		<BasicContent className="h-full bg-colorBgLayout">
			<AuthGuarded auth={DOMAIN_SLA_READ} fallback={<Empty description="无权限查看 SLA 配置" />}>
				{!domainId
					? <Empty description="暂无可用业务域" />
					: (
						<Card
							title="SLA 配置"
							bordered={false}
							extra={<Typography.Text type="secondary">作用于本域全部工单</Typography.Text>}
						>
							<Form form={form} layout="vertical" disabled={loading}>
								<div className="flex flex-col gap-4">
									<Card title="响应时限" size="small">
										<div className="grid gap-4 lg:grid-cols-2">
											<Form.Item
												name="firstResponseMinutes"
												label="首次响应"
												tooltip="工单超时未响应则触发超时动作；留空表示不启用"
											>
												<InputNumber className="w-full" min={0} placeholder="分钟，空=不启用" />
											</Form.Item>
											<Form.Item
												name="resolutionMinutes"
												label="解决响应"
												tooltip="工单超时未解决则触发超时动作；留空表示不启用"
											>
												<InputNumber className="w-full" min={0} placeholder="分钟，空=不启用" />
											</Form.Item>
										</div>
									</Card>

									<Card
										title="超时动作"
										size="small"
										extra={(
											<AuthGuarded auth={DOMAIN_SLA_UPDATE} fallback={null}>
												<Select
													placeholder="添加动作"
													style={{ width: 140 }}
													value={actionPickerValue}
													options={ACTION_TYPE_OPTIONS}
													onChange={(value) => {
														addBlock(value as ActionBlockKind);
														setActionPickerValue(undefined);
													}}
												/>
											</AuthGuarded>
										)}
									>
										<div className="flex flex-col gap-3">
											<Typography.Text type="secondary" className="text-xs">
												超时后自动执行；执行顺序固定：升级优先级 → 更换处理人 → 添加关注人，每工单仅执行一次。
											</Typography.Text>
											<div className="flex items-center gap-3">
												<Typography.Text className="w-32 shrink-0">升级优先级</Typography.Text>
												<Form.Item name="escalateEnabled" valuePropName="checked" noStyle>
													<Switch />
												</Form.Item>
												<Form.Item name="escalateToPriorityLevelId" noStyle>
													<Select
														style={{ flex: 1, minWidth: 200 }}
														allowClear
														disabled={escalateEnabled !== true}
														placeholder="目标优先级（留空=按序升到下一档）"
														options={priorityOptions}
													/>
												</Form.Item>
											</div>
											{blocks.map(block => (
												<div key={block.key} className="flex items-center gap-3">
													<Typography.Text className="w-32 shrink-0">
														{block.kind === "assign" ? "更换处理人" : "添加关注人"}
													</Typography.Text>
													<MemberPicker
														domainId={domainId}
														multiple={block.kind === "watchers"}
														value={block.staffId ?? (block.kind === "watchers" ? [] : null)}
														placeholder={block.kind === "assign"
															? "超时后强制指派处理人"
															: "超时后追加关注人（不覆盖已有）"}
														onChange={value => patchBlock(block.key, value)}
													/>
													<Button
														type="text"
														danger
														icon={<DeleteOutlined />}
														onClick={() => removeBlock(block.key)}
													/>
												</div>
											))}
										</div>
									</Card>

									<Card title="工作日历" size="small">
										<div className="flex flex-col gap-3">
											<Form.Item
												name="workingDays"
												label="工作日"
												rules={[{
													validator: (_, value: unknown) => (
														Array.isArray(value) && value.length > 0
															? Promise.resolve()
															: Promise.reject(new Error("请至少选择一天作为工作日"))
													),
												}]}
											>
												<Checkbox.Group options={WEEKDAY_OPTIONS} />
											</Form.Item>
											<Form.Item
												name="weekendWork"
												label="周末是否工作"
												valuePropName="checked"
												tooltip="开启后周六/周日计入 SLA 工时，否则不计"
											>
												<Switch />
											</Form.Item>
											<div>
												<Typography.Text>节假日</Typography.Text>
												<div className="mt-1 flex flex-wrap items-center gap-2">
													<DatePicker
														value={holidayPickerValue}
														format="YYYY-MM-DD"
														placeholder="选择日期添加节假日"
														onChange={handleAddHoliday}
													/>
													{holidays.length === 0
														? <Typography.Text type="secondary" className="text-xs">暂无节假日</Typography.Text>
														: holidays.map(date => (
															<Tag key={date} closable onClose={() => handleRemoveHoliday(date)}>
																{date}
															</Tag>
														))}
												</div>
											</div>
										</div>
									</Card>

									<Collapse
										ghost
										items={[{
											key: "advanced",
											label: "高级模式（JSON）",
											children: (
												<Input.TextArea
													readOnly
													rows={10}
													className={styles.jsonEditor}
													value={previewJson}
												/>
											),
										}]}
									/>
								</div>
							</Form>

							<div className="mt-4 flex items-center gap-3">
								<AuthGuarded auth={DOMAIN_SLA_UPDATE} fallback={null}>
									<Button type="primary" loading={saving} onClick={() => void handleSave()}>
										保存
									</Button>
								</AuthGuarded>
								<Button onClick={() => void loadConfig()}>重置</Button>
							</div>
						</Card>
					)}
			</AuthGuarded>
		</BasicContent>
	);
}
