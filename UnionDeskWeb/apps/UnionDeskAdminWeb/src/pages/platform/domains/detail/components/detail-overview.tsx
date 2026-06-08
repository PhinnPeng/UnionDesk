import type { AdminDomain } from "@uniondesk/shared";
import { fetchP0AdminDomainTicketsPage } from "@uniondesk/shared";

import { useAuth } from "#src/hooks/use-auth";

import { Button, Card, Empty, Space, Typography } from "antd";
import { useEffect, useState } from "react";

import { DOMAIN_CUSTOMER_READ_PERMISSION, type DetailTabKey } from "./detail-shared";

import styles from "../index.module.less";

const { Title, Text } = Typography;

export interface DetailOverviewProps {
	domain: AdminDomain;
	onNavigateTab: (tab: DetailTabKey) => void;
}

const KPI_ITEMS = [
	{ key: "pendingTickets", label: "待处理工单数" },
	{ key: "activeMembers", label: "在岗员工" },
	{ key: "customers", label: "域内客户" },
	{ key: "roles", label: "角色数" },
	{ key: "blockedWords", label: "屏蔽词" },
] as const;

export function DetailOverview({ domain, onNavigateTab }: DetailOverviewProps) {
	const { hasPermission } = useAuth();
	const canViewCustomers = hasPermission(DOMAIN_CUSTOMER_READ_PERMISSION);
	const [pendingTicketCount, setPendingTicketCount] = useState<number | null>(null);

	useEffect(() => {
		let ignore = false;
		void (async () => {
			try {
				const result = await fetchP0AdminDomainTicketsPage({
					domainId: domain.id,
					page: 1,
					page_size: 1,
				});
				if (!ignore) {
					setPendingTicketCount(result.total ?? 0);
				}
			}
			catch {
				if (!ignore) {
					setPendingTicketCount(null);
				}
			}
		})();
		return () => {
			ignore = true;
		};
	}, [domain.id]);

	const resolveKpiValue = (key: (typeof KPI_ITEMS)[number]["key"]) => {
		if (key === "pendingTickets") {
			return pendingTicketCount === null ? "—" : String(pendingTicketCount);
		}
		return "—";
	};

	return (
		<div>
			<Title level={5} className="!mb-4">
				概览
			</Title>
			<div className={styles.kpiGrid}>
				{KPI_ITEMS.map(item => (
					<div key={item.key} className={styles.kpiCard}>
						<div className={styles.kpiLabel}>{item.label}</div>
						<div className={styles.kpiValue}>{resolveKpiValue(item.key)}</div>
					</div>
				))}
			</div>

			<Card title="趋势分析" className="mb-6">
				<Empty description="趋势数据接入中" />
			</Card>

			<div>
				<Title level={5} className="!mb-3">
					高频运营向导
				</Title>
				<Space wrap>
					<Button onClick={() => onNavigateTab("tickets")}>工单管理</Button>
					<Button onClick={() => onNavigateTab("onboarding")}>入域管理</Button>
					<Button onClick={() => onNavigateTab("members")}>人员管理</Button>
					{canViewCustomers ? (
						<Button onClick={() => onNavigateTab("customers")}>客户管理</Button>
					) : null}
				</Space>
				<Text type="secondary" className="mt-3 block text-sm">
					快捷入口将跳转到对应配置页签。
				</Text>
			</div>
		</div>
	);
}
