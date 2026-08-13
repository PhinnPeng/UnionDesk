import {
	createSlaCalendar,
	createSlaRule,
	deleteSlaCalendar,
	deleteSlaRule,
	fetchSlaCalendars,
	fetchSlaRules,
	updateSlaCalendar,
	updateSlaRule,
	type SlaCalendarView,
	type SlaRuleView,
} from "#src/api/platform/sla";
import { AuthGuarded } from "#src/components/auth-guarded";
import { BasicContent } from "#src/components/basic-content";
import { ConfirmPopover } from "#src/components/confirm-popover";
import {
	DOMAIN_SLA_CREATE,
	DOMAIN_SLA_READ,
	DOMAIN_SLA_UPDATE,
} from "#src/pages/domain/domain-permissions";
import { useAuthStore } from "#src/store/auth";

import { DeleteOutlined, EditOutlined } from "@ant-design/icons";
import {
	App,
	Button,
	Card,
	Empty,
	Form,
	Input,
	InputNumber,
	Modal,
	Space,
	Switch,
	Table,
	Tabs,
	Tooltip,
	Typography,
} from "antd";
import type { TableColumnsType } from "antd";
import { useCallback, useEffect, useMemo, useState } from "react";

import styles from "./index.module.less";

type EditorKind = "rule" | "calendar" | null;

function resolveBusinessDomainId(
	defaultBusinessDomainId: number,
	accessibleDomains: Array<{ id: number }>,
): string {
	if (defaultBusinessDomainId > 0) {
		return String(defaultBusinessDomainId);
	}
	const first = accessibleDomains[0];
	return first ? String(first.id) : "";
}

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

function prettyJson(value: Record<string, unknown> | undefined) {
	return JSON.stringify(value ?? {}, null, 2);
}

export default function DomainSlaPage() {
	const { message } = App.useApp();
	const defaultBusinessDomainId = useAuthStore(state => state.defaultBusinessDomainId);
	const accessibleDomains = useAuthStore(state => state.accessibleDomains);

	const domainId = useMemo(
		() => resolveBusinessDomainId(defaultBusinessDomainId, accessibleDomains ?? []),
		[accessibleDomains, defaultBusinessDomainId],
	);
	const numericDomainId = Number(domainId);

	const [activeTab, setActiveTab] = useState<"rules" | "calendars">("rules");
	const [ruleRows, setRuleRows] = useState<SlaRuleView[]>([]);
	const [ruleTotal, setRuleTotal] = useState(0);
	const [calendarRows, setCalendarRows] = useState<SlaCalendarView[]>([]);
	const [calendarTotal, setCalendarTotal] = useState(0);
	const [page, setPage] = useState(1);
	const [pageSize, setPageSize] = useState(20);
	const [loading, setLoading] = useState(false);
	const [editorKind, setEditorKind] = useState<EditorKind>(null);
	const [editingRule, setEditingRule] = useState<SlaRuleView | null>(null);
	const [editingCalendar, setEditingCalendar] = useState<SlaCalendarView | null>(null);
	const [form] = Form.useForm();

	const loadData = useCallback(async (nextPage = page, nextPageSize = pageSize) => {
		if (!numericDomainId) {
			setRuleRows([]);
			setRuleTotal(0);
			setCalendarRows([]);
			setCalendarTotal(0);
			return;
		}
		setLoading(true);
		try {
			if (activeTab === "rules") {
				const result = await fetchSlaRules(numericDomainId, { page: nextPage, page_size: nextPageSize });
				setRuleRows(result.list);
				setRuleTotal(result.total);
			}
			else {
				const result = await fetchSlaCalendars(numericDomainId, { page: nextPage, page_size: nextPageSize });
				setCalendarRows(result.list);
				setCalendarTotal(result.total);
			}
			setPage(nextPage);
			setPageSize(nextPageSize);
		}
		catch (error) {
			message.error(error instanceof Error ? error.message : "加载 SLA 数据失败");
		}
		finally {
			setLoading(false);
		}
	}, [activeTab, numericDomainId, message, page, pageSize]);

	useEffect(() => {
		void loadData(1, pageSize);
		// eslint-disable-next-line react-hooks/exhaustive-deps -- 域或 Tab 切换时回到第一页
	}, [domainId, activeTab]);

	const handleTabChange = (key: string) => {
		setActiveTab(key as "rules" | "calendars");
	};

	const openRuleEditor = (row?: SlaRuleView) => {
		setEditorKind("rule");
		setEditingRule(row ?? null);
		form.setFieldsValue({
			name: row?.name ?? "",
			ticketTypeId: row?.ticketTypeId ?? undefined,
			priorityLevelId: row?.priorityLevelId ?? undefined,
			calendarId: row?.calendarId ?? undefined,
			firstResponseMinutes: row?.firstResponseMinutes ?? undefined,
			resolutionMinutes: row?.resolutionMinutes ?? undefined,
			isUrgentConfig: row?.isUrgentConfig ?? false,
			breachActionText: prettyJson(row?.breachAction as Record<string, unknown> | undefined),
		});
	};

	const openCalendarEditor = (row?: SlaCalendarView) => {
		setEditorKind("calendar");
		setEditingCalendar(row ?? null);
		form.setFieldsValue({
			name: row?.name ?? "",
			configText: prettyJson(row?.config),
		});
	};

	const closeEditor = () => {
		setEditorKind(null);
		setEditingRule(null);
		setEditingCalendar(null);
	};

	const submitEditor = async () => {
		if (!numericDomainId || !editorKind) {
			return;
		}
		const values = await form.validateFields().catch(() => null);
		if (!values) {
			return;
		}
		try {
			if (editorKind === "rule") {
				const payload = {
					name: values.name,
					ticketTypeId: values.ticketTypeId ?? null,
					priorityLevelId: values.priorityLevelId ?? null,
					calendarId: values.calendarId ?? null,
					firstResponseMinutes: values.firstResponseMinutes ?? null,
					resolutionMinutes: values.resolutionMinutes ?? null,
					isUrgentConfig: !!values.isUrgentConfig,
					breachAction: safeJson(values.breachActionText),
				};
				if (editingRule) {
					await updateSlaRule(numericDomainId, editingRule.id, payload);
					message.success("SLA 规则已更新");
				}
				else {
					await createSlaRule(numericDomainId, payload);
					message.success("SLA 规则已创建");
				}
			}
			else {
				const payload = {
					name: values.name,
					config: safeJson(values.configText),
				};
				if (editingCalendar) {
					await updateSlaCalendar(numericDomainId, editingCalendar.id, payload);
					message.success("SLA 日历已更新");
				}
				else {
					await createSlaCalendar(numericDomainId, payload);
					message.success("SLA 日历已创建");
				}
			}
			closeEditor();
			await loadData(page, pageSize);
		}
		catch (error) {
			message.error(error instanceof Error ? error.message : "保存失败");
		}
	};

	const handleDeleteRule = useCallback(async (ruleId: number) => {
		if (!numericDomainId) {
			return;
		}
		try {
			await deleteSlaRule(numericDomainId, ruleId);
			message.success("SLA 规则已删除");
			await loadData(page, pageSize);
		}
		catch (error) {
			message.error(error instanceof Error ? error.message : "删除失败");
		}
	}, [numericDomainId, loadData, message, page, pageSize]);

	const handleDeleteCalendar = useCallback(async (calendarId: number) => {
		if (!numericDomainId) {
			return;
		}
		try {
			await deleteSlaCalendar(numericDomainId, calendarId);
			message.success("SLA 日历已删除");
			await loadData(page, pageSize);
		}
		catch (error) {
			message.error(error instanceof Error ? error.message : "删除失败");
		}
	}, [numericDomainId, loadData, message, page, pageSize]);

	const ruleColumns: TableColumnsType<SlaRuleView> = [
		{ title: "名称", dataIndex: "name", width: 180 },
		{ title: "工单类型", dataIndex: "ticketTypeId", width: 110, render: value => value ?? "-" },
		{ title: "优先级", dataIndex: "priorityLevelId", width: 110, render: value => value ?? "-" },
		{ title: "日历", dataIndex: "calendarId", width: 110, render: value => value ?? "-" },
		{ title: "首响分钟", dataIndex: "firstResponseMinutes", width: 120, render: value => value ?? "-" },
		{ title: "解决分钟", dataIndex: "resolutionMinutes", width: 120, render: value => value ?? "-" },
		{ title: "升级优先级", dataIndex: "isUrgentConfig", width: 110, render: value => value ? "是" : "否" },
		{ title: "触发动作", dataIndex: "breachAction", ellipsis: true, render: value => JSON.stringify(value ?? {}) },
		{
			title: "操作",
			key: "actions",
			width: 100,
			render: (_, row) => (
				<Space>
					<AuthGuarded auth={DOMAIN_SLA_UPDATE} fallback={null}>
						<Tooltip title="编辑">
							<Button type="link" size="small" icon={<EditOutlined />} onClick={() => openRuleEditor(row)} />
						</Tooltip>
					</AuthGuarded>
					<AuthGuarded auth={DOMAIN_SLA_UPDATE} fallback={null}>
						<Tooltip title="删除">
							<ConfirmPopover title="确认删除该 SLA 规则？" onConfirm={() => handleDeleteRule(row.id)}>
								<Button type="link" size="small" danger icon={<DeleteOutlined />} />
							</ConfirmPopover>
						</Tooltip>
					</AuthGuarded>
				</Space>
			),
		},
	];

	const calendarColumns: TableColumnsType<SlaCalendarView> = [
		{ title: "名称", dataIndex: "name", width: 180 },
		{ title: "配置", dataIndex: "config", ellipsis: true, render: value => JSON.stringify(value ?? {}) },
		{
			title: "操作",
			key: "actions",
			width: 130,
			render: (_, row) => (
				<Space>
					<AuthGuarded auth={DOMAIN_SLA_UPDATE} fallback={null}>
						<Tooltip title="编辑">
							<Button type="link" size="small" icon={<EditOutlined />} onClick={() => openCalendarEditor(row)} />
						</Tooltip>
					</AuthGuarded>
					<AuthGuarded auth={DOMAIN_SLA_UPDATE} fallback={null}>
						<Tooltip title="删除">
							<ConfirmPopover title="确认删除该 SLA 日历？" onConfirm={() => handleDeleteCalendar(row.id)}>
								<Button type="link" size="small" danger icon={<DeleteOutlined />} />
							</ConfirmPopover>
						</Tooltip>
					</AuthGuarded>
				</Space>
			),
		},
	];

	return (
		<BasicContent className="h-full bg-colorBgLayout">
			<AuthGuarded auth={DOMAIN_SLA_READ} fallback={<Empty description="无权限查看 SLA 管理" />}>
				{!domainId
					? <Empty description="暂无可用业务域" />
					: (
						<Card
							title="SLA 管理"
							bordered={false}
							extra={<Typography.Text type="secondary">规则、日历与优先级关联统一维护</Typography.Text>}
						>
							<Tabs
								activeKey={activeTab}
								onChange={handleTabChange}
								items={[
									{ key: "rules", label: "SLA 规则" },
									{ key: "calendars", label: "SLA 日历" },
								]}
								tabBarExtraContent={(
									<AuthGuarded auth={DOMAIN_SLA_CREATE} fallback={null}>
										<Button type="primary" onClick={() => activeTab === "rules" ? openRuleEditor() : openCalendarEditor()}>
											新增{activeTab === "rules" ? "规则" : "日历"}
										</Button>
									</AuthGuarded>
								)}
							/>

							{activeTab === "rules" ? (
								<Table<SlaRuleView>
									rowKey="id"
									loading={loading}
									columns={ruleColumns}
									dataSource={ruleRows}
									pagination={{
										current: page,
										pageSize,
										total: ruleTotal,
										showSizeChanger: true,
										showTotal: value => `共 ${value} 条`,
										onChange: (nextPage, nextPageSize) => {
											void loadData(nextPage, nextPageSize);
										},
									}}
									scroll={{ x: 1200 }}
									locale={{ emptyText: <Empty description="暂无 SLA 规则" /> }}
								/>
							) : (
								<Table<SlaCalendarView>
									rowKey="id"
									loading={loading}
									columns={calendarColumns}
									dataSource={calendarRows}
									pagination={{
										current: page,
										pageSize,
										total: calendarTotal,
										showSizeChanger: true,
										showTotal: value => `共 ${value} 条`,
										onChange: (nextPage, nextPageSize) => {
											void loadData(nextPage, nextPageSize);
										},
									}}
									scroll={{ x: 1000 }}
									locale={{ emptyText: <Empty description="暂无 SLA 日历" /> }}
								/>
							)}
						</Card>
					)}
			</AuthGuarded>

			<Modal
				title={editorKind === "rule" ? (editingRule ? "编辑 SLA 规则" : "新增 SLA 规则") : editingCalendar ? "编辑 SLA 日历" : "新增 SLA 日历"}
				open={editorKind !== null}
				onCancel={closeEditor}
				onOk={() => void submitEditor()}
				destroyOnClose
				width={720}
			>
				<Form form={form} layout="vertical">
					{editorKind === "rule" ? (
						<>
							<Form.Item name="name" label="规则名称" rules={[{ required: true, message: "请输入规则名称" }]}>
								<Input placeholder="如 默认首响规则" />
							</Form.Item>
							<div className="grid gap-4 lg:grid-cols-2">
								<Form.Item name="ticketTypeId" label="工单类型 ID">
									<InputNumber className="w-full" min={1} />
								</Form.Item>
								<Form.Item name="priorityLevelId" label="优先级 ID">
									<InputNumber className="w-full" min={1} />
								</Form.Item>
								<Form.Item name="calendarId" label="日历 ID">
									<InputNumber className="w-full" min={1} />
								</Form.Item>
								<Form.Item name="isUrgentConfig" label="紧急配置" valuePropName="checked">
									<Switch />
								</Form.Item>
								<Form.Item name="firstResponseMinutes" label="首响分钟">
									<InputNumber className="w-full" min={0} />
								</Form.Item>
								<Form.Item name="resolutionMinutes" label="解决分钟">
									<InputNumber className="w-full" min={0} />
								</Form.Item>
							</div>
							<Form.Item name="breachActionText" label="超时动作 JSON">
								<Input.TextArea rows={5} className={styles.jsonEditor} placeholder='例如 {"raise_priority_to":"urgent"}' />
							</Form.Item>
						</>
					) : (
						<>
							<Form.Item name="name" label="日历名称" rules={[{ required: true, message: "请输入日历名称" }]}>
								<Input placeholder="如 工作日历" />
							</Form.Item>
							<Form.Item name="configText" label="日历配置 JSON">
								<Input.TextArea rows={8} className={styles.jsonEditor} placeholder='例如 {"timezone":"Asia/Shanghai"}' />
							</Form.Item>
						</>
					)}
				</Form>
			</Modal>
		</BasicContent>
	);
}
