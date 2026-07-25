import type { TicketAttribute } from "@uniondesk/shared";

import { appScopes } from "#src/router/extra-info/app-scope";
import { openAppScopeTab } from "#src/utils/tabbar-utils";

import { buildTicketTypeAttributesPath } from "#src/pages/platform/domains/ticket-type-attributes/path";
import { buildTicketConfigPath, TICKET_CONFIG_BASE } from "#src/pages/platform/ticket-config/ticket-config-path";

import { Button, Form, Modal, Select, Space, Switch, Typography } from "antd";
import { useCallback, useMemo, useState } from "react";
import { useNavigate } from "react-router";

const { Text } = Typography;

interface AddAttributeModalProps {
	open: boolean;
	domainId?: string;
	createAttributePath?: string;
	availableAttributes: TicketAttribute[];
	insertedAttributeIds: Set<string>;
	loading: boolean;
	onConfirm: (attributeId: string, required: boolean) => void;
	onCancel: () => void;
}

export function AddAttributeModal({
	open,
	domainId,
	createAttributePath,
	availableAttributes,
	insertedAttributeIds,
	loading,
	onConfirm,
	onCancel,
}: AddAttributeModalProps) {
	const navigate = useNavigate();
	const [attributeId, setAttributeId] = useState<string>();
	const [required, setRequired] = useState(false);

	const options = useMemo(
		() => availableAttributes
			.filter(item => !insertedAttributeIds.has(item.id))
			.map(item => ({ value: item.id, label: item.name })),
		[availableAttributes, insertedAttributeIds],
	);

	const handleCancel = useCallback(() => {
		setAttributeId(undefined);
		setRequired(false);
		onCancel();
	}, [onCancel]);

	const handleConfirm = useCallback(() => {
		if (!attributeId) {
			return;
		}
		onConfirm(attributeId, required);
		setAttributeId(undefined);
		setRequired(false);
	}, [attributeId, required, onConfirm]);

	const handleGoCreate = useCallback(() => {
		const path = createAttributePath
			?? (domainId ? buildTicketTypeAttributesPath(domainId, "new") : buildTicketConfigPath({ section: "attributes" }));
		if (path.startsWith(TICKET_CONFIG_BASE)) {
			navigate(path, { replace: true });
			return;
		}
		openAppScopeTab(appScopes.platform, navigate, path, {
			key: path,
			label: "新建属性",
			newTabTitle: "新建属性",
			closable: true,
			draggable: true,
		});
	}, [createAttributePath, domainId, navigate]);

	return (
		<Modal
			title="添加属性"
			open={open}
			confirmLoading={loading}
			okText="确定添加"
			cancelText="取消"
			destroyOnHidden
			width={480}
			onCancel={handleCancel}
			onOk={handleConfirm}
			okButtonProps={{ disabled: !attributeId }}
		>
			<Form layout="vertical">
				<Form.Item label="属性" required>
					<Select
						placeholder="请选择属性"
						options={options}
						value={attributeId}
						onChange={setAttributeId}
						allowClear
						showSearch
						filterOption={(input, option) =>
							(option?.label ?? "").toLowerCase().includes(input.toLowerCase())
						}
						notFoundContent={(
							<Space direction="vertical" align="center" className="w-full py-2">
								<Text type="secondary">暂无可用属性</Text>
							</Space>
						)}
					/>
				</Form.Item>
				<div className="mb-4">
					<Text type="secondary">
						没有找到想要的？
						<Button type="link" size="small" onClick={handleGoCreate}>
							前往新建属性
						</Button>
					</Text>
				</div>
				<Form.Item label="是否必填">
					<Switch checked={required} onChange={setRequired} />
				</Form.Item>
			</Form>
		</Modal>
	);
}
