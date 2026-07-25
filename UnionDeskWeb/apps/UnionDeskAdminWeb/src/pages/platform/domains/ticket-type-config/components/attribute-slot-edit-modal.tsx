import type { TicketAttribute } from "@uniondesk/shared";

import {
	resolveAttributeTypeKey,
} from "#src/pages/platform/ticket-config/attributes/components/attribute-utils";
import type { SlotRow } from "./attribute-slot-table";
import {
	parseSlotDefaultValue,
	serializeSlotDefaultValue,
	type SlotDefaultValue,
} from "./attribute-default-value";

import { DatePicker, Form, Input, InputNumber, Modal, Select } from "antd";
import dayjs from "dayjs";
import { useEffect, useMemo } from "react";

interface AttributeSlotEditModalProps {
	open: boolean;
	slot: SlotRow | null;
	loading?: boolean;
	onCancel: () => void;
	onSubmit: (patch: {
		display_name: string;
		placeholder?: string;
		default_value?: string;
	}) => void | Promise<void>;
}

interface FormValues {
	display_name: string;
	placeholder?: string;
	default_literal?: string | number | boolean | string[] | null;
	default_date?: dayjs.Dayjs | null;
}

function toFormDefault(attribute: TicketAttribute, parsed: SlotDefaultValue | null): Partial<FormValues> {
	const typeKey = resolveAttributeTypeKey(attribute.field_type, attribute.type_config);
	if (!parsed || parsed.mode !== "literal") {
		return {};
	}
	if (typeKey === "switch") {
		if (parsed.value === true) {
			return { default_literal: "true" };
		}
		if (parsed.value === false) {
			return { default_literal: "false" };
		}
		return {};
	}
	if (typeKey === "date" || typeKey === "datetime") {
		const text = parsed.value == null ? "" : String(parsed.value);
		return {
			default_date: text ? dayjs(text) : null,
		};
	}
	return { default_literal: parsed.value };
}

function fromFormDefault(attribute: TicketAttribute, values: FormValues): SlotDefaultValue | null {
	const typeKey = resolveAttributeTypeKey(attribute.field_type, attribute.type_config);
	if (typeKey === "switch") {
		if (values.default_literal === "true") {
			return { mode: "literal", value: true };
		}
		if (values.default_literal === "false") {
			return { mode: "literal", value: false };
		}
		return null;
	}
	if (typeKey === "date" || typeKey === "datetime") {
		if (!values.default_date) {
			return null;
		}
		return {
			mode: "literal",
			value: values.default_date.format(typeKey === "datetime" ? "YYYY-MM-DD HH:mm:ss" : "YYYY-MM-DD"),
		};
	}
	const literal = values.default_literal;
	if (literal === null || literal === undefined) {
		return null;
	}
	if (typeof literal === "string" && literal.trim() === "") {
		return null;
	}
	if (Array.isArray(literal) && literal.length === 0) {
		return null;
	}
	return { mode: "literal", value: literal };
}

function DefaultValueField({ attribute }: { attribute: TicketAttribute }) {
	const typeKey = resolveAttributeTypeKey(attribute.field_type, attribute.type_config);
	const options = attribute.type_config?.options ?? [];

	if (typeKey === "switch") {
		return (
			<Form.Item name="default_literal" label="默认值">
				<Select
					allowClear
					placeholder="未指定"
					options={[
						{ label: "是", value: "true" },
						{ label: "否", value: "false" },
					]}
				/>
			</Form.Item>
		);
	}

	if (typeKey === "single_select") {
		return (
			<Form.Item name="default_literal" label="默认值">
				<Select allowClear placeholder="未指定" options={options.map(item => ({
					label: item.label,
					value: item.value,
				}))}
				/>
			</Form.Item>
		);
	}

	if (typeKey === "multi_select") {
		return (
			<Form.Item name="default_literal" label="默认值">
				<Select
					allowClear
					mode="multiple"
					placeholder="未指定"
					options={options.map(item => ({
						label: item.label,
						value: item.value,
					}))}
				/>
			</Form.Item>
		);
	}

	if (typeKey === "date" || typeKey === "datetime") {
		return (
			<Form.Item name="default_date" label="默认值">
				<DatePicker
					className="w-full"
					showTime={typeKey === "datetime"}
					placeholder="未指定"
				/>
			</Form.Item>
		);
	}

	if (typeKey === "integer" || typeKey === "decimal") {
		return (
			<Form.Item name="default_literal" label="默认值">
				<InputNumber
					className="w-full"
					precision={typeKey === "decimal" ? 2 : 0}
					placeholder="未指定"
				/>
			</Form.Item>
		);
	}

	if (typeKey === "multi_line_text") {
		return (
			<Form.Item name="default_literal" label="默认值">
				<Input.TextArea rows={3} placeholder="未指定" />
			</Form.Item>
		);
	}

	return (
		<Form.Item name="default_literal" label="默认值">
			<Input placeholder="未指定" />
		</Form.Item>
	);
}

export function AttributeSlotEditModal({
	open,
	slot,
	loading = false,
	onCancel,
	onSubmit,
}: AttributeSlotEditModalProps) {
	const [form] = Form.useForm<FormValues>();
	const attribute = slot?.attribute;

	const title = useMemo(() => {
		if (!slot) {
			return "编辑属性";
		}
		return `编辑属性 — ${slot.slot_config.display_name?.trim() || slot.attribute.name}`;
	}, [slot]);

	useEffect(() => {
		if (!open || !slot || !attribute) {
			return;
		}
		const parsed = parseSlotDefaultValue(slot.slot_config.default_value);
		form.setFieldsValue({
			display_name: slot.slot_config.display_name?.trim() || slot.attribute.name,
			placeholder: slot.slot_config.placeholder ?? "",
			...toFormDefault(attribute, parsed),
		});
	}, [attribute, form, open, slot]);

	const handleOk = async () => {
		if (!slot || !attribute) {
			return;
		}
		const values = await form.validateFields();
		const defaultValue = fromFormDefault(attribute, values);
		await onSubmit({
			display_name: values.display_name.trim(),
			placeholder: values.placeholder?.trim() || undefined,
			default_value: serializeSlotDefaultValue(defaultValue),
		});
	};

	return (
		<Modal
			title={title}
			open={open}
			confirmLoading={loading}
			destroyOnHidden
			okText="确定"
			cancelText="取消"
			onCancel={onCancel}
			onOk={() => void handleOk()}
		>
			<Form form={form} layout="vertical" preserve={false}>
				<Form.Item
					name="display_name"
					label="显示名称"
					extra="仅影响当前事项类型下的表单展示，不会修改全局属性字典。"
					rules={[{ required: true, message: "请输入显示名称" }]}
				>
					<Input maxLength={64} placeholder="请输入显示名称" />
				</Form.Item>
				<Form.Item name="placeholder" label="占位符">
					<Input maxLength={128} placeholder="未指定" />
				</Form.Item>
				{attribute ? <DefaultValueField attribute={attribute} /> : null}
			</Form>
		</Modal>
	);
}
