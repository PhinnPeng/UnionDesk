import type { AdminDomain, CreateAdminDomainPayload } from "@uniondesk/shared";
import { updateAdminDomain, toErrorMessage } from "@uniondesk/shared";

import { AuthGuarded } from "#src/components/auth-guarded";

import { App, Button, Col, Form, Input, Row, Select, Space, Switch } from "antd";
import { useEffect, useState } from "react";

import { normalizeVisibility, registrationOptions } from "../constants";
import { DEFAULT_DOMAIN_LOGO, isDomainEnabled, resolveNumericDomainId } from "../domain-utils";
import { DomainLogoField } from "../components/domain-logo-field";
import { VisibilityPolicyField } from "../components/visibility-policy-field";

interface DomainBasicInfoTabProps {
	domain: AdminDomain;
	onSaved: (domain: AdminDomain) => void;
}

type BasicInfoFormValues = Pick<CreateAdminDomainPayload, "name" | "logo" | "description" | "registration_policy" | "visibility_policy_codes"> & {
	code: string;
	enabled: boolean;
};

export function DomainBasicInfoTab({ domain, onSaved }: DomainBasicInfoTabProps) {
	const { message } = App.useApp();
	const [form] = Form.useForm<BasicInfoFormValues>();
	const [submitting, setSubmitting] = useState(false);

	useEffect(() => {
		form.setFieldsValue({
			name: domain.name,
			logo: domain.logo?.trim() || DEFAULT_DOMAIN_LOGO,
			description: domain.description ?? "",
			code: domain.code,
			registration_policy: domain.registration_policy,
			visibility_policy_codes: domain.visibility_policy_codes,
			enabled: isDomainEnabled(domain),
		});
	}, [domain, form]);

	const handleSave = async () => {
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
				status: values.enabled ? 1 : 0,
			});
			message.success("已保存基础信息");
			onSaved({
				...domain,
				name: values.name.trim(),
				logo,
				description: values.description?.trim() || null,
				registration_policy: values.registration_policy,
				visibility_policy_codes: normalizeVisibility(values.visibility_policy_codes ?? ["public"]),
				status: values.enabled ? "1" : "0",
			});
		}
		catch (error) {
			message.error(toErrorMessage(error));
		}
		finally {
			setSubmitting(false);
		}
	};

	return (
		<Form form={form} layout="vertical" className="max-w-2xl">
			<DomainLogoField
				form={form}
				previewName={domain.name}
				uploadDomainId={resolveNumericDomainId(domain.id)}
			/>
			<Row gutter={16}>
				<Col span={24}>
					<Form.Item name="name" label="名称" rules={[{ required: true, message: "请输入名称" }]}>
						<Input />
					</Form.Item>
				</Col>
				<Col span={24}>
					<Form.Item name="code" label="短码">
						<Input disabled />
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
				<Col span={24}>
					<Form.Item name="enabled" label="状态" valuePropName="checked">
						<Switch checkedChildren="已启用" unCheckedChildren="已禁用" />
					</Form.Item>
				</Col>
			</Row>
			<AuthGuarded auth="domain.admin.update">
				<Space>
					<Button type="primary" loading={submitting} onClick={() => void handleSave()}>
						保存
					</Button>
				</Space>
			</AuthGuarded>
		</Form>
	);
}
