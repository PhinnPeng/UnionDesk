import type { CreateTeamTemplateBody } from "@uniondesk/shared";

import { Form, Input, Modal } from "antd";
import { useEffect } from "react";

type FormValues = {
	name: string;
	description?: string;
};

export interface TeamTemplateFormModalProps {
	open: boolean;
	submitting: boolean;
	onCancel: () => void;
	onSubmit: (body: CreateTeamTemplateBody) => Promise<void>;
}

export function TeamTemplateFormModal({
	open,
	submitting,
	onCancel,
	onSubmit,
}: TeamTemplateFormModalProps) {
	const [form] = Form.useForm<FormValues>();

	useEffect(() => {
		if (!open) {
			return;
		}
		form.resetFields();
		form.setFieldsValue({
			name: "",
			description: "",
		});
	}, [form, open]);

	const handleOk = async () => {
		const values = await form.validateFields();
		await onSubmit({
			name: values.name.trim(),
			description: values.description?.trim() || undefined,
		});
	};

	return (
		<Modal
			title="创建团队模板"
			open={open}
			onCancel={onCancel}
			onOk={() => void handleOk()}
			okText="创建"
			cancelText="取消"
			confirmLoading={submitting}
			destroyOnHidden
			width={520}
		>
			<Form form={form} layout="vertical" preserve={false}>
				<Form.Item
					name="name"
					label="模板名称"
					rules={[{ required: true, message: "请输入模板名称" }]}
				>
					<Input placeholder="例如：客服标准模板" maxLength={128} />
				</Form.Item>
				<Form.Item name="description" label="描述">
					<Input.TextArea rows={3} maxLength={500} placeholder="说明模板用途" />
				</Form.Item>
			</Form>
		</Modal>
	);
}
