import type {
	AdditionalAttributeItem,
	TicketAttribute,
	TicketAttributeSlot,
} from "@uniondesk/shared";

import { DatePicker, Form, Input, Radio, Select, Switch } from "antd";
import dayjs from "dayjs";
import { useMemo } from "react";

export type AdditionalAttributeDraft = {
	slot_id: string | null;
	required: boolean;
	default_mode: "keep" | "set";
	default_value: unknown;
};

interface StepRuleAdditionalAttributeFormProps {
	value: AdditionalAttributeDraft;
	slots: Array<TicketAttributeSlot & { dragId: string }>;
	availableAttributes: TicketAttribute[];
	/** 已占用的 slot（编辑当前项时排除自身） */
	excludeSlotIds?: string[];
	onChange: (patch: Partial<AdditionalAttributeDraft>) => void;
}

function resolveAttribute(
	slot: TicketAttributeSlot | undefined,
	availableAttributes: TicketAttribute[],
): TicketAttribute | null {
	if (!slot) return null;
	return slot.attribute
		?? availableAttributes.find(a => a.id === slot.attribute_id)
		?? null;
}

function AttributeValueEditor({
	attribute,
	value,
	onChange,
}: {
	attribute: TicketAttribute | null;
	value: unknown;
	onChange: (val: unknown) => void;
}) {
	if (!attribute) {
		return (
			<Input
				placeholder="请输入默认值"
				value={(value as string) ?? ""}
				onChange={e => onChange(e.target.value)}
			/>
		);
	}

	switch (attribute.field_type) {
		case "select":
			return (
				<Select
					allowClear
					placeholder="请选择默认值"
					value={value as string | undefined}
					onChange={onChange}
					options={attribute.type_config.options?.map(o => ({
						value: o.value,
						label: o.label,
					}))}
					style={{ width: "100%" }}
				/>
			);
		case "switch":
			return (
				<Switch
					checked={Boolean(value)}
					onChange={onChange}
				/>
			);
		case "date": {
			const dateValue = value
				? dayjs(value as string)
				: null;
			return (
				<DatePicker
					value={dateValue?.isValid() ? dateValue : null}
					onChange={(_, dateString) => onChange(dateString || "")}
					style={{ width: "100%" }}
				/>
			);
		}
		case "input":
		default:
			return (
				<Input
					placeholder="请输入默认值"
					value={(value as string) ?? ""}
					onChange={e => onChange(e.target.value)}
				/>
			);
	}
}

export function StepRuleAdditionalAttributeForm({
	value,
	slots,
	availableAttributes,
	excludeSlotIds = [],
	onChange,
}: StepRuleAdditionalAttributeFormProps) {
	const selectedSlot = useMemo(
		() => slots.find(s => s.id === value.slot_id),
		[slots, value.slot_id],
	);
	const selectedAttribute = resolveAttribute(selectedSlot, availableAttributes);
	const slotForcedRequired = selectedSlot?.slot_config?.required === true;

	const slotOptions = slots
		.filter(s => s.id === value.slot_id || !excludeSlotIds.includes(s.id))
		.map(s => ({
			value: s.id,
			label: s.attribute?.name ?? s.slot_config?.display_name ?? s.id,
		}));

	return (
		<div className="step-rule-additional-form">
			<Form.Item label="附加属性" required style={{ marginBottom: 16 }}>
				<Select
					placeholder="请选择属性"
					value={value.slot_id ?? undefined}
					options={slotOptions}
					onChange={(slot_id) => {
						const slot = slots.find(s => s.id === slot_id);
						const forced = slot?.slot_config?.required === true;
						onChange({
							slot_id,
							required: forced ? true : false,
							default_mode: "keep",
							default_value: undefined,
						});
					}}
					showSearch
					optionFilterProp="label"
				/>
			</Form.Item>

			{value.slot_id && (
				<>
					<Form.Item label="属性默认值" style={{ marginBottom: 8 }}>
						<Radio.Group
							value={value.default_mode}
							onChange={(e) => {
								const default_mode = e.target.value as "keep" | "set";
								onChange({
									default_mode,
									default_value: default_mode === "keep" ? undefined : value.default_value,
								});
							}}
						>
							<Radio value="keep">保持原有值</Radio>
							<Radio value="set">更改为</Radio>
						</Radio.Group>
					</Form.Item>

					{value.default_mode === "set" ? (
						<Form.Item style={{ marginBottom: 16 }}>
							<AttributeValueEditor
								attribute={selectedAttribute}
								value={value.default_value}
								onChange={default_value => onChange({ default_value })}
							/>
						</Form.Item>
					) : (
						<p className="step-rule-additional-form__hint">默认值将保持原有值</p>
					)}

					<Form.Item label="该属性必填" style={{ marginBottom: 0 }}>
						<Switch
							checked={slotForcedRequired ? true : value.required}
							disabled={slotForcedRequired}
							onChange={required => onChange({ required })}
						/>
						{slotForcedRequired && (
							<span className="step-rule-additional-form__forced">
								该属性在事项类型中已设为必填，不可关闭
							</span>
						)}
					</Form.Item>
				</>
			)}
		</div>
	);
}

export function draftFromItem(item: AdditionalAttributeItem | null): AdditionalAttributeDraft {
	if (!item) {
		return {
			slot_id: null,
			required: false,
			default_mode: "keep",
			default_value: undefined,
		};
	}
	return {
		slot_id: item.slot_id,
		required: item.required,
		default_mode: item.default_mode,
		default_value: item.default_value,
	};
}

export function itemFromDraft(draft: AdditionalAttributeDraft): AdditionalAttributeItem | null {
	if (!draft.slot_id) return null;
	return {
		slot_id: draft.slot_id,
		required: draft.required,
		default_mode: draft.default_mode,
		default_value: draft.default_mode === "set" ? draft.default_value : undefined,
	};
}
