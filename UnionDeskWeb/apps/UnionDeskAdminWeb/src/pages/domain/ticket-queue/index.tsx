import { fetchDomainTicketTypes, toErrorMessage, type DomainTicketType } from "@uniondesk/shared";

import {
	assignAdminTicket,
	claimAdminTicket,
	fetchAdminDomainTicketsPage,
	type AdminTicketListQuery,
	type TicketRow,
} from "#src/api/platform/ticket";
import { AuthGuarded } from "#src/components/auth-guarded";
import { BasicContent } from "#src/components/basic-content";
import { useAuth } from "#src/hooks/use-auth";
import { resolveMenuIcon } from "#src/icons/resolve-menu-icon";
import { MemberPicker } from "#src/pages/platform/components/member-picker";
import { useAuthStore } from "#src/store/auth";

import { ReloadOutlined } from "@ant-design/icons";
import { App, Button, Card, Empty, Form, Input, Modal, Radio, Space, Table, Tag, Typography } from "antd";
import type { TableColumnsType } from "antd";
import dayjs from "dayjs";
import { useCallback, useEffect, useLayoutEffect, useMemo, useRef, useState } from "react";

import { TicketDetailDrawer } from "./ticket-detail-drawer";
import { AssigneeCell } from "./assignee-cell";
import { slaStatusMeta } from "./sla-display";
import styles from "./type-filter.module.less";

interface QueueSearchValues {
	keyword?: string;
	assigned_to_me?: boolean;
}

const EMPTY_QUEUE_SEARCH: QueueSearchValues = {
	keyword: "",
	assigned_to_me: false,
};

function resolveBusinessDomainId(
	defaultBusinessDomainId: string,
	accessibleDomains: Array<{ id: string }>,
): string {
	if (defaultBusinessDomainId) {
		return defaultBusinessDomainId;
	}
	const first = accessibleDomains[0];
	return first ? first.id : "";
}

function formatTime(value?: string | null) {
	return value ? dayjs(value).format("YYYY-MM-DD HH:mm") : "-";
}

export default function DomainTicketQueuePage({
	embedded = false,
	defaultAssignedToMe = false,
}: { embedded?: boolean; defaultAssignedToMe?: boolean }) {
	const { message } = App.useApp();
	const { hasPermission } = useAuth();
	const defaultBusinessDomainId = useAuthStore(state => state.defaultBusinessDomainId);
	const accessibleDomains = useAuthStore(state => state.accessibleDomains);

	const domainId = useMemo(
		() => resolveBusinessDomainId(defaultBusinessDomainId, accessibleDomains ?? []),
		[accessibleDomains, defaultBusinessDomainId],
	);

	const [loading, setLoading] = useState(false);
	const [rows, setRows] = useState<TicketRow[]>([]);
	const [total, setTotal] = useState(0);
	const [page, setPage] = useState(1);
	const [pageSize, setPageSize] = useState(20);
	const [searchValues, setSearchValues] = useState<QueueSearchValues>(
		() => (defaultAssignedToMe ? { ...EMPTY_QUEUE_SEARCH, assigned_to_me: true } : EMPTY_QUEUE_SEARCH),
	);
	const [ticketTypes, setTicketTypes] = useState<DomainTicketType[]>([]);
	const [selectedTypeId, setSelectedTypeId] = useState<string | null>(null);
	const [assignTarget, setAssignTarget] = useState<TicketRow | null>(null);
	const [assignOpen, setAssignOpen] = useState(false);
	const [assignSubmitting, setAssignSubmitting] = useState(false);
	/** 分配方式：分配给自己（claim）/ 分配给他人（指派） */
	const [assignMode, setAssignMode] = useState<"self" | "other">("self");
	const [assignForm] = Form.useForm<{ assigneeStaffAccountId: number }>();
	const [detailTicketId, setDetailTicketId] = useState<string | null>(null);
	const [drawerOpen, setDrawerOpen] = useState(false);

	/** 工单表格滚动区容器：量测高度后传给 Table scroll.y（表格内部滚动，不撑高页面） */
	const tableBodyRef = useRef<HTMLDivElement>(null);
	const [tableScrollY, setTableScrollY] = useState<number | undefined>(undefined);

	const loadMeta = useCallback(async () => {
		if (!domainId) {
			return;
		}
		try {
			const typesResult = await fetchDomainTicketTypes(String(domainId));
			setTicketTypes((typesResult ?? []).filter(item => item.status === "active"));
		}
		catch {
			// 类型加载失败不阻塞列表，类型筛选留空兜底
		}
	}, [domainId]);

	/** 表格高度量测：表头 + 分页高度随渲染自校准，容器变化（窗口/Tab 切换）自动跟随 */
	useLayoutEffect(() => {
		const el = tableBodyRef.current;
		if (!el) {
			return;
		}
		const update = () => {
			const header = el.querySelector<HTMLElement>(".ant-table-header")?.offsetHeight ?? 55;
			const pagination = el.querySelector<HTMLElement>(".ant-table-pagination")?.offsetHeight ?? 48;
			setTableScrollY(Math.max(120, el.clientHeight - header - pagination - 8));
		};
		update();
		const observer = new ResizeObserver(update);
		observer.observe(el);
		return () => observer.disconnect();
	}, []);

	const loadTickets = useCallback(async (
		nextPage = page,
		nextPageSize = pageSize,
		nextSearch = searchValues,
	) => {
		if (!domainId) {
			setRows([]);
			setTotal(0);
			return;
		}
		setLoading(true);
		try {
			const params: AdminTicketListQuery = {
				page: nextPage,
				page_size: nextPageSize,
				keyword: nextSearch.keyword?.trim() || undefined,
				assigned_to_me: nextSearch.assigned_to_me || undefined,
				ticket_type_id: selectedTypeId || undefined,
			};
			const result = await fetchAdminDomainTicketsPage(domainId, params);
			setRows(result.list);
			setTotal(result.total);
			setPage(nextPage);
			setPageSize(nextPageSize);
			setSearchValues(nextSearch);
		}
		catch (error) {
			message.error(toErrorMessage(error));
		}
		finally {
			setLoading(false);
		}
	}, [domainId, message, page, pageSize, searchValues, selectedTypeId]);

	useEffect(() => {
		void loadTickets(1, 20, defaultAssignedToMe ? { ...EMPTY_QUEUE_SEARCH, assigned_to_me: true } : EMPTY_QUEUE_SEARCH);
		void loadMeta();
	// eslint-disable-next-line react-hooks/exhaustive-deps -- domainId 变化时初始化
	}, [domainId]);

	const handleKeywordSearch = useCallback((keyword: string) => {
		void loadTickets(1, pageSize, { ...searchValues, keyword: keyword.trim() || undefined });
	}, [loadTickets, pageSize, searchValues]);

	const handleTypeSelect = useCallback((typeId: string | null) => {
		setSelectedTypeId(typeId);
		void loadTickets(1, pageSize, searchValues);
	}, [loadTickets, pageSize, searchValues]);

	const handleClaim = useCallback(async (row: TicketRow) => {
		try {
			await claimAdminTicket(domainId, row.id, { version: row.version });
			message.success("工单已领取");
			await loadTickets(page, pageSize, searchValues);
		}
		catch (error) {
			message.error(toErrorMessage(error));
		}
	}, [domainId, loadTickets, message, page, pageSize, searchValues]);

	const openAssign = useCallback((row: TicketRow, mode: "self" | "other" = "self") => {
		setAssignTarget(row);
		setAssignMode(mode);
		assignForm.resetFields();
		setAssignOpen(true);
	}, [assignForm]);

	const handleAssign = useCallback(async () => {
		if (!domainId || !assignTarget) {
			return;
		}
		// 分配给自己：走领取链路（乐观锁 + SLA 首响），随后关闭弹窗
		if (assignMode === "self") {
			await handleClaim(assignTarget);
			setAssignOpen(false);
			return;
		}
		const values = await assignForm.validateFields().catch(() => null);
		if (!values) {
			return;
		}
		setAssignSubmitting(true);
		try {
			await assignAdminTicket(domainId, assignTarget.id, {
				version: assignTarget.version,
				assigneeStaffAccountId: String(values.assigneeStaffAccountId),
			});
			message.success("工单已指派");
			setAssignOpen(false);
			await loadTickets(page, pageSize, searchValues);
		}
		catch (error) {
			message.error(toErrorMessage(error));
		}
		finally {
			setAssignSubmitting(false);
		}
	}, [assignForm, assignMode, assignTarget, domainId, handleClaim, loadTickets, message, page, pageSize, searchValues]);

	const openDetail = useCallback((row: TicketRow) => {
		setDetailTicketId(row.id);
		setDrawerOpen(true);
	}, []);

	const columns: TableColumnsType<TicketRow> = useMemo(() => [
		{
			title: "标题",
			dataIndex: "title",
			width: 220,
			align: "center",
			ellipsis: true,
			render: (value: string, row) => <a className="cursor-pointer" onClick={() => openDetail(row)}>{value}</a>,
		},
		{
			title: "客户",
			dataIndex: "customerName",
			width: 160,
			align: "center",
			ellipsis: true,
			render: (_, row) => row.customerName || (row.customerId ? `客户 #${row.customerId}` : "-"),
		},
		{
			title: "处理人",
			dataIndex: "assignedTo",
			width: 160,
			align: "center",
			render: (_, row) => (
				<AssigneeCell
					domainId={domainId}
					row={row}
					editable={hasPermission("ticket.assign")}
					onChanged={() => void loadTickets(page, pageSize, searchValues)}
				/>
			),
		},
		{
			title: "创建时间",
			dataIndex: "createdAt",
			width: 160,
			align: "center",
			render: value => formatTime(value),
		},
		{
			title: "SLA",
			dataIndex: "slaStatus",
			width: 100,
			align: "center",
			render: (_, row) => {
				const meta = slaStatusMeta(row.slaStatus);
				return <Tag color={meta.color} style={{ marginInlineEnd: 0 }}>{meta.text}</Tag>;
			},
		},
		{
			title: "操作",
			key: "actions",
			width: 80,
			align: "center",
			fixed: "right",
			render: (_, row) => {
				// 仅未领取的工单提供「分配」；终态与已领取行无行操作
				if (!row.assignedTo && !["closed", "withdrawn", "merged"].includes(row.status)) {
					return (
						<AuthGuarded auth={["ticket.claim", "ticket.assign"]} fallback={null}>
							<Button type="link" size="small" className="!px-1" onClick={() => openAssign(row)}>
								分配
							</Button>
						</AuthGuarded>
					);
				}
				return null;
			},
		},
	], [domainId, hasPermission, loadTickets, openAssign, openDetail, page, pageSize, searchValues]);

	const content = (
		<>
			<AuthGuarded
				auth="ticket.view.domain_all"
				fallback={<Empty description="无权限查看工单队列" className="py-16" />}
			>
				{!domainId ? (
					<Empty description="暂无可用业务域" className="py-16" />
				) : (
					<div className="flex h-full min-h-0 flex-col">
							<Card
								bordered={false}
								className="flex min-h-0 flex-1 flex-col"
								styles={{ header: { padding: "0 8px" }, body: { flex: 1, minHeight: 0, paddingLeft: 0 } }}
								title="工单列表"
							extra={(
								<Space size={8}>
										<Input.Search
											size="small"
											allowClear
											placeholder="搜索工单编号 / 标题"
											defaultValue={searchValues.keyword}
											onSearch={handleKeywordSearch}
											style={{ width: 300 }}
										/>
									<Button icon={<ReloadOutlined />} onClick={() => void loadTickets(page, pageSize, searchValues)}>
										刷新
									</Button>
								</Space>
							)}
						>
							<div className="flex h-full min-h-0">
								<div className="flex w-[176px] shrink-0 flex-col overflow-y-auto border-r border-colorBorderSecondary pr-3">
									<div className="flex flex-col gap-1">
										<Button
											type="text"
											block
											className={`${styles.item} ${selectedTypeId === null ? styles.active : ""}`}
											onClick={() => handleTypeSelect(null)}
										>
											<span className="truncate">全部类型</span>
										</Button>
										{ticketTypes.map(item => {
											const active = selectedTypeId === item.id;
											return (
												<Button
													type="text"
													block
													key={item.id}
													className={`${styles.item} ${active ? styles.active : ""}`}
													onClick={() => handleTypeSelect(item.id)}
												>
													{item.icon?.trim()
														? <span className="shrink-0">{resolveMenuIcon(item.icon, { fontSize: 14 })}</span>
														: null}
													<span className="truncate">{item.name}</span>
												</Button>
											);
										})}
									</div>
								</div>
								<div className="flex min-h-0 min-w-0 flex-1 flex-col pl-4">
									<div ref={tableBodyRef} className="min-h-0 flex-1">
										<Table<TicketRow>
											rowKey="id"
											loading={loading}
											columns={columns}
											dataSource={rows}
												scroll={{ x: 880, y: tableScrollY }}
												pagination={{
													current: page,
													pageSize,
													total,
													showSizeChanger: true,
													showTotal: t => `共 ${t} 条`,
													onChange: (nextPage, nextPageSize) => {
														void loadTickets(nextPage, nextPageSize, searchValues);
													},
												}}
												locale={{ emptyText: <Empty description="暂无工单" /> }}
											/>
										</div>
									</div>
								</div>
							</Card>
					</div>
				)}
			</AuthGuarded>

			<Modal
				title="分配工单"
				open={assignOpen}
				onCancel={() => setAssignOpen(false)}
				onOk={() => void handleAssign()}
				confirmLoading={assignSubmitting}
				destroyOnClose
			>
				{assignTarget ? (
					<div className="flex flex-col gap-4">
						<Typography.Paragraph type="secondary" className="!mb-0">
							工单：
							{assignTarget.ticketNo}
						</Typography.Paragraph>
						<Radio.Group value={assignMode} onChange={event => setAssignMode(event.target.value as "self" | "other")}>
							<Radio value="self">分配给自己</Radio>
							<Radio value="other">分配给他人</Radio>
						</Radio.Group>
						{assignMode === "other" ? (
							<Form form={assignForm} layout="vertical">
								<Form.Item name="assigneeStaffAccountId" label="处理人" rules={[{ required: true, message: "请选择处理人" }]}>
									<MemberPicker domainId={domainId} />
								</Form.Item>
							</Form>
						) : null}
					</div>
				) : null}
			</Modal>
			<TicketDetailDrawer
				domainId={domainId}
				ticketId={detailTicketId}
				open={drawerOpen}
				onClose={() => setDrawerOpen(false)}
				tickets={rows}
				onAssign={row => openAssign(row, "other")}
				onChanged={() => void loadTickets(page, pageSize, searchValues)}
			/>
		</>
	);
	// 嵌入工作台时不再套 BasicContent（避免双层 p-4 内边距导致左右未对齐）
	return embedded ? content : <BasicContent className="h-full">{content}</BasicContent>;
}
