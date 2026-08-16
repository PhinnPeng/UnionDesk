import type { BusinessDomainView, PlatformSlaRuleView } from "@uniondesk/shared";
import {
	createGlobalSlaRule,
	deleteGlobalSlaRule,
	fetchGlobalSlaRules,
	updateGlobalSlaRule,
} from "@uniondesk/shared";

import { fetchBusinessDomains } from "#src/api/platform/domain";
import {
	createSlaCalendar,
	createSlaRule,
	deleteSlaRule,
	fetchSlaCalendars,
	fetchSlaRules,
	updateSlaCalendar,
	updateSlaRule,
	type SlaCalendarView,
	type SlaRuleView,
} from "#src/api/platform/sla";
import { BasicContent } from "#src/components/basic-content";
import { SlaRuleForm } from "#src/pages/domain/sla/sla-rule-form";

import { DeleteOutlined, EditOutlined } from "@ant-design/icons";
import { App, Button, Card, Form, Input, Modal, Segmented, Select, Space, Table, Tabs, Tooltip, Typography } from "antd";
import type { TableColumnsType } from "antd";
import { useEffect, useMemo, useState } from "react";

type Section = "global" | "domain";
type EditorKind = "rule" | "calendar" | null;

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

export default function PlatformSlaManagement() {
	const { message } = App.useApp();
	const [section, setSection] = useState<Section>("global");
	const [domains, setDomains] = useState<BusinessDomainView[]>([]);
	const [domainId, setDomainId] = useState<string>();
	const [activeTab, setActiveTab] = useState<"rules" | "calendars">("rules");
	const [ruleRows, setRuleRows] = useState<SlaRuleView[]>([]);
	const [calendarRows, setCalendarRows] = useState<SlaCalendarView[]>([]);
	const [loading, setLoading] = useState(false);
	const [editorKind, setEditorKind] = useState<EditorKind>(null);
	const [editingRule, setEditingRule] = useState<SlaRuleView | null>(null);
	const [editingCalendar, setEditingCalendar] = useState<SlaCalendarView | null>(null);
	const [form] = Form.useForm();

	// 全局默认规则（平台级跨域兜底）
	const [globalRuleRows, setGlobalRuleRows] = useState<PlatformSlaRuleView[]>([]);
	const [globalLoading, setGlobalLoading] = useState(false);
	const [globalEditorOpen, setGlobalEditorOpen] = useState(false);
	const [editingGlobalRule, setEditingGlobalRule] = useState<PlatformSlaRuleView | null>(null);
	const [globalForm] = Form.useForm();

	useEffect(() => {
		let ignore = false;
		void (async () => {
			try {
				const list = await fetchBusinessDomains();
				if (ignore) {
					return;
				}
				setDomains(list);
				setDomainId(list[0]?.id);
			}
			catch (error) {
				message.error(error instanceof Error ? error.message : "加载业务域失败");
			}
		})();
		return () => {
			ignore = true;
		};
	}, [message]);

	const domainOptions = useMemo(() => domains.map(domain => ({
		label: `${domain.name} / ${domain.code}`,
		value: domain.id,
	})), [domains]);

	const loadGlobalRules = async () => {
		setGlobalLoading(true);
		try {
			const page = await fetchGlobalSlaRules({ page: 1, page_size: 100 });
			setGlobalRuleRows(page.list);
		}
		catch (error) {
			message.error(error instanceof Error ? error.message : "加载全局 SLA 规则失败");
		}
		finally {
			setGlobalLoading(false);
		}
	};

	useEffect(() => {
		if (section === "global") {
			void loadGlobalRules();
		}
		// eslint-disable-next-line react-hooks/exhaustive-deps
	}, [section]);

	const loadData = async () => {
		if (!domainId) {
			return;
		}
		setLoading(true);
		try {
			if (activeTab === "rules") {
				const page = await fetchSlaRules(domainId, { page: 1, page_size: 100 });
				setRuleRows(page.list);
			}
			else {
				const page = await fetchSlaCalendars(domainId, { page: 1, page_size: 100 });
				setCalendarRows(page.list);
			}
		}
		catch (error) {
			message.error(error instanceof Error ? error.message : "加载 SLA 数据失败");
		}
		finally {
			setLoading(false);
		}
	};

	useEffect(() => {
		void loadData();
		// eslint-disable-next-line react-hooks/exhaustive-deps
	}, [domainId, activeTab]);

	const openGlobalEditor = (row?: PlatformSlaRuleView) => {
		setEditingGlobalRule(row ?? null);
		globalForm.setFieldsValue({
			name: row?.name ?? "",
			firstResponseMinutes: row?.firstResponseMinutes ?? undefined,
			resolutionMinutes: row?.resolutionMinutes ?? undefined,
			breachActionText: prettyJson(row?.breachAction as Record<string, unknown> | undefined),
		});
		setGlobalEditorOpen(true);
	};

	const submitGlobalEditor = async () => {
		const values = await globalForm.validateFields().catch(() => null);
		if (!values) {
			return;
		}
		try {
			const payload = {
				name: values.name,
				firstResponseMinutes: values.firstResponseMinutes ?? null,
				resolutionMinutes: values.resolutionMinutes ?? null,
				breachAction: safeJson(values.breachActionText),
			};
			if (editingGlobalRule) {
				await updateGlobalSlaRule(editingGlobalRule.id, payload);
				message.success("全局默认规则已更新");
			}
			else {
				await createGlobalSlaRule(payload);
				message.success("全局默认规则已创建");
			}
			setGlobalEditorOpen(false);
			setEditingGlobalRule(null);
			await loadGlobalRules();
		}
		catch (error) {
			message.error(error instanceof Error ? error.message : "保存失败");
		}
	};

	const handleDeleteGlobalRule = async (ruleId: string) => {
		try {
			await deleteGlobalSlaRule(ruleId);
			message.success("全局默认规则已删除");
			await loadGlobalRules();
		}
		catch (error) {
			message.error(error instanceof Error ? error.message : "删除失败");
		}
	};

	const openRuleEditor = (row?: SlaRuleView) => {
		setEditorKind("rule");
		setEditingRule(row ?? null);
		form.setFieldsValue({
			name: row?.name ?? "",
			ticketTypeId: row?.ticketTypeId != null ? String(row.ticketTypeId) : undefined,
			priorityLevelId: row?.priorityLevelId != null ? String(row.priorityLevelId) : undefined,
			calendarId: row?.calendarId != null ? String(row.calendarId) : undefined,
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

	const submitEditor = async () => {
		if (!domainId || !editorKind) {
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
					await updateSlaRule(domainId, editingRule.id, payload);
					message.success("SLA 规则已更新");
				}
				else {
					await createSlaRule(domainId, payload);
					message.success("SLA 规则已创建");
				}
			}
			else {
				const payload = {
					name: values.name,
					config: safeJson(values.configText),
				};
				if (editingCalendar) {
					await updateSlaCalendar(domainId, editingCalendar.id, payload);
					message.success("SLA 日历已更新");
				}
				else {
					await createSlaCalendar(domainId, payload);
					message.success("SLA 日历已创建");
				}
			}
			setEditorKind(null);
			setEditingRule(null);
			setEditingCalendar(null);
			await loadData();
		}
		catch (error) {
			message.error(error instanceof Error ? error.message : "保存失败");
		}
	};

	const globalColumns: TableColumnsType<PlatformSlaRuleView> = [
		{ title: "名称", dataIndex: "name", width: 200 },
		{ title: "首响分钟", dataIndex: "firstResponseMinutes", width: 120, render: value => value ?? "-" },
		{ title: "解决分钟", dataIndex: "resolutionMinutes", width: 120, render: value => value ?? "-" },
		{ title: "触发动作", dataIndex: "breachAction", ellipsis: true, render: value => JSON.stringify(value ?? {}) },
		{
			title: "操作",
			key: "actions",
			width: 100,
			render: (_, row) => (
				<Space>
					<Tooltip title="编辑">
						<Button type="link" size="small" icon={<EditOutlined />} onClick={() => openGlobalEditor(row)} />
					</Tooltip>
					<Tooltip title="删除">
						<Button type="link" size="small" danger icon={<DeleteOutlined />} onClick={() => void handleDeleteGlobalRule(row.id)} />
					</Tooltip>
				</Space>
			),
		},
	];

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
					<Tooltip title="编辑">
						<Button type="link" size="small" icon={<EditOutlined />} onClick={() => openRuleEditor(row)} />
					</Tooltip>
					<Tooltip title="删除">
						<Button type="link" size="small" danger icon={<DeleteOutlined />} onClick={async () => {
							if (!domainId) {
								return;
							}
							try {
								await deleteSlaRule(domainId, row.id);
								message.success("SLA 规则已删除");
								await loadData();
							}
							catch (error) {
								message.error(error instanceof Error ? error.message : "删除失败");
							}
						}} />
					</Tooltip>
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
			width: 80,
			render: (_, row) => (
				<Tooltip title="编辑">
					<Button type="link" size="small" icon={<EditOutlined />} onClick={() => openCalendarEditor(row)} />
				</Tooltip>
			),
		},
	];

	return (
		<BasicContent className="h-full bg-colorBgLayout">
			<Card
				title="SLA 管理"
				bordered={false}
				extra={<Typography.Text type="secondary">规则、日历与优先级关联统一维护</Typography.Text>}
			>
				<Space direction="vertical" size={16} className="w-full">
					<Segmented
						options={[
							{ label: "全局默认规则", value: "global" },
							{ label: "业务域规则（代管）", value: "domain" },
						]}
						value={section}
						onChange={value => setSection(value as Section)}
					/>

					{section === "global" ? (
						<Space direction="vertical" size={16} className="w-full">
							<div className="flex flex-wrap items-center gap-3">
								<Typography.Text className="text-slate-600">成员业务域</Typography.Text>
								<Select className="min-w-72" value={domainId} options={domainOptions} onChange={setDomainId} />
								<Typography.Text type="secondary">超时动作中处理人/关注人的成员数据源</Typography.Text>
							</div>
							<div className="flex justify-end">
								<Button type="primary" onClick={() => openGlobalEditor()}>新增全局规则</Button>
							</div>
							<Table<PlatformSlaRuleView>
								rowKey="id"
								loading={globalLoading}
								columns={globalColumns}
								dataSource={globalRuleRows}
								pagination={false}
								scroll={{ x: 900 }}
							/>
						</Space>
					) : (
						<Space direction="vertical" size={16} className="w-full">
							<div className="flex flex-wrap items-center gap-3">
								<Typography.Text className="text-slate-600">业务域</Typography.Text>
								<Select className="min-w-72" value={domainId} options={domainOptions} onChange={setDomainId} />
							</div>

							<Tabs
								activeKey={activeTab}
								onChange={key => setActiveTab(key as "rules" | "calendars")}
								items={[
									{ key: "rules", label: "SLA 规则" },
									{ key: "calendars", label: "SLA 日历" },
								]}
								tabBarExtraContent={(
									<Space>
										<Button type="primary" onClick={() => activeTab === "rules" ? openRuleEditor() : openCalendarEditor()}>
											新增{activeTab === "rules" ? "规则" : "日历"}
										</Button>
									</Space>
								)}
							/>

							{activeTab === "rules" ? (
								<Table<SlaRuleView>
									rowKey="id"
									loading={loading}
									columns={ruleColumns}
									dataSource={ruleRows}
									pagination={false}
									scroll={{ x: 1200 }}
								/>
							) : (
								<Table<SlaCalendarView>
									rowKey="id"
									loading={loading}
									columns={calendarColumns}
									dataSource={calendarRows}
									pagination={false}
									scroll={{ x: 1000 }}
								/>
							)}
						</Space>
					)}
				</Space>
			</Card>

			<Modal
				title={editingGlobalRule ? "编辑全局默认规则" : "新增全局默认规则"}
				open={globalEditorOpen}
				onCancel={() => {
					setGlobalEditorOpen(false);
					setEditingGlobalRule(null);
				}}
				onOk={() => void submitGlobalEditor()}
				destroyOnClose
				width={720}
			>
				<Form form={globalForm} layout="vertical">
					<SlaRuleForm form={globalForm} domainId={domainId} global />
				</Form>
			</Modal>

			<Modal
				title={editorKind === "rule" ? (editingRule ? "编辑 SLA 规则" : "新增 SLA 规则") : editingCalendar ? "编辑 SLA 日历" : "新增 SLA 日历"}
				open={editorKind !== null}
				onCancel={() => {
					setEditorKind(null);
					setEditingRule(null);
					setEditingCalendar(null);
				}}
				onOk={() => void submitEditor()}
				destroyOnClose
				width={720}
			>
				<Form form={form} layout="vertical">
					{editorKind === "rule" ? (
						<SlaRuleForm form={form} domainId={domainId} />
					) : (
						<>
							<Form.Item name="name" label="日历名称" rules={[{ required: true, message: "请输入日历名称" }]}>
								<Input placeholder="如 工作日历" />
							</Form.Item>
							<Form.Item name="configText" label="日历配置 JSON">
								<Input.TextArea rows={8} placeholder='例如 {"timezone":"Asia/Shanghai"}' />
							</Form.Item>
						</>
					)}
				</Form>
			</Modal>
		</BasicContent>
	);
}
