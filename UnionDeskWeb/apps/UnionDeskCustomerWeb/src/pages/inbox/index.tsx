import {
	fetchCustomerInboxLive,
	markCustomerInboxReadLive,
	type CustomerPortalInboxMessage,
	toErrorMessage,
} from "@uniondesk/shared";
import { useCallback, useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";

import { useToast } from "../../components/Toast";
import { formatDateTime } from "../../utils/date";

export default function InboxPage() {
	const toast = useToast();
	const navigate = useNavigate();
	const [messages, setMessages] = useState<CustomerPortalInboxMessage[]>([]);
	const [unreadCount, setUnreadCount] = useState(0);
	const [loading, setLoading] = useState(true);

	const load = useCallback(async () => {
		setLoading(true);
		try {
			const result = await fetchCustomerInboxLive();
			setMessages(result.messages);
			setUnreadCount(result.unreadCount);
		}
		catch (error) {
			toast.error(toErrorMessage(error));
		}
		finally {
			setLoading(false);
		}
	}, [toast]);

	useEffect(() => {
		void load();
	}, [load]);

	const handleMarkRead = async (item: CustomerPortalInboxMessage) => {
		try {
			await markCustomerInboxReadLive(item.id);
			setMessages(prev => prev.map(row => (row.id === item.id ? { ...row, isRead: true } : row)));
			setUnreadCount(prev => Math.max(0, prev - (item.isRead ? 0 : 1)));
			toast.success("已标记为已读");
		}
		catch (error) {
			toast.error(toErrorMessage(error));
		}
	};

	return (
		<div className="ud-stack ud-stack--lg">
			<header>
				<p className="ud-kicker">消息</p>
				<h1 className="ud-title" style={{ fontSize: 32 }}>通知</h1>
				<p className="ud-subtitle">
					工单进展与系统消息会汇聚在这里
					{unreadCount > 0 ? ` · 未读 ${unreadCount}` : ""}
					。
				</p>
			</header>

			{loading
				? <div className="ud-glass ud-empty">加载中…</div>
				: messages.length === 0
					? <div className="ud-glass ud-empty">暂无通知</div>
					: (
						<div className="ud-card-list">
							{messages.map(item => (
								<article key={item.id} className="ud-glass ud-ticket" style={{ cursor: "default" }}>
									<div className="ud-row ud-row--between">
										<div className="ud-row">
											<strong>{item.title}</strong>
											{item.isRead
												? <span className="ud-tag">已读</span>
												: <span className="ud-tag ud-tag--red">未读</span>}
											<span className="ud-tag ud-tag--blue">
												{item.kind === "system" ? "登录安全" : item.kind === "ticket" ? "工单" : item.kind}
											</span>
										</div>
										<span className="ud-muted" style={{ fontSize: 12 }}>{formatDateTime(item.createdAt)}</span>
									</div>
									<p className="ud-muted" style={{ margin: 0 }}>{item.content}</p>
									<div className="ud-row">
										<button
											type="button"
											className="ud-btn ud-btn--ghost ud-btn--sm"
											disabled={item.isRead}
											onClick={() => {
												void handleMarkRead(item);
											}}
										>
											标为已读
										</button>
										<button
											type="button"
											className="ud-btn ud-btn--primary ud-btn--sm"
											onClick={() => {
												if (!item.isRead) {
													void handleMarkRead(item);
												}
												const jump = item.jumpUrl?.startsWith("/workspace")
													? "/home"
													: (item.jumpUrl || "/home");
												navigate(jump);
											}}
										>
											查看
										</button>
									</div>
								</article>
							))}
						</div>
					)}
		</div>
	);
}
