import type { TeamTemplate, TeamTemplateItemBody } from "@uniondesk/shared";
import { deleteTeamTemplate, toErrorMessage, updateTeamTemplate } from "@uniondesk/shared";

import { AuthGuarded } from "#src/components/auth-guarded";
import { ConfirmPopover } from "#src/components/confirm-popover";
import {
	PLATFORM_TICKET_CONFIG_TEMPLATE_DELETE,
	PLATFORM_TICKET_CONFIG_TEMPLATE_UPDATE,
} from "#src/pages/platform/domains/platform-domain-permissions";
import { buildTicketConfigPath } from "#src/pages/platform/ticket-config/ticket-config-path";

import { App, Button, Form, Input, Typography } from "antd";
import { useEffect, useState } from "react";
import { useNavigate } from "react-router";

type FormValues = {
	name: string;
	description?: string;
};

function toItemBodies(template: TeamTemplate): TeamTemplateItemBody[] {
	return (template.items ?? []).map((item, index) => ({
		ticket_type_id: Number(item.ticket_type_id),
		sort_order: item.sort_order ?? index,
		include_form_schema: item.include_form_schema,
		include_workflow: item.include_workflow,
		include_description_template: item.include_description_template,
	}));
}

export interface BasicInfoPanelProps {
	template: TeamTemplate;
	onUpdated: (next: TeamTemplate) => void;
}

export function BasicInfoPanel({ template, onUpdated }: BasicInfoPanelProps) {
	const { message } = App.useApp();
	const navigate = useNavigate();
	const [form] = Form.useForm<FormValues>();
	const [saving, setSaving] = useState(false);
	const [deleting, setDeleting] = useState(false);

	useEffect(() => {
		form.setFieldsValue({
			name: template.name,
			description: template.description ?? "",
		});
	}, [form, template.description, template.id, template.name]);

	const handleSave = async () => {
		const values = await form.validateFields();
		setSaving(true);
		try {
			const next = await updateTeamTemplate(template.id, {
				name: values.name.trim(),
				description: values.description?.trim() ?? "",
				items: toItemBodies(template),
			});
			message.success("已保存");
			onUpdated(next);
		}
		catch (error) {
			message.error(toErrorMessage(error));
		}
		finally {
			setSaving(false);
		}
	};

	const handleDelete = async () => {
		setDeleting(true);
		try {
			await deleteTeamTemplate(template.id);
			message.success("团队模板已删除");
			navigate(buildTicketConfigPath({ section: "templates" }), { replace: true });
		}
		catch (error) {
			message.error(toErrorMessage(error));
		}
		finally {
			setDeleting(false);
		}
	};

	return (
		<div>
			<Form form={form} layout="vertical" style={{ maxWidth: 560 }}>
				<Form.Item
					name="name"
					label="名称"
					rules={[{ required: true, message: "请输入名称" }]}
				>
					<Input maxLength={128} placeholder="模板名称" />
				</Form.Item>
				<Form.Item name="description" label="描述">
					<Input.TextArea rows={4} maxLength={500} placeholder="模板描述" />
				</Form.Item>
			</Form>

			<div className="team-template-config__basic-actions">
				<AuthGuarded auth={PLATFORM_TICKET_CONFIG_TEMPLATE_UPDATE} fallback={null}>
					<Button type="primary" loading={saving} onClick={() => void handleSave()}>
						保存
					</Button>
				</AuthGuarded>
			</div>

			<div className="team-template-config__danger">
				<div className="team-template-config__danger-title">删除配置方案</div>
				<Typography.Paragraph type="secondary">
					删除后不可恢复。已套用过该模板的业务域不受影响。
				</Typography.Paragraph>
				<AuthGuarded auth={PLATFORM_TICKET_CONFIG_TEMPLATE_DELETE} fallback={null}>
					<ConfirmPopover
						title="确认删除该团队模板？"
						onConfirm={() => void handleDelete()}
					>
						<Button danger loading={deleting}>删除</Button>
					</ConfirmPopover>
				</AuthGuarded>
			</div>
		</div>
	);
}
