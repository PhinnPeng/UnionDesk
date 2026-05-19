import type { P0AdminDomain, P0DomainCustomer, P0InvitationCode } from "@uniondesk/shared";

import { BasicContent } from "#src/components/basic-content";

import { fetchP0AdminDomainsPage, fetchP0DomainCustomersPage, fetchP0InvitationCodes, toErrorMessage } from "@uniondesk/shared";
import { App, Alert, Select, Space, Table, Tabs, Tag } from "antd";
import type { TableColumnsType } from "antd";
import { useCallback, useEffect, useState } from "react";

export default function PlatformDomainOnboarding() {
	const { message } = App.useApp();
	const [domains, setDomains] = useState<P0AdminDomain[]>([]);
	const [domainId, setDomainId] = useState<string | undefined>();
	const [invites, setInvites] = useState<P0InvitationCode[]>([]);
	const [customers, setCustomers] = useState<P0DomainCustomer[]>([]);
	const [loadingInv, setLoadingInv] = useState(false);
	const [loadingCus, setLoadingCus] = useState(false);

	const loadDomains = useCallback(async () => {
		try {
			const page = await fetchP0AdminDomainsPage({ page: 1, page_size: 100 });
			setDomains(page.list);
			setDomainId(prev => prev ?? page.list[0]?.id);
		} catch (e) {
			message.error(toErrorMessage(e));
		}
	}, [message]);

	const loadInvites = useCallback(async () => {
		if (!domainId) {
			setInvites([]);
			return;
		}
		setLoadingInv(true);
		try {
			const page = await fetchP0InvitationCodes(domainId);
			setInvites(page.list);
		} catch (e) {
			message.error(toErrorMessage(e));
		} finally {
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
		} catch (e) {
			message.error(toErrorMessage(e));
		} finally {
			setLoadingCus(false);
		}
	}, [domainId, message]);

	useEffect(() => {
		void loadDomains();
	}, [loadDomains]);

	useEffect(() => {
		void loadInvites();
		void loadCustomers();
	}, [loadCustomers, loadInvites]);

	const invColumns: TableColumnsType<P0InvitationCode> = [
		{ title: "邀请码", dataIndex: "code", width: 160 },
		{ title: "渠道", dataIndex: "channel", render: v => v ?? "-" },
		{ title: "过期时间", dataIndex: "expires_at", render: v => v ?? "-" },
		{ title: "用量", key: "uses", render: (_, r) => `${r.used_count ?? 0}/${r.max_uses ?? "∞"}` },
		{ title: "状态", dataIndex: "status", width: 100, render: s => <Tag>{s ?? "-"}</Tag> },
	];

	const cusColumns: TableColumnsType<P0DomainCustomer> = [
		{ title: "展示名", dataIndex: "display_name" },
		{ title: "手机", dataIndex: "phone", width: 140, render: v => v ?? "-" },
		{
			title: "状态",
			dataIndex: "status",
			width: 120,
			render: s => <Tag>{s}</Tag>,
		},
		{ title: "来源", dataIndex: "source", width: 120, render: v => v ?? "-" },
		{ title: "创建时间", dataIndex: "created_at", width: 180 },
	];

	return (
		<BasicContent className="h-full bg-colorBgLayout">
			<Space direction="vertical" size="large" className="w-full">
				<Alert
					type="info"
					showIcon
					message="客户入域（P0）"
					description="邀请码：`GET /admin/domains/{id}/invitation-codes`；域客户：`GET /admin/domains/{id}/customers`。后端未发布时表格为空。"
				/>
				<Space wrap>
					<span>业务域</span>
					<Select
						className="min-w-56"
						value={domainId}
						options={domains.map(d => ({ value: d.id, label: `${d.name} (${d.code})` }))}
						onChange={v => setDomainId(v)}
					/>
				</Space>
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
			</Space>
		</BasicContent>
	);
}
