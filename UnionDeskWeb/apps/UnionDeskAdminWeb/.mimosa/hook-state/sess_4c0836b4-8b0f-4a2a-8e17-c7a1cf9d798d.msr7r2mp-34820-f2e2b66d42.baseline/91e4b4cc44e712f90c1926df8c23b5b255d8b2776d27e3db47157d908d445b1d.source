import type { P0DomainCustomer, P0InvitationCode } from "@uniondesk/shared";
import { fetchP0DomainCustomersPage, fetchP0InvitationCodes, toErrorMessage } from "@uniondesk/shared";

import { App, Table, Tabs, Tag } from "antd";
import type { TableColumnsType } from "antd";
import { useCallback, useEffect, useState } from "react";

interface DomainOnboardingPanelProps {
	domainId: string;
}

/** 客户入域：邀请码与域客户（可嵌入详情 Tab） */
export function DomainOnboardingPanel({ domainId }: DomainOnboardingPanelProps) {
	const { message } = App.useApp();
	const [invites, setInvites] = useState<P0InvitationCode[]>([]);
	const [customers, setCustomers] = useState<P0DomainCustomer[]>([]);
	const [loadingInv, setLoadingInv] = useState(false);
	const [loadingCus, setLoadingCus] = useState(false);

	const loadInvites = useCallback(async () => {
		if (!domainId) {
			setInvites([]);
			return;
		}
		setLoadingInv(true);
		try {
			const page = await fetchP0InvitationCodes(domainId);
			setInvites(page.list);
		}
		catch (error) {
			message.error(toErrorMessage(error));
		}
		finally {
			setLoadingInv(false);
		}
	}, [domainId, message]);

	const loadCustomers = useCallback(async () => {
		if (!domainId) {
			setCustomers([]);
			return;
		}
		setLoadingCus(true);
		try {
			const page = await fetchP0DomainCustomersPage({ domainId, page: 1, page_size: 100 });
			setCustomers(page.list);
		}
		catch (error) {
			message.error(toErrorMessage(error));
		}
		finally {
			setLoadingCus(false);
		}
	}, [domainId, message]);

	useEffect(() => {
		void loadInvites();
		void loadCustomers();
	}, [loadCustomers, loadInvites]);

	const invColumns: TableColumnsType<P0InvitationCode> = [
		{ title: "邀请码", dataIndex: "code", width: 160 },
		{ title: "渠道", dataIndex: "channel", render: value => value ?? "-" },
		{ title: "过期时间", dataIndex: "expires_at", render: value => value ?? "-" },
		{ title: "用量", key: "uses", render: (_, row) => `${row.used_count ?? 0}/${row.max_uses ?? "∞"}` },
		{ title: "状态", dataIndex: "status", width: 100, render: status => <Tag>{status ?? "-"}</Tag> },
	];

	const cusColumns: TableColumnsType<P0DomainCustomer> = [
		{ title: "展示名", dataIndex: "display_name" },
		{ title: "手机", dataIndex: "phone", width: 140, render: value => value ?? "-" },
		{ title: "状态", dataIndex: "status", width: 120, render: status => <Tag>{status}</Tag> },
		{ title: "来源", dataIndex: "source", width: 120, render: value => value ?? "-" },
		{ title: "创建时间", dataIndex: "created_at", width: 180 },
	];

	return (
		<Tabs
			items={[
				{
					key: "inv",
					label: "邀请码",
					children: (
						<Table<P0InvitationCode>
							rowKey="id"
							loading={loadingInv}
							columns={invColumns}
							dataSource={invites}
							pagination={false}
						/>
					),
				},
				{
					key: "cus",
					label: "域客户",
					children: (
						<Table<P0DomainCustomer>
							rowKey="id"
							loading={loadingCus}
							columns={cusColumns}
							dataSource={customers}
							pagination={false}
						/>
					),
				},
			]}
		/>
	);
}
