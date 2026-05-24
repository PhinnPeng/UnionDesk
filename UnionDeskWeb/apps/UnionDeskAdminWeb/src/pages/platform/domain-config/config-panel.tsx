import { fetchDomainConfig, updateDomainConfig } from "#src/api/platform/domain-config";

import { App, Button, Card, Col, Form, Input, Row, Select, Space } from "antd";
import { useEffect, useState } from "react";

import { resolveNumericDomainId } from "../domains/domain-utils";

interface DomainConfigPanelProps {
	domainId: string;
}

/** 域配置 KV 表单（可嵌入详情 Tab） */
export function DomainConfigPanel({ domainId }: DomainConfigPanelProps) {
	const { message } = App.useApp();
	const numericDomainId = resolveNumericDomainId(domainId);
	const [loading, setLoading] = useState(false);
	const [form] = Form.useForm();

	useEffect(() => {
		if (!numericDomainId) {
			form.resetFields();
			return;
		}
		let ignore = false;
		void (async () => {
			setLoading(true);
			try {
				const data = await fetchDomainConfig(numericDomainId);
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
				message.error(error instanceof Error ? error.message : "加载域配置失败");
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
	}, [form, message, numericDomainId]);

	const onSave = async () => {
		if (!numericDomainId) {
			message.warning("当前业务域 ID 无法加载域配置");
			return;
		}
		const values = await form.validateFields().catch(() => null);
		if (!values) {
			return;
		}
		try {
			await updateDomainConfig(numericDomainId, {
				items: (values.items ?? []).map((item: Record<string, string>) => ({
					key: item.key,
					value: item.value,
					valueType: item.valueType,
					description: item.description,
				})),
			});
			message.success("域配置已保存");
		}
		catch (error) {
			message.error(error instanceof Error ? error.message : "保存失败");
		}
	};

	if (!numericDomainId) {
		return (
			<Card size="small" type="inner">
				当前业务域暂不支持域配置 KV（需数字型业务域 ID）。
			</Card>
		);
	}

	return (
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
									title={`配置项 #${index + 1}`}
									extra={<Button type="link" danger onClick={() => remove(field.name)}>删除</Button>}
								>
									<Row gutter={16}>
										<Col xs={24} lg={12}>
											<Form.Item
												{...field}
												name={[field.name, "key"]}
												label="配置键"
												rules={[{ required: true, message: "请输入配置键" }]}
											>
												<Input placeholder="如 business_hours" />
											</Form.Item>
										</Col>
										<Col xs={24} lg={12}>
											<Form.Item
												{...field}
												name={[field.name, "valueType"]}
												label="值类型"
												initialValue="string"
											>
												<Select
													options={[
														{ label: "string", value: "string" },
														{ label: "number", value: "number" },
														{ label: "boolean", value: "boolean" },
														{ label: "json", value: "json" },
													]}
												/>
											</Form.Item>
										</Col>
									</Row>
									<Form.Item {...field} name={[field.name, "value"]} label="配置值">
										<Input.TextArea rows={3} placeholder="请输入配置值" />
									</Form.Item>
									<Form.Item {...field} name={[field.name, "description"]} label="说明">
										<Input placeholder="该项配置的业务含义" />
									</Form.Item>
								</Card>
							))}
							<Button type="dashed" onClick={() => add({ valueType: "string" })}>新增配置项</Button>
						</Space>
					)}
				</Form.List>
			</Form>
			<div className="flex justify-end">
				<Button type="primary" onClick={() => void onSave()} loading={loading}>
					保存配置
				</Button>
			</div>
		</Space>
	);
}
