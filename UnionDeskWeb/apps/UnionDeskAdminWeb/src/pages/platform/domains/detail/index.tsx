import type { AdminDomain } from "@uniondesk/shared";
import { fetchAdminDomain, toErrorMessage } from "@uniondesk/shared";

import { AuthGuarded } from "#src/components/auth-guarded";
import { BasicContent } from "#src/components/basic-content";
import { DomainConfigPanel } from "#src/pages/platform/domain-config/config-panel";
import { DomainOnboardingPanel } from "#src/pages/platform/domain-onboarding/onboarding-panel";
import { appScopes } from "#src/router/extra-info/app-scope";
import { useTabsStore } from "#src/store/tabs";

import { GlobalOutlined } from "@ant-design/icons";
import { App, Avatar, Card, Empty, Space, Spin, Tabs, Tag, Typography } from "antd";
import { useEffect, useMemo, useState } from "react";
import { useNavigate, useParams, useSearchParams } from "react-router";

import { DOMAIN_DETAIL_TAB_KEYS, platformDomainDetailPath, type DomainDetailTabKey } from "../domain-routes";
import { isDomainEnabled } from "../domain-utils";
import { DomainBasicInfoTab } from "./basic-info-tab";
import { DomainOverviewTab } from "./overview-tab";

const { Text } = Typography;

function parseDetailTab(value: string | null): DomainDetailTabKey {
	if (value && (DOMAIN_DETAIL_TAB_KEYS as readonly string[]).includes(value)) {
		return value as DomainDetailTabKey;
	}
	return "overview";
}

export default function PlatformDomainDetail() {
	const { message } = App.useApp();
	const navigate = useNavigate();
	const { domainId: domainIdParam } = useParams();
	const [searchParams, setSearchParams] = useSearchParams();
	const domainId = useMemo(() => {
		const fromPath = domainIdParam?.trim() ?? "";
		if (fromPath) {
			return fromPath;
		}
		return searchParams.get("domainId")?.trim() ?? "";
	}, [domainIdParam, searchParams]);

	const activeTab = parseDetailTab(searchParams.get("tab"));
	const detailPath = domainId ? platformDomainDetailPath(domainId) : "";
	const { setTableTitle, resetTableTitle } = useTabsStore();

	const [loading, setLoading] = useState(false);
	const [domain, setDomain] = useState<AdminDomain | null>(null);

	useEffect(() => {
		if (!domainIdParam && searchParams.get("domainId")?.trim()) {
			navigate(platformDomainDetailPath(searchParams.get("domainId")!.trim(), activeTab), { replace: true });
		}
	}, [activeTab, domainIdParam, navigate, searchParams]);

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
		if (!detailPath || !domain?.name) {
			return;
		}
		setTableTitle(appScopes.platform, detailPath, `业务域详情 - ${domain.name}`);
		return () => {
			resetTableTitle(appScopes.platform, detailPath);
		};
	}, [detailPath, domain?.name, resetTableTitle, setTableTitle]);

	const handleTabChange = (key: string) => {
		const next = new URLSearchParams(searchParams);
		next.set("tab", key);
		setSearchParams(next, { replace: true });
	};

	const enabled = domain ? isDomainEnabled(domain) : false;

	return (
		<BasicContent className="h-full overflow-auto bg-colorBgLayout">
			<Card bordered={false}>
				{!domainId ? (
					<Empty description="缺少业务域 ID，请从业务域列表进入" />
				) : loading ? (
					<div className="flex justify-center py-16">
						<Spin />
					</div>
				) : !domain ? (
					<Empty description="未找到业务域" />
				) : (
					<AuthGuarded auth="domain.admin.detail.read">
						<div className="mb-4 flex flex-wrap items-center gap-3">
							<Avatar
								size={56}
								src={domain.logo ?? undefined}
								icon={!domain.logo ? <GlobalOutlined /> : undefined}
								style={{ backgroundColor: domain.logo ? "transparent" : "#1677ff" }}
							>
								{domain.name.charAt(0).toUpperCase()}
							</Avatar>
							<div className="min-w-0 flex-1">
								<Space align="center" wrap>
									<Typography.Title level={4} className="!mb-0">
										{domain.name}
									</Typography.Title>
									<Tag color={enabled ? "success" : "default"}>
										{enabled ? "已启用" : "已禁用"}
									</Tag>
								</Space>
								<Text type="secondary" copyable className="text-sm">
									短码：{domain.code}
								</Text>
							</div>
						</div>

						<Tabs
							activeKey={activeTab}
							onChange={handleTabChange}
							destroyInactiveTabPane={false}
							items={[
								{
									key: "overview",
									label: "概览",
									children: <DomainOverviewTab domain={domain} />,
								},
								{
									key: "basic",
									label: "基础信息",
									children: (
										<DomainBasicInfoTab
											domain={domain}
											onSaved={setDomain}
										/>
									),
								},
								{
									key: "config",
									label: "域配置",
									children: <DomainConfigPanel domainId={domain.id} />,
								},
								{
									key: "onboarding",
									label: "客户入域",
									children: <DomainOnboardingPanel domainId={domain.id} />,
								},
							]}
						/>
					</AuthGuarded>
				)}
			</Card>
		</BasicContent>
	);
}
