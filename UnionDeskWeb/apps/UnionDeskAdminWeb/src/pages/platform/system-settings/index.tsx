import { fetchLoginConfig, updateLoginConfig } from "#src/api/auth";
import { fetchSystemConfig, updateSystemConfig } from "#src/api/platform/system-config";
import { BasicContent } from "#src/components/basic-content";

import { App, Button, Card, Form, Input, InputNumber, Select, Space, Switch, Typography } from "antd";
import { useEffect, useState } from "react";

function splitIpList(value?: string | null): string[] {
	if (!value) {
		return [];
	}
	return value
		.split(/[,，\n]/)
		.map(item => item.trim())
		.filter(Boolean);
}

function joinIpList(items?: string[]): string {
	return (items ?? [])
		.map(item => item.trim())
		.filter(Boolean)
		.join(",");
}

export default function PlatformSystemSettings() {
	const { message } = App.useApp();
	const [loading, setLoading] = useState(false);
	const [securityLoading, setSecurityLoading] = useState(false);
	const [form] = Form.useForm();
	const [securityForm] = Form.useForm();

	useEffect(() => {
		let ignore = false;
		void (async () => {
			setLoading(true);
			try {
				const data = await fetchSystemConfig();
				if (ignore) {
					return;
				}
				form.setFieldsValue({
					items: data.items.map(item => ({
						key: item.key,
						value: item.value ?? "",
						valueType: item.valueType ?? "string",
						description: item.description ?? "",
					})),
				});
			}
			catch (error) {
				message.error(error instanceof Error ? error.message : "加载系统设置失败");
			}
			finally {
				if (!ignore) {
					setLoading(false);
				}
			}
		})();
		return () => {
			ignore = true;
		};
	}, [form, message]);

	useEffect(() => {
		let ignore = false;
		void (async () => {
			setSecurityLoading(true);
			try {
				const data = await fetchLoginConfig();
				if (ignore) {
					return;
				}
				securityForm.setFieldsValue({
					passwordMinLength: data.passwordMinLength ?? 8,
					passwordRequireMixed: data.passwordRequireMixed ?? false,
					loginFailLockEnabled: data.loginFailLockEnabled ?? false,
					loginFailMaxAttempts: data.loginFailMaxAttempts ?? 5,
					loginFailLockMinutes: data.loginFailLockMinutes ?? 30,
					ipWhitelistEnabled: data.ipWhitelistEnabled ?? false,
					ipWhitelist: splitIpList(data.ipWhitelist).join("\n"),
				});
			}
			catch (error) {
				message.error(error instanceof Error ? error.message : "加载安全策略失败");
			}
			finally {
				if (!ignore) {
					setSecurityLoading(false);
				}
			}
		})();
		return () => {
			ignore = true;
		};
	}, [securityForm, message]);

	const onSave = async () => {
		const values = await form.validateFields().catch(() => null);
		if (!values) {
			return;
		}
		try {
			await updateSystemConfig({
				items: (values.items ?? []).map((item: Record<string, string>) => ({
					key: item.key,
					value: item.value,
					valueType: item.valueType,
					description: item.description,
				})),
			});
			message.success("系统设置已保存");
		}
		catch (error) {
			message.error(error instanceof Error ? error.message : "保存失败");
		}
	};

	const onSaveSecurityPolicy = async () => {
		const values = await securityForm.validateFields().catch(() => null);
		if (!values) {
			return;
		}
		try {
			await updateLoginConfig({
				passwordMinLength: values.passwordMinLength,
				passwordRequireMixed: values.passwordRequireMixed,
				loginFailLockEnabled: values.loginFailLockEnabled,
				loginFailMaxAttempts: values.loginFailMaxAttempts,
				loginFailLockMinutes: values.loginFailLockMinutes,
				ipWhitelistEnabled: values.ipWhitelistEnabled,
				ipWhitelist: joinIpList(splitIpList(values.ipWhitelist)),
			});
			message.success("安全策略已保存");
		}
		catch (error) {
			message.error(error instanceof Error ? error.message : "保存失败");
		}
	};

	return (
		<BasicContent className="h-full bg-colorBgLayout">
			<Card
				title="系统设置"
				bordered={false}
				extra={<Typography.Text type="secondary">密码策略、会话超时和安全参数统一维护</Typography.Text>}
			>
				<Space direction="vertical" size={16} className="w-full">
					<Form form={form} layout="vertical" disabled={loading}>
						<Form.List name="items">
							{(fields, { add, remove }) => (
								<Space direction="vertical" size={12} className="w-full">
									{fields.map((field, index) => (
										<Card
											key={field.key}
											size="small"
											type="inner"
											title={`系统配置 #${index + 1}`}
											extra={<Button type="link" danger onClick={() => remove(field.name)}>删除</Button>}
										>
											<div className="grid gap-4 lg:grid-cols-2">
												<Form.Item
													{...field}
													name={[field.name, "key"]}
													label="配置键"
													rules={[{ required: true, message: "请输入配置键" }]}
												>
													<Input placeholder="如 password.min_length" />
												</Form.Item>
												<Form.Item {...field} name={[field.name, "valueType"]} label="值类型">
													<Select
														options={[
															{ label: "string", value: "string" },
															{ label: "number", value: "number" },
															{ label: "boolean", value: "boolean" },
															{ label: "json", value: "json" },
														]}
													/>
												</Form.Item>
											</div>
											<Form.Item {...field} name={[field.name, "value"]} label="配置值">
												<Input.TextArea rows={3} placeholder="请输入配置值" />
											</Form.Item>
											<Form.Item {...field} name={[field.name, "description"]} label="说明">
												<Input placeholder="这项设置的含义" />
											</Form.Item>
										</Card>
									))}
									<Button type="dashed" onClick={() => add({ valueType: "string" })}>新增设置项</Button>
								</Space>
							)}
						</Form.List>

						<div className="flex justify-end">
							<Button type="primary" loading={loading} onClick={() => void onSave()}>
								保存设置
							</Button>
						</div>
					</Form>
				</Space>
			</Card>

			<Card
				title="安全策略"
				bordered={false}
				className="mt-4"
				extra={<Typography.Text type="secondary">密码强度、登录失败锁定与 IP 白名单统一维护</Typography.Text>}
			>
				<Form form={securityForm} layout="vertical" disabled={securityLoading}>
					<div className="grid gap-4 lg:grid-cols-2">
						<Form.Item
							label="密码最小长度（位）"
							name="passwordMinLength"
							rules={[{ required: true, message: "请输入密码最小长度" }]}
						>
							<InputNumber min={1} max={64} className="w-full" placeholder="默认 8" />
						</Form.Item>
						<Form.Item label="密码需同时包含字母和数字" name="passwordRequireMixed" valuePropName="checked">
							<Switch />
						</Form.Item>
						<Form.Item label="启用连续失败锁定" name="loginFailLockEnabled" valuePropName="checked">
							<Switch />
						</Form.Item>
						<Form.Item
							label="失败次数阈值"
							name="loginFailMaxAttempts"
							rules={[{ required: true, message: "请输入失败次数阈值" }]}
						>
							<InputNumber min={1} max={100} className="w-full" placeholder="默认 5" />
						</Form.Item>
						<Form.Item
							label="锁定时长（分钟）"
							name="loginFailLockMinutes"
							rules={[{ required: true, message: "请输入锁定分钟数" }]}
						>
							<InputNumber min={1} max={1440} className="w-full" placeholder="默认 30" />
						</Form.Item>
						<Form.Item
							label="启用 IP 白名单"
							name="ipWhitelistEnabled"
							valuePropName="checked"
							tooltip="开启后仅允许列表中的 IP 登录管理端"
						>
							<Switch />
						</Form.Item>
					</div>
					<Form.Item label="白名单 IP 列表（每行一个，或逗号分隔）" name="ipWhitelist">
						<Input.TextArea rows={3} placeholder={"例如：\n10.0.0.1\n10.0.0.2"} />
					</Form.Item>
					<div className="flex justify-end">
						<Button type="primary" loading={securityLoading} onClick={() => void onSaveSecurityPolicy()}>
							保存安全策略
						</Button>
					</div>
				</Form>
			</Card>
		</BasicContent>
	);
}
