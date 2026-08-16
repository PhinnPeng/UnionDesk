import {
	fetchSatisfactionLive,
	getCustomerTicketLive,
	loadAuthSession,
	realtimeClient,
	REALTIME_EVENT,
	replyCustomerTicketLive,
	submitSatisfactionLive,
	trackEvent,
	withdrawCustomerTicketLive,
	type CustomerPortalTicket,
	type CustomerSatisfactionView,
} from "@uniondesk/shared";
import { useEffect, useState, type FormEvent } from "react";
import { useNavigate, useParams } from "react-router-dom";

import { IconBack } from "../../components/Icons";
import { StatusTag } from "../../components/StatusTag";
import { useToast } from "../../components/Toast";
import { formatDateTime } from "../../utils/date";

const STAR_ACTIVE_COLOR = "#f5a623";
const STAR_INACTIVE_COLOR = "#d1d5db";

type StarRatingProps = {
	value: number;
	onChange?: (value: number) => void;
	readOnly?: boolean;
};

function StarRating({ value, onChange, readOnly }: StarRatingProps) {
	return (
		<div className="ud-row" style={{ gap: 4 }}>
			{[1, 2, 3, 4, 5].map(star => (
				<button
					key={star}
					type="button"
					disabled={readOnly}
					aria-label={`${star} 星`}
					style={{
						background: "none",
						border: "none",
						padding: "0 2px",
						fontSize: 26,
						lineHeight: 1,
						cursor: readOnly ? "default" : "pointer",
						color: star <= value ? STAR_ACTIVE_COLOR : STAR_INACTIVE_COLOR,
					}}
					onClick={() => onChange?.(star)}
				>
					★
				</button>
			))}
		</div>
	);
}

export default function TicketDetailPage() {
	const toast = useToast();
	const navigate = useNavigate();
	const params = useParams();
	const ticketId = params.ticketId ?? "";
	const [ticket, setTicket] = useState<CustomerPortalTicket | null>(null);
	const [version, setVersion] = useState(0);
	const [loading, setLoading] = useState(true);
	const [reply, setReply] = useState("");
	const [submitting, setSubmitting] = useState(false);
	const [satisfaction, setSatisfaction] = useState<CustomerSatisfactionView | null>(null);
	const [rating, setRating] = useState(0);
	const [comment, setComment] = useState("");
	const [submittingSatisfaction, setSubmittingSatisfaction] = useState(false);

	const loadSatisfaction = async (target: CustomerPortalTicket | null) => {
		if (!target || (target.status !== "closed" && target.status !== "resolved")) {
			setSatisfaction(null);
			return;
		}
		try {
			const result = await fetchSatisfactionLive(target.id);
			setSatisfaction(result);
			trackEvent("satisfaction.view", { ticketId: target.id });
		}
		catch (error) {
			setSatisfaction(null);
		}
	};

	const reload = async () => {
		if (!ticketId) {
			setTicket(null);
			setLoading(false);
			return;
		}
		setLoading(true);
		try {
			const detail = await getCustomerTicketLive(ticketId);
			setTicket(detail?.ticket ?? null);
			setVersion(detail?.version ?? 0);
			await loadSatisfaction(detail?.ticket ?? null);
		}
		catch (error) {
			toast.error(error instanceof Error ? error.message : "加载失败");
			setTicket(null);
		}
		finally {
			setLoading(false);
		}
	};

	useEffect(() => {
		void reload();
		// eslint-disable-next-line react-hooks/exhaustive-deps -- reload on ticket id only
	}, [ticketId]);

	// 实时刷新：客服回复/状态变更即时可见（ticket.replied / ticket.updated）
	useEffect(() => {
		if (!ticketId) {
			return;
		}
		const token = loadAuthSession()?.accessToken;
		if (token) {
			realtimeClient.connect(token);
		}
		const onTicketEvent = (payload: Record<string, unknown>) => {
			if (String(payload.ticketId ?? "") === ticketId) {
				void reload();
			}
		};
		realtimeClient.on(REALTIME_EVENT.TICKET_REPLIED, onTicketEvent);
		realtimeClient.on(REALTIME_EVENT.TICKET_UPDATED, onTicketEvent);
		return () => {
			realtimeClient.off(REALTIME_EVENT.TICKET_REPLIED, onTicketEvent);
			realtimeClient.off(REALTIME_EVENT.TICKET_UPDATED, onTicketEvent);
		};
	// eslint-disable-next-line react-hooks/exhaustive-deps -- reload 内部依赖 ticketId
	}, [ticketId]);

	if (loading) {
		return <div className="ud-glass ud-empty">加载中…</div>;
	}

	if (!ticket) {
		return (
			<section className="ud-glass ud-glass--lg ud-empty">
				<p>工单不存在或不属于当前账号</p>
				<button type="button" className="ud-btn ud-btn--primary" onClick={() => navigate("/tickets")}>
					返回列表
				</button>
			</section>
		);
	}

	const handleWithdraw = async () => {
		try {
			await withdrawCustomerTicketLive(ticket.id, version);
			toast.success("工单已撤回");
			await reload();
		}
		catch (error) {
			toast.error(error instanceof Error ? error.message : "撤回失败");
		}
	};

	const handleReply = async (event: FormEvent) => {
		event.preventDefault();
		const content = reply.trim();
		if (!content) {
			return;
		}
		setSubmitting(true);
		try {
			await replyCustomerTicketLive(ticket.id, content, version);
			setReply("");
			toast.success("已补充说明");
			await reload();
		}
		catch (error) {
			toast.error(error instanceof Error ? error.message : "发送失败");
		}
		finally {
			setSubmitting(false);
		}
	};

	const handleSubmitSatisfaction = async () => {
		if (rating < 1) {
			return;
		}
		setSubmittingSatisfaction(true);
		try {
			await submitSatisfactionLive(ticket.id, { rating, comment: comment.trim() });
			trackEvent("satisfaction.submit", { ticketId: ticket.id, rating });
			toast.success("感谢您的评价");
			setRating(0);
			setComment("");
			await reload();
		}
		catch (error) {
			toast.error(error instanceof Error ? error.message : "提交失败");
		}
		finally {
			setSubmittingSatisfaction(false);
		}
	};

	return (
		<div className="ud-stack ud-stack--lg">
			<div className="ud-row">
				<button type="button" className="ud-btn ud-btn--ghost ud-btn--sm" onClick={() => navigate(-1)}>
					<IconBack />
					返回
				</button>
				<span className="ud-mono ud-muted">{ticket.ticketNo}</span>
				<StatusTag status={ticket.status} />
			</div>

			<div className="ud-detail-layout">
				<div className="ud-stack">
					<header className="ud-glass ud-glass--lg" style={{ padding: 22 }}>
						<p className="ud-kicker">{ticket.typeName}</p>
						<h1 className="ud-title" style={{ fontSize: 28 }}>{ticket.title}</h1>
						<p className="ud-subtitle">{ticket.description}</p>
					</header>

					<section className="ud-glass ud-glass--lg" style={{ padding: 22 }}>
						<h2 className="ud-section-title">公开动态</h2>
						<div className="ud-timeline" style={{ marginTop: 16 }}>
							{[...ticket.replies].reverse().map(item => (
								<div key={item.id} className="ud-timeline__item">
									<span className="ud-timeline__dot" />
									<div>
										<div className="ud-row">
											<strong>{item.authorName}</strong>
											<span className="ud-tag">{item.authorType}</span>
											<span className="ud-muted" style={{ fontSize: 12 }}>{formatDateTime(item.createdAt)}</span>
										</div>
										<p className="ud-muted" style={{ margin: "6px 0 0" }}>{item.content}</p>
									</div>
								</div>
							))}
							{ticket.replies.length === 0
								? <p className="ud-muted">暂无动态</p>
								: null}
						</div>
					</section>

					{ticket.status !== "withdrawn" && ticket.status !== "closed"
						? (
							<form className="ud-glass ud-glass--lg ud-form" style={{ padding: 18 }} onSubmit={handleReply}>
								<div className="ud-field">
									<label htmlFor="reply">补充说明</label>
									<textarea
										id="reply"
										className="ud-textarea"
										placeholder="补充材料、最新进展…"
										value={reply}
										onChange={event => setReply(event.target.value)}
									/>
								</div>
								<button className="ud-btn ud-btn--primary" type="submit" disabled={submitting}>
									{submitting ? "发送中…" : "发送"}
								</button>
							</form>
						)
						: null}

					{ticket.status === "closed" || ticket.status === "resolved"
						? (
							<section className="ud-glass ud-glass--lg" style={{ padding: 18 }}>
								<h2 className="ud-section-title">服务评价</h2>
								{satisfaction
									? (
										<div className="ud-stack" style={{ marginTop: 12, gap: 8 }}>
											<div className="ud-row">
												<StarRating value={satisfaction.rating} readOnly />
												<span className="ud-muted" style={{ fontSize: 12 }}>
													已评价 · {formatDateTime(satisfaction.createdAt)}
												</span>
											</div>
											{satisfaction.comment
												? <p className="ud-muted" style={{ margin: 0 }}>{satisfaction.comment}</p>
												: null}
										</div>
									)
									: (
										<div className="ud-stack" style={{ marginTop: 12, gap: 10 }}>
											<p className="ud-muted" style={{ margin: 0 }}>请为本次服务打分（1-5 星）：</p>
											<StarRating value={rating} onChange={setRating} />
											<textarea
												className="ud-textarea"
												placeholder="说说您的体验（选填）…"
												value={comment}
												onChange={event => setComment(event.target.value)}
											/>
											<div>
												<button
													type="button"
													className="ud-btn ud-btn--primary"
													disabled={submittingSatisfaction || rating < 1}
													onClick={() => void handleSubmitSatisfaction()}
												>
													{submittingSatisfaction ? "提交中…" : "提交评价"}
												</button>
											</div>
										</div>
									)}
							</section>
						)
						: null}
				</div>

				<aside className="ud-detail-side">
					<section className="ud-glass" style={{ padding: 18 }}>
						<h2 className="ud-section-title">工单信息</h2>
						<div className="ud-stack" style={{ marginTop: 12, gap: 8 }}>
							<p className="ud-muted" style={{ margin: 0 }}>
								类型：
								{ticket.typeName}
							</p>
							<p className="ud-muted" style={{ margin: 0 }}>
								创建：
								{formatDateTime(ticket.createdAt)}
							</p>
							<p className="ud-muted" style={{ margin: 0 }}>
								更新：
								{formatDateTime(ticket.updatedAt)}
							</p>
						</div>
						{ticket.status === "open"
							? (
								<button type="button" className="ud-btn ud-btn--danger ud-btn--sm" style={{ marginTop: 14 }} onClick={() => void handleWithdraw()}>
									撤回工单
								</button>
							)
							: null}
					</section>

					<section className="ud-glass ud-placeholder" style={{ padding: 18 }} aria-disabled>
						<h2 className="ud-section-title">关联咨询</h2>
						<p>在线咨询即将开放。二期可从会话转工单并在此关联。</p>
					</section>
				</aside>
			</div>
		</div>
	);
}
