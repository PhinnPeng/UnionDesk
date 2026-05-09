import type { BusinessDomainView } from "@uniondesk/shared";

import { fetchBusinessDomains } from "#src/api/platform/domain";
import { fetchDomainConfig, updateDomainConfig } from "#src/api/platform/domain-config";
import { BasicContent } from "#src/components/basic-content";

import { App, Button, Card, Form, Input, Select, Space, Typography } from "antd";
import { useEffect, useMemo, useState } from "react";

type ValueType = "string" | "number" | "boolean" | "json";

export default function PlatformDomainConfig() {
	const { message } = App.useApp();
	const [domains, setDomains] = useState<BusinessDomainView[]>([]);
	const [domainId, setDomainId] = useState<number>();
	const [loading, setLoading] = useState(false);
	const [form] = Form.useForm();

	useEffect(() => {
		let ignore = false;
		void (async () => {
			try {
				const list = await fetchBusinessDomains();
				if (ignore) {
					return;
				}
				setDomains(list);
				setDomainId(list[0]?.id);
			}
			catch (error) {
				message.error(error instanceof Error ? error.message : "加载业务域失败");
			}
		})();
		return () => {
			ignore = true;
		};
	}, [message]);

	useEffect(() => {
		if (!domainId) {
			return;
		}
		let ignore = false;
		void (async () => {
			setLoading(true);
			try {
				const data = await fetchDomainConfig(domainId);
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
	}, [domainId, form, message]);

	const domainOptions = useMemo(() => {
		return domains.map(domain => ({
			label: `${domain.name} / ${domain.code}`,
			value: domain.id,
		}));
	}, [domains]);

	const onSave = async () => {
		if (!domainId) {
			return;
		}
		const values = await form.validateFields().catch(() => null);
		if (!values) {
			return;
		}
		try {
			await updateDomainConfig(domainId, {
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

	return (
		<BasicContent className="h-full bg-colorBgLayout">
			<Card
				title="域配置"
				bordered={false}
				extra={(
					<Space>
						<Typography.Text type="secondary">按业务域独立维护工作时间、时区和语言等基础配置</Typography.Text>
					</Space>
				)}
			>
				<Space direction="vertical" size={16} className="w-full">
					<div className="flex flex-wrap items-center gap-3">
						<Typography.Text className="text-slate-600">业务域</Typography.Text>
						<Select
							className="min-w-72"
							value={domainId}
							options={domainOptions}
							onChange={setDomainId}
							placeholder="请选择业务域"
						/>
					</div>

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
											<div className="grid gap-4 lg:grid-cols-2">
												<Form.Item
													{...field}
													name={[field.name, "key"]}
													label="配置键"
													rules={[{ required: true, message: "请输入配置键" }]}
												>
													<Input placeholder="如 business_hours" />
												</Form.Item>
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
											</div>
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
			</Card>
		</BasicContent>
	);
}

