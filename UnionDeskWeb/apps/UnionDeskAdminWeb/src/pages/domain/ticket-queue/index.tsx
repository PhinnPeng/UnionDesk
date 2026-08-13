import { fetchDomainPriorityLevels, fetchDomainTicketStatuses, toErrorMessage, type DomainPriorityLevelView, type TicketStatusDefinition } from "@uniondesk/shared";

import {
	assignAdminTicket,
	claimAdminTicket,
	fetchAdminDomainTicketsPage,
	updateAdminTicketStatus,
	type AdminTicketListQuery,
	type TicketRow,
} from "#src/api/platform/ticket";
import { AuthGuarded } from "#src/components/auth-guarded";
import { BasicContent } from "#src/components/basic-content";
import { ConfirmPopover } from "#src/components/confirm-popover";
import { TableSearchForm } from "#src/components/table-search-form";
import { MemberPicker } from "#src/pages/platform/components/member-picker";
import { PriorityBadge } from "#src/pages/platform/components/priority-badge";
import { useAuthStore } from "#src/store/auth";

import { ReloadOutlined, SearchOutlined } from "@ant-design/icons";
import { App, Button, Card, Empty, Form, Input, Modal, Select, Space, Switch, Table, Tag, Typography } from "antd";
import type { TableColumnsType } from "antd";
import dayjs from "dayjs";
import { useCallback, useEffect, useMemo, useState } from "react";
import { Link } from "react-router";

interface QueueSearchValues {
	keyword?: string;
	status?: string;
	priority?: string;
	assigned_to_me?: boolean;
}

const EMPTY_QUEUE_SEARCH: QueueSearchValues = {
	keyword: "",
	status: "",
	priority: "",
	assigned_to_me: false,
};

function resolveBusinessDomainId(
	defaultBusinessDomainId: number,
	accessibleDomains: Array<{ id: number }>,
): number {
	if (defaultBusinessDomainId > 0) {
		return defaultBusinessDomainId;
	}
	const first = accessibleDomains[0];
	return first ? Number(first.id) : 0;
}

function statusOptionMap(statuses: TicketStatusDefinition[]) {
	const map: Record<string, TicketStatusDefinition> = {};
	for (const item of statuses) {
		if (item.status === "active") {
			map[item.code] = item;
		}
	}
	return map;
}

function priorityOptionMap(levels: DomainPriorityLevelView[]) {
	const map: Record<string, DomainPriorityLevelView> = {};
	for (const item of levels) {
		map[item.code] = item;
	}
	return map;
}

function formatTime(value?: string | null) {
	return value ? dayjs(value).format("YYYY-MM-DD HH:mm") : "-";
}

export default function DomainTicketQueuePage() {
	const { message } = App.useApp();
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
	const [searchValues, setSearchValues] = useState<QueueSearchValues>(EMPTY_QUEUE_SEARCH);
	const [statuses, setStatuses] = useState<TicketStatusDefinition[]>([]);
	const [priorityLevels, setPriorityLevels] = useState<DomainPriorityLevelView[]>([]);
	const [assignTarget, setAssignTarget] = useState<TicketRow | null>(null);
	const [assignOpen, setAssignOpen] = useState(false);
	const [assignSubmitting, setAssignSubmitting] = useState(false);
	const [assignForm] = Form.useForm<{ assigneeStaffAccountId: number }>();

	const statusMap = useMemo(() => statusOptionMap(statuses), [statuses]);
	const priorityMap = useMemo(() => priorityOptionMap(priorityLevels), [priorityLevels]);

	const statusOptions = useMemo(
		() => statuses
			.filter(item => item.status === "active")
			.map(item => ({ value: item.code, label: item.name })),
		[statuses],
	);

	const priorityOptions = useMemo(
		() => priorityLevels.map(item => ({
			value: item.code,
			label: item.display_label ?? item.name,
		})),
		[priorityLevels],
	);

	const loadMeta = useCallback(async () => {
		if (!domainId) {
			return;
		}
		try {
			const [statusResult, priorityResult] = await Promise.all([
				fetchDomainTicketStatuses(String(domainId), { page: 1, page_size: 200 }),
				fetchDomainPriorityLevels(String(domainId)),
			]);
			setStatuses(statusResult.items ?? []);
			setPriorityLevels(priorityResult.items ?? []);
		}
		catch {
			// 下拉选项加载失败不阻塞列表，选项留空兜底
		}
	}, [domainId]);

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
				status: nextSearch.status || undefined,
				priority: nextSearch.priority || undefined,
				assigned_to_me: nextSearch.assigned_to_me || undefined,
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
	}, [domainId, message, page, pageSize, searchValues]);

	useEffect(() => {
		void loadTickets(1, 20, EMPTY_QUEUE_SEARCH);
		void loadMeta();
	// eslint-disable-next-line react-hooks/exhaustive-deps -- domainId 变化时初始化
	}, [domainId]);

	const handleSearch = useCallback((values: QueueSearchValues) => {
		void loadTickets(1, pageSize, values);
	}, [loadTickets, pageSize]);

	const handleResetSearch = useCallback(() => {
		void loadTickets(1, pageSize, EMPTY_QUEUE_SEARCH);
	}, [loadTickets, pageSize]);

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

	const openAssign = useCallback((row: TicketRow) => {
		setAssignTarget(row);
		assignForm.resetFields();
		setAssignOpen(true);
	}, [assignForm]);

	const handleAssign = useCallback(async () => {
		if (!domainId || !assignTarget) {
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
				assigneeStaffAccountId: values.assigneeStaffAccountId,
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
	}, [assignForm, assignTarget, domainId, loadTickets, message, page, pageSize, searchValues]);

	const handleClose = useCallback(async (row: TicketRow) => {
		try {
			await updateAdminTicketStatus(domainId, row.id, { status: "closed", version: row.version });
			message.success("工单已关闭");
			await loadTickets(page, pageSize, searchValues);
		}
		catch (error) {
			message.error(toErrorMessage(error));
		}
	}, [domainId, loadTickets, message, page, pageSize, searchValues]);

	const renderStatus = useCallback((status: string) => {
		const meta = statusMap[status];
		return <Tag color={meta ? "blue" : "default"}>{meta?.name ?? status}</Tag>;
	}, [statusMap]);

	const renderPriority = useCallback((priority: string) => {
		const meta = priorityMap[priority];
		return <PriorityBadge code={priority} name={meta?.display_label ?? meta?.name} color={meta?.color} icon={meta?.icon} />;
	}, [priorityMap]);

	const columns: TableColumnsType<TicketRow> = useMemo(() => [
		{
			title: "编号",
			dataIndex: "ticketNo",
			width: 170,
			render: (value: string, row) => <Link to={`/domain/ticket-queue/${row.id}`}>{value}</Link>,
		},
		{
			title: "标题",
			dataIndex: "title",
			ellipsis: true,
			render: (value: string, row) => <Link to={`/domain/ticket-queue/${row.id}`}>{value}</Link>,
		},
		{
			title: "类型",
			dataIndex: "ticketTypeName",
			width: 130,
			render: value => value ?? "-",
		},
		{
			title: "状态",
			dataIndex: "status",
			width: 110,
			render: value => renderStatus(value),
		},
		{
			title: "优先级",
			dataIndex: "priority",
			width: 110,
			render: value => renderPriority(value),
		},
		{
			title: "受理人",
			dataIndex: "assignedTo",
			width: 110,
			render: value => (value ? `员工 #${value}` : "-"),
		},
		{
			title: "创建时间",
			dataIndex: "createdAt",
			width: 160,
			render: value => formatTime(value),
		},
		{
			title: "操作",
			key: "actions",
			width: 200,
			fixed: "right",
			render: (_, row) => (
				<Space size="small">
					<AuthGuarded auth="ticket.claim" fallback={null}>
						<Button type="link" size="small" onClick={() => void handleClaim(row)}>
							领取
						</Button>
					</AuthGuarded>
					<AuthGuarded auth="ticket.assign" fallback={null}>
						<Button type="link" size="small" onClick={() => openAssign(row)}>
							指派
						</Button>
					</AuthGuarded>
					<AuthGuarded auth="ticket.close" fallback={null}>
						<ConfirmPopover
							title={`确认关闭工单「${row.ticketNo}」？`}
							onConfirm={() => void handleClose(row)}
						>
							<Button type="link" size="small" danger>关闭</Button>
						</ConfirmPopover>
					</AuthGuarded>
				</Space>
			),
		},
	], [handleClaim, handleClose, openAssign, renderPriority, renderStatus]);

	return (
		<BasicContent>
			<AuthGuarded
				auth="ticket.view.domain_all"
				fallback={<Empty description="无权限查看工单队列" className="py-16" />}
			>
				{!domainId ? (
					<Empty description="暂无可用业务域" className="py-16" />
				) : (
					<div className="flex flex-col gap-4">
						<Card
							bordered={false}
							title={(
								<Space>
									<SearchOutlined />
									<span>筛选条件</span>
								</Space>
							)}
						>
							<TableSearchForm<QueueSearchValues>
								loading={loading}
								initialValues={EMPTY_QUEUE_SEARCH}
								onFinish={handleSearch}
								onReset={handleResetSearch}
							>
								<Form.Item name="keyword" label="关键字">
									<Input allowClear placeholder="工单标题" prefix={<SearchOutlined />} disabled={loading} />
								</Form.Item>
								<Form.Item name="status" label="状态">
									<Select allowClear placeholder="全部状态" disabled={loading} options={statusOptions} />
								</Form.Item>
								<Form.Item name="priority" label="优先级">
									<Select allowClear placeholder="全部优先级" disabled={loading} options={priorityOptions} />
								</Form.Item>
								<Form.Item name="assigned_to_me" label="我的待办" valuePropName="checked">
									<Switch disabled={loading} />
								</Form.Item>
							</TableSearchForm>
						</Card>
						<Card
							bordered={false}
							title="工单列表"
							extra={(
								<Button icon={<ReloadOutlined />} onClick={() => void loadTickets(page, pageSize, searchValues)}>
									刷新
								</Button>
							)}
						>
							<Table<TicketRow>
								rowKey="id"
								loading={loading}
								columns={columns}
								dataSource={rows}
								scroll={{ x: 1100 }}
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
						</Card>
					</div>
				)}
			</AuthGuarded>

			<Modal
				title="指派工单"
				open={assignOpen}
				onCancel={() => setAssignOpen(false)}
				onOk={() => void handleAssign()}
				confirmLoading={assignSubmitting}
				destroyOnClose
			>
				{assignTarget ? (
					<Form form={assignForm} layout="vertical">
						<Typography.Paragraph type="secondary" className="!mb-4">
							工单：
							{assignTarget.ticketNo}
						</Typography.Paragraph>
						<Form.Item name="assigneeStaffAccountId" label="处理人" rules={[{ required: true, message: "请选择处理人" }]}>
							<MemberPicker domainId={domainId} />
						</Form.Item>
					</Form>
				) : null}
			</Modal>
		</BasicContent>
	);
}
