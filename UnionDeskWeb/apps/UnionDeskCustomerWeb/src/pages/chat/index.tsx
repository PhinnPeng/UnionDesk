import {
	createCustomerConsultation,
	fetchConsultationAvailability,
	getMyConsultationMessages,
	listCustomerMyConsultations,
	loadAuthSession,
	realtimeClient,
	REALTIME_EVENT,
	replyCustomerConsultation,
	toErrorMessage,
	useCustomerPortal,
	type ConsultationMessageRow,
	type ConsultationSessionRow,
} from "@uniondesk/shared";
import { useCallback, useEffect, useRef, useState, type FormEvent } from "react";

import { useToast } from "../../components/Toast";
import { formatDateTime } from "../../utils/date";

function statusLabel(status: string): string {
	if (status === "closed") {
		return "已关闭";
	}
	return "进行中";
}

function sessionPreview(session: ConsultationSessionRow): string {
	if (session.linkedTicketNo) {
		return `已转工单 ${session.linkedTicketNo}`;
	}
	return `${statusLabel(session.sessionStatus)} · ${session.messageCount} 条消息`;
}

export default function ChatPage() {
	const portal = useCustomerPortal();
	const toast = useToast();
	const domainId = portal.activeDomain?.id ?? 0;

	const [sessions, setSessions] = useState<ConsultationSessionRow[]>([]);
	const [activeSessionNo, setActiveSessionNo] = useState<string | null>(null);
	const [messages, setMessages] = useState<ConsultationMessageRow[]>([]);
	const [draft, setDraft] = useState("");
	const [sending, setSending] = useState(false);
	const [createOpen, setCreateOpen] = useState(false);
	const [createDraft, setCreateDraft] = useState("");
	const [creating, setCreating] = useState(false);
	/** 当前无在线坐席提示（发起咨询时查询；排队中展示） */
	const [noAgentOnline, setNoAgentOnline] = useState(false);
	const messageEndRef = useRef<HTMLDivElement | null>(null);

	const activeSession = sessions.find(item => item.sessionNo === activeSessionNo) ?? null;

	const loadSessions = useCallback(async () => {
		if (!domainId) {
			return;
		}
		try {
			const rows = await listCustomerMyConsultations(domainId);
			setSessions(rows);
			setActiveSessionNo(prev => {
				if (prev && rows.some(item => item.sessionNo === prev)) {
					return prev;
				}
				return rows[0]?.sessionNo ?? null;
			});
		}
		catch (error) {
			toast.error(toErrorMessage(error));
		}
	}, [domainId, toast]);

	const loadMessages = useCallback(async (sessionNo: string) => {
		if (!domainId) {
			return;
		}
		try {
			const rows = await getMyConsultationMessages(domainId, sessionNo);
			setMessages(rows);
		}
		catch (error) {
			toast.error(toErrorMessage(error));
		}
	}, [domainId, toast]);

	/** 查询坐席可用性：无在线坐席时提示排队等待 */
	const loadAvailability = useCallback(async () => {
		if (!domainId) {
			return;
		}
		try {
			const result = await fetchConsultationAvailability(String(domainId));
			setNoAgentOnline(!result.hasOnlineAgent);
		}
		catch {
			setNoAgentOnline(false);
		}
	}, [domainId]);

	useEffect(() => {
		void loadSessions();
		void loadAvailability();
	}, [loadSessions, loadAvailability]);

	// 实时通道：连接 + 订阅（替代 3s 轮询）
	useEffect(() => {
		const token = loadAuthSession()?.accessToken;
		if (!token) {
			return;
		}
		realtimeClient.connect(token);
		realtimeClient.onReady(() => {
			// 重连后拉全量（WS 丢帧兜底）
			void loadSessions();
			if (activeSessionNo) {
				void loadMessages(activeSessionNo);
			}
		});
		const onChatMessage = (payload: Record<string, unknown>) => {
			const sessionNo = String(payload.sessionNo ?? "");
			const messageId = String(payload.messageId ?? "");
			if (!sessionNo) {
				return;
			}
			if (sessionNo === activeSessionNo) {
				setMessages(prev => prev.some(item => String(item.id) === messageId)
					? prev
					: [...prev, {
						id: messageId,
						sessionNo,
						seqNo: 0,
						businessDomainId: String(domainId),
						senderRole: String(payload.senderRole ?? "agent"),
						messageType: "text",
						content: String(payload.content ?? ""),
						createdAt: String(payload.createdAt ?? new Date().toISOString()),
					}]);
			}
			void loadSessions();
		};
		const onChatSession = () => {
			void loadSessions();
		};
		realtimeClient.on(REALTIME_EVENT.CHAT_MESSAGE, onChatMessage);
		realtimeClient.on(REALTIME_EVENT.CHAT_SESSION, onChatSession);
		return () => {
			realtimeClient.off(REALTIME_EVENT.CHAT_MESSAGE, onChatMessage);
			realtimeClient.off(REALTIME_EVENT.CHAT_SESSION, onChatSession);
		};
	// eslint-disable-next-line react-hooks/exhaustive-deps -- activeSessionNo 由回调内读取最新值
	}, [loadMessages, loadSessions, domainId]);

	useEffect(() => {
		if (!activeSessionNo) {
			setMessages([]);
			return;
		}
		void loadMessages(activeSessionNo);
	}, [activeSessionNo, loadMessages]);

	useEffect(() => {
		messageEndRef.current?.scrollIntoView({ block: "end" });
	}, [messages]);

	const handleSend = async (event: FormEvent) => {
		event.preventDefault();
		const content = draft.trim();
		if (!content || !activeSessionNo || !domainId) {
			return;
		}
		setSending(true);
		try {
			const row = await replyCustomerConsultation(domainId, activeSessionNo, content);
			setMessages(prev => [...prev, row]);
			setDraft("");
			await loadSessions();
		}
		catch (error) {
			toast.error(toErrorMessage(error));
		}
		finally {
			setSending(false);
		}
	};

	const handleCreate = async (event: FormEvent) => {
		event.preventDefault();
		const content = createDraft.trim();
		if (!content || !domainId) {
			return;
		}
		setCreating(true);
		try {
			const session = await createCustomerConsultation(domainId, content);
			setCreateOpen(false);
			setCreateDraft("");
			setActiveSessionNo(session.sessionNo);
			await loadSessions();
			await loadAvailability();
			toast.success("咨询已发起");
		}
		catch (error) {
			toast.error(toErrorMessage(error));
		}
		finally {
			setCreating(false);
		}
	};

	return (
		<div className="ud-stack ud-stack--lg">
			<header className="ud-row ud-row--between">
				<div>
					<p className="ud-kicker">在线服务</p>
					<h1 className="ud-title" style={{ fontSize: 28 }}>在线咨询</h1>
					<p className="ud-subtitle" style={{ marginBottom: 0 }}>
						与客服在线沟通，需要时可将会话转为工单继续跟进。
					</p>
				</div>
				<button
					type="button"
					className="ud-btn ud-btn--primary"
					onClick={() => {
						void loadAvailability();
						setCreateOpen(true);
					}}
				>
					发起咨询
				</button>
			</header>

			{noAgentOnline ? (
				<div
					role="alert"
					style={{
						padding: "10px 14px",
						borderRadius: 8,
						backgroundColor: "#eef4ff",
						color: "#1d4ed8",
						fontSize: 13,
					}}
				>
					当前暂无坐席在线，您的咨询将进入排队，客服上线后会第一时间接入。
				</div>
			) : null}

			<div className="ud-chat-layout">
				<aside className="ud-glass ud-chat-sessions" aria-label="咨询会话列表">
					{sessions.length === 0
						? <div className="ud-chat-sessions__empty">暂无咨询会话</div>
						: sessions.map(item => (
							<button
								key={item.sessionNo}
								type="button"
								className={item.sessionNo === activeSessionNo ? "ud-chat-session is-active" : "ud-chat-session"}
								onClick={() => setActiveSessionNo(item.sessionNo)}
							>
								<strong>{item.businessDomainName}</strong>
								<span className={item.sessionStatus === "closed" ? "ud-chat-session__meta is-closed" : "ud-chat-session__meta"}>
									{sessionPreview(item)}
								</span>
								<span className="ud-chat-session__time">{formatDateTime(item.updatedAt)}</span>
							</button>
						))}
				</aside>

				<section className="ud-glass ud-chat-panel">
					{!activeSession
						? <div className="ud-chat-panel__empty">选择左侧会话开始沟通</div>
						: (
							<>
								<header className="ud-chat-panel__head">
									<div>
										<strong>{activeSession.businessDomainName}</strong>
										<span className={activeSession.sessionStatus === "closed" ? "ud-tag" : "ud-tag ud-tag--blue"}>
											{statusLabel(activeSession.sessionStatus)}
										</span>
										{activeSession.linkedTicketNo
											? <span className="ud-tag ud-tag--green">已转工单 {activeSession.linkedTicketNo}</span>
											: null}
									</div>
								</header>
								{activeSession.sessionStatus === "queued" && noAgentOnline ? (
									<div
										role="alert"
										style={{
											margin: 12,
											padding: "10px 14px",
											borderRadius: 8,
											backgroundColor: "#eef4ff",
											color: "#1d4ed8",
											fontSize: 13,
										}}
									>
										当前暂无坐席在线，正在排队等待接入…
									</div>
								) : null}
								<div className="ud-chat-messages">
									{messages.length === 0
										? <div className="ud-chat-panel__empty">暂无消息，发送第一条消息开始咨询</div>
										: messages.map(item => (
											<div
												key={item.id}
												className={item.senderRole === "customer" ? "ud-chat-msg ud-chat-msg--mine" : "ud-chat-msg"}
											>
												<div className="ud-chat-msg__bubble">
													<p>{item.content}</p>
													<span className="ud-chat-msg__time">{formatDateTime(item.createdAt)}</span>
												</div>
											</div>
										))}
									<div ref={messageEndRef} />
								</div>
								<form className="ud-chat-composer" onSubmit={handleSend}>
									<textarea
										className="ud-textarea"
										rows={3}
										placeholder="输入消息内容…"
										value={draft}
										disabled={activeSession.sessionStatus === "closed"}
										onChange={event => setDraft(event.target.value)}
									/>
									<button
										type="submit"
										className="ud-btn ud-btn--primary"
										disabled={sending || !draft.trim() || activeSession.sessionStatus === "closed"}
									>
										{sending ? "发送中…" : "发送"}
									</button>
								</form>
							</>
						)}
				</section>
			</div>

			{createOpen
				? (
					<div className="ud-modal" role="dialog" aria-modal="true" aria-label="发起咨询">
						<form className="ud-modal__card" onSubmit={handleCreate}>
							<h2 className="ud-title" style={{ fontSize: 20 }}>发起咨询</h2>
							<p className="ud-muted" style={{ margin: "4px 0 12px" }}>
								当前业务域：{portal.activeDomain?.name ?? "未选择"}
							</p>
							{noAgentOnline ? (
								<p className="ud-muted" style={{ margin: "0 0 12px", color: "#b54708" }}>
									当前暂无坐席在线，提交后将进入排队，客服上线后第一时间接入。
								</p>
							) : null}
							<textarea
								className="ud-textarea"
								rows={4}
								autoFocus
								placeholder="请描述您想咨询的问题…"
								value={createDraft}
								onChange={event => setCreateDraft(event.target.value)}
							/>
							<div style={{ display: "flex", justifyContent: "flex-end", gap: 8 }}>
								<button
									type="button"
									className="ud-btn ud-btn--ghost"
									onClick={() => {
										setCreateOpen(false);
										setCreateDraft("");
									}}
								>
									取消
								</button>
								<button
									type="submit"
									className="ud-btn ud-btn--primary"
									disabled={creating || !createDraft.trim()}
								>
									{creating ? "提交中…" : "发起"}
								</button>
							</div>
						</form>
					</div>
				)
				: null}
		</div>
	);
}
