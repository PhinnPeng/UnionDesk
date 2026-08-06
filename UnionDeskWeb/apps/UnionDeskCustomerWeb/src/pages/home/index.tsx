import { refreshCustomerTicketsLive, useCustomerPortal } from "@uniondesk/shared";
import { useEffect, useMemo } from "react";
import { Link, useNavigate } from "react-router-dom";

import { IconPlus } from "../../components/Icons";
import { StatusTag } from "../../components/StatusTag";
import { formatDateTime } from "../../utils/date";
import { countByLifecycle } from "../../utils/ticket-lifecycle";

function greetingByHour(hour: number): string {
	if (hour < 12) {
		return "上午好";
	}
	if (hour < 18) {
		return "下午好";
	}
	return "晚上好";
}

export default function HomePage() {
	const portal = useCustomerPortal();
	const navigate = useNavigate();

	useEffect(() => {
		void refreshCustomerTicketsLive().catch(() => undefined);
	}, [portal.activeDomain?.id]);

	const recent = portal.currentDomainTickets.slice(0, 5);
	const life = useMemo(
		() => countByLifecycle(portal.currentDomainTickets.map(item => item.status)),
		[portal.currentDomainTickets],
	);

	if (!portal.activeDomain) {
		return (
			<section className="ud-glass ud-glass--lg" style={{ padding: 28 }}>
				<p className="ud-kicker">尚未选择业务域</p>
				<h1 className="ud-title" style={{ fontSize: 28 }}>先选择一个服务空间</h1>
				<p className="ud-subtitle">选择或加入业务域后，即可提交工单与接收通知。</p>
				<button type="button" className="ud-btn ud-btn--primary" style={{ marginTop: 16 }} onClick={() => navigate("/domains")}>
					去选择业务域
				</button>
			</section>
		);
	}

	return (
		<div className="ud-stack ud-stack--lg">
			<section className="ud-glass ud-glass--lg ud-home-hero" aria-label="欢迎">
				<div className="ud-home-hero__copy">
					<p className="ud-kicker">{greetingByHour(new Date().getHours())}</p>
					<h1 className="ud-title">
						你好，
						{portal.account?.displayName ?? "朋友"}
					</h1>
					<p className="ud-subtitle">提交问题、跟踪进度。需要列表请去「工单」。</p>
				</div>
				<Link to="/tickets/new" className="ud-btn ud-btn--primary ud-home-hero__cta">
					<IconPlus />
					提交工单
				</Link>
			</section>

			<section className="ud-life-grid" aria-label="工单生命周期概览">
				<Link to="/tickets?life=pending" className="ud-glass ud-life-card ud-life-card--pending">
					<div className="ud-life-card__value">{life.pending}</div>
					<div className="ud-life-card__label">待处理</div>
				</Link>
				<Link to="/tickets?life=active" className="ud-glass ud-life-card ud-life-card--active">
					<div className="ud-life-card__value">{life.active}</div>
					<div className="ud-life-card__label">进行中</div>
				</Link>
				<Link to="/tickets?life=done" className="ud-glass ud-life-card ud-life-card--done">
					<div className="ud-life-card__value">{life.done}</div>
					<div className="ud-life-card__label">已完成</div>
				</Link>
			</section>

			<section className="ud-stack">
				<div className="ud-row ud-row--between">
					<h2 className="ud-section-title">最近工单</h2>
					<div className="ud-row" style={{ gap: 14 }}>
						{portal.unreadCount > 0
							? (
								<Link to="/inbox" className="ud-quiet-link">
									{portal.unreadCount}
									{" "}
									条未读通知
								</Link>
							)
							: null}
						<Link to="/tickets">查看全部</Link>
					</div>
				</div>
				{recent.length === 0
					? (
						<div className="ud-glass ud-empty">还没有工单，试着提交第一个吧。</div>
					)
					: (
						<div className="ud-card-list">
							{recent.map(ticket => (
								<Link key={ticket.id} to={`/tickets/${ticket.id}`} className="ud-glass ud-ticket">
									<div className="ud-row ud-row--between">
										<span className="ud-tag ud-tag--blue">{ticket.typeName}</span>
										<StatusTag status={ticket.status} />
									</div>
									<h3 className="ud-ticket__title">{ticket.title}</h3>
									<p className="ud-muted" style={{ margin: 0 }}>
										<span className="ud-mono">{ticket.ticketNo}</span>
										{" · "}
										{formatDateTime(ticket.updatedAt)}
									</p>
								</Link>
							))}
						</div>
					)}
			</section>
		</div>
	);
}
