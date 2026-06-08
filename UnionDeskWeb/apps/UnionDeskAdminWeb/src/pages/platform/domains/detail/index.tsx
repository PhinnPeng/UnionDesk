import type { AdminDomain } from "@uniondesk/shared";
import { fetchAdminDomain, toErrorMessage } from "@uniondesk/shared";

import { AuthGuarded } from "#src/components/auth-guarded";
import { BasicContent } from "#src/components/basic-content";
import { useAuth } from "#src/hooks/use-auth";
import { appScopes } from "#src/router/extra-info/app-scope";
import { useTabsStore } from "#src/store/tabs";

import { App, Empty, Spin } from "antd";
import { useEffect, useMemo, useState } from "react";
import { useNavigate, useParams, useSearchParams } from "react-router";

import { DetailBaseinfo } from "./components/detail-baseinfo";
import { DetailBlockwords } from "./components/detail-blockwords";
import { DetailConfig } from "./components/detail-config";
import { DetailCustomers } from "./components/detail-customers";
import { DetailHeader } from "./components/detail-header";
import { DetailLogs } from "./components/detail-logs";
import { DetailMembers } from "./components/detail-members";
import { DetailNotifications } from "./components/detail-notifications";
import { DetailOnboarding } from "./components/detail-onboarding";
import { DetailOverview } from "./components/detail-overview";
import { DetailRoles } from "./components/detail-roles";
import { DetailSider } from "./components/detail-sider";
import { DetailTickets } from "./components/detail-tickets";
import {
	DOMAIN_CONTROL_ENTRY_PERMISSION,
	DOMAIN_CONTROL_OVERVIEW_PERMISSION,
	DOMAIN_CUSTOMER_READ_PERMISSION,
	DOMAIN_ROLES_READ_PERMISSION,
	type DetailTabKey,
	parseDetailTab,
} from "./components/detail-shared";

import styles from "./index.module.less";

function resolveEffectiveTab(
	activeTab: DetailTabKey,
	canViewOverview: boolean,
	canViewCustomers: boolean,
	canViewRoles: boolean,
): DetailTabKey {
	if (activeTab === "overview" && !canViewOverview) {
		return "basic";
	}
	if (activeTab === "customers" && !canViewCustomers) {
		return canViewOverview ? "overview" : "basic";
	}
	if (activeTab === "roles" && !canViewRoles) {
		return canViewOverview ? "overview" : "basic";
	}
	return activeTab;
}

function renderActiveTab(
	tab: DetailTabKey,
	domain: AdminDomain,
	onSaved: (domain: AdminDomain) => void,
	onNavigateTab: (tab: DetailTabKey) => void,
	onDeleted: () => void,
) {
	switch (tab) {
		case "overview":
			return <DetailOverview domain={domain} onNavigateTab={onNavigateTab} />;
		case "basic":
			return <DetailBaseinfo domain={domain} onSaved={onSaved} onDeleted={onDeleted} />;
		case "members":
			return <DetailMembers domainId={domain.id} />;
		case "roles":
			return <DetailRoles domainId={domain.id} />;
		case "customers":
			return <DetailCustomers domainId={domain.id} />;
		case "onboarding":
			return <DetailOnboarding domain={domain} onSaved={onSaved} />;
		case "tickets":
			return <DetailTickets domainId={domain.id} />;
		case "blockwords":
			return <DetailBlockwords domainId={domain.id} />;
		case "notifications":
			return <DetailNotifications />;
		case "config":
			return <DetailConfig domainId={domain.id} />;
		case "logs":
			return <DetailLogs domainId={domain.id} />;
		default:
			return <DetailOverview domain={domain} onNavigateTab={onNavigateTab} />;
	}
}

export default function PlatformDomainDetail() {
	const { message } = App.useApp();
	const { hasPermission } = useAuth();
	const canViewCustomers = hasPermission(DOMAIN_CUSTOMER_READ_PERMISSION);
	const canViewOverview = hasPermission(DOMAIN_CONTROL_OVERVIEW_PERMISSION);
	const canViewRoles = hasPermission(DOMAIN_ROLES_READ_PERMISSION);
	const navigate = useNavigate();
	const { domainId: domainIdParam } = useParams();
	const [searchParams, setSearchParams] = useSearchParams();
	const { setTableTitle, resetTableTitle } = useTabsStore();

	const [loading, setLoading] = useState(false);
	const [domain, setDomain] = useState<AdminDomain | null>(null);

	const domainId = useMemo(() => {
		const fromPath = domainIdParam?.trim() ?? "";
		if (fromPath) {
			return fromPath;
		}
		return searchParams.get("domainId")?.trim() ?? "";
	}, [domainIdParam, searchParams]);

	useEffect(() => {
		const legacyId = searchParams.get("domainId")?.trim();
		if (!domainIdParam && legacyId) {
			const activeTab = parseDetailTab(searchParams.get("tab"));
			const base = `/platform/domains/detail/${encodeURIComponent(legacyId)}`;
			const path = activeTab === "overview" ? base : `${base}?tab=${encodeURIComponent(activeTab)}`;
			navigate(path, { replace: true });
		}
	}, [domainIdParam, navigate, searchParams]);

	useEffect(() => {
		if (!domainId) {
			setDomain(null);
			return;
		}

		let ignore = false;
		void (async () => {
			setLoading(true);
			try {
				const data = await fetchAdminDomain(domainId);
				if (!ignore) {
					setDomain(data);
				}
			}
			catch (error) {
				if (!ignore) {
					message.error(toErrorMessage(error));
					setDomain(null);
				}
			}
			finally {
				if (!ignore) {
					setLoading(false);
				}
			}
		})();

		return () => {
			ignore = true;
		};
	}, [domainId, message]);

	useEffect(() => {
		const detailPath = domainId
			? `/platform/domains/detail/${encodeURIComponent(domainId)}`
			: "";
		if (!detailPath || !domain?.name) {
			return;
		}
		setTableTitle(appScopes.platform, detailPath, `业务域控制台 - ${domain.name}`);
		return () => {
			resetTableTitle(appScopes.platform, detailPath);
		};
	}, [domain?.name, domainId, resetTableTitle, setTableTitle]);

	const activeTab = parseDetailTab(searchParams.get("tab"));
	const effectiveTab = resolveEffectiveTab(activeTab, canViewOverview, canViewCustomers, canViewRoles);

	useEffect(() => {
		if (effectiveTab === activeTab) {
			return;
		}
		setSearchParams((prev) => {
			const next = new URLSearchParams(prev);
			if (effectiveTab === "overview") {
				next.delete("tab");
			}
			else {
				next.set("tab", effectiveTab);
			}
			return next;
		}, { replace: true });
	}, [activeTab, effectiveTab, setSearchParams]);

	const handleTabSelect = (tab: DetailTabKey) => {
		if (tab === "customers" && !canViewCustomers) {
			return;
		}
		if (tab === "overview" && !canViewOverview) {
			return;
		}
		if (tab === "roles" && !canViewRoles) {
			return;
		}
		const next = new URLSearchParams(searchParams);
		if (tab === "overview") {
			next.delete("tab");
		}
		else {
			next.set("tab", tab);
		}
		setSearchParams(next, { replace: true });
	};

	return (
		<BasicContent className="h-full overflow-auto bg-colorBgLayout">
			{!domainId ? (
				<Empty description="缺少业务域 ID，请从业务域列表进入" />
			) : loading ? (
				<div className="flex justify-center py-16">
					<Spin />
				</div>
			) : !domain ? (
				<Empty description="未找到业务域" />
			) : (
				<AuthGuarded auth={DOMAIN_CONTROL_ENTRY_PERMISSION}>
					<div className={`${styles.detailShell} flex flex-col`}>
						<DetailHeader domain={domain} />
						<div className={styles.detailBody}>
							<DetailSider activeTab={effectiveTab} onSelect={handleTabSelect} />
							<div className={styles.contentCard}>
								<div className={styles.contentScroll}>
									{renderActiveTab(
										effectiveTab,
										domain,
										setDomain,
										handleTabSelect,
										() => navigate("/platform/domains"),
									)}
								</div>
							</div>
						</div>
					</div>
				</AuthGuarded>
			)}
		</BasicContent>
	);
}
