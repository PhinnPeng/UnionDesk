import type {
	CreateTicketStatusDefinitionBody,
	TicketStatusDefinition,
	TicketStatusDefinitionCategory,
	UpdateTicketStatusDefinitionBody,
} from "@uniondesk/shared";

import { STATUS_CATEGORY_OPTIONS } from "./status-utils";

import { Form, Input, Modal, Select } from "antd";
import { useEffect } from "react";

export interface StatusFormModalProps {
	open: boolean;
	editing: TicketStatusDefinition | null;
	submitting: boolean;
	onCancel: () => void;
	onSubmit: (body: CreateTicketStatusDefinitionBody | UpdateTicketStatusDefinitionBody) => Promise<void>;
}

export function StatusFormModal({
	open,
	editing,
	submitting,
	onCancel,
	onSubmit,
}: StatusFormModalProps) {
	const [form] = Form.useForm<{
		name: string;
		description: string;
		category: TicketStatusDefinitionCategory;
	}>();

	useEffect(() => {
		if (!open) {
			return;
		}
		if (editing) {
			form.setFieldsValue({
				name: editing.name,
				description: editing.description,
				category: editing.category,
			});
			return;
		}
		form.resetFields();
		form.setFieldsValue({
			category: "not_started",
			description: "",
		});
	}, [editing, form, open]);

	const isSystem = editing?.is_system ?? false;

	const handleOk = async () => {
		const values = await form.validateFields();
		if (editing) {
			await onSubmit({
				name: isSystem ? undefined : values.name,
				description: values.description,
				category: isSystem ? undefined : values.category,
			});
			return;
		}
		await onSubmit({
			name: values.name,
			description: values.description,
			category: values.category,
		});
	};

	return (
		<Modal
			title={editing ? "编辑状态" : "创建状态"}
			open={open}
			onCancel={onCancel}
			onOk={() => void handleOk()}
			confirmLoading={submitting}
			destroyOnHidden
			width={520}
		>
			<Form form={form} layout="vertical" preserve={false}>
				<Form.Item
					name="name"
					label="状态名称"
					rules={[{ required: !isSystem, message: "请输入状态名称" }]}
				>
					<Input placeholder="例如：待评估" disabled={isSystem} maxLength={128} />
				</Form.Item>
				<Form.Item
					name="category"
					label="状态类型"
					rules={[{ required: !isSystem, message: "请选择状态类型" }]}
				>
					<Select
						options={STATUS_CATEGORY_OPTIONS}
						disabled={isSystem}
						placeholder="选择状态类型"
					/>
				</Form.Item>
				<Form.Item name="description" label="描述">
					<Input.TextArea placeholder="可选，补充说明该状态的用途" rows={3} maxLength={500} showCount />
				</Form.Item>
			</Form>
		</Modal>
	);
}
