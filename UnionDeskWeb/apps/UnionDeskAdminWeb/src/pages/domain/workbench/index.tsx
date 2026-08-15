import { fetchAdminDomainTicketsPage } from "#src/api/platform/ticket";
import { BasicContent } from "#src/components/basic-content";
import DomainConsultationsPage from "#src/pages/domain/consultations";
import DomainTicketQueuePage from "#src/pages/domain/ticket-queue";
import { listAdminConsultations } from "@uniondesk/shared";
import { useAuthStore } from "#src/store/auth";

import { Badge, Card, Row, Col, Statistic, Tabs, Typography } from "antd";
import { useCallback, useEffect, useState } from "react";
import { useSearchParams } from "react-router";

const { Title, Text } = Typography;

type WorkbenchTabKey = "ticket" | "consultation";

/** 解析 URL ?tab= 参数，非法值回退到「工单队列」 */
function parseWorkbenchTab(raw: string | null): WorkbenchTabKey {
	if (raw === "consultation") {
		return "consultation";
	}
	return "ticket";
}

/** 解析当前业务域：默认域优先，其次可访问域首个 */
function resolveBusinessDomainId(defaultId?: string | null, accessibleDomains?: Array<{ id: string }> | null): string {
	if (defaultId) {
		return defaultId;
	}
	return accessibleDomains?.[0]?.id ?? "";
}

/** 工作台统计：我的待办 / 进行中咨询 / SLA 告警（均为列表 total，page_size=1 取计数） */
interface WorkbenchStats {
	myTickets: number
	openConsultations: number
	slaBreached: number
	loading: boolean
}

/**
 * 「工作台」壳页：顶部统计条 + Tabs 聚合工单队列与在线咨询两个原子页面，
 * 内嵌页面保留各自的数据加载、权限守卫与布局；?tab= 参数同步激活 Tab，
 * 供「咨询转工单 → 前往工单」等跨页跳转直接定位。
 */
export default function DomainWorkbenchPage() {
	const [searchParams, setSearchParams] = useSearchParams();
	const [activeTab, setActiveTab] = useState<WorkbenchTabKey>(() => parseWorkbenchTab(searchParams.get("tab")));
	const [stats, setStats] = useState<WorkbenchStats>({ myTickets: 0, openConsultations: 0, slaBreached: 0, loading: true });

	const defaultBusinessDomainId = useAuthStore(state => state.defaultBusinessDomainId);
	const accessibleDomains = useAuthStore(state => state.accessibleDomains);
	const domainId = resolveBusinessDomainId(defaultBusinessDomainId, accessibleDomains);

	const loadStats = useCallback(async () => {
		if (!domainId) {
			setStats(prev => ({ ...prev, loading: false }));
			return;
		}
		setStats(prev => ({ ...prev, loading: true }));
		try {
			const [tickets, consultations, breached] = await Promise.all([
				fetchAdminDomainTicketsPage(domainId, { page: 1, page_size: 1, assigned_to_me: true }),
				listAdminConsultations(domainId, { page: 1, pageSize: 1, status: "open" }),
				fetchAdminDomainTicketsPage(domainId, { page: 1, page_size: 1, assigned_to_me: true, sla_status: "breached" }),
			]);
			setStats({
				myTickets: tickets.total,
				openConsultations: consultations.total,
				slaBreached: breached.total,
				loading: false,
			});
		}
		catch {
			// 统计加载失败不阻塞工作台主体
			setStats(prev => ({ ...prev, loading: false }));
		}
	}, [domainId]);

	useEffect(() => {
		loadStats();
	}, [loadStats]);

	useEffect(() => {
		setActiveTab(parseWorkbenchTab(searchParams.get("tab")));
	}, [searchParams]);

	const handleTabChange = useCallback((nextTab: string) => {
		const tab = parseWorkbenchTab(nextTab);
		setActiveTab(tab);
		setSearchParams(prev => {
			const next = new URLSearchParams(prev);
			next.set("tab", tab);
			return next;
		}, { replace: true });
	}, [setSearchParams]);

	const ticketTabLabel = (
		<Badge count={stats.myTickets || undefined} size="small" offset={[6, 0]}>工单队列</Badge>
	);
	const consultationTabLabel = (
		<Badge count={stats.openConsultations || undefined} size="small" offset={[6, 0]}>在线咨询</Badge>
	);

	return (
		<BasicContent>
			<div className="flex flex-col gap-4">
				<div>
					<Title level={5} className="!mb-1">工作台</Title>
					<Text type="secondary">聚合「工单队列」与「在线咨询」，集中处理业务事项</Text>
				</div>

				<Row gutter={[16, 16]}>
					<Col xs={24} md={8}>
						<Card bordered={false} loading={stats.loading}>
							<Statistic title="我的待办工单" value={stats.myTickets} />
						</Card>
					</Col>
					<Col xs={24} md={8}>
						<Card bordered={false} loading={stats.loading}>
							<Statistic title="进行中咨询" value={stats.openConsultations} />
						</Card>
					</Col>
					<Col xs={24} md={8}>
						<Card bordered={false} loading={stats.loading}>
							<Statistic title="SLA 告警" value={stats.slaBreached} valueStyle={{ color: stats.slaBreached > 0 ? "#ff4d4f" : undefined }} />
						</Card>
					</Col>
				</Row>

				<Tabs
					type="card"
					activeKey={activeTab}
					onChange={handleTabChange}
					items={[
						{
							key: "ticket",
							label: ticketTabLabel,
							children: <DomainTicketQueuePage />,
						},
						{
							key: "consultation",
							label: consultationTabLabel,
							children: <DomainConsultationsPage />,
						},
					]}
				/>
			</div>
		</BasicContent>
	);
}
