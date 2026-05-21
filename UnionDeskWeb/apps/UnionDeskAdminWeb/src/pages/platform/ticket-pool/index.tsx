import type { AdminDomain, P0AdminTicketListItem } from "@uniondesk/shared";

import { BasicContent } from "#src/components/basic-content";

import { claimP0AdminTicket, fetchP0AdminDomainTicketsPage, fetchAdminDomainsPage, toErrorMessage } from "@uniondesk/shared";
import { App, Alert, Button, Card, Select, Space, Table, Tag } from "antd";
import type { TableColumnsType } from "antd";
import { useCallback, useEffect, useState } from "react";

export default function PlatformTicketPool() {
	const { message } = App.useApp();
	const [domains, setDomains] = useState<AdminDomain[]>([]);
	const [domainId, setDomainId] = useState<string | undefined>();
	const [rows, setRows] = useState<P0AdminTicketListItem[]>([]);
	const [loading, setLoading] = useState(false);

	const loadDomains = useCallback(async () => {
		try {
			const page = await fetchAdminDomainsPage({ page: 1, page_size: 100 });
			setDomains(page.list);
			setDomainId(prev => prev ?? page.list[0]?.id);
		} catch (e) {
			message.error(toErrorMessage(e));
		}
	}, [message]);

	const loadTickets = useCallback(async () => {
		if (!domainId) {
			setRows([]);
			return;
		}
		setLoading(true);
		try {
			const page = await fetchP0AdminDomainTicketsPage({
				domainId,
				page: 1,
				page_size: 100,
			});
			setRows(page.list);
		} catch (e) {
			message.error(toErrorMessage(e));
		} finally {
			setLoading(false);
		}
	}, [domainId, message]);

	useEffect(() => {
		void loadDomains();
	}, [loadDomains]);

	useEffect(() => {
		void loadTickets();
	}, [loadTickets]);

	const claim = async (ticketId: string) => {
		if (!domainId) {
			return;
		}
		try {
			await claimP0AdminTicket(domainId, ticketId);
			message.success("已尝试领取工单");
			await loadTickets();
		} catch (e) {
			message.error(toErrorMessage(e));
		}
	};

	const columns: TableColumnsType<P0AdminTicketListItem> = [
		{ title: "编号", dataIndex: "ticket_no", width: 160 },
		{ title: "标题", dataIndex: "title" },
		{ title: "类型", dataIndex: "type_name", width: 120, render: v => v ?? "-" },
		{
			title: "状态",
			dataIndex: "status",
			width: 120,
			render: s => <Tag>{s}</Tag>,
		},
		{ title: "优先级", dataIndex: "priority", width: 100, render: v => v ?? "-" },
		{ title: "受理人", dataIndex: "assignee_name", width: 120, render: v => v ?? "-" },
		{ title: "创建时间", dataIndex: "created_at", width: 180 },
		{
			title: "操作",
			key: "op",
			width: 100,
			render: (_, row) => (
				<Button type="link" size="small" onClick={() => void claim(row.id)}>领取</Button>
			),
		},
	];

	return (
		<BasicContent className="h-full bg-colorBgLayout">
			<Card title="工单池（P0）">
				<Alert
					type="info"
					showIcon
					className="mb-4"
					message="路径对齐"
					description="列表调用 `GET /api/v1/admin/domains/{domain_id}/tickets`；后端未发布时回退演示 `/tickets`。领取调用 `POST .../claim`。"
				/>
				<Space className="mb-4" wrap>
					<span>当前业务域</span>
					<Select
						className="min-w-48"
						placeholder="选择业务域"
						value={domainId}
						options={domains.map(d => ({ value: d.id, label: `${d.name} (${d.code})` }))}
						onChange={v => setDomainId(v)}
					/>
					<Button onClick={() => void loadTickets()}>刷新</Button>
				</Space>
				<Table<P0AdminTicketListItem>
					rowKey="id"
					loading={loading}
					columns={columns}
					dataSource={rows}
					pagination={false}
				/>
			</Card>
		</BasicContent>
	);
}
