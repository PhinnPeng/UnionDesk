import { fetchAdminDomainTicketsPage } from "#src/api/platform/ticket";
import { BasicContent } from "#src/components/basic-content";
import DomainConsultationsPage from "#src/pages/domain/consultations";
import DomainTicketQueuePage from "#src/pages/domain/ticket-queue";
import { useAuthStore } from "#src/store/auth";
import { listAdminConsultations } from "@uniondesk/shared";

import { AlertOutlined, FileTextOutlined, MessageOutlined } from "@ant-design/icons";
import { Card, Col, Row, Skeleton, Tabs } from "antd";
import { useCallback, useEffect, useState } from "react";
import type { ReactNode } from "react";
import { useSearchParams } from "react-router";

import styles from "./index.module.less";

type WorkbenchTabKey = "ticket" | "consultation" | "my-todo";

/** 统计卡语义配色（对齐 Figma「CustomerWeb v3 · 工作台」：待办蓝 / 咨询橙 / SLA 红） */
const STAT_CARD_VISUALS = {
	ticket: { color: "#1778ff", tint: "rgba(23, 120, 255, 0.08)" },
	consultation: { color: "#fa731f", tint: "rgba(250, 115, 31, 0.08)" },
	sla: { color: "#ff3333", tint: "rgba(255, 51, 51, 0.08)" },
} as const;

/** 统计卡数值空态灰显（SLA 无告警时） */
const STAT_VALUE_MUTED = "#9ea6b2";

/** 统计卡空态图标块底色（中性浅填充） */
const STAT_TINT_MUTED = "rgba(16, 24, 40, 0.05)";

/** 解析 URL ?tab= 参数，非法值回退到「工单队列」 */
function parseWorkbenchTab(raw: string | null): WorkbenchTabKey {
	if (raw === "consultation" || raw === "my-todo") {
		return raw;
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

/** 工作台统计：我的待办 / 进行中咨询（含排队）/ SLA 告警（均为列表 total，page_size=1 取计数） */
interface WorkbenchStats {
	myTickets: number
	openConsultations: number
	queuedConsultations: number
	slaBreached: number
	loading: boolean
}

interface WorkbenchStatCardProps {
	label: string
	value: number
	color: string
	tint: string
	icon: ReactNode
	hint?: string
	loading: boolean
}

/**
 * 统计卡：语义图标 + 标题 / 数值 / 说明三段式。
 * 材质与排版遵循设计精修原则：antd token 容器（亮暗自适应）、
 * 单一语义 accent、tabular-nums 等宽数字、次级文字满足 WCAG AA。
 */
function WorkbenchStatCard({ label, value, color, tint, icon, hint, loading }: WorkbenchStatCardProps) {
	return (
		<div className="flex h-full min-h-[96px] items-center gap-4 rounded-lg border border-colorBorderSecondary bg-colorBgContainer p-4 shadow-[0_1px_2px_rgba(16,24,40,0.04),0_2px_8px_rgba(16,24,40,0.04)]">
			{loading ? (
				<Skeleton active avatar={{ shape: "square", size: 40 }} title={false} paragraph={{ rows: 2, width: ["55%", "75%"] }} />
			) : (
				<>
					<span
						className="flex size-10 shrink-0 items-center justify-center rounded-lg text-[20px]"
						style={{ backgroundColor: tint, color }}
					>
						{icon}
					</span>
					<div className="flex min-w-0 flex-col gap-1.5">
						<span className="text-[13px] leading-none text-colorTextSecondary">{label}</span>
						<span className="text-[28px] font-bold leading-none tabular-nums" style={{ color }}>{value}</span>
						{hint ? <span className="truncate text-[12px] leading-none text-colorTextTertiary">{hint}</span> : null}
					</div>
				</>
			)}
		</div>
	);
}

/**
 * 「工作台」壳页：顶部统计条 + Tabs 聚合工单队列与在线咨询两个原子页面，
 * 内嵌页面保留各自的数据加载、权限守卫与布局；?tab= 参数同步激活 Tab，
 * 供「咨询转工单 → 前往工单」等跨页跳转直接定位。
 * 结构对齐 Figma「CustomerWeb v3 · 工作台」（308:2），视觉按
 * design-taste-frontend 精修：统计卡语义图标 + 轻阴影材质 + token 化亮暗适配。
 */
export default function DomainWorkbenchPage() {
	const [searchParams, setSearchParams] = useSearchParams();
	const [activeTab, setActiveTab] = useState<WorkbenchTabKey>(() => parseWorkbenchTab(searchParams.get("tab")));
	const [stats, setStats] = useState<WorkbenchStats>({
		myTickets: 0,
		openConsultations: 0,
		queuedConsultations: 0,
		slaBreached: 0,
		loading: true,
	});

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
			const [tickets, consultations, queued, breached] = await Promise.all([
				fetchAdminDomainTicketsPage(domainId, { page: 1, page_size: 1, assigned_to_me: true }),
				listAdminConsultations(domainId, { page: 1, pageSize: 1, status: "open" }),
				listAdminConsultations(domainId, { page: 1, pageSize: 1, status: "queued" }),
				fetchAdminDomainTicketsPage(domainId, { page: 1, page_size: 1, assigned_to_me: true, sla_status: "breached" }),
			]);
			setStats({
				myTickets: tickets.total,
				openConsultations: consultations.total,
				queuedConsultations: queued.total,
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

	const ticketTabLabel = <span>工单队列</span>;
	const consultationTabLabel = <span>咨询会话</span>;
	const myTodoTabLabel = <span>我的待办</span>;

	return (
		<BasicContent className="h-full">
			<div className="flex h-full min-h-0 flex-col gap-4">
				<Row gutter={[16, 16]} className="shrink-0">
					<Col xs={24} md={8}>
						<WorkbenchStatCard
							label="我的待办"
							value={stats.myTickets}
							color={STAT_CARD_VISUALS.ticket.color}
							tint={STAT_CARD_VISUALS.ticket.tint}
							icon={<FileTextOutlined />}
							loading={stats.loading}
						/>
					</Col>
					<Col xs={24} md={8}>
						<WorkbenchStatCard
							label="进行中咨询"
							value={stats.openConsultations}
							color={STAT_CARD_VISUALS.consultation.color}
							tint={STAT_CARD_VISUALS.consultation.tint}
							icon={<MessageOutlined />}
							hint={`排队 ${stats.queuedConsultations} 位客户`}
							loading={stats.loading}
						/>
					</Col>
					<Col xs={24} md={8}>
						<WorkbenchStatCard
							label="SLA 告警"
							value={stats.slaBreached}
							color={stats.slaBreached > 0 ? STAT_CARD_VISUALS.sla.color : STAT_VALUE_MUTED}
							tint={stats.slaBreached > 0 ? STAT_CARD_VISUALS.sla.tint : STAT_TINT_MUTED}
							icon={<AlertOutlined />}
							hint={stats.slaBreached > 0 ? `已超时 ${stats.slaBreached} 条` : "暂无超时告警"}
							loading={stats.loading}
						/>
					</Col>
				</Row>

				<Card
					bordered
					className="flex min-h-0 flex-1 flex-col"
					styles={{ body: { flex: 1, minHeight: 0 } }}
				>
					<Tabs
						activeKey={activeTab}
						onChange={handleTabChange}
						className={`h-full ${styles.tabs}`}
						items={[
							{
								key: "ticket",
								label: ticketTabLabel,
								children: <DomainTicketQueuePage embedded />,
							},
							{
								key: "consultation",
								label: consultationTabLabel,
								children: <DomainConsultationsPage embedded />,
							},
							{
								key: "my-todo",
								label: myTodoTabLabel,
								children: <DomainTicketQueuePage embedded defaultAssignedToMe />,
							},
						]}
					/>
				</Card>
			</div>
		</BasicContent>
	);
}
