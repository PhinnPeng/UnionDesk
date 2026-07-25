import type {
	AttributeUpdateItem,
	TicketAttribute,
	TicketAttributeSlot,
} from "@uniondesk/shared";

import { MemberPicker } from "#src/pages/platform/components/member-picker";

import { Button, DatePicker, Input, Select, Switch, Table } from "antd";

interface StepRuleAttributeUpdateEditorProps {
	value: AttributeUpdateItem[];
	slots: Array<TicketAttributeSlot & { dragId: string }>;
	availableAttributes: TicketAttribute[];
	domainId?: string;
	onChange: (attribute_updates: AttributeUpdateItem[]) => void;
}

export function StepRuleAttributeUpdateEditor({
	value,
	slots,
	availableAttributes,
	domainId,
	onChange,
}: StepRuleAttributeUpdateEditorProps) {
	const getSlotAttribute = (slotId: string) => {
		const slot = slots.find(s => s.id === slotId);
		if (!slot) return null;
		return availableAttributes.find(a => a.id === slot.attribute_id) ?? slot.attribute ?? null;
	};

	const renderAttributeInput = (slotId: string, itemValue: unknown, onValueChange: (val: unknown) => void) => {
		const attr = getSlotAttribute(slotId);
		if (!attr) {
			return (
				<Input
					placeholder="请输入值"
					value={itemValue as string}
					onChange={e => onValueChange(e.target.value)}
				/>
			);
		}

		switch (attr.field_type) {
			case "input":
				return (
					<Input
						placeholder="请输入值"
						value={itemValue as string}
						onChange={e => onValueChange(e.target.value)}
					/>
				);
			case "select": {
				const options = attr.type_config.options_source === "priority_levels"
					&& !(attr.type_config.options?.length)
					? [
						{ label: "紧急", value: "urgent" },
						{ label: "高", value: "high" },
						{ label: "中", value: "normal" },
						{ label: "低", value: "low" },
					]
					: (attr.type_config.options ?? []);
				return (
					<Select
						value={itemValue as string}
						onChange={onValueChange}
						options={options.map(o => ({
							value: o.value,
							label: o.label,
						}))}
						style={{ width: "100%" }}
					/>
				);
			}
			case "switch":
				return <Switch checked={itemValue as boolean} onChange={onValueChange} />;
			case "date":
				return <DatePicker value={itemValue as string} onChange={onValueChange} style={{ width: "100%" }} />;
			case "member":
				return (
					<MemberPicker
						domainId={domainId}
						multiple={!!attr.type_config.multiple}
						value={
							attr.type_config.multiple
								? (Array.isArray(itemValue) ? itemValue as number[] : [])
								: (typeof itemValue === "number" ? itemValue : null)
						}
						onChange={onValueChange}
					/>
				);
			default:
				return (
					<Input
						placeholder="请输入值"
						value={itemValue as string}
						onChange={e => onValueChange(e.target.value)}
					/>
				);
		}
	};

	const availableSlots = slots.filter(s => !value.some(u => u.slot_id === s.id));

	return (
		<div className="step-rule-editor">
			<Table
				dataSource={value}
				rowKey={(record, index) => `${record.slot_id}-${index}`}
				pagination={false}
				size="small"
				columns={[
					{
						title: "属性",
						dataIndex: "slot_id",
						render: (slotId: string) => {
							const slot = slots.find(s => s.id === slotId);
							return slot?.attribute?.name ?? slotId;
						},
					},
					{
						title: "值",
						render: (_, record: AttributeUpdateItem, index: number) => (
							renderAttributeInput(
								record.slot_id,
								record.value,
								(val) => {
									const next = [...value];
									next[index] = { ...record, value: val };
									onChange(next);
								},
							)
						),
					},
					{
						title: "操作",
						width: 64,
						render: (_, __, index: number) => (
							<Button
								type="link"
								danger
								size="small"
								onClick={() => onChange(value.filter((_, i) => i !== index))}
							>
								删除
							</Button>
						),
					},
				]}
			/>
			<Button
				type="dashed"
				size="small"
				className="step-rule-editor__add"
				disabled={availableSlots.length === 0}
				onClick={() => {
					const slot = availableSlots[0];
					if (!slot) return;
					onChange([
						...value,
						{
							slot_id: slot.id,
							value: "",
							value_type: "string",
						},
					]);
				}}
			>
				+ 添加属性变更
			</Button>
		</div>
	);
}

export function summarizeAttributeUpdates(count: number): string {
	return count > 0 ? `${count} 项属性变更` : "未配置属性变更";
}

export function hasAttributeUpdateRule(attribute_updates: AttributeUpdateItem[] | undefined): boolean {
	return (attribute_updates?.length ?? 0) > 0;
}
