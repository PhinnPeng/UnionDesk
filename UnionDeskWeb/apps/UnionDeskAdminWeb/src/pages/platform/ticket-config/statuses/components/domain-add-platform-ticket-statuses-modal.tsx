import type { TicketStatusDefinition } from "@uniondesk/shared";
import { fetchPlatformTicketStatuses, toErrorMessage } from "@uniondesk/shared";

import {
	formatDateTime,
	getStatusCategoryBadgeColor,
	getStatusCategoryLabel,
} from "./status-utils";

import { App, Badge, Button, Modal, Space, Table, Tag } from "antd";
import type { TableColumnsType } from "antd";
import { useCallback, useEffect, useMemo, useState } from "react";

export interface DomainAddPlatformTicketStatusesModalProps {
	open: boolean;
	excludePlatformStatusIds: Set<string>;
	excludeNames: Set<string>;
	submitting: boolean;
	onCancel: () => void;
	onAdd: (platformStatusIds: string[]) => Promise<void>;
}

export function DomainAddPlatformTicketStatusesModal({
	open,
	excludePlatformStatusIds,
	excludeNames,
	submitting,
	onCancel,
	onAdd,
}: DomainAddPlatformTicketStatusesModalProps) {
	const { message } = App.useApp();
	const [loading, setLoading] = useState(false);
	const [allStatuses, setAllStatuses] = useState<TicketStatusDefinition[]>([]);
	const [selectedKeys, setSelectedKeys] = useState<string[]>([]);

	const loadStatuses = useCallback(async () => {
		setLoading(true);
		try {
			const result = await fetchPlatformTicketStatuses();
			setAllStatuses(result.items);
		}
		catch (error) {
			message.error(toErrorMessage(error));
		}
		finally {
			setLoading(false);
		}
	}, [message]);

	useEffect(() => {
		if (!open) {
			return;
		}
		setSelectedKeys([]);
		void loadStatuses();
	}, [loadStatuses, open]);

	const availableStatuses = useMemo(
		() => allStatuses.filter(status =>
			!excludePlatformStatusIds.has(status.id) && !excludeNames.has(status.name)),
		[allStatuses, excludeNames, excludePlatformStatusIds],
	);

	const columns = useMemo<TableColumnsType<TicketStatusDefinition>>(() => [
		{
			title: "状态名称",
			dataIndex: "name",
			key: "name",
			render: (_value, record) => (
				<Space size={8}>
					<span>{record.name}</span>
					{record.is_system ? <Tag color="default">系统</Tag> : null}
				</Space>
			),
		},
		{
			title: "状态类型",
			dataIndex: "category",
			key: "category",
			width: 120,
			render: value => (
				<Badge
					color={getStatusCategoryBadgeColor(value)}
					text={getStatusCategoryLabel(value)}
				/>
			),
		},
		{
			title: "描述",
			dataIndex: "description",
			key: "description",
			ellipsis: true,
			render: value => value || "—",
		},
		{
			title: "创建时间",
			dataIndex: "created_at",
			key: "created_at",
			width: 160,
			render: value => formatDateTime(value),
		},
	], []);

	const handleAdd = async () => {
		if (selectedKeys.length === 0) {
			message.warning("请选择要添加的事项状态");
			return;
		}
		await onAdd(selectedKeys);
	};

	return (
		<Modal
			title="添加状态"
			open={open}
			onCancel={onCancel}
			width={800}
			destroyOnHidden
			footer={(
				<div style={{ display: "flex", justifyContent: "flex-end", gap: 8 }}>
					<Button onClick={onCancel}>取消</Button>
					<Button type="primary" loading={submitting} onClick={() => void handleAdd()}>
						添加
					</Button>
				</div>
			)}
		>
			<div style={{ marginBottom: 12, color: "var(--ant-color-text-secondary)" }}>
				从平台引用尚未添加的事项状态（含系统状态）；已添加或同名的不会出现在列表中。
			</div>
			<Table
				rowKey="id"
				size="small"
				loading={loading}
				columns={columns}
				dataSource={availableStatuses}
				pagination={false}
				locale={{ emptyText: "未找到相关结果" }}
				rowSelection={{
					selectedRowKeys: selectedKeys,
					onChange: keys => setSelectedKeys(keys.map(String)),
				}}
			/>
		</Modal>
	);
}
