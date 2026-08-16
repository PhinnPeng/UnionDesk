import {
	archiveAdminConsultation,
	claimAdminConsultation,
	convertConsultationToTicket,
	endAdminConsultation,
	fetchAgentPresence,
	fetchDomainPriorityLevels,
	fetchDomainTicketTypes,
	getAdminConsultationMessages,
	listAdminConsultations,
	realtimeClient,
	REALTIME_EVENT,
	replyAdminConsultation,
	reportAgentPresence,
	retractAdminConsultationMessage,
	toErrorMessage,
	type AgentPresenceMode,
	type AgentPresenceResult,
	type AgentPresenceStatus,
	type ConsultationMessageRow,
	type ConsultationSessionRow,
	type DomainPriorityLevelView,
	type DomainTicketType,
} from "@uniondesk/shared";
import { ReloadOutlined, SettingOutlined } from "@ant-design/icons";
import { App, Button, Card, Empty, Form, Input, InputNumber, Modal, Radio, Select, Space, Switch, Tag, Typography } from "antd";
import dayjs from "dayjs";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { useNavigate } from "react-router";

import { AuthGuarded } from "#src/components/auth-guarded";
import { BasicContent } from "#src/components/basic-content";
import { ConfirmPopover } from "#src/components/confirm-popover";
import { fetchDomainConfig, updateDomainConfig } from "#src/api/platform/domain-config";
import { useAuthStore } from "#src/store/auth";

import { DOMAIN_CONSULTATION_CLAIM, DOMAIN_CONSULTATION_CLOSE } from "../domain-permissions";

/** 会话状态筛选（三态；已归档会话默认隐藏，无独立入口） */
type SessionFilterKey = "queued" | "open" | "closed";

const SESSION_FILTER_OPTIONS: Array<{ value: SessionFilterKey; label: string }> = [
	{ value: "queued", label: "排队中" },
	{ value: "open", label: "进行中" },
	{ value: "closed", label: "已关闭" },
];

/** 在线心跳间隔（毫秒），与后端 presence 在线 TTL 对齐 */
const PRESENCE_HEARTBEAT_INTERVAL_MS = 30_000;

/** 消息可撤回时限（分钟），仅限客服本人 2 分钟内的消息 */
const RETRACT_WINDOW_MINUTES = 2;

/** 归档配置 KV（domain_config，配合后端 ConsultationArchiveJob 自动扫描） */
const ARCHIVE_CONFIG_ENABLED_KEY = "consultation_archive_auto_enabled";
const ARCHIVE_CONFIG_DAYS_KEY = "consultation_archive_auto_days";
const ARCHIVE_CONFIG_DEFAULT_DAYS = 30;

/** 聊天室配色（对齐 Figma V2：客服右蓝、客户左灰、排队橙、归档灰） */
const CHAT_COLORS = {
	primary: "#1778ff",
	primaryTint: "rgba(23, 120, 255, 0.08)",
	orange: "#fa731f",
	orangeTint: "rgba(250, 115, 31, 0.08)",
	green: "#52c41a",
	greenTint: "rgba(82, 196, 26, 0.1)",
	agentBubble: "#1778ff",
	customerBubble: "#f2f4f7",
} as const;

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
	return value ? dayjs(value).format("MM-DD HH:mm") : "-";
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

/** 会话状态 Tag 配色：排队橙 / 进行中蓝 / 已关闭灰 / 已归档灰 */
function SessionStatusTag({ session }: { session: ConsultationSessionRow }) {
	if (session.sessionStatus === "queued") {
		return <Tag color="orange" style={{ marginInlineEnd: 0 }}>排队中</Tag>;
	}
	if (session.sessionStatus === "closed") {
		return <Tag style={{ marginInlineEnd: 0 }}>已关闭</Tag>;
	}
	return <Tag color="blue" style={{ marginInlineEnd: 0 }}>进行中</Tag>;
}

/** 会话列表项：会话号 + 状态 / 客户 + 时间（排队显示等待时长）；激活态左侧蓝色指示条 + 浅蓝底 */
function SessionListItem({
	session,
	active,
	onSelect,
}: {
	session: ConsultationSessionRow
	active: boolean
	onSelect: () => void
}) {
	return (
		<button
			type="button"
			className={`block w-full border-0 rounded-lg px-4 py-3 text-left transition-colors ${active ? "border-l-[3px] border-l-[#1778ff] bg-[rgba(23,120,255,0.08)]" : "hover:bg-colorFillTertiary"}`}
			onClick={onSelect}
		>
			<div className="flex items-center justify-between gap-2">
				<span className={`truncate text-[13px] ${active ? "font-medium text-[#1778ff]" : "text-colorText"}`}>
					{session.sessionNo}
				</span>
				<SessionStatusTag session={session} />
			</div>
			<div className="mt-1 flex items-center justify-between gap-2">
				<span className="truncate text-xs text-colorTextSecondary">
					客户 #{session.customerId}
				</span>
				<span className="shrink-0 text-[11px] text-colorTextTertiary">
					{session.sessionStatus === "queued"
						? formatWaitingMinutes(session.createdAt)
						: formatTime(session.lastMessageAt ?? session.updatedAt)}
				</span>
			</div>
		</button>
	);
}

/** 消息气泡：客服右（蓝底白字）、客户左（灰底）；撤回入口仅客服 2 分钟内消息 */
function MessageBubble({
	item,
	retracting,
	onRetract,
}: {
	item: ConsultationMessageRow
	retracting: boolean
	onRetract: () => void
}) {
	const isAgent = item.senderRole === "agent";
	const retracted = isRetracted(item);
	return (
		<div className={`flex flex-col ${isAgent ? "items-end" : "items-start"}`}>
			<div
				className="max-w-[55%] whitespace-pre-wrap rounded-lg px-3 py-2 text-[13px] leading-5"
				style={{
					backgroundColor: isAgent ? CHAT_COLORS.agentBubble : CHAT_COLORS.customerBubble,
					color: isAgent ? "#fff" : "#101828",
				}}
			>
				{retracted ? <span className="text-colorTextTertiary">已撤回</span> : item.content}
			</div>
			<div className="mt-0.5 flex items-center gap-2 text-[11px] text-colorTextTertiary">
				<span>{formatTime(item.createdAt)}</span>
				{isRetractable(item) ? (
					<Button
						type="link"
						size="small"
						className="!h-auto !p-0 text-[11px]"
						disabled={retracting}
						onClick={onRetract}
					>
						撤回
					</Button>
				) : null}
			</div>
		</div>
	);
}

/** 归档配置弹窗（对齐 Figma V2：自动归档开关 + 关闭满 N 天 + 手动归档说明） */
function ArchiveConfigModal({
	domainId,
	open,
	onClose,
}: {
	domainId: string
	open: boolean
	onClose: () => void
}) {
	const { message } = App.useApp();
	const [enabled, setEnabled] = useState(false);
	const [days, setDays] = useState(ARCHIVE_CONFIG_DEFAULT_DAYS);
	const [loading, setLoading] = useState(false);
	const [saving, setSaving] = useState(false);

	const loadConfig = useCallback(async () => {
		if (!open || !domainId) {
			return;
		}
		setLoading(true);
		try {
			const view = await fetchDomainConfig(domainId);
			const enabledItem = view.items.find(item => item.key === ARCHIVE_CONFIG_ENABLED_KEY);
			const daysItem = view.items.find(item => item.key === ARCHIVE_CONFIG_DAYS_KEY);
			setEnabled(enabledItem?.value === "true");
			const parsedDays = daysItem?.value ? Number(daysItem.value) : NaN;
			setDays(Number.isFinite(parsedDays) && parsedDays > 0 ? parsedDays : ARCHIVE_CONFIG_DEFAULT_DAYS);
		}
		catch (error) {
			message.error(toErrorMessage(error));
		}
		finally {
			setLoading(false);
		}
	}, [domainId, message, open]);

	useEffect(() => {
		void loadConfig();
	}, [loadConfig]);

	const handleSave = useCallback(async () => {
		if (!domainId) {
			return;
		}
		setSaving(true);
		try {
			await updateDomainConfig(domainId, {
				items: [
					{ key: ARCHIVE_CONFIG_ENABLED_KEY, value: enabled ? "true" : "false", valueType: "bool", description: "咨询会话自动归档开关" },
					{ key: ARCHIVE_CONFIG_DAYS_KEY, value: String(days), valueType: "int", description: "关闭满 N 天自动归档" },
				],
			});
			message.success("归档配置已保存");
			onClose();
		}
		catch (error) {
			message.error(toErrorMessage(error));
		}
		finally {
			setSaving(false);
		}
	}, [days, domainId, enabled, message, onClose]);

	return (
		<Modal
			title="归档配置"
			open={open}
			onCancel={onClose}
			onOk={() => void handleSave()}
			confirmLoading={saving}
			destroyOnClose
		>
			<div className="flex flex-col gap-5 py-2">
				<div className="flex items-center gap-3">
					<Switch checked={enabled} disabled={loading} onChange={setEnabled} />
					<span className="text-[13px] font-medium">启用自动归档</span>
				</div>
				<div className="flex items-center gap-2">
					<span className="text-[13px] text-colorTextSecondary">关闭满</span>
					<InputNumber
						min={1}
						max={365}
						value={days}
						disabled={loading || !enabled}
						onChange={value => setDays(value ?? ARCHIVE_CONFIG_DEFAULT_DAYS)}
					/>
					<span className="text-[13px] text-colorTextSecondary">
						天自动归档
						<span className="ml-2 text-xs text-colorTextTertiary">（仅对未转工单的已关闭会话）</span>
					</span>
				</div>
				<Typography.Paragraph type="secondary" className="!mb-0 text-xs">
					已关闭会话可在会话列表中点击「归档」，归档后从默认列表隐藏，可在「已归档」筛选中查看；
					归档对客户不可见（客户咨询历史仍可见）。
				</Typography.Paragraph>
			</div>
		</Modal>
	);
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
	const [filterKey, setFilterKey] = useState<SessionFilterKey>("open");
	const [assignedToMe, setAssignedToMe] = useState(false);

	const [activeSessionNo, setActiveSessionNo] = useState<string | null>(null);
	const [messages, setMessages] = useState<ConsultationMessageRow[]>([]);
	const [messagesLoading, setMessagesLoading] = useState(false);
	const [replyDraft, setReplyDraft] = useState("");
	const [replying, setReplying] = useState(false);

	const [convertOpen, setConvertOpen] = useState(false);
	const [convertSubmitting, setConvertSubmitting] = useState(false);
	const [convertForm] = Form.useForm<{ ticketTypeId?: string; priority?: string; title?: string; description?: string }>();
	const [ticketTypes, setTicketTypes] = useState<DomainTicketType[]>([]);
	const [priorityLevels, setPriorityLevels] = useState<DomainPriorityLevelView[]>([]);
	const [archiveConfigOpen, setArchiveConfigOpen] = useState(false);

	// 在线/接入模式状态（基于 presence 心跳）
	const [presence, setPresence] = useState<AgentPresenceResult | null>(null);
	const [presenceLoading, setPresenceLoading] = useState(false);
	const [ending, setEnding] = useState(false);
	const [retracting, setRetracting] = useState(false);
	const [archiving, setArchiving] = useState(false);
	// 当前客服状态与接入模式（供心跳读取最新值，避免重建定时器）
	const presenceStatusRef = useRef<AgentPresenceStatus>("online");
	const presenceModeRef = useRef<AgentPresenceMode>("manual");
	// 当前活跃会话号（供 WS 回调读取最新值，避免重建订阅）
	const activeSessionNoRef = useRef<string | null>(null);

	const activeSession = useMemo(
		() => rows.find(item => item.sessionNo === activeSessionNo) ?? null,
		[activeSessionNo, rows],
	);

	const loadSessions = useCallback(async (nextPage: number, nextPageSize: number, nextFilter: SessionFilterKey, nextAssignedToMe: boolean) => {
		if (!domainId) {
			return;
		}
		setLoading(true);
		try {
			const options: { page: number; pageSize: number; status: string; assignedToMe: boolean } = {
				page: nextPage,
				pageSize: nextPageSize,
				status: nextFilter,
				assignedToMe: nextAssignedToMe,
			};
			const result = await listAdminConsultations(domainId, options);
			setRows(result.list);
			setTotal(result.total);
			setActiveSessionNo(prev => {
				if (prev && result.list.some(item => item.sessionNo === prev)) {
					return prev;
				}
				return result.list[0]?.sessionNo ?? null;
			});
		}
		catch (error) {
			message.error(toErrorMessage(error));
		}
		finally {
			setLoading(false);
		}
	}, [domainId, message]);

	/** 上报 presence（状态+模式一体心跳）：silent 时失败静默，仅用于定时心跳 */
	const loadPresence = useCallback(async (status: AgentPresenceStatus, mode: AgentPresenceMode, silent = false) => {
		if (!domainId) {
			return null;
		}
		try {
			const result = await reportAgentPresence(domainId, status, mode);
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
		void loadSessions(1, pageSize, filterKey, assignedToMe);
	}, [loadSessions, pageSize, filterKey, assignedToMe]);

	useEffect(() => {
		presenceStatusRef.current = presence?.status ?? "online";
		presenceModeRef.current = presence?.mode ?? "manual";
	}, [presence?.status, presence?.mode]);

	useEffect(() => {
		if (!domainId) {
			return;
		}
		// 进入页面先只读查询当前状态（隐身不因刷新丢失）；无在线记录则默认上报「上线 + 手动」
		void (async () => {
			try {
				const current = await fetchAgentPresence(domainId);
				if (current.status) {
					setPresence(current);
					return;
				}
			}
			catch {
				// 查询失败回退默认上报
			}
			await loadPresence("online", "manual", true);
		})();
	}, [domainId, loadPresence]);

	useEffect(() => {
		if (!domainId) {
			return;
		}
		// 页面活跃期间每 30s 心跳一次，携带当前状态与接入模式；失败静默
		const timer = window.setInterval(() => {
			void loadPresence(presenceStatusRef.current, presenceModeRef.current, true);
		}, PRESENCE_HEARTBEAT_INTERVAL_MS);
		return () => window.clearInterval(timer);
	}, [domainId, loadPresence]);

	/** 拉取当前会话消息（打开与轮询共用） */
	const loadMessages = useCallback(async (sessionNo: string) => {
		if (!domainId) {
			return;
		}
		setMessagesLoading(true);
		try {
			const rowsData = await getAdminConsultationMessages(domainId, sessionNo);
			setMessages(rowsData);
		}
		catch (error) {
			message.error(toErrorMessage(error));
		}
		finally {
			setMessagesLoading(false);
		}
	}, [domainId, message]);

	const openDetail = useCallback((session: ConsultationSessionRow) => {
		setActiveSessionNo(session.sessionNo);
		activeSessionNoRef.current = session.sessionNo;
		setReplyDraft("");
		void loadMessages(session.sessionNo);
	}, [loadMessages]);

	// 实时通道：chat.message 追加当前会话消息 / chat.session 刷新列表 / chat.queue 刷新排队会话
	const accessToken = useAuthStore(state => state.token);
	useEffect(() => {
		if (!accessToken) {
			return;
		}
		realtimeClient.connect(accessToken);
		realtimeClient.onReady(() => {
			void loadSessions(page, pageSize, filterKey, assignedToMe);
			const current = activeSessionNoRef.current;
			if (current) {
				void loadMessages(current);
			}
		});
		const onChatMessage = (payload: Record<string, unknown>) => {
			const sessionNo = String(payload.sessionNo ?? "");
			const messageId = String(payload.messageId ?? "");
			if (sessionNo && sessionNo === activeSessionNoRef.current) {
				setMessages(prev => prev.some(item => String(item.id) === messageId)
					? prev
					: [...prev, {
						id: messageId,
						sessionNo,
						seqNo: 0,
						businessDomainId: String(domainId),
						senderRole: String(payload.senderRole ?? "customer"),
						messageType: "text",
						content: String(payload.content ?? ""),
						createdAt: String(payload.createdAt ?? new Date().toISOString()),
					}]);
			}
			else if (sessionNo) {
				// 其他会话的新消息：刷新列表（最后消息时间/消息数）
				void loadSessions(page, pageSize, filterKey, assignedToMe);
			}
		};
		const onChatSession = () => {
			void loadSessions(page, pageSize, filterKey, assignedToMe);
		};
		const onChatQueue = () => {
			void loadSessions(page, pageSize, filterKey, assignedToMe);
		};
		realtimeClient.on(REALTIME_EVENT.CHAT_MESSAGE, onChatMessage);
		realtimeClient.on(REALTIME_EVENT.CHAT_SESSION, onChatSession);
		realtimeClient.on(REALTIME_EVENT.CHAT_QUEUE, onChatQueue);
		return () => {
			realtimeClient.off(REALTIME_EVENT.CHAT_MESSAGE, onChatMessage);
			realtimeClient.off(REALTIME_EVENT.CHAT_SESSION, onChatSession);
			realtimeClient.off(REALTIME_EVENT.CHAT_QUEUE, onChatQueue);
		};
	}, [accessToken, assignedToMe, domainId, filterKey, loadMessages, loadSessions, page, pageSize]);

	const handleReply = useCallback(async () => {
		const content = replyDraft.trim();
		if (!content || !activeSession || !domainId) {
			return;
		}
		setReplying(true);
		try {
			const row = await replyAdminConsultation(domainId, activeSession.sessionNo, content);
			setMessages(prev => [...prev, row]);
			setReplyDraft("");
			await loadSessions(page, pageSize, filterKey, assignedToMe);
		}
		catch (error) {
			message.error(toErrorMessage(error));
		}
		finally {
			setReplying(false);
		}
	}, [activeSession, assignedToMe, domainId, filterKey, loadSessions, message, page, pageSize, replyDraft]);

	const openConvert = useCallback(async (session: ConsultationSessionRow) => {
		setActiveSessionNo(session.sessionNo);
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
		if (!activeSession || !domainId) {
			return;
		}
		const values = await convertForm.validateFields().catch(() => null);
		if (!values) {
			return;
		}
		setConvertSubmitting(true);
		try {
			const result = await convertConsultationToTicket(domainId, activeSession.sessionNo, {
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
			await loadSessions(page, pageSize, filterKey, assignedToMe);
		}
		catch (error) {
			message.error(toErrorMessage(error));
		}
		finally {
			setConvertSubmitting(false);
		}
	}, [activeSession, assignedToMe, convertForm, domainId, filterKey, loadSessions, message, navigate, page, pageSize]);

	const handlePresenceStatusChange = useCallback(async (status: AgentPresenceStatus) => {
		if (!domainId) {
			return;
		}
		setPresenceLoading(true);
		try {
			const result = await reportAgentPresence(domainId, status, presenceModeRef.current);
			setPresence(result);
			message.success(status === "online" ? "已上线，可接入会话" : "已隐身，不再接入新会话（已接入会话不受影响）");
		}
		catch (error) {
			message.error(toErrorMessage(error));
		}
		finally {
			setPresenceLoading(false);
		}
	}, [domainId, message]);

	const handlePresenceModeChange = useCallback(async (mode: AgentPresenceMode) => {
		if (!domainId) {
			return;
		}
		setPresenceLoading(true);
		try {
			const result = await reportAgentPresence(domainId, presenceStatusRef.current, mode);
			setPresence(result);
			if (mode === "auto") {
				message.success("已开启自动接入，正在拉取排队会话");
				setPage(1);
				await loadSessions(1, pageSize, filterKey, assignedToMe);
			}
		}
		catch (error) {
			message.error(toErrorMessage(error));
		}
		finally {
			setPresenceLoading(false);
		}
	}, [assignedToMe, domainId, filterKey, loadSessions, message, pageSize]);

	const handleClaim = useCallback(async (session: ConsultationSessionRow) => {
		if (!domainId) {
			return;
		}
		try {
			await claimAdminConsultation(domainId, session.sessionNo);
			message.success("已接入会话");
			await loadSessions(page, pageSize, filterKey, assignedToMe);
		}
		catch (error) {
			message.error(toErrorMessage(error));
		}
	}, [assignedToMe, domainId, filterKey, loadSessions, message, page, pageSize]);

	const handleEndSession = useCallback(async () => {
		if (!activeSession || !domainId) {
			return;
		}
		setEnding(true);
		try {
			await endAdminConsultation(domainId, activeSession.sessionNo);
			message.success("咨询已结束");
			await loadSessions(page, pageSize, filterKey, assignedToMe);
		}
		catch (error) {
			message.error(toErrorMessage(error));
		}
		finally {
			setEnding(false);
		}
	}, [activeSession, assignedToMe, domainId, filterKey, loadSessions, message, page, pageSize]);

	const handleArchive = useCallback(async (session: ConsultationSessionRow) => {
		if (!domainId) {
			return;
		}
		setArchiving(true);
		try {
			await archiveAdminConsultation(domainId, session.sessionNo);
			message.success("已归档会话");
			await loadSessions(page, pageSize, filterKey, assignedToMe);
		}
		catch (error) {
			message.error(toErrorMessage(error));
		}
		finally {
			setArchiving(false);
		}
	}, [assignedToMe, domainId, filterKey, loadSessions, message, page, pageSize]);

	const handleRetract = useCallback(async (item: ConsultationMessageRow) => {
		if (!activeSession || !domainId) {
			return;
		}
		setRetracting(true);
		try {
			const updated = await retractAdminConsultationMessage(domainId, activeSession.sessionNo, item.id);
			setMessages(prev => prev.map(row => (row.id === item.id ? { ...row, ...updated } : row)));
		}
		catch (error) {
			message.error(toErrorMessage(error));
		}
		finally {
			setRetracting(false);
		}
	}, [activeSession, domainId, message]);

	const content = (
		<>
			<AuthGuarded
				auth="consultation.view"
				fallback={<Empty description="无权限查看咨询会话" className="py-16" />}
			>
				{!domainId ? (
					<Empty description="暂无可用业务域" className="py-16" />
				) : (
					<Card
						bordered
						className="h-full overflow-hidden"
						styles={{ body: { padding: 0, height: "100%" } }}
						title={null}
					>
						<div className="flex h-full min-h-0">
							{/* 左栏：在线状态 + 接入模式 + 状态筛选 + 会话列表 */}
							<div className="flex w-[320px] shrink-0 flex-col border-r border-colorBorderSecondary bg-colorBgLayout">
							<div className="flex h-12 shrink-0 items-center gap-2 border-b border-colorBorderSecondary px-3">
								{presence ? (
									<Radio.Group
										size="small"
										optionType="button"
										buttonStyle="solid"
										value={presence.status ?? "online"}
										disabled={presenceLoading}
										options={[
											{ value: "online", label: "上线" },
											{ value: "invisible", label: "隐身" },
										]}
										onChange={event => void handlePresenceStatusChange(event.target.value as AgentPresenceStatus)}
									/>
								) : (
									<span className="text-xs text-colorTextTertiary">检测中</span>
								)}
								<Radio.Group
									size="small"
									optionType="button"
									buttonStyle="solid"
									value={presence?.mode ?? "manual"}
									disabled={presenceLoading || presence?.status !== "online"}
									options={[
										{ value: "auto", label: "自动接入" },
										{ value: "manual", label: "手动接入" },
									]}
									onChange={event => void handlePresenceModeChange(event.target.value as AgentPresenceMode)}
								/>
									<div className="ml-auto flex items-center gap-1">
										<Button
											type="text"
											size="small"
											icon={<ReloadOutlined />}
											title="刷新"
											onClick={() => void loadSessions(page, pageSize, filterKey, assignedToMe)}
										/>
										<AuthGuarded auth={DOMAIN_CONSULTATION_CLOSE} fallback={null}>
											<Button
												type="text"
												size="small"
												icon={<SettingOutlined />}
												title="归档配置"
												onClick={() => setArchiveConfigOpen(true)}
											/>
										</AuthGuarded>
									</div>
								</div>
								<div className="shrink-0 border-b border-colorBorderSecondary p-3">
									<Radio.Group
										size="small"
										value={filterKey}
										options={SESSION_FILTER_OPTIONS}
										optionType="button"
										buttonStyle="solid"
										onChange={event => {
											setFilterKey(event.target.value as SessionFilterKey);
											setPage(1);
										}}
									/>
								</div>
								<div className="flex shrink-0 items-center gap-2 px-3 pb-2">
									<span className="text-xs text-colorTextSecondary">仅看我的</span>
									<Switch
										size="small"
										checked={assignedToMe}
										onChange={value => {
											setAssignedToMe(value);
											setPage(1);
										}}
									/>
								</div>
								<div className="min-h-0 flex-1 overflow-y-auto">
									{loading && rows.length === 0 ? (
										<Empty description="加载中…" className="py-10" />
									) : rows.length === 0 ? (
										<Empty description="暂无咨询会话" className="py-10" />
									) : (
											rows.map(item => (
												<SessionListItem
													key={item.sessionNo}
													session={item}
													active={item.sessionNo === activeSessionNo}
													onSelect={() => openDetail(item)}
												/>
											))
									)}
								</div>
								<div className="shrink-0 border-t border-colorBorderSecondary px-3 py-2 text-xs text-colorTextTertiary">
									共 {total} 条 · 第 {page} 页
								</div>
							</div>

							{/* 右栏：会话头 + 消息区 + 输入区 */}
							<div className="flex min-w-0 flex-1 flex-col bg-colorBgContainer">
								{!activeSession ? (
									<div className="flex flex-1 items-center justify-center">
										<Empty description="选择左侧会话开始处理" />
									</div>
								) : (
									<>
										<div className="flex h-14 shrink-0 items-center gap-2 border-b border-colorBorderSecondary px-4">
											<span className="text-sm font-medium">{activeSession.sessionNo}</span>
											<SessionStatusTag session={activeSession} />
											{activeSession.linkedTicketNo ? (
												<Tag color="green" style={{ marginInlineEnd: 0 }}>
													已转工单 {activeSession.linkedTicketNo}
												</Tag>
											) : null}
											<div className="ml-auto flex items-center gap-2">
												{activeSession.sessionStatus !== "closed" ? (
													<>
														<AuthGuarded auth="consultation.convert" fallback={null}>
															<Button
																type="primary"
																size="small"
																onClick={() => void openConvert(activeSession)}
															>
																转工单
															</Button>
														</AuthGuarded>
															{!activeSession.assignedTo
																&& (activeSession.sessionStatus === "open" || activeSession.sessionStatus === "queued") ? (
																	<AuthGuarded auth={DOMAIN_CONSULTATION_CLAIM} fallback={null}>
																		<Button
																			size="small"
																			disabled={presence?.status === "invisible"}
																			onClick={() => void handleClaim(activeSession)}
																		>
																			接入
																		</Button>
																	</AuthGuarded>
																) : null}
														<AuthGuarded auth={DOMAIN_CONSULTATION_CLOSE} fallback={null}>
															<ConfirmPopover
																title={`确认结束会话「${activeSession.sessionNo}」？`}
																onConfirm={() => void handleEndSession()}
															>
																<Button danger size="small" loading={ending}>
																	结束咨询
																</Button>
															</ConfirmPopover>
														</AuthGuarded>
													</>
													) : (
														<AuthGuarded auth={DOMAIN_CONSULTATION_CLOSE} fallback={null}>
															<ConfirmPopover
																title={`确认归档会话「${activeSession.sessionNo}」？归档后从默认列表隐藏，客户不可见。`}
																onConfirm={() => void handleArchive(activeSession)}
															>
																<Button size="small" loading={archiving}>
																	归档
																</Button>
															</ConfirmPopover>
														</AuthGuarded>
													)}
											</div>
										</div>

										<div className="min-h-0 flex-1 space-y-4 overflow-y-auto bg-colorBgLayout px-4 py-4">
											{messagesLoading && messages.length === 0 ? (
												<Empty description="消息加载中…" className="py-10" />
											) : messages.length === 0 ? (
												<Empty description="暂无消息" className="py-10" />
											) : (
												messages.map(item => (
													<MessageBubble
														key={item.id}
														item={item}
														retracting={retracting}
														onRetract={() => void handleRetract(item)}
													/>
												))
											)}
										</div>

										<AuthGuarded auth="consultation.reply" fallback={null}>
											<div className="shrink-0 border-t border-colorBorderSecondary p-3">
												<Input.TextArea
													rows={3}
													placeholder="输入回复内容…"
													value={replyDraft}
													disabled={activeSession.sessionStatus === "closed"}
													onChange={event => setReplyDraft(event.target.value)}
													onPressEnter={event => {
														if (!event.shiftKey) {
															event.preventDefault();
															void handleReply();
														}
													}}
												/>
												<div className="mt-2 flex items-center justify-between">
													<span className="text-[11px] text-colorTextTertiary">
														已关闭会话可归档，归档后从默认列表隐藏，客户不可见
													</span>
													<Button
														type="primary"
														loading={replying}
														disabled={!replyDraft.trim() || activeSession.sessionStatus === "closed"}
														onClick={() => void handleReply()}
													>
														发送
													</Button>
												</div>
											</div>
										</AuthGuarded>
									</>
								)}
							</div>
						</div>
					</Card>
				)}
			</AuthGuarded>

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
						{activeSession?.sessionNo}
						{" · 客户 ID："}
						{activeSession?.customerId}
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

			<ArchiveConfigModal
				domainId={domainId}
				open={archiveConfigOpen}
				onClose={() => setArchiveConfigOpen(false)}
			/>
		</>
	);
	// 嵌入工作台时不再套 BasicContent（避免双层 p-4 内边距导致左右未对齐）
	return embedded ? content : <BasicContent className="h-full">{content}</BasicContent>;
}
