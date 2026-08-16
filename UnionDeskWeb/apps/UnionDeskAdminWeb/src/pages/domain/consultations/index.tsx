import {
	claimAdminConsultation,
	convertConsultationToTicket,
	endAdminConsultation,
	fetchDomainPriorityLevels,
	fetchDomainTicketTypes,
	getAdminConsultationMessages,
	listAdminConsultations,
	replyAdminConsultation,
	reportAgentPresence,
	retractAdminConsultationMessage,
	toErrorMessage,
	type AgentPresenceMode,
	type AgentPresenceResult,
	type ConsultationMessageRow,
	type ConsultationSessionRow,
	type DomainPriorityLevelView,
	type DomainTicketType,
} from "@uniondesk/shared";
import { ReloadOutlined, SearchOutlined } from "@ant-design/icons";
import { App, Button, Card, Drawer, Empty, Form, Input, Modal, Radio, Select, Space, Switch, Table, Tag, Typography } from "antd";
import type { TableColumnsType } from "antd";
import dayjs from "dayjs";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { useNavigate } from "react-router";

import { AuthGuarded } from "#src/components/auth-guarded";
import { BasicContent } from "#src/components/basic-content";
import { ConfirmPopover } from "#src/components/confirm-popover";
import { TableSearchForm } from "#src/components/table-search-form";
import { useAuthStore } from "#src/store/auth";

import { DOMAIN_CONSULTATION_CLAIM, DOMAIN_CONSULTATION_CLOSE } from "../domain-permissions";

interface ConsultationSearchValues {
	status?: string;
	assigned_to_me?: boolean;
}

const EMPTY_CONSULTATION_SEARCH: ConsultationSearchValues = {};

/** 在线心跳间隔（毫秒），与后端 presence 在线 TTL 对齐 */
const PRESENCE_HEARTBEAT_INTERVAL_MS = 30_000;

/** 消息可撤回时限（分钟），仅限客服本人 2 分钟内的消息 */
const RETRACT_WINDOW_MINUTES = 2;

const STATUS_OPTIONS = [
	{ value: "open", label: "进行中" },
	{ value: "closed", label: "已关闭" },
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

function formatTime(value?: string | null): string {
	return value ? dayjs(value).format("YYYY-MM-DD HH:mm") : "-";
}

function statusLabel(status: string): string {
	if (status === "closed") {
		return "已关闭";
	}
	if (status === "queued") {
		return "排队中";
	}
	return "进行中";
}

/** 排队等待时长（分钟）：now - createdAt */
function formatWaitingMinutes(createdAt: string): string {
	const minutes = Math.max(0, dayjs().diff(dayjs(createdAt), "minute"));
	return `已等待 ${minutes} 分钟`;
}

/** 消息是否已撤回（后端返回 retractedAt/retracted 其一） */
function isRetracted(item: ConsultationMessageRow): boolean {
	return Boolean(item.retractedAt || item.retracted);
}

/** 消息是否可撤回：客服本人发送且 2 分钟内、未撤回 */
function isRetractable(item: ConsultationMessageRow): boolean {
	if (item.senderRole !== "agent" || isRetracted(item)) {
		return false;
	}
	return dayjs().diff(dayjs(item.createdAt), "minute") < RETRACT_WINDOW_MINUTES;
}

function buildSessionSummary(sessionNo: string, messages: ConsultationMessageRow[]): string {
	const lines = messages.map(item => {
		const roleName = item.senderRole === "agent" ? "客服" : "客户";
		return `· (${roleName}) ${item.content}`;
	});
	return [`【咨询转工单】会话 ${sessionNo}`, "会话消息：", ...lines].join("\n");
}

export default function DomainConsultationsPage({ embedded = false }: { embedded?: boolean }) {
	const { message } = App.useApp();
	const navigate = useNavigate();
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

	// 在线/接入模式状态（基于 presence 心跳）
	const [presence, setPresence] = useState<AgentPresenceResult | null>(null);
	const [presenceLoading, setPresenceLoading] = useState(false);
	const [ending, setEnding] = useState(false);
	const [retracting, setRetracting] = useState(false);
	// 当前接入模式（供心跳读取最新值，避免重建定时器）
	const presenceModeRef = useRef<AgentPresenceMode>("manual");

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
				assignedToMe: values.assigned_to_me,
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

	/** 上报 presence（心跳+模式一体）：silent 时失败静默，仅用于定时心跳 */
	const loadPresence = useCallback(async (mode: AgentPresenceMode, silent = false) => {
		if (!domainId) {
			return null;
		}
		try {
			const result = await reportAgentPresence(domainId, mode);
			setPresence(result);
			return result;
		}
		catch (error) {
			if (!silent) {
				message.error(toErrorMessage(error));
			}
			return null;
		}
	}, [domainId, message]);

	useEffect(() => {
		void loadSessions(1, pageSize, EMPTY_CONSULTATION_SEARCH);
	}, [loadSessions, pageSize]);

	useEffect(() => {
		presenceModeRef.current = presence?.mode ?? "manual";
	}, [presence?.mode]);

	useEffect(() => {
		if (!domainId) {
			return;
		}
		// 进入页面先上报一次 presence 获取当前模式与在线状态（携带默认手动模式，
		// 以响应返回的 mode/online 为准；若后端支持 GET 查询，联调时改为只读请求）
		void loadPresence("manual", true);
	}, [domainId, loadPresence]);

	useEffect(() => {
		if (!domainId) {
			return;
		}
		// 页面活跃期间每 30s 心跳一次，携带当前接入模式；失败静默，成功时同步在线状态
		const timer = window.setInterval(() => {
			void loadPresence(presenceModeRef.current, true);
		}, PRESENCE_HEARTBEAT_INTERVAL_MS);
		return () => window.clearInterval(timer);
	}, [domainId, loadPresence]);

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
				fetchDomainTicketTypes(domainId),
				fetchDomainPriorityLevels(domainId),
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
				ticketTypeId: values.ticketTypeId ? values.ticketTypeId : undefined,
				priority: values.priority,
				title: values.title,
				description: values.description,
			});
			message.success({
				content: (
					<span>
						已转为工单 {result.ticketNo}
						<Typography.Link
							className="ml-2"
							onClick={() => {
								message.destroy();
								navigate("/domain/workbench?tab=ticket");
							}}
						>
							前往工单
						</Typography.Link>
					</span>
				),
				duration: 5,
			});
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

	const handlePresenceModeChange = useCallback(async (mode: AgentPresenceMode) => {
		if (!domainId) {
			return;
		}
		setPresenceLoading(true);
		try {
			const result = await reportAgentPresence(domainId, mode);
			setPresence(result);
			if (!result.online) {
				message.warning("需在线才能开启自动接入");
				return;
			}
			if (mode === "auto") {
				message.success("已开启自动接入，正在拉取排队会话");
				// 开启自动接入成功后刷新列表（后端已拉取排队会话）
				setPage(1);
				await loadSessions(1, pageSize, searchValues);
			}
		}
		catch (error) {
			message.error(toErrorMessage(error));
		}
		finally {
			setPresenceLoading(false);
		}
	}, [domainId, loadSessions, message, pageSize, searchValues]);

	const handleClaim = useCallback(async (session: ConsultationSessionRow) => {
		if (!domainId) {
			return;
		}
		try {
			await claimAdminConsultation(domainId, session.sessionNo);
			message.success("已接入会话");
			await loadSessions(page, pageSize, searchValues);
		}
		catch (error) {
			message.error(toErrorMessage(error));
		}
	}, [domainId, loadSessions, message, page, pageSize, searchValues]);

	const handleEndSession = useCallback(async () => {
		if (!detailSession || !domainId) {
			return;
		}
		setEnding(true);
		try {
			await endAdminConsultation(domainId, detailSession.sessionNo);
			message.success("咨询已结束");
			setDetailOpen(false);
			await loadSessions(page, pageSize, searchValues);
		}
		catch (error) {
			message.error(toErrorMessage(error));
		}
		finally {
			setEnding(false);
		}
	}, [detailSession, domainId, loadSessions, message, page, pageSize, searchValues]);

	const handleRetract = useCallback(async (item: ConsultationMessageRow) => {
		if (!detailSession || !domainId) {
			return;
		}
		setRetracting(true);
		try {
			const updated = await retractAdminConsultationMessage(domainId, detailSession.sessionNo, item.id);
			setMessages(prev => prev.map(row => (row.id === item.id ? { ...row, ...updated } : row)));
		}
		catch (error) {
			message.error(toErrorMessage(error));
		}
		finally {
			setRetracting(false);
		}
	}, [detailSession, domainId, message]);

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
			width: 150,
			render: (value: string, row) => (
				value === "queued"
					? (
						<Space size={4}>
							<Tag color="orange">排队中</Tag>
							<span className="text-xs text-slate-400">{formatWaitingMinutes(row.createdAt)}</span>
						</Space>
					)
					: <Tag color={value === "closed" ? "default" : "blue"}>{statusLabel(value)}</Tag>
			),
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
			width: 140,
			render: (_, row) => (
				<Space size="small">
					<Button type="link" size="small" onClick={() => void openDetail(row)}>
						查看
					</Button>
					{!row.assignedTo && (row.sessionStatus === "open" || row.sessionStatus === "queued") ? (
						<AuthGuarded auth={DOMAIN_CONSULTATION_CLAIM} fallback={null}>
							<Button type="link" size="small" onClick={() => void handleClaim(row)}>
								接入
							</Button>
						</AuthGuarded>
					) : null}
				</Space>
			),
		},
	], [handleClaim, openDetail]);

	const content = (
		<>
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
								<Form.Item name="assigned_to_me" label="仅看我的" valuePropName="checked">
									<Switch disabled={loading} />
								</Form.Item>
							</TableSearchForm>
						</Card>
						<Card
							bordered={false}
							title="咨询会话列表"
							extra={(
								<Space size={12}>
									<Space size={4}>
										<Tag color={presence?.online ? "success" : "default"}>
											{presence ? (presence.online ? "在线" : "离线") : "检测中"}
										</Tag>
										<Radio.Group
											size="small"
											optionType="button"
											buttonStyle="solid"
											value={presence?.mode ?? "manual"}
											disabled={presenceLoading || (presence ? !presence.online : false)}
											options={[
												{ value: "auto", label: "自动接入" },
												{ value: "manual", label: "手动接入" },
											]}
											onChange={event => void handlePresenceModeChange(event.target.value as AgentPresenceMode)}
										/>
									</Space>
									{presence && !presence.online ? (
										<Typography.Text type="secondary" className="text-xs">
											需在线才能开启自动接入
										</Typography.Text>
									) : null}
									<Button icon={<ReloadOutlined />} onClick={() => void loadSessions(page, pageSize, searchValues)}>
										刷新
									</Button>
								</Space>
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
				extra={detailSession && detailSession.sessionStatus !== "closed" ? (
					<AuthGuarded auth={DOMAIN_CONSULTATION_CLOSE} fallback={null}>
						<ConfirmPopover
							title={`确认结束会话「${detailSession.sessionNo}」？`}
							onConfirm={() => void handleEndSession()}
						>
							<Button danger loading={ending}>
								结束咨询
							</Button>
						</ConfirmPopover>
					</AuthGuarded>
				) : undefined}
			>
				{detailSession ? (
					<div className="flex flex-col gap-4">
						<Space wrap>
							<Tag color={detailSession.sessionStatus === "queued" ? "orange" : detailSession.sessionStatus === "closed" ? "default" : "blue"}>
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
												{isRetractable(item) ? (
													<Button
														type="link"
														size="small"
														className="!p-0 !h-auto text-xs"
														disabled={retracting}
														onClick={() => void handleRetract(item)}
													>
														撤回
													</Button>
												) : null}
											</Space>
											{isRetracted(item) ? (
												<Typography.Text type="secondary" className="text-xs">已撤回</Typography.Text>
											) : (
												<Typography.Paragraph className="!mb-0" style={{ whiteSpace: "pre-wrap" }}>
													{item.content}
												</Typography.Paragraph>
											)}
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
		</>
	);
	// 嵌入工作台时不再套 BasicContent（避免双层 p-4 内边距导致左右未对齐）
	return embedded ? content : <BasicContent>{content}</BasicContent>;
}
