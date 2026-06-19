import { Form, FormItem, Input, NumberPicker, Select } from "@formily/antd-v5";
import { createForm } from "@formily/core";
import { FormProvider, createSchemaField } from "@formily/react";

import { DeleteOutlined, PlusOutlined } from "@ant-design/icons";
import { Button, Card, List, Space, Tag, Typography } from "antd";
import { useEffect, useMemo, useState, type ChangeEvent } from "react";

import { DEFAULT_FORM_SCHEMA, mergeSystemFormSchema } from "./ticket-type-form-defaults";

const { Text } = Typography;

const SchemaField = createSchemaField({
	components: {
		FormItem,
		Input,
		NumberPicker,
		Select,
	},
});

type FieldSchema = Record<string, unknown> & {
	title?: string;
	type?: string;
	"x-component"?: string;
	"x-system-field"?: boolean;
};

export interface TicketTypeFormDesignerProps {
	value: Record<string, unknown> | null | undefined;
	onChange: (schema: Record<string, unknown>) => void;
	disabled?: boolean;
}

const EXTENDABLE_COMPONENTS = [
	{ label: "单行文本", component: "Input" },
	{ label: "多行文本", component: "Input.TextArea" },
	{ label: "数字", component: "NumberPicker" },
	{ label: "下拉选择", component: "Select" },
];

function createFieldKey(existing: Record<string, FieldSchema>): string {
	let index = Object.keys(existing).length + 1;
	let key = `field_${index}`;
	while (existing[key]) {
		index += 1;
		key = `field_${index}`;
	}
	return key;
}

export function TicketTypeFormDesigner({ value, onChange, disabled }: TicketTypeFormDesignerProps) {
	const mergedSchema = useMemo(() => mergeSystemFormSchema(value), [value]);
	const properties = (mergedSchema.properties ?? {}) as Record<string, FieldSchema>;
	const [previewForm] = useState(() => createForm());

	useEffect(() => {
		previewForm.setValues({});
	}, [mergedSchema, previewForm]);

	const handleAddField = (component: string) => {
		const nextProperties = { ...properties };
		const key = createFieldKey(nextProperties);
		nextProperties[key] = {
			type: component === "NumberPicker" ? "number" : "string",
			title: `字段${Object.keys(nextProperties).length}`,
			"x-component": component,
			"x-decorator": "FormItem",
			"x-component-props": component === "Input.TextArea" ? { rows: 3 } : undefined,
			"x-index": Object.keys(nextProperties).length,
		};
		onChange({ ...mergedSchema, properties: nextProperties });
	};

	const handleRemoveField = (key: string) => {
		const nextProperties = { ...properties };
		delete nextProperties[key];
		onChange({ ...mergedSchema, properties: nextProperties });
	};

	const handleRenameField = (key: string, title: string) => {
		const nextProperties = {
			...properties,
			[key]: { ...properties[key], title },
		};
		onChange({ ...mergedSchema, properties: nextProperties });
	};

	const sortedEntries = Object.entries(properties).sort(([, a], [, b]) => {
		const indexA = typeof a["x-index"] === "number" ? a["x-index"] : 0;
		const indexB = typeof b["x-index"] === "number" ? b["x-index"] : 0;
		return indexA - indexB;
	});

	return (
		<div className="grid grid-cols-2 gap-4">
			<Card title="字段配置" bordered={false} size="small">
				<Space wrap className="mb-3">
					{EXTENDABLE_COMPONENTS.map(item => (
						<Button
							key={item.component}
							size="small"
							icon={<PlusOutlined />}
							disabled={disabled}
							onClick={() => handleAddField(item.component)}
						>
							{item.label}
						</Button>
					))}
				</Space>
				<List
					size="small"
					dataSource={sortedEntries}
					renderItem={([key, field]) => (
						<List.Item
							actions={field["x-system-field"] || disabled ? [] : [
								<Button
									key="delete"
									type="text"
									danger
									size="small"
									icon={<DeleteOutlined />}
									onClick={() => handleRemoveField(key)}
								/>,
							]}
						>
							<List.Item.Meta
								title={(
									<Space>
										{field["x-system-field"] ? (
											<Input
												size="small"
												value={field.title}
												disabled
												style={{ width: 160 }}
											/>
										) : (
											<Input
												size="small"
												value={field.title}
												style={{ width: 160 }}
												onChange={(event: ChangeEvent<HTMLInputElement>) => handleRenameField(key, event.target.value)}
											/>
										)}
										{field["x-system-field"] ? <Tag color="blue">系统</Tag> : null}
										<Text type="secondary">{field["x-component"]}</Text>
									</Space>
								)}
								description={`key: ${key}`}
							/>
						</List.Item>
					)}
				/>
			</Card>
			<Card title="Formily 预览" bordered={false} size="small">
				<FormProvider form={previewForm}>
					<Form layout="vertical">
						<SchemaField schema={mergedSchema ?? DEFAULT_FORM_SCHEMA} />
					</Form>
				</FormProvider>
			</Card>
		</div>
	);
}
