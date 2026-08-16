import { fetchTicketDetail, type TicketHistoryRow, type TicketRow } from "#src/api/platform/ticket";
import { AuthGuarded } from "#src/components/auth-guarded";
import { resolveMenuIcon } from "#src/icons/resolve-menu-icon";
import dayjs from "dayjs";

import { Button, Descriptions, Drawer, Empty, Space, Spin, Tabs, Tag, Timeline, Typography } from "antd";
import { useCallback, useEffect, useState } from "react";

const { Title, Text, Paragraph } = Typography;

type LogFilterKey = "all" | "log" | "comment";

/** 活动日志动作中文映射 */
const ACTION_LABELS: Record<string, string> = {
	create: "创建工单",
	claim: "领取工单",
	assign: "指派工单",
	reply: "回复",
	status_change: "状态变更",
	merge: "合并工单",
	close: "关闭工单",
};

/** 运行时状态码中文（域配置状态由列表页映射，抽屉内用运行时语义） */
const STATUS_LABELS: Record<string, string> = {
	open: "待处理",
	new: "待处理",
	processing: "处理中",
	resolved: "已解决",
	closed: "已关闭",
	merged: "已合并",
	withdrawn: "已撤回",
};

/** 优先级码中文 */
const PRIORITY_LABELS: Record<string, string> = {
	low: "低",
	normal: "中",
	high: "高",
	urgent: "紧急",
};

function formatTime(value?: string | null) {
	return value ? dayjs(value).format("YYYY-MM-DD HH:mm") : "-";
}

function filterHistory(history: TicketHistoryRow[], filter: LogFilterKey): TicketHistoryRow[] {
	if (filter === "log") {
		return history.filter(item => item.action !== "reply");
	}
	if (filter === "comment") {
		return history.filter(item => item.action === "reply");
	}
	return history;
}

interface TicketDetailDrawerProps {
	domainId: string
	ticketId: string | null
	open: boolean
	onClose: () => void
	onClaim?: (row: TicketRow) => void
	onAssign?: (row: TicketRow) => void
	onCloseTicket?: (row: TicketRow) => void
	/** 领取/指派/关闭等操作成功后触发（父级刷新列表） */
	onChanged?: () => void
}

/**
 * 工单详情侧边栏（900px）：
 * 左侧 = 标题（含操作按钮）+ 描述（展开/收起）+ 活动日志（全部/只看日志/只看评论）；
 * 右侧 = 额外信息（按事项配置顺序垂直排列）。
 */
export function TicketDetailDrawer({
	domainId,
	ticketId,
	open,
	onClose,
	onClaim,
	onAssign,
	onCloseTicket,
	onChanged,
}: TicketDetailDrawerProps) {
	const [loading, setLoading] = useState(false);
	const [ticket, setTicket] = useState<TicketRow | null>(null);
	const [history, setHistory] = useState<TicketHistoryRow[]>([]);
	const [logFilter, setLogFilter] = useState<LogFilterKey>("all");

	const loadDetail = useCallback(async () => {
		if (!ticketId) {
			return;
		}
		setLoading(true);
		try {
			const detail = await fetchTicketDetail(domainId, ticketId);
			setTicket(detail.ticket);
			setHistory(detail.history ?? []);
		}
		catch {
			// 加载失败保持空态
		}
		finally {
			setLoading(false);
		}
	}, [domainId, ticketId]);

	useEffect(() => {
		if (open && ticketId) {
			setLogFilter("all");
			void loadDetail();
		}
		else if (!open) {
			setTicket(null);
			setHistory([]);
		}
	}, [open, ticketId, loadDetail]);

	const handleActionDone = useCallback(() => {
		onChanged?.();
		void loadDetail();
	}, [onChanged, loadDetail]);

	const visibleHistory = filterHistory(history, logFilter);

	return (
		<Drawer
			open={open}
			onClose={onClose}
			width={900}
			title={null}
			styles={{ body: { padding: 0 } }}
		>
			<Spin spinning={loading}>
				{ticket ? (
					<div className="flex h-full">
						{/* 左侧主区 */}
						<div className="flex-1 min-w-0 p-5 flex flex-col gap-4">
							<div>
								<div className="flex items-start gap-2">
									<Title level={4} className="!mb-1">{ticket.title || "-"}</Title>
									{ticket.status && <Tag color="blue">{STATUS_LABELS[ticket.status] ?? ticket.status}</Tag>}
								</div>
								<div className="flex items-center gap-3">
									<Text type="secondary">{ticket.ticketNo}</Text>
									<Space size="small">
										<AuthGuarded auth="ticket.claim" fallback={null}>
											<Button size="small" onClick={() => { onClaim?.(ticket); handleActionDone(); }}>领取</Button>
										</AuthGuarded>
										<AuthGuarded auth="ticket.assign" fallback={null}>
											<Button size="small" onClick={() => { onAssign?.(ticket); }}>指派</Button>
										</AuthGuarded>
										<AuthGuarded auth="ticket.close" fallback={null}>
											<Button size="small" danger onClick={() => { onCloseTicket?.(ticket); handleActionDone(); }}>关闭</Button>
										</AuthGuarded>
									</Space>
								</div>
							</div>

							{/* 描述：超长截断，展开/收起 */}
							<div>
								<Text strong>描述</Text>
								<Paragraph
									className="!mb-0"
									ellipsis={{ rows: 3, expandable: true, symbol: "展开描述" }}
								>
									{ticket.description || "暂无描述"}
								</Paragraph>
							</div>

							{/* 活动日志：全部 / 只看日志 / 只看评论 */}
							<div className="flex-1 min-h-0 flex flex-col">
								<Tabs
									size="small"
									activeKey={logFilter}
									onChange={key => setLogFilter(key as LogFilterKey)}
									items={[
										{ key: "all", label: "全部" },
										{ key: "log", label: "只看日志" },
										{ key: "comment", label: "只看评论" },
									]}
								/>
								<div className="overflow-auto">
									{visibleHistory.length === 0 ? (
										<Empty description="暂无记录" image={Empty.PRESENTED_IMAGE_SIMPLE} />
									) : (
										<Timeline
											items={visibleHistory.map(item => ({
												children: (
													<div>
														<div>
															<Text>{ACTION_LABELS[item.action ?? ""] ?? item.action ?? "-"}</Text>
															{item.fromValue != null && item.toValue != null && (
																<Text type="secondary">
																	{" "}{item.fromValue} → {item.toValue}
																</Text>
															)}
														</div>
														<Text type="secondary" style={{ fontSize: 12 }}>{formatTime(item.createdAt)}</Text>
													</div>
												),
											}))}
										/>
									)}
								</div>
							</div>
						</div>

						{/* 右侧额外信息（按事项配置顺序） */}
						<div className="w-[280px] shrink-0 border-l border-solid border-[#f0f0f0] p-5">
							<Text strong>额外信息</Text>
							<Descriptions
								className="mt-3"
								column={1}
								size="small"
								items={[
									{
										key: "type",
										label: "事项类型",
										children: (
											<Space size={4}>
												{ticket.ticketTypeIcon?.trim()
													? resolveMenuIcon(ticket.ticketTypeIcon, { fontSize: 14 })
													: null}
												<span>{ticket.ticketTypeName || "-"}</span>
											</Space>
										),
									},
									{ key: "status", label: "状态", children: (STATUS_LABELS[ticket.status ?? ""] ?? ticket.status) || "-" },
									{ key: "priority", label: "优先级", children: (PRIORITY_LABELS[ticket.priority ?? ""] ?? ticket.priority) || "-" },
									{
										key: "assignee",
										label: "受理人",
										children: ticket.assigneeName || (ticket.assignedTo ? `员工 #${ticket.assignedTo}` : "-"),
									},
									{
										key: "customer",
										label: "客户",
										children: ticket.customerName || (ticket.customerId ? `客户 #${ticket.customerId}` : "-"),
									},
									{ key: "source", label: "来源", children: ticket.source || "-" },
									{ key: "createdAt", label: "创建时间", children: formatTime(ticket.createdAt) },
									{ key: "updatedAt", label: "更新时间", children: formatTime(ticket.updatedAt) },
								]}
							/>
						</div>
					</div>
				) : (
					!loading && <Empty description="工单不存在" className="py-16" />
				)}
			</Spin>
		</Drawer>
	);
}
