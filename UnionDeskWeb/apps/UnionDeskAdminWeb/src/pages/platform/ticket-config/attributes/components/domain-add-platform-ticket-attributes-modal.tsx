import type { TicketAttribute } from "@uniondesk/shared";
import { fetchPlatformTicketAttributes, toErrorMessage } from "@uniondesk/shared";

import { App, Button, Modal, Space, Table, Tag } from "antd";
import type { TableColumnsType } from "antd";
import { useCallback, useEffect, useMemo, useState } from "react";

export interface DomainAddPlatformTicketAttributesModalProps {
	open: boolean;
	excludePlatformAttributeIds: Set<string>;
	excludeNames: Set<string>;
	excludeSystemKeys: Set<string>;
	submitting: boolean;
	onCancel: () => void;
	onAdd: (platformAttributeIds: string[]) => Promise<void>;
}

export function DomainAddPlatformTicketAttributesModal({
	open,
	excludePlatformAttributeIds,
	excludeNames,
	excludeSystemKeys,
	submitting,
	onCancel,
	onAdd,
}: DomainAddPlatformTicketAttributesModalProps) {
	const { message } = App.useApp();
	const [loading, setLoading] = useState(false);
	const [allAttributes, setAllAttributes] = useState<TicketAttribute[]>([]);
	const [selectedKeys, setSelectedKeys] = useState<string[]>([]);

	const loadAttributes = useCallback(async () => {
		setLoading(true);
		try {
			const result = await fetchPlatformTicketAttributes();
			setAllAttributes(result.items);
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
		void loadAttributes();
	}, [loadAttributes, open]);

	const availableAttributes = useMemo(
		() => allAttributes.filter((attr) => {
			if (excludePlatformAttributeIds.has(attr.id)) {
				return false;
			}
			if (excludeNames.has(attr.name)) {
				return false;
			}
			if (attr.system_key && excludeSystemKeys.has(attr.system_key)) {
				return false;
			}
			return true;
		}),
		[allAttributes, excludeNames, excludePlatformAttributeIds, excludeSystemKeys],
	);

	const columns = useMemo<TableColumnsType<TicketAttribute>>(() => [
		{
			title: "属性名称",
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
			title: "字段类型",
			dataIndex: "field_type",
			key: "field_type",
			width: 120,
		},
		{
			title: "描述",
			dataIndex: "description",
			key: "description",
			ellipsis: true,
			render: value => value || "—",
		},
	], []);

	const handleAdd = async () => {
		if (selectedKeys.length === 0) {
			message.warning("请选择要添加的事项属性");
			return;
		}
		await onAdd(selectedKeys);
	};

	return (
		<Modal
			title="添加属性"
			open={open}
			onCancel={onCancel}
			width={720}
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
				从平台引用尚未添加的事项属性（含系统属性）；已添加、类型带入或同名的不会出现在列表中。
			</div>
			<Table
				rowKey="id"
				size="small"
				loading={loading}
				columns={columns}
				dataSource={availableAttributes}
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
