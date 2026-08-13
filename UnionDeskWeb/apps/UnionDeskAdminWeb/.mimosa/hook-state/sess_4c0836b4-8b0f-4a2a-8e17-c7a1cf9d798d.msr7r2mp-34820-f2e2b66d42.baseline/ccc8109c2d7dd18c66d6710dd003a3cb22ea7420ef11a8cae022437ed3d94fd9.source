import type { PlatformTicketType } from "@uniondesk/shared";
import {
	fetchPlatformTicketTypes,
	toErrorMessage,
} from "@uniondesk/shared";

import { AuthGuarded } from "#src/components/auth-guarded";
import { PLATFORM_TICKET_CONFIG_TYPE_CREATE } from "#src/pages/platform/domains/platform-domain-permissions";
import { buildTicketConfigPath } from "#src/pages/platform/ticket-config/ticket-config-path";

import { App, Button, Modal, Table } from "antd";
import type { TableColumnsType } from "antd";
import { useCallback, useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router";

export interface AddTicketTypeModalProps {
	open: boolean
	excludeTypeIds: Set<string>
	submitting: boolean
	onCancel: () => void
	onAdd: (types: PlatformTicketType[]) => Promise<void>
}

export function AddTicketTypeModal({
	open,
	excludeTypeIds,
	submitting,
	onCancel,
	onAdd,
}: AddTicketTypeModalProps) {
	const { message } = App.useApp();
	const navigate = useNavigate();
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
		() => allTypes.filter(type => !excludeTypeIds.has(type.id)),
		[allTypes, excludeTypeIds],
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
		const selected = availableTypes.filter(type => selectedKeys.includes(type.id));
		if (selected.length === 0) {
			message.warning("请选择要添加的事项类型");
			return;
		}
		await onAdd(selected);
	};

	/** 跳转事项类型页并自动展开「创建事项类型」下拉 */
	const handleGoCreate = () => {
		onCancel();
		const base = buildTicketConfigPath({ section: "types" });
		navigate(base.includes("?") ? `${base}&action=create` : `${base}?action=create`);
	};

	return (
		<Modal
			title="添加事项类型"
			open={open}
			onCancel={onCancel}
			width={720}
			destroyOnHidden
			footer={(
				<div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
					<AuthGuarded auth={PLATFORM_TICKET_CONFIG_TYPE_CREATE} fallback={<span />}>
						<Button type="link" style={{ paddingInline: 0 }} onClick={handleGoCreate}>
							新建事项类型
						</Button>
					</AuthGuarded>
					<div style={{ display: "flex", gap: 8 }}>
						<Button onClick={onCancel}>取消</Button>
						<Button type="primary" loading={submitting} onClick={() => void handleAdd()}>
							添加
						</Button>
					</div>
				</div>
			)}
		>
			<div style={{ marginBottom: 12, color: "var(--ant-color-text-secondary)" }}>从全局添加</div>
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
