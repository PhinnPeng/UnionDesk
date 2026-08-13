import { P0_STEP_UP_OPERATION, postAuthStepUp, toErrorMessage } from "@uniondesk/shared";

import { App, Form, Input, Modal } from "antd";
import { useState } from "react";

export type StepUpModalProps = {
	open: boolean;
	title?: string;
	description?: string;
	/** 与 P0 契约一致的 operation_code，默认删除业务域 */
	operationCode?: string;
	onCancel: () => void;
	/** 验证成功并拿到 step_up_token */
	onVerified: (stepUpToken: string) => void;
};

/**
 * P0 敏感操作二次验证：输入登录密码后调用 `POST /auth/step-up`。
 * @see doc/P0接口契约表.md §1.9
 */
export default function StepUpModal(props: StepUpModalProps) {
	const { open, title = "二次验证", description, operationCode = P0_STEP_UP_OPERATION.DELETE_BUSINESS_DOMAIN, onCancel, onVerified } = props;
	const { message } = App.useApp();
	const [loading, setLoading] = useState(false);
	const [form] = Form.useForm<{ password: string }>();

	const handleOk = async () => {
		const values = await form.validateFields().catch(() => null);
		if (!values) {
			return;
		}
		setLoading(true);
		try {
			const res = await postAuthStepUp({ password: values.password, operation_code: operationCode });
			form.resetFields();
			onVerified(res.step_up_token);
		} catch (e) {
			message.error(toErrorMessage(e));
		} finally {
			setLoading(false);
		}
	};

	const handleCancel = () => {
		form.resetFields();
		onCancel();
	};

	return (
		<Modal
			open={open}
			title={title}
			okText="验证"
			cancelText="取消"
			confirmLoading={loading}
			onOk={handleOk}
			onCancel={handleCancel}
			destroyOnClose
		>
			<p className="mb-3 text-sm text-[var(--ant-color-text-secondary)]">
				{description ?? "为符合 P0 安全要求，请再次输入当前账号登录密码以获取短时 step-up 令牌。"}
			</p>
			<Form form={form} layout="vertical">
				<Form.Item
					label="登录密码"
					name="password"
					rules={[{ required: true, message: "请输入密码" }]}
				>
					<Input.Password autoComplete="current-password" placeholder="请输入密码" />
				</Form.Item>
			</Form>
		</Modal>
	);
}
