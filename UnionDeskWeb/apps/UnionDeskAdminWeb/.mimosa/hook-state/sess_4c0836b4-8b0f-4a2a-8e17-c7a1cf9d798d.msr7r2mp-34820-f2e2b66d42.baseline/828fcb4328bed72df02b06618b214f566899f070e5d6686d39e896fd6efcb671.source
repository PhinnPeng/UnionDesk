import type { PlatformTicketType } from "@uniondesk/shared";

import { IconPicker } from "#src/components/icon-picker";

import type { TicketTypeTemplateKey } from "./ticket-type-utils";
import { findTicketTypeTemplate } from "./ticket-type-utils";

import { Alert, Form, Input, Modal } from "antd";
import { useEffect } from "react";

interface TicketTypeFormValues {
	name: string;
	icon: string;
	description?: string;
}

interface TicketTypeFormModalProps {
	open: boolean;
	loading: boolean;
	editing: PlatformTicketType | null;
	templateKey?: TicketTypeTemplateKey | null;
	copyFrom?: PlatformTicketType | null;
	onCancel: () => void;
	onSubmit: (values: TicketTypeFormValues) => Promise<void>;
}

export function TicketTypeFormModal({
	open,
	loading,
	editing,
	templateKey,
	copyFrom,
	onCancel,
	onSubmit,
}: TicketTypeFormModalProps) {
	const [form] = Form.useForm<TicketTypeFormValues>();

	useEffect(() => {
		if (!open) {
			return;
		}
		if (editing) {
			form.setFieldsValue({
				name: editing.name,
				icon: editing.icon,
				description: editing.description ?? "",
			});
			return;
		}
		if (copyFrom) {
			form.setFieldsValue({
				name: `${copyFrom.name}（副本）`,
				icon: copyFrom.icon,
				description: copyFrom.description ?? "",
			});
			return;
		}
		const template = templateKey ? findTicketTypeTemplate(templateKey) : null;
		form.setFieldsValue({
			name: template?.defaultName ?? "",
			icon: template?.defaultIcon ?? "",
			description: template?.defaultDescription ?? "",
		});
	}, [copyFrom, editing, form, open, templateKey]);

	const handleOk = async () => {
		const values = await form.validateFields();
		await onSubmit(values);
	};

	const template = templateKey ? findTicketTypeTemplate(templateKey) : null;

	return (
		<Modal
			title={editing ? "编辑事项类型" : copyFrom ? "复制为新类型" : template ? `创建${template.label}` : "创建事项类型"}
			open={open}
			onCancel={onCancel}
			onOk={() => void handleOk()}
			confirmLoading={loading}
			destroyOnHidden
			width={520}
		>
			{template && !editing && !copyFrom ? (
				<Alert
					type="info"
					showIcon
					className="mb-4"
					message={`将创建${template.label}`}
					description={template.helperText}
				/>
			) : null}
			<Form form={form} layout="vertical" preserve={false}>
				<Form.Item
					name="name"
					label="名称"
					rules={[{ required: true, message: "请输入名称" }]}
				>
					<Input placeholder="请输入事项类型名称" maxLength={64} />
				</Form.Item>
				<Form.Item
					name="icon"
					label="选择类型图标"
					rules={[{ required: true, message: "请选择图标" }]}
				>
					<IconPicker />
				</Form.Item>
				<Form.Item name="description" label="描述">
					<Input.TextArea placeholder="可选，描述该类型的用途" rows={3} maxLength={500} />
				</Form.Item>
			</Form>
		</Modal>
	);
}
