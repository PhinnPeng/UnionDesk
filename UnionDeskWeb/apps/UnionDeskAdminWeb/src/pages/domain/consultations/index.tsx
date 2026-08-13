import {
	convertConsultationToTicket,
	fetchDomainPriorityLevels,
	fetchDomainTicketTypes,
	getAdminConsultationMessages,
	listAdminConsultations,
	replyAdminConsultation,
	toErrorMessage,
	type ConsultationMessageRow,
	type ConsultationSessionRow,
	type DomainPriorityLevelView,
	type DomainTicketType,
} from "@uniondesk/shared";
import { ReloadOutlined, SearchOutlined } from "@ant-design/icons";
import { App, Button, Card, Drawer, Empty, Form, Input, Modal, Select, Space, Table, Tag, Typography } from "antd";
import type { TableColumnsType } from "antd";
import dayjs from "dayjs";
import { useCallback, useEffect, useMemo, useState } from "react";

import { AuthGuarded } from "#src/components/auth-guarded";
import { BasicContent } from "#src/components/basic-content";
import { TableSearchForm } from "#src/components/table-search-form";
import { useAuthStore } from "#src/store/auth";

interface ConsultationSearchValues {
	status?: string;
}

const EMPTY_CONSULTATION_SEARCH: ConsultationSearchValues = {};

const STATUS_OPTIONS = [
	{ value: "open", label: "进行中" },
	{ value: "closed", label: "已关闭" },
];

function resolveBusinessDomainId(
	defaultBusinessDomainId: number,
	accessibleDomains: Array<{ id: number }>,
): number {
	if (defaultBusinessDomainId > 0) {
		return defaultBusinessDomainId;
	}
	const first = accessibleDomains[0];
	return first ? Number(first.id) : 0;
}

function formatTime(value?: string | null): string {
	return value ? dayjs(value).format("YYYY-MM-DD HH:mm") : "-";
}

function statusLabel(status: string): string {
	return status === "closed" ? "已关闭" : "进行中";
}

function buildSessionSummary(sessionNo: string, messages: ConsultationMessageRow[]): string {
	const lines = messages.map(item => {
		const roleName = item.senderRole === "agent" ? "客服" : "客户";
		return `· (${roleName}) ${item.content}`;
	});
	return [`【咨询转工单】会话 ${sessionNo}`, "会话消息：", ...lines].join("\n");
}

export default function DomainConsultationsPage() {
	const { message } = App.useApp();
	const defaultBusinessDomainId = useAuthStore(state => state.defaultBusinessDomainId);
	const accessibleDomains = useAuthStore(state => state.accessibleDomains);

	const domainId = useMemo(
		() => resolveBusinessDomainId(defaultBusinessDomainId, accessibleDomains ?? []),
		[accessibleDomains, defaultBusinessDomainId],
	);

	const [loading, setLoading] = useState(false);
	const [rows, setRows] = useState<ConsultationSessionRow[]>([]);
	const [total, setTotal] = useState(0);
	const [page, setPage] = useState(1);
	const [pageSize, setPageSize] = useState(20);
	const [searchValues, setSearchValues] = useState<ConsultationSearchValues>(EMPTY_CONSULTATION_SEARCH);

	const [detailOpen, setDetailOpen] = useState(false);
	const [detailSession, setDetailSession] = useState<ConsultationSessionRow | null>(null);
	const [messages, setMessages] = useState<ConsultationMessageRow[]>([]);
	const [messagesLoading, setMessagesLoading] = useState(false);
	const [replyDraft, setReplyDraft] = useState("");
	const [replying, setReplying] = useState(false);

	const [convertOpen, setConvertOpen] = useState(false);
	const [convertSubmitting, setConvertSubmitting] = useState(false);
	const [convertForm] = Form.useForm<{ ticketTypeId?: string; priority?: string; title?: string; description?: string }>();
	const [ticketTypes, setTicketTypes] = useState<DomainTicketType[]>([]);
	const [priorityLevels, setPriorityLevels] = useState<DomainPriorityLevelView[]>([]);

	const loadSessions = useCallback(async (nextPage: number, nextPageSize: number, values: ConsultationSearchValues) => {
		if (!domainId) {
			return;
		}
		setLoading(true);
		try {
			const result = await listAdminConsultations(domainId, {
				page: nextPage,
				pageSize: nextPageSize,
				status: values.status,
			});
			setRows(result.list);
			setTotal(result.total);
		}
		catch (error) {
			message.error(toErrorMessage(error));
		}
		finally {
			setLoading(false);
		}
	}, [domainId, message]);

	useEffect(() => {
		void loadSessions(1, pageSize, EMPTY_CONSULTATION_SEARCH);
	}, [loadSessions, pageSize]);

	const openDetail = useCallback(async (session: ConsultationSessionRow) => {
		setDetailSession(session);
		setDetailOpen(true);
		setReplyDraft("");
		if (!domainId) {
			return;
		}
		setMessagesLoading(true);
		try {
			const rowsData = await getAdminConsultationMessages(domainId, session.sessionNo);
			setMessages(rowsData);
		}
		catch (error) {
			message.error(toErrorMessage(error));
		}
		finally {
			setMessagesLoading(false);
		}
	}, [domainId, message]);

	const handleReply = useCallback(async () => {
		const content = replyDraft.trim();
		if (!content || !detailSession || !domainId) {
			return;
		}
		setReplying(true);
		try {
			const row = await replyAdminConsultation(domainId, detailSession.sessionNo, content);
			setMessages(prev => [...prev, row]);
			setReplyDraft("");
			await loadSessions(page, pageSize, searchValues);
		}
		catch (error) {
			message.error(toErrorMessage(error));
		}
		finally {
			setReplying(false);
		}
	}, [detailSession, domainId, loadSessions, message, page, pageSize, replyDraft, searchValues]);

	const openConvert = useCallback(async (session: ConsultationSessionRow) => {
		setDetailSession(session);
		setConvertOpen(true);
		convertForm.resetFields();
		convertForm.setFieldsValue({
			title: "咨询转工单",
			description: buildSessionSummary(session.sessionNo, messages),
		});
		if (!domainId) {
			return;
		}
		try {
			const [types, priorities] = await Promise.all([
				fetchDomainTicketTypes(String(domainId)),
				fetchDomainPriorityLevels(String(domainId)),
			]);
			setTicketTypes(types.filter(item => item.status === "active"));
			setPriorityLevels(priorities.items);
		}
		catch (error) {
			message.error(toErrorMessage(error));
		}
	}, [convertForm, domainId, message, messages]);

	const handleConvert = useCallback(async () => {
		if (!detailSession || !domainId) {
			return;
		}
		const values = await convertForm.validateFields().catch(() => null);
		if (!values) {
			return;
		}
		setConvertSubmitting(true);
		try {
			const result = await convertConsultationToTicket(domainId, detailSession.sessionNo, {
				ticketTypeId: values.ticketTypeId ? Number(values.ticketTypeId) : undefined,
				priority: values.priority,
				title: values.title,
				description: values.description,
			});
			message.success(`已转为工单 ${result.ticketNo}`);
			setConvertOpen(false);
			setDetailOpen(false);
			await loadSessions(page, pageSize, searchValues);
		}
		catch (error) {
			message.error(toErrorMessage(error));
		}
		finally {
			setConvertSubmitting(false);
		}
	}, [convertForm, detailSession, domainId, loadSessions, message, page, pageSize, searchValues]);

	const columns: TableColumnsType<ConsultationSessionRow> = useMemo(() => [
		{
			title: "会话编号",
			dataIndex: "sessionNo",
			width: 180,
			render: (value: string, row) => (
				<Typography.Link onClick={() => void openDetail(row)}>{value}</Typography.Link>
			),
		},
		{
			title: "客户 ID",
			dataIndex: "customerId",
			width: 100,
			render: value => value ?? "-",
		},
		{
			title: "状态",
			dataIndex: "sessionStatus",
			width: 100,
			render: value => <Tag color={value === "closed" ? "default" : "blue"}>{statusLabel(value)}</Tag>,
		},
		{
			title: "消息数",
			dataIndex: "messageCount",
			width: 90,
			render: value => value ?? 0,
		},
		{
			title: "关联工单",
			dataIndex: "linkedTicketNo",
			width: 160,
			render: value => value ? <Tag color="green">{value}</Tag> : <span className="text-slate-400">—</span>,
		},
		{
			title: "最后消息时间",
			dataIndex: "lastMessageAt",
			width: 170,
			render: value => formatTime(value),
		},
		{
			title: "创建时间",
			dataIndex: "createdAt",
			width: 170,
			render: value => formatTime(value),
		},
		{
			title: "操作",
			key: "actions",
			width: 120,
			render: (_, row) => (
				<Button type="link" size="small" onClick={() => void openDetail(row)}>
					查看
				</Button>
			),
		},
	], [openDetail]);

	return (
		<BasicContent>
			<AuthGuarded
				auth="consultation.view"
				fallback={<Empty description="无权限查看咨询会话" className="py-16" />}
			>
				{!domainId ? (
					<Empty description="暂无可用业务域" className="py-16" />
				) : (
					<div className="flex flex-col gap-4">
						<Card
							bordered={false}
							title={(
								<Space>
									<SearchOutlined />
									<span>筛选条件</span>
								</Space>
							)}
						>
							<TableSearchForm<ConsultationSearchValues>
								loading={loading}
								initialValues={EMPTY_CONSULTATION_SEARCH}
								onFinish={values => {
									setSearchValues(values);
									void loadSessions(1, pageSize, values);
								}}
								onReset={() => {
									setSearchValues(EMPTY_CONSULTATION_SEARCH);
									void loadSessions(1, pageSize, EMPTY_CONSULTATION_SEARCH);
								}}
							>
								<Form.Item name="status" label="状态">
									<Select allowClear placeholder="全部状态" disabled={loading} options={STATUS_OPTIONS} />
								</Form.Item>
							</TableSearchForm>
						</Card>
						<Card
							bordered={false}
							title="咨询会话列表"
							extra={(
								<Button icon={<ReloadOutlined />} onClick={() => void loadSessions(page, pageSize, searchValues)}>
									刷新
								</Button>
							)}
						>
							<Table<ConsultationSessionRow>
								rowKey="id"
								loading={loading}
								columns={columns}
								dataSource={rows}
								scroll={{ x: 1100 }}
								pagination={{
									current: page,
									pageSize,
									total,
									showSizeChanger: true,
									showTotal: t => `共 ${t} 条`,
									onChange: (nextPage, nextPageSize) => {
										setPage(nextPage);
										setPageSize(nextPageSize);
										void loadSessions(nextPage, nextPageSize, searchValues);
									},
								}}
								locale={{ emptyText: <Empty description="暂无咨询会话" /> }}
							/>
						</Card>
					</div>
				)}
			</AuthGuarded>

			<Drawer
				title={detailSession ? `会话 ${detailSession.sessionNo}` : "会话详情"}
				open={detailOpen}
				onClose={() => setDetailOpen(false)}
				width={520}
			>
				{detailSession ? (
					<div className="flex flex-col gap-4">
						<Space wrap>
							<Tag color={detailSession.sessionStatus === "closed" ? "default" : "blue"}>
								{statusLabel(detailSession.sessionStatus)}
							</Tag>
							{detailSession.linkedTicketNo
								? <Tag color="green">已转工单 {detailSession.linkedTicketNo}</Tag>
								: null}
							<AuthGuarded auth="consultation.convert" fallback={null}>
								<Button
									type="primary"
									size="small"
									disabled={detailSession.sessionStatus === "closed"}
									onClick={() => void openConvert(detailSession)}
								>
									转工单
								</Button>
							</AuthGuarded>
						</Space>

						<div className="flex flex-col gap-2">
							{messagesLoading
								? <Empty description="消息加载中…" />
								: messages.length === 0
									? <Empty description="暂无消息" />
									: messages.map(item => (
										<div
											key={item.id}
											className={item.senderRole === "customer" ? "rounded-lg border border-slate-200 bg-slate-50 p-3" : "rounded-lg border border-teal-200 bg-teal-50 p-3"}
										>
											<Space size={8} className="mb-1">
												<Tag color={item.senderRole === "agent" ? "blue" : "default"}>
													{item.senderRole === "agent" ? "客服" : "客户"}
												</Tag>
												<span className="text-xs text-slate-400">{formatTime(item.createdAt)}</span>
											</Space>
											<Typography.Paragraph className="!mb-0" style={{ whiteSpace: "pre-wrap" }}>
												{item.content}
											</Typography.Paragraph>
										</div>
									))}
						</div>

						<AuthGuarded auth="consultation.reply" fallback={null}>
							<div className="flex flex-col gap-2">
								<Input.TextArea
									rows={3}
									placeholder="输入回复内容…"
									value={replyDraft}
									disabled={detailSession.sessionStatus === "closed"}
									onChange={event => setReplyDraft(event.target.value)}
								/>
								<Button
									type="primary"
									loading={replying}
									disabled={!replyDraft.trim() || detailSession.sessionStatus === "closed"}
									onClick={() => void handleReply()}
								>
									回复
								</Button>
							</div>
						</AuthGuarded>
					</div>
				) : null}
			</Drawer>

			<Modal
				title="转工单"
				open={convertOpen}
				onCancel={() => setConvertOpen(false)}
				onOk={() => void handleConvert()}
				confirmLoading={convertSubmitting}
				destroyOnClose
			>
				<Form form={convertForm} layout="vertical">
					<Typography.Paragraph type="secondary" className="!mb-4">
						会话：
						{detailSession?.sessionNo}
						{" · 客户 ID："}
						{detailSession?.customerId}
					</Typography.Paragraph>
					<Form.Item name="ticketTypeId" label="事项类型" rules={[{ required: true, message: "请选择事项类型" }]}>
						<Select
							placeholder="请选择事项类型"
							options={ticketTypes.map(item => ({ value: item.id, label: item.name }))}
						/>
					</Form.Item>
					<Form.Item name="priority" label="优先级">
						<Select
							allowClear
							placeholder="默认优先级"
							options={priorityLevels.map(item => ({
								value: item.code,
								label: item.display_label ?? item.name,
							}))}
						/>
					</Form.Item>
					<Form.Item name="title" label="标题" rules={[{ required: true, message: "请输入工单标题" }]}>
						<Input placeholder="工单标题" />
					</Form.Item>
					<Form.Item name="description" label="描述（已预填会话摘要，可修改）">
						<Input.TextArea rows={6} placeholder="工单描述" />
					</Form.Item>
				</Form>
			</Modal>
		</BasicContent>
	);
}
