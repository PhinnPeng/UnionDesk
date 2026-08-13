import { fetchSystemConfig, updateSystemConfig } from "#src/api/platform/system-config";
import { BasicContent } from "#src/components/basic-content";

import { App, Button, Card, Form, Input, Select, Space, Typography } from "antd";
import { useEffect, useState } from "react";

export default function PlatformSystemSettings() {
	const { message } = App.useApp();
	const [loading, setLoading] = useState(false);
	const [form] = Form.useForm();

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
					</Form>

					<div className="flex justify-end">
						<Button type="primary" loading={loading} onClick={() => void onSave()}>
							保存设置
						</Button>
					</div>
				</Space>
			</Card>
		</BasicContent>
	);
}

