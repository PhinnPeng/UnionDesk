import { useCustomerPortal } from "@uniondesk/shared";
import { useMemo, useState, type FormEvent } from "react";
import { NavLink, Outlet, useLocation, useNavigate } from "react-router-dom";

import {
	IconNavBell,
	IconNavChat,
	IconNavDomain,
	IconNavGear,
	IconNavHome,
	IconNavMessage,
	IconNavTicket,
	IconNavUser,
	IconSearchSolid,
} from "./Icons";

const navItems = [
	{ to: "/home", label: "首页", icon: IconNavHome, end: true },
	{ to: "/tickets", label: "工单", icon: IconNavTicket, end: false },
	{ to: "/chat", label: "咨询", icon: IconNavChat, end: false },
	{ to: "/inbox", label: "通知", icon: IconNavBell, end: false, badge: true },
	{ to: "/me", label: "我的", icon: IconNavUser, end: false },
] as const;

function pageTitle(pathname: string): string {
	if (pathname.startsWith("/tickets/new")) {
		return "提交工单";
	}
	if (pathname.startsWith("/tickets/")) {
		return "工单详情";
	}
	if (pathname.startsWith("/tickets")) {
		return "工单";
	}
	if (pathname.startsWith("/chat")) {
		return "咨询";
	}
	if (pathname.startsWith("/inbox")) {
		return "通知";
	}
	if (pathname.startsWith("/me")) {
		return "我的";
	}
	if (pathname.startsWith("/domains")) {
		return "业务域";
	}
	if (pathname.startsWith("/home")) {
		return "首页";
	}
	return "UnionDesk";
}

function NavItems({
	variant,
	unreadCount,
}: {
	variant: "rail" | "dock";
	unreadCount: number;
}) {
	const itemClass = variant === "rail" ? "ud-rail__item" : "ud-dock__item";
	const badgeClass = variant === "rail" ? "ud-rail__badge" : "ud-dock__badge";

	return (
		<>
			{navItems.map((item) => {
				const Icon = item.icon;
				return (
					<NavLink
						key={item.to}
						to={item.to}
						end={"end" in item ? item.end : false}
						className={({ isActive }) =>
							[itemClass, isActive ? "is-active" : ""].filter(Boolean).join(" ")
						}
					>
						<Icon />
						<span>{item.label}</span>
						{"badge" in item && item.badge && unreadCount > 0
							? <i className={badgeClass}>{unreadCount > 9 ? "9+" : unreadCount}</i>
							: null}
					</NavLink>
				);
			})}
		</>
	);
}

export default function AppShell() {
	const portal = useCustomerPortal();
	const navigate = useNavigate();
	const location = useLocation();
	const title = useMemo(() => pageTitle(location.pathname), [location.pathname]);
	const [search, setSearch] = useState("");

	const avatarLetter = (portal.account?.displayName?.trim()?.[0] ?? "U").toUpperCase();
	const displayName = portal.account?.displayName ?? "未登录";
	const domainName = portal.activeDomain?.name ?? "未选择";

	const onSearch = (event: FormEvent) => {
		event.preventDefault();
		const q = search.trim();
		navigate(q ? `/tickets?q=${encodeURIComponent(q)}` : "/tickets");
	};

	return (
		<div className="ud-stage">
			<div className="ud-shell">
				<aside className="ud-rail" aria-label="主导航">
					<div className="ud-rail__header">
						<span className="ud-rail__logo" aria-hidden>U</span>
						<span className="ud-rail__wordmark">UnionDesk</span>
					</div>
					<nav className="ud-rail__nav" aria-label="主导航">
						<NavItems variant="rail" unreadCount={portal.unreadCount} />
					</nav>
					<div className="ud-rail__spacer" />
					<div className="ud-rail__foot">
						<div className="ud-rail__foot-row--tools">
							<button
								type="button"
								className="ud-rail__foot-btn"
								onClick={() => navigate("/me")}
								title="设置"
							>
								<IconNavGear />
							</button>
							<span className="ud-rail__foot-divider" aria-hidden />
							<button
								type="button"
								className="ud-rail__foot-btn"
								onClick={() => navigate("/inbox")}
								title="消息"
							>
								<IconNavMessage />
								{portal.unreadCount > 0
									? <i className="ud-rail__foot-dot">{portal.unreadCount > 9 ? "9+" : portal.unreadCount}</i>
									: null}
							</button>
						</div>
						<button
							type="button"
							className="ud-rail__foot-link"
							onClick={() => navigate("/domains")}
							title="切换业务域"
						>
							<IconNavDomain />
							<span>{domainName}</span>
							<em aria-hidden>▼</em>
						</button>
						<button
							type="button"
							className="ud-rail__foot-link"
							onClick={() => navigate("/me")}
							title="我的账户"
						>
							<span className="ud-rail__avatar">{avatarLetter}</span>
							<span>{displayName}</span>
						</button>
					</div>
				</aside>

				<div className="ud-shell__body">
					<header className="ud-topbar">
						<div className="ud-topbar__left">
							<h1 className="ud-topbar__title">{title}</h1>
							<form className="ud-topbar__search" onSubmit={onSearch}>
								<IconSearchSolid />
								<input
									type="search"
									placeholder="搜索工单、通知..."
									value={search}
									onChange={event => setSearch(event.target.value)}
									aria-label="搜索工单、通知"
								/>
							</form>
						</div>
						<div className="ud-topbar__right">
							<button
								type="button"
								className="ud-topbar__domain"
								onClick={() => navigate("/domains")}
								title="切换业务域"
							>
								<strong>{domainName}</strong>
							</button>
							<button
								type="button"
								className="ud-topbar__submit"
								onClick={() => navigate("/tickets/new")}
							>
								提交工单
							</button>
						</div>
					</header>

					<main className="ud-content">
						<Outlet />
					</main>
				</div>
			</div>

			<nav className="ud-dock" aria-label="主导航">
				<NavItems variant="dock" unreadCount={portal.unreadCount} />
			</nav>
		</div>
	);
}
