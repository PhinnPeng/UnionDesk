import { refreshCustomerTicketTypesLive, refreshCustomerTicketsLive, useCustomerPortal } from "@uniondesk/shared";
import { useEffect, useMemo, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";

import { StatusTag } from "../../components/StatusTag";
import { formatDateTime } from "../../utils/date";
import {
	LIFECYCLE_FILTERS,
	matchesLifecycle,
	type LifecycleBucket,
} from "../../utils/ticket-lifecycle";

function parseLife(value: string | null): "all" | LifecycleBucket {
	if (value === "pending" || value === "active" || value === "done") {
		return value;
	}
	return "all";
}

export default function TicketsPage() {
	const portal = useCustomerPortal();
	const [params, setParams] = useSearchParams();
	const [typeId, setTypeId] = useState<string>("all");
	const [life, setLife] = useState<"all" | LifecycleBucket>(() => parseLife(params.get("life")));
	const [keyword, setKeyword] = useState(() => params.get("q") ?? "");

	useEffect(() => {
		void refreshCustomerTicketsLive().catch(() => undefined);
		void refreshCustomerTicketTypesLive().catch(() => undefined);
	}, [portal.activeDomain?.id]);

	useEffect(() => {
		setLife(parseLife(params.get("life")));
		setKeyword(params.get("q") ?? "");
	}, [params]);

	const typeCounts = useMemo(() => {
		const map = new Map<string, number>();
		for (const ticket of portal.currentDomainTickets) {
			map.set(ticket.typeId, (map.get(ticket.typeId) ?? 0) + 1);
		}
		return map;
	}, [portal.currentDomainTickets]);

	const tickets = useMemo(() => {
		const q = keyword.trim().toLowerCase();
		return portal.currentDomainTickets.filter((ticket) => {
			if (typeId !== "all" && ticket.typeId !== typeId) {
				return false;
			}
			if (!matchesLifecycle(ticket.status, life)) {
				return false;
			}
			if (!q) {
				return true;
			}
			return (
				ticket.title.toLowerCase().includes(q)
				|| ticket.ticketNo.toLowerCase().includes(q)
				|| ticket.typeName.toLowerCase().includes(q)
			);
		});
	}, [keyword, life, portal.currentDomainTickets, typeId]);

	const setLifeFilter = (next: "all" | LifecycleBucket) => {
		setLife(next);
		const nextParams = new URLSearchParams(params);
		if (next === "all") {
			nextParams.delete("life");
		}
		else {
			nextParams.set("life", next);
		}
		setParams(nextParams, { replace: true });
	};

	return (
		<div className="ud-stack ud-stack--lg">
			<header className="ud-row ud-row--between">
				<div>
					<p className="ud-kicker">服务跟踪</p>
					<h1 className="ud-title" style={{ fontSize: 28 }}>我的工单</h1>
				</div>
				<Link to="/tickets/new" className="ud-btn ud-btn--primary">新建</Link>
			</header>

			<div className="ud-tickets-layout">
				<aside className="ud-glass ud-type-rail" aria-label="工单类型">
					<button
						type="button"
						className={typeId === "all" ? "ud-type-rail__item is-active" : "ud-type-rail__item"}
						onClick={() => setTypeId("all")}
					>
						<span>全部类型</span>
						<span className="ud-type-rail__count">{portal.currentDomainTickets.length}</span>
					</button>
					{portal.ticketTypes.map(type => (
						<button
							key={type.id}
							type="button"
							className={typeId === type.id ? "ud-type-rail__item is-active" : "ud-type-rail__item"}
							onClick={() => setTypeId(type.id)}
						>
							<span>{type.name}</span>
							<span className="ud-type-rail__count">{typeCounts.get(type.id) ?? 0}</span>
						</button>
					))}
				</aside>

				<section className="ud-tickets-main">
					<input
						className="ud-input"
						placeholder="搜索标题、工单号、类型"
						value={keyword}
						onChange={(event) => {
							const value = event.target.value;
							setKeyword(value);
							const nextParams = new URLSearchParams(params);
							if (value.trim()) {
								nextParams.set("q", value.trim());
							}
							else {
								nextParams.delete("q");
							}
							setParams(nextParams, { replace: true });
						}}
					/>
					<div className="ud-segment" aria-label="生命周期">
						{LIFECYCLE_FILTERS.map(item => (
							<button
								key={item.key}
								type="button"
								className={life === item.key ? "is-active" : undefined}
								onClick={() => setLifeFilter(item.key)}
							>
								{item.label}
							</button>
						))}
					</div>

					{tickets.length === 0
						? <div className="ud-glass ud-empty">没有匹配的工单</div>
						: (
							<div className="ud-card-list">
								{tickets.map(ticket => (
									<Link key={ticket.id} to={`/tickets/${ticket.id}`} className="ud-glass ud-ticket">
										<div className="ud-row ud-row--between">
											<span className="ud-tag ud-tag--blue">{ticket.typeName}</span>
											<StatusTag status={ticket.status} />
										</div>
										<h3 className="ud-ticket__title">{ticket.title}</h3>
										<p className="ud-muted" style={{ margin: 0 }}>
											<span className="ud-mono">{ticket.ticketNo}</span>
											{" · 更新于 "}
											{formatDateTime(ticket.updatedAt)}
										</p>
									</Link>
								))}
							</div>
						)}
				</section>
			</div>
		</div>
	);
}
