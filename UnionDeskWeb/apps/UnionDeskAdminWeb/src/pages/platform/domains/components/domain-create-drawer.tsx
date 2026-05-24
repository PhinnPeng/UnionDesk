import type { CreateAdminDomainPayload, P0VisibilityPolicyCode } from "@uniondesk/shared";
import { createAdminDomain, toErrorMessage } from "@uniondesk/shared";

import { AuthGuarded } from "#src/components/auth-guarded";

import { ReloadOutlined } from "@ant-design/icons";
import { App, Button, Col, Drawer, Form, Input, Row, Select, Space } from "antd";
import { useEffect, useState } from "react";

import { normalizeVisibility, registrationOptions } from "../constants";
import { DEFAULT_DOMAIN_LOGO, DOMAIN_LOGO_UPLOAD_FALLBACK_DOMAIN_ID, generateDomainCode } from "../domain-utils";
import { DomainLogoField } from "./domain-logo-field";
import { VisibilityPolicyField } from "./visibility-policy-field";

export interface DomainCreateDrawerProps {
	open: boolean;
	onClose: () => void;
	onCreated: (result: { id: string; name: string }) => void;
}

export function DomainCreateDrawer({ open, onClose, onCreated }: DomainCreateDrawerProps) {
	const { message, modal } = App.useApp();
	const [form] = Form.useForm<CreateAdminDomainPayload>();
	const [submitting, setSubmitting] = useState(false);
	const [dirty, setDirty] = useState(false);

	useEffect(() => {
		if (!open) {
			form.resetFields();
			setDirty(false);
		}
	}, [form, open]);

	const requestClose = () => {
		if (dirty) {
			modal.confirm({
				title: "放弃新建？",
				content: "当前表单尚未保存，关闭后将丢失已填写内容。",
				okText: "放弃",
				cancelText: "继续编辑",
				onOk: onClose,
			});
			return;
		}
		onClose();
	};

	const handleRandomCode = () => {
		const name = form.getFieldValue("name") as string | undefined;
		form.setFieldValue("code", generateDomainCode(name));
		setDirty(true);
	};

	const handleSubmit = async () => {
		const values = await form.validateFields().catch(() => null);
		if (!values) {
			return;
		}
		const payload: CreateAdminDomainPayload = {
			name: values.name.trim(),
			code: values.code.trim(),
			logo: values.logo?.trim() || DEFAULT_DOMAIN_LOGO,
			description: values.description?.trim() || undefined,
			visibility_policy_codes: normalizeVisibility(values.visibility_policy_codes ?? ["public"]),
			registration_policy: values.registration_policy,
		};
		setSubmitting(true);
		try {
			const result = await createAdminDomain(payload);
			message.success("已创建业务域");
			onCreated({ id: result.id, name: payload.name });
			onClose();
		}
		catch (error) {
			message.error(toErrorMessage(error));
		}
		finally {
			setSubmitting(false);
		}
	};

	return (
		<Drawer
			title="新建业务域"
			width={560}
			open={open}
			onClose={requestClose}
			maskClosable={false}
			destroyOnClose
			footer={(
				<div className="flex justify-end">
					<Space>
						<Button onClick={requestClose}>取消</Button>
						<AuthGuarded auth="domain.admin.create">
							<Button type="primary" loading={submitting} onClick={() => void handleSubmit()}>
								创建
							</Button>
						</AuthGuarded>
					</Space>
				</div>
			)}
		>
			<Form
				form={form}
				layout="vertical"
				onValuesChange={() => setDirty(true)}
				initialValues={{
					name: "",
					code: "",
					logo: DEFAULT_DOMAIN_LOGO,
					description: "",
					visibility_policy_codes: ["public"] as P0VisibilityPolicyCode[],
					registration_policy: "open",
				}}
			>
				<DomainLogoField
					form={form}
					uploadDomainId={DOMAIN_LOGO_UPLOAD_FALLBACK_DOMAIN_ID}
				/>
				<Row gutter={16}>
					<Col span={24}>
						<Form.Item name="name" label="名称" rules={[{ required: true, message: "请输入名称" }]}>
							<Input placeholder="业务域显示名称" />
						</Form.Item>
					</Col>
					<Col span={24}>
						<Form.Item
							name="code"
							label="短码"
							extra="仅小写字母、数字、下划线与连字符"
							rules={[
								{ required: true, message: "请输入短码" },
								{ pattern: /^[a-z0-9_-]+$/, message: "仅支持小写字母、数字、下划线与连字符" },
							]}
						>
							<Space.Compact className="!w-full">
								<Input className="!flex-1" placeholder="例如 acme-support" />
								<Button icon={<ReloadOutlined />} onClick={handleRandomCode}>
									随机生成
								</Button>
							</Space.Compact>
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
