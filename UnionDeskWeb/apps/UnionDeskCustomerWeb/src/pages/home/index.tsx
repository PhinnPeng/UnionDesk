import { refreshCustomerTicketsLive, useCustomerPortal } from "@uniondesk/shared";
import { useEffect, useMemo } from "react";
import { Link, useNavigate } from "react-router-dom";

import {
	IconNotifShield,
	IconNotifTicket,
	IconStatCheck,
	IconStatClock,
	IconStatDoc,
} from "../../components/Icons";
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

function relativeTime(value?: string | null): string {
	if (!value) {
		return "";
	}
	const time = Date.parse(value);
	if (Number.isNaN(time)) {
		return "";
	}
	const diffMs = Date.now() - time;
	const minute = 60 * 1000;
	const hour = 60 * minute;
	const day = 24 * hour;
	if (diffMs < minute) {
		return "刚刚";
	}
	if (diffMs < hour) {
		return `${Math.floor(diffMs / minute)} 分钟前`;
	}
	if (diffMs < day) {
		return `${Math.floor(diffMs / hour)} 小时前`;
	}
	if (diffMs < 7 * day) {
		return `${Math.floor(diffMs / day)} 天前`;
	}
	return formatDateTime(value);
}

function safeJump(jumpUrl?: string | null): string {
	if (!jumpUrl || jumpUrl.startsWith("/workspace")) {
		return "/home";
	}
	return jumpUrl;
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
	const notifs = useMemo(() => {
		return [...portal.inboxMessages]
			.sort((left, right) => Number(left.isRead) - Number(right.isRead))
			.slice(0, 3);
	}, [portal.inboxMessages]);

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
		<div className="ud-dash">
			<section className="ud-welcome" aria-label="欢迎">
				<div className="ud-welcome__copy">
					<h1>{greetingByHour(new Date().getHours())}，{portal.account?.displayName ?? "朋友"}</h1>
					<p>
						{life.pending > 0
							? `有 ${life.pending} 个待处理的工单需要您的关注`
							: "暂时没有待处理的工单"}
					</p>
				</div>
				<Link to="/tickets?life=pending" className="ud-welcome__btn">
					查看待处理
				</Link>
			</section>

			<section className="ud-stat-grid" aria-label="工单生命周期概览">
				<Link className="ud-stat-card ud-stat-card--pending" to="/tickets?life=pending">
					<span className="ud-stat-card__icon"><IconStatDoc /></span>
					<strong>{life.pending}</strong>
					<span className="ud-stat-card__label">待处理</span>
				</Link>
				<Link className="ud-stat-card ud-stat-card--active" to="/tickets?life=active">
					<span className="ud-stat-card__icon"><IconStatClock /></span>
					<strong>{life.active}</strong>
					<span className="ud-stat-card__label">进行中</span>
				</Link>
				<Link className="ud-stat-card ud-stat-card--done" to="/tickets?life=done">
					<span className="ud-stat-card__icon"><IconStatCheck /></span>
					<strong>{life.done}</strong>
					<span className="ud-stat-card__label">已完成</span>
				</Link>
			</section>

			<section className="ud-dash-grid">
				<div className="ud-card">
					<header className="ud-card__head">
						<h2>我的工单</h2>
						<Link to="/tickets">查看全部 →</Link>
					</header>
					{recent.length === 0
						? (
							<div className="ud-empty">还没有工单，试着提交第一个吧。</div>
						)
						: (
							<div>
								{recent.map(ticket => (
									<Link key={ticket.id} to={`/tickets/${ticket.id}`} className="ud-ticket-row">
										<span className="ud-tag ud-tag--blue">{ticket.typeName}</span>
										<span className="ud-ticket-row__title">{ticket.title}</span>
										<StatusTag status={ticket.status} />
										<time>{relativeTime(ticket.updatedAt)}</time>
									</Link>
								))}
							</div>
						)}
				</div>

				<div className="ud-card">
					<header className="ud-card__head">
						<h2>未读通知</h2>
						<Link to="/inbox">全部通知 →</Link>
					</header>
					{notifs.length === 0
						? (
							<div className="ud-empty">暂无未读通知</div>
						)
						: (
							<div>
								{notifs.map(message => (
									<button
										key={message.id}
										type="button"
										className="ud-notif-row"
										onClick={() => navigate(safeJump(message.jumpUrl))}
									>
										<span className="ud-notif-row__icon">
											{message.kind === "system" ? <IconNotifShield /> : <IconNotifTicket />}
										</span>
										<span className="ud-notif-row__body">
											<span className="ud-notif-row__title">{message.title}</span>
											<time>{relativeTime(message.createdAt)}</time>
										</span>
									</button>
								))}
							</div>
						)}
				</div>
			</section>

			<p className="ud-foot-copy">© 2026 UnionDesk · 客户服务中心</p>
		</div>
	);
}
