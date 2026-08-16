import {
	replyAdminTicket,
	replaceAdminTicketWatchers,
	fetchTicketDetail,
	type TicketDetailResult,
	type TicketHistoryRow,
	type TicketReplyRow,
	type TicketRow,
} from "#src/api/platform/ticket";
import { resolveMenuIcon } from "#src/icons/resolve-menu-icon";
import { useAuthStore } from "#src/store/auth";
import ReactMarkdown from "react-markdown";
import dayjs from "dayjs";
import { realtimeClient, REALTIME_EVENT, toErrorMessage } from "@uniondesk/shared";

import {
	App,
	Button,
	Descriptions,
	Dropdown,
	Empty,
	Input,
	Segmented,
	Space,
	Spin,
	Tabs,
	Tag,
	Timeline,
	Tooltip,
	Typography,
} from "antd";
import {
	CopyOutlined,
	EllipsisOutlined,
	FullscreenExitOutlined,
	FullscreenOutlined,
	LinkOutlined,
	SendOutlined,
	StarOutlined,
	SwapLeftOutlined,
	SwapRightOutlined,
} from "@ant-design/icons";
import { useCallback, useEffect, useMemo, useRef, useState, type ComponentRef } from "react";

const { Title, Text, Paragraph } = Typography;

/** 活动日志筛选：全部 / 只看日志 / 只看对话 / 只看备注（备注仅业务域可见） */
type LogFilterKey = "all" | "log" | "conversation" | "note";

/** 动作中文映射 */
const ACTION_LABELS: Record<string, string> = {
	create: "创建工单",
	claim: "领取工单",
	assign: "指派工单",
	reply: "回复",
	status_change: "状态变更",
	merge: "合并工单",
	close: "关闭工单",
	withdraw: "撤回工单",
};

/** 运行时状态码中文 */
const STATUS_LABELS: Record<string, string> = {
	open: "待处理",
	new: "待处理",
	processing: "处理中",
	resolved: "已解决",
	closed: "已关闭",
	merged: "已合并",
	withdrawn: "已撤回",
};

/** 优先级码中文 */
const PRIORITY_LABELS: Record<string, string> = {
	low: "低",
	normal: "中",
	high: "高",
	urgent: "紧急",
};

function formatTime(value?: string | null) {
	return value ? dayjs(value).format("YYYY-MM-DD HH:mm") : "-";
}

/** 复制文本（clipboard API，失败回退） */
async function copyText(text: string): Promise<boolean> {
	try {
		await navigator.clipboard.writeText(text);
		return true;
	}
	catch {
		const textarea = document.createElement("textarea");
		textarea.value = text;
		textarea.style.position = "fixed";
		textarea.style.opacity = "0";
		document.body.appendChild(textarea);
		textarea.select();
		const ok = document.execCommand("copy");
		document.body.removeChild(textarea);
		return ok;
	}
}

/** 工单分享链接（业务域详情路由） */
function ticketShareUrl(domainId: string, ticketId: string): string {
	return `${window.location.origin}/domain/ticket-queue/${ticketId}?domain=${domainId}`;
}

interface TicketDetailContentProps {
	domainId: string
	ticketId: string | null
	/** 当前列表行（上一条/下一条导航） */
	tickets: TicketRow[]
	onClose: () => void
	/** 操作成功后父级刷新列表 */
	onChanged?: () => void
	/** 分配（父级弹窗） */
	onAssign?: (row: TicketRow) => void
	/** 全屏形态切换（容器负责 localStorage 记忆） */
	onToggleFullscreen?: () => void
	isFullscreen?: boolean
}

/** 底部 Markdown 输入：编辑/预览 + 标题/加粗/斜体/链接/引用 工具栏 */
function MarkdownEditor({
	value,
	onChange,
	placeholder,
	submitText,
	onSubmit,
	onCancel,
	submitting,
}: {
	value: string
	onChange: (next: string) => void
	placeholder: string
	submitText: string
	onSubmit: () => void
	onCancel: () => void
	submitting: boolean
}) {
	const [mode, setMode] = useState<"edit" | "preview">("edit");
	const textareaRef = useRef<ComponentRef<typeof Input.TextArea>>(null);

	/** 在光标处插入语法（包裹选中文本；heading/quote 为行首插入） */
	const insertSyntax = useCallback((before: string, after: string, placeholderText: string) => {
		const el = textareaRef.current?.nativeElement as HTMLTextAreaElement | undefined;
		if (!el) {
			return;
		}
		const start = el.selectionStart ?? value.length;
		const end = el.selectionEnd ?? value.length;
		const selected = value.slice(start, end) || placeholderText;
		const next = value.slice(0, start) + before + selected + after + value.slice(end);
		onChange(next);
		requestAnimationFrame(() => {
			el.focus();
			const cursor = start + before.length + selected.length + after.length;
			el.setSelectionRange(cursor, cursor);
		});
	}, [onChange, value]);

	const insertLinePrefix = useCallback((prefix: string) => {
		const el = textareaRef.current?.nativeElement as HTMLTextAreaElement | undefined;
		if (!el) {
			return;
		}
		const start = el.selectionStart ?? value.length;
		const lineStart = value.lastIndexOf("\n", start - 1) + 1;
		const next = value.slice(0, lineStart) + prefix + value.slice(lineStart);
		onChange(next);
		requestAnimationFrame(() => {
			el.focus();
			el.setSelectionRange(lineStart + prefix.length, lineStart + prefix.length);
		});
	}, [onChange, value]);

	return (
		<div className="rounded-lg border border-solid border-colorBorderSecondary">
			<div className="flex items-center gap-1 border-b border-solid border-colorBorderSecondary px-2 py-1">
				<Segmented
					size="small"
					value={mode}
					onChange={value => setMode(value as "edit" | "preview")}
					options={[
						{ value: "edit", label: "编辑" },
						{ value: "preview", label: "预览" },
					]}
				/>
				<div className="ml-2 flex items-center gap-0.5">
					<Dropdown
						menu={{
							items: [
								{ key: "h1", label: "一级标题" },
								{ key: "h2", label: "二级标题" },
								{ key: "h3", label: "三级标题" },
								{ key: "h4", label: "四级标题" },
							],
							onClick: ({ key }) => insertLinePrefix(`${"#".repeat(Number(key.slice(1)))} `),
						}}
					>
						<Button type="text" size="small">标题</Button>
					</Dropdown>
					<Tooltip title="加粗">
						<Button type="text" size="small" onClick={() => insertSyntax("**", "**", "加粗文本")}>B</Button>
					</Tooltip>
					<Tooltip title="斜体">
						<Button type="text" size="small" className="italic" onClick={() => insertSyntax("*", "*", "斜体文本")}>I</Button>
					</Tooltip>
					<Tooltip title="链接">
						<Button type="text" size="small" onClick={() => insertSyntax("[", "](https://)", "链接文字")}>
							<LinkOutlined />
						</Button>
					</Tooltip>
					<Tooltip title="引用">
						<Button type="text" size="small" onClick={() => insertLinePrefix("> ")}>❝</Button>
					</Tooltip>
				</div>
			</div>
			{mode === "edit" ? (
				<Input.TextArea
					ref={textareaRef}
					value={value}
					onChange={event => onChange(event.target.value)}
					placeholder={placeholder}
					autoSize={{ minRows: 4, maxRows: 10 }}
					bordered={false}
					className="!px-3 !py-2"
				/>
			) : (
				<div className="max-h-[240px] min-h-[96px] overflow-auto px-3 py-2 text-[13px] leading-5">
					{value.trim() ? <ReactMarkdown>{value}</ReactMarkdown> : <Text type="secondary">暂无内容</Text>}
				</div>
			)}
			<div className="flex items-center justify-end gap-2 border-t border-solid border-colorBorderSecondary px-2 py-1.5">
				<Button size="small" onClick={onCancel}>取消</Button>
				<Button size="small" type="primary" icon={<SendOutlined />} loading={submitting} onClick={onSubmit}>
					{submitText}
				</Button>
			</div>
		</div>
	);
}

/** 工单详情内容体：头部（编号/上下条/全屏/更多）+ 操作栏 + 描述 + 活动日志四 Tab + 底部 Markdown 输入 */
export function TicketDetailContent({
	domainId,
	ticketId: initialTicketId,
	tickets,
	onClose,
	onChanged,
	onAssign,
	onToggleFullscreen,
	isFullscreen,
}: TicketDetailContentProps) {
	const { message: messageApi } = App.useApp();
	const [ticketId, setTicketId] = useState<string | null>(initialTicketId);
	const [loading, setLoading] = useState(false);
	const [ticket, setTicket] = useState<TicketRow | null>(null);
	const [history, setHistory] = useState<TicketHistoryRow[]>([]);
	const [replies, setReplies] = useState<TicketReplyRow[]>([]);
	const [watchers, setWatchers] = useState<number[]>([]);
	const [logFilter, setLogFilter] = useState<LogFilterKey>("all");
	const [descExpanded, setDescExpanded] = useState(false);
	const [descOverflow, setDescOverflow] = useState(false);
	const [replyDraft, setReplyDraft] = useState("");
	const [replyMode, setReplyMode] = useState<"conversation" | "note">("conversation");
	const [submitting, setSubmitting] = useState(false);
	const descWrapRef = useRef<HTMLDivElement>(null);
	const replyInputRef = useRef<HTMLDivElement>(null);

	const currentUserInfo = useAuthStore(state => state.user);

	const currentIndex = useMemo(
		() => (ticketId ? tickets.findIndex(row => row.id === ticketId) : -1),
		[ticketId, tickets],
	);
	const hasPrev = currentIndex > 0;
	const hasNext = currentIndex >= 0 && currentIndex < tickets.length - 1;

	/** 初始 ticketId 变化（打开/切换工单）时同步内部状态 */
	useEffect(() => {
		if (initialTicketId) {
			setTicketId(initialTicketId);
		}
	}, [initialTicketId]);

	const loadDetail = useCallback(async () => {
		if (!ticketId) {
			return;
		}
		setLoading(true);
		try {
			const detail: TicketDetailResult = await fetchTicketDetail(domainId, ticketId);
			setTicket(detail.ticket);
			setHistory(detail.history ?? []);
			setReplies(detail.replies ?? []);
			setWatchers(detail.watcherStaffAccountIds ?? []);
		}
		catch (error) {
			messageApi.error(toErrorMessage(error));
		}
		finally {
			setLoading(false);
		}
	}, [domainId, ticketId, messageApi]);

	useEffect(() => {
		setLogFilter("all");
		setReplyDraft("");
		setReplyMode("conversation");
		setDescExpanded(false);
		void loadDetail();
	}, [ticketId, loadDetail]);

	// 描述是否超出行数（>6 行时显示模糊展开）
	useEffect(() => {
		const el = descWrapRef.current;
		if (!el) {
			return;
		}
		setDescOverflow(el.scrollHeight > el.clientHeight + 2);
	}, [ticket?.description, descExpanded]);

	// 实时刷新：客户新回复/状态变更即时可见
	const accessToken = useAuthStore(state => state.token);
	useEffect(() => {
		if (!ticketId || !accessToken) {
			return;
		}
		realtimeClient.connect(accessToken);
		const onTicketEvent = (payload: Record<string, unknown>) => {
			if (String(payload.ticketId ?? "") === ticketId) {
				void loadDetail();
			}
		};
		realtimeClient.on(REALTIME_EVENT.TICKET_REPLIED, onTicketEvent);
		realtimeClient.on(REALTIME_EVENT.TICKET_UPDATED, onTicketEvent);
		return () => {
			realtimeClient.off(REALTIME_EVENT.TICKET_REPLIED, onTicketEvent);
			realtimeClient.off(REALTIME_EVENT.TICKET_UPDATED, onTicketEvent);
		};
	}, [ticketId, accessToken, loadDetail]);

	const navigateTo = useCallback((nextIndex: number) => {
		const next = tickets[nextIndex];
		if (next) {
			setTicketId(next.id);
		}
	}, [tickets]);

	const currentStaffAccountId = currentUserInfo?.id ? Number(currentUserInfo.id) : null;
	const watching = currentStaffAccountId != null && watchers.includes(currentStaffAccountId);

	const handleToggleWatch = useCallback(async () => {
		if (!ticketId || currentStaffAccountId == null) {
			return;
		}
		const next = watching
			? watchers.filter(id => id !== currentStaffAccountId)
			: [...watchers, currentStaffAccountId];
		try {
			await replaceAdminTicketWatchers(domainId, ticketId, { watcherStaffAccountIds: next });
			setWatchers(next);
			messageApi.success(watching ? "已取消关注" : "已关注");
		}
		catch (error) {
			messageApi.error(toErrorMessage(error));
		}
	}, [currentStaffAccountId, domainId, messageApi, ticketId, watching, watchers]);

	const handleCopyLink = useCallback(async () => {
		if (!ticketId) {
			return;
		}
		const ok = await copyText(ticketShareUrl(domainId, ticketId));
		messageApi.success(ok ? "链接已复制" : "复制失败");
	}, [domainId, messageApi, ticketId]);

	const handleCopyTitleAndLink = useCallback(async () => {
		if (!ticketId || !ticket) {
			return;
		}
		const ok = await copyText(`${ticket.title}\n${ticketShareUrl(domainId, ticketId)}`);
		messageApi.success(ok ? "标题和链接已复制" : "复制失败");
	}, [domainId, messageApi, ticket, ticketId]);

	const focusNote = useCallback(() => {
		setReplyMode("note");
		replyInputRef.current?.scrollIntoView({ behavior: "smooth", block: "nearest" });
	}, []);

	const handleSubmitReply = useCallback(async () => {
		if (!ticketId || !ticket || !replyDraft.trim()) {
			return;
		}
		setSubmitting(true);
		try {
			await replyAdminTicket(domainId, ticketId, {
				version: ticket.version,
				content: replyDraft.trim(),
				quickReplyTemplateId: null,
				attachmentIds: [],
				internal: replyMode === "note",
			});
			messageApi.success(replyMode === "note" ? "内部备注已添加" : "回复已发送");
			setReplyDraft("");
			await loadDetail();
			onChanged?.();
		}
		catch (error) {
			messageApi.error(toErrorMessage(error));
		}
		finally {
			setSubmitting(false);
		}
	}, [domainId, loadDetail, messageApi, onChanged, replyDraft, replyMode, ticket, ticketId]);

	const visibleItems = useMemo(() => {
		// 合并回复与系统操作，统一按时间排序
		const replyItems = replies.map(reply => ({
			kind: reply.replyType === "internal_note" ? "note" : "conversation",
			createdAt: reply.createdAt,
			payload: reply,
		}));
		const historyItems = history.map(item => ({ kind: "log", createdAt: item.createdAt, payload: item }));
		let merged = [...replyItems, ...historyItems].sort((a, b) =>
			dayjs(a.createdAt ?? 0).valueOf() - dayjs(b.createdAt ?? 0).valueOf());
		if (logFilter === "log") {
			merged = merged.filter(item => item.kind === "log");
		}
		else if (logFilter === "conversation") {
			merged = merged.filter(item => item.kind === "conversation");
		}
		else if (logFilter === "note") {
			merged = merged.filter(item => item.kind === "note");
		}
		return merged;
	}, [history, logFilter, replies]);

	const renderLogItem = (item: { kind: string; payload: TicketReplyRow | TicketHistoryRow }) => {
		if (item.kind === "log") {
			const row = item.payload as TicketHistoryRow;
			return (
				<div>
					<div>
						<Text>{ACTION_LABELS[row.action ?? ""] ?? row.action ?? "-"}</Text>
						{row.fromValue != null && row.toValue != null && (
							<Text type="secondary"> {row.fromValue} → {row.toValue}</Text>
						)}
					</div>
					<Text type="secondary" style={{ fontSize: 12 }}>{formatTime(row.createdAt)}</Text>
				</div>
			);
		}
		const reply = item.payload as TicketReplyRow;
		const isNote = reply.replyType === "internal_note";
		return (
			<div>
				<div className="flex items-center gap-2">
					{isNote ? <Tag color="default" style={{ marginInlineEnd: 0 }}>内部备注</Tag> : null}
					<Text type="secondary" style={{ fontSize: 12 }}>
						{reply.senderType === "staff" ? "客服" : "客户"}
					</Text>
					<Text type="secondary" style={{ fontSize: 12 }}>{formatTime(reply.createdAt)}</Text>
				</div>
				<Paragraph className={`!mb-0 whitespace-pre-wrap text-[13px] leading-5 ${isNote ? "text-colorTextSecondary" : ""}`}>
					{reply.content || "-"}
				</Paragraph>
			</div>
		);
	};

	if (!ticketId) {
		return <Empty description="请选择工单" className="py-16" />;
	}

	return (
		<div className="flex h-full flex-col">
			{/* 头部：编号 + 上一条/下一条 + 全屏 + 更多 */}
			<div className="flex shrink-0 items-center justify-between border-b border-solid border-colorBorderSecondary px-5 py-2.5">
				<Space size={8}>
					{ticket?.ticketTypeIcon?.trim() ? resolveMenuIcon(ticket.ticketTypeIcon, { fontSize: 14 }) : null}
					<Text strong>{ticket ? `#${ticket.ticketNo}` : "…"}</Text>
					<Button
						type="text"
						size="small"
						icon={<CopyOutlined />}
						title="复制编号"
						onClick={() => {
							if (ticket) {
								void copyText(ticket.ticketNo);
								messageApi.success("编号已复制");
							}
						}}
					/>
				</Space>
				<Space size={4}>
					<Button size="small" disabled={!hasPrev} onClick={() => navigateTo(currentIndex - 1)}>
						<SwapLeftOutlined />上一条
					</Button>
					<Button size="small" disabled={!hasNext} onClick={() => navigateTo(currentIndex + 1)}>
						下一条<SwapRightOutlined />
					</Button>
					<Tooltip title={isFullscreen ? "退出全屏" : "全屏"}>
						<Button
							size="small"
							icon={isFullscreen ? <FullscreenExitOutlined /> : <FullscreenOutlined />}
							onClick={onToggleFullscreen}
						/>
					</Tooltip>
					<Dropdown
						menu={{
							items: [
								{ key: "link", label: "复制链接" },
								{ key: "title-link", label: "复制标题和链接" },
							],
							onClick: ({ key }) => {
								if (key === "link") {
									void handleCopyLink();
								}
								else {
									void handleCopyTitleAndLink();
								}
							},
						}}
					>
						<Button size="small" icon={<EllipsisOutlined />} title="更多" />
					</Dropdown>
				</Space>
			</div>

			<Spin spinning={loading}>
				{ticket ? (
					<div className="min-h-0 flex-1 overflow-y-auto">
						<div className="flex flex-col gap-4 px-5 py-4">
							{/* 标题 + 操作栏 */}
							<div>
								<div className="flex items-center gap-2">
									<Title level={4} className="!mb-0">{ticket.title || "-"}</Title>
									{ticket.status ? <Tag color="blue">{STATUS_LABELS[ticket.status] ?? ticket.status}</Tag> : null}
								</div>
								<Space size={8} className="mt-2 flex-wrap">
									<Button size="small" onClick={() => onAssign?.(ticket)}>分配</Button>
									<Button
										size="small"
										type={watching ? "primary" : "default"}
										icon={<StarOutlined />}
										onClick={() => void handleToggleWatch()}
									>
										{watching ? "已关注" : "关注"}
									</Button>
									<Tooltip title="待开发">
										<Button size="small" disabled>标签</Button>
									</Tooltip>
									<Button size="small" onClick={focusNote}>备注</Button>
									<Dropdown
										menu={{
											items: [
												{ key: "link", label: "复制链接" },
												{ key: "title-link", label: "复制标题和链接" },
											],
											onClick: ({ key }) => {
												if (key === "link") {
													void handleCopyLink();
												}
												else {
													void handleCopyTitleAndLink();
												}
											},
										}}
									>
										<Button size="small">分享</Button>
									</Dropdown>
									<Dropdown
										menu={{
											items: [{ key: "close", label: "关闭", onClick: onClose }],
										}}
									>
										<Button size="small" icon={<EllipsisOutlined />} />
									</Dropdown>
								</Space>
							</div>

							{/* 描述：超行时模糊展开 */}
							<div
								ref={descWrapRef}
								className={`relative overflow-hidden ${descExpanded ? "" : "max-h-[132px]"}`}
							>
								<Paragraph className="!mb-0 whitespace-pre-wrap">{ticket.description || "暂无描述"}</Paragraph>
								{!descExpanded && descOverflow && (
									<>
										<div className="absolute inset-x-0 bottom-0 h-12 bg-gradient-to-t from-colorBgContainer via-colorBgContainer/70 to-transparent backdrop-blur-[1px]" />
										<button
											type="button"
											className="absolute bottom-1 left-1/2 -translate-x-1/2 rounded-full border-0 bg-colorBgContainer/70 px-3 py-0.5 text-xs text-[#1677ff] shadow backdrop-blur-md transition-colors hover:bg-colorBgContainer"
											onClick={() => setDescExpanded(true)}
										>
											展开描述
										</button>
									</>
								)}
							</div>

							{/* 活动日志：全部 / 只看日志 / 只看对话 / 只看备注 */}
							<div>
								<Tabs
									size="small"
									activeKey={logFilter}
									onChange={key => setLogFilter(key as LogFilterKey)}
									items={[
										{ key: "all", label: "全部" },
										{ key: "log", label: "只看日志" },
										{ key: "conversation", label: "只看对话" },
										{ key: "note", label: "只看备注" },
									]}
								/>
								<div className="max-h-[320px] overflow-y-auto">
									{visibleItems.length === 0 ? (
										<Empty description="暂无记录" image={Empty.PRESENTED_IMAGE_SIMPLE} />
									) : (
										<Timeline items={visibleItems.map(item => ({ children: renderLogItem(item) }))} />
									)}
								</div>
							</div>
						</div>
					</div>
				) : (
					!loading && <Empty description="工单不存在" className="py-16" />
				)}
			</Spin>

			{/* 底部：富文本对话/备注输入 */}
			<div ref={replyInputRef} className="shrink-0 border-t border-solid border-colorBorderSecondary p-4">
				<MarkdownEditor
					value={replyDraft}
					onChange={setReplyDraft}
					placeholder={replyMode === "note" ? "输入内部备注，客户不可见（支持 Markdown）" : "输入回复内容，支持 Markdown"}
					submitText={replyMode === "note" ? "添加备注" : "发表"}
					onSubmit={() => void handleSubmitReply()}
					onCancel={() => setReplyDraft("")}
					submitting={submitting}
				/>
			</div>
		</div>
	);
}
