import type { P0InboxMessage } from "@uniondesk/shared";

import { BasicContent } from "#src/components/basic-content";

import { fetchP0InboxPage, fetchP0InboxUnreadCount, markP0InboxMessageRead, toErrorMessage } from "@uniondesk/shared";
import { App, Alert, Button, Card, Space, Table, Tag } from "antd";
import type { TableColumnsType } from "antd";
import { useCallback, useEffect, useState } from "react";

export default function PlatformInbox() {
	const { message } = App.useApp();
	const [rows, setRows] = useState<P0InboxMessage[]>([]);
	const [unread, setUnread] = useState(0);
	const [loading, setLoading] = useState(false);

	const load = useCallback(async () => {
		setLoading(true);
		try {
			const [page, count] = await Promise.all([
				fetchP0InboxPage({ page: 1, page_size: 50 }),
				fetchP0InboxUnreadCount(),
			]);
			setRows(page.list);
			setUnread(typeof page.unread_count === "number" ? page.unread_count : count);
		} catch (e) {
			message.error(toErrorMessage(e));
		} finally {
			setLoading(false);
		}
	}, [message]);

	useEffect(() => {
		void load();
	}, [load]);

	const markRead = async (id: string) => {
		try {
			await markP0InboxMessageRead(id);
			message.success("已标记已读");
			await load();
		} catch (e) {
			message.error(toErrorMessage(e));
		}
	};

	const columns: TableColumnsType<P0InboxMessage> = [
		{ title: "标题", dataIndex: "title", width: 220 },
		{ title: "业务域", dataIndex: "domain_name", width: 160, render: v => v ?? "-" },
		{
			title: "状态",
			dataIndex: "is_read",
			width: 100,
			render: (read: boolean) => <Tag color={read ? "default" : "processing"}>{read ? "已读" : "未读"}</Tag>,
		},
		{ title: "时间", dataIndex: "created_at", width: 180 },
		{
			title: "操作",
			key: "op",
			width: 120,
			render: (_, row) => (
				<Button type="link" size="small" disabled={row.is_read} onClick={() => void markRead(row.id)}>标记已读</Button>
			),
		},
	];

	return (
		<BasicContent className="h-full bg-colorBgLayout">
			<Card title="通知中心 · 站内信（P0）" extra={<Button onClick={() => void load()}>刷新</Button>}>
				<Alert
					type="info"
					showIcon
					className="mb-4"
					message="接口对齐"
					description="使用 `GET /api/v1/inbox` 与 `GET /api/v1/inbox/unread-count`；后端未发布时返回空列表。"
				/>
				<Space className="mb-3">
					<span>未读</span>
					<Tag color="blue">{unread}</Tag>
				</Space>
				<Table<P0InboxMessage> rowKey="id" loading={loading} columns={columns} dataSource={rows} pagination={false} />
			</Card>
		</BasicContent>
	);
}
