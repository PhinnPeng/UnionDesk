import type { IamUser } from "@uniondesk/shared";

import { fetchUpdatePlatformUser } from "#src/api/platform/iam";
import { toErrorMessage } from "@uniondesk/shared";
import { generateResetPassword } from "#src/pages/platform/user/utils";

import { App, Button, Form, Input, Modal } from "antd";
import { useEffect, useState } from "react";

type ResetPasswordFormValues = {
	password: string;
	confirmPassword: string;
};

interface ResetPasswordModalProps {
	open: boolean;
	user: IamUser | null;
	onClose: () => void;
	onSuccess: (user: IamUser) => void | Promise<void>;
}

export function ResetPasswordModal({ open, user, onClose, onSuccess }: ResetPasswordModalProps) {
	const { message } = App.useApp();
	const [form] = Form.useForm<ResetPasswordFormValues>();
	const [saving, setSaving] = useState(false);

	useEffect(() => {
		if (!open) {
			form.resetFields();
		}
		else {
			form.resetFields();
		}
	}, [form, open, user]);

	const handleGeneratePassword = () => {
		const password = generateResetPassword();
		form.setFieldsValue({
			password,
			confirmPassword: password,
		});
	};

	const handleSave = async () => {
		const values = await form.validateFields().catch(() => null);
		if (!values || !user) {
			return;
		}

		if (values.password !== values.confirmPassword) {
			message.error("两次输入的密码不一致");
			return;
		}

		setSaving(true);
		try {
			const updatedUser = await fetchUpdatePlatformUser(user.id, {
				password: values.password,
			});
			message.success("密码重置成功");
			await onSuccess(updatedUser);
			onClose();
		}
		catch (error) {
			message.error(toErrorMessage(error));
		}
		finally {
			setSaving(false);
		}
	};

	return (
		<Modal
			open={open}
			title="重置密码"
			okText="重置"
			cancelText="取消"
			confirmLoading={saving}
			onOk={() => void handleSave()}
			onCancel={onClose}
			destroyOnHidden
		>
			<Form<ResetPasswordFormValues>
				form={form}
				layout="vertical"
				preserve={false}
			>
				<Form.Item
					name="password"
					label="密码"
					rules={[{ required: true, message: "请输入密码" }]}
				>
					<Input.Password autoComplete="new-password" placeholder="请输入密码" />
				</Form.Item>

				<Form.Item
					name="confirmPassword"
					label="确认密码"
					dependencies={["password"]}
					rules={[
						{ required: true, message: "请确认密码" },
						({ getFieldValue }) => ({
							validator: async (_, value) => {
								if (!value) {
									return Promise.reject(new Error("请确认密码"));
								}
								if (value !== getFieldValue("password")) {
									return Promise.reject(new Error("两次输入的密码不一致"));
								}
								return Promise.resolve();
							},
						}),
					]}
				>
					<Input.Password autoComplete="new-password" placeholder="请再次输入密码" />
				</Form.Item>

				<Button htmlType="button" onClick={handleGeneratePassword}>
					随机生成
				</Button>
			</Form>
		</Modal>
	);
}
