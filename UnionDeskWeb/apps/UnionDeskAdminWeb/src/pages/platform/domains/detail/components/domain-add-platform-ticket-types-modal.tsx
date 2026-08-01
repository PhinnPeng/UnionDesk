import type { PlatformTicketType } from "@uniondesk/shared";
import { fetchPlatformTicketTypes, toErrorMessage } from "@uniondesk/shared";

import { App, Button, Modal, Table } from "antd";
import type { TableColumnsType } from "antd";
import { useCallback, useEffect, useMemo, useState } from "react";

export interface DomainAddPlatformTicketTypesModalProps {
	open: boolean;
	excludePlatformTypeIds: Set<string>;
	submitting: boolean;
	onCancel: () => void;
	onAdd: (platformTypeIds: string[]) => Promise<void>;
}

export function DomainAddPlatformTicketTypesModal({
	open,
	excludePlatformTypeIds,
	submitting,
	onCancel,
	onAdd,
}: DomainAddPlatformTicketTypesModalProps) {
	const { message } = App.useApp();
	const [loading, setLoading] = useState(false);
	const [allTypes, setAllTypes] = useState<PlatformTicketType[]>([]);
	const [selectedKeys, setSelectedKeys] = useState<string[]>([]);

	const loadTypes = useCallback(async () => {
		setLoading(true);
		try {
			const result = await fetchPlatformTicketTypes();
			setAllTypes(result.items);
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
		void loadTypes();
	}, [loadTypes, open]);

	const availableTypes = useMemo(
		() => allTypes.filter(type => !excludePlatformTypeIds.has(type.id)),
		[allTypes, excludePlatformTypeIds],
	);

	const columns = useMemo<TableColumnsType<PlatformTicketType>>(() => [
		{
			title: "事项类型名称",
			dataIndex: "name",
			key: "name",
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
			message.warning("请选择要添加的事项类型");
			return;
		}
		await onAdd(selectedKeys);
	};

	return (
		<Modal
			title="添加事项类型"
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
				从平台引用尚未添加的事项类型；将连同关联属性一并落入本域，并复制工作流与描述模板。
			</div>
			<Table
				rowKey="id"
				size="small"
				loading={loading}
				columns={columns}
				dataSource={availableTypes}
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
