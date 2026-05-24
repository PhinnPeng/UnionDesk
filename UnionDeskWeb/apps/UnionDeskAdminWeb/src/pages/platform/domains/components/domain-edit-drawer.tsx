import type { AdminDomain, CreateAdminDomainPayload } from "@uniondesk/shared";
import { toErrorMessage, updateAdminDomain } from "@uniondesk/shared";

import { AuthGuarded } from "#src/components/auth-guarded";

import { App, Button, Col, Drawer, Form, Input, Row, Select, Space } from "antd";
import { useEffect, useState } from "react";

import { normalizeVisibility, registrationOptions } from "../constants";
import { DEFAULT_DOMAIN_LOGO, resolveNumericDomainId } from "../domain-utils";
import { DomainLogoField } from "./domain-logo-field";
import { VisibilityPolicyField } from "./visibility-policy-field";

export interface DomainEditDrawerProps {
	open: boolean;
	domain: AdminDomain | null;
	onClose: () => void;
	onSaved: (domain: AdminDomain) => void;
}

type EditFormValues = Pick<CreateAdminDomainPayload, "name" | "logo" | "description" | "registration_policy" | "visibility_policy_codes">;

export function DomainEditDrawer({ open, domain, onClose, onSaved }: DomainEditDrawerProps) {
	const { message, modal } = App.useApp();
	const [form] = Form.useForm<EditFormValues>();
	const [submitting, setSubmitting] = useState(false);
	const [dirty, setDirty] = useState(false);

	useEffect(() => {
		if (!open || !domain) {
			form.resetFields();
			setDirty(false);
			return;
		}
		form.setFieldsValue({
			name: domain.name,
			logo: domain.logo?.trim() || DEFAULT_DOMAIN_LOGO,
			description: domain.description ?? "",
			registration_policy: domain.registration_policy,
			visibility_policy_codes: domain.visibility_policy_codes,
		});
		setDirty(false);
	}, [domain, form, open]);

	const requestClose = () => {
		if (dirty) {
			modal.confirm({
				title: "放弃修改？",
				content: "当前表单尚未保存，关闭后将丢失已填写内容。",
				okText: "放弃",
				cancelText: "继续编辑",
				onOk: onClose,
			});
			return;
		}
		onClose();
	};

	const handleSubmit = async () => {
		if (!domain) {
			return;
		}
		const values = await form.validateFields().catch(() => null);
		if (!values) {
			return;
		}
		const logo = values.logo?.trim() || DEFAULT_DOMAIN_LOGO;
		setSubmitting(true);
		try {
			await updateAdminDomain(domain.id, {
				name: values.name.trim(),
				logo,
				description: values.description?.trim() || undefined,
				registration_policy: values.registration_policy,
				visibility_policy_codes: normalizeVisibility(values.visibility_policy_codes ?? ["public"]),
			});
			message.success("已保存业务域");
			onSaved({
				...domain,
				name: values.name.trim(),
				logo,
				description: values.description?.trim() || null,
				registration_policy: values.registration_policy,
				visibility_policy_codes: normalizeVisibility(values.visibility_policy_codes ?? ["public"]),
			});
			onClose();
		}
		catch (error) {
			message.error(toErrorMessage(error));
		}
		finally {
			setSubmitting(false);
		}
	};

	const uploadDomainId = domain ? resolveNumericDomainId(domain.id) : null;

	return (
		<Drawer
			title={`编辑业务域${domain ? `：${domain.name}` : ""}`}
			width={560}
			open={open}
			onClose={requestClose}
			maskClosable={false}
			destroyOnClose
			footer={(
				<div className="flex justify-end">
					<Space>
						<Button onClick={requestClose}>取消</Button>
						<AuthGuarded auth="domain.admin.update">
							<Button type="primary" loading={submitting} onClick={() => void handleSubmit()}>
								保存
							</Button>
						</AuthGuarded>
					</Space>
				</div>
			)}
		>
			<Form form={form} layout="vertical" onValuesChange={() => setDirty(true)}>
				<DomainLogoField form={form} uploadDomainId={uploadDomainId} previewName={domain?.name} />
				<Row gutter={16}>
					<Col span={24}>
						<Form.Item name="name" label="名称" rules={[{ required: true, message: "请输入名称" }]}>
							<Input placeholder="业务域显示名称" />
						</Form.Item>
					</Col>
					<Col span={24}>
						<Form.Item label="短码">
							<Input disabled value={domain?.code ?? ""} />
						</Form.Item>
					</Col>
					<Col span={24}>
						<Form.Item name="description" label="描述">
							<Input.TextArea rows={3} maxLength={512} showCount placeholder="选填，简要说明业务域用途" />
						</Form.Item>
					</Col>
					<Col span={24}>
						<Form.Item name="registration_policy" label="注册策略" rules={[{ required: true }]}>
							<Select options={registrationOptions} />
						</Form.Item>
					</Col>
					<Col span={24}>
						<VisibilityPolicyField form={form} />
					</Col>
				</Row>
			</Form>
		</Drawer>
	);
}
