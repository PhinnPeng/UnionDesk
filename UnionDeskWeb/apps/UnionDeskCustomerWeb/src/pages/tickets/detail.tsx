import {
	getCustomerTicketLive,
	replyCustomerTicketLive,
	withdrawCustomerTicketLive,
	type CustomerPortalTicket,
} from "@uniondesk/shared";
import { useEffect, useState, type FormEvent } from "react";
import { useNavigate, useParams } from "react-router-dom";

import { IconBack } from "../../components/Icons";
import { StatusTag } from "../../components/StatusTag";
import { useToast } from "../../components/Toast";
import { formatDateTime } from "../../utils/date";

export default function TicketDetailPage() {
	const toast = useToast();
	const navigate = useNavigate();
	const params = useParams();
	const ticketId = Number(params.ticketId);
	const [ticket, setTicket] = useState<CustomerPortalTicket | null>(null);
	const [version, setVersion] = useState(0);
	const [loading, setLoading] = useState(true);
	const [reply, setReply] = useState("");
	const [submitting, setSubmitting] = useState(false);

	const reload = async () => {
		if (!Number.isFinite(ticketId)) {
			setTicket(null);
			setLoading(false);
			return;
		}
		setLoading(true);
		try {
			const detail = await getCustomerTicketLive(ticketId);
			setTicket(detail?.ticket ?? null);
			setVersion(detail?.version ?? 0);
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
