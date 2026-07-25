import type { DomainTicketTemplate, DomainTicketType } from "@uniondesk/shared";
import {
	createDomainTicketTemplate,
	updateDomainTicketTemplate,
	toErrorMessage,
} from "@uniondesk/shared";

import { App, Form, Input, InputNumber, Modal, Select } from "antd";
import { useEffect } from "react";

export interface TicketTemplateModalProps {
	open: boolean;
	domainId: string;
	ticketTypes: DomainTicketType[];
	template: DomainTicketTemplate | null;
	onClose: () => void;
	onSaved: () => void;
}

interface TemplateFormValues {
	name: string;
	type_id: string;
	content?: string;
	sort_order?: number;
}

export function TicketTemplateModal({
	open,
	domainId,
	ticketTypes,
	template,
	onClose,
	onSaved,
}: TicketTemplateModalProps) {
	const { message } = App.useApp();
	const [form] = Form.useForm<TemplateFormValues>();

	useEffect(() => {
		if (!open) {
			return;
		}
		if (template) {
			form.setFieldsValue({
				name: template.name,
				type_id: template.type_id,
				content: template.content ?? "",
				sort_order: template.sort_order ?? undefined,
			});
			return;
		}
		form.setFieldsValue({
			name: "",
			type_id: ticketTypes[0]?.id,
			content: "",
			sort_order: undefined,
		});
	}, [form, open, template, ticketTypes]);

	const handleSubmit = async () => {
		const values = await form.validateFields();
		const selectedType = ticketTypes.find(item => item.id === values.type_id);
		if (!selectedType) {
			message.warning("请选择关联事项类型");
			return;
		}
		try {
			if (template) {
				await updateDomainTicketTemplate(domainId, template.id, {
					name: values.name.trim(),
					type: selectedType.code,
					type_id: selectedType.id,
					content: values.content?.trim() || undefined,
					sort_order: values.sort_order,
				});
				message.success("模板已更新");
			}
			else {
				await createDomainTicketTemplate(domainId, {
					name: values.name.trim(),
					type: selectedType.code,
					type_id: selectedType.id,
					content: values.content?.trim() || undefined,
					sort_order: values.sort_order,
				});
				message.success("模板已创建");
			}
			onSaved();
			onClose();
		}
		catch (error) {
			message.error(toErrorMessage(error));
		}
	};

	return (
		<Modal
			title={template ? "编辑事项模板" : "新建事项模板"}
			open={open}
			okText={template ? "保存" : "创建"}
			cancelText="取消"
			destroyOnClose
			onCancel={onClose}
			onOk={() => void handleSubmit()}
		>
			<Form form={form} layout="vertical">
				<Form.Item
					name="name"
					label="模板名称"
					rules={[{ required: true, message: "请输入模板名称" }]}
				>
					<Input placeholder="例如：常见问题反馈" maxLength={64} />
				</Form.Item>
				<Form.Item
					name="type_id"
					label="关联事项类型"
					rules={[{ required: true, message: "请选择事项类型" }]}
				>
					<Select
						options={ticketTypes.map(item => ({ value: item.id, label: `${item.name}（${item.code}）` }))}
						placeholder="选择事项类型"
					/>
				</Form.Item>
				<Form.Item name="content" label="模板内容">
					<Input.TextArea rows={5} placeholder="可选：预填工单描述或指引文案" />
				</Form.Item>
				<Form.Item name="sort_order" label="排序">
					<InputNumber min={0} className="w-full" placeholder="数字越小越靠前" />
				</Form.Item>
			</Form>
		</Modal>
	);
}
